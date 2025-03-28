package com.sistema_pedidos

import com.sistema_pedidos.controller.ConfiguracoesController
import com.sistema_pedidos.database.DatabaseHelper
import com.sistema_pedidos.util.DwmApi
import com.sistema_pedidos.util.VersionChecker
import com.sistema_pedidos.util.WindowsStyler
import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Rectangle2D
import javafx.scene.Scene
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import com.sistema_pedidos.view.*
import javafx.scene.paint.Color
import javafx.scene.image.Image
import java.io.InputStream
import java.lang.management.ManagementFactory
import javafx.application.Platform.exit
import kotlin.system.exitProcess

import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.AWTException

class Main : Application() {
    companion object {
        var startMinimized = false
        private var trayIcon: TrayIcon? = null
    }
    override fun init() {
        val args = parameters.raw
        startMinimized = args.contains("-minimized")
    }

    override fun start(primaryStage: Stage) {

        ConfiguracoesController.initializeDailyBackup()

        primaryStage.setOnCloseRequest { event ->
            // Get a fresh instance of ConfiguracoesController to ensure latest settings
            val configController = ConfiguracoesController()
            println("Closing window. minimizeToTray setting: ${configController.minimizeToTray}")

            if (configController.minimizeToTray) {
                event.consume() // Prevent default close action
                println("Minimizing to tray instead of closing")
                primaryStage.hide() // Hide the window instead of closing

                setupSystemTray(primaryStage)
            } else {
                if (trayIconInstance != null) {
                    try {
                        SystemTray.getSystemTray().remove(trayIconInstance)
                        trayIconInstance = null
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        val splashScreen = SplashScreen(3000)
        splashScreen.show()

        Thread {
            try {
                Thread.sleep(100)

                val versionChecker = VersionChecker()
                versionChecker.onStatusUpdate = { statusMessage ->
                    Platform.runLater {

                        val scene = splashScreen.getScene()
                        val root = scene.root
                        if (root is javafx.scene.layout.VBox) {
                            val label = root.children.last() as? javafx.scene.control.Label
                            label?.text = statusMessage
                        }
                    }
                }
                versionChecker.progressCallback = { progress ->
                    Platform.runLater {
                        splashScreen.updateProgress(progress)
                    }
                }

                versionChecker.onStatusUpdate?.invoke("Versão atual: ${versionChecker.getCurrentVersion()}")
                Thread.sleep(500)

                val (updateAvailable, latestVersion, downloadUrl) = versionChecker.isUpdateAvailable()

                if (updateAvailable && downloadUrl != null) {
                    versionChecker.onStatusUpdate?.invoke("Nova versão disponível: $latestVersion")
                    Thread.sleep(1000)

                    val updateSuccess = versionChecker.downloadAndInstallUpdate(downloadUrl)

                    if (updateSuccess) {
                        versionChecker.onStatusUpdate?.invoke("Atualização instalada. Reiniciando...")
                        Thread.sleep(2000)

                        Platform.runLater {
                            splashScreen.hide {
                                exit()
                                exitProcess(0)
                            }
                        }
                        return@Thread
                    } else {
                        versionChecker.onStatusUpdate?.invoke("Falha na atualização. Continuando...")
                        Thread.sleep(1000)
                    }
                } else {
                    versionChecker.onStatusUpdate?.invoke("Sistema atualizado!")
                    Thread.sleep(500)
                }

                versionChecker.onStatusUpdate?.invoke("Inicializando o sistema...")

                if (!SingleInstanceLock.initialize(primaryStage)) {
                    Platform.exit()
                    return@Thread
                }

                setProcessName("Blossom ERP")
                Runtime.getRuntime().addShutdownHook(Thread {
                    try {
                        ConfiguracoesController.backupOnExitIfEnabled()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        SingleInstanceLock.release()
                    }
                })

                val dbHelper = DatabaseHelper()
                val tables = dbHelper.listTables()
                println("Tabelas no banco de dados: $tables")

                Platform.runLater {
                    try {
                        val mainView = MainView(primaryStage)
                        val dashboardView = DashboardView()
                        val novoPedidoView = NovoPedidoView()
                        val produtosView = ProdutosView()
                        val pedidosEmAndamentoView = PedidosEmAndamentoView()
                        val historicoPedidosView = HistoricoPedidosView()
                        val configuracoesView = ConfiguracoesView(primaryStage)
                        val clientesView = ClientesView()
                        val pedidoWizardView = PedidoWizardView()

                        val menuView = MenuView { viewName ->
                            when (viewName) {
                                "dashboard" -> mainView.setCenterView(dashboardView)
                                "pedidosAndamento" -> mainView.setCenterView(pedidosEmAndamentoView)
                                "produtos" -> mainView.setCenterView(produtosView)
                                "historicoPedidos" -> mainView.setCenterView(historicoPedidosView)
                                "configuracoes" -> mainView.setCenterView(configuracoesView)
                                "clientes" -> mainView.setCenterView(clientesView)
                                "wizard" -> mainView.setCenterView(pedidoWizardView)
                            }
                        }

                        mainView.setLeftMenu(menuView)
                        mainView.setCenterView(pedidoWizardView)

                        val screenBounds: Rectangle2D = Screen.getPrimary().visualBounds
                        primaryStage.x = screenBounds.minX
                        primaryStage.y = screenBounds.minY
                        primaryStage.width = screenBounds.width
                        primaryStage.height = screenBounds.height

                        // Set the scene and other properties
                        val scene = Scene(mainView, 1000.0, 680.0, Color.TRANSPARENT)
                        scene.stylesheets.add(javaClass.getResource("/styles.css").toExternalForm())

                        primaryStage.initStyle(StageStyle.DECORATED)
                        primaryStage.scene = scene
                        primaryStage.title = "Blossom ERP"

                        val image = Image("/icons/icon.png")
                        primaryStage.icons.add(image)

                        loadIcons(primaryStage)

                        primaryStage.minWidth = 800.0
                        primaryStage.minHeight = 600.0

                        primaryStage.opacity = 0.0
                        primaryStage.show()

                        Thread {
                            WindowsStyler.applyStyling(primaryStage, 0xFF312D2B.toInt())

                            Platform.runLater {
                                javafx.animation.PauseTransition(javafx.util.Duration.millis(300.0)).apply {
                                    setOnFinished {
                                        splashScreen.hide {
                                            primaryStage.opacity = 1.0
                                        }
                                    }
                                    play()
                                }
                            }
                        }.start()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Platform.runLater { splashScreen.hide() }
            }
        }.start()
    }

    private fun loadIcons(stage: Stage) {
        try {
            val icoStream = javaClass.getResourceAsStream("/icons/icon.ico")
            if (icoStream != null) {
                try {
                    val image = Image(icoStream)
                    stage.icons.add(image)
                } finally {
                    icoStream.close()
                }
            }

            val iconSizes = listOf(16, 32, 48, 64, 128, 256)
            for (size in iconSizes) {
                val iconStream = javaClass.getResourceAsStream("/icons/icon${size}.png")
                if (iconStream != null) {
                    try {
                        val image = Image(iconStream)
                        stage.icons.add(image)
                    } finally {
                        iconStream.close()
                    }
                }
            }

            if (stage.icons.isEmpty()) {
                val defaultIconStream = javaClass.getResourceAsStream("/icons/icon.png")
                if (defaultIconStream != null) {
                    try {
                        stage.icons.add(Image(defaultIconStream))
                    } finally {
                        defaultIconStream.close()
                    }
                }
            }

            println("Ícones carregados: ${stage.icons.size}")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Falha ao carregar ícones: ${e.message}")
        }
    }
}

private var trayIconInstance: TrayIcon? = null

private fun setupSystemTray(stage: Stage) {
    if (!SystemTray.isSupported()) {
        println("SystemTray is not supported")
        return
    }

    // If tray icon already exists, just return
    if (trayIconInstance != null) {
        println("Tray icon already exists")
        return
    }

    println("Creating new tray icon")
    val tray = SystemTray.getSystemTray()
    val image = Toolkit.getDefaultToolkit().getImage(Main::class.java.getResource("/icons/icon.png"))

    val popup = PopupMenu()
    val openItem = MenuItem("Abrir")
    openItem.addActionListener {
        Platform.runLater {
            stage.show()
            stage.toFront()
        }
    }

    val exitItem = MenuItem("Sair")
    exitItem.addActionListener {
        // Remove tray icon before exiting
        try {
            tray.remove(trayIconInstance)
            trayIconInstance = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Platform.exit()
        exitProcess(0)
    }

    popup.add(openItem)
    popup.addSeparator()
    popup.add(exitItem)

    val trayIcon = TrayIcon(image, "Blossom ERP", popup)
    trayIcon.isImageAutoSize = true

    trayIcon.addActionListener {
        Platform.runLater {
            stage.show()
            stage.toFront()
        }
    }

    try {
        tray.add(trayIcon)
        trayIconInstance = trayIcon
        println("Tray icon added successfully")
    } catch (e: AWTException) {
        println("TrayIcon could not be added: ${e.message}")
    }
}

fun setProcessName(name: String) {
    try {
        val pid = ManagementFactory.getRuntimeMXBean().name.split("@")[0]
        val process = ProcessBuilder("cmd", "/c", "title", name).start()
        process.waitFor()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun main() {
    Application.launch(Main::class.java)
}
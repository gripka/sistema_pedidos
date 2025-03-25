package com.sistema_pedidos

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

class Main : Application() {
    override fun start(primaryStage: Stage) {
        // Show splash screen first
        val splashScreen = SplashScreen(3000)
        splashScreen.show()

        // Load application in background thread
        Thread {
            try {
                Thread.sleep(100) // Pequena pausa para garantir que as animações começem

                // Check for updates
                val versionChecker = VersionChecker()
                versionChecker.onStatusUpdate = { statusMessage ->
                    Platform.runLater {
                        // Update splash screen status label
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

                // Show current version
                versionChecker.onStatusUpdate?.invoke("Versão atual: ${versionChecker.getCurrentVersion()}")
                Thread.sleep(500)

                // Check for updates
                val (updateAvailable, latestVersion, downloadUrl) = versionChecker.isUpdateAvailable()

                if (updateAvailable && downloadUrl != null) {
                    versionChecker.onStatusUpdate?.invoke("Nova versão disponível: $latestVersion")
                    Thread.sleep(1000)

                    // Download and install update
                    val updateSuccess = versionChecker.downloadAndInstallUpdate(downloadUrl)

                    if (updateSuccess) {
                        versionChecker.onStatusUpdate?.invoke("Atualização instalada. Reiniciando...")
                        Thread.sleep(2000)

                        // Exit application - the installer will restart it
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

                // Continue with normal application startup
                versionChecker.onStatusUpdate?.invoke("Inicializando o sistema...")

                // Application initialization
                if (!SingleInstanceLock.initialize(primaryStage)) {
                    Platform.exit()
                    return@Thread
                }

                setProcessName("Blossom ERP")
                Runtime.getRuntime().addShutdownHook(Thread { SingleInstanceLock.release() })

                // Initialize database (only once)
                val dbHelper = DatabaseHelper()
                val tables = dbHelper.listTables()
                println("Tabelas no banco de dados: $tables")


                // Prepare UI on JavaFX thread when ready
                Platform.runLater {
                    try {
                        // Initialize views
                        val mainView = MainView(primaryStage)
                        val dashboardView = DashboardView()
                        val novoPedidoView = NovoPedidoView()
                        val produtosView = ProdutosView()
                        val pedidosEmAndamentoView = PedidosEmAndamentoView()
                        val historicoPedidosView = HistoricoPedidosView()
                        val configuracoesView = ConfiguracoesView()
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

                        // Setup scene
                        val scene = Scene(mainView, 1000.0, 680.0, Color.TRANSPARENT)
                        scene.stylesheets.add(javaClass.getResource("/styles.css").toExternalForm())

                        // Configure primary stage
                        primaryStage.initStyle(StageStyle.DECORATED)
                        primaryStage.scene = scene
                        primaryStage.title = "Blossom ERP"

                        val image = Image("/icons/icon.png")
                        primaryStage.icons.add(image)

                        loadIcons(primaryStage)

                        primaryStage.minWidth = 800.0
                        primaryStage.minHeight = 600.0

                        // Show the window but keep it invisible
                        primaryStage.opacity = 0.0
                        primaryStage.show()

                        Thread {
                            // Apply styling to the now-present window
                            WindowsStyler.applyStyling(primaryStage, 0xFF312D2B.toInt())

                            // Após a estilização, voltar para a thread da UI para fazer transição
                            Platform.runLater {
                                // Use animation timeline instead of sleep
                                javafx.animation.PauseTransition(javafx.util.Duration.millis(300.0)).apply {
                                    setOnFinished {
                                        // Then hide splash and make window visible
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
            // Carregar o ícone principal para o executável e barra de tarefas
            val icoStream = javaClass.getResourceAsStream("/icons/icon.ico")
            if (icoStream != null) {
                try {
                    val image = Image(icoStream)
                    stage.icons.add(image)
                } finally {
                    icoStream.close()
                }
            }

            // Adicionar múltiplas resoluções para diferentes situações
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

            // Se nenhum ícone específico foi encontrado, tente um genérico
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

            // Verificar se o ícone foi carregado
            println("Ícones carregados: ${stage.icons.size}")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Falha ao carregar ícones: ${e.message}")
        }
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
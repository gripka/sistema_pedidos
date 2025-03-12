package com.sistema_pedidos

import com.sistema_pedidos.database.DatabaseHelper
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
import java.lang.management.ManagementFactory

class Main : Application() {
    override fun start(primaryStage: Stage) {
        // Initialize the single instance lock with our stage
        if (!SingleInstanceLock.initialize(primaryStage)) {
            // Another instance is already running and was notified to focus
            Platform.exit()
            return
        }

        setProcessName("Blossom ERP")

        Runtime.getRuntime().addShutdownHook(Thread {
            SingleInstanceLock.release()
        })

        DatabaseHelper()
        val dbHelper = DatabaseHelper()

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

        val tables = dbHelper.listTables()
        println("Tabelas no banco de dados: $tables")

        val titleBarView = TitleBarView(primaryStage)

        mainView.setTopTitleBar(titleBarView)
        mainView.setLeftMenu(menuView)
        mainView.setCenterView(dashboardView)

        val scene = Scene(mainView, 1000.0, 680.0, Color.TRANSPARENT)
        scene.stylesheets.add(javaClass.getResource("/styles.css").toExternalForm())
        primaryStage.initStyle(StageStyle.TRANSPARENT)
        primaryStage.scene = scene
        primaryStage.title = "Blossom ERP"

        primaryStage.icons.add(Image("icons/icon.ico"))

        primaryStage.minWidth = 800.0
        primaryStage.minHeight = 600.0

        primaryStage.maximizedProperty().addListener { _, _, maximized ->
            if (maximized) {
                val screenBounds: Rectangle2D = Screen.getPrimary().visualBounds
                primaryStage.x = screenBounds.minX
                primaryStage.width = screenBounds.width
                primaryStage.height = screenBounds.height
            }
        }

        ResizableWindow(primaryStage)

        primaryStage.setOnCloseRequest {
            SingleInstanceLock.release()
        }

        primaryStage.show()
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
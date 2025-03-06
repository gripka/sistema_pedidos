package com.sistema_pedidos

import com.sistema_pedidos.database.DatabaseHelper
import javafx.application.Application
import javafx.geometry.Rectangle2D
import javafx.scene.Scene
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import com.sistema_pedidos.view.*
import javafx.scene.paint.Color
import javafx.scene.image.Image

class Main : Application() {
    override fun start(primaryStage: Stage) {
        // Inicializa o banco de dados
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
        val menuView = MenuView { viewName ->
            when (viewName) {
                "dashboard" -> mainView.setCenterView(dashboardView)
                "novoPedido" -> mainView.setCenterView(novoPedidoView)
                "pedidosAndamento" -> mainView.setCenterView(pedidosEmAndamentoView)
                "produtos" -> mainView.setCenterView(produtosView)
                "historicoPedidos" -> mainView.setCenterView(historicoPedidosView)
                "configuracoes" -> mainView.setCenterView(configuracoesView)
                "clientes" -> mainView.setCenterView(clientesView)
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
        primaryStage.title = "Sistema de Pedidos"

        primaryStage.icons.add(Image("icons/icon.png"))

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

        primaryStage.show()
    }
}

fun main() {
    Application.launch(Main::class.java)
}
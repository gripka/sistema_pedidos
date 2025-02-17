package com.sistema_pedidos

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
        val mainView = MainView(primaryStage)
        val novoPedidoView = NovoPedidoView()
        val produtosView = ProdutosView()
        val historicoPedidosView = HistoricoPedidosView()
        val configuracoesView = ConfiguracoesView()
        val menuView = MenuView { viewName ->
            when (viewName) {
                "novoPedido" -> mainView.setCenterView(novoPedidoView)
                "produtos" -> mainView.setCenterView(produtosView)
                "historicoPedidos" -> mainView.setCenterView(historicoPedidosView)
                "configuracoes" -> mainView.setCenterView(configuracoesView)
            }
        }
        val titleBarView = TitleBarView(primaryStage)

        // Set the initial view, side menu, and title bar
        mainView.setTopTitleBar(titleBarView)
        mainView.setLeftMenu(menuView)
        mainView.setCenterView(novoPedidoView)

        val scene = Scene(mainView, 1000.0, 680.0, Color.TRANSPARENT)
        scene.stylesheets.add(javaClass.getResource("/styles.css").toExternalForm())
        primaryStage.initStyle(StageStyle.TRANSPARENT)
        primaryStage.scene = scene
        primaryStage.title = "Sistema de Pedidos"

        // Set the application icon
        primaryStage.icons.add(Image("icons/icon.png"))

        // Set minimum width and height for the window
        primaryStage.minWidth = 800.0
        primaryStage.minHeight = 600.0

        // Adjust bounds when maximized to not overlap the taskbar
        primaryStage.maximizedProperty().addListener { _, _, maximized ->
            if (maximized) {
                val screenBounds: Rectangle2D = Screen.getPrimary().visualBounds
                primaryStage.x = screenBounds.minX
                primaryStage.width = screenBounds.width
                primaryStage.height = screenBounds.height
            }
        }

        // Make the window resizable from any border
        ResizableWindow(primaryStage)

        primaryStage.show()
    }
}

fun main() {
    Application.launch(Main::class.java)
}
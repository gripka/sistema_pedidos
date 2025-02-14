package com.sistema_pedidos

import javafx.application.Application
import javafx.geometry.Rectangle2D
import javafx.scene.Scene
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import com.sistema_pedidos.view.MainView
import com.sistema_pedidos.view.NovoPedidoView
import com.sistema_pedidos.view.MenuView
import com.sistema_pedidos.view.TitleBarView
import javafx.scene.paint.Color
import javafx.scene.image.Image

class Main : Application() {
    override fun start(primaryStage: Stage) {
        val mainView = MainView(primaryStage)
        val novoPedidoView = NovoPedidoView()
        val menuView = MenuView()
        val titleBarView = TitleBarView(primaryStage)

        // Set the initial view, side menu, and title bar
        mainView.setCenterView(novoPedidoView)
        mainView.setLeftMenu(menuView)
        mainView.setTopTitleBar(titleBarView)

        val scene = Scene(mainView, 800.0, 600.0, Color.TRANSPARENT)
        scene.stylesheets.add(javaClass.getResource("/styles.css").toExternalForm())
        primaryStage.initStyle(StageStyle.TRANSPARENT)
        primaryStage.scene = scene
        primaryStage.title = "Sistema de Pedidos"

        // Set the application icon
        primaryStage.icons.add(Image("icons/icon.png"))


        // Adjust bounds when maximized to not overlap the taskbar
        primaryStage.maximizedProperty().addListener { _, _, maximized ->
            if (maximized) {
                val screenBounds: Rectangle2D = Screen.getPrimary().visualBounds
                primaryStage.x = screenBounds.minX
                primaryStage.y = screenBounds.minY
                primaryStage.width = screenBounds.width
                primaryStage.height = screenBounds.height
            }
        }

        primaryStage.show()
    }
}

fun main() {
    Application.launch(Main::class.java)
}
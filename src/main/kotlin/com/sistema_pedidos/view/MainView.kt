package com.sistema_pedidos.view

import javafx.application.Application
import javafx.beans.value.ObservableValue
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.ScrollPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Stage

class MainView(private val stage: Stage) : BorderPane() {
    private val scrollPane = ScrollPane()

    init {
        val cornerRadii = CornerRadii(15.0, 15.0, 0.0, 0.0, false)
        background = Background(BackgroundFill(Color.web("#F7F7F7"), cornerRadii, null))

        // Configure ScrollPane
        scrollPane.apply {
            isFitToWidth = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
            styleClass.add("main-scroll-pane")
        }

        val clipRectangle = Rectangle().apply {
            arcWidth = 15.0
            arcHeight = 15.0
            widthProperty().bind(this@MainView.widthProperty())
            heightProperty().bind(this@MainView.heightProperty())
        }
        clip = clipRectangle

        stage.maximizedProperty().addListener { _: ObservableValue<out Boolean>, _: Boolean, maximized: Boolean ->
            if (maximized) {
                background = Background(BackgroundFill(Color.web("#F7F7F7"), CornerRadii.EMPTY, null))
                clip = null
            } else {
                background = Background(BackgroundFill(Color.web("#F7F7F7"), cornerRadii, null))
                clip = clipRectangle
            }
        }

        stage.isResizable = true
        stage.minWidth = 400.0
        stage.minHeight = 300.0
        styleClass.add("window-border")
    }

    fun setCenterView(view: javafx.scene.Node) {
        scrollPane.content = view
        center = scrollPane
    }

    fun setLeftMenu(menu: javafx.scene.Node) {
        left = menu
    }

    fun setTopTitleBar(titleBar: javafx.scene.Node) {
        top = titleBar
    }
}

class MainApp : Application() {
    override fun start(primaryStage: Stage) {
        val mainView = MainView(primaryStage)
        val scene = Scene(mainView, 780.0, 600.0)
        primaryStage.scene = scene
        primaryStage.show()
    }
}

fun main() {
    Application.launch(MainApp::class.java)
}
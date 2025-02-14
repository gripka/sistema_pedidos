package com.sistema_pedidos.view

import javafx.application.Application
import javafx.beans.value.ObservableValue
import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Stage

class MainView(private val stage: Stage) : BorderPane() {
    init {
        // Define the background color as white with rounded corners
        val cornerRadii = CornerRadii(15.0, 15.0, 0.0, 0.0, false)
        background = Background(BackgroundFill(Color.WHITE, cornerRadii, null))

        // Apply a clip to ensure the rounded corners are visible
        val clipRectangle = Rectangle().apply {
            arcWidth = 15.0
            arcHeight = 15.0
            widthProperty().bind(this@MainView.widthProperty())
            heightProperty().bind(this@MainView.heightProperty())
        }
        clip = clipRectangle

        // Add a listener to the stage's maximized property
        stage.maximizedProperty().addListener { _: ObservableValue<out Boolean>, _: Boolean, maximized: Boolean ->
            if (maximized) {
                // Remove rounded corners when maximized
                background = Background(BackgroundFill(Color.WHITE, CornerRadii.EMPTY, null))
                clip = null
            } else {
                // Add rounded corners when not maximized
                background = Background(BackgroundFill(Color.WHITE, cornerRadii, null))
                clip = clipRectangle
            }
        }

        // Allow resizing from the corners
        stage.isResizable = true

        // Set minimum width and height for the window
        stage.minWidth = 400.0
        stage.minHeight = 300.0
    }

    fun setCenterView(view: javafx.scene.Node) {
        center = view
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
        val scene = Scene(mainView, 800.0, 600.0)
        primaryStage.scene = scene
        primaryStage.show()
    }
}

fun main() {
    Application.launch(MainApp::class.java)
}
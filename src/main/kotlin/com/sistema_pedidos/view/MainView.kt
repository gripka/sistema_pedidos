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
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Stage

class MainView(private val stage: Stage) : BorderPane() {
    private val scrollPane = ScrollPane()
    private val contentContainer = VBox()

    init {
        val cornerRadii = CornerRadii(15.0, 15.0, 0.0, 0.0, false)
        background = Background(BackgroundFill(Color.web("#F7F7F7"), cornerRadii, null))

        scrollPane.apply {
            isFitToWidth = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
            styleClass.add("main-scroll-pane")
            content = contentContainer
            center = scrollPane
        }

        contentContainer.apply {
            maxWidthProperty().bind(scrollPane.widthProperty().subtract(12.0))
            prefWidthProperty().bind(maxWidthProperty())
        }

        center = scrollPane

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
        // Se o node atual for um Closeable, feche-o antes de substituir
        val currentView = center
        if (currentView is AutoCloseable) {
            try {
                currentView.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        center = view
        bottom = null

        if (view is Refreshable) {
            view.refresh()
        }
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
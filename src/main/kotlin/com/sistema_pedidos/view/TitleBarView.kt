package com.sistema_pedidos.view

import javafx.geometry.Pos
import javafx.scene.layout.HBox
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.geometry.Insets
import javafx.stage.Stage
import javafx.application.Platform

class TitleBarView(private val stage: Stage) : HBox() {
    private var xOffset = 0.0
    private var yOffset = 0.0
    private val maximizeIcon = ImageView(Image("/icons/maximize.png"))
    private val shrinkIcon = ImageView(Image("/icons/shrink.png"))

    init {
        background = Background(BackgroundFill(Color.web("#2B2D31"), CornerRadii.EMPTY, Insets.EMPTY))
        prefHeight = 35.0
        alignment = Pos.CENTER_LEFT
        styleClass.add("title-bar")

        val logoContainer = HBox().apply {
            prefWidth = 40.0
            alignment = Pos.CENTER
            padding = Insets(0.0, 0.0, 0.0, 22.0)
            children.add(ImageView(Image("/icons/logo.png")).apply {
                fitHeight = 27.0
                fitWidth = 27.0
            })
            setOnMousePressed { event: MouseEvent ->
                xOffset = event.sceneX
                yOffset = event.sceneY
            }
            setOnMouseDragged { event: MouseEvent ->
                stage.x = event.screenX - xOffset
                stage.y = event.screenY - yOffset
            }
        }

        // Add a label or other components to the title bar
        val titleLabel = Label("Blossom ERP\n").apply {
            textFill = Color.WHITE
            padding = Insets(0.0, 0.0, 0.0, 10.0) // Move the title slightly to the right
            setOnMousePressed { event: MouseEvent ->
                xOffset = event.sceneX
                yOffset = event.sceneY
            }
            setOnMouseDragged { event: MouseEvent ->
                stage.x = event.screenX - xOffset
                stage.y = event.screenY - yOffset
            }
        }

        // Window control buttons
        val buttonSize = 16.0
        val buttonContainer = HBox(0.0).apply {
            alignment = Pos.CENTER_RIGHT
            children.addAll(
                createButton("/icons/minimize.png", buttonSize) {
                    stage.isIconified = true
                },
                createButton(maximizeIcon, shrinkIcon, buttonSize) {
                    stage.isMaximized = !stage.isMaximized
                },
                createButton("/icons/close.png", buttonSize, true) {
                    stage.close()
                }
            )
        }

        // Draggable area for moving the window
        val draggableArea = HBox().apply {
            alignment = Pos.CENTER_LEFT
            HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
            setOnMousePressed { event: MouseEvent ->
                xOffset = event.sceneX
                yOffset = event.sceneY
            }
            setOnMouseDragged { event: MouseEvent ->
                stage.x = event.screenX - xOffset
                stage.y = event.screenY - yOffset
            }
        }

        // Add components to the title bar
        children.addAll(logoContainer, titleLabel, draggableArea, buttonContainer)

        // Maximize the window when double-clicking the draggable area
        draggableArea.setOnMouseClicked { event: MouseEvent ->
            if (event.clickCount == 2) {
                stage.isMaximized = !stage.isMaximized
            }
        }

        // Listener to change the maximize icon to shrink icon
        stage.maximizedProperty().addListener { _, _, maximized ->
            if (maximized) {
                maximizeIcon.image = Image("/icons/shrink.png")
            } else {
                maximizeIcon.image = Image("/icons/maximize.png")
            }
        }
    }

    private fun createButton(iconPath: String, iconSize: Double, isCloseButton: Boolean = false, action: () -> Unit): HBox {
        val button = HBox().apply {
            prefWidth = 40.0
            prefHeight = 35.0
            alignment = Pos.CENTER
            children.add(ImageView(Image(iconPath)).apply {
                fitWidth = iconSize
                fitHeight = iconSize
            })
            setOnMouseClicked { action() }
            styleClass.add(if (isCloseButton) "close-button" else "control-button")
        }
        return button
    }

    private fun createButton(maximizeIcon: ImageView, shrinkIcon: ImageView, iconSize: Double, action: () -> Unit): HBox {
        val button = HBox().apply {
            prefWidth = 40.0
            prefHeight = 35.0
            alignment = Pos.CENTER
            children.add(maximizeIcon.apply {
                fitWidth = iconSize
                fitHeight = iconSize
            })
            setOnMouseClicked { action() }
            styleClass.add("control-button")
        }
        return button
    }
}
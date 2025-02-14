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

class TitleBarView(private val stage: Stage) : HBox() {
    private var xOffset = 0.0
    private var yOffset = 0.0
    private val maximizeIcon = ImageView(Image("/icons/maximize.png"))
    private val shrinkIcon = ImageView(Image("/icons/shrink.png"))

    init {
        // Defina a cor de fundo da barra de títulos
        background = Background(BackgroundFill(Color.web("#333333"), CornerRadii.EMPTY, Insets.EMPTY))
        prefHeight = 35.0
        alignment = Pos.CENTER_LEFT

        // Adicione um contêiner para o ícone de logo
        val logoContainer = HBox().apply {
            prefWidth = 40.0
            alignment = Pos.CENTER
            children.add(ImageView(Image("/icons/logo.png")).apply {
                fitHeight = 27.0
                fitWidth = 27.0
            })
        }

        // Adicione um rótulo ou outros componentes à barra de títulos
        val titleLabel = Label("Sistema de Pedidos").apply {
            textFill = Color.WHITE
            padding = Insets(0.0, 0.0, 0.0, 10.0) // Move the title slightly to the right
        }

        // Botões de controle da janela
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

        // Adicione os componentes à barra de título
        children.addAll(logoContainer, titleLabel, HBox().apply {
            alignment = Pos.CENTER_RIGHT
            children.add(buttonContainer)
            HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
        })

        // Permitir arrastar a janela
        setOnMousePressed { event: MouseEvent ->
            xOffset = event.sceneX
            yOffset = event.sceneY
        }

        setOnMouseDragged { event: MouseEvent ->
            stage.x = event.screenX - xOffset
            stage.y = event.screenY - yOffset
        }

        // Maximizar a janela ao clicar duas vezes na barra de título
        setOnMouseClicked { event: MouseEvent ->
            if (event.clickCount == 2) {
                stage.isMaximized = !stage.isMaximized
            }
        }

        // Listener para mudar o ícone de maximize para shrink
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
package com.sistema_pedidos.view

import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color

class MenuView : VBox() {
    private val isExpanded = SimpleBooleanProperty(false)
    private var selectedButton: Button? = null

    init {
        background = Background(BackgroundFill(Color.web("#333333"), CornerRadii.EMPTY, Insets.EMPTY))
        prefWidth = 50.0
        val toggleButton = Button().apply {
            graphic = ImageView(Image("/icons/menu.png")).apply {
                fitHeight = 20.0
                fitWidth = 20.0
            }
            style = "-fx-background-color: transparent; -fx-text-fill: white;" // Remove o fundo branco
            setOnAction {
                isExpanded.set(!isExpanded.get())
            }
            setOnMouseEntered { style = "-fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: white;" }
            setOnMouseExited { style = "-fx-background-color: transparent; -fx-text-fill: white;" }
        }
        children.add(toggleButton)

        val buttonContainer = VBox().apply {
            alignment = Pos.CENTER
            spacing = 10.0
        }

        val homeButton = createMenuButton("Home", "/icons/error.png")
        val newOrderButton = createMenuButton("Novo Pedido", "/icons/novopedido.png")
        val ordersButton = createMenuButton("Pedidos", "/icons/pedidos.png")
        buttonContainer.children.addAll(homeButton, newOrderButton, ordersButton)
        children.add(buttonContainer)

        // Add a spacer to push the settings button to the bottom
        val spacer = VBox()
        VBox.setVgrow(spacer, Priority.ALWAYS)
        children.add(spacer)

        val settingsButton = createMenuButton("Configurações", "/icons/config.png")
        VBox.setMargin(settingsButton, Insets(10.0, 0.0, 10.0, 0.0))
        children.add(settingsButton)

        isExpanded.addListener { _, _, expanded ->
            prefWidth = if (expanded) 200.0 else 50.0
            updateButtonLayout(homeButton, expanded)
            updateButtonLayout(newOrderButton, expanded)
            updateButtonLayout(ordersButton, expanded)
            updateButtonLayout(settingsButton, expanded)
        }
    }

    private fun createMenuButton(text: String, iconPath: String): Button {
        val btn = Button(text).apply {
            graphic = ImageView(Image(iconPath)).apply {
                fitHeight = 30.0
                fitWidth = 30.0
            }
            maxWidth = Double.MAX_VALUE
            style = "-fx-background-color: transparent; -fx-text-fill: white;"
            setOnMouseEntered { if (this != selectedButton) style = "-fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: white;" }
            setOnMouseExited { if (this != selectedButton) style = "-fx-background-color: transparent; -fx-text-fill: white;" }
            setOnAction {
                selectedButton?.style = "-fx-background-color: transparent; -fx-text-fill: white;"
                selectedButton = this
                style = "-fx-background-color: #777777; -fx-text-fill: white;"
            }
        }
        return btn
    }

    private fun updateButtonLayout(button: Button, expanded: Boolean) {
        button.text = if (expanded) button.text else ""
        button.graphicTextGap = if (expanded) 10.0 else 0.0
    }
}
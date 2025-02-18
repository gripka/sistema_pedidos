package com.sistema_pedidos.view

import javafx.animation.Transition
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.util.Duration

class MenuView(private val onNavigate: (String) -> Unit) : VBox() {
    private var selectedButton: Button? = null
    private var isExpanded = false

    init {
        background = Background(BackgroundFill(Color.web("#2B2D31"), CornerRadii.EMPTY, Insets.EMPTY))
        prefWidth = 40.0
        padding = Insets(1.0)
        styleClass.addAll("menu-right-border", "menu-left-bottom-border") // Add the new CSS classes here

        // Top section (Fixed Button)
        val topButton = createTopButton("/icons/menu.png") { toggleMenu() }
        val topSection = HBox(topButton).apply {
            alignment = Pos.TOP_LEFT
            padding = Insets(10.0, 0.0, 10.0, 17.0)
            style = "-fx-border-color: #212121; -fx-border-width: 0 1px 0 0;"
        }
        children.add(topSection)


        val sectionsContainer = VBox().apply {
            alignment = Pos.CENTER
            spacing = 10.0
            padding = Insets(10.0, 0.0, 10.0, 0.0)
            style = "-fx-border-color: #212121; -fx-border-width: 0 1px 0 0;"
        }

        val homeButton = createMenuButton("/icons/error.png", "Produtos") { onNavigate("produtos") }
        val newOrderButton = createMenuButton("/icons/novopedido.png", "Novo Pedido") { onNavigate("novoPedido") }
        val ordersButton = createMenuButton("/icons/pedidos.png", "Histórico de Pedidos") { onNavigate("historicoPedidos") }
        sectionsContainer.children.addAll(newOrderButton, ordersButton, homeButton)
        VBox.setVgrow(sectionsContainer, Priority.ALWAYS)
        children.add(sectionsContainer)

        // Bottom section
        val bottomSection = VBox().apply {
            alignment = Pos.BOTTOM_CENTER
            spacing = 10.0
            padding = Insets(0.0, 0.0, 20.0, 0.0) // Add bottom padding
            style = "-fx-border-color: #212121; -fx-border-width: 0 1px 0 0;"

        }
        val settingsButton = createMenuButton("/icons/config.png", "Configurações") { onNavigate("configuracoes") }
        bottomSection.children.add(settingsButton)
        children.add(bottomSection)
    }

    private fun createTopButton(iconPath: String, onClick: () -> Unit): Button {
        return Button().apply {
            graphic = ImageView(Image(iconPath)).apply {
                fitHeight = 23.0
                fitWidth = 23.0
            }
            prefWidth = 30.0
            style = "-fx-background-color: transparent; -fx-border-radius: 10px;"
            setOnMouseEntered { style = "-fx-background-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 10px;" }
            setOnMouseExited { style = "-fx-background-color: transparent; -fx-border-radius: 10px;" }
            setOnAction { onClick() }
        }
    }

    private fun createMenuButton(iconPath: String, buttonText: String, onClick: () -> Unit): HBox {
        val btn = Button().apply {
            graphic = ImageView(Image(iconPath)).apply {
                fitHeight = 23.0
                fitWidth = 23.0
            }
            prefWidth = 30.0
            style = "-fx-background-color: transparent; -fx-border-radius: 10px;"
            tooltip = Tooltip(buttonText)
            setOnAction {
                selectedButton?.parent?.style = "-fx-background-color: transparent; -fx-border-radius: 10px;"
                selectedButton = this
                (parent as HBox).style = "-fx-background-color: #777777; -fx-border-radius: 10px;"
                onClick()
            }
            setOnMousePressed {
                (parent as HBox).style = "-fx-background-color: rgba(200, 200, 200, 0.3); -fx-border-radius: 10px;"
            }
            setOnMouseReleased {
                if (this == selectedButton) {
                    (parent as HBox).style = "-fx-background-color: #777777; -fx-border-radius: 10px;"
                } else {
                    (parent as HBox).style = "-fx-background-color: transparent; -fx-border-radius: 10px;"
                }
            }
        }
        val label = Label(buttonText).apply {
            textFill = Color.web("#F5F5F5")
            isVisible = isExpanded
        }
        val hbox = HBox(btn, label).apply {
            alignment = Pos.CENTER_LEFT
            spacing = 10.0
            padding = Insets(10.0, 0.0, 10.0, 17.0) // Adjust padding to center
            setOnMouseEntered {
                if (btn != selectedButton) {
                    style = "-fx-background-color: rgba(200, 200, 200, 0.1); -fx-border-radius: 10px;" // Change hover color
                }
            }
            setOnMouseExited {
                if (btn != selectedButton) {
                    style = "-fx-background-color: transparent; -fx-border-radius: 10px;"
                }
            }
            setOnMouseClicked { btn.fire() }
        }
        return hbox
    }

    private fun toggleMenu() {
        val targetWidth = if (isExpanded) 40.0 else 230.0 // Adjust the closed menu width to 40.0
        val transition = object : Transition() {
            init {
                cycleDuration = Duration.millis(300.0)
            }

            override fun interpolate(frac: Double) {
                prefWidth = 40.0 + (targetWidth - 40.0) * frac
            }
        }

        children.filterIsInstance<VBox>().forEach { section ->
            section.children.filterIsInstance<HBox>().forEach { hbox ->
                val label = hbox.children[1] as Label
                label.isVisible = !isExpanded
            }
        }

        transition.play()
        isExpanded = !isExpanded
    }
}
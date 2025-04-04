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
    private val viewButtonMap = mutableMapOf<String, Button>()
    private var isAnimating = false

    init {
        background = Background(BackgroundFill(Color.web("#202020"), CornerRadii.EMPTY, Insets.EMPTY))
        prefWidth = 40.0
        padding = Insets(1.0)
        styleClass.addAll("menu-right-border", "menu-straight-border")
        style = "-fx-background-color: #202020; -fx-background-radius: 0; -fx-border-radius: 0;"

        val topButton = createTopButton("/icons/menu.png") {
            if (!isAnimating) { // Only toggle if not animating
                toggleMenu()
            }
        }
        val topSection = HBox(topButton).apply {
            alignment = Pos.TOP_LEFT
            padding = Insets(10.0, 0.0, 10.0, 17.0)
            style = "-fx-background-color: #202020; -fx-border-color: #212121; -fx-border-width: 0 1px 0 0; -fx-background-radius: 0; -fx-border-radius: 0;"
        }
        children.add(topSection)

        val sectionsContainer = VBox().apply {
            alignment = Pos.CENTER
            spacing = 10.0
            padding = Insets(10.0, 0.0, 10.0, 0.0)
            style = "-fx-background-color: #202020; -fx-border-color: #212121; -fx-border-width: 0 1px 0 0; -fx-background-radius: 0; -fx-border-radius: 0;"
        }

        val homeButton = createMenuButton("/icons/produtos.png", "Produtos", "produtos")
        val dashboardButton = createMenuButton("/icons/dashboard.png", "Dashboard", "dashboard")
        val newOrderButton = createMenuButton("/icons/novopedido.png", "Novo Pedido", "wizard")
        val pedidosEmAndamentoButton = createMenuButton("/icons/pedidos.png", "Pedidos em Andamento", "pedidosAndamento")
        val ordersButton = createMenuButton("/icons/historicopedidos.png", "Histórico de Pedidos", "historicoPedidos")
        val clienteButton = createMenuButton("/icons/cliente.png", "Clientes", "clientes")

        sectionsContainer.children.addAll(dashboardButton, newOrderButton, pedidosEmAndamentoButton, ordersButton, homeButton, clienteButton)
        VBox.setVgrow(sectionsContainer, Priority.ALWAYS)
        children.add(sectionsContainer)

        val bottomSection = VBox().apply {
            alignment = Pos.BOTTOM_CENTER
            spacing = 10.0
            padding = Insets(0.0, 0.0, 20.0, 0.0)
            style = "-fx-background-color: #202020; -fx-border-color: #212121; -fx-border-width: 0 1px 0 0; -fx-background-radius: 0; -fx-border-radius: 0;"
        }
        val settingsButton = createMenuButton("/icons/config.png", "Configurações", "configuracoes")
        bottomSection.children.add(settingsButton)
        children.add(bottomSection)

        selectView("wizard")
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

    private fun createMenuButton(iconPath: String, buttonText: String, viewName: String): HBox {
        val btn = Button().apply {
            graphic = ImageView(Image(iconPath)).apply {
                fitHeight = 23.0
                fitWidth = 23.0
            }
            prefWidth = 30.0
            style = "-fx-background-color: transparent; -fx-border-radius: 10px;"
            tooltip = Tooltip(buttonText)
            setOnAction {
                selectButton(this)
                onNavigate(viewName)
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

        viewButtonMap[viewName] = btn

        val label = Label(buttonText).apply {
            textFill = Color.web("#F5F5F5")
            isVisible = isExpanded
        }
        val hbox = HBox(btn, label).apply {
            alignment = Pos.CENTER_LEFT
            spacing = 10.0
            padding = Insets(10.0, 0.0, 10.0, 17.0)
            setOnMouseEntered {
                if (btn != selectedButton) {
                    style = "-fx-background-color: rgba(200, 200, 200, 0.1); -fx-border-radius: 10px;"
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

    private fun selectButton(button: Button) {
        selectedButton?.parent?.style = "-fx-background-color: transparent; -fx-border-radius: 10px;"
        selectedButton = button
        (button.parent as HBox).style = "-fx-background-color: #777777; -fx-border-radius: 10px;"
    }

    fun selectView(viewName: String) {
        viewButtonMap[viewName]?.let { button ->
            selectButton(button)
        }
    }

    private fun toggleMenu() {
        if (isAnimating) return
        isAnimating = true

        val startWidth = prefWidth
        val endWidth = if (isExpanded) 40.0 else 230.0

        val transition = object : Transition() {
            init {
                cycleDuration = Duration.millis(300.0)
            }

            override fun interpolate(frac: Double) {
                prefWidth = startWidth + (endWidth - startWidth) * frac
            }
        }

        transition.setOnFinished {
            children.filterIsInstance<VBox>().forEach { section ->
                section.children.filterIsInstance<HBox>().forEach { hbox ->
                    val label = hbox.children[1] as Label
                    label.isVisible = !isExpanded
                }
            }
            isExpanded = !isExpanded
            isAnimating = false
        }
        transition.play()
    }
}
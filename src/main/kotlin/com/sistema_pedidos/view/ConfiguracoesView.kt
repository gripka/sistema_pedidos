package com.sistema_pedidos.view

import com.sistema_pedidos.controller.ConfiguracoesController
import javafx.scene.layout.VBox
import javafx.scene.control.Label
import javafx.geometry.Insets
import javafx.scene.layout.GridPane
import javafx.scene.text.Font
import javafx.scene.text.FontWeight

class ConfiguracoesView : VBox() {
    private val controller = ConfiguracoesController()

    init {
        spacing = 20.0
        padding = Insets(20.0)

        // Título
        val titleLabel = Label("Configurações")
        titleLabel.font = Font.font("System", FontWeight.BOLD, 18.0)

        // Informações do aplicativo
        val infoGrid = GridPane()
        infoGrid.hgap = 10.0
        infoGrid.vgap = 8.0
        infoGrid.padding = Insets(10.0)

        infoGrid.add(Label("Nome:"), 0, 0)
        infoGrid.add(Label(controller.appName), 1, 0)

        infoGrid.add(Label("Versão:"), 0, 1)
        infoGrid.add(Label(controller.appVersion), 1, 1)

        children.addAll(titleLabel, infoGrid)
    }
}
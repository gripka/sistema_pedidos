package com.sistema_pedidos.view

import com.sistema_pedidos.controller.ConfiguracoesController
import com.sistema_pedidos.util.VersionChecker
import javafx.scene.layout.VBox
import javafx.scene.control.Label
import javafx.geometry.Insets
import javafx.scene.layout.GridPane
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.control.ProgressIndicator
import javafx.application.Platform
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority

class ConfiguracoesView : VBox() {
    private val controller = ConfiguracoesController()
    private val versionChecker = VersionChecker()
    private val updateDateLabel = Label("Carregando...")

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

        infoGrid.add(Label("Última atualização:"), 0, 2)
        infoGrid.add(updateDateLabel, 1, 2)

        children.addAll(titleLabel, infoGrid)

        // Fetch update date in background
        val spinner = ProgressIndicator(-1.0)
        spinner.maxHeight = 20.0
        spinner.maxWidth = 20.0

        val loadingBox = HBox(5.0, updateDateLabel, spinner)
        infoGrid.add(loadingBox, 1, 2)

        Thread {
            try {
                val (_, _, releaseDate) = versionChecker.getLatestReleaseInfo()
                Platform.runLater {
                    loadingBox.children.remove(spinner)
                    updateDateLabel.text = releaseDate ?: "Não disponível"
                }
            } catch (e: Exception) {
                Platform.runLater {
                    loadingBox.children.remove(spinner)
                    updateDateLabel.text = "Não disponível"
                }
            }
        }.start()
    }
}
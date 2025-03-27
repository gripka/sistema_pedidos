package com.sistema_pedidos.view

import com.sistema_pedidos.controller.ConfiguracoesController
import com.sistema_pedidos.controller.PrinterController
import com.sistema_pedidos.util.VersionChecker
import javafx.scene.layout.VBox
import javafx.scene.control.*
import javafx.geometry.Insets
import javafx.scene.layout.GridPane
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.application.Platform
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.stage.DirectoryChooser
import javafx.geometry.Pos
import javafx.stage.Stage
import java.io.File
import javax.print.PrintServiceLookup

class ConfiguracoesView(stage: Stage) : MainView(stage), Refreshable {
    private val controller = ConfiguracoesController()
    private val versionChecker = VersionChecker()
    private val updateDateLabel = Label("Carregando...")
    private lateinit var tabPane: TabPane
    private lateinit var cnpjField: TextField

    init {
        val contentView = createContentView()
        setCenterView(contentView)
    }

    private fun createContentView(): VBox {
        val container = VBox()
        container.stylesheets.add(javaClass.getResource("/configuracoes.css").toExternalForm())
        container.spacing = 15.0
        container.padding = Insets(0.0)
        container.style = "-fx-background-color: transparent;"
        styleClass.add("main-container")

        // Título principal
        val titleLabel = Label("Configurações")
        titleLabel.font = Font.font("System", FontWeight.BOLD, 22.0)
        titleLabel.style = "-fx-text-fill: #2B2D31;"
        titleLabel.padding = Insets(20.0, 20.0, 10.0, 20.0)

        // Create TabPane with flat modern styling
        tabPane = TabPane()
        tabPane.tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
        tabPane.style = """
            -fx-background-color: transparent;
            -fx-tab-min-width: 100px;
            -fx-tab-max-width: 100px;
            -fx-tab-min-height: 40px;
            -fx-border-width: 0 0 1 0;
            -fx-border-color: #E5E5E5;
        """

        // Create tabs
        val generalTab = Tab("Geral")
        generalTab.content = createGeneralTab()
        generalTab.style = "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;"

        val backupTab = Tab("Backup")
        backupTab.content = createBackupTab()
        backupTab.style = "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;"

        val sobreTab = Tab("Sobre")
        sobreTab.content = createSobreTab()
        sobreTab.style = "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;"

        val printerTab = Tab("Impressão")
        printerTab.content = createPrinterTab()
        printerTab.style = "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;"

        tabPane.tabs.addAll(generalTab, backupTab, printerTab, sobreTab)

        // Add a spacer region
        val spacer = Region()
        VBox.setVgrow(spacer, Priority.ALWAYS)

        // Actions bar with buttons (moved from backup tab)
        val actionsBar = HBox()
        actionsBar.spacing = 10.0
        actionsBar.padding = Insets(10.0, 20.0, 20.0, 20.0)
        actionsBar.alignment = Pos.CENTER_RIGHT
        actionsBar.style = "-fx-background-color: transparent;"

        val saveButton = Button("Salvar Configurações")
        saveButton.style = """
            -fx-background-color: #4CAF50;
            -fx-text-fill: white;
            -fx-background-radius: 4px;
            -fx-border-radius: 4px;
            -fx-font-weight: normal;
            -fx-padding: 8px 15px;
            -fx-cursor: hand;
            -fx-effect: null;
            -fx-border-width: 0;
            -fx-border-color: transparent;
        """

        saveButton.setOnAction {
            if (controller.showSaveConfigConfirmationAlert()) {
                val backupTabContent = tabPane.tabs[1].content as VBox
                val backupCard = backupTabContent.children[0] as VBox
                val directorySection = backupCard.children[1] as VBox
                val pathBox = directorySection.children[1] as HBox
                val backupPathField = pathBox.children[0] as TextField

                val optionsSection = backupCard.children[2] as VBox
                val autoBackupCheck = optionsSection.children[1] as CheckBox
                val backupOnExitCheck = optionsSection.children[2] as CheckBox
                val keepBackupsCheck = optionsSection.children[3] as CheckBox

                val generalTabContent = tabPane.tabs[0].content as VBox
                val startupCard = generalTabContent.children[0] as VBox
                val startWithSystemCheck = startupCard.children[1] as CheckBox
                val startMinimizedCheck = startupCard.children[2] as CheckBox
                val minimizeToTrayCheck = startupCard.children[4] as CheckBox

                val printerTabContent = tabPane.tabs[2].content as VBox
                val printerCard = printerTabContent.children[0] as VBox
                val companySection = printerCard.children[1] as VBox

                // Fix: Access fields from nameAndCnpjBox using the correct hierarchy
                val nameAndCnpjBox = companySection.children[1] as HBox
                val nameVBox = nameAndCnpjBox.children[0] as VBox
                val cnpjVBox = nameAndCnpjBox.children[1] as VBox
                val nameField = nameVBox.children[1] as TextField
                val cnpjField = cnpjVBox.children[1] as TextField

                // Fix: Access phoneField and addressField with proper indexes
                val phoneField = companySection.children[3] as TextField
                val addressField = companySection.children[5] as TextField

                val printerSection = printerCard.children[2] as VBox
                val printerComboBox = printerSection.children[1] as ComboBox<String>


                controller.backupDirectory = backupPathField.text
                controller.autoBackupDaily = autoBackupCheck.isSelected
                controller.backupOnExit = backupOnExitCheck.isSelected
                controller.keepLastFiveBackups = keepBackupsCheck.isSelected

                controller.startWithSystem = startWithSystemCheck.isSelected
                controller.startMinimized = startMinimizedCheck.isSelected
                controller.minimizeToTray = minimizeToTrayCheck.isSelected

                controller.companyName = nameField.text
                controller.companyCnpj = cnpjField.text
                controller.companyPhone = phoneField.text
                controller.companyAddress = addressField.text
                controller.defaultPrinter = printerComboBox.selectionModel.selectedItem ?: ""

                if (controller.saveConfigProperties()) {
                    controller.showSaveSuccessAlert()
                } else {
                    controller.showSaveErrorAlert()
                }
            }
        }

        val resetButton = Button("Redefinir")
        resetButton.style = """
            -fx-background-color: #757575;
            -fx-text-fill: white;
            -fx-background-radius: 4px;
            -fx-border-radius: 4px;
            -fx-font-weight: normal;
            -fx-padding: 8px 15px;
            -fx-cursor: hand;
            -fx-effect: null;
            -fx-border-width: 0;
            -fx-border-color: transparent;
        """

        resetButton.setOnAction {
            if (controller.showResetConfirmationAlert()) {
                if (controller.resetToDefaults()) {
                    // Atualizar UI com valores padrão
                    // Atualizar campos da aba backup
                    val backupTabContent = tabPane.tabs[1].content as VBox
                    val backupCard = backupTabContent.children[0] as VBox
                    val directorySection = backupCard.children[1] as VBox
                    val pathBox = directorySection.children[1] as HBox
                    val backupPathField = pathBox.children[0] as TextField

                    val optionsSection = backupCard.children[2] as VBox
                    val autoBackupCheck = optionsSection.children[1] as CheckBox
                    val backupOnExitCheck = optionsSection.children[2] as CheckBox
                    val keepBackupsCheck = optionsSection.children[3] as CheckBox

                    // Atualizar campos da aba geral
                    val generalTabContent = tabPane.tabs[0].content as VBox
                    val startupCard = generalTabContent.children[0] as VBox
                    val startWithSystemCheck = startupCard.children[1] as CheckBox
                    val startMinimizedCheck = startupCard.children[2] as CheckBox

                    backupPathField.text = controller.backupDirectory
                    autoBackupCheck.isSelected = controller.autoBackupDaily
                    backupOnExitCheck.isSelected = controller.backupOnExit
                    keepBackupsCheck.isSelected = controller.keepLastFiveBackups
                    startWithSystemCheck.isSelected = controller.startWithSystem
                    startMinimizedCheck.isSelected = controller.startMinimized

                    controller.showSaveSuccessAlert()
                } else {
                    controller.showSaveErrorAlert()
                }
            }
        }

        actionsBar.children.addAll(resetButton, saveButton)

        // Adicione todos os componentes ao contêiner principal
        container.children.addAll(tabPane, spacer, actionsBar)
        VBox.setVgrow(tabPane, Priority.ALWAYS)

        applyTabPaneStyling()

        return container
    }

    private fun applyTabPaneStyling() {
        // Aplicar estilos modernos
        Platform.runLater {
            // Remove padding from header area
            tabPane.lookup(".tab-header-area")?.style = "-fx-padding: 0 0 10 0;"

            // Make tabs transparent with bottom border only for selected tab
            tabPane.lookupAll(".tab").forEach { tab ->
                tab.style = """
                    -fx-background-color: transparent;
                    -fx-background-radius: 0;
                    -fx-focus-color: transparent;
                    -fx-faint-focus-color: transparent;
                """
            }

            // Remove gray background completely
            tabPane.lookup(".tab-header-background")?.style = "-fx-background-color: transparent;"

            // Style selected tab with bottom indicator
            tabPane.lookup(".tab:selected")?.style = """
                -fx-background-color: transparent;
                -fx-background-radius: 0;
                -fx-border-width: 0 0 2 0;
                -fx-border-color: #6056e8;
                -fx-focus-color: transparent;
                -fx-faint-focus-color: transparent;
            """

            // Add hover effect to tabs
            tabPane.lookupAll(".tab").forEach { tab ->
                tab.setOnMouseEntered {
                    if (!tab.styleClass.contains("selected")) {
                        tab.style = """
                            -fx-background-color: rgba(96, 86, 232, 0.05);
                            -fx-background-radius: 0;
                            -fx-focus-color: transparent;
                            -fx-faint-focus-color: transparent;
                            -fx-border-width: 0 0 1 0;
                            -fx-border-color: rgba(96, 86, 232, 0.3);
                        """
                    }
                }

                tab.setOnMouseExited {
                    if (!tab.styleClass.contains("selected")) {
                        tab.style = """
                            -fx-background-color: transparent;
                            -fx-background-radius: 0;
                            -fx-focus-color: transparent;
                            -fx-faint-focus-color: transparent;
                        """
                    }
                }
            }

            // Style tab content area
            tabPane.lookup(".tab-content-area")?.style = "-fx-background-color: transparent;"
        }
    }

    private fun createGeneralTab(): VBox {
        val container = VBox()
        container.spacing = 20.0
        container.style = "-fx-background-color: transparent;"
        container.padding = Insets(20.0, 20.0, 20.0, 20.0)

        // Opções de inicialização
        val startupCard = VBox()
        startupCard.spacing = 15.0
        startupCard.padding = Insets(20.0)
        startupCard.style = """
        -fx-background-color: white;
        -fx-border-color: #E5E5E5;
        -fx-border-width: 1px;
        -fx-border-radius: 8px;
    """

        val startupTitle = Label("Comportamento do Sistema")
        startupTitle.font = Font.font("System", FontWeight.BOLD, 16.0)

        val startWithSystemCheck = CheckBox("Iniciar com o Windows")
        startWithSystemCheck.styleClass.add("custom-checkbox")
        startWithSystemCheck.isSelected = controller.startWithSystem

        val startMinimizedCheck = CheckBox("Iniciar em segundo plano")
        startMinimizedCheck.styleClass.add("custom-checkbox")
        startMinimizedCheck.isSelected = controller.startMinimized
        startMinimizedCheck.isDisable = !startWithSystemCheck.isSelected

        val minimizeToTrayCheck = CheckBox("Manter na bandeja ao fechar")
        minimizeToTrayCheck.styleClass.add("custom-checkbox")
        minimizeToTrayCheck.isSelected = controller.minimizeToTray ?: false

        startWithSystemCheck.selectedProperty().addListener { _, _, newValue ->
            startMinimizedCheck.isDisable = !newValue
            if (!newValue) {
                startMinimizedCheck.isSelected = false
            }
        }

        val startupDesc = Label("A inicialização automática iniciará o aplicativo quando o Windows for ligado.\n" +
                "A opção 'Iniciar em segundo plano' iniciará o aplicativo na bandeja do Windows.")
        startupDesc.style = "-fx-text-fill: #6E6E6E; -fx-font-size: 11px;"
        startupDesc.wrapTextProperty().set(true)

        val minimizeToTrayDesc = Label("A opção 'Manter na bandeja ao fechar' manterá o aplicativo em execução na bandeja do Windows ao fechar a janela.")
        minimizeToTrayDesc.style = "-fx-text-fill: #6E6E6E; -fx-font-size: 11px;"
        minimizeToTrayDesc.wrapTextProperty().set(true)

        startupCard.children.addAll(startupTitle, startWithSystemCheck, startMinimizedCheck, startupDesc, minimizeToTrayCheck, minimizeToTrayDesc)
        container.children.add(startupCard)

        return container
    }

    private fun createSobreTab(): VBox {
        val container = VBox()
        container.spacing = 20.0
        container.style = "-fx-background-color: transparent;"
        container.padding = Insets(20.0, 20.0, 20.0, 20.0)

        // Informações do aplicativo
        val infoCard = VBox()
        infoCard.spacing = 15.0
        infoCard.padding = Insets(20.0)
        infoCard.style = """
        -fx-background-color: white;
        -fx-border-color: #E5E5E5;
        -fx-border-width: 1px;
        -fx-border-radius: 8px;
    """

        // Title for info section
        val infoTitle = Label("Informações do Sistema")
        infoTitle.font = Font.font("System", FontWeight.BOLD, 16.0)

        // Create a grid layout
        val infoGrid = GridPane()
        infoGrid.hgap = 20.0
        infoGrid.vgap = 15.0
        infoGrid.padding = Insets(10.0, 0.0, 0.0, 0.0)

        // Style for labels
        val labelStyle = "-fx-text-fill: #6E6E6E;"

        // Info entries with better styling
        val nameLabel = Label("Nome:")
        nameLabel.style = labelStyle
        val nameValue = Label(controller.appName)
        nameValue.style = "-fx-font-weight: bold;"

        val versionLabel = Label("Versão:")
        versionLabel.style = labelStyle
        val versionValue = Label(controller.appVersion)
        versionValue.style = "-fx-font-weight: bold;"

        val devLabel = Label("Desenvolvido por:")
        devLabel.style = labelStyle
        val devValue = Label(" ... ")
        devValue.style = "-fx-font-weight: bold;"

        val updateLabel = Label("Última atualização:")
        updateLabel.style = labelStyle

        infoGrid.add(nameLabel, 0, 0)
        infoGrid.add(nameValue, 1, 0)

        infoGrid.add(versionLabel, 0, 1)
        infoGrid.add(versionValue, 1, 1)

        infoGrid.add(updateLabel, 0, 2)

        // Fetch update date with spinner
        val spinner = ProgressIndicator(-1.0)
        spinner.maxHeight = 16.0
        spinner.maxWidth = 16.0
        spinner.style = "-fx-progress-color: #6056e8;"

        val loadingBox = HBox(5.0, updateDateLabel, spinner)
        updateDateLabel.style = "-fx-font-weight: bold;"
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

        infoGrid.add(devLabel, 0, 3)
        infoGrid.add(devValue, 1, 3)

        infoCard.children.addAll(infoTitle, infoGrid)

        container.children.add(infoCard)

        return container
    }

    private fun createBackupTab(): VBox {
        val container = VBox()
        container.spacing = 20.0
        container.style = "-fx-background-color: transparent;"
        container.padding = Insets(20.0, 20.0, 20.0, 20.0)

        // Card for backup settings
        val backupCard = VBox()
        backupCard.spacing = 20.0
        backupCard.padding = Insets(20.0)
        backupCard.style = """
            -fx-background-color: white;
            -fx-border-color: #E5E5E5;
            -fx-border-width: 1px;
            -fx-border-radius: 8px;
        """

        // Title for backup card
        val backupTitle = Label("Configurações de Backup")
        backupTitle.font = Font.font("System", FontWeight.BOLD, 16.0)

        // Directory selection section
        val directorySection = VBox()
        directorySection.spacing = 10.0

        val directoryLabel = Label("Diretório de backup")
        directoryLabel.style = "-fx-text-fill: #6E6E6E;"

        val backupPathField = TextField(controller.backupDirectory)
        backupPathField.isEditable = false
        backupPathField.promptText = "Selecione um diretório para o backup"
        backupPathField.prefWidth = 300.0

        val chooseDirButton = Button("Selecionar")
        chooseDirButton.style = """
            -fx-background-color: #6056e8;
            -fx-text-fill: white;
            -fx-background-radius: 4px;
            -fx-border-radius: 4px;
            -fx-font-weight: normal;
            -fx-padding: 8px 15px;
            -fx-cursor: hand;
            -fx-effect: null;
            -fx-border-width: 0;
            -fx-border-color: transparent;
        """
        chooseDirButton.setOnAction {
            val directoryChooser = DirectoryChooser()
            directoryChooser.title = "Selecione o diretório para backup"

            if (backupPathField.text.isNotEmpty()) {
                val currentDir = File(backupPathField.text)
                if (currentDir.exists()) {
                    directoryChooser.initialDirectory = currentDir
                }
            }

            val selectedDir = directoryChooser.showDialog(scene.window)
            selectedDir?.let {
                backupPathField.text = it.absolutePath
            }
        }

        val pathBox = HBox(10.0, backupPathField, chooseDirButton)
        HBox.setHgrow(backupPathField, Priority.ALWAYS)
        pathBox.alignment = Pos.CENTER_LEFT

        directorySection.children.addAll(directoryLabel, pathBox)

        // Options section
        val optionsSection = VBox()
        optionsSection.spacing = 10.0
        optionsSection.padding = Insets(10.0, 0.0, 0.0, 0.0)

        val optionsLabel = Label("Opções de backup")
        optionsLabel.style = "-fx-text-fill: #6E6E6E;"

        val autoBackupCheck = CheckBox("Backup automático diário")
        autoBackupCheck.styleClass.add("custom-checkbox")
        autoBackupCheck.isSelected = controller.autoBackupDaily

        val backupOnExitCheck = CheckBox("Backup ao sair do aplicativo")
        backupOnExitCheck.styleClass.add("custom-checkbox")
        backupOnExitCheck.isSelected = controller.backupOnExit

        val keepBackupsCheck = CheckBox("Manter últimos 5 backups")
        keepBackupsCheck.styleClass.add("custom-checkbox")
        keepBackupsCheck.isSelected = controller.keepLastFiveBackups

        optionsSection.children.addAll(
            optionsLabel,
            autoBackupCheck,
            backupOnExitCheck,
            keepBackupsCheck
        )

        // Manual backup section
        val manualSection = VBox()
        manualSection.spacing = 10.0
        manualSection.padding = Insets(10.0, 0.0, 0.0, 0.0)

        val manualLabel = Label("Backup manual")
        manualLabel.style = "-fx-text-fill: #6E6E6E;"

        val backupNowButton = Button("Realizar Backup Agora")
        backupNowButton.style = """
            -fx-background-color: #4CAF50;
            -fx-text-fill: white;
            -fx-background-radius: 4px;
            -fx-border-radius: 4px;
            -fx-font-weight: normal;
            -fx-padding: 8px 15px;
            -fx-cursor: hand;
            -fx-effect: null;
            -fx-border-width: 0;
            -fx-border-color: transparent;
        """
        backupNowButton.setOnAction {
            if (controller.showBackupConfirmationAlert()) {
                if (controller.performBackup()) {
                    controller.showBackupSuccessAlert()
                } else {
                    controller.showBackupErrorAlert()
                }
            }
        }

        manualSection.children.addAll(manualLabel, backupNowButton)

        // Add all sections to the card
        backupCard.children.addAll(
            backupTitle,
            directorySection,
            optionsSection,
            manualSection
        )

        container.children.add(backupCard)

        return container
    }

    private fun createPrinterTab(): VBox {
        val container = VBox()
        container.spacing = 20.0
        container.style = "-fx-background-color: transparent;"
        container.padding = Insets(20.0, 20.0, 20.0, 20.0)

        // Card para configurações de impressora
        val printerCard = VBox()
        printerCard.spacing = 20.0
        printerCard.padding = Insets(20.0)
        printerCard.style = """
        -fx-background-color: white;
        -fx-border-color: #E5E5E5;
        -fx-border-width: 1px;
        -fx-border-radius: 8px;
    """

        // Título para o card
        val printerTitle = Label("Configurações de Impressão")
        printerTitle.font = Font.font("System", FontWeight.BOLD, 16.0)

        // Seção de informações da empresa
        val companySection = VBox()
        companySection.spacing = 15.0

        val companyLabel = Label("Informações da Empresa")
        companyLabel.style = "-fx-text-fill: #6E6E6E; -fx-font-weight: bold;"

        // Nome da empresa
        val nameLabel = Label("Nome da Empresa")
        nameLabel.style = "-fx-text-fill: #6E6E6E;"
        val nameField = TextField(controller.companyName)
        nameField.promptText = "Nome que aparecerá nos comprovantes"
        nameField.prefWidth = 500.0
        HBox.setHgrow(nameField, Priority.ALWAYS)

        val cnpjLabel = Label("CNPJ")
        cnpjLabel.style = "-fx-text-fill: #6E6E6E; -fx-padding: 0 0 0 10;"
        val cnpjField = TextField(controller.companyCnpj)
        cnpjField.promptText = "CNPJ da empresa"
        cnpjField.prefWidth = 200.0

        val nameAndCnpjBox = HBox(10.0,
            VBox(5.0, nameLabel, nameField),
            VBox(5.0, cnpjLabel, cnpjField)
        )
        nameAndCnpjBox.alignment = Pos.CENTER_LEFT

        // Telefone
        val phoneLabel = Label("Telefone")
        phoneLabel.style = "-fx-text-fill: #6E6E6E;"
        val phoneField = TextField(controller.companyPhone)
        phoneField.promptText = "Telefone para contato"

        // Endereço
        val addressLabel = Label("Endereço")
        addressLabel.style = "-fx-text-fill: #6E6E6E;"
        val addressField = TextField(controller.companyAddress)
        addressField.promptText = "Endereço completo"

        companySection.children.addAll(
            companyLabel,
            nameAndCnpjBox,
            phoneLabel, phoneField,
            addressLabel, addressField
        )

        // Seção de seleção de impressora
        val printerSection = VBox()
        printerSection.spacing = 15.0
        printerSection.padding = Insets(10.0, 0.0, 0.0, 0.0)

        val printerSelectLabel = Label("Impressora Térmica Padrão")
        printerSelectLabel.style = "-fx-text-fill: #6E6E6E; -fx-font-weight: bold;"

        val printerComboBox = ComboBox<String>()
        printerComboBox.promptText = "Selecionar impressora"
        printerComboBox.prefWidth = 300.0

        // Carregar impressoras disponíveis
        val availablePrinters = PrintServiceLookup.lookupPrintServices(null, null)
        val printerNames = availablePrinters.map { it.name }
        printerComboBox.items.addAll(printerNames)

// Selecionar a impressora padrão, se existir
        if (controller.defaultPrinter.isNotEmpty()) {
            printerComboBox.selectionModel.select(controller.defaultPrinter)
        }

// Create test print button
        val testPrintButton = Button("Imprimir Teste")
        testPrintButton.style = """
    -fx-background-color: #6056e8;
    -fx-text-fill: white;
    -fx-background-radius: 4px;
    -fx-border-radius: 4px;
    -fx-font-weight: normal;
    -fx-padding: 6px 12px;
    -fx-cursor: hand;
    -fx-effect: null;
    -fx-border-width: 0;
    -fx-border-color: transparent;
"""

        testPrintButton.setOnAction {
            val printerSelected = printerComboBox.selectionModel.selectedItem
            if (printerSelected != null && printerSelected.isNotEmpty()) {
                val printerController = PrinterController()
                val testData = mapOf(
                    "company_name" to nameField.text,
                    "company_phone" to phoneField.text,
                    "company_address" to addressField.text
                )
                printerController.printTestPage(printerSelected, testData)
            } else {
                val alert = Alert(Alert.AlertType.WARNING)
                alert.title = "Impressora não selecionada"
                alert.headerText = null
                alert.contentText = "Por favor, selecione uma impressora para testar a impressão."
                alert.showAndWait()
            }
        }

        //val printerInfoLabel = Label("Selecione a impressora térmica para impressão de pedidos")
        //printerInfoLabel.style = "-fx-text-fill: #6E6E6E; -fx-font-size: 11px;"
        //printerInfoLabel.wrapTextProperty().set(true)
        //HBox.setHgrow(printerInfoLabel, Priority.ALWAYS)

// Create horizontal layout with label and button
        val infoActionBox = HBox(10.0)
        infoActionBox.alignment = Pos.CENTER_LEFT
        infoActionBox.children.addAll( testPrintButton)

        printerSection.children.addAll(
            printerSelectLabel,
            printerComboBox,
            infoActionBox
        )

// Remove the separate test section and add remaining sections to card
        printerCard.children.addAll(
            printerTitle,
            companySection,
            printerSection
        )

        container.children.add(printerCard)

        return container
    }

    override fun refresh() {
        // Refresh data when the view is displayed
        Thread {
            try {
                val (_, _, releaseDate) = versionChecker.getLatestReleaseInfo()
                Platform.runLater {
                    updateDateLabel.text = releaseDate ?: "Não disponível"
                }
            } catch (e: Exception) {
                // Handle exception
            }
        }.start()
    }
}
package com.sistema_pedidos.view

import com.sistema_pedidos.controller.ClientesController
import com.sistema_pedidos.model.Cliente
import com.sistema_pedidos.model.TipoCliente
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.application.Platform
import java.util.*
import javafx.stage.StageStyle

class ClientesView : VBox(10.0) {
    private val tableView = TableView<Cliente>()
    private val clientes = FXCollections.observableArrayList<Cliente>()
    private val searchField = TextField()
    private val formPanel = VBox(10.0)
    private var clienteSelecionado: Cliente? = null
    private val controller = ClientesController()

    // Common fields
    private lateinit var tfId: TextField
    private lateinit var tfTelefone: TextField
    private lateinit var tfEmail: TextField
    private lateinit var taObservacao: TextArea
    private lateinit var btnSalvar: Button
    private lateinit var btnCancelar: Button

    // PF fields
    private lateinit var tfNome: TextField
    private lateinit var tfSobrenome: TextField
    private lateinit var tfCpf: TextField

    // PJ fields
    private lateinit var tfRazaoSocial: TextField
    private lateinit var tfNomeFantasia: TextField
    private lateinit var tfCnpj: TextField
    private lateinit var tfInscricaoEstadual: TextField

    // Address fields
    private lateinit var tfCep: TextField
    private lateinit var tfLogradouro: TextField
    private lateinit var tfNumero: TextField
    private lateinit var tfComplemento: TextField
    private lateinit var tfBairro: TextField
    private lateinit var tfCidade: TextField
    private lateinit var cbEstado: ComboBox<String>

    // Type selector
    private lateinit var rgTipoCliente: ToggleGroup
    private lateinit var rbPessoaFisica: RadioButton
    private lateinit var rbPessoaJuridica: RadioButton

    // Form content containers
    private lateinit var pessoaFisicaForm: VBox
    private lateinit var pessoaJuridicaForm: VBox

    init {
        padding = Insets(20.0)
        prefWidth = 1000.0
        prefHeight = 700.0

        styleClass.add("main-container")
        stylesheets.add(javaClass.getResource("/clientesview.css").toExternalForm())

        setupUI()
        carregarClientes()
    }

    private fun setupUI() {
        val title = Label("Gerenciamento de Clientes").apply {
            style = "-fx-font-size: 24px; -fx-font-weight: bold;"
        }

        // Search controls
        searchField.apply {
            promptText = "Buscar clientes por nome, CPF/CNPJ ou telefone..."
            prefWidth = 300.0
            prefHeight = 36.0
        }

        val searchButton = Button("Buscar").apply {
            styleClass.add("primary-button")
            prefHeight = 36.0
            setOnAction { buscarClientes(searchField.text) }
        }

        val refreshButton = Button("Atualizar").apply {
            styleClass.add("primary-button")
            prefHeight = 36.0
            setOnAction { carregarClientes() }
        }

        val newButton = Button("Novo Cliente").apply {
            styleClass.add("primary-button")
            prefHeight = 36.0
            setOnAction { limparFormulario() }
        }

        val topControls = HBox(10.0, searchField, searchButton, refreshButton, newButton).apply {
            alignment = Pos.CENTER_LEFT
        }

        val headerBox = VBox(15.0, title, topControls).apply {
            padding = Insets(0.0, 0.0, 15.0, 0.0)
        }

        val splitPane = HBox(20.0).apply {
            children.addAll(
                createTableView(),
                createFormPanel()
            )
            HBox.setHgrow(tableView, Priority.ALWAYS)
        }

        VBox.setVgrow(splitPane, Priority.ALWAYS)

        children.addAll(headerBox, splitPane)
    }

    private fun createTableView(): TableView<Cliente> {
        tableView.apply {
            columnResizePolicy = TableView.UNCONSTRAINED_RESIZE_POLICY
            items = clientes
            styleClass.add("table-view")
        }

        setupTableColumns()

        VBox.setVgrow(tableView, Priority.ALWAYS)
        HBox.setHgrow(tableView, Priority.ALWAYS)

        return tableView
    }

    private fun setupTableColumns() {
        val idColumn = TableColumn<Cliente, Long>("ID").apply {
            cellValueFactory = PropertyValueFactory("id")
            isVisible = false
        }

        val tipoColumn = TableColumn<Cliente, String>("Tipo").apply {
            cellValueFactory = PropertyValueFactory("tipoDisplay")
            prefWidth = 100.0
        }

        val nomeColumn = TableColumn<Cliente, String>("Nome/Razão Social").apply {
            cellValueFactory = PropertyValueFactory("nomeDisplay")
            prefWidth = 250.0
        }

        val documentoColumn = TableColumn<Cliente, String>("CPF/CNPJ").apply {
            cellValueFactory = PropertyValueFactory("documentoDisplay")
            prefWidth = 120.0
        }

        val telefoneColumn = TableColumn<Cliente, String>("Telefone").apply {
            cellValueFactory = PropertyValueFactory("telefone")
            prefWidth = 120.0
        }

        val emailColumn = TableColumn<Cliente, String>("Email").apply {
            cellValueFactory = PropertyValueFactory("email")
            prefWidth = 180.0
        }

        val acoesColumn = TableColumn<Cliente, Void>("Ações").apply {
            prefWidth = 180.0
            cellFactory = javafx.util.Callback {
                object : TableCell<Cliente, Void>() {
                    private val editButton = Button("Editar").apply {
                        styleClass.add("small-button")
                        prefWidth = 80.0
                        setOnAction {
                            val cliente = tableView.items[index]
                            preencherFormulario(cliente)
                        }
                    }

                    private val deleteButton = Button("Excluir").apply {
                        styleClass.add("small-button")
                        styleClass.add("botao-cancel")
                        prefWidth = 72.0
                        setOnAction {
                            val cliente = tableView.items[index]
                            confirmarExclusao(cliente)
                        }
                    }

                    private val buttonBox = HBox(8.0, editButton, deleteButton).apply {
                        alignment = Pos.CENTER
                        padding = Insets(2.0)  // Add padding around buttons
                    }

                    override fun updateItem(item: Void?, empty: Boolean) {
                        super.updateItem(item, empty)
                        graphic = if (empty) null else buttonBox
                    }
                }
            }
        }
        tableView.columns.addAll(idColumn, tipoColumn, nomeColumn, documentoColumn, telefoneColumn, emailColumn, acoesColumn)
    }

    private fun createFormPanel(): VBox {
        tfId = TextField().apply {
            isEditable = false
            promptText = "ID (gerado automaticamente)"
            styleFormField(this)
            // Remove the red debugging background
        }

        rgTipoCliente = ToggleGroup()

        rbPessoaFisica = RadioButton("Pessoa Física").apply {
            toggleGroup = rgTipoCliente
            isSelected = true
            styleClass.add("custom-radio")
        }

        rbPessoaJuridica = RadioButton("Pessoa Jurídica").apply {
            toggleGroup = rgTipoCliente
            styleClass.add("custom-radio")
        }

        val tipoClienteBox = HBox(20.0, rbPessoaFisica, rbPessoaJuridica).apply {
            alignment = Pos.CENTER_LEFT
            padding = Insets(10.0)
            style = "-fx-background-color: white; -fx-border-color: #eaeaea; -fx-border-radius: 5px;"
            prefHeight = 36.0
        }

        createPessoaFisicaForm()
        createPessoaJuridicaForm()

        // Initially hide pessoaJuridicaForm
        pessoaJuridicaForm.isVisible = false
        pessoaJuridicaForm.isManaged = false

        val contatoPanel = createContatoFields()
        val enderecoPanel = createEnderecoFields()

        btnSalvar = Button("Salvar").apply {
            styleClass.add("primary-button")
            prefWidth = 120.0
            setOnAction { salvarCliente() }
        }

        btnCancelar = Button("Cancelar").apply {
            styleClass.add("secondary-button")
            prefWidth = 120.0
            setOnAction { limparFormulario() }
        }

        val buttonsBox = HBox(10.0, btnSalvar, btnCancelar).apply {
            alignment = Pos.CENTER
            padding = Insets(0.0, 0.0, 0.0, 0.0)
            style = "-fx-background-color: transparent;"
        }

        // Create a placeholder to maintain consistent form layout
        val formPlaceholder = VBox().apply {
            children.addAll(pessoaFisicaForm, pessoaJuridicaForm)
        }

        // Form container setup
        val formContainer = VBox(20.0).apply {
            children.addAll(tipoClienteBox, formPlaceholder, contatoPanel, enderecoPanel, buttonsBox)
            style = "-fx-background-color: #F7F7F7;"
            padding = Insets(0.0, 0.0, 0.0, 0.0)
        }

        rgTipoCliente.selectedToggleProperty().addListener { _, _, newValue ->
            when (newValue) {
                rbPessoaFisica -> {
                    pessoaFisicaForm.isVisible = true
                    pessoaFisicaForm.isManaged = true
                    pessoaJuridicaForm.isVisible = false
                    pessoaJuridicaForm.isManaged = false
                }
                rbPessoaJuridica -> {
                    pessoaFisicaForm.isVisible = false
                    pessoaFisicaForm.isManaged = false
                    pessoaJuridicaForm.isVisible = true
                    pessoaJuridicaForm.isManaged = true
                }
            }
        }
        formContainer.padding = Insets(0.0, 2.0, 0.0, 0.0)

        val scrollPane = ScrollPane().apply {
            isFitToWidth = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
            style = "-fx-background-color: #F7F7F7; -fx-background: #F7F7F7;"
            content = formContainer
        }

        formPanel.apply {
            padding = Insets(15.0)
            spacing = 10.0
            prefWidth = 450.0
            maxWidth = 450.0
            style = """
            -fx-background-color: white;
            -fx-border-color: rgb(223, 225, 230);
            -fx-border-width: 2px;
            -fx-border-radius: 3px;
        """
            children.add(Label("Dados do Cliente").apply { style = "-fx-font-weight: bold; -fx-font-size: 16px;" })
        }

        return VBox(scrollPane).apply {
            HBox.setHgrow(this, Priority.NEVER)
            style = "-fx-background-color: #F7F7F7;"
        }
    }

    private fun createPessoaFisicaForm(): VBox {
        tfNome = TextField().apply {
            promptText = "Nome"
            styleFormField(this)
        }

        tfSobrenome = TextField().apply {
            promptText = "Sobrenome"
            styleFormField(this)
        }

        tfCpf = TextField().apply {
            promptText = "CPF"
            styleFormField(this)
        }
        formatarCpf(tfCpf)

        pessoaFisicaForm = VBox(10.0).apply {
            children.addAll(
                Label("Dados Pessoais").apply { style = "-fx-font-weight: bold;" },
                Label("Nome:"),
                tfNome,
                Label("Sobrenome:"),
                tfSobrenome,
                Label("CPF:"),
                tfCpf
            )
            style = "-fx-padding: 10px; -fx-border-color: #eaeaea; -fx-border-radius: 5px; -fx-background-color: white;"        }

        return pessoaFisicaForm
    }

    private fun createPessoaJuridicaForm(): VBox {
        tfRazaoSocial = TextField().apply {
            promptText = "Razão Social"
            styleFormField(this)
        }

        tfNomeFantasia = TextField().apply {
            promptText = "Nome Fantasia"
            styleFormField(this)
        }

        tfCnpj = TextField().apply {
            promptText = "CNPJ"
            styleFormField(this)
        }
        formatarCnpj(tfCnpj)

        tfInscricaoEstadual = TextField().apply {
            promptText = "Inscrição Estadual"
            styleFormField(this)
        }

        pessoaJuridicaForm = VBox(10.0).apply {
            children.addAll(
                Label("Dados da Empresa").apply { style = "-fx-font-weight: bold;" },
                Label("Razão Social:"),
                tfRazaoSocial,
                Label("Nome Fantasia:"),
                tfNomeFantasia,
                Label("CNPJ:"),
                tfCnpj,
                Label("Inscrição Estadual:"),
                tfInscricaoEstadual
            )
            style = "-fx-padding: 10px; -fx-border-color: #eaeaea; -fx-border-radius: 5px; -fx-background-color: white;"        }

        return pessoaJuridicaForm
    }

    private fun createContatoFields(): VBox {
        tfTelefone = TextField().apply {
            promptText = "Telefone"
            styleFormField(this)
        }
        formatarTelefone(tfTelefone)

        tfEmail = TextField().apply {
            promptText = "Email"
            styleFormField(this)
        }

        taObservacao = TextArea().apply {
            promptText = "Observações"
            prefHeight = 80.0
            style = """
                -fx-background-color: rgb(250, 251, 252);
                -fx-border-color: rgb(223, 225, 230);
                -fx-text-fill: rgb(9, 30, 66);
                -fx-border-radius: 3px;
                -fx-border-width: 2px;
                -fx-font-size: 14px;
            """
        }

        return VBox(10.0).apply {
            children.addAll(
                Label("Contato").apply { style = "-fx-font-weight: bold;" },
                Label("Telefone:"),
                tfTelefone,
                Label("Email:"),
                tfEmail,
                Label("Observações:"),
                taObservacao
            )
            style = "-fx-padding: 10px; -fx-border-color: #eaeaea; -fx-border-radius: 5px; -fx-background-color: white;"        }
    }

    private fun createEnderecoFields(): VBox {
        tfCep = TextField().apply {
            promptText = "CEP"
            styleFormField(this)
        }
        formatarCep(tfCep)

        tfLogradouro = TextField().apply {
            promptText = "Logradouro"
            styleFormField(this)
        }

        tfNumero = TextField().apply {
            promptText = "Número"
            styleFormField(this)
            prefWidth = 100.0
        }

        tfComplemento = TextField().apply {
            promptText = "Complemento"
            styleFormField(this)
        }

        tfBairro = TextField().apply {
            promptText = "Bairro"
            styleFormField(this)
        }

        tfCidade = TextField().apply {
            promptText = "Cidade"
            styleFormField(this)
        }

        cbEstado = ComboBox<String>().apply {
            items = FXCollections.observableArrayList(
                "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS",
                "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC",
                "SP", "SE", "TO"
            )
            promptText = "UF"
            prefHeight = 36.0
            prefWidth = 100.0
            styleClass.add("combo-box")
        }

        val enderecoLinha1 = HBox(10.0).apply {
            children.addAll(
                VBox(5.0, Label("CEP:"), tfCep),
                VBox(5.0, Label("Logradouro:"), tfLogradouro)
            )
            HBox.setHgrow(tfLogradouro.parent as VBox, Priority.ALWAYS)
        }

        val enderecoLinha2 = HBox(10.0).apply {
            children.addAll(
                VBox(5.0, Label("Número:"), tfNumero),
                VBox(5.0, Label("Complemento:"), tfComplemento)
            )
            HBox.setHgrow(tfComplemento.parent as VBox, Priority.ALWAYS)
        }

        val enderecoLinha3 = HBox(10.0).apply {
            children.addAll(
                VBox(5.0, Label("Bairro:"), tfBairro),
                VBox(5.0, Label("Cidade:"), tfCidade),
                VBox(5.0, Label("UF:"), cbEstado)
            )
            HBox.setHgrow(tfBairro.parent as VBox, Priority.SOMETIMES)
            HBox.setHgrow(tfCidade.parent as VBox, Priority.SOMETIMES)
        }

        return VBox(10.0).apply {
            children.addAll(
                Label("Endereço").apply { style = "-fx-font-weight: bold;" },
                enderecoLinha1,
                enderecoLinha2,
                enderecoLinha3
            )
            style = "-fx-padding: 10px; -fx-border-color: #eaeaea; -fx-border-radius: 5px; -fx-background-color: white;"        }
    }

    private fun formatarTelefone(textField: TextField) {
        var isUpdating = false
        textField.textProperty().addListener { _, oldValue, newValue ->
            if (isUpdating || newValue == oldValue) return@addListener

            isUpdating = true
            Platform.runLater {
                try {
                    val digits = newValue.filter { it.isDigit() }.take(11)
                    val formatted = when {
                        digits.isEmpty() -> ""
                        digits.length <= 2 -> digits
                        digits.length <= 7 -> "(${digits.take(2)}) ${digits.drop(2)}"
                        else -> "(${digits.take(2)}) ${digits.slice(2..6)}-${digits.drop(7)}"
                    }

                    if (newValue != formatted) {
                        textField.text = formatted
                        textField.positionCaret(formatted.length)
                    }
                } finally {
                    isUpdating = false
                }
            }
        }
    }

    private fun formatarCpf(textField: TextField) {
        var isUpdating = false
        textField.textProperty().addListener { _, oldValue, newValue ->
            if (isUpdating || newValue == oldValue) return@addListener

            isUpdating = true
            Platform.runLater {
                try {
                    val digits = newValue.filter { it.isDigit() }.take(11)
                    val formatted = when {
                        digits.isEmpty() -> ""
                        digits.length <= 3 -> digits
                        digits.length <= 6 -> "${digits.take(3)}.${digits.drop(3)}"
                        digits.length <= 9 -> "${digits.take(3)}.${digits.slice(3..5)}.${digits.drop(6)}"
                        else -> "${digits.take(3)}.${digits.slice(3..5)}.${digits.slice(6..8)}-${digits.drop(9)}"
                    }

                    if (newValue != formatted) {
                        textField.text = formatted
                        textField.positionCaret(formatted.length)
                    }
                } finally {
                    isUpdating = false
                }
            }
        }
    }

    private fun formatarCnpj(textField: TextField) {
        var isUpdating = false
        textField.textProperty().addListener { _, oldValue, newValue ->
            if (isUpdating || newValue == oldValue) return@addListener

            isUpdating = true
            Platform.runLater {
                try {
                    val digits = newValue.filter { it.isDigit() }.take(14)
                    val formatted = when {
                        digits.isEmpty() -> ""
                        digits.length <= 2 -> digits
                        digits.length <= 5 -> "${digits.take(2)}.${digits.drop(2)}"
                        digits.length <= 8 -> "${digits.take(2)}.${digits.slice(2..4)}.${digits.drop(5)}"
                        digits.length <= 12 -> "${digits.take(2)}.${digits.slice(2..4)}.${digits.slice(5..7)}/${digits.drop(8)}"
                        else -> "${digits.take(2)}.${digits.slice(2..4)}.${digits.slice(5..7)}/${digits.slice(8..11)}-${digits.drop(12)}"
                    }

                    if (newValue != formatted) {
                        textField.text = formatted
                        textField.positionCaret(formatted.length)
                    }
                } finally {
                    isUpdating = false
                }
            }
        }
    }

    private fun formatarCep(textField: TextField) {
        var isUpdating = false
        textField.textProperty().addListener { _, oldValue, newValue ->
            if (isUpdating || newValue == oldValue) return@addListener

            isUpdating = true
            Platform.runLater {
                try {
                    val digits = newValue.filter { it.isDigit() }.take(8)
                    val formatted = when {
                        digits.isEmpty() -> ""
                        digits.length <= 5 -> digits
                        else -> "${digits.take(5)}-${digits.drop(5)}"
                    }

                    if (newValue != formatted) {
                        textField.text = formatted
                        textField.positionCaret(formatted.length)
                    }
                } finally {
                    isUpdating = false
                }
            }
        }
    }

    private fun styleFormField(field: TextField) {
        field.apply {
            prefHeight = 36.0
            styleClass.add("text-field")
            style = "-fx-background-color: white;"
        }
    }


    private fun carregarClientes() {
        clientes.clear()
        clientes.addAll(controller.buscarTodosClientes())
        tableView.refresh()
    }

    private fun buscarClientes(termo: String) {
        if (termo.isBlank()) {
            carregarClientes()
            return
        }

        clientes.clear()
        clientes.addAll(controller.buscarClientesPorTermo(termo))
        tableView.refresh()
    }

    private fun preencherFormulario(cliente: Cliente) {
        limparFormulario()
        clienteSelecionado = cliente

        tfId.text = cliente.id.toString()
        tfTelefone.text = cliente.telefone
        tfEmail.text = cliente.email
        taObservacao.text = cliente.observacao

        tfCep.text = cliente.cep
        tfLogradouro.text = cliente.logradouro
        tfNumero.text = cliente.numero
        tfComplemento.text = cliente.complemento
        tfBairro.text = cliente.bairro
        tfCidade.text = cliente.cidade
        if (cliente.estado.isNotBlank()) {
            cbEstado.selectionModel.select(cliente.estado)
        }

        when (cliente.tipo) {
            TipoCliente.PESSOA_FISICA -> {
                rbPessoaFisica.isSelected = true
                tfNome.text = cliente.nome
                tfSobrenome.text = cliente.sobrenome
                tfCpf.text = cliente.cpf
            }
            TipoCliente.PESSOA_JURIDICA -> {
                rbPessoaJuridica.isSelected = true
                tfRazaoSocial.text = cliente.razaoSocial
                tfNomeFantasia.text = cliente.nomeFantasia
                tfCnpj.text = cliente.cnpj
                tfInscricaoEstadual.text = cliente.inscricaoEstadual
            }
        }

        btnSalvar.text = "Atualizar"
    }

    private fun confirmarExclusao(cliente: Cliente) {
        val result = showAlert(
            Alert.AlertType.CONFIRMATION,
            "Confirmar Exclusão",
            "Deseja realmente excluir o cliente ${cliente.nomeDisplay}?"
        )

        if (result.isPresent && result.get().buttonData == ButtonBar.ButtonData.YES) {
            if (controller.excluirCliente(cliente.id)) {
                carregarClientes()
                limparFormulario()
            }
        }
    }

    private fun showStyledAlert(type: Alert.AlertType, title: String, message: String): Optional<ButtonType> {
        val alert = Alert(type).apply {
            this.title = title
            headerText = title
            contentText = message
            initStyle(StageStyle.UNDECORATED)

            dialogPane.styleClass.add("custom-dialog")
            dialogPane.stylesheets.addAll(this@ClientesView.stylesheets)

            // Remove the default icon from the header
            dialogPane.graphic = null

            if (type == Alert.AlertType.CONFIRMATION) {
                val btnSim = ButtonType("Sim", ButtonBar.ButtonData.YES)
                val btnNao = ButtonType("Não", ButtonBar.ButtonData.NO)
                buttonTypes.setAll(btnSim, btnNao)
            }
        }

        Platform.runLater {
            alert.dialogPane.buttonTypes.forEach { buttonType ->
                val button = alert.dialogPane.lookupButton(buttonType)
                button.styleClass.add("dialog-button")
            }
        }

        return alert.showAndWait()
    }

    private fun salvarCliente() {
        val required = when {
            rbPessoaFisica.isSelected && tfNome.text.isBlank() -> "Nome"
            rbPessoaFisica.isSelected && tfCpf.text.isBlank() -> "CPF"
            rbPessoaJuridica.isSelected && tfRazaoSocial.text.isBlank() -> "Razão Social"
            rbPessoaJuridica.isSelected && tfCnpj.text.isBlank() -> "CNPJ"
            tfTelefone.text.isBlank() -> "Telefone"
            else -> null
        }

        if (required != null) {
            showStyledAlert(
                Alert.AlertType.WARNING,
                "Campo Obrigatório",
                "O campo $required é obrigatório."
            )
            return
        }

        val tipoCliente = if (rbPessoaFisica.isSelected) TipoCliente.PESSOA_FISICA else TipoCliente.PESSOA_JURIDICA

        val cliente = Cliente(
            id = if (clienteSelecionado != null) clienteSelecionado!!.id else 0,
            tipo = tipoCliente,

            // PF fields
            nome = tfNome.text,
            sobrenome = tfSobrenome.text,
            cpf = tfCpf.text,

            // PJ fields
            razaoSocial = tfRazaoSocial.text,
            nomeFantasia = tfNomeFantasia.text,
            cnpj = tfCnpj.text,
            inscricaoEstadual = tfInscricaoEstadual.text,

            // Common fields
            telefone = tfTelefone.text,
            email = tfEmail.text,
            observacao = taObservacao.text,

            // Address fields
            cep = tfCep.text,
            logradouro = tfLogradouro.text,
            numero = tfNumero.text,
            complemento = tfComplemento.text,
            bairro = tfBairro.text,
            cidade = tfCidade.text,
            estado = cbEstado.selectionModel.selectedItem ?: ""
        )

        val sucesso = if (clienteSelecionado == null) {
            controller.adicionarCliente(cliente)
        } else {
            controller.atualizarCliente(cliente)
        }

        if (sucesso) {
            carregarClientes()
            limparFormulario()

            showStyledAlert(
                Alert.AlertType.INFORMATION,
                "Sucesso",
                "Cliente ${if (clienteSelecionado == null) "adicionado" else "atualizado"} com sucesso!"
            )
        }
    }

    private fun showAlert(type: Alert.AlertType, title: String, message: String): Optional<ButtonType> {
        return showStyledAlert(type, title, message)
    }

    private fun limparFormulario() {
        clienteSelecionado = null
        tfId.text = ""

        // Clear PF fields
        tfNome.text = ""
        tfSobrenome.text = ""
        tfCpf.text = ""

        // Clear PJ fields
        tfRazaoSocial.text = ""
        tfNomeFantasia.text = ""
        tfCnpj.text = ""
        tfInscricaoEstadual.text = ""

        // Clear common fields
        tfTelefone.text = ""
        tfEmail.text = ""
        taObservacao.text = ""

        // Clear address fields
        tfCep.text = ""
        tfLogradouro.text = ""
        tfNumero.text = ""
        tfComplemento.text = ""
        tfBairro.text = ""
        tfCidade.text = ""
        cbEstado.selectionModel.clearSelection()

        rbPessoaFisica.isSelected = true
        btnSalvar.text = "Salvar"
    }
}
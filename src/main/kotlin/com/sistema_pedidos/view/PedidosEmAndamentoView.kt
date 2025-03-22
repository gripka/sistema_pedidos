package com.sistema_pedidos.view

import com.sistema_pedidos.controller.PedidosEmAndamentoController
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.StageStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javafx.application.Platform

class PedidosEmAndamentoView : VBox(10.0), ViewLifecycle {
    private val controller = PedidosEmAndamentoController()
    private val tableView = TableView<Map<String, Any>>()
    private val dataInicial = DatePicker(LocalDate.now().minusDays(7))
    private val dataFinal = DatePicker(LocalDate.now())
    private val searchField = TextField()


    init {
        padding = Insets(20.0)
        prefWidth = 1000.0
        prefHeight = 700.0

        styleClass.add("main-container")
        stylesheets.add(javaClass.getResource("/historicopedidosview.css").toExternalForm())

        val headerBox = createHeaderBox()

        // Add keyboard shortcut help indicator
        val shortcutHelp = Label("Atalhos: [P] Pagamento | [S] Status | [I] Imprimir | [V] Ver detalhes").apply {
            style = "-fx-font-size: 12px; -fx-text-fill: #707070;"
        }

        val filterBox = createFilterBox()

        setupTableView()

        val actionBox = createActionBox()

        children.addAll(headerBox, shortcutHelp, filterBox, tableView, actionBox)

        refreshData()

        // Add keyboard shortcuts for quickly managing orders
        tableView.setOnKeyPressed { event ->
            val selectedItem = tableView.selectionModel.selectedItem ?: return@setOnKeyPressed
            val pedidoId = selectedItem["id"] as Long

            when (event.code) {
                javafx.scene.input.KeyCode.P -> {
                    // P key toggles payment status
                    val currentStatus = selectedItem["status"] as String
                    val newStatus = if (currentStatus == "Pendente") "Pago" else "Pendente"

                    // Add confirmation dialog
                    val confirmMessage = "Deseja alterar o status de pagamento de '$currentStatus' para '$newStatus'?"
                    if (showConfirmationDialog("Confirmar Alteração", confirmMessage)) {
                        if (controller.atualizarStatusPagamentoPedido(pedidoId, newStatus)) {
                            (selectedItem as MutableMap<String, Any>)["status"] = newStatus
                            tableView.refresh()
                        }
                    }
                    event.consume()
                }

                javafx.scene.input.KeyCode.S -> {
                    // S key advances order status
                    val currentStatus = selectedItem["status_pedido"] as? String ?: "Pendente"
                    val newStatus = when (currentStatus) {
                        "Pendente" -> "Preparando"
                        "Preparando" -> "Em Entrega"
                        "Em Entrega" -> "Concluido"
                        else -> currentStatus
                    }

                    // Add confirmation dialog
                    val confirmMessage = "Deseja alterar o status do pedido de '$currentStatus' para '$newStatus'?"
                    if (showConfirmationDialog("Confirmar Alteração", confirmMessage)) {
                        if (controller.atualizarStatusPedido(pedidoId, newStatus)) {
                            (selectedItem as MutableMap<String, Any>)["status_pedido"] = newStatus
                            tableView.refresh()

                            if (newStatus == "Concluido" || newStatus == "Cancelado") {
                                tableView.items.remove(selectedItem)
                            }
                        }
                    }
                    event.consume()
                }
                javafx.scene.input.KeyCode.I -> {
                    // I key for printing
                    controller.imprimirPedido(selectedItem)
                    event.consume()
                }
                javafx.scene.input.KeyCode.V -> {
                    // V key to view order details
                    showOrderDetailsDialog(selectedItem)
                    event.consume()
                }
                else -> {}
            }
        }
    }

    override fun refresh() {
        refreshData()
    }

    override fun close() {
        tableView.items.clear()

    }

    private fun createHeaderBox(): HBox {
        return HBox(10.0).apply {
            alignment = Pos.CENTER_LEFT

            val titleLabel = Label("Pedidos em Andamento").apply {
                style = "-fx-font-size: 22px; -fx-font-weight: bold;"
            }

            children.add(titleLabel)
        }
    }

    private fun createFilterBox(): HBox {
        return HBox(10.0).apply {
            alignment = Pos.CENTER_LEFT
            padding = Insets(10.0, 0.0, 10.0, 0.0)
            styleClass.add("filter-container")

            val dataInicialBox = VBox(5.0).apply {
                children.addAll(
                    Label("Data Inicial:").apply {
                        styleClass.add("field-label")
                    },
                    dataInicial.apply {
                        prefHeight = 36.0
                    }
                )
            }

            val dataFinalBox = VBox(5.0).apply {
                children.addAll(
                    Label("Data Final:").apply {
                        styleClass.add("field-label")
                    },
                    dataFinal.apply {
                        prefHeight = 36.0
                    }
                )
            }

            val searchBox = VBox(5.0).apply {
                HBox.setHgrow(this, Priority.ALWAYS)
                children.addAll(
                    Label("Buscar (Nº pedido, cliente, telefone):").apply {
                        styleClass.add("field-label")
                    },
                    searchField.apply {
                        promptText = "Digite para buscar..."
                        prefWidth = 250.0
                        prefHeight = 36.0
                    }
                )
            }

            val filterButtonBox = VBox(5.0).apply {
                children.addAll(
                    Label(" ").apply {
                        styleClass.add("field-label")
                        isVisible = false
                    },
                    Button("Filtrar").apply {
                        styleClass.add("primary-button")
                        prefHeight = 36.0
                        prefWidth = 100.0
                        setOnAction {
                            refreshData()
                        }
                    }
                )
            }

            children.addAll(dataInicialBox, dataFinalBox, searchBox, filterButtonBox)
        }
    }

    private fun showOrderDetailsDialog(pedido: Map<String, Any>) {
        // Fetch complete order data
        val orderDetails = controller.getCompleteOrderDetails(pedido["id"] as Long)

        // Create content sections
        val content = VBox(10.0).apply {
            padding = Insets(20.0)
            prefWidth = 500.0
            style = """
            -fx-background-color: white;
            -fx-background-radius: 0;
        """

            // Add order header with number and date
            children.add(
                HBox(10.0).apply {
                    alignment = Pos.CENTER
                    children.addAll(
                        Label("PEDIDO ${orderDetails["numero"]}").apply {
                            style = "-fx-font-size: 16px; -fx-font-weight: bold;"
                        },
                        Label("Data: ${orderDetails["data_pedido"]}").apply {
                            style = "-fx-font-size: 14px;"
                        }
                    )
                }
            )

            children.addAll(
                createDetailsSection("Cliente", orderDetails["cliente"] as List<Pair<String, String>>),
                createDetailsSection("Produtos", orderDetails["produtos"] as List<Pair<String, String>>),
                createDetailsSection("Pagamento", orderDetails["pagamento"] as List<Pair<String, String>>)
            )

            val entregaDetails = orderDetails["entrega"] as List<Pair<String, String>>
            if (entregaDetails.isNotEmpty() && entregaDetails.first().second != "Não") {
                children.add(createDetailsSection("Entrega", entregaDetails))
            }
        }

        val dialog = Dialog<ButtonType>().apply {
            initStyle(StageStyle.UNDECORATED)
            title = "Detalhes do Pedido"
            headerText = "Detalhes do Pedido ${orderDetails["numero"]}"

            dialogPane.content = ScrollPane(content).apply {
                isFitToWidth = true
                prefViewportHeight = 400.0
                vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
                style = "-fx-background: white; -fx-background-color: white;"
            }

            dialogPane.buttonTypes.add(ButtonType.CLOSE)
            dialogPane.lookupButton(ButtonType.CLOSE).style = "-fx-pref-width: 100px;"

            (dialogPane.lookup(".button-bar") as ButtonBar).buttonOrder = "C:0"

            dialogPane.stylesheets.addAll(this@PedidosEmAndamentoView.stylesheets)
            dialogPane.style = """
            -fx-background-color: white;
            -fx-border-color: #D3D3D3;
            -fx-border-width: 1px;
            -fx-focus-color: transparent;
            -fx-faint-focus-color: transparent;
        """

            // Set additional styles to remove blue focus border
            dialogPane.lookup(".content").style = "-fx-background-color: white;"
            dialogPane.lookup(".button-bar").style = "-fx-background-color: white;"

            // Add a style for the header panel to maintain gray color
            dialogPane.lookup(".header-panel").style = """
            -fx-background-color: #2B2D30;
            -fx-background-radius: 0;
        """
        }

        dialog.showAndWait()
    }

    private fun createDetailsSection(title: String, items: List<Pair<String, String>>): VBox {
        return VBox(5.0).apply {
            children.add(Label(title).apply {
                styleClass.add("section-label")
            })

            items.forEach { (label, value) ->
                children.add(
                    HBox(10.0).apply {
                        children.addAll(
                            Label("$label:").apply {
                                styleClass.add("field-label")
                                prefWidth = 150.0
                            },
                            Label(value).apply {
                                styleClass.add("field-label")
                            }
                        )
                    }
                )
            }

            children.add(Separator().apply {
                padding = Insets(10.0, 0.0, 10.0, 0.0)
            })
        }
    }

    private fun setupTableView() {
        VBox.setVgrow(tableView, Priority.ALWAYS)
        tableView.isTableMenuButtonVisible = true
        tableView.prefHeight = Region.USE_COMPUTED_SIZE

        val numeroCol = TableColumn<Map<String, Any>, String>("Nº Pedido")
        numeroCol.setCellValueFactory { data -> javafx.beans.property.SimpleStringProperty(data.value["numero"] as String) }
        numeroCol.prefWidth = 100.0

        val dataCol = TableColumn<Map<String, Any>, String>("Data/Hora")
        dataCol.setCellValueFactory { data -> javafx.beans.property.SimpleStringProperty(data.value["data_hora"] as String) }
        dataCol.prefWidth = 150.0

        val clienteCol = TableColumn<Map<String, Any>, String>("Cliente")
        clienteCol.setCellValueFactory { data -> javafx.beans.property.SimpleStringProperty(data.value["cliente"] as String) }
        clienteCol.prefWidth = 200.0

        val telefoneCol = TableColumn<Map<String, Any>, String>("Telefone")
        telefoneCol.setCellValueFactory { data -> javafx.beans.property.SimpleStringProperty(data.value["telefone"] as String) }
        telefoneCol.prefWidth = 130.0

        val produtosCol = TableColumn<Map<String, Any>, String>("Produtos")
        produtosCol.setCellValueFactory { data ->
            javafx.beans.property.SimpleStringProperty(
                data.value["produtos"] as? String ?: ""
            )
        }
        produtosCol.prefWidth = 300.0

        val totalCol = TableColumn<Map<String, Any>, String>("Total")
        totalCol.setCellValueFactory { data -> javafx.beans.property.SimpleStringProperty(data.value["valor_total"] as String) }
        totalCol.style = "-fx-alignment: CENTER-RIGHT;"
        totalCol.prefWidth = 100.0

        val statusPedidoCol = TableColumn<Map<String, Any>, String>("Status")
        statusPedidoCol.setCellValueFactory { data ->
            javafx.beans.property.SimpleStringProperty(data.value["status_pedido"] as? String ?: "Pendente")
        }
        statusPedidoCol.prefWidth = 140.0
        statusPedidoCol.setCellFactory { column ->
            object : TableCell<Map<String, Any>, String>() {
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        text = null
                        style = ""
                        graphic = null
                    } else {
                        val statusLabel = Label(item).apply {
                            padding = Insets(3.0, 8.0, 3.0, 8.0)
                            style = when (item) {
                                "Pendente" -> "-fx-background-color: #FFF3CD; -fx-text-fill: #856404; -fx-background-radius: 4;"
                                "Preparando" -> "-fx-background-color: #CCE5FF; -fx-text-fill: #004085; -fx-background-radius: 4;"
                                "Em Entrega" -> "-fx-background-color: #E2D9F3; -fx-text-fill: #5A3A7E; -fx-background-radius: 4;"
                                "Concluido" -> "-fx-background-color: #D4EDDA; -fx-text-fill: #155724; -fx-background-radius: 4;"
                                "Cancelado" -> "-fx-background-color: #F8D7DA; -fx-text-fill: #721C24; -fx-background-radius: 4;"
                                else -> ""
                            }
                        }

                        graphic = statusLabel
                        text = null

// Add click handler to cycle through common status progressions
                        setOnMouseClicked { event ->
                            val pedido = tableView.items[index]
                            val pedidoId = pedido["id"] as Long
                            val novoStatus = when (item) {
                                "Pendente" -> "Preparando"
                                "Preparando" -> "Em Entrega"
                                "Em Entrega" -> "Concluido"
                                else -> "Pendente"
                            }

                            // Add confirmation dialog
                            val confirmMessage = "Deseja alterar o status do pedido de '$item' para '$novoStatus'?"
                            if (showConfirmationDialog("Confirmar Alteração", confirmMessage)) {
                                if (controller.atualizarStatusPedido(pedidoId, novoStatus)) {
                                    (pedido as MutableMap<String, Any>)["status_pedido"] = novoStatus
                                    tableView.refresh()

                                    // Remove completed orders
                                    if (novoStatus == "Concluido" || novoStatus == "Cancelado") {
                                        tableView.items.remove(pedido)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val acoesCol = TableColumn<Map<String, Any>, Void>("Ações")
        acoesCol.prefWidth = 290.0
        acoesCol.setCellFactory {
            object : TableCell<Map<String, Any>, Void>() {
                // Helper function to safely load an image or return null
                private fun safeLoadImage(path: String): javafx.scene.image.Image? {
                    val inputStream = javaClass.getResourceAsStream(path)
                    return if (inputStream != null) javafx.scene.image.Image(inputStream) else null
                }

                private val viewButton = Button().apply {
                    // Try to load the image, use text fallback if not available
                    val viewImage = safeLoadImage("/icons/view.png")
                    if (viewImage != null) {
                        graphic = javafx.scene.image.ImageView(viewImage).apply {
                            fitHeight = 20.0
                            fitWidth = 20.0
                        }
                    } else {
                        text = "Ver"
                    }
                    tooltip = Tooltip("Visualizar detalhes")
                    styleClass.add("icon-button")
                    setOnAction {
                        val pedido = tableView.items[index]
                        showOrderDetailsDialog(pedido)
                    }
                }

                private val printButton = Button().apply {
                    val printImage = safeLoadImage("/icons/print.png")
                    if (printImage != null) {
                        graphic = javafx.scene.image.ImageView(printImage).apply {
                            fitHeight = 20.0
                            fitWidth = 20.0
                        }
                    } else {
                        text = "Imprimir"
                    }
                    tooltip = Tooltip("Imprimir pedido")
                    styleClass.add("icon-button")
                    setOnAction {
                        val pedido = tableView.items[index]
                        controller.imprimirPedido(pedido)
                    }
                }

                private val completeButton = Button().apply {
                    val checkImage = safeLoadImage("/icons/check.png")
                    if (checkImage != null) {
                        graphic = javafx.scene.image.ImageView(checkImage).apply {
                            fitHeight = 20.0
                            fitWidth = 20.0
                        }
                    } else {
                        text = "Concluir"
                    }
                    tooltip = Tooltip("Marcar como Concluído")
                    styleClass.add("icon-button")
                    setOnAction {
                        val pedido = tableView.items[index]
                        val pedidoId = pedido["id"] as Long

                        if (controller.atualizarStatusPedido(pedidoId, "Concluido")) {
                            tableView.items.remove(pedido)
                        }
                    }
                }

                override fun updateItem(item: Void?, empty: Boolean) {
                    super.updateItem(item, empty)
                    graphic = if (empty) null else HBox(10.0).apply {
                        children.addAll(viewButton, printButton, completeButton)
                        alignment = Pos.CENTER
                    }
                }
            }
        }

        val pagamentoCol = TableColumn<Map<String, Any>, String>("Pagamento")
        pagamentoCol.setCellValueFactory { data -> javafx.beans.property.SimpleStringProperty(data.value["status"] as String) }
        pagamentoCol.prefWidth = 100.0
        pagamentoCol.setCellFactory { column ->
            object : TableCell<Map<String, Any>, String>() {
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        text = null
                        style = ""
                        graphic = null
                    } else {
                        // Create a clickable label with status indicator
                        val statusLabel = Label(item).apply {
                            padding = Insets(3.0, 8.0, 3.0, 8.0)
                            style = when (item) {
                                "Pendente" -> "-fx-background-color: #FFF3CD; -fx-text-fill: #856404; -fx-background-radius: 4;"
                                "Pago" -> "-fx-background-color: #D4EDDA; -fx-text-fill: #155724; -fx-background-radius: 4;"
                                "Cancelado" -> "-fx-background-color: #F8D7DA; -fx-text-fill: #721C24; -fx-background-radius: 4;"
                                else -> ""
                            }
                        }

                        graphic = statusLabel
                        text = null

                        // Add click handler to cycle through statuses
                        setOnMouseClicked { event ->
                            val pedido = tableView.items[index]
                            val pedidoId = pedido["id"] as Long
                            val novoStatus = when (item) {
                                "Pendente" -> "Pago"
                                "Pago" -> "Pendente"
                                "Cancelado" -> "Pendente"
                                else -> "Pendente"
                            }

                            // Add confirmation dialog
                            val confirmMessage = "Deseja alterar o status de pagamento de '$item' para '$novoStatus'?"
                            if (showConfirmationDialog("Confirmar Alteração", confirmMessage)) {
                                if (controller.atualizarStatusPagamentoPedido(pedidoId, novoStatus)) {
                                    (pedido as MutableMap<String, Any>)["status"] = novoStatus
                                    tableView.refresh()
                                }
                            }
                        }
                    }
                }
            }
        }

        val retiradaCol = TableColumn<Map<String, Any>, String>("Retirada/Entrega")
        retiradaCol.setCellValueFactory { data -> javafx.beans.property.SimpleStringProperty(data.value["retirada"] as String) }
        retiradaCol.prefWidth = 200.0



        tableView.columns.addAll(
            numeroCol,
            statusPedidoCol,
            clienteCol,
            telefoneCol,
            produtosCol,
            totalCol,
            pagamentoCol,
            retiradaCol,
            acoesCol,
            dataCol,
            )

        tableView.columnResizePolicy = TableView.UNCONSTRAINED_RESIZE_POLICY
        tableView.placeholder = Label("Nenhum pedido em andamento")

        val totalColumnWidth = tableView.columns.sumOf { it.prefWidth }
        tableView.prefWidth = totalColumnWidth
    }

    private fun showConfirmationDialog(title: String, message: String): Boolean {
        val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
            this.title = title
            headerText = null
            contentText = message
            dialogPane.styleClass.add("custom-dialog")
        }

        with(alert.dialogPane) {
            lookupAll(".content").forEach { it.styleClass.add("dialog-content") }
            lookupAll(".header-panel").forEach { it.styleClass.add("dialog-header") }
            lookupAll(".button-bar").forEach { it.styleClass.add("dialog-button-bar") }
        }

        Platform.runLater {
            alert.dialogPane.buttonTypes.forEach { buttonType ->
                val button = alert.dialogPane.lookupButton(buttonType)
                button.styleClass.add("dialog-button")
            }
        }

        val result = alert.showAndWait()
        return result.isPresent && result.get() == ButtonType.OK
    }

    private fun createActionBox(): HBox {
        return HBox(10.0).apply {
            alignment = Pos.CENTER_RIGHT
            padding = Insets(10.0, 0.0, 0.0, 0.0)

            val refreshButton = Button("Atualizar").apply {
                styleClass.add("primary-button")
                setOnAction {
                    refreshData()
                }
            }

            children.add(refreshButton)
        }
    }

    private fun refreshData() {
        val dataIni = dataInicial.value
        val dataFim = dataFinal.value
        val busca = searchField.text

        // Get all orders first
        val todosOsPedidos = controller.buscarPedidos(dataIni, dataFim, busca, null)

        // Filter to include only "Preparando", "Pendente", or "Em Entrega" status
        val pedidosEmAndamento = todosOsPedidos.filter { pedido ->
            val status = pedido["status_pedido"] as? String ?: "Pendente"
            status == "Preparando" || status == "Pendente" || status == "Em Entrega"
        }

        // Sort orders by delivery/pickup time
        val pedidosOrdenados = pedidosEmAndamento.sortedBy { pedido ->
            // Parse the delivery/pickup information from "retirada" field
            val retirada = pedido["retirada"] as String
            extractDateTimeForSorting(retirada)
        }

        tableView.items.clear()
        tableView.items.addAll(pedidosOrdenados)
    }

    /**
     * Extracts a sortable datetime from the delivery/pickup text
     * Returns a date string in format "yyyy-MM-dd HH:mm" or "9999-12-31" for entries without dates
     */
    private fun extractDateTimeForSorting(retiradaText: String): String {
        try {
            // Check if it's a delivery
            if (retiradaText.startsWith("Endereço:")) {
                // For deliveries, extract the date and time after the address
                val dateTimePart = retiradaText.substringAfter("Entrega: ", "")
                if (dateTimePart.isNotEmpty()) {
                    return convertToSortableDateTime(dateTimePart)
                }
            } else {
                // For pickups, the format is "Retirada: dd/MM/yyyy às HH:mm"
                val dateTimePart = retiradaText.substringAfter("Retirada: ", "")
                if (dateTimePart.isNotEmpty() && !dateTimePart.contains("Não definida")) {
                    return convertToSortableDateTime(dateTimePart)
                }
            }

            // For pending orders without dates, return a far future date to place them at the end
            return "9999-12-31"
        } catch (e: Exception) {
            // If any error occurs during parsing, return a high value to sort to the end
            return "9999-12-31"
        }
    }

    /**
     * Converts date string from "dd/MM/yyyy às HH:mm" to "yyyy-MM-dd HH:mm" for proper sorting
     */
    private fun convertToSortableDateTime(dateTimeText: String): String {
        try {
            // Extract the date and time parts
            val datePart = dateTimeText.substringBefore(" às ")
            val timePart = dateTimeText.substringAfter(" às ", "00:00")

            // Parse the date
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val parsedDate = LocalDate.parse(datePart, formatter)

            // Return in sortable format
            return "${parsedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)} $timePart"
        } catch (e: Exception) {
            return "9999-12-31"
        }
    }
}
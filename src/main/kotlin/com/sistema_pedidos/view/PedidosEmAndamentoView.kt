package com.sistema_pedidos.view

import com.sistema_pedidos.controller.HistoricoPedidosController
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.StageStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PedidosEmAndamentoView : VBox(10.0) {
    private val controller = HistoricoPedidosController()
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

        val filterBox = createFilterBox()

        setupTableView()

        val actionBox = createActionBox()

        children.addAll(headerBox, filterBox, tableView, actionBox)

        refreshData()
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

            // Show entrega details only if there is delivery
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

        // Configure columns with proper widths
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
        produtosCol.setCellValueFactory { data -> javafx.beans.property.SimpleStringProperty(data.value["produtos"] as? String ?: "") }
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
                    } else {
                        text = item
                        style = when (item) {
                            "Pendente" -> "-fx-text-fill: orange; -fx-font-weight: bold;"
                            "Preparando" -> "-fx-text-fill: blue; -fx-font-weight: bold;"
                            "Em Entrega" -> "-fx-text-fill: purple; -fx-font-weight: bold;"
                            else -> ""
                        }
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
                    } else {
                        text = item
                        style = when (item) {
                            "Pendente" -> "-fx-text-fill: orange; -fx-font-weight: bold;"
                            "Pago" -> "-fx-text-fill: green; -fx-font-weight: bold;"
                            "Cancelado" -> "-fx-text-fill: red; -fx-font-weight: bold;"
                            else -> ""
                        }
                    }
                }
            }
        }

        val retiradaCol = TableColumn<Map<String, Any>, String>("Retirada/Entrega")
        retiradaCol.setCellValueFactory { data -> javafx.beans.property.SimpleStringProperty(data.value["retirada"] as String) }
        retiradaCol.prefWidth = 200.0

        val acoesCol = TableColumn<Map<String, Any>, Void>("Ações")
        acoesCol.prefWidth = 420.0
        acoesCol.setCellFactory {
            object : TableCell<Map<String, Any>, Void>() {
                private val viewButton = Button("Ver").apply {
                    styleClass.add("small-button")
                    prefWidth = 70.0
                    setOnAction {
                        val pedido = tableView.items[index]
                        showOrderDetailsDialog(pedido)
                    }
                }

                private val printButton = Button("Imprimir").apply {
                    styleClass.add("small-button")
                    prefWidth = 120.0
                    setOnAction {
                        val pedido = tableView.items[index]
                        controller.imprimirPedido(pedido)
                    }
                }

                private val statusButton = Button("Atualizar Status").apply {
                    styleClass.add("small-button")
                    prefWidth = 140.0

                    setOnAction {
                        val pedido = tableView.items[index]
                        val pedidoId = pedido["id"] as Long
                        val currentStatus = pedido["status_pedido"] as? String ?: "Pendente"

                        val dialog = Dialog<String>()
                        dialog.title = "Atualizar Status do Pedido"
                        dialog.headerText = "Selecione o novo status do pedido"
                        dialog.initStyle(StageStyle.UNDECORATED)

                        val buttonTypeOk = ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE)
                        val buttonTypeCancel = ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE)
                        dialog.dialogPane.buttonTypes.addAll(buttonTypeOk, buttonTypeCancel)

                        (dialog.dialogPane.lookup(".button-bar") as ButtonBar).apply {
                            buttonOrder = ""
                            buttonMinWidth = 100.0
                            alignment = Pos.CENTER
                            styleClass.add("dialog-button-bar")
                        }

                        val statusOptions = listOf("Concluido", "Preparando", "Pendente", "Em Entrega", "Cancelado")
                        val comboBox = ComboBox<String>().apply {
                            items.addAll(statusOptions)
                            value = currentStatus
                            prefHeight = 36.0
                            prefWidth = 200.0
                        }

                        val content = VBox(10.0).apply {
                            padding = Insets(20.0)
                            spacing = 10.0
                            prefWidth = 400.0
                            styleClass.add("dialog-content")
                            children.addAll(
                                Label("Novo status:").apply {
                                    styleClass.add("field-label")
                                },
                                comboBox
                            )
                        }

                        dialog.dialogPane.stylesheets.addAll(this@PedidosEmAndamentoView.stylesheets)
                        dialog.dialogPane.content = content
                        dialog.dialogPane.styleClass.add("custom-dialog")

                        dialog.setResultConverter { buttonType ->
                            if (buttonType == buttonTypeOk) comboBox.value else null
                        }

                        val result = dialog.showAndWait()

                        result.ifPresent { novoStatus ->
                            if (controller.atualizarStatusPedido(pedidoId, novoStatus)) {
                                (pedido as MutableMap<String, Any>)["status_pedido"] = novoStatus
                                tableView.refresh()

                                // If order is marked as completed or canceled, remove it from the table
                                if (novoStatus == "Concluido" || novoStatus == "Cancelado") {
                                    tableView.items.remove(pedido)
                                }
                            } else {
                                Alert(Alert.AlertType.ERROR).apply {
                                    title = "Erro"
                                    headerText = null
                                    contentText = "Erro ao atualizar o status do pedido"
                                    showAndWait()
                                }
                            }
                        }
                    }
                }

                override fun updateItem(item: Void?, empty: Boolean) {
                    super.updateItem(item, empty)
                    graphic = if (empty) null else box
                }

                private val box = HBox(5.0).apply {
                    children.addAll(viewButton, printButton, statusButton)
                    alignment = Pos.CENTER
                }
            }
        }

        tableView.columns.addAll(
            numeroCol,
            dataCol,
            statusPedidoCol,
            clienteCol,
            telefoneCol,
            produtosCol,
            totalCol,
            pagamentoCol,
            retiradaCol,
            acoesCol
        )

        tableView.columnResizePolicy = TableView.UNCONSTRAINED_RESIZE_POLICY
        tableView.placeholder = Label("Nenhum pedido em andamento")

        val totalColumnWidth = tableView.columns.sumOf { it.prefWidth }
        tableView.prefWidth = totalColumnWidth
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

        tableView.items.clear()
        tableView.items.addAll(pedidosEmAndamento)
    }
}
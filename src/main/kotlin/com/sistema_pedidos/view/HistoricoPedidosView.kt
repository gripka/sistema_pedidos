package com.sistema_pedidos.view

import com.sistema_pedidos.controller.HistoricoPedidosController
import com.sistema_pedidos.controller.PrinterController
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HistoricoPedidosView : VBox(10.0) {
    private val controller = HistoricoPedidosController()
    private val tableView = TableView<Map<String, Any>>()
    private val dataInicial = DatePicker(LocalDate.now().minusDays(7))
    private val dataFinal = DatePicker(LocalDate.now())
    private val searchField = TextField()
    private val statusComboBox = ComboBox<String>()

    init {
        padding = Insets(20.0)
        prefWidth = 1000.0
        prefHeight = 700.0

        styleClass.add("main-container")
        stylesheets.add(javaClass.getResource("/historicopedidosview.css").toExternalForm())

        // Cabeçalho
        val headerBox = createHeaderBox()

        // Filtros
        val filterBox = createFilterBox()

        // Tabela
        setupTableView()

        // Barra de ações
        val actionBox = createActionBox()

        // Layout principal
        children.addAll(headerBox, filterBox, tableView, actionBox)

        // Carrega os dados
        refreshData()
    }

    private fun createHeaderBox(): HBox {
        return HBox(10.0).apply {
            alignment = Pos.CENTER_LEFT

            val titleLabel = Label("Histórico de Pedidos").apply {
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
                        prefHeight = 36.0  // Consistent height
                    }
                )
            }

            val dataFinalBox = VBox(5.0).apply {
                children.addAll(
                    Label("Data Final:").apply {
                        styleClass.add("field-label")
                    },
                    dataFinal.apply {
                        prefHeight = 36.0  // Consistent height
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
                        prefHeight = 36.0  // Consistent height
                    }
                )
            }

            val statusBox = VBox(5.0).apply {
                children.addAll(
                    Label("Pagamento:").apply {
                        styleClass.add("field-label")
                    },
                    statusComboBox.apply {
                        prefHeight = 36.0
                        prefWidth = 150.0
                        items.addAll("Todos", "Pendente", "Pago", "Cancelado")
                        selectionModel.select(0)
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
                        prefHeight = 36.0  // Consistent height
                        prefWidth = 100.0
                        setOnAction {
                            refreshData()
                        }
                    }
                )
            }

            children.addAll(dataInicialBox, dataFinalBox, searchBox, statusBox, filterButtonBox)
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
        produtosCol.prefWidth = 200.0

        val totalCol = TableColumn<Map<String, Any>, String>("Total")
        totalCol.setCellValueFactory { data -> javafx.beans.property.SimpleStringProperty(data.value["valor_total"] as String) }
        totalCol.style = "-fx-alignment: CENTER-RIGHT;"
        totalCol.prefWidth = 100.0

        val statusCol = TableColumn<Map<String, Any>, String>("Pagamento")
        statusCol.setCellValueFactory { data -> javafx.beans.property.SimpleStringProperty(data.value["status"] as String) }
        statusCol.prefWidth = 100.0
        statusCol.setCellFactory { column ->
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
        acoesCol.prefWidth = 180.0
        acoesCol.setCellFactory {
            object : TableCell<Map<String, Any>, Void>() {
                private val viewButton = Button("Ver").apply {
                    styleClass.add("small-button")
                    prefWidth = 70.0
                    setOnAction {
                        val pedido = tableView.items[index]
                        controller.mostrarDetalhesPedido(pedido)
                    }
                }

                private val printButton = Button("Imprimir").apply {
                    styleClass.add("small-button")
                    prefWidth = 70.0
                    setOnAction {
                        val pedido = tableView.items[index]
                        controller.imprimirPedido(pedido)
                    }
                }

                private val box = HBox(5.0).apply {
                    children.addAll(viewButton, printButton)
                    alignment = Pos.CENTER
                }

                override fun updateItem(item: Void?, empty: Boolean) {
                    super.updateItem(item, empty)
                    graphic = if (empty) null else box
                }
            }
        }

        // Add columns in correct order
        tableView.columns.addAll(numeroCol, dataCol, clienteCol, telefoneCol, produtosCol, totalCol, statusCol, retiradaCol, acoesCol)

        // Use unconstrained resize policy for horizontal scrolling
        tableView.columnResizePolicy = TableView.UNCONSTRAINED_RESIZE_POLICY

        // Set placeholder text
        tableView.placeholder = Label("Nenhum pedido encontrado")

        // Ensure table doesn't cause window expansion while keeping columns visible
        val totalColumnWidth = tableView.columns.sumOf { it.prefWidth }
        tableView.prefWidth = totalColumnWidth
    }

    private fun createActionBox(): HBox {
        return HBox(10.0).apply {
            alignment = Pos.CENTER_RIGHT
            padding = Insets(10.0, 0.0, 0.0, 0.0)

            val exportButton = Button("Exportar").apply {
                styleClass.add("secondary-button")
                setOnAction {
                    controller.exportarPedidos(
                        dataInicial.value,
                        dataFinal.value,
                        searchField.text,
                        statusComboBox.selectionModel.selectedItem
                    )
                }
            }

            val refreshButton = Button("Atualizar").apply {
                styleClass.add("primary-button")
                setOnAction {
                    refreshData()
                }
            }

            children.addAll(exportButton, refreshButton)
        }
    }

    private fun refreshData() {
        val dataIni = dataInicial.value
        val dataFim = dataFinal.value
        val busca = searchField.text
        val status = statusComboBox.selectionModel.selectedItem
            .takeIf { it != "Todos" }

        val pedidos = controller.buscarPedidos(dataIni, dataFim, busca, status)
        tableView.items.clear()
        tableView.items.addAll(pedidos)
    }
}
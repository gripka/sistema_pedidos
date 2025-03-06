package com.sistema_pedidos.view

import com.sistema_pedidos.controller.DashboardController
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class DashboardView : BorderPane(), ViewLifecycle {
    private val dashboardController = DashboardController()
    private val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private var dashboardData: Map<String, Any> = emptyMap()

    private val contentBox = VBox(20.0)
    private val scrollPane = ScrollPane()

    init {
        padding = Insets(15.0)
        stylesheets.add(javaClass.getResource("/dashboardview.css").toExternalForm())

        scrollPane.apply {
            content = contentBox
            isFitToWidth = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            styleClass.add("main-scroll-pane")
        }

        center = scrollPane
    }

    override fun refresh() {
        refreshDashboard()
    }

    override fun close() {
        contentBox.children.clear()
        dashboardData = emptyMap()
    }

    private fun refreshDashboard() {
        dashboardData = loadDashboardData()

        contentBox.children.clear()

        createDashboard()
    }

    private fun loadDashboardData(): Map<String, Any> {
        return mapOf(
            "pedidosHoje" to dashboardController.getTotalPedidosHoje(),
            "valorTotalHoje" to dashboardController.getValorTotalHoje(),
            "totalClientes" to dashboardController.getTotalClientes(),
            "entregasRealizadas" to dashboardController.getTotalEntregasRealizadas(),
            "pedidosSemEntrega" to dashboardController.getTotalPedidosSemEntrega(),
            "pedidosCancelados" to dashboardController.getTotalPedidosCancelados(),
            "totalDescontos" to dashboardController.getTotalDescontosAplicados(),
            "produtosMaisVendidos" to dashboardController.getProdutosMaisVendidos(),
            "vendasUltimos7Dias" to dashboardController.getVendasUltimos7Dias(),
            "itensBaixoEstoque" to dashboardController.getItensBaixoEstoque(),
            "pedidosPendentes" to dashboardController.getPedidosPendentes(),

            // New data for additional cards
            "ticketMedio" to dashboardController.getTicketMedio(),
            "produtosCadastrados" to dashboardController.getTotalProdutosCadastrados(),
            "produtosEstoqueBaixo" to dashboardController.getTotalProdutosEstoqueBaixo(),
            "totalEntradasMes" to dashboardController.getTotalEntradasMes(),
            "pedidosConcluidos" to dashboardController.getTotalPedidosConcluidos(),
            "entregasHoje" to dashboardController.getEntregasHoje(),
            "produtosSemEstoque" to dashboardController.getTotalProdutosSemEstoque()
        )
    }

    private fun createDashboard() {
        val contentBox = VBox(20.0)

        // Header with title
        val titleLabel = Label("Dashboard").apply {
            font = Font.font("System", FontWeight.BOLD, 24.0)
            styleClass.add("section-label")
        }

        // Summary Cards - First Row (8 cards)
        val summaryCardsRow1 = createSummaryCardsRow1()

        // Summary Cards - Second Row (6 cards)
        val summaryCardsRow2 = createSummaryCardsRow2()

        // Charts section
        val chartsPane = createCharts()

        // Tables Section
        val tablesPane = createTables()

        contentBox.children.addAll(summaryCardsRow1, summaryCardsRow2, chartsPane, tablesPane)
        val scrollPane = ScrollPane().apply {
            content = contentBox
            isFitToWidth = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            styleClass.add("main-scroll-pane")
        }

        center = scrollPane
    }

    private fun createSummaryCardsRow2(): FlowPane {
        val cardsBox = FlowPane().apply {
            hgap = 15.0
            vgap = 15.0
            alignment = Pos.CENTER
            prefWrapLength = 1000.0
        }

        // Original cards
        val pedidosCard = createInfoCard(
            "Pedidos Hoje",
            (dashboardData["pedidosHoje"] as Int).toString(),
            "#4CAF50"
        )

        val faturamentoCard = createInfoCard(
            "Faturamento Hoje",
            formatter.format(dashboardData["valorTotalHoje"] as Double),
            "#2196F3"
        )

        val entregasCard = createInfoCard(
            "Entregas Realizadas",
            (dashboardData["entregasRealizadas"] as Int).toString(),
            "#9C27B0"
        )

        val semEntregaCard = createInfoCard(
            "Pedidos sem Entrega",
            (dashboardData["pedidosSemEntrega"] as Int).toString(),
            "#FF9800"
        )

        // New cards
        val ticketMedioCard = createInfoCard(
            "Ticket Médio",
            formatter.format(dashboardData["ticketMedio"] as Double),
            "#607D8B" // Blue Grey
        )

        val produtosCard = createInfoCard(
            "Produtos Ativos",
            (dashboardData["produtosCadastrados"] as Int).toString(),
            "#795548" // Brown
        )

        val estoqueBaixoCard = createInfoCard(
            "Produtos Est. Baixo",
            (dashboardData["produtosEstoqueBaixo"] as Int).toString(),
            "#FF5722" // Deep Orange
        )

        val entradasMesCard = createInfoCard(
            "Faturamento do Mês",
            formatter.format(dashboardData["totalEntradasMes"] as Double),
            "#3F51B5" // Indigo
        )

        cardsBox.children.addAll(
            pedidosCard, faturamentoCard, entregasCard, semEntregaCard,
            ticketMedioCard, produtosCard, estoqueBaixoCard, entradasMesCard
        )

        return cardsBox
    }

    private fun createSummaryCardsRow1(): FlowPane {
        val cardsBox = FlowPane().apply {
            hgap = 15.0
            vgap = 15.0
            alignment = Pos.CENTER
            prefWrapLength = 1000.0 // This is valid for FlowPane
        }

        // Original cards
        val canceladosCard = createInfoCard(
            "Pedidos Cancelados",
            (dashboardData["pedidosCancelados"] as Int).toString(),
            "#F44336" // Red
        )

        val descontosCard = createInfoCard(
            "Total Descontos",
            formatter.format(dashboardData["totalDescontos"] as Double),
            "#009688" // Teal
        )

        val clientesCard = createInfoCard(
            "Total de Clientes",
            (dashboardData["totalClientes"] as Int).toString(),
            "#FFC107" // Amber
        )

        // New cards
        val concluidosCard = createInfoCard(
            "Pedidos Concluídos",
            (dashboardData["pedidosConcluidos"] as Int).toString(),
            "#8BC34A" // Light Green
        )

        val entregasHojeCard = createInfoCard(
            "Entregas Hoje",
            (dashboardData["entregasHoje"] as Int).toString(),
            "#673AB7" // Deep Purple
        )

        val semEstoqueCard = createInfoCard(
            "Sem Estoque",
            (dashboardData["produtosSemEstoque"] as Int).toString(),
            "#E91E63" // Pink
        )

        cardsBox.children.addAll(
            canceladosCard, descontosCard, clientesCard,
            concluidosCard, entregasHojeCard, semEstoqueCard
        )

        return cardsBox
    }

    private fun createInfoCard(title: String, value: String, colorHex: String): VBox {
        return VBox(5.0).apply {
            padding = Insets(15.0)
            minWidth = 175.0
            minHeight = 110.0
            alignment = Pos.CENTER
            styleClass.add("info-card")
            style =
                "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 5);"

            children.addAll(
                Label(title).apply {
                    styleClass.add("field-label")
                },
                Label(value).apply {
                    styleClass.add("value-label")
                    style = "-fx-text-fill: $colorHex; -fx-font-size: 22px; -fx-font-weight: bold;"
                    padding = Insets(5.0, 0.0, 0.0, 0.0)
                }
            )
        }
    }

    private fun createCharts(): VBox {
        val chartBox = VBox(15.0)

        // Top products table
        val topProductsContainer = createTopProductsSection()

        // Sales last 7 days chart
        val chartContainer = createSalesChartSection()

        chartBox.children.addAll(topProductsContainer, chartContainer)
        return chartBox
    }

    private fun createTopProductsSection(): VBox {
        val topProductsData = dashboardData["produtosMaisVendidos"] as List<Map<String, Any>>

        val container = VBox(10.0).apply {
            styleClass.add("container-box")
            style =
                "-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 5);"
        }

        val title = Label("Produtos Mais Vendidos").apply {
            styleClass.add("section-label")
        }

        val table = createTopProductsTable(topProductsData)
        table.styleClass.add("table-view")

        container.children.addAll(title, table)
        return container
    }

    private fun createTopProductsTable(products: List<Map<String, Any>>): TableView<Map<String, Any>> {
        val tableView = TableView<Map<String, Any>>()

        val codeColumn = TableColumn<Map<String, Any>, String>("Código").apply {
            prefWidth = 100.0
            setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value["codigo"] as String) }
        }

        val nameColumn = TableColumn<Map<String, Any>, String>("Nome").apply {
            prefWidth = 250.0
            setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value["nome"] as String) }
        }

        val qtyColumn = TableColumn<Map<String, Any>, String>("Qtd. Vendida").apply {
            prefWidth = 110.0
            setCellValueFactory { cellData ->
                javafx.beans.property.SimpleStringProperty((cellData.value["quantidade_vendida"] as Int).toString())
            }
        }

        val valueColumn = TableColumn<Map<String, Any>, String>("Valor Total").apply {
            prefWidth = 120.0
            setCellValueFactory { cellData ->
                javafx.beans.property.SimpleStringProperty(formatter.format(cellData.value["valor_total"] as Double))
            }
        }

        tableView.columns.addAll(codeColumn, nameColumn, qtyColumn, valueColumn)
        tableView.items.addAll(products)
        tableView.prefHeight = 200.0

        return tableView
    }

    private fun createSalesChartSection(): VBox {
        val container = VBox(10.0).apply {
            styleClass.add("container-box")
            style =
                "-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 5);"
        }

        val title = Label("Faturamento - Últimos 7 dias").apply {
            styleClass.add("section-label")
        }

        val chart = createSalesLineChart()
        container.children.addAll(title, chart)

        return container
    }

    private fun createSalesLineChart(): LineChart<String, Number> {
        val vendasData = dashboardData["vendasUltimos7Dias"] as Map<String, Double>

        val xAxis = CategoryAxis()
        val yAxis = NumberAxis()

        xAxis.label = "Data"
        yAxis.label = "Valor (R$)"

        val lineChart = LineChart<String, Number>(xAxis, yAxis).apply {
            title = "Faturamento - Últimos 7 dias"
            animated = true
            createSymbols = true
            isLegendVisible = false
            prefHeight = 300.0
        }

        val series = XYChart.Series<String, Number>()
        series.name = "Faturamento"

        // Add data points for past 7 days
        val today = LocalDate.now()
        for (i in 6 downTo 0) {
            val date = today.minusDays(i.toLong())
            val formattedDate = date.format(dateFormatter)
            val dateKey = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val value = vendasData[dateKey] ?: 0.0
            series.data.add(XYChart.Data(formattedDate, value))
        }

        lineChart.data.add(series)
        return lineChart
    }

    private fun createTables(): HBox {
        val tablesBox = HBox(15.0)

        // Low inventory table
        val lowInventoryItems = dashboardData["itensBaixoEstoque"] as List<Map<String, Any>>
        val lowInventoryContainer = VBox(10.0).apply {
            styleClass.add("container-box")
            style =
                "-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 5);"
            prefWidth = 400.0
            HBox.setHgrow(this, Priority.ALWAYS)
        }

        val lowInventoryTitle = Label("Produtos com Baixo Estoque").apply {
            styleClass.add("section-label")
        }

        val lowInventoryTable = createLowInventoryTable(lowInventoryItems)
        lowInventoryTable.styleClass.add("table-view")
        lowInventoryContainer.children.addAll(lowInventoryTitle, lowInventoryTable)

        // Pending orders table
        val pendingOrders = dashboardData["pedidosPendentes"] as List<Map<String, Any>>
        val pendingOrdersContainer = VBox(10.0).apply {
            styleClass.add("container-box")
            style =
                "-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 5);"
            prefWidth = 400.0
            HBox.setHgrow(this, Priority.ALWAYS)
        }

        val pendingOrdersTitle = Label("Pedidos Pendentes").apply {
            styleClass.add("section-label")
        }

        val pendingOrdersTable = createPendingOrdersTable(pendingOrders)
        pendingOrdersTable.styleClass.add("table-view")
        pendingOrdersContainer.children.addAll(pendingOrdersTitle, pendingOrdersTable)

        tablesBox.children.addAll(lowInventoryContainer, pendingOrdersContainer)
        return tablesBox
    }

    private fun createLowInventoryTable(items: List<Map<String, Any>>): TableView<Map<String, Any>> {
        val tableView = TableView<Map<String, Any>>()

        val codeColumn = TableColumn<Map<String, Any>, String>("Código").apply {
            prefWidth = 90.0
            setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value["codigo"] as String) }
        }

        val nameColumn = TableColumn<Map<String, Any>, String>("Nome").apply {
            prefWidth = 160.0
            setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value["nome"] as String) }
        }

        val currentStockColumn = TableColumn<Map<String, Any>, String>("Est. Atual").apply {
            prefWidth = 70.0
            setCellValueFactory { cellData ->
                javafx.beans.property.SimpleStringProperty((cellData.value["estoque_atual"] as Int).toString())
            }
        }

        val minStockColumn = TableColumn<Map<String, Any>, String>("Est. Mínimo").apply {
            prefWidth = 80.0
            setCellValueFactory { cellData ->
                javafx.beans.property.SimpleStringProperty((cellData.value["estoque_minimo"] as Int).toString())
            }
        }

        tableView.columns.addAll(codeColumn, nameColumn, currentStockColumn, minStockColumn)
        tableView.items.addAll(items)
        tableView.prefHeight = 200.0

        return tableView
    }

    private fun createPendingOrdersTable(orders: List<Map<String, Any>>): TableView<Map<String, Any>> {
        val tableView = TableView<Map<String, Any>>()

        val numberColumn = TableColumn<Map<String, Any>, String>("Número").apply {
            prefWidth = 80.0
            setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value["numero"] as String) }
        }

        val clientColumn = TableColumn<Map<String, Any>, String>("Cliente").apply {
            prefWidth = 180.0
            setCellValueFactory { cellData -> javafx.beans.property.SimpleStringProperty(cellData.value["cliente"] as String) }
        }

        val valueColumn = TableColumn<Map<String, Any>, String>("Valor").apply {
            prefWidth = 100.0
            setCellValueFactory { cellData ->
                javafx.beans.property.SimpleStringProperty(formatter.format(cellData.value["valor_total"] as Double))
            }
        }

        val statusColumn = TableColumn<Map<String, Any>, String>("Status").apply {
            prefWidth = 100.0
            setCellValueFactory { cellData ->
                val status = cellData.value["status_pedido"] as String
                javafx.beans.property.SimpleStringProperty(status)
            }
            setCellFactory { column ->
                object : TableCell<Map<String, Any>, String>() {
                    override fun updateItem(item: String?, empty: Boolean) {
                        super.updateItem(item, empty)

                        if (item == null || empty) {
                            text = null
                            style = ""
                            graphic = null
                        } else {
                            text = item
                            style = when (item) {
                                "Pendente" -> "-fx-text-fill: #856404; -fx-background-color: #fff3cd; -fx-padding: 3px 8px; -fx-background-radius: 4px;"
                                "Preparando" -> "-fx-text-fill: #004085; -fx-background-color: #cce5ff; -fx-padding: 3px 8px; -fx-background-radius: 4px;"
                                "Em Entrega" -> "-fx-text-fill: #155724; -fx-background-color: #d4edda; -fx-padding: 3px 8px; -fx-background-radius: 4px;"
                                "Concluido" -> "-fx-text-fill: #1b5e20; -fx-background-color: #c8e6c9; -fx-padding: 3px 8px; -fx-background-radius: 4px;"
                                else -> ""
                            }
                        }
                    }
                }
            }
        }

        tableView.columns.addAll(numberColumn, clientColumn, valueColumn, statusColumn)
        tableView.items.addAll(orders)
        tableView.prefHeight = 200.0

        return tableView
    }
}
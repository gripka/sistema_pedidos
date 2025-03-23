package com.sistema_pedidos.view

import javafx.animation.*
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Duration
import java.util.concurrent.Executors

class SplashScreen(private val minimumDisplayTimeMs: Long = 2000) {
    private val splashStage = Stage()
    private var showTime: Long = 0
    private val animations = mutableListOf<Animation>()
    private val backgroundExecutor = Executors.newSingleThreadExecutor()

    // Adicionar logging auxiliar
    private fun log(message: String) {
        System.err.println("[SplashScreen][${System.currentTimeMillis()}] $message")
    }

    init {
        log("Iniciando construção da SplashScreen")

        // Componentes da UI
        val logoPlaceholder = StackPane().apply {
            prefHeight = 120.0
            prefWidth = 120.0
            style = "-fx-background-radius: 60; -fx-background-color: rgba(255, 255, 255, 0.1);"
        }

        log("Criando indicador de carregamento")
        val loadingIndicator = createCustomLoader()
        log("Indicador de carregamento criado")

        val label = Label("Inicializando...").apply {
            textFill = Color.WHITE
            style = "-fx-font-size: 14px;"
        }

        val root = VBox(20.0, logoPlaceholder, loadingIndicator, label).apply {
            alignment = Pos.CENTER
            style = "-fx-background-color: #202020; -fx-background-radius: 10;"
            padding = javafx.geometry.Insets(30.0)
        }

        log("Configurando cena")
        val scene = Scene(root, 300.0, 300.0)
        scene.fill = Color.TRANSPARENT

        splashStage.apply {
            initStyle(StageStyle.TRANSPARENT)
            title = "Blossom ERP"
            this.scene = scene
            centerOnScreen()
        }
        log("Cena configurada")

        // Pré-carregar recursos em thread separado - NÃO faça isso no construtor
        log("Configuração inicial da SplashScreen concluída")
    }

    // Separar o carregamento de recursos para ser chamado explicitamente após show()
    fun preloadResources() {
        log("Iniciando pré-carregamento de recursos")
        backgroundExecutor.submit {
            try {
                log("Thread de background iniciada")

                // Carregar recursos pesados
                log("Carregando logo")
                val logoImage = Image("/icons/logo.png", true) // true = background loading
                log("Logo carregado")

                log("Carregando ícone")
                val iconImage = Image("/icons/icon.ico", true) // true = background loading
                log("Ícone carregado")

                Platform.runLater {
                    log("Atualizando UI com recursos carregados")
                    try {
                        // Adicionar ícone à janela
                        splashStage.icons.add(iconImage)

                        // Criar e configurar o logo
                        val logo = ImageView(logoImage).apply {
                            fitHeight = 120.0
                            fitWidth = 120.0
                            isPreserveRatio = true
                            isSmooth = true
                        }

                        // Substituir o placeholder pelo logo real com fade-in
                        val fadeIn = FadeTransition(Duration.millis(300.0), logo)
                        fadeIn.fromValue = 0.0
                        fadeIn.toValue = 1.0

                        val root = splashStage.scene.root as VBox
                        val logoPlaceholder = root.children.find { it is StackPane } as? StackPane
                        if (logoPlaceholder != null) {
                            val index = root.children.indexOf(logoPlaceholder)
                            root.children.remove(logoPlaceholder)
                            root.children.add(index, logo)
                            fadeIn.play()
                        }
                        log("UI atualizada com recursos carregados")
                    } catch (e: Exception) {
                        log("Erro ao atualizar UI: ${e.message}")
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                log("Erro no carregamento de recursos: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun createCustomLoader(): Pane {
        log("Criando componentes do loader")
        val loaderContainer = StackPane()
        loaderContainer.prefWidth = 48.0
        loaderContainer.prefHeight = 48.0

        // Criar círculos de loading com propriedades pré-configuradas
        val outerCircle = Circle(24.0).apply {
            fill = Color.TRANSPARENT
            stroke = Color.WHITE
            strokeWidth = 5.0
            strokeDashArray.addAll(150.0, 150.0)
            strokeDashOffset = 150.0 // Posição inicial do dash
        }

        val innerCircle = Circle(19.0).apply {
            fill = Color.TRANSPARENT
            stroke = Color.web("#FF3D00")
            strokeWidth = 5.0
            strokeDashArray.addAll(120.0, 120.0)
            strokeDashOffset = 0.0 // Posição inicial do dash
        }

        loaderContainer.children.addAll(outerCircle, innerCircle)
        log("Componentes do loader criados")

        log("Configurando animações")
        // Criar animações
        val outerTimeline = Timeline(
            KeyFrame(Duration.ZERO, KeyValue(outerCircle.strokeDashOffsetProperty(), 150.0)),
            KeyFrame(Duration.seconds(2.0), KeyValue(outerCircle.strokeDashOffsetProperty(), 0.0))
        ).apply {
            cycleCount = Animation.INDEFINITE
        }

        val innerTimeline = Timeline(
            KeyFrame(Duration.ZERO, KeyValue(innerCircle.strokeDashOffsetProperty(), 0.0)),
            KeyFrame(Duration.seconds(2.0), KeyValue(innerCircle.strokeDashOffsetProperty(), 240.0))
        ).apply {
            cycleCount = Animation.INDEFINITE
        }

        val rotateTransition = RotateTransition(Duration.seconds(1.0), loaderContainer).apply {
            fromAngle = 0.0
            toAngle = 360.0
            cycleCount = Animation.INDEFINITE
            interpolator = Interpolator.LINEAR
        }

        // Armazenar animações para limpeza posterior
        animations.add(outerTimeline)
        animations.add(innerTimeline)
        animations.add(rotateTransition)
        log("Animações configuradas")

        // Iniciar animações no próximo pulso da UI para evitar bloqueio
        log("Agendando início das animações no próximo pulso")
        Platform.runLater {
            log("Iniciando animações")
            outerTimeline.play()
            innerTimeline.play()
            rotateTransition.play()
            log("Animações iniciadas")
        }

        return loaderContainer
    }

    private fun stopAllAnimations() {
        log("Parando todas as animações")
        animations.forEach { it.stop() }
        animations.clear()
        backgroundExecutor.shutdown()
        log("Todas as animações paradas e recursos liberados")
    }

    fun show() {
        log("Mostrando splash screen")
        showTime = System.currentTimeMillis()
        splashStage.show()
        log("Splash screen visível")

        // Iniciar carregamento de recursos DEPOIS que a UI estiver visível
        preloadResources()
    }

    fun hide(afterHideCallback: () -> Unit = {}) {
        log("Solicitação para esconder splash screen")
        val elapsedTime = System.currentTimeMillis() - showTime
        val remainingTime = (minimumDisplayTimeMs - elapsedTime).coerceAtLeast(0)
        log("Tempo restante para exibição mínima: $remainingTime ms")

        val transition = PauseTransition(Duration.millis(remainingTime.toDouble()))
        transition.setOnFinished {
            log("Iniciando fade-out da splash screen")
            val fadeOut = FadeTransition(Duration.millis(300.0), splashStage.scene.root)
            fadeOut.fromValue = 1.0
            fadeOut.toValue = 0.0
            fadeOut.setOnFinished {
                log("Fade-out concluído, fechando splash screen")
                stopAllAnimations()
                splashStage.close()
                log("Splash screen fechada, chamando callback")
                afterHideCallback()
            }
            fadeOut.play()
        }
        transition.play()
    }
}
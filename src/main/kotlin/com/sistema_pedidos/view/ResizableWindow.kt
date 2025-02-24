package com.sistema_pedidos.view

import javafx.scene.input.MouseEvent
import javafx.scene.layout.Region
import javafx.stage.Stage

class ResizableWindow(private val stage: Stage) {
    private val borderWidth = 5.0

    init {
        val root = stage.scene.root as Region
        root.addEventHandler(MouseEvent.MOUSE_MOVED, this::onMouseMoved)
        root.addEventHandler(MouseEvent.MOUSE_PRESSED, this::onMousePressed)
        root.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onMouseDragged)
    }

    private var xOffset = 0.0
    private var yOffset = 0.0
    private var cursorType: CursorType = CursorType.DEFAULT

    private fun onMouseMoved(event: MouseEvent) {
        val sceneX = event.sceneX
        val sceneY = event.sceneY
        val sceneWidth = stage.scene.width
        val sceneHeight = stage.scene.height

        cursorType = when {
            sceneX < borderWidth && sceneY > sceneHeight - borderWidth -> CursorType.SW_RESIZE
            sceneX > sceneWidth - borderWidth && sceneY > sceneHeight - borderWidth -> CursorType.SE_RESIZE
            sceneX > sceneWidth - borderWidth -> CursorType.E_RESIZE
            sceneY > sceneHeight - borderWidth -> CursorType.S_RESIZE
            else -> CursorType.DEFAULT
        }

        stage.scene.cursor = cursorType.cursor
    }

    private fun onMousePressed(event: MouseEvent) {
        if (cursorType != CursorType.DEFAULT) {
            xOffset = event.sceneX
            yOffset = event.sceneY
        }
    }

    private fun onMouseDragged(event: MouseEvent) {
        if (cursorType == CursorType.DEFAULT) return

        val deltaX = event.sceneX - xOffset
        val deltaY = event.sceneY - yOffset

        when (cursorType) {
            CursorType.SW_RESIZE -> {
                if (stage.width - deltaX >= stage.minWidth) {
                    stage.width -= deltaX
                    stage.x += deltaX
                    xOffset = borderWidth
                }
                if (stage.height + deltaY >= stage.minHeight) {
                    stage.height += deltaY
                    yOffset = stage.height - borderWidth
                }
            }
            CursorType.SE_RESIZE -> {
                if (stage.width + deltaX >= stage.minWidth) {
                    stage.width += deltaX
                    xOffset = stage.width - borderWidth
                }
                if (stage.height + deltaY >= stage.minHeight) {
                    stage.height += deltaY
                    yOffset = stage.height - borderWidth
                }
            }
            CursorType.E_RESIZE -> {
                if (stage.width + deltaX >= stage.minWidth) {
                    stage.width += deltaX
                    xOffset = stage.width - borderWidth
                }
            }
            CursorType.S_RESIZE -> {
                if (stage.height + deltaY >= stage.minHeight) {
                    stage.height += deltaY
                    yOffset = stage.height - borderWidth
                }
            }
            else -> {}
        }
    }

    private enum class CursorType(val cursor: javafx.scene.Cursor) {
        DEFAULT(javafx.scene.Cursor.DEFAULT),
        SW_RESIZE(javafx.scene.Cursor.SW_RESIZE),
        SE_RESIZE(javafx.scene.Cursor.SE_RESIZE),
        E_RESIZE(javafx.scene.Cursor.E_RESIZE),
        S_RESIZE(javafx.scene.Cursor.S_RESIZE)
    }
}
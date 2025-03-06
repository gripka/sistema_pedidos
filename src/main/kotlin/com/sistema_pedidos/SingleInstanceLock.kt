package com.sistema_pedidos

import javafx.application.Platform
import javafx.stage.Stage
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import java.nio.channels.FileLock

object SingleInstanceLock {
    private val LOCK_FILE = File(System.getProperty("user.home"), "blossomerp.lock")
    private const val PORT = 44556
    private var lock: FileLock? = null
    private var channel: FileChannel? = null
    private var serverSocket: ServerSocket? = null
    private var stage: Stage? = null

    fun initialize(primaryStage: Stage): Boolean {
        stage = primaryStage
        return if (lock()) {
            // First instance - start a server to listen for focus requests
            startServer()
            true
        } else {
            // Another instance is running, send focus command and exit
            sendFocusCommand()
            false
        }
    }

    private fun lock(): Boolean {
        return try {
            channel = FileChannel.open(LOCK_FILE.toPath(),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.WRITE)
            lock = channel?.tryLock()
            lock != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun startServer() {
        Thread {
            try {
                serverSocket = ServerSocket(PORT, 10, InetAddress.getLoopbackAddress())
                while (true) {
                    val clientSocket = serverSocket?.accept() ?: break
                    clientSocket.use {
                        // Request to bring window to front
                        Platform.runLater {
                            bringToFront()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    private fun sendFocusCommand() {
        try {
            Socket(InetAddress.getLoopbackAddress(), PORT).close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun bringToFront() {
        stage?.let {
            if (it.isIconified) {
                it.isIconified = false
            }
            it.toFront()
            it.requestFocus()
        }
    }

    fun release() {
        try {
            serverSocket?.close()
            lock?.release()
            channel?.close()
            LOCK_FILE.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
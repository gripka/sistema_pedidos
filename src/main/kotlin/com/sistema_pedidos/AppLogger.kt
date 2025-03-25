package com.sistema_pedidos.util

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date

object AppLogger {
    private val logFile: File by lazy {
        val appDataDir = File(System.getProperty("user.home"), ".blossom-erp")
        if (!appDataDir.exists()) appDataDir.mkdirs()
        File(appDataDir, "application.log")
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

    init {
        // Truncate log file if it's too large (>5MB)
        if (logFile.exists() && logFile.length() > 5 * 1024 * 1024) {
            logFile.writeText("") // Clear contents
        }

        info("------- Application Started -------")
        info("User directory: ${System.getProperty("user.dir")}")
        info("App data directory: ${logFile.parentFile.absolutePath}")
        info("Java version: ${System.getProperty("java.version")}")
        info("App version: ${try {
            VersionChecker().getCurrentVersion()
        } catch (e: Exception) {
            "Error getting version: ${e.message}"
        }}")
    }

    fun info(message: String) {
        log("INFO", message)
    }

    fun error(message: String, exception: Throwable? = null) {
        log("ERROR", message)
        exception?.let {
            synchronized(logFile) {
                try {
                    PrintWriter(FileWriter(logFile, true)).use { writer ->
                        exception.printStackTrace(writer)
                        writer.println()
                    }
                } catch (e: Exception) {
                    // Last resort if logging fails
                    e.printStackTrace()
                }
            }
        }
    }

    private fun log(level: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val logMessage = "[$timestamp] [$level] $message"

        // Print to console and file
        println(logMessage)

        synchronized(logFile) {
            try {
                FileWriter(logFile, true).use { it.write("$logMessage\n") }
            } catch (e: Exception) {
                // Last resort if logging fails
                e.printStackTrace()
            }
        }
    }
}
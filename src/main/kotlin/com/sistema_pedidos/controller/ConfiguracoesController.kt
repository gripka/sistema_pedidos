package com.sistema_pedidos.controller

import java.util.Properties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class ConfiguracoesController {
    private val appProperties = Properties()
    private val configProperties = Properties()
    private val configFile = File(System.getProperty("user.home"), ".blossom-erp/config.properties")
    private var scheduler: ScheduledExecutorService? = null

    // Propriedades de impressão
    var defaultPrinter: String
        get() = configProperties.getProperty("printer.default", "")
        set(value) {
            configProperties.setProperty("printer.default", value)
        }

    var companyName: String
        get() = configProperties.getProperty("printer.company.name", "")
        set(value) {
            configProperties.setProperty("printer.company.name", value)
        }

    var companyCnpj: String
        get() = configProperties.getProperty("printer.company.cnpj", "")
        set(value) {
            configProperties.setProperty("printer.company.cnpj", value)
        }

    var companyPhone: String
        get() = configProperties.getProperty("printer.company.phone", "")
        set(value) {
            configProperties.setProperty("printer.company.phone", value)
        }

    var companyAddress: String
        get() = configProperties.getProperty("printer.company.address", "")
        set(value) {
            configProperties.setProperty("printer.company.address", value)
        }

    // Propriedades da aplicação
    val appVersion: String
        get() = appProperties.getProperty("app.version", "Versão não encontrada")

    val appName: String
        get() = appProperties.getProperty("app.name", "Blossom ERP")

    // Propriedades de backup
    var backupDirectory: String
        get() = configProperties.getProperty("backup.directory",
            System.getProperty("user.home") + File.separator + "Blossom_Backups")
        set(value) {
            configProperties.setProperty("backup.directory", value)
        }

    var autoBackupDaily: Boolean
        get() = configProperties.getProperty("backup.auto.daily", "false").toBoolean()
        set(value) {
            configProperties.setProperty("backup.auto.daily", value.toString())
            if (value) {
                startScheduledBackup()
            } else {
                stopScheduledBackup()
            }
        }

    var lastBackupDate: String
        get() = configProperties.getProperty("backup.last.date", "")
        private set(value) {
            configProperties.setProperty("backup.last.date", value)
        }

    var backupOnExit: Boolean
        get() = configProperties.getProperty("backup.on.exit", "false").toBoolean()
        set(value) {
            configProperties.setProperty("backup.on.exit", value.toString())
        }

    var keepLastFiveBackups: Boolean
        get() = configProperties.getProperty("backup.keep.five", "true").toBoolean()
        set(value) {
            configProperties.setProperty("backup.keep.five", value.toString())
        }

    init {
        loadAppProperties()
        loadConfigProperties()

        // Garantir que o diretório padrão de backup exista
        val defaultBackupDir = File(backupDirectory)
        if (!defaultBackupDir.exists()) {
            defaultBackupDir.mkdirs()
        }

        // Iniciar backup agendado se estiver habilitado
        if (autoBackupDaily) {
            startScheduledBackup()
        }
    }

    // Propriedades de inicialização
    var startWithSystem: Boolean
        get() = configProperties.getProperty("startup.with.system", "false").toBoolean()
        set(value) {
            configProperties.setProperty("startup.with.system", value.toString())
            if (value) {
                createStartupShortcut()
            } else {
                removeStartupShortcut()
            }
        }

    var startMinimized: Boolean
        get() = configProperties.getProperty("startup.minimized", "false").toBoolean()
        set(value) {
            configProperties.setProperty("startup.minimized", value.toString())
        }

    private fun createStartupShortcut() {
        try {
            val startupFolder = File(System.getProperty("user.home") +
                    File.separator + "AppData" + File.separator +
                    "Roaming" + File.separator + "Microsoft" + File.separator +
                    "Windows" + File.separator + "Start Menu" + File.separator +
                    "Programs" + File.separator + "Startup")

            if (!startupFolder.exists()) {
                startupFolder.mkdirs()
            }

            val jarPath = File(ConfiguracoesController::class.java.protectionDomain.codeSource.location.toURI()).absolutePath
            val shortcutFile = File(startupFolder, "BlossomERP.bat")

            val startMinimizedFlag = if (startMinimized) "-minimized" else ""
            val shortcutContent = """
            @echo off
            start javaw -jar "$jarPath" $startMinimizedFlag
        """.trimIndent()

            shortcutFile.writeText(shortcutContent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeStartupShortcut() {
        try {
            val startupFolder = File(System.getProperty("user.home") +
                    File.separator + "AppData" + File.separator +
                    "Roaming" + File.separator + "Microsoft" + File.separator +
                    "Windows" + File.separator + "Start Menu" + File.separator +
                    "Programs" + File.separator + "Startup")

            val shortcutFile = File(startupFolder, "BlossomERP.bat")
            if (shortcutFile.exists()) {
                shortcutFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // In ConfiguracoesController.kt
    var minimizeToTray: Boolean
        get() = configProperties.getProperty("app.minimize.to.tray", "false").toBoolean()
        set(value) {
            configProperties.setProperty("app.minimize.to.tray", value.toString())
        }

    private fun startScheduledBackup() {
        stopScheduledBackup() // Para evitar múltiplas instâncias do scheduler

        scheduler = Executors.newScheduledThreadPool(1)

        // Verificar se é necessário realizar backup agora
        scheduler?.submit { checkAndPerformDailyBackup() }

        // Agendar verificação diária (à meia-noite)
        scheduler?.scheduleAtFixedRate(
            { checkAndPerformDailyBackup() },
            calculateInitialDelay(),
            24, TimeUnit.HOURS
        )
    }

    private fun stopScheduledBackup() {
        scheduler?.shutdown()
        scheduler = null
    }

    private fun calculateInitialDelay(): Long {
        val now = LocalDateTime.now()
        val nextRun = LocalDateTime.of(now.toLocalDate().plusDays(1), java.time.LocalTime.MIDNIGHT)
        return java.time.Duration.between(now, nextRun).seconds
    }

    private fun checkAndPerformDailyBackup() {
        try {
            val today = LocalDate.now().toString()

            // Verificar se já foi feito backup hoje
            if (lastBackupDate != today) {
                println("Realizando backup automático diário...")
                if (performBackup()) {
                    println("Backup automático diário realizado com sucesso!")
                } else {
                    println("Falha ao realizar backup automático diário")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun loadAppProperties() {
        try {
            javaClass.getResourceAsStream("/app.properties")?.use {
                appProperties.load(it)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadConfigProperties() {
        try {
            if (configFile.exists()) {
                FileInputStream(configFile).use {
                    configProperties.load(it)
                }
            } else {
                // Criar diretório para configurações se não existir
                configFile.parentFile.mkdirs()

                // Inicializar com valores padrão
                configProperties.setProperty("backup.directory",
                    System.getProperty("user.home") + File.separator + "Blossom_Backups")
                configProperties.setProperty("backup.auto.daily", "false")
                configProperties.setProperty("backup.on.exit", "false")
                configProperties.setProperty("backup.keep.five", "true")
                configProperties.setProperty("backup.last.date", "")

                saveConfigProperties()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun saveConfigProperties(): Boolean {
        try {
            configFile.parentFile.mkdirs()
            FileOutputStream(configFile).use {
                configProperties.store(it, "Configurações do Blossom ERP")
            }
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    fun resetToDefaults(): Boolean {
        try {
            configProperties.setProperty("backup.directory",
                System.getProperty("user.home") + File.separator + "Blossom_Backups")
            configProperties.setProperty("backup.auto.daily", "false")
            configProperties.setProperty("backup.on.exit", "false")
            configProperties.setProperty("backup.keep.five", "true")
            configProperties.setProperty("backup.last.date", "")

            configProperties.setProperty("printer.default", "")
            configProperties.setProperty("printer.company.name", "")
            configProperties.setProperty("printer.company.cnpj", "")
            configProperties.setProperty("printer.company.phone", "")
            configProperties.setProperty("printer.company.address", "")

            stopScheduledBackup()
            saveConfigProperties()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun performBackup(): Boolean {
        try {
            val backupDir = File(backupDirectory)
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val dbDir = File(System.getProperty("user.home") + File.separator +
                    "AppData" + File.separator + "Local" + File.separator +
                    "BlossomERP")
            val databaseFile = File(dbDir, "database.db")

            if (!databaseFile.exists()) {
                throw IOException("Banco de dados não encontrado em: ${databaseFile.absolutePath}")
            }

            val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
            val backupFileName = "backup_${timestamp}.zip"
            val backupFile = File(backupDir, backupFileName)

            FileOutputStream(backupFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    val dbEntry = ZipEntry(databaseFile.name)
                    zos.putNextEntry(dbEntry)
                    Files.copy(databaseFile.toPath(), zos)
                    zos.closeEntry()

                    if (configFile.exists()) {
                        val configEntry = ZipEntry("config.properties")
                        zos.putNextEntry(configEntry)
                        Files.copy(configFile.toPath(), zos)
                        zos.closeEntry()
                    }
                }
            }

            lastBackupDate = LocalDate.now().toString()
            saveConfigProperties()

            if (keepLastFiveBackups) {
                cleanupOldBackups(backupDir)
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun cleanupOldBackups(backupDir: File) {
        val backupFiles = backupDir.listFiles { file ->
            file.isFile && file.name.startsWith("backup_") && file.name.endsWith(".zip")
        }

        if (backupFiles != null && backupFiles.size > 5) {
            // Ordena arquivos por data de modificação (mais antigos primeiro)
            backupFiles.sortBy { it.lastModified() }

            // Remove os arquivos mais antigos, mantendo apenas os 5 mais recentes
            for (i in 0 until backupFiles.size - 5) {
                backupFiles[i].delete()
            }
        }
    }

    // Métodos para mostrar alertas

    fun showBackupConfirmationAlert(): Boolean {
        val alert = Alert(Alert.AlertType.CONFIRMATION)
        alert.title = "Confirmar Backup"
        alert.headerText = "Realizar backup do sistema"
        alert.contentText = "Deseja realizar um backup agora? Isso pode levar alguns instantes."

        val result = alert.showAndWait()
        return result.isPresent && result.get() == ButtonType.OK
    }

    fun showSaveConfigConfirmationAlert(): Boolean {
        val alert = Alert(Alert.AlertType.CONFIRMATION)
        alert.title = "Salvar Configurações"
        alert.headerText = "Salvar configurações de backup"
        alert.contentText = "Deseja salvar as alterações nas configurações de backup?"

        val result = alert.showAndWait()
        return result.isPresent && result.get() == ButtonType.OK
    }

    fun showResetConfirmationAlert(): Boolean {
        val alert = Alert(Alert.AlertType.CONFIRMATION)
        alert.title = "Redefinir Configurações"
        alert.headerText = "Redefinir para configurações padrão"
        alert.contentText = "Deseja redefinir todas as configurações para os valores padrão? Esta ação não pode ser desfeita."

        val result = alert.showAndWait()
        return result.isPresent && result.get() == ButtonType.OK
    }

    fun showBackupSuccessAlert() {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = "Backup Concluído"
        alert.headerText = "Backup do banco de dados realizado com sucesso"
        alert.contentText = "O backup foi salvo em:\n$backupDirectory"
        alert.showAndWait()
    }

    fun showBackupErrorAlert() {
        val alert = Alert(Alert.AlertType.ERROR)
        alert.title = "Erro de Backup"
        alert.headerText = "Falha ao realizar backup"
        alert.contentText = "Ocorreu um erro ao realizar o backup. Verifique as permissões do diretório e tente novamente."
        alert.showAndWait()
    }

    fun showSaveSuccessAlert() {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = "Configurações Salvas"
        alert.headerText = "Configurações salvas com sucesso"
        alert.contentText = "Suas configurações de backup foram salvas."
        alert.showAndWait()
    }

    fun showSaveErrorAlert() {
        val alert = Alert(Alert.AlertType.ERROR)
        alert.title = "Erro ao Salvar"
        alert.headerText = "Falha ao salvar configurações"
        alert.contentText = "Ocorreu um erro ao salvar as configurações. Verifique as permissões do sistema e tente novamente."
        alert.showAndWait()
    }

    companion object {
        private var instanceBackupScheduler: ScheduledExecutorService? = null

        fun backupOnExitIfEnabled() {
            try {
                val controller = ConfiguracoesController()
                if (controller.backupOnExit) {
                    println("Realizando backup ao sair do aplicativo...")
                    controller.performBackup()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun initializeDailyBackup() {
            try {
                val controller = ConfiguracoesController()
                if (controller.autoBackupDaily) {
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
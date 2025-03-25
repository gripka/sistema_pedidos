package com.sistema_pedidos.util

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.Properties
import com.sistema_pedidos.Main
import com.sistema_pedidos.util.AppLogger
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class VersionChecker {
    private val FALLBACK_VERSION = "0.8.2"

    var progressCallback: ((Double) -> Unit)? = null
    //progressCallback?.invoke(progress / 100.0)

    private val trustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .sslSocketFactory(createSSLSocketFactory(), trustManager)
        .hostnameVerifier { _, _ -> true }
        .build()

    private val releasesUrl = "https://api.github.com/repos/gripka/sistema_pedidos/releases"

    var onStatusUpdate: ((String) -> Unit)? = null

    private val isPackagedApp by lazy {
        val userDir = System.getProperty("user.dir")
        AppLogger.info("Checking if packaged app. User dir: $userDir")
        val isPackaged = File(userDir).listFiles()
            ?.any { it.name.endsWith(".exe") && (it.name.contains("blossom", ignoreCase = true) ||
                    it.name.contains("erp", ignoreCase = true)) } ?: false
        AppLogger.info("Is packaged app: $isPackaged")
        isPackaged
    }

    fun getCurrentVersion(): String {
        try {
            AppLogger.info("Getting current version...")

            val fromResource = loadVersionFromResource()
            if (fromResource != "0.0.0") {
                AppLogger.info("Version from resource: $fromResource")
                return fromResource
            }

            AppLogger.info("Using fallback version: $FALLBACK_VERSION")
            return FALLBACK_VERSION
        } catch (e: Exception) {
            AppLogger.error("Error loading version", e)
            return FALLBACK_VERSION
        }
    }

    private fun createSSLSocketFactory(): SSLSocketFactory {
        try {
            val sslContext = SSLContext.getInstance("TLSv1.2")
            sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
            return sslContext.socketFactory
        } catch (e: Exception) {
            AppLogger.error("SSL initialization error", e)
            throw RuntimeException(e)
        }
    }

    private fun loadVersionFromResource(): String {
        val methods = listOf(
            { Main::class.java.getResourceAsStream("/app.properties") },
            { javaClass.getResourceAsStream("/app.properties") },
            { javaClass.classLoader.getResourceAsStream("app.properties") },
            { ClassLoader.getSystemResourceAsStream("app.properties") }
        )

        for ((index, method) in methods.withIndex()) {
            try {
                val stream = method() ?: continue
                val properties = Properties()
                properties.load(stream)
                stream.close()
                val version = properties.getProperty("app.version", "0.0.0")
                AppLogger.info("Loaded version from method $index: $version")
                return version
            } catch (e: Exception) {
                AppLogger.info("Method $index failed: ${e.message}")
            }
        }

        return "0.0.0"
    }

    fun getLatestVersion(): Pair<String, String>? {
        try {
            AppLogger.info("Checking for updates from GitHub...")
            onStatusUpdate?.invoke("Verificando atualizações...")

            val request = Request.Builder()
                .url(releasesUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 403) {
                    AppLogger.error("GitHub API rate limit exceeded")
                    onStatusUpdate?.invoke("API GitHub: Limite de requisições atingido")
                    return null
                }

                if (!response.isSuccessful) {
                    AppLogger.error("GitHub API error: ${response.code} - ${response.message}")
                    onStatusUpdate?.invoke("Erro API GitHub: ${response.code}")
                    return null
                }

                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {
                    AppLogger.error("Empty response from GitHub API")
                    return null
                }

                AppLogger.info("GitHub response: ${responseBody.take(200)}...")

                val jsonArray = JSONArray(responseBody)
                if (jsonArray.length() == 0) {
                    AppLogger.info("No releases found")
                    return null
                }

                val latestRelease = jsonArray.getJSONObject(0)
                val tagName = latestRelease.getString("tag_name").removePrefix("v")
                AppLogger.info("Latest tag: $tagName")

                val assets = latestRelease.getJSONArray("assets")
                AppLogger.info("Found ${assets.length()} assets")

                if (assets.length() == 0) {
                    AppLogger.info("No assets found in release")
                    return null
                }

                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name")
                    val url = asset.getString("browser_download_url")
                    AppLogger.info("Asset $i: $name ($url)")
                }

                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name").lowercase()
                    AppLogger.info("Checking asset: $name")

                    if (name.endsWith(".exe") && (
                                name.contains("blossom") ||
                                        name.contains("erp") ||
                                        name.contains("sistema_pedidos")
                                )) {
                        val downloadUrl = asset.getString("browser_download_url")
                        AppLogger.info("Found update file: $name ($downloadUrl)")
                        return Pair(tagName, downloadUrl)
                    }
                }

                AppLogger.info("No update file found in assets")
                return null
            }
        } catch (e: Exception) {
            AppLogger.error("Error checking for updates", e)
            onStatusUpdate?.invoke("Erro de rede: ${e.message}")
            return null
        }
    }

    fun downloadAndInstallUpdate(downloadUrl: String): Boolean {
        try {
            AppLogger.info("Starting update download: $downloadUrl")
            onStatusUpdate?.invoke("Baixando atualização...")

            val tempDir = File(System.getProperty("java.io.tmpdir"), "blossom_updates")
            if (!tempDir.exists() && !tempDir.mkdirs()) {
                AppLogger.error("Could not create temp directory for update")
                return false
            }

            val installerFile = File(tempDir, "BlossomERP_Update.exe")
            AppLogger.info("Downloading to: ${installerFile.absolutePath}")

            val request = Request.Builder()
                .url(downloadUrl)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    AppLogger.error("Failed to download update: ${response.code} - ${response.message}")
                    onStatusUpdate?.invoke("Erro ao baixar atualização")
                    return false
                }

                val responseBody = response.body ?: return false

                onStatusUpdate?.invoke("Salvando arquivo de atualização...")
                FileOutputStream(installerFile).use { fileOutputStream ->
                    responseBody.byteStream().use { inputStream ->
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        val totalBytes = responseBody.contentLength()
                        var bytesDownloaded: Long = 0

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            fileOutputStream.write(buffer, 0, bytesRead)
                            bytesDownloaded += bytesRead

                            if (totalBytes > 0) {
                                val progress = bytesDownloaded.toDouble() / totalBytes.toDouble()
                                progressCallback?.invoke(progress)
                                onStatusUpdate?.invoke("Baixando atualização: ${(progress * 100).toInt()}%")
                            }
                        }
                    }
                }
            }

            if (installerFile.exists()) {
                AppLogger.info("Executing installer: ${installerFile.absolutePath}")
                progressCallback?.invoke(0.0)
                onStatusUpdate?.invoke("Instalando atualização...")

                // Use /passive instead of /quiet to show progress bar do instalador
                val installCommand = "\"${installerFile.absolutePath}\" /quiet /norestart"
                AppLogger.info("Install command: $installCommand")

                // Inicia thread de progresso artificial antes de iniciar a instalação
                val progressThread = Thread {
                    try {
                        for (progress in 1..100) {
                            // Ajusta velocidade da barra de progresso
                            val delay = if (progress < 30) 50L else if (progress < 70) 100L else 200L
                            Thread.sleep(delay)
                            val progressValue = progress / 100.0
                            progressCallback?.invoke(progressValue)
                            onStatusUpdate?.invoke("Instalando atualização: $progress%")
                        }
                    } catch (e: Exception) {
                        AppLogger.error("Erro ao atualizar progresso", e)
                    }
                }
                progressThread.start()

                // Inicia o processo de instalação
                val process = ProcessBuilder("cmd", "/c", installCommand)
                    .redirectErrorStream(true)
                    .start()

                // Monitor installation output
                process.inputStream.bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        AppLogger.info("Installer: $line")
                    }
                }

                // Wait for the installer to complete
                val exitCode = process.waitFor()
                AppLogger.info("Installer exited with code: $exitCode")

                if (exitCode == 0 || exitCode == 3010) {
                    // Get path to the installed application
                    val appDataPath = System.getProperty("user.home") + "\\AppData\\Local\\Blossom ERP"
                    val exePath = "$appDataPath\\Blossom ERP.exe"

                    // Wait briefly to ensure installation completes file operations
                    Thread.sleep(1000)

                    // Launch the updated application
                    AppLogger.info("Launching updated application: $exePath")
                    ProcessBuilder(exePath).start()

                    return true // Signal the main app to exit
                }

                return exitCode == 0 || exitCode == 3010 // 3010 means success but requires restart
            } else {
                AppLogger.error("Installer file was not created correctly")
                return false
            }
        } catch (e: Exception) {
            AppLogger.error("Error downloading or installing update", e)
            onStatusUpdate?.invoke("Erro na atualização: ${e.message}")
            return false
        }
    }

    private fun logEnvironmentInfo() {
        AppLogger.info("=== Environment Information ===")
        AppLogger.info("User directory: ${System.getProperty("user.dir")}")
        AppLogger.info("Is packaged app: $isPackagedApp")
        AppLogger.info("Java version: ${System.getProperty("java.version")}")
        AppLogger.info("Classpath: ${System.getProperty("java.class.path")}")

        val workingDir = File(System.getProperty("user.dir"))
        AppLogger.info("Working directory exists: ${workingDir.exists()}")
        AppLogger.info("Working directory is directory: ${workingDir.isDirectory}")
        AppLogger.info("Working directory can read: ${workingDir.canRead()}")

        val files = workingDir.listFiles()
        if (files == null) {
            AppLogger.info("Working directory cannot be listed")
        } else {
            AppLogger.info("Working directory files: ${files.joinToString(", ") { it.name }}")
        }

        // Log resources
        val resourcePaths = listOf(
            "/app.properties",
            "app.properties",
            "/com/sistema_pedidos/app.properties",
            "com/sistema_pedidos/app.properties"
        )

        for (path in resourcePaths) {
            try {
                val resource = javaClass.getResourceAsStream(path)
                AppLogger.info("Resource $path exists: ${resource != null}")
                resource?.close()
            } catch (e: Exception) {
                AppLogger.info("Error checking resource $path: ${e.message}")
            }
        }
    }

    fun isUpdateAvailable(): Triple<Boolean, String, String?> {
        AppLogger.info("=== Version Check Started ===")
        logEnvironmentInfo()

        try {
            // Get current version
            val currentVersion = getCurrentVersion()
            AppLogger.info("Current version: $currentVersion")

            // Get latest version from GitHub
            val latestVersionInfo = getLatestVersion()
            if (latestVersionInfo == null) {
                AppLogger.info("Failed to get latest version")
                return Triple(false, currentVersion, null)
            }

            val (latestVersion, downloadUrl) = latestVersionInfo
            AppLogger.info("Latest version: $latestVersion, URL: $downloadUrl")

            // Compare versions
            val comparisonResult = compareVersions(latestVersion, currentVersion)
            AppLogger.info("Comparison result: $comparisonResult")

            val isNewer = comparisonResult > 0
            return Triple(isNewer, latestVersion, if (isNewer) downloadUrl else null)
        } catch (e: Exception) {
            AppLogger.error("Critical error in isUpdateAvailable", e)
            return Triple(false, getCurrentVersion(), null)
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        AppLogger.info("Comparing versions: '$v1' vs '$v2'")

        try {
            val parts1 = v1.split('.').map { it.toIntOrNull() ?: 0 }
            val parts2 = v2.split('.').map { it.toIntOrNull() ?: 0 }

            for (i in 0 until maxOf(parts1.size, parts2.size)) {
                val p1 = parts1.getOrNull(i) ?: 0
                val p2 = parts2.getOrNull(i) ?: 0
                AppLogger.info("Segment $i: $p1 vs $p2")
                if (p1 != p2) return p1.compareTo(p2)
            }

            return 0
        } catch (e: Exception) {
            AppLogger.error("Error comparing versions", e)
            // If comparison fails, assume versions are equal
            return 0
        }
    }
}
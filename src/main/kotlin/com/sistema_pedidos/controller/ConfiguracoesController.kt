package com.sistema_pedidos.controller

import java.util.Properties
import java.io.IOException

class ConfiguracoesController {
    private val appProperties = Properties()

    val appVersion: String
        get() = appProperties.getProperty("app.version", "Versão não encontrada")

    val appName: String
        get() = appProperties.getProperty("app.name", "Blossom ERP")

    init {
        loadAppProperties()
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
}
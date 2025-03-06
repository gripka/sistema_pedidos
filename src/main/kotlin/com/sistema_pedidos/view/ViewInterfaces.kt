package com.sistema_pedidos.view

interface Refreshable {

    fun refresh()
}


interface ViewLifecycle : Refreshable, AutoCloseable {

    override fun close() {
    }
}
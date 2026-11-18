package com.rojas.centrotareas.data

import kotlinx.coroutines.delay
import java.io.IOException

class TareasRepositoryLocal : TareasRepository {

    // Carga simulada: tarda 2 segundos
    override suspend fun cargarPerfil(): String {
        delay(2000)
        return "Ana Torres (ficticia)"
    }

    // Carga simulada: tarda 3 segundos y puede fallar si se pide
    override suspend fun cargarPedidos(simularError: Boolean): List<String> {
        delay(3000)
        if (simularError) throw IOException("Sin conexión")
        return listOf("Pedido 101", "Pedido 102", "Pedido 103")
    }

    override suspend fun contarPrimos(hasta: Int): Int {
        // TODO Paso 5
        return 0
    }
}

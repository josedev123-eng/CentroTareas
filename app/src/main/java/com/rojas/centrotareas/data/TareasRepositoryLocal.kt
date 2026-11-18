package com.rojas.centrotareas.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
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

    // Cálculo pesado: se hace en Dispatchers.Default para no bloquear el hilo principal
    override suspend fun contarPrimos(hasta: Int, onProgreso: (Float) -> Unit): Int = withContext(Dispatchers.Default) {
        var cantidad = 0
        var porcentajeAnterior = -1

        for (n in 2..hasta) {
            // Si cancelaron la corrutina, aquí se detiene el ciclo
            ensureActive()
            if (esPrimo(n)) {
                cantidad++
            }

            // Calcula el porcentaje entero actual (0 a 100)
            val porcentajeActual = (n.toLong() * 100 / hasta).toInt()

            // Solo notifica cuando cambia el porcentaje entero (1 %, 2 %, ...)
            if (porcentajeActual != porcentajeAnterior) {
                porcentajeAnterior = porcentajeActual
                onProgreso(porcentajeActual / 100f)
            }
        }
        cantidad
    }

    // Un número es primo si ningún d entre 2 y su raíz lo divide
    private fun esPrimo(n: Int): Boolean {
        var d = 2
        // d * d <= n es lo mismo que d <= raíz de n (con Long para no desbordar)
        while (d.toLong() * d <= n) {
            if (n % d == 0) return false
            d++
        }
        return true
    }
}

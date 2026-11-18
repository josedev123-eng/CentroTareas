package com.rojas.centrotareas.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rojas.centrotareas.data.TareasRepository
import com.rojas.centrotareas.model.CargaUi
import com.rojas.centrotareas.model.PrimosUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TareasViewModel(private val repo: TareasRepository) : ViewModel() {

    // ----- Tarea con progreso (pasos 1 a 4)
    private val _mensaje = MutableStateFlow("Listo")
    val mensaje: StateFlow<String> = _mensaje.asStateFlow()

    // Progreso de la tarea, de 0f a 1f. Afuera solo se puede leer
    private val _progreso = MutableStateFlow(0f)
    val progreso: StateFlow<Float> = _progreso.asStateFlow()

    // Guarda la corrutina de la tarea para poder cancelarla
    private var trabajo: Job? = null

    // true mientras la tarea está trabajando
    private val _corriendo = MutableStateFlow(false)
    val corriendo: StateFlow<Boolean> = _corriendo.asStateFlow()

    // La tarea larga corre en una corrutina: el hilo principal queda libre
    fun tareaLarga() {
        // Si ya está corriendo no lanzo otra
        if (_corriendo.value) return

        _corriendo.value = true
        trabajo = viewModelScope.launch {
            try {
                _progreso.value = 0f
                _mensaje.value = "Trabajando..."
                Log.d("HILO", "Antes: ${Thread.currentThread().name}")

                // El trabajo se hace en un hilo de fondo (Dispatchers.Default):
                // 100 vueltas de 40 ms, en total unos 4 segundos
                withContext(Dispatchers.Default) {
                    Log.d("HILO", "Dentro: ${Thread.currentThread().name}")
                    for (i in 1..100) {
                        delay(40)
                        _progreso.value = i / 100f
                    }
                }

                // Al salir de withContext volvemos al hilo principal
                Log.d("HILO", "Despues: ${Thread.currentThread().name}")
                _mensaje.value = "Terminó"
            } finally {
                // Termine bien o la cancelen, deja de estar corriendo
                _corriendo.value = false
            }
        }
    }

    // Detiene la tarea a mitad de camino. La barra se queda donde estaba
    fun cancelar() {
        trabajo?.cancel()
        _mensaje.value = "Cancelado"
    }


    // ----- Contar primos (paso 5)
    private val _primos = MutableStateFlow<PrimosUi>(PrimosUi.Inicial)
    val primos: StateFlow<PrimosUi> = _primos.asStateFlow()

    // Guarda la corrutina del cálculo para poder cancelarlo
    private var trabajoPrimos: Job? = null

    fun contar(hasta: Int) {
        // Si ya está calculando no lanzo otro cálculo
        if (_primos.value is PrimosUi.Calculando) return

        _primos.value = PrimosUi.Calculando
        trabajoPrimos = viewModelScope.launch {
            val inicio = System.currentTimeMillis()
            try {
                val cantidad = repo.contarPrimos(hasta)
                val milisegundos = System.currentTimeMillis() - inicio
                _primos.value = PrimosUi.Listo(cantidad, milisegundos)
            } catch (e: CancellationException) {
                _primos.value = PrimosUi.Cancelado
                // Se vuelve a lanzar para que la cancelación siga funcionando
                throw e
            }
        }
    }

    fun cancelarPrimos() {
        trabajoPrimos?.cancel()
    }

    // ----- Dos cargas (pasos 6 y 7)
    private val _carga = MutableStateFlow<CargaUi>(CargaUi.Inicial)
    val carga: StateFlow<CargaUi> = _carga.asStateFlow()
    // TODO Paso 6: fun cargarUnaPorUna() y fun cargarALaVez()
    // TODO Paso 7: simularError y manejo de errores
}

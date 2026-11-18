package com.rojas.centrotareas.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rojas.centrotareas.data.TareasRepository
import com.rojas.centrotareas.model.CargaUi
import com.rojas.centrotareas.model.PrimosUi
import kotlinx.coroutines.Dispatchers
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

    // La tarea larga corre en una corrutina: el hilo principal queda libre
    fun tareaLarga() {
        viewModelScope.launch {
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
        }
    }

    // TODO Paso 4: trabajo (Job), corriendo y cancelar()

    // ----- Contar primos (paso 5)
    private val _primos = MutableStateFlow<PrimosUi>(PrimosUi.Inicial)
    val primos: StateFlow<PrimosUi> = _primos.asStateFlow()
    // TODO Paso 5: fun contar(hasta: Int) y fun cancelarPrimos()

    // ----- Dos cargas (pasos 6 y 7)
    private val _carga = MutableStateFlow<CargaUi>(CargaUi.Inicial)
    val carga: StateFlow<CargaUi> = _carga.asStateFlow()
    // TODO Paso 6: fun cargarUnaPorUna() y fun cargarALaVez()
    // TODO Paso 7: simularError y manejo de errores
}

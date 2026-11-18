package com.rojas.centrotareas.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rojas.centrotareas.data.TareasRepository
import com.rojas.centrotareas.model.CargaUi
import com.rojas.centrotareas.model.PrimosUi
import com.rojas.centrotareas.model.RegistroTiempo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

        _primos.value = PrimosUi.Calculando(0f)
        trabajoPrimos = viewModelScope.launch {
            val inicio = System.currentTimeMillis()
            try {
                val cantidad = repo.contarPrimos(hasta) { progreso ->
                    _primos.value = PrimosUi.Calculando(progreso)
                }
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

    // Interruptor para simular que los pedidos fallan
    private val _simularError = MutableStateFlow(false)
    val simularError: StateFlow<Boolean> = _simularError.asStateFlow()

    fun cambiarSimularError(valor: Boolean) {
        _simularError.value = valor
    }

    // ----- Comparación de tiempos
    private val _registros = MutableStateFlow<List<RegistroTiempo>>(emptyList())
    val registros: StateFlow<List<RegistroTiempo>> = _registros.asStateFlow()

    // Promedios: null cuando ese modo todavía no tiene datos
    private val _promedioUnaPorUna = MutableStateFlow<Long?>(null)
    val promedioUnaPorUna: StateFlow<Long?> = _promedioUnaPorUna.asStateFlow()

    private val _promedioALaVez = MutableStateFlow<Long?>(null)
    val promedioALaVez: StateFlow<Long?> = _promedioALaVez.asStateFlow()

    private val _mensajeDiferencia = MutableStateFlow("")
    val mensajeDiferencia: StateFlow<String> = _mensajeDiferencia.asStateFlow()

    // Borra la lista de tiempos y reinicia los promedios
    fun limpiarTabla() {
        _registros.value = emptyList()
        recalcularPromedios(emptyList())
    }

    // Agrega una medición exitosa y recalcula los promedios
    private fun guardarRegistro(modo: String, milisegundos: Long) {
        val nuevaLista = _registros.value + RegistroTiempo(modo, milisegundos)
        _registros.value = nuevaLista
        recalcularPromedios(nuevaLista)
    }

    // Calcula los promedios y el mensaje de diferencia
    private fun recalcularPromedios(lista: List<RegistroTiempo>) {
        val tiemposUnaPorUna = lista.filter { it.modo == "Una por una" }.map { it.milisegundos }
        val tiemposALaVez = lista.filter { it.modo == "A la vez" }.map { it.milisegundos }

        // Si un modo no tiene datos su promedio es null (no 0, porque 0 ms parecería un tiempo real)
        val promUna = if (tiemposUnaPorUna.isNotEmpty()) tiemposUnaPorUna.average().toLong() else null
        val promVez = if (tiemposALaVez.isNotEmpty()) tiemposALaVez.average().toLong() else null

        _promedioUnaPorUna.value = promUna
        _promedioALaVez.value = promVez

        if (promUna != null && promVez != null) {
            val diff = promUna - promVez
            _mensajeDiferencia.value = if (diff >= 0) {
                "A la vez fue $diff ms más rápido en promedio"
            } else {
                "Una por una fue ${-diff} ms más rápido en promedio"
            }
        } else {
            _mensajeDiferencia.value = ""
        }
    }

    // Cantidad máxima de intentos: se define en un solo lugar y viaja en el estado
    private val MAX_INTENTOS = 3

    // Función auxiliar para reintentar hasta MAX_INTENTOS veces si la carga falla
    private fun ejecutarCarga(bloque: suspend () -> CargaUi.Exito) {
        // Si ya está cargando no lanzo otra carga
        if (_carga.value is CargaUi.Cargando) return

        viewModelScope.launch {
            var ultimoError: Exception? = null
            for (intento in 1..MAX_INTENTOS) {
                // Notifica a la UI en qué intento va
                _carga.value = CargaUi.Cargando(intento, MAX_INTENTOS)

                // Entre intentos espera un tiempo creciente (1 s para intento 2, 2 s para intento 3)
                if (intento > 1) {
                    delay((intento - 1) * 1000L)
                }

                try {
                    val resultado = bloque()
                    _carga.value = resultado
                    // Guarda el tiempo registrado solo si la carga fue exitosa
                    guardarRegistro(resultado.modo, resultado.milisegundos)
                    return@launch // Si la carga tuvo éxito, termina
                } catch (e: Exception) {
                    // La cancelación NO se reintenta: se relanza siempre
                    if (e is CancellationException) throw e
                    ultimoError = e
                }
            }
            // Si después de todos los intentos sigue fallando, muestra el error
            // y le avisa al usuario que la app ya reintentó sola
            _carga.value = CargaUi.Error(
                "No se pudo cargar después de $MAX_INTENTOS intentos: " +
                    (ultimoError?.message ?: "Error desconocido")
            )
        }
    }

    // Carga una por una: primero el perfil (2 s) y después los pedidos (3 s) = unos 5 s
    fun cargarUnaPorUna() {
        ejecutarCarga {
            val inicio = System.currentTimeMillis()
            val perfil = repo.cargarPerfil()
            val pedidos = repo.cargarPedidos(_simularError.value)
            val milisegundos = System.currentTimeMillis() - inicio
            CargaUi.Exito("Una por una", perfil, pedidos.size, milisegundos)
        }
    }

    // Carga a la vez: las dos llamadas empiezan juntas con async = unos 3 s (la más lenta)
    fun cargarALaVez() {
        ejecutarCarga {
            val inicio = System.currentTimeMillis()
            val (perfil, pedidos) = coroutineScope {
                val perfilAsync = async { repo.cargarPerfil() }
                val pedidosAsync = async { repo.cargarPedidos(_simularError.value) }
                // await espera a que cada una termine
                Pair(perfilAsync.await(), pedidosAsync.await())
            }
            val milisegundos = System.currentTimeMillis() - inicio
            CargaUi.Exito("A la vez", perfil, pedidos.size, milisegundos)
        }
    }

}

package com.luis.taximetro

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.luis.taximetro.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Estados del taxímetro según manual EM-900
    private enum class Estado {
        INACTIVO, // Taxímetro apagado - pantalla muestra "INACTIVO"
        LIBRE, // Activo pero sin carrera - LEDs traseros encendidos
        OCUPADO, // En carrera - GPS activo, contando distancia/tiempo
        PAGAR // Mostrando total a pagar
    }

    private var estadoActual = Estado.INACTIVO

    // Tarifas
    private val BAJADA_BANDERA = 450
    private val COSTO_FICHA = 190
    private val TIEMPO_FICHA_MS = 60000L // 60 segundos
    private val METROS_POR_FICHA = 200.0 // Cada 200 metros cobra una ficha

    // Parámetros EM-900
    private val VELOCIDAD_MINIMA_KMH = 3.0
    private val VELOCIDAD_MAXIMA_KMH = 120.0

    // Variables de negocio
    private var precioTotal = 0
    private var tiempoDetenidoSegundos = 0
    private var tiempoEnEsperaMs = 0L
    private var ultimaTiempoEspera = 0L
    private var distanciaRecorrida = 0.0 // en metros
    private var velocidadActualKmh = 0.0

    // GPS
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var lastLocation: Location? = null
    private var trackingGPS = false

    // Handlers para actualizaciones
    private val handlerReloj = Handler(Looper.getMainLooper())
    private val handlerTiempo = Handler(Looper.getMainLooper())
    private val handlerAlertaVelo = Handler(Looper.getMainLooper())
    private var mostrandoAlertaVelo = false

    // Runnables
    private val actualizarReloj =
            object : Runnable {
                override fun run() {
                    actualizarFechaHora()
                    handlerReloj.postDelayed(this, 1000)
                }
            }

    private val actualizarTiempoEspera =
            object : Runnable {
                override fun run() {
                    if (estadoActual == Estado.OCUPADO) {
                        // Solo cuenta como detenido si velocidad < mínima
                        if (velocidadActualKmh < VELOCIDAD_MINIMA_KMH) {
                            tiempoEnEsperaMs += 1000
                            tiempoDetenidoSegundos = (tiempoEnEsperaMs / 1000).toInt()

                            // Cada 60 segundos detenido, agregar ficha
                            if (tiempoEnEsperaMs - ultimaTiempoEspera >= TIEMPO_FICHA_MS) {
                                precioTotal += COSTO_FICHA
                                ultimaTiempoEspera = tiempoEnEsperaMs
                                actualizarPrecio()
                            }

                            actualizarTiempoDetenido()
                        } else {
                            // Si se está moviendo, resetear tiempo de espera
                            if (tiempoEnEsperaMs > 0) {
                                tiempoEnEsperaMs = 0
                                ultimaTiempoEspera = 0
                            }
                        }
                    }
                    handlerTiempo.postDelayed(this, 1000)
                }
            }

    private val parpadeoAlerta =
            object : Runnable {
                override fun run() {
                    if (mostrandoAlertaVelo) {
                        binding.tvAlertaVelocidad.visibility =
                                if (binding.tvAlertaVelocidad.visibility == View.VISIBLE) View.GONE
                                else View.VISIBLE
                        handlerAlertaVelo.postDelayed(this, 500)
                    }
                }
            }

    // Launcher para permisos de ubicación
    private val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    permissions ->
                val fineLocationGranted =
                        permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                val coarseLocationGranted =
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

                if (fineLocationGranted || coarseLocationGranted) {
                    // Permisos otorgados, GPS ya está inicializado
                    Toast.makeText(this, "Permisos de ubicación otorgados", Toast.LENGTH_SHORT)
                            .show()
                } else {
                    Toast.makeText(
                                    this,
                                    "Se requieren permisos de ubicación para usar el taxímetro",
                                    Toast.LENGTH_LONG
                            )
                            .show()
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inicializarGPS()
        inicializarUI()
        configurarBotones()
        iniciarReloj()
        solicitarPermisosUbicacion()
    }

    private fun inicializarGPS() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback =
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        locationResult.lastLocation?.let { location -> actualizarConGPS(location) }
                    }
                }
    }

    private fun solicitarPermisosUbicacion() {
        when {
            hayPermisosUbicacion() -> {
                // Ya tenemos permisos
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Mostrar explicación antes de solicitar
                AlertDialog.Builder(this)
                        .setTitle("Permisos de Ubicación")
                        .setMessage(
                                "Esta aplicación requiere acceso a GPS para calcular distancias y velocidad real durante el viaje."
                        )
                        .setPositiveButton("Aceptar") { _, _ -> solicitarPermisos() }
                        .setNegativeButton("Cancelar", null)
                        .show()
            }
            else -> {
                solicitarPermisos()
            }
        }
    }

    private fun hayPermisosUbicacion(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun solicitarPermisos() {
        requestPermissionLauncher.launch(
                arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                )
        )
    }

    private fun iniciarActualizacionesGPS() {
        if (!hayPermisosUbicacion()) {
            Toast.makeText(this, "No hay permisos de ubicación", Toast.LENGTH_SHORT).show()
            return
        }

        val locationRequest =
                LocationRequest.Builder(
                                Priority.PRIORITY_HIGH_ACCURACY,
                                1000 // Actualizar cada 1 segundo
                        )
                        .apply {
                            setMinUpdateIntervalMillis(500)
                            setMaxUpdateDelayMillis(2000)
                        }
                        .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            )
            trackingGPS = true
        } catch (e: SecurityException) {
            Toast.makeText(this, "Error al iniciar GPS: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun detenerActualizacionesGPS() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        trackingGPS = false
        lastLocation = null
    }

    private fun actualizarConGPS(location: Location) {
        if (estadoActual != Estado.OCUPADO) return

        // Actualizar velocidad (convertir m/s a km/h)
        velocidadActualKmh = (location.speed * 3.6)
        actualizarVelocidad()

        // Detectar exceso de velocidad
        if (velocidadActualKmh > VELOCIDAD_MAXIMA_KMH) {
            mostrarAlertaVelocidad()
        } else {
            ocultarAlertaVelocidad()
        }

        // Calcular distancia si hay ubicación previa
        lastLocation?.let { prevLocation ->
            val distancia = location.distanceTo(prevLocation) // en metros

            // Solo acumular distancia si velocidad >= mínima
            if (velocidadActualKmh >= VELOCIDAD_MINIMA_KMH) {
                distanciaRecorrida += distancia
                calcularCostoPorDistancia()
                actualizarDistancia()
            }
        }

        lastLocation = location
    }

    private fun calcularCostoPorDistancia() {
        // Calcular fichas por distancia recorrida
        val fichasPorDistancia = (distanciaRecorrida / METROS_POR_FICHA).toInt()
        val costoDistancia = fichasPorDistancia * COSTO_FICHA

        // Calcular fichas por tiempo detenido
        val fichasPorTiempo = (ultimaTiempoEspera / TIEMPO_FICHA_MS).toInt()
        val costoTiempo = fichasPorTiempo * COSTO_FICHA

        precioTotal = BAJADA_BANDERA + costoDistancia + costoTiempo
        actualizarPrecio()
    }

    private fun mostrarAlertaVelocidad() {
        if (!mostrandoAlertaVelo) {
            mostrandoAlertaVelo = true
            handlerAlertaVelo.post(parpadeoAlerta)
        }
    }

    private fun ocultarAlertaVelocidad() {
        mostrandoAlertaVelo = false
        handlerAlertaVelo.removeCallbacks(parpadeoAlerta)
        binding.tvAlertaVelocidad.visibility = View.GONE
    }

    private fun inicializarUI() {
        // Inicializar en estado INACTIVO
        cambiarEstado(Estado.INACTIVO)
        actualizarFechaHora()
    }

    private fun configurarBotones() {
        // Botón 1: Depende del estado
        binding.btnInicio.setOnClickListener {
            when (estadoActual) {
                Estado.OCUPADO -> pausarTiempoEspera()
                else -> {}
            }
        }

        // Botón 2: Control (no usado actualmente)
        binding.btnControl.setOnClickListener {
            // Placeholder para futuras funciones
        }

        // Botón 3: Activar/Desactivar/Fin
        binding.btnFin.setOnClickListener {
            when (estadoActual) {
                Estado.INACTIVO -> activarTaximetro()
                Estado.LIBRE -> iniciarCarrera()
                Estado.OCUPADO -> finalizarCarrera()
                Estado.PAGAR -> {}
            }
        }

        // Botón 4: Menú
        binding.btnMenu.setOnClickListener { toggleMenu() }
    }

    private fun activarTaximetro() {
        // Cambiar de INACTIVO a LIBRE
        cambiarEstado(Estado.LIBRE)
        Toast.makeText(this, "Taxímetro activado", Toast.LENGTH_SHORT).show()
    }

    private fun desactivarTaximetro() {
        // Volver a INACTIVO desde LIBRE
        cambiarEstado(Estado.INACTIVO)
        Toast.makeText(this, "Taxímetro desactivado", Toast.LENGTH_SHORT).show()
    }

    private fun iniciarCarrera() {
        if (!hayPermisosUbicacion()) {
            Toast.makeText(
                            this,
                            "Se requieren permisos de ubicación para iniciar carrera",
                            Toast.LENGTH_LONG
                    )
                    .show()
            solicitarPermisosUbicacion()
            return
        }

        // Cambiar a estado OCUPADO
        cambiarEstado(Estado.OCUPADO)

        // Aplicar bajada de bandera
        precioTotal = BAJADA_BANDERA
        actualizarPrecio()

        // Resetear contadores
        velocidadActualKmh = 0.0
        tiempoDetenidoSegundos = 0
        tiempoEnEsperaMs = 0
        ultimaTiempoEspera = 0
        distanciaRecorrida = 0.0
        lastLocation = null

        // Iniciar GPS y timers
        iniciarActualizacionesGPS()
        handlerTiempo.post(actualizarTiempoEspera)

        actualizarVelocidad()
        actualizarTiempoDetenido()
        actualizarDistancia()

        Toast.makeText(
                        this,
                        "Carrera iniciada - Bajada de bandera: $$BAJADA_BANDERA",
                        Toast.LENGTH_SHORT
                )
                .show()
    }

    private fun pausarTiempoEspera() {
        // Función placeholder para botón "s/Tiempo"
        // Podría implementarse para pausar el contador de tiempo
        Toast.makeText(this, "Función s/Tiempo", Toast.LENGTH_SHORT).show()
    }

    private fun finalizarCarrera() {
        // Detener GPS y handlers
        detenerActualizacionesGPS()
        handlerTiempo.removeCallbacks(actualizarTiempoEspera)
        ocultarAlertaVelocidad()

        // Mostrar diálogo de impresión
        mostrarDialogoImpresion()
    }

    private fun mostrarDialogoImpresion() {
        val distanciaKm = distanciaRecorrida / 1000.0

        val builder = AlertDialog.Builder(this)
        builder.setTitle("IMPRIMIENDO...")
        builder.setMessage(
                """
            =============================
            TAXÍMETRO EKO MAIKO EM-900
            =============================
            
            Bajada de Bandera: $ $BAJADA_BANDERA
            Costo Ficha: $ $COSTO_FICHA
            
            Distancia: ${String.format("%.2f", distanciaKm)} km
            Tiempo detenido: ${String.format("%02d:%02d", tiempoDetenidoSegundos / 60, tiempoDetenidoSegundos % 60)}
            
            TOTAL A PAGAR: $ $precioTotal
            
            =============================
            Gracias por su preferencia
            =============================
        """.trimIndent()
        )

        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            resetearTaximetro()
        }

        builder.setCancelable(false)
        builder.show()
    }

    private fun resetearTaximetro() {
        // Volver a estado LIBRE
        cambiarEstado(Estado.LIBRE)

        // Resetear valores
        precioTotal = 0
        velocidadActualKmh = 0.0
        tiempoDetenidoSegundos = 0
        tiempoEnEsperaMs = 0
        ultimaTiempoEspera = 0
        distanciaRecorrida = 0.0
        lastLocation = null

        // Actualizar UI
        actualizarPrecio()
        actualizarVelocidad()
        actualizarTiempoDetenido()
        actualizarDistancia()
    }

    private fun toggleMenu() {
        if (binding.menuSuperpuesto.visibility == View.VISIBLE) {
            binding.menuSuperpuesto.visibility = View.GONE
        } else {
            binding.menuSuperpuesto.visibility = View.VISIBLE
        }

        // Cerrar menú al hacer clic en él
        binding.menuSuperpuesto.setOnClickListener {
            binding.menuSuperpuesto.visibility = View.GONE
        }
    }

    private fun cambiarEstado(nuevoEstado: Estado) {
        estadoActual = nuevoEstado

        when (nuevoEstado) {
            Estado.INACTIVO -> {
                // Mostrar mensaje central "TAXIMETRO INACTIVO"
                binding.tvMensajeInactivo.visibility = View.VISIBLE
                binding.zonaPrincipal.visibility = View.GONE

                // LEDs apagados
                setLEDColor(binding.ledVerde, Color.parseColor("#333333"))
                setLEDColor(binding.ledRojo, Color.parseColor("#333333"))
                setLEDColor(binding.ledAzul, Color.parseColor("#333333"))

                // Semáforo apagado
                setLEDColor(binding.semaforoVerde, Color.parseColor("#333333"))
                setLEDColor(binding.semaforoRojo, Color.parseColor("#333333"))
            }
            Estado.LIBRE -> {
                // Ocultar mensaje inactivo, mostrar pantalla normal
                binding.tvMensajeInactivo.visibility = View.GONE
                binding.zonaPrincipal.visibility = View.VISIBLE

                // LEDs: Verde encendido (LEDs traseros)
                setLEDColor(binding.ledVerde, Color.GREEN)
                setLEDColor(binding.ledRojo, Color.parseColor("#333333"))
                setLEDColor(binding.ledAzul, Color.parseColor("#333333"))

                // Semáforo: Verde encendido
                setLEDColor(binding.semaforoVerde, Color.GREEN)
                setLEDColor(binding.semaforoRojo, Color.parseColor("#333333"))
            }
            Estado.OCUPADO -> {
                // Pantalla normal visible
                binding.tvMensajeInactivo.visibility = View.GONE
                binding.zonaPrincipal.visibility = View.VISIBLE

                // LEDs: Rojo encendido, apagar traseros verdes
                setLEDColor(binding.ledRojo, Color.RED)
                setLEDColor(binding.ledVerde, Color.parseColor("#333333"))
                setLEDColor(binding.ledAzul, Color.parseColor("#333333"))

                // Semáforo: Rojo encendido
                setLEDColor(binding.semaforoRojo, Color.RED)
                setLEDColor(binding.semaforoVerde, Color.parseColor("#333333"))
            }
            Estado.PAGAR -> {
                // LEDs: Azul encendido
                setLEDColor(binding.ledAzul, Color.BLUE)
                setLEDColor(binding.ledRojo, Color.parseColor("#333333"))
                setLEDColor(binding.ledVerde, Color.parseColor("#333333"))
            }
        }

        actualizarBotones()
    }

    private fun actualizarBotones() {
        when (estadoActual) {
            Estado.INACTIVO -> {
                binding.btnInicio.isEnabled = false
                binding.btnInicio.text = ""
                binding.btnControl.isEnabled = false
                binding.btnControl.text = ""
                binding.btnFin.text = "Activar"
                binding.btnFin.isEnabled = true
                binding.btnMenu.isEnabled = false
                binding.btnMenu.text = ""
            }
            Estado.LIBRE -> {
                binding.btnInicio.text = "Inicio"
                binding.btnInicio.isEnabled = true
                binding.btnControl.text = "Control"
                binding.btnControl.isEnabled = false
                binding.btnFin.text = "Inicio"
                binding.btnFin.isEnabled = true
                binding.btnMenu.text = "Menú"
                binding.btnMenu.isEnabled = true
            }
            Estado.OCUPADO -> {
                binding.btnInicio.text = "s/Tiempo"
                binding.btnInicio.isEnabled = true
                binding.btnControl.text = ""
                binding.btnControl.isEnabled = false
                binding.btnFin.text = "Fin/Imp"
                binding.btnFin.isEnabled = true
                binding.btnMenu.text = "Menú"
                binding.btnMenu.isEnabled = true
            }
            Estado.PAGAR -> {
                // Deshabilitar todos durante pago
                binding.btnInicio.isEnabled = false
                binding.btnControl.isEnabled = false
                binding.btnFin.isEnabled = false
                binding.btnMenu.isEnabled = false
            }
        }
    }

    private fun setLEDColor(view: View, color: Int) {
        val drawable = ContextCompat.getDrawable(this, R.drawable.led_circle)?.mutate()
        drawable?.setTint(color)
        view.background = drawable
    }

    private fun iniciarReloj() {
        handlerReloj.post(actualizarReloj)
    }

    private fun actualizarFechaHora() {
        val ahora = Calendar.getInstance()
        val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formatoHora = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        binding.tvFecha.text = formatoFecha.format(ahora.time)
        binding.tvHora.text = formatoHora.format(ahora.time)
    }

    private fun actualizarPrecio() {
        binding.tvPrecio.text = precioTotal.toString()
    }

    private fun actualizarVelocidad() {
        binding.tvVelocidad.text = "${String.format("%.0f", velocidadActualKmh)} km/h"
    }

    private fun actualizarTiempoDetenido() {
        val minutos = tiempoDetenidoSegundos / 60
        val segundos = tiempoDetenidoSegundos % 60
        binding.tvTiempoDetenido.text = String.format("%02d:%02d", minutos, segundos)
    }

    private fun actualizarDistancia() {
        if (distanciaRecorrida < 1000) {
            binding.tvDistanciaRecorrida.text = "${String.format("%.0f", distanciaRecorrida)} m"
        } else {
            val km = distanciaRecorrida / 1000.0
            binding.tvDistanciaRecorrida.text = "${String.format("%.2f", km)} km"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar handlers y GPS
        handlerReloj.removeCallbacks(actualizarReloj)
        handlerTiempo.removeCallbacks(actualizarTiempoEspera)
        handlerAlertaVelo.removeCallbacks(parpadeoAlerta)
        if (trackingGPS) {
            detenerActualizacionesGPS()
        }
    }
}

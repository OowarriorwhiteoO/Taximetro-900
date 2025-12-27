package com.luis.taximetro

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.luis.taximetro.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    
    // Estados del taxímetro
    private enum class Estado {
        LIBRE,
        OCUPADO,
        PAGAR
    }
    
    private var estadoActual = Estado.LIBRE
    
    // Tarifas
    private val BAJADA_BANDERA = 450
    private val COSTO_FICHA = 190
    private val TIEMPO_FICHA_MS = 60000L // 60 segundos
    
    // Variables de negocio
    private var precioTotal = 0
    private var velocidadActual = 0
    private var tiempoDetenidoSegundos = 0
    private var tiempoEnEsperaMs = 0L
    private var ultimaTiempoEspera = 0L
    
    // Handlers para actualizaciones
    private val handlerReloj = Handler(Looper.getMainLooper())
    private val handlerTiempo = Handler(Looper.getMainLooper())
    private val handlerMovimiento = Handler(Looper.getMainLooper())
    
    // Runnables
    private val actualizarReloj = object : Runnable {
        override fun run() {
            actualizarFechaHora()
            handlerReloj.postDelayed(this, 1000)
        }
    }
    
    private val actualizarTiempoEspera = object : Runnable {
        override fun run() {
            if (estadoActual == Estado.OCUPADO && velocidadActual == 0) {
                tiempoEnEsperaMs += 1000
                tiempoDetenidoSegundos = (tiempoEnEsperaMs / 1000).toInt()
                
                // Cada 60 segundos, agregar ficha
                if (tiempoEnEsperaMs - ultimaTiempoEspera >= TIEMPO_FICHA_MS) {
                    precioTotal += COSTO_FICHA
                    ultimaTiempoEspera = tiempoEnEsperaMs
                    actualizarPrecio()
                }
                
                actualizarTiempoDetenido()
            }
            handlerTiempo.postDelayed(this, 1000)
        }
    }
    
    private val simularMovimiento = object : Runnable {
        override fun run() {
            if (estadoActual == Estado.OCUPADO && velocidadActual > 0) {
                // Simular aumento de precio por distancia (aleatorio)
                val incremento = (10..30).random()
                precioTotal += incremento
                actualizarPrecio()
            }
            handlerMovimiento.postDelayed(this, 3000) // Cada 3 segundos
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        inicializarUI()
        configurarBotones()
        iniciarReloj()
    }
    
    private fun inicializarUI() {
        // Inicializar en estado LIBRE
        cambiarEstado(Estado.LIBRE)
        actualizarFechaHora()
    }
    
    private fun configurarBotones() {
        // Botón 1: Inicio
        binding.btnInicio.setOnClickListener {
            when (estadoActual) {
                Estado.LIBRE -> iniciarCarrera()
                else -> Toast.makeText(this, "Ya hay una carrera en curso", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Botón 2: Control (Simula movimiento)
        binding.btnControl.setOnClickListener {
            if (estadoActual == Estado.OCUPADO) {
                toggleMovimiento()
            } else {
                Toast.makeText(this, "Debe iniciar una carrera primero", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Botón 3: Fin
        binding.btnFin.setOnClickListener {
            when (estadoActual) {
                Estado.OCUPADO -> finalizarCarrera()
                else -> Toast.makeText(this, "No hay carrera activa", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Botón 4: Menú
        binding.btnMenu.setOnClickListener {
            toggleMenu()
        }
    }
    
    private fun iniciarCarrera() {
        // Cambiar a estado OCUPADO
        cambiarEstado(Estado.OCUPADO)
        
        // Aplicar bajada de bandera
        precioTotal = BAJADA_BANDERA
        actualizarPrecio()
        
        // Resetear contadores
        velocidadActual = 0
        tiempoDetenidoSegundos = 0
        tiempoEnEsperaMs = 0
        ultimaTiempoEspera = 0
        
        // Iniciar timers
        handlerTiempo.post(actualizarTiempoEspera)
        handlerMovimiento.post(simularMovimiento)
        
        actualizarVelocidad()
        actualizarTiempoDetenido()
        
        Toast.makeText(this, "Carrera iniciada - Bajada de bandera: $$BAJADA_BANDERA", Toast.LENGTH_SHORT).show()
    }
    
    private fun toggleMovimiento() {
        velocidadActual = if (velocidadActual == 0) {
            45 // Simular velocidad de 45 km/h
        } else {
            0 // Detenido
        }
        
        if (velocidadActual == 0) {
            // Al detenerse, resetear tiempo de espera para próxima ficha
            tiempoEnEsperaMs = 0
            ultimaTiempoEspera = 0
        }
        
        actualizarVelocidad()
    }
    
    private fun finalizarCarrera() {
        // Detener todos los handlers
        handlerTiempo.removeCallbacks(actualizarTiempoEspera)
        handlerMovimiento.removeCallbacks(simularMovimiento)
        
        // Mostrar diálogo de impresión
        mostrarDialogoImpresion()
    }
    
    private fun mostrarDialogoImpresion() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("IMPRIMIENDO...")
        builder.setMessage("""
            =============================
            TAXÍMETRO EKO MAIKO EM-900
            =============================
            
            Bajada de Bandera: $ $BAJADA_BANDERA
            Costo Parking: $ $COSTO_FICHA
            
            TOTAL A PAGAR: $ $precioTotal
            
            =============================
            Gracias por su preferencia
            =============================
        """.trimIndent())
        
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
        velocidadActual = 0
        tiempoDetenidoSegundos = 0
        tiempoEnEsperaMs = 0
        ultimaTiempoEspera = 0
        
        // Actualizar UI
        actualizarPrecio()
        actualizarVelocidad()
        actualizarTiempoDetenido()
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
            Estado.LIBRE -> {
                // LEDs: Verde encendido, Rojo y Azul apagados
                setLEDColor(binding.ledVerde, Color.GREEN)
                setLEDColor(binding.ledRojo, Color.parseColor("#333333"))
                setLEDColor(binding.ledAzul, Color.parseColor("#333333"))
                
                // Semáforo: Verde encendido
                setLEDColor(binding.semaforoVerde, Color.GREEN)
                setLEDColor(binding.semaforoRojo, Color.parseColor("#333333"))
            }
            
            Estado.OCUPADO -> {
                // LEDs: Rojo encendido, Verde y Azul apagados
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
        binding.tvVelocidad.text = "$velocidadActual km/h"
    }
    
    private fun actualizarTiempoDetenido() {
        val minutos = tiempoDetenidoSegundos / 60
        val segundos = tiempoDetenidoSegundos % 60
        binding.tvTiempoDetenido.text = String.format("%02d:%02d", minutos, segundos)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Limpiar handlers
        handlerReloj.removeCallbacks(actualizarReloj)
        handlerTiempo.removeCallbacks(actualizarTiempoEspera)
        handlerMovimiento.removeCallbacks(simularMovimiento)
    }
}
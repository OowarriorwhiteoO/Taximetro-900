package com.luis.taximetro

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

/** Extensiones de funcionalidad de menú para MainActivity */
class MenuManager(private val activity: AppCompatActivity) {

    private var textToSpeech: TextToSpeech? = null
    private var ttsInited = false
    private lateinit var dataManager: TripDataManager

    // Estado actual del menú
    private enum class MenuState {
        HIDDEN,
        MAIN_MENU,
        TAXI_SUBMENU,
        MAP_SUBMENU,
        TOTALES_SUBMENU,
        CONFIG_SUBMENU
    }

    private var currentMenuState = MenuState.HIDDEN

    fun inicializar() {
        dataManager = TripDataManager(activity)
        inicializarTTS()
    }

    private fun inicializarTTS() {
        textToSpeech =
                TextToSpeech(activity) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        textToSpeech?.language = Locale("es", "AR") // Español argentino
                        ttsInited = true
                    }
                }
    }

    fun destruir() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }

    // ============ MENU PRINCIPAL ============

    fun mostrarMenuPrincipal() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("EKO MAIKO EM-900")

        val opciones =
                arrayOf(
                        "🚕 1. Taxi",
                        "🗺️ 2. Mapa en Vivo",
                        "📊 3. Ver Totales",
                        "⚙️ 4. Configuración"
                )

        builder.setItems(opciones) { dialog, which ->
            when (which) {
                0 -> mostrarMenuTaxi()
                1 -> mostrarMenuMapa()
                2 -> mostrarMenuTotales()
                3 -> mostrarMenuConfiguracion()
            }
        }

        builder.setNegativeButton("Cerrar", null)
        builder.show()
    }

    // ============ MENU 1: TAXI ============

    private fun mostrarMenuTaxi() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("🚕 Menú Taxi")

        val opciones =
                arrayOf("1.1 Inicio", "1.2 Hora (Anunciar)", "1.3 Activar Taxímetro", "1.4 Volver")

        builder.setItems(opciones) { dialog, which ->
            when (which) {
                0 -> mostrarInicio()
                1 -> anunciarHora()
                2 -> activarTaximetroManual()
                3 -> mostrarMenuPrincipal()
            }
        }

        builder.setNegativeButton("Atrás") { _, _ -> mostrarMenuPrincipal() }
        builder.show()
    }

    private fun mostrarInicio() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Inicio")
        builder.setMessage(
                """
            EKO MAIKO EM-900
            Taxímetro Digital
            
            Versión: 1.0
            Estado: Listo para operar
            
            Presione el botón "Inicio" para comenzar una carrera.
        """.trimIndent()
        )
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            mostrarMenuTaxi()
        }
        builder.show()
    }

    private fun anunciarHora() {
        if (!ttsInited) {
            Toast.makeText(activity, "Text-to-Speech no está disponible", Toast.LENGTH_SHORT).show()
            return
        }

        val calendario = Calendar.getInstance()
        val hora = calendario.get(Calendar.HOUR_OF_DAY)
        val minutos = calendario.get(Calendar.MINUTE)

        val textoHora = "Son las $hora horas con $minutos minutos"
        textToSpeech?.speak(textoHora, TextToSpeech.QUEUE_FLUSH, null, null)

        Toast.makeText(activity, "🔊 $textoHora", Toast.LENGTH_SHORT).show()
    }

    private fun activarTaximetroManual() {
        Toast.makeText(
                        activity,
                        "Use el botón 'Activar' en la pantalla principal",
                        Toast.LENGTH_SHORT
                )
                .show()
    }

    // ============ MENU 2: MAPA ============

    private fun mostrarMenuMapa() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("🗺️ Mapa en Vivo")

        val opciones =
                arrayOf(
                        "2.1 Mostrar ruta en Google Maps",
                        "2.2 Tracking visual del viaje",
                        "2.3 Volver"
                )

        builder.setItems(opciones) { dialog, which ->
            when (which) {
                0 -> abrirGoogleMaps()
                1 -> mostrarTrackingVisual()
                2 -> mostrarMenuPrincipal()
            }
        }

        builder.setNegativeButton("Atrás") { _, _ -> mostrarMenuPrincipal() }
        builder.show()
    }

    private fun abrirGoogleMaps() {
        try {
            // Abrir Google Maps con ubicación actual
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))
            intent.setPackage("com.google.android.apps.maps")
            activity.startActivity(intent)
        } catch (e: Exception) {
            // Si Google Maps no está instalado, abrir en navegador
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/"))
            activity.startActivity(intent)
        }
    }

    private fun mostrarTrackingVisual() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Tracking Visual")
        builder.setMessage(
                """
            El tracking visual está activo durante la carrera.
            
            La aplicación registra automáticamente:
            - Distancia recorrida
            - Velocidad actual
            - Tiempo del viaje
            
            Todos los datos se guardan al finalizar la carrera.
        """.trimIndent()
        )
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            mostrarMenuMapa()
        }
        builder.show()
    }

    // ============ MENU 3: TOTALES ============

    private fun mostrarMenuTotales() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("📊 Ver Totales")

        val opciones =
                arrayOf(
                        "3.1 Totales del Día",
                        "3.2 Totales del Mes",
                        "3.3 Historial de Viajes",
                        "3.4 Volver"
                )

        builder.setItems(opciones) { dialog, which ->
            when (which) {
                0 -> mostrarTotalesDia()
                1 -> mostrarTotalesMes()
                2 -> mostrarHistorialViajes()
                3 -> mostrarMenuPrincipal()
            }
        }

        builder.setNegativeButton("Atrás") { _, _ -> mostrarMenuPrincipal() }
        builder.show()
    }

    private fun mostrarTotalesDia() {
        val totales = dataManager.calcularTotalDia()
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fecha = formato.format(Date())

        val builder = AlertDialog.Builder(activity)
        builder.setTitle("📊 Totales del Día")
        builder.setMessage(
                """
            Fecha: $fecha
            
            Viajes realizados: ${totales.cantidadViajes}
            Total recaudado: $ ${totales.totalRecaudado}
            Distancia total: ${String.format("%.2f", totales.distanciaTotal / 1000)} km
            
            Promedio por viaje: $ ${if (totales.cantidadViajes > 0) totales.totalRecaudado / totales.cantidadViajes else 0}
        """.trimIndent()
        )
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            mostrarMenuTotales()
        }
        builder.show()
    }

    private fun mostrarTotalesMes() {
        val totales = dataManager.calcularTotalMes()
        val formato = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val mes = formato.format(Date())

        val builder = AlertDialog.Builder(activity)
        builder.setTitle("📊 Totales del Mes")
        builder.setMessage(
                """
            Mes: $mes
            
            Viajes realizados: ${totales.cantidadViajes}
            Total recaudado: $ ${totales.totalRecaudado}
            Distancia total: ${String.format("%.2f", totales.distanciaTotal / 1000)} km
            
            Promedio por viaje: $ ${if (totales.cantidadViajes > 0) totales.totalRecaudado / totales.cantidadViajes else 0}
            Promedio por día: $ ${totales.totalRecaudado / Calendar.getInstance().get(Calendar.DAY_OF_MONTH)}
        """.trimIndent()
        )
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            mostrarMenuTotales()
        }
        builder.show()
    }

    private fun mostrarHistorialViajes() {
        val viajes = dataManager.obtenerTodosLosViajes().takeLast(10).reversed()

        if (viajes.isEmpty()) {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("Historial de Viajes")
            builder.setMessage("No hay viajes registrados todavía.")
            builder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                mostrarMenuTotales()
            }
            builder.show()
            return
        }

        val formato = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        val textoViajes =
                viajes.joinToString("\n━━━━━━━━━━━━━━━━━━\n") { viaje ->
                    """
            📅 ${formato.format(Date(viaje.fecha))}
            💵 Total: $ ${viaje.precioTotal}
            📍 Distancia: ${String.format("%.2f", viaje.distanciaMetros / 1000)} km
            ⏱️ Tiempo detenido: ${String.format("%02d:%02d", viaje.tiempoDetenidoSegundos / 60, viaje.tiempoDetenidoSegundos % 60)}
            🏷️ Tarifa: ${viaje.tarifaUsada}
            """.trimIndent()
                }

        val builder = AlertDialog.Builder(activity)
        builder.setTitle("📋 Historial de Viajes (Últimos 10)")
        builder.setMessage(textoViajes)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            mostrarMenuTotales()
        }
        builder.show()
    }

    // ============ MENU 4: CONFIGURACION ============

    private fun mostrarMenuConfiguracion() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("⚙️ Configuración")

        val opciones =
                arrayOf(
                        "4.1 Bajada de Bandera",
                        "4.2 Valor Ficha (CP)",
                        "4.3 Tiempo Detenido",
                        "4.4 Datos del Vehículo",
                        "4.5 Tarifas (Diurna/Nocturna/etc)",
                        "4.6 Volver"
                )

        builder.setItems(opciones) { dialog, which ->
            when (which) {
                0 -> configurarBajadaBandera()
                1 -> configurarValorFicha()
                2 -> configurarTiempoDetenido()
                3 -> configurarDatosVehiculo()
                4 -> configurarTarifas()
                5 -> mostrarMenuPrincipal()
            }
        }

        builder.setNegativeButton("Atrás") { _, _ -> mostrarMenuPrincipal() }
        builder.show()
    }

    private fun configurarBajadaBandera() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Configurar Bajada de Bandera")

        val input = EditText(activity)
        input.hint = "Ingrese nuevo valor (actual: $450)"
        input.setText("")
        builder.setView(input)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val nuevoValor = input.text.toString().toIntOrNull()
            if (nuevoValor != null && nuevoValor > 0) {
                dataManager.guardarConfiguracion("bajada_bandera", nuevoValor)
                Toast.makeText(
                                activity,
                                "✓ Bajada de bandera actualizada a: $$nuevoValor",
                                Toast.LENGTH_SHORT
                        )
                        .show()
            } else {
                Toast.makeText(activity, "Valor inválido", Toast.LENGTH_SHORT).show()
            }
            mostrarMenuConfiguracion()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> mostrarMenuConfiguracion() }
        builder.show()
    }

    private fun configurarValorFicha() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Configurar Valor por Ficha (CP)")

        val input = EditText(activity)
        input.hint = "Ingrese nuevo valor (actual: $190)"
        input.setText("")
        builder.setView(input)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val nuevoValor = input.text.toString().toIntOrNull()
            if (nuevoValor != null && nuevoValor > 0) {
                dataManager.guardarConfiguracion("costo_ficha", nuevoValor)
                Toast.makeText(
                                activity,
                                "✓ Valor de ficha actualizado a: $$nuevoValor",
                                Toast.LENGTH_SHORT
                        )
                        .show()
            } else {
                Toast.makeText(activity, "Valor inválido", Toast.LENGTH_SHORT).show()
            }
            mostrarMenuConfiguracion()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> mostrarMenuConfiguracion() }
        builder.show()
    }

    private fun configurarTiempoDetenido() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Configurar Tiempo Detenido")

        val input = EditText(activity)
        input.hint = "Ingrese segundos para cobrar ficha (actual: 60)"
        input.setText("")
        builder.setView(input)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val nuevoValor = input.text.toString().toIntOrNull()
            if (nuevoValor != null && nuevoValor > 0) {
                dataManager.guardarConfiguracion("tiempo_ficha_seg", nuevoValor)
                Toast.makeText(
                                activity,
                                "✓ Tiempo detenido actualizado a: $nuevoValor seg",
                                Toast.LENGTH_SHORT
                        )
                        .show()
            } else {
                Toast.makeText(activity, "Valor inválido", Toast.LENGTH_SHORT).show()
            }
            mostrarMenuConfiguracion()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> mostrarMenuConfiguracion() }
        builder.show()
    }

    private fun configurarDatosVehiculo() {
        val datosActuales = dataManager.obtenerDatosVehiculo()

        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Datos del Vehículo")

        val layout = LinearLayout(activity)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val inputPlaca = EditText(activity)
        inputPlaca.hint = "Placa"
        inputPlaca.setText(datosActuales.placa)
        layout.addView(inputPlaca)

        val inputModelo = EditText(activity)
        inputModelo.hint = "Modelo"
        inputModelo.setText(datosActuales.modelo)
        layout.addView(inputModelo)

        val inputConductor = EditText(activity)
        inputConductor.hint = "Nombre del conductor"
        inputConductor.setText(datosActuales.conductor)
        layout.addView(inputConductor)

        builder.setView(layout)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val nuevosDatos =
                    TripDataManager.DatosVehiculo(
                            placa = inputPlaca.text.toString(),
                            modelo = inputModelo.text.toString(),
                            conductor = inputConductor.text.toString()
                    )
            dataManager.guardarDatosVehiculo(nuevosDatos)
            Toast.makeText(activity, "✓ Datos del vehículo actualizados", Toast.LENGTH_SHORT).show()
            mostrarMenuConfiguracion()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> mostrarMenuConfiguracion() }
        builder.show()
    }

    private fun configurarTarifas() {
        val tarifas = dataManager.obtenerTodasLasTarifas()
        val tarifaActiva = dataManager.obtenerTarifaActiva()

        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Seleccionar Tarifa")

        val nombresTarifas = tarifas.keys.toTypedArray()
        val indiceActual = nombresTarifas.indexOf(tarifaActiva.nombre)

        builder.setSingleChoiceItems(nombresTarifas, indiceActual) { dialog, which ->
            val tarifaSeleccionada = nombresTarifas[which]
            dataManager.activarTarifa(tarifaSeleccionada)
            Toast.makeText(activity, "✓ Tarifa '$tarifaSeleccionada' activada", Toast.LENGTH_SHORT)
                    .show()
            dialog.dismiss()
            mostrarMenuConfiguracion()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> mostrarMenuConfiguracion() }
        builder.show()
    }

    fun obtenerTarifaActiva(): TripDataManager.TarifaConfig {
        return dataManager.obtenerTarifaActiva()
    }

    fun guardarViaje(distanciaMetros: Double, tiempoDetenidoSegundos: Int, precioTotal: Int) {
        val tarifaActual = obtenerTarifaActiva()
        val viaje =
                TripDataManager.TripRecord(
                        distanciaMetros = distanciaMetros,
                        tiempoDetenidoSegundos = tiempoDetenidoSegundos,
                        precioTotal = precioTotal,
                        tarifaUsada = tarifaActual.nombre
                )
        dataManager.guardarViaje(viaje)
    }
}

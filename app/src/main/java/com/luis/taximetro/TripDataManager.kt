package com.luis.taximetro

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

/** Gestor de datos para el taxímetro Maneja la persistencia de viajes, totales y configuraciones */
class TripDataManager(context: Context) {

    private val prefs: SharedPreferences =
            context.getSharedPreferences("TaximetroData", Context.MODE_PRIVATE)
    private val gson = Gson()

    // ============ DATA CLASSES ============

    data class TripRecord(
            val id: String = UUID.randomUUID().toString(),
            val fecha: Long = System.currentTimeMillis(),
            val distanciaMetros: Double,
            val tiempoDetenidoSegundos: Int,
            val precioTotal: Int,
            val tarifaUsada: String = "Diurna"
    )

    data class TarifaConfig(
            val nombre: String,
            val bajadaBandera: Int,
            val costoFicha: Int,
            val tiempoFichaMs: Long,
            val metrosPorFicha: Double,
            val activa: Boolean = false
    )

    data class DatosVehiculo(
            val placa: String = "",
            val modelo: String = "",
            val conductor: String = ""
    )

    // ============ VIAJES ============

    fun guardarViaje(viaje: TripRecord) {
        val viajes = obtenerTodosLosViajes().toMutableList()
        viajes.add(viaje)
        prefs.edit().putString("viajes", gson.toJson(viajes)).apply()
    }

    fun obtenerTodosLosViajes(): List<TripRecord> {
        val json = prefs.getString("viajes", "[]") ?: "[]"
        val type = object : TypeToken<List<TripRecord>>() {}.type
        return gson.fromJson(json, type)
    }

    fun obtenerViajesDelDia(fecha: Long = System.currentTimeMillis()): List<TripRecord> {
        val calendario = Calendar.getInstance()
        calendario.timeInMillis = fecha
        calendario.set(Calendar.HOUR_OF_DAY, 0)
        calendario.set(Calendar.MINUTE, 0)
        calendario.set(Calendar.SECOND, 0)
        calendario.set(Calendar.MILLISECOND, 0)
        val inicioDia = calendario.timeInMillis

        calendario.add(Calendar.DAY_OF_MONTH, 1)
        val finDia = calendario.timeInMillis

        return obtenerTodosLosViajes().filter { it.fecha in inicioDia until finDia }
    }

    fun obtenerViajesDelMes(fecha: Long = System.currentTimeMillis()): List<TripRecord> {
        val calendario = Calendar.getInstance()
        calendario.timeInMillis = fecha
        calendario.set(Calendar.DAY_OF_MONTH, 1)
        calendario.set(Calendar.HOUR_OF_DAY, 0)
        calendario.set(Calendar.MINUTE, 0)
        calendario.set(Calendar.SECOND, 0)
        calendario.set(Calendar.MILLISECOND, 0)
        val inicioMes = calendario.timeInMillis

        calendario.add(Calendar.MONTH, 1)
        val finMes = calendario.timeInMillis

        return obtenerTodosLosViajes().filter { it.fecha in inicioMes until finMes }
    }

    fun calcularTotalDia(fecha: Long = System.currentTimeMillis()): TotalesSummary {
        val viajes = obtenerViajesDelDia(fecha)
        return TotalesSummary(
                cantidadViajes = viajes.size,
                totalRecaudado = viajes.sumOf { it.precioTotal },
                distanciaTotal = viajes.sumOf { it.distanciaMetros }
        )
    }

    fun calcularTotalMes(fecha: Long = System.currentTimeMillis()): TotalesSummary {
        val viajes = obtenerViajesDelMes(fecha)
        return TotalesSummary(
                cantidadViajes = viajes.size,
                totalRecaudado = viajes.sumOf { it.precioTotal },
                distanciaTotal = viajes.sumOf { it.distanciaMetros }
        )
    }

    data class TotalesSummary(
            val cantidadViajes: Int,
            val totalRecaudado: Int,
            val distanciaTotal: Double
    )

    // ============ TARIFAS ============

    fun guardarTarifa(tarifa: TarifaConfig) {
        val tarifas = obtenerTodasLasTarifas().toMutableMap()
        tarifas[tarifa.nombre] = tarifa
        prefs.edit().putString("tarifas", gson.toJson(tarifas)).apply()
    }

    fun obtenerTodasLasTarifas(): Map<String, TarifaConfig> {
        val json = prefs.getString("tarifas", null)
        return if (json != null) {
            val type = object : TypeToken<Map<String, TarifaConfig>>() {}.type
            gson.fromJson(json, type)
        } else {
            // Tarifas por defecto
            mapOf(
                    "Diurna" to TarifaConfig("Diurna", 450, 190, 60000L, 200.0, true),
                    "Nocturna" to TarifaConfig("Nocturna", 550, 230, 60000L, 200.0, false),
                    "Festivos" to TarifaConfig("Festivos", 600, 250, 60000L, 200.0, false),
                    "Suburbana" to TarifaConfig("Suburbana", 500, 210, 60000L, 250.0, false),
                    "Urbana" to TarifaConfig("Urbana", 450, 190, 60000L, 180.0, false)
            )
        }
    }

    fun obtenerTarifaActiva(): TarifaConfig {
        return obtenerTodasLasTarifas().values.firstOrNull { it.activa }
                ?: obtenerTodasLasTarifas().values.first()
    }

    fun activarTarifa(nombreTarifa: String) {
        val tarifas = obtenerTodasLasTarifas().toMutableMap()
        // Desactivar todas
        tarifas.forEach { (nombre, tarifa) -> tarifas[nombre] = tarifa.copy(activa = false) }
        // Activar la seleccionada
        tarifas[nombreTarifa]?.let { tarifas[nombreTarifa] = it.copy(activa = true) }
        prefs.edit().putString("tarifas", gson.toJson(tarifas)).apply()
    }

    // ============ DATOS VEHÍCULO ============

    fun guardarDatosVehiculo(datos: DatosVehiculo) {
        prefs.edit().putString("vehiculo", gson.toJson(datos)).apply()
    }

    fun obtenerDatosVehiculo(): DatosVehiculo {
        val json = prefs.getString("vehiculo", null)
        return if (json != null) {
            gson.fromJson(json, DatosVehiculo::class.java)
        } else {
            DatosVehiculo()
        }
    }

    // ============ CONFIGURACIÓN SIMPLE ============

    fun guardarConfiguracion(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun obtenerConfiguracion(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }

    fun limpiarTodosLosDatos() {
        prefs.edit().clear().apply()
    }
}
# Taxímetro EKO MAIKO EM-900

![Android](https://img.shields.io/badge/Android-24%2B-green.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

Aplicación Android nativa que simula un **taxímetro modelo EKO MAIKO EM-900**. Desarrollada en Kotlin, replica fielmente el aspecto visual y la funcionalidad de un taxímetro físico profesional.

## 📱 Características

- ✅ **Máquina de Estados**: LIBRE, OCUPADO, PAGAR
- ✅ **Sistema de Tarifas**:
  - Bajada de bandera: $450
  - Costo por parking/espera: $190 cada 60 segundos
  - Cobro simulado por distancia
- ✅ **Interfaz Realista**:
  - LEDs de estado (Rojo, Verde, Azul)
  - Pantalla LCD digital con texto cyan
  - Semáforo digital sincronizado
  - Botones físicos simulados
- ✅ **Funcionalidades**:
  - Reloj en tiempo real (fecha y hora)
  - Contador de tiempo detenido
  - Simulador de velocidad
  - Recibo de impresión
- ✅ **Orientación Horizontal**: Diseñado exclusivamente para modo landscape

## 🎬 Demo

La aplicación simula un taxímetro profesional con:

- **Panel lateral izquierdo**: LEDs de estado
- **Pantalla LCD central**: Información del viaje (precio, velocidad, tiempo)
- **Botones inferiores**: Control de operaciones

## 🏗️ Arquitectura

- **Lenguaje**: Kotlin
- **Min SDK**: 24 (Android 7.0 Lollipop)
- **Target SDK**: 36
- **View System**: XML Layouts con View Binding
- **Patrón**: Activity-based con máquina de estados

## 📦 Estructura del Proyecto

```
app/
├── src/main/
│   ├── java/com/luis/taximetro/
│   │   └── MainActivity.kt              # Lógica principal
│   ├── res/
│   │   ├── layout/
│   │   │   └── activity_main.xml        # UI horizontal
│   │   ├── drawable/
│   │   │   ├── led_circle.xml           # LEDs circulares
│   │   │   └── boton_verde.xml          # Botones
│   │   └── values/
│   │       ├── colors.xml               # Paleta de colores
│   │       └── themes.xml               # Tema sin ActionBar
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## 🚀 Instalación

### Requisitos

- Android Studio Hedgehog | 2023.1.1 o superior
- JDK 11 o superior
- Android SDK 36
- Dispositivo/Emulador con Android 7.0+

### Pasos

1. **Clonar el repositorio**:

   ```bash
   git clone https://github.com/OowarriorwhiteoO/Taximetro-900.git
   cd Taximetro-900
   ```

2. **Abrir en Android Studio**:

   - File → Open → Seleccionar carpeta del proyecto
   - Esperar a que Gradle sincronice

3. **Ejecutar**:
   - Conectar dispositivo Android o iniciar emulador
   - Click en Run ▶️ (Shift + F10)

## 🎮 Uso

### Flujo de Operación

1. **Iniciar Carrera** (Botón 1):

   - LED cambia a Rojo
   - Se aplica bajada de bandera ($450)
   - Inicia el taxímetro

2. **Control de Movimiento** (Botón 2):

   - Alternar entre movimiento (45 km/h) y detenido (0 km/h)
   - En movimiento: precio aumenta por distancia
   - Detenido: cobra ficha cada 60 segundos

3. **Finalizar Carrera** (Botón 3):

   - Muestra recibo con total a pagar
   - Resetea el taxímetro a estado LIBRE

4. **Menú** (Botón 4):
   - Muestra/oculta opciones del taxímetro

## 🎨 Colores del Taxímetro

```kotlin
carcasa_gris: #333333      // Fondo de carcasa
lcd_negro: #000000         // Pantalla LCD
texto_cyan: #00FFFF        // Texto estilo LCD
texto_blanco: #FFFFFF      // Texto principal
led_rojo: #FF0000          // Estado OCUPADO
led_verde: #00FF00         // Estado LIBRE
led_azul: #0000FF          // Estado PAGAR
boton_verde: #4CAF50       // Botones físicos
```

## 🧩 Componentes Principales

### MainActivity.kt

- **Estados**: Enum `Estado { LIBRE, OCUPADO, PAGAR }`
- **Handlers**:
  - `actualizarReloj`: Actualiza fecha/hora cada segundo
  - `actualizarTiempoEspera`: Cuenta tiempo detenido y cobra fichas
  - `simularMovimiento`: Simula cobro por distancia
- **Funciones clave**:
  - `iniciarCarrera()`: Inicia viaje
  - `toggleMovimiento()`: Alterna velocidad
  - `finalizarCarrera()`: Muestra recibo
  - `cambiarEstado()`: Actualiza LEDs y semáforo

### activity_main.xml

- Panel lateral con 3 LEDs (Rojo, Verde, Azul)
- Pantalla LCD con:
  - Barra superior: Fecha y hora
  - Zona principal: Semáforo + Precio
  - Zona inferior: Tarifas + Estado
- 4 botones físicos verdes
- Overlay de menú superpuesto

## 📝 Tarifas Configuradas

```kotlin
BAJADA_BANDERA = 450    // Cargo inicial
COSTO_FICHA = 190       // Cada 60 segundos detenido
TIEMPO_FICHA_MS = 60000L
```

## 🔧 Tecnologías

- **Kotlin**: Lenguaje principal
- **View Binding**: Acceso type-safe a views
- **Handler & Runnable**: Actualizaciones en tiempo real
- **Material Components**: Botones y componentes UI
- **ConstraintLayout**: Layouts responsive

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver archivo `LICENSE` para más detalles.

## 👨‍💻 Autor

**Luis V.**

- GitHub: [@OowarriorwhiteoO](https://github.com/OowarriorwhiteoO)

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Si deseas mejorar el proyecto:

1. Fork el proyecto
2. Crea tu Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add: nueva característica'`)
4. Push al Branch (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 🐛 Reportar Problemas

Si encuentras un bug o tienes una sugerencia, por favor abre un [issue](https://github.com/OowarriorwhiteoO/Taximetro-900/issues).

---

⭐ **Si te gusta este proyecto, dale una estrella en GitHub!**

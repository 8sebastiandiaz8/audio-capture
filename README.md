# Audio Capture - Sistema de Captura de Audio Remoto

Sistema completo de captura y transmisión de audio en tiempo real desde Android hacia PC, compuesto por dos aplicaciones:

1. **App Android (Kotlin)** - Captura audio del micrófono y del audio interno del dispositivo
2. **App Windows (Python + Tkinter)** - Recibe, reproduce y guarda el audio

## 📋 Características

### App Android
- ✅ Captura de audio del **micrófono** (Android 4.4+)
- ✅ Captura de **audio interno** (Android 10+)
- ✅ Transmisión en tiempo real por **TCP Socket**
- ✅ Servicio en primer plano para ejecución continua
- ✅ Interfaz intuitiva y moderna con Material Design
- ✅ Compatible desde Android 4.4 (API 19) hasta Android 15 (API 35)

### App Windows
- ✅ Servidor TCP configurable
- ✅ **Reproducción en tiempo real** del audio recibido
- ✅ **Grabación** y guardado en formato WAV
- ✅ Interfaz gráfica con Tkinter
- ✅ Log de eventos en tiempo real
- ✅ Convertible a ejecutable .exe

### Métodos de Conexión
- 🔌 **WiFi** - PC y teléfono en la misma red
- 🔌 **USB** - Usando `adb forward` (sin necesidad de WiFi)

## 🎵 Especificaciones de Audio

Ambas aplicaciones utilizan la misma configuración de audio para garantizar compatibilidad:

- **Sample Rate**: 44100 Hz
- **Canales**: 1 (Mono)
- **Codificación**: PCM 16-bit (Little Endian)
- **Tamaño de buffer**: 4096 bytes
- **Puerto por defecto**: 5000

## 📦 Requisitos Previos

### Para la App Android
- **Android Studio** (versión recomendada: Hedgehog o posterior)
- **JDK** 8 o superior
- **Gradle** (incluido con Android Studio)
- Dispositivo Android o emulador con API 19+

### Para la App Windows
- **Python 3.8+**
- **pip** (gestor de paquetes de Python)
- **PyAudio** (requiere instalación especial en Windows)

### Para conexión USB
- **Android Debug Bridge (ADB)**
  - Incluido con Android Studio
  - O descargable desde: https://developer.android.com/studio/releases/platform-tools

## 🚀 Instalación y Configuración

### 1. Compilar la App Android

#### Opción A: Con Android Studio (Recomendado)

1. Abre Android Studio
2. Selecciona "Open" y navega a la carpeta `android-app/`
3. Espera a que Gradle sincronice el proyecto
4. Conecta tu dispositivo Android por USB (con depuración USB habilitada) o inicia un emulador
5. Haz clic en el botón "Run" (▶️) o presiona `Shift + F10`
6. La app se instalará y se abrirá automáticamente

#### Opción B: Con línea de comandos

```bash
cd android-app
./gradlew assembleDebug
# El APK se generará en: app/build/outputs/apk/debug/app-debug.apk

# Para instalar en dispositivo conectado:
./gradlew installDebug
```

**Nota para Windows**: Usa `gradlew.bat` en lugar de `./gradlew`

### 2. Configurar la App Windows

#### Instalar PyAudio en Windows

PyAudio requiere pasos especiales en Windows:

**Opción 1: Instalar wheel precompilado (Recomendado)**
```bash
# Descargar wheel desde: https://www.lfd.uci.edu/~gohlke/pythonlibs/#pyaudio
# Ejemplo para Python 3.11 64-bit:
pip install PyAudio-0.2.11-cp311-cp311-win_amd64.whl
```

**Opción 2: Usar pipwin**
```bash
pip install pipwin
pipwin install pyaudio
```

#### Instalar dependencias

```bash
cd windows-app
pip install -r requirements.txt
```

#### Ejecutar la aplicación

```bash
python main.py
```

## 🔧 Uso de las Aplicaciones

### App Windows (Servidor)

1. **Ejecuta** la aplicación de Windows:
   ```bash
   python main.py
   ```

2. **Configuración**:
   - El puerto por defecto es `5000` (puedes cambiarlo si es necesario)
   - Asegúrate de que el firewall de Windows permita conexiones en este puerto

3. **Inicia el servidor**:
   - Haz clic en "▶ Iniciar Servidor"
   - El estado cambiará a "🟡 Servidor activo - Esperando conexión..."

4. **Obtén la IP de tu PC**:
   - En Windows, abre CMD y ejecuta: `ipconfig`
   - Busca tu dirección IPv4 (ejemplo: `192.168.1.100`)

### App Android (Cliente)

1. **Abre** la app Audio Capture en tu teléfono

2. **Configuración del servidor**:
   - Ingresa la **IP de tu PC** en el campo "IP del servidor"
   - Ingresa el **puerto** (por defecto: `5000`)

3. **Selecciona fuentes de audio**:
   - **Capturar Micrófono**: ✅ (disponible en todos los dispositivos)
   - **Capturar Audio Interno**: ⚠️ Solo Android 10+ (se deshabilitará automáticamente en versiones anteriores)

4. **Conecta y graba**:
   - Toca el botón "CONECTAR Y GRABAR"
   - Concede los permisos necesarios (micrófono, notificaciones)
   - Si seleccionaste audio interno, acepta la solicitud de MediaProjection

5. **Estado**:
   - Verás el estado cambiar: Conectando → Conectado → Grabando
   - En el PC verás: "🟢 Conectado - Recibiendo audio"
   - El audio se reproducirá automáticamente en tu PC

6. **Detener**:
   - Toca el botón "DETENER" cuando termines

### Grabar Audio en PC

1. Una vez conectado, haz clic en "⚫ Iniciar Grabación" en la app de Windows
2. El audio se guardará en un buffer interno
3. Cuando termines, haz clic en "⬛ Detener Grabación"
4. Haz clic en "💾 Guardar como WAV" para guardar el archivo
5. Selecciona la ubicación y nombre del archivo

## 🌐 Métodos de Conexión

### Método 1: Conexión por WiFi

**Requisitos**:
- PC y teléfono deben estar en la **misma red WiFi**

**Pasos**:

1. **En el PC**: Obtén tu IP local
   ```bash
   # Windows
   ipconfig
   
   # Linux/Mac
   ifconfig
   ```
   Ejemplo de IP: `192.168.1.100`

2. **En la app Android**: 
   - Ingresa la IP del PC: `192.168.1.100`
   - Puerto: `5000`

3. **Conecta**: Toca "CONECTAR Y GRABAR"

### Método 2: Conexión por USB (con ADB)

**Requisitos**:
- Cable USB
- ADB instalado
- Depuración USB habilitada en el teléfono

**Pasos**:

1. **Conecta** el teléfono al PC por USB

2. **Habilita depuración USB**:
   - Ve a Ajustes → Acerca del teléfono
   - Toca 7 veces en "Número de compilación"
   - Ve a Ajustes → Opciones de desarrollo
   - Activa "Depuración USB"

3. **Verifica la conexión ADB**:
   ```bash
   adb devices
   ```
   Deberías ver tu dispositivo listado

4. **Configura port forwarding**:
   ```bash
   adb forward tcp:5000 tcp:5000
   ```

5. **En la app Android**:
   - Ingresa IP: `127.0.0.1`
   - Puerto: `5000`

6. **Conecta**: Toca "CONECTAR Y GRABAR"

**Ventajas del método USB**:
- ✅ No requiere WiFi
- ✅ Conexión más estable
- ✅ Sin interferencias de red

## 🎯 Crear Ejecutable de Windows

Para crear un archivo `.exe` independiente que no requiera Python instalado:

```bash
cd windows-app
build_exe.bat
```

O manualmente:

```bash
pip install pyinstaller
pyinstaller --onefile --windowed --name AudioCapture main.py
```

El ejecutable se creará en: `dist/AudioCapture.exe`

**Nota**: El ejecutable será de ~50-80 MB debido a que incluye Python y todas las dependencias.

## 🔍 Solución de Problemas

### Problema: "No se puede conectar al servidor"

**Soluciones**:
- ✅ Verifica que el servidor de Windows esté iniciado
- ✅ Confirma que PC y teléfono estén en la misma red WiFi
- ✅ Verifica que la IP sea correcta (usa `ipconfig`)
- ✅ Desactiva temporalmente el firewall de Windows para probar
- ✅ Si usas USB, verifica que `adb forward` esté configurado

### Problema: "Permiso de grabación denegado"

**Soluciones**:
- ✅ Ve a Ajustes → Aplicaciones → Audio Capture → Permisos
- ✅ Concede permiso de "Micrófono"
- ✅ Reinicia la app

### Problema: "Audio interno no disponible"

**Explicación**:
- La captura de audio interno solo está disponible en Android 10+ (API 29+)
- En versiones anteriores, la opción aparecerá deshabilitada
- Usa solo captura de micrófono en dispositivos más antiguos

### Problema: "Error al instalar PyAudio en Windows"

**Solución**:
- Descarga el wheel precompilado desde: https://www.lfd.uci.edu/~gohlke/pythonlibs/#pyaudio
- Instala usando: `pip install PyAudio-[version].whl`
- O usa: `pip install pipwin` y luego `pipwin install pyaudio`

### Problema: "El audio se escucha entrecortado"

**Soluciones**:
- ✅ Verifica la calidad de la conexión WiFi
- ✅ Acerca el teléfono al router
- ✅ Usa conexión USB en su lugar
- ✅ Cierra otras aplicaciones que usen la red

### Problema: "El servicio se detiene en segundo plano"

**Soluciones**:
- ✅ Desactiva la optimización de batería para Audio Capture
- ✅ Ve a Ajustes → Batería → Optimización de batería → Audio Capture → No optimizar
- ✅ En algunos dispositivos (Xiaomi, Huawei), verifica configuraciones de "Autostart"

## ⚠️ Limitaciones Conocidas

1. **Audio Interno**:
   - Solo disponible en Android 10+ (API 29+)
   - Requiere permiso de MediaProjection (proyección de pantalla)
   - Algunos dispositivos pueden tener restricciones del fabricante

2. **Latencia**:
   - La latencia típica es de 100-500ms dependiendo de:
     - Método de conexión (WiFi vs USB)
     - Calidad de la red
     - Procesamiento del dispositivo

3. **Compatibilidad**:
   - PyAudio en Windows requiere instalación especial
   - Algunos antivirus pueden bloquear conexiones TCP

4. **Batería**:
   - La captura continua consume batería
   - Se recomienda mantener el dispositivo conectado durante sesiones largas

## 📱 Permisos Requeridos

### Android
- **RECORD_AUDIO**: Para capturar audio del micrófono
- **INTERNET**: Para enviar datos por red
- **FOREGROUND_SERVICE**: Para mantener el servicio activo
- **FOREGROUND_SERVICE_MICROPHONE**: Tipo de servicio para Android 14+
- **FOREGROUND_SERVICE_MEDIA_PROJECTION**: Tipo de servicio para Android 14+
- **POST_NOTIFICATIONS**: Para mostrar notificación en Android 13+

## 🏗️ Arquitectura del Sistema

```
┌─────────────────────┐         TCP Socket          ┌──────────────────────┐
│   Android Device    │   ───────────────────────►  │     Windows PC       │
│                     │     (Audio PCM Stream)       │                      │
│  ┌───────────────┐  │                              │  ┌────────────────┐  │
│  │ MainActivity  │  │                              │  │   GUI (Tkinter)│  │
│  └───────┬───────┘  │                              │  └────────┬───────┘  │
│          │          │                              │           │          │
│  ┌───────▼───────┐  │                              │  ┌────────▼───────┐  │
│  │AudioCapture   │  │                              │  │AudioReceiver   │  │
│  │Service        │  │                              │  │                │  │
│  │(Foreground)   │  │                              │  │- TCP Server    │  │
│  └───────┬───────┘  │                              │  │- PyAudio Play  │  │
│          │          │                              │  │- WAV Recording │  │
│  ┌───────▼───────┐  │                              │  └────────────────┘  │
│  │AudioRecord    │  │                              │                      │
│  │(Microphone)   │  │                              │                      │
│  └───────┬───────┘  │                              │                      │
│          │          │                              │                      │
│  ┌───────▼───────┐  │                              │                      │
│  │AudioStream    │  │                              │                      │
│  │Sender         │  │                              │                      │
│  │(TCP Client)   │  │                              │                      │
│  └───────────────┘  │                              │                      │
└─────────────────────┘                              └──────────────────────┘
```

## 🛠️ Desarrollo

### Estructura del Proyecto

```
audio-capture/
├── android-app/
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/audiocapture/app/
│   │   │   │   ├── MainActivity.kt           # Actividad principal
│   │   │   │   ├── AudioCaptureService.kt    # Servicio de captura
│   │   │   │   └── AudioStreamSender.kt      # Cliente TCP
│   │   │   ├── res/                          # Recursos (layouts, strings, etc.)
│   │   │   └── AndroidManifest.xml           # Manifest con permisos
│   │   └── build.gradle.kts                  # Configuración de Gradle
│   └── settings.gradle.kts
│
├── windows-app/
│   ├── audio_receiver.py                     # Servidor TCP y reproducción
│   ├── gui.py                                # Interfaz gráfica
│   ├── main.py                               # Punto de entrada
│   ├── requirements.txt                      # Dependencias Python
│   └── build_exe.bat                         # Script para crear .exe
│
└── README.md                                 # Este archivo
```

### Compilar desde Código Fuente

#### Android
```bash
cd android-app
./gradlew assembleDebug
# APK en: app/build/outputs/apk/debug/app-debug.apk
```

#### Windows
```bash
cd windows-app
pip install -r requirements.txt
python main.py
```

## 🔒 Consideraciones de Seguridad

### Servidor TCP
- El servidor de Windows escucha en todas las interfaces de red (0.0.0.0) para permitir conexiones desde WiFi, Ethernet o USB tethering
- **Recomendación**: Configura el firewall de Windows para permitir conexiones solo en redes confiables (red doméstica/trabajo)
- El servidor no implementa autenticación por diseño, ya que está pensado para uso personal en redes privadas

### Conexión Segura
- **WiFi**: Asegúrate de estar en una red WiFi privada y segura
- **USB**: El método más seguro, ya que la conexión es física y directa
- **Puerto**: El puerto 5000 debe estar bloqueado en el firewall para redes públicas

### Datos de Audio
- El audio se transmite sin cifrar por TCP
- Para uso en redes públicas, considera usar una VPN
- No se guardan credenciales ni datos sensibles

### Permisos Android
- La app solicita solo los permisos estrictamente necesarios
- Revisa los permisos en la configuración de Android
- El servicio en primer plano muestra una notificación visible

### Mejores Prácticas
1. Usa el método USB cuando sea posible para mayor seguridad
2. Desactiva el servidor cuando no lo uses
3. Mantén el software actualizado
4. No uses en redes WiFi públicas sin protección adicional

## 📄 Licencia

Este proyecto es de código abierto y está disponible bajo la Licencia MIT.

## 🤝 Contribuciones

Las contribuciones son bienvenidas! Si encuentras un bug o tienes una sugerencia:

1. Abre un Issue en GitHub
2. Haz un Fork del proyecto
3. Crea una rama con tu feature (`git checkout -b feature/AmazingFeature`)
4. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
5. Push a la rama (`git push origin feature/AmazingFeature`)
6. Abre un Pull Request

## 📞 Soporte

Si tienes problemas o preguntas:
- Abre un Issue en GitHub
- Consulta la sección de "Solución de Problemas" en este README

## 🎉 Agradecimientos

- Android AudioRecord API
- PyAudio library
- Material Design Components
- Comunidad de desarrolladores de Android y Python

---

**Desarrollado con ❤️ para captura de audio en tiempo real**

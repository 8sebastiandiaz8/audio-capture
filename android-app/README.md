# Audio Capture - Android App

Esta es la aplicación Android del sistema Audio Capture.

## Configuración Inicial

### Gradle Wrapper JAR

Este proyecto requiere el archivo `gradle/wrapper/gradle-wrapper.jar` para funcionar correctamente. 

**Opción 1: Usar Android Studio (Recomendado)**
- Abre el proyecto en Android Studio
- Android Studio descargará automáticamente el Gradle wrapper

**Opción 2: Generar manualmente el wrapper**
```bash
# Si tienes Gradle instalado globalmente
gradle wrapper --gradle-version 8.2
```

**Opción 3: Descargar manualmente**
Si no puedes usar las opciones anteriores, puedes descargar el gradle-wrapper.jar desde:
https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar

Y colocarlo en: `gradle/wrapper/gradle-wrapper.jar`

## Compilación

Una vez que tengas el wrapper configurado:

```bash
# Linux/Mac
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

## Permisos

La aplicación solicita los siguientes permisos:
- RECORD_AUDIO - Para capturar audio del micrófono
- INTERNET - Para enviar audio al servidor
- FOREGROUND_SERVICE - Para mantener el servicio activo
- POST_NOTIFICATIONS - Para mostrar notificación (Android 13+)

## Iconos de la Aplicación

Los iconos de la aplicación se pueden generar usando Android Studio:
1. Clic derecho en `res` → New → Image Asset
2. Selecciona "Launcher Icons (Adaptive and Legacy)"
3. Configura tu icono personalizado
4. Genera los assets

Por ahora, la app usa iconos adaptivos simples con el símbolo de micrófono.

package com.audiocapture.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Servicio en primer plano que captura audio del micrófono
 * y lo transmite al servidor por TCP
 */
class AudioCaptureService : Service() {

    companion object {
        private const val TAG = "AudioCaptureService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "AudioCaptureChannel"
        
        // Configuración de audio - debe coincidir con la app de Windows
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_MULTIPLIER = 2
    }

    // Estado del servicio
    private val isRunning = AtomicBoolean(false)
    private var captureThread: Thread? = null
    
    // Audio y red
    private var audioRecord: AudioRecord? = null
    private var audioStreamSender: AudioStreamSender? = null
    
    // Configuración
    private var serverIp: String = ""
    private var serverPort: Int = 5000
    private var captureMicrophone: Boolean = true
    private var captureInternalAudio: Boolean = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Servicio creado")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")

        // Extraer configuración del intent
        intent?.let {
            serverIp = it.getStringExtra("server_ip") ?: "127.0.0.1"
            serverPort = it.getIntExtra("server_port", 5000)
            captureMicrophone = it.getBooleanExtra("capture_microphone", true)
            captureInternalAudio = it.getBooleanExtra("capture_internal_audio", false)
        }

        // Iniciar servicio en primer plano
        startForeground(NOTIFICATION_ID, createNotification())

        // Iniciar captura de audio
        startAudioCapture()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d(TAG, "Servicio destruido")
        stopAudioCapture()
        super.onDestroy()
    }

    /**
     * Crea el canal de notificación (requerido para Android 8+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Crea la notificación para el servicio en primer plano
     */
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, pendingIntentFlags
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        return builder.build()
    }

    /**
     * Inicia la captura y transmisión de audio
     */
    private fun startAudioCapture() {
        if (isRunning.get()) {
            Log.w(TAG, "La captura ya está en ejecución")
            return
        }

        Log.d(TAG, "Iniciando captura de audio")
        Log.d(TAG, "Servidor: $serverIp:$serverPort")
        Log.d(TAG, "Micrófono: $captureMicrophone, Audio interno: $captureInternalAudio")

        isRunning.set(true)

        // Iniciar hilo de captura
        captureThread = thread(start = true, name = "AudioCaptureThread") {
            captureAndStreamAudio()
        }
    }

    /**
     * Detiene la captura y transmisión de audio
     */
    private fun stopAudioCapture() {
        Log.d(TAG, "Deteniendo captura de audio")
        isRunning.set(false)

        // Esperar a que termine el hilo de captura
        captureThread?.join(2000)
        captureThread = null

        // Limpiar recursos
        audioRecord?.apply {
            if (state == AudioRecord.STATE_INITIALIZED) {
                stop()
            }
            release()
        }
        audioRecord = null

        audioStreamSender?.disconnect()
        audioStreamSender = null
    }

    /**
     * Hilo principal de captura y transmisión de audio
     */
    private fun captureAndStreamAudio() {
        try {
            // Conectar al servidor
            audioStreamSender = AudioStreamSender(serverIp, serverPort)
            if (!audioStreamSender!!.connect()) {
                Log.e(TAG, "No se pudo conectar al servidor")
                stopSelf()
                return
            }

            // Inicializar AudioRecord para captura de micrófono
            if (captureMicrophone) {
                val bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT
                ) * BUFFER_SIZE_MULTIPLIER

                if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "Error al obtener tamaño de buffer")
                    stopSelf()
                    return
                }

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord no se inicializó correctamente")
                    stopSelf()
                    return
                }

                Log.d(TAG, "AudioRecord inicializado. Buffer size: $bufferSize")

                // Iniciar grabación
                audioRecord?.startRecording()

                // Buffer para leer datos de audio
                val audioBuffer = ByteArray(4096) // 4KB buffer

                // Loop de captura y envío
                while (isRunning.get()) {
                    val bytesRead = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0

                    if (bytesRead > 0) {
                        // Enviar datos al servidor
                        if (!audioStreamSender!!.sendAudioData(audioBuffer, bytesRead)) {
                            Log.e(TAG, "Error al enviar datos. Deteniendo servicio.")
                            break
                        }
                    } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                        Log.e(TAG, "ERROR_INVALID_OPERATION en AudioRecord.read()")
                        break
                    } else if (bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                        Log.e(TAG, "ERROR_BAD_VALUE en AudioRecord.read()")
                        break
                    }
                }
            }

            Log.d(TAG, "Fin del loop de captura")

        } catch (e: SecurityException) {
            Log.e(TAG, "Error de seguridad: permiso de grabación no concedido", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error en captura de audio", e)
        } finally {
            // Limpiar y detener el servicio
            stopSelf()
        }
    }
}

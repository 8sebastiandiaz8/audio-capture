package com.audiocapture.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
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
    private var audioStreamSender: AudioStreamSender? = null
    
    // Configuración
    private var serverIp: String = ""
    private var serverPort: Int = 5000
    private var captureMicrophone: Boolean = true
    private var captureInternalAudio: Boolean = false
    
    // MediaProjection para captura de audio interno (Android 10+)
    private var mediaProjectionResultCode: Int = -1
    private var mediaProjectionData: Intent? = null

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
            mediaProjectionResultCode = it.getIntExtra("media_projection_result_code", -1)
            mediaProjectionData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelableExtra("media_projection_data", Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelableExtra("media_projection_data")
            }
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

        audioStreamSender?.disconnect()
        audioStreamSender = null
    }

    /**
     * Hilo principal de captura y transmisión de audio.
     * Lanza hilos separados para cada fuente de audio seleccionada.
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

            val captureThreads = mutableListOf<Thread>()

            // Hilo de captura de micrófono
            if (captureMicrophone) {
                val micThread = thread(start = true, name = "MicrophoneCaptureThread") {
                    captureMicrophoneAudio()
                }
                captureThreads.add(micThread)
            }

            // Hilo de captura de audio interno (solo Android 10+)
            if (captureInternalAudio && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val projection = createMediaProjection()
                if (projection != null) {
                    val internalThread = thread(start = true, name = "InternalAudioCaptureThread") {
                        captureInternalAudioStream(projection)
                    }
                    captureThreads.add(internalThread)
                } else {
                    Log.e(TAG, "No se pudo crear MediaProjection para captura de audio interno")
                }
            }

            // Esperar a que todos los hilos de captura terminen
            captureThreads.forEach { it.join() }

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

    /**
     * Captura audio del micrófono y lo envía al servidor.
     */
    private fun captureMicrophoneAudio() {
        val sender = audioStreamSender ?: run {
            Log.e(TAG, "audioStreamSender es null al iniciar captura de micrófono")
            return
        }

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        ) * BUFFER_SIZE_MULTIPLIER

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Error al obtener tamaño de buffer para micrófono")
            return
        }

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        try {
            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord (micrófono) no se inicializó correctamente")
                return
            }

            Log.d(TAG, "AudioRecord (micrófono) inicializado. Buffer size: $bufferSize")
            audioRecord.startRecording()

            val audioBuffer = ByteArray(4096)

            while (isRunning.get()) {
                val bytesRead = audioRecord.read(audioBuffer, 0, audioBuffer.size)

                if (bytesRead > 0) {
                    if (!sender.sendAudioData(audioBuffer, bytesRead)) {
                        Log.e(TAG, "Error al enviar datos de micrófono. Deteniendo captura.")
                        break
                    }
                } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "ERROR_INVALID_OPERATION en AudioRecord (micrófono)")
                    break
                } else if (bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "ERROR_BAD_VALUE en AudioRecord (micrófono)")
                    break
                }
            }
        } finally {
            if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord.stop()
            }
            audioRecord.release()
            Log.d(TAG, "AudioRecord (micrófono) liberado")
        }
    }

    /**
     * Captura audio interno del dispositivo (Android 10+) usando MediaProjection
     * y lo envía al servidor.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun captureInternalAudioStream(mediaProjection: MediaProjection) {
        val sender = audioStreamSender ?: run {
            Log.e(TAG, "audioStreamSender es null al iniciar captura de audio interno")
            return
        }
        val captureConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        ) * BUFFER_SIZE_MULTIPLIER

        val audioRecord = AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(captureConfig)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .build()

        try {
            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord (audio interno) no se inicializó correctamente")
                return
            }

            Log.d(TAG, "AudioRecord (audio interno) inicializado. Buffer size: $bufferSize")
            audioRecord.startRecording()

            val audioBuffer = ByteArray(4096)

            while (isRunning.get()) {
                val bytesRead = audioRecord.read(audioBuffer, 0, audioBuffer.size)

                if (bytesRead > 0) {
                    if (!sender.sendAudioData(audioBuffer, bytesRead)) {
                        Log.e(TAG, "Error al enviar datos de audio interno. Deteniendo captura.")
                        break
                    }
                } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "ERROR_INVALID_OPERATION en AudioRecord (audio interno)")
                    break
                } else if (bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "ERROR_BAD_VALUE en AudioRecord (audio interno)")
                    break
                }
            }
        } finally {
            if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord.stop()
            }
            audioRecord.release()
            mediaProjection.stop()
            Log.d(TAG, "AudioRecord (audio interno) y MediaProjection liberados")
        }
    }

    /**
     * Crea un objeto MediaProjection a partir del resultado guardado del intent de permiso.
     * Retorna null si no hay datos válidos de MediaProjection.
     */
    private fun createMediaProjection(): MediaProjection? {
        val data = mediaProjectionData
        if (mediaProjectionResultCode == -1 || data == null) {
            Log.e(TAG, "No hay datos de MediaProjection disponibles")
            return null
        }
        return try {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            manager.getMediaProjection(mediaProjectionResultCode, data)
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear MediaProjection", e)
            null
        }
    }
}

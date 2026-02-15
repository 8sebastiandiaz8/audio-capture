package com.audiocapture.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import android.widget.TextView
import android.view.View

/**
 * Actividad principal de la aplicación de captura de audio
 * Permite configurar la IP y puerto del servidor, seleccionar fuentes de audio
 * y controlar el inicio/detención de la captura
 */
class MainActivity : AppCompatActivity() {

    // Códigos de solicitud de permisos
    private companion object {
        const val REQUEST_RECORD_AUDIO_PERMISSION = 200
        const val REQUEST_NOTIFICATION_PERMISSION = 201
    }

    // Referencias a las vistas
    private lateinit var etServerIp: TextInputEditText
    private lateinit var etServerPort: TextInputEditText
    private lateinit var switchMicrophone: SwitchMaterial
    private lateinit var switchInternalAudio: SwitchMaterial
    private lateinit var tvInternalAudioNotSupported: TextView
    private lateinit var btnConnect: MaterialButton
    private lateinit var tvStatus: TextView

    // Estado de la aplicación
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar vistas
        initViews()

        // Verificar disponibilidad de captura de audio interno (Android 10+)
        checkInternalAudioSupport()

        // Configurar listeners
        setupListeners()

        // Solicitar permisos necesarios
        checkAndRequestPermissions()
    }

    /**
     * Inicializa las referencias a las vistas
     */
    private fun initViews() {
        etServerIp = findViewById(R.id.etServerIp)
        etServerPort = findViewById(R.id.etServerPort)
        switchMicrophone = findViewById(R.id.switchMicrophone)
        switchInternalAudio = findViewById(R.id.switchInternalAudio)
        tvInternalAudioNotSupported = findViewById(R.id.tvInternalAudioNotSupported)
        btnConnect = findViewById(R.id.btnConnect)
        tvStatus = findViewById(R.id.tvStatus)
    }

    /**
     * Verifica si el dispositivo soporta captura de audio interno
     * Solo disponible en Android 10 (API 29) o superior
     */
    private fun checkInternalAudioSupport() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Android 9 o inferior - deshabilitar audio interno
            switchInternalAudio.isEnabled = false
            switchInternalAudio.isChecked = false
            tvInternalAudioNotSupported.visibility = View.VISIBLE
        }
    }

    /**
     * Configura los listeners de los botones y switches
     */
    private fun setupListeners() {
        btnConnect.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }
    }

    /**
     * Verifica y solicita los permisos necesarios
     */
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Permiso de grabación de audio (necesario para todas las versiones)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        // Permiso de notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Solicitar permisos si es necesario
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }
    }

    /**
     * Maneja el resultado de las solicitudes de permisos
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            REQUEST_RECORD_AUDIO_PERMISSION -> {
                if (grantResults.isNotEmpty() && 
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.permission_record_audio_required),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Inicia la grabación y transmisión de audio
     */
    private fun startRecording() {
        // Verificar que se haya seleccionado al menos una fuente de audio
        if (!switchMicrophone.isChecked && !switchInternalAudio.isChecked) {
            Toast.makeText(
                this,
                getString(R.string.error_select_at_least_one),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Validar IP y puerto
        val serverIp = etServerIp.text.toString().trim()
        val serverPortStr = etServerPort.text.toString().trim()
        
        if (serverIp.isEmpty() || serverPortStr.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.error_invalid_ip),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val serverPort = try {
            serverPortStr.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(
                this,
                getString(R.string.error_invalid_ip),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Verificar permisos
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            checkAndRequestPermissions()
            return
        }

        // Iniciar el servicio de captura
        val intent = Intent(this, AudioCaptureService::class.java).apply {
            putExtra("server_ip", serverIp)
            putExtra("server_port", serverPort)
            putExtra("capture_microphone", switchMicrophone.isChecked)
            putExtra("capture_internal_audio", switchInternalAudio.isChecked)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // Actualizar UI
        isRecording = true
        btnConnect.text = getString(R.string.stop)
        tvStatus.text = getString(R.string.connecting)
        
        // Deshabilitar edición de configuración durante la grabación
        etServerIp.isEnabled = false
        etServerPort.isEnabled = false
        switchMicrophone.isEnabled = false
        switchInternalAudio.isEnabled = !switchInternalAudio.isEnabled || 
                                       Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

        Toast.makeText(this, "Iniciando captura de audio...", Toast.LENGTH_SHORT).show()
    }

    /**
     * Detiene la grabación y transmisión de audio
     */
    private fun stopRecording() {
        // Detener el servicio
        val intent = Intent(this, AudioCaptureService::class.java)
        stopService(intent)

        // Actualizar UI
        isRecording = false
        btnConnect.text = getString(R.string.connect_and_record)
        tvStatus.text = getString(R.string.disconnected)
        
        // Habilitar edición de configuración
        etServerIp.isEnabled = true
        etServerPort.isEnabled = true
        switchMicrophone.isEnabled = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            switchInternalAudio.isEnabled = true
        }

        Toast.makeText(this, "Captura detenida", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            stopRecording()
        }
    }
}

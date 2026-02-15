package com.audiocapture.app

import android.util.Log
import java.io.IOException
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Clase encargada de enviar el audio capturado al servidor por TCP Socket
 */
class AudioStreamSender(
    private val serverIp: String,
    private val serverPort: Int
) {
    companion object {
        private const val TAG = "AudioStreamSender"
        private const val SOCKET_TIMEOUT = 5000 // 5 segundos
    }

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private val isConnected = AtomicBoolean(false)

    /**
     * Establece la conexión con el servidor
     * @return true si la conexión fue exitosa, false en caso contrario
     */
    fun connect(): Boolean {
        try {
            Log.d(TAG, "Intentando conectar a $serverIp:$serverPort")
            
            socket = Socket(serverIp, serverPort).apply {
                soTimeout = SOCKET_TIMEOUT
                tcpNoDelay = true // Deshabilitar algoritmo de Nagle para baja latencia
                keepAlive = true
            }
            
            outputStream = socket?.getOutputStream()
            isConnected.set(true)
            
            Log.d(TAG, "Conectado exitosamente a $serverIp:$serverPort")
            return true
            
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Timeout al conectar al servidor", e)
            cleanup()
            return false
        } catch (e: IOException) {
            Log.e(TAG, "Error de IO al conectar al servidor", e)
            cleanup()
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al conectar", e)
            cleanup()
            return false
        }
    }

    /**
     * Envía datos de audio al servidor
     * @param data Buffer con los datos de audio
     * @param length Cantidad de bytes a enviar
     * @return true si el envío fue exitoso, false en caso contrario
     */
    fun sendAudioData(data: ByteArray, length: Int): Boolean {
        if (!isConnected.get() || outputStream == null) {
            Log.w(TAG, "No hay conexión activa para enviar datos")
            return false
        }

        try {
            outputStream?.write(data, 0, length)
            outputStream?.flush()
            return true
            
        } catch (e: SocketException) {
            Log.e(TAG, "Socket cerrado o error de conexión", e)
            isConnected.set(false)
            return false
        } catch (e: IOException) {
            Log.e(TAG, "Error de IO al enviar datos de audio", e)
            isConnected.set(false)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al enviar datos", e)
            return false
        }
    }

    /**
     * Verifica si hay una conexión activa
     */
    fun isConnected(): Boolean {
        return isConnected.get() && socket?.isConnected == true && !socket!!.isClosed
    }

    /**
     * Cierra la conexión con el servidor
     */
    fun disconnect() {
        Log.d(TAG, "Desconectando del servidor")
        isConnected.set(false)
        cleanup()
    }

    /**
     * Limpia los recursos de red
     */
    private fun cleanup() {
        try {
            outputStream?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error al cerrar outputStream", e)
        } finally {
            outputStream = null
        }

        try {
            socket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error al cerrar socket", e)
        } finally {
            socket = null
        }
    }
}

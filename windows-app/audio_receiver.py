"""
Audio Receiver - Servidor TCP que recibe audio del teléfono Android
Recibe el stream de audio PCM y lo reproduce en tiempo real
"""

import socket
import threading
import pyaudio
import wave
from datetime import datetime
import os

class AudioReceiver:
    """
    Clase que maneja la recepción de audio por TCP socket
    """
    
    # Configuración de audio - debe coincidir con la app Android
    SAMPLE_RATE = 44100
    CHANNELS = 1  # Mono
    SAMPLE_WIDTH = 2  # 16-bit = 2 bytes
    CHUNK_SIZE = 4096
    
    def __init__(self, port=5000, callback=None):
        """
        Inicializa el receptor de audio
        
        Args:
            port: Puerto TCP en el que escuchar
            callback: Función callback para eventos (estado, errores, etc.)
        """
        self.port = port
        self.callback = callback
        self.server_socket = None
        self.client_socket = None
        self.is_running = False
        self.is_connected = False
        self.receive_thread = None
        
        # PyAudio para reproducción
        self.pyaudio_instance = pyaudio.PyAudio()
        self.stream = None
        
        # Buffer para grabación
        self.recording_buffer = []
        self.is_recording = False
        
    def start(self):
        """
        Inicia el servidor TCP
        """
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            # Nota de seguridad: Binding a 0.0.0.0 permite conexiones desde cualquier interfaz
            # (WiFi, Ethernet, USB tethering). Asegúrate de que el firewall esté configurado
            # correctamente para limitar el acceso solo a redes confiables.
            self.server_socket.bind(('0.0.0.0', self.port))
            self.server_socket.listen(1)
            self.is_running = True
            
            self._log(f"Servidor iniciado en puerto {self.port}")
            self._log("Esperando conexión del teléfono...")
            
            # Iniciar hilo de recepción
            self.receive_thread = threading.Thread(target=self._receive_loop, daemon=True)
            self.receive_thread.start()
            
            return True
            
        except Exception as e:
            self._log(f"Error al iniciar servidor: {e}", error=True)
            return False
    
    def stop(self):
        """
        Detiene el servidor TCP
        """
        self._log("Deteniendo servidor...")
        self.is_running = False
        
        # Cerrar stream de audio
        if self.stream:
            self.stream.stop_stream()
            self.stream.close()
            self.stream = None
        
        # Cerrar sockets
        if self.client_socket:
            try:
                self.client_socket.close()
            except Exception:
                pass
            self.client_socket = None
            
        if self.server_socket:
            try:
                self.server_socket.close()
            except Exception:
                pass
            self.server_socket = None
        
        self.is_connected = False
        self._log("Servidor detenido")
    
    def start_recording(self):
        """
        Inicia la grabación del audio recibido
        """
        self.recording_buffer = []
        self.is_recording = True
        self._log("Iniciada grabación del audio")
    
    def stop_recording(self):
        """
        Detiene la grabación del audio
        """
        self.is_recording = False
        self._log("Detenida grabación del audio")
    
    def save_recording(self, filename=None):
        """
        Guarda el audio grabado en un archivo WAV
        
        Args:
            filename: Nombre del archivo (opcional, se genera automáticamente si no se proporciona)
        
        Returns:
            str: Ruta del archivo guardado, o None si hubo error
        """
        if not self.recording_buffer:
            self._log("No hay audio grabado para guardar", error=True)
            return None
        
        try:
            # Generar nombre de archivo si no se proporciona
            if not filename:
                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                filename = f"audio_capture_{timestamp}.wav"
            
            # Asegurar que el directorio existe
            os.makedirs(os.path.dirname(filename) if os.path.dirname(filename) else ".", exist_ok=True)
            
            # Guardar archivo WAV
            with wave.open(filename, 'wb') as wf:
                wf.setnchannels(self.CHANNELS)
                wf.setsampwidth(self.SAMPLE_WIDTH)
                wf.setframerate(self.SAMPLE_RATE)
                wf.writeframes(b''.join(self.recording_buffer))
            
            self._log(f"Audio guardado en: {filename}")
            return filename
            
        except Exception as e:
            self._log(f"Error al guardar audio: {e}", error=True)
            return None
    
    def _receive_loop(self):
        """
        Loop principal de recepción de conexiones y audio
        """
        while self.is_running:
            try:
                # Aceptar conexión del cliente (teléfono)
                self.server_socket.settimeout(1.0)  # Timeout para poder verificar is_running
                try:
                    self.client_socket, address = self.server_socket.accept()
                except socket.timeout:
                    continue
                
                self.is_connected = True
                self._log(f"Teléfono conectado desde {address}")
                
                # Abrir stream de audio para reproducción
                self.stream = self.pyaudio_instance.open(
                    format=pyaudio.paInt16,
                    channels=self.CHANNELS,
                    rate=self.SAMPLE_RATE,
                    output=True,
                    frames_per_buffer=self.CHUNK_SIZE
                )
                
                # Recibir y reproducir audio
                self.client_socket.settimeout(5.0)
                while self.is_running and self.is_connected:
                    try:
                        # Recibir datos de audio
                        data = self.client_socket.recv(self.CHUNK_SIZE)
                        
                        if not data:
                            # Conexión cerrada por el cliente
                            self._log("Conexión cerrada por el teléfono")
                            break
                        
                        # Reproducir audio
                        self.stream.write(data)
                        
                        # Guardar en buffer si está grabando
                        if self.is_recording:
                            self.recording_buffer.append(data)
                        
                    except socket.timeout:
                        # Timeout - verificar si aún estamos conectados
                        continue
                    except Exception as e:
                        self._log(f"Error al recibir audio: {e}", error=True)
                        break
                
                # Cerrar conexión del cliente
                self.is_connected = False
                if self.client_socket:
                    self.client_socket.close()
                    self.client_socket = None
                
                if self.stream:
                    self.stream.stop_stream()
                    self.stream.close()
                    self.stream = None
                
                self._log("Cliente desconectado")
                
            except Exception as e:
                if self.is_running:
                    self._log(f"Error en receive_loop: {e}", error=True)
    
    def _log(self, message, error=False):
        """
        Envía un mensaje de log a través del callback
        """
        if self.callback:
            self.callback(message, error)
    
    def cleanup(self):
        """
        Limpia recursos de PyAudio
        """
        if self.pyaudio_instance:
            self.pyaudio_instance.terminate()

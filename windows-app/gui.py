"""
GUI - Interfaz gráfica con Tkinter para la aplicación de recepción de audio
"""

import tkinter as tk
from tkinter import ttk, filedialog, messagebox
from audio_receiver import AudioReceiver
import threading

class AudioReceiverGUI:
    """
    Interfaz gráfica para controlar la recepción de audio
    """
    
    def __init__(self, root):
        """
        Inicializa la interfaz gráfica
        
        Args:
            root: Ventana raíz de Tkinter
        """
        self.root = root
        self.root.title("Audio Capture - Receptor de PC")
        self.root.geometry("600x550")
        self.root.resizable(False, False)
        
        # Estado de la aplicación
        self.receiver = None
        self.is_server_running = False
        self.is_recording = False
        
        # Crear interfaz
        self._create_widgets()
        
        # Configurar cierre de ventana
        self.root.protocol("WM_DELETE_WINDOW", self._on_closing)
    
    def _create_widgets(self):
        """
        Crea todos los widgets de la interfaz
        """
        # Frame principal con padding
        main_frame = ttk.Frame(self.root, padding="20")
        main_frame.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        
        # Título
        title_label = ttk.Label(
            main_frame, 
            text="Audio Capture - Receptor de PC",
            font=("Arial", 16, "bold")
        )
        title_label.grid(row=0, column=0, columnspan=2, pady=(0, 20))
        
        # --- Sección de configuración ---
        config_frame = ttk.LabelFrame(main_frame, text="Configuración", padding="10")
        config_frame.grid(row=1, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 10))
        
        # Puerto
        ttk.Label(config_frame, text="Puerto:").grid(row=0, column=0, sticky=tk.W, pady=5)
        self.port_entry = ttk.Entry(config_frame, width=15)
        self.port_entry.insert(0, "5000")
        self.port_entry.grid(row=0, column=1, sticky=tk.W, pady=5)
        
        ttk.Label(
            config_frame, 
            text="(Asegúrate de que el firewall permita conexiones en este puerto)",
            font=("Arial", 8)
        ).grid(row=1, column=0, columnspan=2, sticky=tk.W, pady=(0, 5))
        
        # --- Botones de control ---
        button_frame = ttk.Frame(main_frame)
        button_frame.grid(row=2, column=0, columnspan=2, pady=(0, 10))
        
        self.btn_start_server = ttk.Button(
            button_frame,
            text="▶ Iniciar Servidor",
            command=self._start_server,
            width=20
        )
        self.btn_start_server.grid(row=0, column=0, padx=5)
        
        self.btn_stop_server = ttk.Button(
            button_frame,
            text="⬛ Detener Servidor",
            command=self._stop_server,
            state=tk.DISABLED,
            width=20
        )
        self.btn_stop_server.grid(row=0, column=1, padx=5)
        
        # --- Estado ---
        status_frame = ttk.LabelFrame(main_frame, text="Estado", padding="10")
        status_frame.grid(row=3, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 10))
        
        self.status_label = ttk.Label(
            status_frame,
            text="⚫ Servidor detenido",
            font=("Arial", 10, "bold")
        )
        self.status_label.grid(row=0, column=0, sticky=tk.W)
        
        # --- Controles de grabación ---
        record_frame = ttk.LabelFrame(main_frame, text="Grabación", padding="10")
        record_frame.grid(row=4, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 10))
        
        self.btn_start_recording = ttk.Button(
            record_frame,
            text="⚫ Iniciar Grabación",
            command=self._start_recording,
            state=tk.DISABLED,
            width=20
        )
        self.btn_start_recording.grid(row=0, column=0, padx=5, pady=5)
        
        self.btn_stop_recording = ttk.Button(
            record_frame,
            text="⬛ Detener Grabación",
            command=self._stop_recording,
            state=tk.DISABLED,
            width=20
        )
        self.btn_stop_recording.grid(row=0, column=1, padx=5, pady=5)
        
        self.btn_save = ttk.Button(
            record_frame,
            text="💾 Guardar como WAV",
            command=self._save_recording,
            state=tk.DISABLED,
            width=42
        )
        self.btn_save.grid(row=1, column=0, columnspan=2, pady=(5, 0))
        
        # --- Log de eventos ---
        log_frame = ttk.LabelFrame(main_frame, text="Log de eventos", padding="10")
        log_frame.grid(row=5, column=0, columnspan=2, sticky=(tk.W, tk.E, tk.N, tk.S), pady=(0, 10))
        
        # Scrollbar para el log
        scrollbar = ttk.Scrollbar(log_frame)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        
        self.log_text = tk.Text(
            log_frame,
            height=10,
            width=65,
            state=tk.DISABLED,
            yscrollcommand=scrollbar.set
        )
        self.log_text.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.config(command=self.log_text.yview)
        
        # --- Instrucciones ---
        instructions = (
            "Instrucciones:\n"
            "1. Haz clic en 'Iniciar Servidor' para empezar a escuchar conexiones\n"
            "2. En tu teléfono Android, abre la app Audio Capture\n"
            "3. Ingresa la IP de este PC y el puerto (5000)\n"
            "4. Haz clic en 'CONECTAR Y GRABAR' en tu teléfono\n"
            "5. El audio se reproducirá automáticamente en tu PC\n"
            "6. (Opcional) Haz clic en 'Iniciar Grabación' para guardar el audio"
        )
        
        info_label = ttk.Label(
            main_frame,
            text=instructions,
            font=("Arial", 8),
            justify=tk.LEFT,
            foreground="gray"
        )
        info_label.grid(row=6, column=0, columnspan=2, sticky=tk.W)
        
        # Agregar mensaje inicial al log
        self._log("Aplicación iniciada. Haz clic en 'Iniciar Servidor' para comenzar.")
    
    def _start_server(self):
        """
        Inicia el servidor de audio
        """
        try:
            port = int(self.port_entry.get())
            
            if port < 1 or port > 65535:
                raise ValueError("Puerto debe estar entre 1 y 65535")
            
            # Crear y iniciar el receptor
            self.receiver = AudioReceiver(port=port, callback=self._log)
            
            if self.receiver.start():
                self.is_server_running = True
                
                # Actualizar UI
                self.status_label.config(text="🟢 Servidor activo - Esperando conexión...")
                self.btn_start_server.config(state=tk.DISABLED)
                self.btn_stop_server.config(state=tk.NORMAL)
                self.btn_start_recording.config(state=tk.NORMAL)
                self.port_entry.config(state=tk.DISABLED)
                
                # Iniciar thread para monitorear el estado de conexión
                threading.Thread(target=self._monitor_connection, daemon=True).start()
            else:
                messagebox.showerror("Error", "No se pudo iniciar el servidor")
                
        except ValueError as e:
            messagebox.showerror("Error", f"Puerto inválido: {e}")
    
    def _stop_server(self):
        """
        Detiene el servidor de audio
        """
        if self.receiver:
            self.receiver.stop()
            self.receiver.cleanup()
            self.receiver = None
        
        self.is_server_running = False
        
        # Actualizar UI
        self.status_label.config(text="⚫ Servidor detenido")
        self.btn_start_server.config(state=tk.NORMAL)
        self.btn_stop_server.config(state=tk.DISABLED)
        self.btn_start_recording.config(state=tk.DISABLED)
        self.btn_stop_recording.config(state=tk.DISABLED)
        self.btn_save.config(state=tk.DISABLED)
        self.port_entry.config(state=tk.NORMAL)
    
    def _start_recording(self):
        """
        Inicia la grabación del audio
        """
        if self.receiver:
            self.receiver.start_recording()
            self.is_recording = True
            
            # Actualizar UI
            self.btn_start_recording.config(state=tk.DISABLED)
            self.btn_stop_recording.config(state=tk.NORMAL)
    
    def _stop_recording(self):
        """
        Detiene la grabación del audio
        """
        if self.receiver:
            self.receiver.stop_recording()
            self.is_recording = False
            
            # Actualizar UI
            self.btn_start_recording.config(state=tk.NORMAL)
            self.btn_stop_recording.config(state=tk.DISABLED)
            self.btn_save.config(state=tk.NORMAL)
    
    def _save_recording(self):
        """
        Guarda la grabación en un archivo WAV
        """
        if not self.receiver:
            return
        
        # Diálogo para seleccionar ubicación y nombre del archivo
        filename = filedialog.asksaveasfilename(
            defaultextension=".wav",
            filetypes=[("Archivos WAV", "*.wav"), ("Todos los archivos", "*.*")],
            initialfile=f"audio_capture.wav"
        )
        
        if filename:
            result = self.receiver.save_recording(filename)
            if result:
                messagebox.showinfo("Éxito", f"Audio guardado correctamente en:\n{result}")
            else:
                messagebox.showerror("Error", "No se pudo guardar el audio")
    
    def _monitor_connection(self):
        """
        Monitorea el estado de la conexión y actualiza la UI
        """
        while self.is_server_running:
            if self.receiver and self.receiver.is_connected:
                self.root.after(0, lambda: self.status_label.config(
                    text="🟢 Conectado - Recibiendo audio"
                ))
            elif self.is_server_running:
                self.root.after(0, lambda: self.status_label.config(
                    text="🟡 Servidor activo - Esperando conexión..."
                ))
            
            # Verificar cada 500ms
            threading.Event().wait(0.5)
    
    def _log(self, message, error=False):
        """
        Agrega un mensaje al log de eventos
        
        Args:
            message: Mensaje a agregar
            error: Si es True, el mensaje se muestra en rojo
        """
        def update_log():
            self.log_text.config(state=tk.NORMAL)
            
            if error:
                self.log_text.insert(tk.END, f"❌ {message}\n", "error")
                self.log_text.tag_config("error", foreground="red")
            else:
                self.log_text.insert(tk.END, f"• {message}\n")
            
            self.log_text.see(tk.END)
            self.log_text.config(state=tk.DISABLED)
        
        # Asegurar que se actualice en el thread principal de Tkinter
        self.root.after(0, update_log)
    
    def _on_closing(self):
        """
        Maneja el cierre de la ventana
        """
        if self.is_server_running:
            if messagebox.askokcancel("Salir", "¿Deseas detener el servidor y salir?"):
                self._stop_server()
                self.root.destroy()
        else:
            self.root.destroy()

def main():
    """
    Función principal para iniciar la GUI
    """
    root = tk.Tk()
    app = AudioReceiverGUI(root)
    root.mainloop()

if __name__ == "__main__":
    main()

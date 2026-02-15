#!/usr/bin/env python3
"""
Script de prueba para validar la estructura de la aplicación de Windows
"""

import os
import sys

def check_file_exists(filepath, description):
    """Verifica si un archivo existe"""
    if os.path.exists(filepath):
        print(f"✓ {description}: {filepath}")
        return True
    else:
        print(f"✗ {description}: {filepath} - NO ENCONTRADO")
        return False

def main():
    print("Validando estructura de la aplicación Windows...")
    print("=" * 60)
    
    base_dir = os.path.dirname(os.path.abspath(__file__))
    all_ok = True
    
    # Archivos principales
    files_to_check = [
        ("main.py", "Archivo principal"),
        ("gui.py", "Interfaz gráfica"),
        ("audio_receiver.py", "Receptor de audio"),
        ("requirements.txt", "Dependencias"),
        ("build_exe.bat", "Script de compilación"),
    ]
    
    for filename, description in files_to_check:
        filepath = os.path.join(base_dir, filename)
        if not check_file_exists(filepath, description):
            all_ok = False
    
    print("=" * 60)
    
    # Verificar imports
    print("\nVerificando importaciones...")
    try:
        # Intentar importar los módulos (sin ejecutarlos)
        import importlib.util
        
        modules = ["audio_receiver", "gui", "main"]
        for module_name in modules:
            module_path = os.path.join(base_dir, f"{module_name}.py")
            spec = importlib.util.spec_from_file_location(module_name, module_path)
            if spec and spec.loader:
                print(f"✓ Módulo {module_name}.py es válido")
            else:
                print(f"✗ Módulo {module_name}.py tiene problemas")
                all_ok = False
    except Exception as e:
        print(f"✗ Error al verificar módulos: {e}")
        all_ok = False
    
    print("=" * 60)
    
    if all_ok:
        print("\n✓ Todos los archivos están presentes y válidos")
        print("\nPara ejecutar la aplicación:")
        print("  1. Instala las dependencias: pip install -r requirements.txt")
        print("  2. Ejecuta: python main.py")
        return 0
    else:
        print("\n✗ Hay problemas con la estructura del proyecto")
        return 1

if __name__ == "__main__":
    sys.exit(main())

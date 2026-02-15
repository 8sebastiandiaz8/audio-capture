@echo off
echo Instalando PyInstaller...
pip install pyinstaller

echo.
echo Construyendo ejecutable...
pyinstaller --onefile --windowed --name AudioCapture main.py

echo.
echo Ejecutable creado en: dist\AudioCapture.exe
pause

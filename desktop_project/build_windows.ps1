$ErrorActionPreference = "Stop"
py -3.11 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements.txt
pip install pyinstaller
pyinstaller --noconfirm --windowed --name AASM2-Desktop --collect-all PySide6 app/main.py
Write-Host "Build complete. See dist/AASM2-Desktop/"

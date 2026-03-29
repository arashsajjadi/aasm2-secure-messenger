#!/usr/bin/env bash
set -euo pipefail
python3 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
pip install -r requirements.txt
pip install pyinstaller
pyinstaller --noconfirm --windowed --name AASM2-Desktop --collect-all PySide6 app/main.py
echo "Build complete. See dist/AASM2-Desktop/"

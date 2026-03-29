# AASM2 Desktop

A modern desktop UI for Windows and Linux using PySide6.

## Features

- Text encryption and decryption
- File encryption and decryption
- Compatible with the Android project in this bundle
- AES 256 GCM authenticated encryption
- Dark modern UI
- Ready for Windows EXE and Linux packaging

## Setup

### Windows PowerShell

```powershell
py -3.11 -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install --upgrade pip
pip install -r requirements.txt
python -m app.main
```

### Linux

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt
python -m app.main
```

## Build Windows EXE

```powershell
py -3.11 -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install --upgrade pip
pip install -r requirements.txt
pip install pyinstaller
pyinstaller --noconfirm --windowed --name AASM2-Desktop --collect-all PySide6 app/main.py
```

The EXE will be created inside `dist/AASM2-Desktop/`.

## Build Linux binary

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt
pip install pyinstaller
pyinstaller --noconfirm --windowed --name AASM2-Desktop --collect-all PySide6 app/main.py
```

The Linux bundle will be created inside `dist/AASM2-Desktop/`.

## Notes

- Build the EXE on Windows.
- Build the Linux binary on Linux.
- PyInstaller is not cross compiling here.

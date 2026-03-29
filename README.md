# AASM2 Secure Messenger

A cross-platform secure messaging and file encryption project built around a shared **AASM2** encrypted format.

This repository is organized as a monorepo and currently contains:

- an **Android** application
- a **desktop** application for **Linux** and **Windows**
- the shared **AASM2 specification**, compatibility notes, and test vectors

## Repository Structure

```text
.
├── android_project/
│   ├── app/
│   ├── build.gradle.kts
│   ├── gradle.properties
│   ├── settings.gradle.kts
│   └── README.md
├── desktop_project/
│   ├── app/
│   ├── build_linux.sh
│   ├── build_windows.ps1
│   ├── requirements.txt
│   ├── verify_test_vector.py
│   └── README.md
└── spec/
    ├── AASM2_SPEC.md
    ├── COMPATIBILITY_NOTES.md
    └── test_vectors.json
````

## Goals

* Use a single shared encrypted format across all supported platforms
* Keep Android, Linux, and Windows implementations compatible
* Provide a clean and practical user experience on each platform
* Maintain test vectors and format documentation in one place
* Expand release packaging for Android APK, Linux builds, and Windows executables

## Current Contents

### Android

The Android project is located in `android_project/` and contains the native Android implementation.

Main areas:

* Android app source
* Kotlin crypto implementation
* Gradle build files

### Desktop

The desktop project is located in `desktop_project/` and contains the desktop implementation intended for Linux and Windows.

Main areas:

* Python application source
* shared crypto core
* Linux build script
* Windows build script
* compatibility verification script

### Specification

The `spec/` directory contains the shared format documentation and verification material:

* AASM2 format specification
* compatibility notes
* test vectors

## Build Notes

### Android

The Android project can be built from the `android_project/` directory using Gradle.

Typical debug build:

```bash
cd android_project
./gradlew assembleDebug
```

### Linux Desktop

The Linux desktop build is managed from `desktop_project/`.

Typical setup:

```bash
cd desktop_project
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
pip install pyinstaller
bash build_linux.sh
```

### Windows Desktop

The Windows desktop build is managed from `desktop_project/` using the provided PowerShell script.

## Compatibility

The repository is intended to keep all implementations aligned around the same encrypted format.

Reference files:

* `spec/AASM2_SPEC.md`
* `spec/COMPATIBILITY_NOTES.md`
* `spec/test_vectors.json`

Desktop verification helper:

```bash
cd desktop_project
python verify_test_vector.py
```

## Roadmap

Planned improvements include:

* more polished Android releases
* more reliable Linux packaging
* Windows release polish
* release automation
* build cleanup
* UI refinement
* improved distribution artifacts for each platform

## Status

This is the initial public monorepo for the AASM2 project. The repository already includes the main source layout for Android and desktop development, and it will be expanded incrementally with improved platform-specific builds and release assets.

## License

License not added yet.

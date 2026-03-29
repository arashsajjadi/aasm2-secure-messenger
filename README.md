# AASM2 Secure Messenger

AASM2 Secure Messenger is a cross platform encrypted messaging and file transfer project built around a shared **AASM2** format.

**Author:** Seyed Arash Sajjadi

This repository currently contains:

- an **Android** application
- a **desktop** application for **Linux**
- a **desktop** application path prepared for **Windows**
- the shared **AASM2 specification**, compatibility notes, and test vectors

The main idea is simple:

- all platforms use the same encrypted container format
- encryption and decryption happen locally on the user's own device
- no central server is required to read user files
- if multiple implementations follow the same format, they can read each other's encrypted output

## Why this project exists

This code was written for personal communication under unreliable internet conditions and with low trust in some local messaging apps.

The original motivation was practical and personal:
a simple private tool for exchanging messages and files without relying on platforms that were not fully trusted.

Hopefully it can also be useful to others.

And yes, so far the dramatic real world payload was not classified state secrets.
It was mostly a couple of Nowruz Haft Sin travel photos and some internet connection screenshots between two brothers.
Still, the bigger hope is more serious than that:
the internet should be a safer place everywhere for everyone.

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

## Core Design

AASM2 is designed around a shared encrypted file format so that the Android, Linux, and Windows versions can interoperate.

That means:

* a message encrypted on Android should be decryptable on Linux
* a file encrypted on Linux should be decryptable on Windows
* a file encrypted on Windows should be decryptable on Android

This compatibility is achieved by keeping these parts identical across platforms:

* the same container format
* the same byte layout
* the same key derivation process
* the same authenticated encryption design
* the same test vectors

## Cryptographic Model

AASM2 uses password based authenticated encryption.

At a high level:

1. the user provides a password
2. the password is transformed into a cryptographic key using a key derivation function
3. a random salt and random nonce are generated
4. the plaintext is encrypted
5. an authentication tag is produced so tampering can be detected during decryption

The construction is based on standard cryptographic building blocks:

* **AES 256 GCM** for authenticated encryption
* **PBKDF2 HMAC SHA 256** for password based key derivation
* random salt for key derivation
* random nonce for encryption

## Mathematical Sketch

Let:

* `P` be the user password
* `S` be a random salt
* `N` be a random nonce
* `M` be the plaintext
* `K` be the derived encryption key
* `C` be the ciphertext
* `T` be the authentication tag

### Key derivation

The key is derived as:

```text
K = PBKDF2-HMAC-SHA256(P, S, r, dkLen)
```

where:

* `P` is the password
* `S` is the random salt
* `r` is the iteration count
* `dkLen` is the desired key length

The role of PBKDF2 is to increase the cost of brute force guessing by making each password trial more expensive.

### Encryption

Authenticated encryption is then performed as:

```text
(C, T) = AES-256-GCM-Encrypt(K, N, M, AAD)
```

where:

* `K` is the derived key
* `N` is the nonce
* `M` is the plaintext
* `AAD` is optional authenticated metadata
* `C` is the ciphertext
* `T` is the authentication tag

Decryption succeeds only if tag verification succeeds:

```text
M = AES-256-GCM-Decrypt(K, N, C, T, AAD)
```

If authentication fails, decryption must reject the ciphertext.

## Why this is considered a reasonable design

This project does not rely on a homemade custom cipher.

Instead, it is built around standard and widely studied primitives.

### AES 256

AES is one of the most widely used symmetric encryption standards in the world.

A 256 bit key means the brute force key space is:

```text
2^256
```

which is astronomically large.

### GCM authentication

GCM provides both confidentiality and integrity.

That means a modified ciphertext should not silently decrypt to plausible garbage.
Instead, authentication should fail.

### PBKDF2

Human passwords are not uniformly random.
PBKDF2 does not magically make weak passwords perfect, but it increases the work required for offline guessing.

If a single password trial costs `W`, then testing `q` guesses costs roughly:

```text
q × W
```

Increasing the iteration count increases `W`, which slows brute force attacks.

### Salt and nonce separation

A random salt prevents identical passwords from deriving identical keys across different encrypted items.

A random nonce prevents the same plaintext encrypted under the same key from always producing the same ciphertext.

## Security interpretation

A more careful statement is:

* if AES-GCM remains secure as assumed
* if HMAC-SHA256 remains secure as assumed
* if PBKDF2 is used correctly
* if nonces are not reused incorrectly
* if users choose sufficiently strong passwords
* if the device itself is not compromised

then the construction is aligned with standard modern cryptographic practice.

This is **not** the same as saying the whole application is formally proven secure in every real world condition.

## Why the three versions work together

The Android, Linux, and Windows versions are intended to stay compatible because they all target the same AASM2 format.

They stay in sync through:

* shared format documentation in `spec/AASM2_SPEC.md`
* compatibility notes in `spec/COMPATIBILITY_NOTES.md`
* test vectors in `spec/test_vectors.json`
* implementation level verification in the desktop verification helper

If a platform implementation matches the spec and passes the same test vectors, then it should be able to decrypt data produced by the others.

## Privacy model

AASM2 is designed as a **local encryption tool**.

That means:

* encryption happens on your device
* decryption happens on your device
* plaintext files are not sent to a central server by the tool itself
* the tool does not need to inspect your private content remotely

In other words, the intended usage model is local processing on your own phone or computer.

If a user later sends the encrypted output through a third party messenger, email service, or cloud storage platform, then transport and metadata are outside the scope of this repository.
But the encryption and decryption themselves are meant to happen locally.

## Practical limitations

No security tool should promise magic.

Important points:

* weak passwords are still weak passwords
* local malware can compromise any local application
* screen capture, clipboard leaks, and infected devices are outside the protection of encryption alone
* unaudited software should still be used with healthy caution

So the right claim is:
this project is designed around standard, sensible cryptographic components and local processing, not that it is beyond all possible failure.

## Current Contents

### Android

The Android project is in `android_project/`.

It contains:

* the Android app source
* the Kotlin implementation
* the Android crypto integration
* Gradle build files

### Desktop

The desktop project is in `desktop_project/`.

It contains:

* the Python desktop app source
* the shared crypto core
* Linux build script
* Windows build script
* compatibility verification script

### Specification

The `spec/` directory contains:

* the AASM2 format specification
* compatibility notes
* test vectors used to keep implementations aligned

## Downloads

Prebuilt binaries are intended to be published in the GitHub **Releases** section.

Typical assets:

* `AASM2-Android-*.apk`
* `AASM2-Linux-*.tar.gz`
* `AASM2-Windows-*.zip` or `AASM2-Windows-*.exe`

If you are a normal user, you should generally download files from **Releases**, not from the source tree.

## How to run

### Android

Download the Android APK from the Releases page.

Then:

1. transfer the APK to your Android phone if needed
2. install it
3. open the app
4. enter your password
5. encrypt or decrypt locally on the device

### Linux

Download the Linux archive from the Releases page.

Typical example:

```bash
tar -xzf AASM2-Linux-x86_64.tar.gz
cd AASM2-Desktop
./AASM2-Desktop
```

### Windows

Download the Windows release asset from the Releases page.

Then:

1. extract it if it is zipped
2. run the executable
3. encrypt or decrypt locally on your machine

## Developer Build Notes

### Android

```bash
cd android_project
./gradlew assembleDebug
```

### Linux Desktop

```bash
cd desktop_project
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
pip install pyinstaller
bash build_linux.sh
```

### Windows Desktop

The Windows desktop build is prepared through the provided PowerShell script in `desktop_project/`.

## Compatibility Verification

Desktop verification helper:

```bash
cd desktop_project
python verify_test_vector.py
```

Reference files:

* `spec/AASM2_SPEC.md`
* `spec/COMPATIBILITY_NOTES.md`
* `spec/test_vectors.json`

## Roadmap

Planned improvements include:

* polished Android release builds
* cleaner Linux packaging
* Windows release packaging
* release automation
* better UI refinement
* cleaner build reproducibility
* improved downloadable artifacts for end users

## Status

This is the initial public monorepo for the AASM2 project.

The repository already contains the main source layout for Android and desktop development.
It is expected to evolve over time with better packaging, better releases, and improved cross platform polish.

## Personal note

This tool started as a personal project for communication with my brother in difficult network conditions and in a context where I did not fully trust some domestic applications.

So this repository is technical, but also personal.

So far the actual traffic was not exactly world changing.
Mostly two travel photos of the Haft Sin table and some internet connection screenshots.

Still, the bigger hope is serious:
privacy should not be a luxury, and a safer internet should be normal for everyone everywhere.

## Author

Seyed Arash Sajjadi

## License

This project is released under the MIT License.
See the `LICENSE` file for details.

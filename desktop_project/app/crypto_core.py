from __future__ import annotations

import base64
import json
import os
import struct
from dataclasses import dataclass
from typing import Any

from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
from cryptography.hazmat.primitives import hashes


MAGIC = b"AASM2"
FORMAT_VERSION = 1
KDF_PBKDF2_SHA256 = 1
CIPHER_AES_256_GCM = 1
DEFAULT_ITERATIONS = 350_000
SALT_LENGTH = 16
NONCE_LENGTH = 12


@dataclass(slots=True)
class DecryptionResult:
    plaintext: bytes
    metadata: dict[str, Any]


class Aasm2Error(Exception):
    pass


class InvalidPasswordOrDataError(Aasm2Error):
    pass


class InvalidFormatError(Aasm2Error):
    pass


def _derive_key(password: str, salt: bytes, iterations: int) -> bytes:
    if not password:
        raise Aasm2Error("Password cannot be empty.")
    if iterations <= 0:
        raise Aasm2Error("Invalid PBKDF2 iteration count.")
    kdf = PBKDF2HMAC(
        algorithm=hashes.SHA256(),
        length=32,
        salt=salt,
        iterations=iterations,
    )
    return kdf.derive(password.encode("utf-8"))


def _encode_metadata(metadata: dict[str, Any] | None) -> bytes:
    return json.dumps(metadata or {}, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def _build_header(salt: bytes, nonce: bytes, metadata_bytes: bytes, iterations: int) -> bytes:
    fixed = struct.pack(
        ">BBBIBBI",
        FORMAT_VERSION,
        KDF_PBKDF2_SHA256,
        CIPHER_AES_256_GCM,
        iterations,
        len(salt),
        len(nonce),
        len(metadata_bytes),
    )
    return MAGIC + fixed + salt + nonce + metadata_bytes


def encrypt_bytes(data: bytes, password: str, metadata: dict[str, Any] | None = None, iterations: int = DEFAULT_ITERATIONS) -> bytes:
    salt = os.urandom(SALT_LENGTH)
    nonce = os.urandom(NONCE_LENGTH)
    metadata_bytes = _encode_metadata(metadata)
    header = _build_header(salt, nonce, metadata_bytes, iterations)
    key = _derive_key(password, salt, iterations)
    ciphertext = AESGCM(key).encrypt(nonce, data, header)
    return header + ciphertext


def decrypt_bytes(blob: bytes, password: str) -> DecryptionResult:
    minimum = len(MAGIC) + struct.calcsize(">BBBIBBI") + SALT_LENGTH + NONCE_LENGTH + 16
    if len(blob) < minimum:
        raise InvalidFormatError("Encrypted data is too short.")
    if not blob.startswith(MAGIC):
        raise InvalidFormatError("Invalid AASM2 magic header.")

    pos = len(MAGIC)
    version, kdf_id, cipher_id, iterations, salt_len, nonce_len, meta_len = struct.unpack(
        ">BBBIBBI", blob[pos:pos + struct.calcsize(">BBBIBBI")]
    )
    pos += struct.calcsize(">BBBIBBI")

    if version != FORMAT_VERSION:
        raise InvalidFormatError("Unsupported AASM2 format version.")
    if kdf_id != KDF_PBKDF2_SHA256 or cipher_id != CIPHER_AES_256_GCM:
        raise InvalidFormatError("Unsupported AASM2 cryptographic identifiers.")
    if salt_len <= 0 or nonce_len <= 0:
        raise InvalidFormatError("Invalid salt or nonce length.")

    salt = blob[pos:pos + salt_len]
    pos += salt_len
    nonce = blob[pos:pos + nonce_len]
    pos += nonce_len
    metadata_bytes = blob[pos:pos + meta_len]
    pos += meta_len

    if pos >= len(blob):
        raise InvalidFormatError("Encrypted payload is missing.")

    header = blob[:pos]
    ciphertext = blob[pos:]

    try:
        metadata = json.loads(metadata_bytes.decode("utf-8")) if metadata_bytes else {}
    except json.JSONDecodeError as exc:
        raise InvalidFormatError("Metadata JSON is corrupted.") from exc

    key = _derive_key(password, salt, iterations)
    try:
        plaintext = AESGCM(key).decrypt(nonce, ciphertext, header)
    except Exception as exc:
        raise InvalidPasswordOrDataError("Wrong password or tampered data.") from exc

    return DecryptionResult(plaintext=plaintext, metadata=metadata)


def encrypt_text_to_base64(text: str, password: str) -> str:
    blob = encrypt_bytes(text.encode("utf-8"), password, {"type": "text"})
    return base64.b64encode(blob).decode("ascii")


def decrypt_text_from_base64(raw: str, password: str) -> str:
    if not raw.strip():
        raise Aasm2Error("Input is empty.")
    try:
        blob = base64.b64decode(raw.encode("ascii"), validate=True)
    except Exception as exc:
        raise InvalidFormatError("Text input is not valid Base64.") from exc
    result = decrypt_bytes(blob, password)
    return result.plaintext.decode("utf-8")

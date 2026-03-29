# AASM2 Format Specification

## Goals

AASM2 is a compact encrypted container format for cross platform text and file encryption.

It is designed for exact compatibility between:

- Android
- Windows
- Linux

## Cryptography

- KDF: PBKDF2 HMAC SHA256
- Iterations: 350000 by default
- Derived key length: 32 bytes
- Cipher: AES 256 GCM
- Salt length: 16 bytes
- Nonce length: 12 bytes
- Tag length: 16 bytes as appended by AES GCM

## Header layout

All integer fields are big endian.

| Field | Size |
|---|---:|
| Magic `AASM2` | 5 bytes |
| Format version | 1 byte |
| KDF id | 1 byte |
| Cipher id | 1 byte |
| PBKDF2 iterations | 4 bytes |
| Salt length | 1 byte |
| Nonce length | 1 byte |
| Metadata JSON length | 4 bytes |
| Salt | variable |
| Nonce | variable |
| Metadata JSON UTF 8 | variable |
| Ciphertext plus GCM tag | remaining bytes |

## Fixed identifiers

- Magic: `AASM2`
- Version: `1`
- KDF id for PBKDF2 SHA256: `1`
- Cipher id for AES 256 GCM: `1`

## Metadata rules

Metadata must be JSON UTF 8 encoded using compact separators and sorted keys.

Recommended keys:

- `type`: `text` or `file`
- `name`: original file name when encrypting a file
- `mime`: optional MIME type

## Authenticated data

The entire header up to and including the metadata JSON is used as AES GCM authenticated associated data.

## Text mode

Text is encrypted as UTF 8 bytes.
The final AASM2 blob is then encoded using standard Base64 without line breaks.

## File mode

The raw encrypted blob is written directly to disk, typically with extension `.aasm2`.

## Error handling

Applications should distinguish these cases:

- Wrong password or tampered data
- Invalid container format
- Empty password
- Corrupted metadata
- File access failure

## Compatibility advice

Do not change any of the following without defining a new format version:

- Magic bytes
- Header ordering
- Endianness
- Metadata encoding rules
- KDF or cipher identifiers

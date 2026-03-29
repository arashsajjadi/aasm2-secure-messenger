from __future__ import annotations

import json
from pathlib import Path

from app.crypto_core import decrypt_bytes


def main() -> None:
    vector = json.loads(Path("../spec/test_vectors.json").read_text(encoding="utf-8"))
    blob = bytes.fromhex(vector["blob_hex"])
    result = decrypt_bytes(blob, vector["password"])
    plaintext = result.plaintext.decode("utf-8")
    assert plaintext == vector["plaintext_utf8"]
    print("[OK] Test vector decrypted successfully")
    print(result.metadata)


if __name__ == "__main__":
    main()

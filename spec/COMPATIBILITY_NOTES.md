# Compatibility Notes

## How to verify compatibility

### Desktop side

Run:

```bash
python verify_test_vector.py
```

If the script prints success, the desktop implementation is decoding the canonical AASM2 vector correctly.

### Android side

Use the values in `test_vectors.json` to create a small instrumentation or unit test that:

1. Converts `blob_hex` to bytes.
2. Calls `Aasm2Crypto.decryptBytes`.
3. Confirms that plaintext equals `plaintext_utf8`.
4. Confirms that metadata contains the expected file name and type.

## Important note about random encryption outputs

Fresh encryption uses a random salt and random nonce for every operation.
This means two encryptions of the same plaintext with the same password will produce different outputs.
That is expected and correct.

#!/usr/bin/env python3
"""Verify the packaged English eSpeak data; optionally compare host phonemization."""
import argparse
import ctypes
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parent.parent
PREFIX = "assets/espeak-ng-data/"
EXAMPLES = [
    "Never, potter, poured.",
    "The quick brown fox jumps over the lazy dog.",
    "Quuxflorp and eBook are unfamiliar words.",
    "Dr. Smith read the record yesterday and will read it tomorrow.",
    "The year is 2026 and the price is $123.45.",
    "The café serves jalapeño and crème brûlée.",
]
VOICES = ["en-us", "en-gb-x-rp"]


def worker(library, parent):
    lib = ctypes.CDLL(library)
    lib.espeak_Initialize.argtypes = [ctypes.c_int, ctypes.c_int, ctypes.c_char_p, ctypes.c_int]
    lib.espeak_Initialize.restype = ctypes.c_int
    lib.espeak_SetVoiceByName.argtypes = [ctypes.c_char_p]
    lib.espeak_SetVoiceByName.restype = ctypes.c_int
    lib.espeak_TextToPhonemes.argtypes = [ctypes.POINTER(ctypes.c_char_p), ctypes.c_int, ctypes.c_int]
    lib.espeak_TextToPhonemes.restype = ctypes.c_char_p
    if lib.espeak_Initialize(2, 0, str(parent).encode(), 0) <= 0:
        raise RuntimeError("eSpeak initialization failed")
    result = {}
    for voice in VOICES:
        if lib.espeak_SetVoiceByName(voice.encode()) != 0:
            raise RuntimeError(f"eSpeak could not load {voice}")
        outputs = []
        for example in EXAMPLES:
            remaining = ctypes.c_char_p(example.encode())
            parts = []
            while remaining.value:
                phonemes = lib.espeak_TextToPhonemes(ctypes.byref(remaining), 1, 2)
                if phonemes is None:
                    raise RuntimeError(f"eSpeak failed on {example!r}")
                parts.append(phonemes.decode())
            if not any(parts):
                raise RuntimeError(f"No pronunciation for {example!r}")
            outputs.append(parts)
        result[voice] = outputs
    print(json.dumps(result, ensure_ascii=False, sort_keys=True))


def dictionary(path):
    relative = path.removeprefix(PREFIX)
    return path.startswith(PREFIX) and "/" not in relative and relative.endswith("_dict")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--espeak-library", help="host libespeak-ng shared library for phonemization comparison")
    parser.add_argument("--worker", nargs=2, metavar=("LIBRARY", "DATA_PARENT"), help=argparse.SUPPRESS)
    args = parser.parse_args()
    if args.worker:
        worker(*args.worker)
        return

    original = ROOT / "app/libs/piper-phonemize-v1.5.9.aar"
    trimmed = ROOT / "app/build/generated/piper/piper-phonemize-english.aar"
    apk = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
    with zipfile.ZipFile(original) as full, zipfile.ZipFile(trimmed) as reduced, zipfile.ZipFile(apk) as package:
        full_files = {n for n in full.namelist() if not n.endswith("/")}
        reduced_files = {n for n in reduced.namelist() if not n.endswith("/")}
        removed = {n for n in full_files if dictionary(n) and n != PREFIX + "en_dict"}
        assert removed and reduced_files == full_files - removed, "Unexpected AAR contents"
        assert all(full.read(n) == reduced.read(n) for n in reduced_files), "Retained AAR files changed"
        apk_dicts = {n for n in package.namelist() if dictionary(n)}
        assert apk_dicts == {PREFIX + "en_dict"}, f"Unexpected APK dictionaries: {apk_dicts}"
        assert package.read(PREFIX + "en_dict") == full.read(PREFIX + "en_dict"), "English dictionary changed"
        print(f"Verified {len(removed)} non-English dictionaries removed; all retained files are unchanged.")

        if args.espeak_library:
            with tempfile.TemporaryDirectory(prefix="paradee-espeak-check-") as temp:
                outputs = []
                for name, archive in [("full", full), ("english", reduced)]:
                    parent = Path(temp) / name
                    for entry in archive.infolist():
                        if entry.filename.startswith(PREFIX) and not entry.is_dir():
                            archive.extract(entry, parent)
                    command = [sys.executable, __file__, "--worker", args.espeak_library, str(parent / "assets")]
                    outputs.append(json.loads(subprocess.check_output(command, text=True)))
                assert outputs[0] == outputs[1], "English pronunciations differ after trimming"
            print(f"Host eSpeak matched for {len(EXAMPLES)} sentences in both English accents.")


if __name__ == "__main__":
    main()

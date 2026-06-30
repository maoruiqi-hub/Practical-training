#!/usr/bin/env python3
from __future__ import annotations

import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RESOURCE_DIR = ROOT / "resource" / "LessonResource" / "python"
MUXER = ROOT / "scripts" / "AudioMuxer"
MUXER_SOURCE = ROOT / "scripts" / "AudioMuxer.swift"
SWIFT_CACHE = ROOT / ".swift-module-cache"

VOICE = "Tingting"
RATE = "165"


def ensure_muxer() -> None:
    if MUXER.exists():
        return
    SWIFT_CACHE.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        ["swiftc", "-module-cache-path", str(SWIFT_CACHE), str(MUXER_SOURCE), "-o", str(MUXER)],
        check=True,
    )


def read_captions(vtt_path: Path) -> str:
    lines: list[str] = []
    for raw in vtt_path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line == "WEBVTT" or "-->" in line:
            continue
        lines.append(line)
    text = " ".join(lines)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def generate_audio(text: str, output: Path) -> None:
    subprocess.run(
        ["say", "-v", VOICE, "-r", RATE, "-o", str(output), text],
        check=True,
    )


def mux(video: Path, audio: Path, output: Path) -> None:
    subprocess.run([str(MUXER), str(video), str(audio), str(output)], check=True)


def main() -> None:
    ensure_muxer()
    videos = sorted(
        path for path in RESOURCE_DIR.glob("lesson_*.mp4")
        if not path.stem.endswith("_voice")
    )
    if not videos:
        raise SystemExit("No lesson mp4 files found")

    for video in videos:
        stem = video.stem
        vtt = RESOURCE_DIR / f"{stem}.vtt"
        if not vtt.exists():
            raise FileNotFoundError(vtt)

        audio = RESOURCE_DIR / f"{stem}.aiff"
        voiced = RESOURCE_DIR / f"{stem}_voice.mp4"
        captions = read_captions(vtt)
        generate_audio(captions, audio)
        mux(video, audio, voiced)
        print(f"generated {voiced.name}")

    print(f"done: {RESOURCE_DIR}")


if __name__ == "__main__":
    main()

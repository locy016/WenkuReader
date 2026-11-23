"""Build an Android libmobi.so from the vendored or a modified source tree."""

import argparse
import os
import re
import subprocess
from pathlib import Path


TARGETS = {"arm64-v8a": "aarch64-linux-android26", "armeabi-v7a": "armv7a-linux-androideabi26"}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    default_source = Path(__file__).resolve().parents[2] / "app/src/main/cpp/libmobi"
    parser.add_argument("--source", type=Path, default=default_source)
    parser.add_argument("--ndk", type=Path, required=True)
    parser.add_argument("--abi", choices=TARGETS, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--ldflag", action="append", default=[])
    args = parser.parse_args()

    compiler_name = "clang.exe" if os.name == "nt" else "clang"
    compilers = list(args.ndk.glob(f"toolchains/llvm/prebuilt/*/bin/{compiler_name}"))
    if len(compilers) != 1:
        parser.error("Expected exactly one host Clang toolchain in the Android NDK")

    configure = (args.source / "configure.ac").read_text(encoding="utf-8")
    version = re.search(r"AC_INIT\(\[libmobi\], \[([0-9.]+)\]", configure)
    if not version:
        parser.error("Could not determine the libmobi version from configure.ac")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    files = ("buffer", "compression", "debug", "index", "memory", "meta",
             "parse_rawml", "read", "structure", "util", "write", "opf", "xmlwriter")
    subprocess.run([
        compilers[0].resolve(), f"--target={TARGETS[args.abi]}", "-std=gnu99",
        "-shared", "-fPIC", "-O2", f'-DPACKAGE_VERSION="{version[1]}"',
        "-DUSE_XMLWRITER", "-DHAVE_UNISTD_H", "-DHAVE_GETOPT", "-DHAVE_STRDUP",
        "-DHAVE_SYS_RESOURCE_H", "-DMOBI_INLINE=inline", "-DHAVE_ATTRIBUTE_NORETURN",
        *(args.source / "src" / f"{file}.c" for file in files),
        "-Wl,-soname,libmobi.so", "-Wl,-z,max-page-size=16384", "-lz", "-lm", *args.ldflag,
        "-o", args.output,
    ], check=True)


if __name__ == "__main__":
    main()

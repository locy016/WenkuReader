"""Create an unsigned APK with compatible libmobi binaries in one or more ABIs."""

import argparse
import re
import shutil
from pathlib import Path
from zipfile import ZipFile


MOBI_PATH = re.compile(r"lib/([^/]+)/libmobi\.so")
JAR_SIGNATURE = re.compile(r"META-INF/(?:MANIFEST\.MF|SIG-[^/]+|[^/]+\.(?:SF|RSA|DSA|EC))", re.IGNORECASE)


def repackage(original: Path, output: Path, libraries: dict[str, Path]) -> None:
    with ZipFile(original) as source:
        names = source.namelist()
        if len(names) != len(set(names)):
            raise ValueError("APK has duplicate ZIP entries")
        packaged = {match[1]: name for name in names if (match := MOBI_PATH.fullmatch(name))}
        missing = libraries.keys() - packaged.keys()
        if missing:
            raise ValueError(f"No bundled libmobi for: {', '.join(sorted(missing))}")
        replacements = {packaged[abi]: file.read_bytes() for abi, file in libraries.items()}
        for name, replacement in replacements.items():
            with source.open(name) as bundled:
                # EI_CLASS, EI_DATA and e_machine must match the shipped library.
                header = bundled.read(20)
                if (len(replacement) < 20 or replacement[:6] != header[:6]
                        or replacement[18:20] != header[18:20] or header[:4] != b"\x7fELF"):
                    raise ValueError(f"Replacement has incompatible ELF architecture: {name}")

        # Rewriting the ZIP also discards any APK v2/v3 signing block.
        with ZipFile(output, "x") as apk:
            for entry in source.infolist():
                if JAR_SIGNATURE.fullmatch(entry.filename):
                    continue
                if entry.filename in replacements:
                    apk.writestr(entry, replacements[entry.filename])
                else:
                    with source.open(entry) as contents, apk.open(entry, "w") as target:
                        shutil.copyfileobj(contents, target)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("output", type=Path, help="New unsigned APK file")
    parser.add_argument("--library", action="append", required=True, metavar="ABI=LIBMOBI_SO")
    args = parser.parse_args()
    libraries = {}
    for item in args.library:
        abi, separator, file = item.partition("=")
        if not abi or not separator or not file or abi in libraries:
            parser.error(f"Expected a unique ABI=LIBMOBI_SO, got {item!r}")
        libraries[abi] = Path(file)
    try:
        repackage(args.apk, args.output, libraries)
    except (OSError, ValueError) as error:
        parser.exit(1, f"{error}\n")


if __name__ == "__main__":
    main()

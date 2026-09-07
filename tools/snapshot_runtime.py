"""Snapshot the unchanged target API from a git revision, without modifying the target."""
from pathlib import Path
import hashlib
import json
import subprocess
import sys

if len(sys.argv) != 3:
    raise SystemExit("Usage: python tools/snapshot_runtime.py TARGET_REPOSITORY GIT_REVISION")
target = Path(sys.argv[1]).resolve()
root = Path(__file__).resolve().parents[1] / "runtime/naseps-fixture"
metadata_path = root / "metadata.json"
previous = json.loads(metadata_path.read_text(encoding="utf-8"))
revision = subprocess.check_output(
    ["git", "-C", str(target), "rev-parse", "--verify", sys.argv[2] + "^{commit}"], text=True).strip()
# Stage all reads before changing any fixture file. Never snapshot patched working files.
files = {name: subprocess.check_output(["git", "-C", str(target), "show", revision + ":" + name])
         for name in previous["files"]}
for name, data in files.items():
    if b"AsnAper" in data or b"encodeAper" in data or b"decodeAper" in data:
        raise SystemExit("Expected an unpatched API revision: " + name)
for name, data in files.items():
    (root / name).write_bytes(data)
metadata = {"sourceRepository": previous["sourceRepository"], "baseRevision": revision,
            "files": {name: hashlib.sha256(data).hexdigest() for name, data in files.items()},
            "description": "Unmodified target API from baseRevision. No APER prerequisite patch."}
metadata_path.write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8", newline="\n")
print(f"Saved {len(files)} unchanged source files from {revision}; target untouched")

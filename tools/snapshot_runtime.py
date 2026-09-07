"""Refresh the reviewable naseps prerequisite patch and portable compilation fixture."""
from pathlib import Path
import difflib
import hashlib
import json
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
TARGET = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else ROOT.parent / 'naseps'
runtime = ROOT / 'runtime'
runtime.mkdir(exist_ok=True)
changed = ['src/main/java/tel/core/s1ap/core/asn/AsnAper.java',
           'src/main/java/tel/core/s1ap/core/asn/AsnPrintableString.java',
           'src/main/java/tel/core/s1ap/spec/ProtocolIeId.java',
           'src/test/java/tel/core/s1ap/core/asn/AsnAperTest.java']
patch = ''
for relative in changed:
    before = subprocess.run(['git', '-C', str(TARGET), 'show', 'HEAD:' + relative], capture_output=True)
    old = before.stdout.decode('utf-8').replace('\r\n', '\n') if before.returncode == 0 else ''
    new = (TARGET / relative).read_text(encoding='utf-8')
    patch += ''.join(difflib.unified_diff(old.splitlines(keepends=True), new.splitlines(keepends=True),
                    fromfile='a/' + relative if old else '/dev/null', tofile='b/' + relative))
(runtime / 'naseps-aper-v1.patch').write_text(patch, encoding='utf-8', newline='\n')
files = sorted((TARGET / 'src/main/java/tel/core/s1ap/core/asn').glob('*.java'))
files += [TARGET / ('src/main/java/tel/core/s1ap/' + p) for p in [
    'core/model/InformationElement.java', 'core/model/InformationElementDecoderRegistry.java',
    'core/model/Criticality.java', 'core/error/S1apException.java', 'spec/ProtocolIeId.java']]
metadata = {'sourceRepository': 'https://github.com/Anc3vt/tmpproj',
            'baseRevision': subprocess.check_output(['git', '-C', str(TARGET), 'rev-parse', 'HEAD'], text=True).strip(),
            'prerequisitePatch': 'naseps-aper-v1.patch', 'files': {}}
for file in files:
    relative = file.relative_to(TARGET)
    destination = runtime / 'naseps-fixture' / relative
    destination.parent.mkdir(parents=True, exist_ok=True)
    content = file.read_text(encoding='utf-8')
    destination.write_text(content, encoding='utf-8', newline='\n')
    metadata['files'][relative.as_posix()] = hashlib.sha256(content.encode()).hexdigest()
(runtime / 'naseps-fixture/metadata.json').write_text(json.dumps(metadata, indent=2) + '\n', encoding='utf-8', newline='\n')
print(f'Saved prerequisite patch and {len(files)} actual naseps source files')

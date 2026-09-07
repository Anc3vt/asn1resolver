"""Supplementary X.691 vectors, independent pycrate compiler, deliberately unaligned context."""
from pathlib import Path
import hashlib
import importlib.util
import tempfile
import sys
sys.set_int_max_str_digits(0)
from pycrate_asn1c.asnproc import compile_text
from pycrate_asn1c.generator import PycrateGenerator

SCHEMA = '''RuntimeVectors DEFINITIONS AUTOMATIC TAGS ::= BEGIN
I ::= SEQUENCE { pre BOOLEAN, value INTEGER (0..18446744073709551615), post BOOLEAN }
EI ::= SEQUENCE { pre BOOLEAN, value INTEGER (0..15,...), post BOOLEAN }
FB ::= SEQUENCE { pre BOOLEAN, value BIT STRING (SIZE(3)), post BOOLEAN }
VB ::= SEQUENCE { pre BOOLEAN, value BIT STRING (SIZE(0..7)), post BOOLEAN }
LB ::= SEQUENCE { pre BOOLEAN, value BIT STRING, post BOOLEAN }
O ::= SEQUENCE { pre BOOLEAN, value OCTET STRING, post BOOLEAN }
FO ::= SEQUENCE { pre BOOLEAN, value OCTET STRING (SIZE(2)), post BOOLEAN }
ZO ::= SEQUENCE { pre BOOLEAN, value OCTET STRING (SIZE(0..3)), post BOOLEAN }
PS ::= SEQUENCE { pre BOOLEAN, value PrintableString (SIZE(0..1)), post BOOLEAN }
L ::= SEQUENCE { pre BOOLEAN, value SEQUENCE (SIZE(0..65535)) OF INTEGER (0..1), post BOOLEAN }
X ::= SEQUENCE { a INTEGER (0..7), ..., b INTEGER (0..255) OPTIONAL, c OCTET STRING OPTIONAL }
END'''
compile_text(SCHEMA)
if '--second' in sys.argv:
    import asn1tools
    reference = asn1tools.compile_string(SCHEMA, 'per')
    for name, value in [('VB',(b'',0)), ('ZO',b''), ('PS','A')]:
        print('asn1tools', name, reference.encode(name, {'pre':True,'value':value,'post':True}).hex())
    print('asn1tools X', reference.encode('X', {'a':5,'b':255,'c':b'\x12'}).hex())
with tempfile.TemporaryDirectory() as tmp:
    file = Path(tmp) / 'runtime_schema.py'
    PycrateGenerator(str(file))
    spec = importlib.util.spec_from_file_location('runtime_schema', file)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    def emit(name, value):
        typ = getattr(module.RuntimeVectors, name)
        typ.set_val(value)
        data = typ.to_aper()
        print(name, repr(value)[:100], len(data), data.hex() if len(data) < 70 else hashlib.sha256(data).hexdigest())
        if '--trace' in sys.argv and name == 'X':
            typ.to_aper_ws()
            print(typ._struct.show())
    for name, value in [('I',2**64-1), ('EI',16), ('EI',-1), ('FB',(5,3)), ('VB',(5,3)),
                         ('VB',(0,0)), ('FO',b'\x12\x34'), ('ZO',b''), ('PS','A'), ('L',[1,0,1])]:
        emit(name, {'pre':True,'value':value,'post':True})
    for length in [0,127,128,16383,16384,65536,65537]:
        emit('O', {'pre':True,'value':b'\x55'*length,'post':True})
    for length in [16384,65537]:
        emit('LB', {'pre':True,'value':((1 << length)-1,length),'post':True})
    emit('X', {'a':5,'b':255,'c':b'\x12'})

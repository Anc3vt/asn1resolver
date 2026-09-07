"""Regenerate independent APER vectors with pycrate 0.8.1 and THIS repository's full ASN.

Run with Python 3.11+ after: python -m pip install pycrate==0.8.1
No precompiled pycrate S1AP schema is used (it may be a different release).
"""
from pathlib import Path
import hashlib
import importlib.util
import importlib.metadata
import json
import tempfile
from pycrate_asn1c.asnproc import compile_text
from pycrate_asn1c.generator import PycrateGenerator

ROOT = Path(__file__).resolve().parents[1]
ASN = ROOT / "s1ap.asn"
assert importlib.metadata.version("pycrate") == "0.8.1"
compile_text(ASN.read_text(encoding="utf-8"))
with tempfile.TemporaryDirectory() as temporary:
    compiled = Path(temporary) / "reference_s1ap.py"
    PycrateGenerator(str(compiled))
    spec = importlib.util.spec_from_file_location("reference_s1ap", compiled)
    schema = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(schema)

vectors = []
def add(name, typ, java, value, extension=False):
    typ.set_val(value)
    encoded = typ.to_aper()
    vectors.append({"name": name, "asnType": typ._name, "javaClass": java,
                    "extension": extension, "value": repr(value), "hex": encoded.hex()})

ies = schema.S1AP_IEs
pdu = schema.S1AP_PDU_Contents
for value in [0, 255, 256, 65535, 65536, 4294967295]:
    add(f"mme-id-{value}", ies.MME_UE_S1AP_ID, "MmeUeS1apId", value)
for text in ["A", "Test eNB", "Z" * 150, "X" * 151]:
    add(f"enb-name-length-{len(text)}", ies.ENBname, "EnbName", text, len(text) > 150)
for value in ies.TimeToWait._cont:
    add(f"time-to-wait-{value}", ies.TimeToWait, "TimeToWait", value)
for value in ies.ForbiddenInterRATs._cont:
    add(f"forbidden-{value}", ies.ForbiddenInterRATs, "ForbiddenInterRATs", value,
        value in (ies.ForbiddenInterRATs._ext or []))
add("fixed-teid", ies.GTP_TEID, "GtpTeid", bytes.fromhex("12345678"))
for alternative, width in [("macroENB-ID", 20), ("homeENB-ID", 28), ("short-macroENB-ID", 18), ("long-macroENB-ID", 21)]:
    add(alternative, ies.Global_ENB_ID, "GlobalEnbId", {
        "pLMNidentity": bytes.fromhex("52f099"), "eNB-ID": (alternative, (0x12345, width))}, alternative.startswith(("short-", "long-")))
add("restriction-optional-list", ies.HandoverRestrictionList, "HandoverRestrictionList", {
    "servingPLMN": bytes.fromhex("52f099"),
    "equivalentPLMNs": [bytes.fromhex("52f010")],
    "forbiddenTAs": [{"pLMN-Identity": bytes.fromhex("52f099"), "forbiddenTACs": [b"\x00\x01", b"\xff\xff"]}],
    "forbiddenInterRATs": "geran"})
add("restriction-protocol-extension", ies.HandoverRestrictionList, "HandoverRestrictionList", {
    "servingPLMN": bytes.fromhex("52f099"), "iE-Extensions": [{"id": 261, "criticality": "ignore",
    "extensionValue": (("S1AP-IEs", "NRrestrictioninEPSasSecondaryRAT"), "nRrestrictedinEPSasSecondaryRAT")}]})
add("single-container-list", pdu.E_RABSetupListCtxtSURes, "ERabSetupList", [{
    "id": 50, "criticality": "ignore", "value": (("S1AP-PDU-Contents", "E-RABSetupItemCtxtSURes"), {
        "e-RAB-ID": 5, "transportLayerAddress": (0x0a000001, 32), "gTP-TEID": bytes.fromhex("12345678")})}])
for up, down in [(0, 0), (2**63, 2**64-1)]:
    add(f"usage-count-{up}", ies.E_RABUsageReportItem, "ERabUsageReportItem", {
        "startTimestamp": bytes.fromhex("01020304"), "endTimestamp": bytes.fromhex("05060708"),
        "usageCountUL": up, "usageCountDL": down})
# ID 225 has inline OCTET STRING: select its linked information object TYPE.
for name in dir(pdu):
    typ = getattr(pdu, name)
    if getattr(typ, "_mode", None) == "SET" and getattr(typ, "_typeref", None) is not None:
        val = typ.get_val()
        if hasattr(val, "root"):
            for obj in val.root:
                if isinstance(obj, dict) and obj.get("id") == 225 and "Value" in obj:
                    if not any(v["name"] == "inline-s1-message" for v in vectors):
                        add("inline-s1-message", obj["Value"], "S1Message", b"\x01\x02\x03")
                    break
assert any(v["name"] == "inline-s1-message" for v in vectors)

result = {"schemaVersion": 1, "specVersion": "15.3.0", "asnSha256": hashlib.sha256(ASN.read_bytes()).hexdigest(),
          "reference": "pycrate 0.8.1, pycrate_asn1c.compile_text on the complete pinned input",
          "source": "https://github.com/P1sec/pycrate", "vectors": vectors}
destination = ROOT / "src/test/resources/generator/aper-vectors.json"
destination.parent.mkdir(parents=True, exist_ok=True)
destination.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8", newline="\n")
print(f"Wrote {len(vectors)} vectors to {destination}")

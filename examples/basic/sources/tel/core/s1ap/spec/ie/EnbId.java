// Generated from S1AP ASN.1 type ENB-ID.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.util.Objects;
import tel.core.s1ap.core.asn.AsnAper;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.error.S1apException;
import tel.core.s1ap.core.model.InformationElement;

public final class EnbId implements InformationElement {
    public enum Choice { MACROENBID, HOMEENBID, SHORTMACROENBID, LONGMACROENBID }
    private final Choice choice;
    private final EnbIdMacroEnbId macroEnbId;
    private final EnbIdHomeEnbId homeEnbId;
    private final EnbIdShortMacroEnbId shortMacroEnbId;
    private final EnbIdLongMacroEnbId longMacroEnbId;
    public EnbId(BitInput in) {
        try {
            int index = AsnAper.index(in, 2, true, false);
            BitInput content = index < 0 ? AsnAper.open(in) : in;
            this.choice = switch (index) {
                case 0 -> Choice.MACROENBID;
                case 1 -> Choice.HOMEENBID;
                case -1 -> Choice.SHORTMACROENBID;
                case -2 -> Choice.LONGMACROENBID;
                default -> throw new S1apException("Unknown CHOICE index");
            };
            this.macroEnbId = choice == Choice.MACROENBID ? new EnbIdMacroEnbId(content) : null;
            this.homeEnbId = choice == Choice.HOMEENBID ? new EnbIdHomeEnbId(content) : null;
            this.shortMacroEnbId = choice == Choice.SHORTMACROENBID ? new EnbIdShortMacroEnbId(content) : null;
            this.longMacroEnbId = choice == Choice.LONGMACROENBID ? new EnbIdLongMacroEnbId(content) : null;
            if (index < 0) AsnAper.finishOpen(content);
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
        }
    }

    private EnbId(Choice choice, 
            EnbIdMacroEnbId macroEnbId,
            EnbIdHomeEnbId homeEnbId,
            EnbIdShortMacroEnbId shortMacroEnbId,
            EnbIdLongMacroEnbId longMacroEnbId) {
        this.choice = choice;
        this.macroEnbId = macroEnbId;
        this.homeEnbId = homeEnbId;
        this.shortMacroEnbId = shortMacroEnbId;
        this.longMacroEnbId = longMacroEnbId;
    }

    public static EnbId macroEnbId(EnbIdMacroEnbId value) {
        return new EnbId(Choice.MACROENBID, Objects.requireNonNull(value, "value"), null, null, null);
    }
    public static EnbId homeEnbId(EnbIdHomeEnbId value) {
        return new EnbId(Choice.HOMEENBID, null, Objects.requireNonNull(value, "value"), null, null);
    }
    public static EnbId shortMacroEnbId(EnbIdShortMacroEnbId value) {
        throw new IllegalArgumentException("CHOICE extensions are disabled");
    }
    public static EnbId longMacroEnbId(EnbIdLongMacroEnbId value) {
        throw new IllegalArgumentException("CHOICE extensions are disabled");
    }
    public Choice getChoice() { return choice; }
    public EnbIdMacroEnbId getMacroEnbId() { return macroEnbId; }
    public EnbIdHomeEnbId getHomeEnbId() { return homeEnbId; }
    public EnbIdShortMacroEnbId getShortMacroEnbId() { return shortMacroEnbId; }
    public EnbIdLongMacroEnbId getLongMacroEnbId() { return longMacroEnbId; }

    @Override
    public void encode(BitOutput out) {
        int populated = (macroEnbId == null ? 0 : 1) + (homeEnbId == null ? 0 : 1) + (shortMacroEnbId == null ? 0 : 1) + (longMacroEnbId == null ? 0 : 1);
        if (populated != 1) throw new IllegalStateException("CHOICE must contain exactly one value");
        switch (choice) {
            case MACROENBID -> {
                AsnAper.index(out, 0, false, 2, true, false);
                macroEnbId.encode(out);
            }
            case HOMEENBID -> {
                AsnAper.index(out, 1, false, 2, true, false);
                homeEnbId.encode(out);
            }
            case SHORTMACROENBID -> {
                AsnAper.index(out, 0, true, 2, true, false);
                AsnAper.open(out, shortMacroEnbId);
            }
            case LONGMACROENBID -> {
                AsnAper.index(out, 1, true, 2, true, false);
                AsnAper.open(out, longMacroEnbId);
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + choice + '}';
    }
}

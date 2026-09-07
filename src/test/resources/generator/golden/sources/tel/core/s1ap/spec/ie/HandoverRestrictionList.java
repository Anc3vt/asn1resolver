// Generated from S1AP ASN.1 type HandoverRestrictionList, ProtocolIE-ID 41.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.util.Objects;
import tel.core.s1ap.core.asn.AsnAper;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.model.InformationElement;

public final class HandoverRestrictionList implements InformationElement {
    private final PlmnIdentity servingPlmn;
    private final EPlmnS equivalentPlmnS;
    private final ForbiddenTAs forbiddenTAs;
    private final ForbiddenLAs forbiddenLAs;
    private final ForbiddenInterRATs forbiddenInterRATs;
    private final HandoverRestrictionListIeExtensions ieExtensions;
    public HandoverRestrictionList(BitInput in) {
        try {
            boolean hasExtensions = in.readBit();
            boolean hasEquivalentPlmnS = in.readBit();
            boolean hasForbiddenTAs = in.readBit();
            boolean hasForbiddenLAs = in.readBit();
            boolean hasForbiddenInterRATs = in.readBit();
            boolean hasIeExtensions = in.readBit();
            this.servingPlmn = new PlmnIdentity(in);
            this.equivalentPlmnS = hasEquivalentPlmnS ? new EPlmnS(in) : null;
            this.forbiddenTAs = hasForbiddenTAs ? new ForbiddenTAs(in) : null;
            this.forbiddenLAs = hasForbiddenLAs ? new ForbiddenLAs(in) : null;
            this.forbiddenInterRATs = hasForbiddenInterRATs ? new ForbiddenInterRATs(in) : null;
            this.ieExtensions = hasIeExtensions ? new HandoverRestrictionListIeExtensions(in) : null;
            BitInput[] additions = hasExtensions ? AsnAper.extensions(in, 0) : new BitInput[0];
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
        }
    }

    public HandoverRestrictionList(
            PlmnIdentity servingPlmn,
            EPlmnS equivalentPlmnS,
            ForbiddenTAs forbiddenTAs,
            ForbiddenLAs forbiddenLAs,
            ForbiddenInterRATs forbiddenInterRATs,
            HandoverRestrictionListIeExtensions ieExtensions) {
        Objects.requireNonNull(servingPlmn, "servingPlmn");
        this.servingPlmn = servingPlmn;
        this.equivalentPlmnS = equivalentPlmnS;
        this.forbiddenTAs = forbiddenTAs;
        this.forbiddenLAs = forbiddenLAs;
        this.forbiddenInterRATs = forbiddenInterRATs;
        this.ieExtensions = ieExtensions;
    }

    public PlmnIdentity getServingPlmn() { return servingPlmn; }
    public EPlmnS getEquivalentPlmnS() { return equivalentPlmnS; }
    public ForbiddenTAs getForbiddenTAs() { return forbiddenTAs; }
    public ForbiddenLAs getForbiddenLAs() { return forbiddenLAs; }
    public ForbiddenInterRATs getForbiddenInterRATs() { return forbiddenInterRATs; }
    public HandoverRestrictionListIeExtensions getIeExtensions() { return ieExtensions; }

    @Override
    public void encode(BitOutput out) {
        boolean hasExtensions = false;
        out.writeBit(hasExtensions);
        out.writeBit(equivalentPlmnS != null);
        out.writeBit(forbiddenTAs != null);
        out.writeBit(forbiddenLAs != null);
        out.writeBit(forbiddenInterRATs != null);
        out.writeBit(ieExtensions != null);
        servingPlmn.encode(out);
        if (equivalentPlmnS != null) equivalentPlmnS.encode(out);
        if (forbiddenTAs != null) forbiddenTAs.encode(out);
        if (forbiddenLAs != null) forbiddenLAs.encode(out);
        if (forbiddenInterRATs != null) forbiddenInterRATs.encode(out);
        if (ieExtensions != null) ieExtensions.encode(out);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + '}';
    }
}

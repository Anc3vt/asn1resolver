package com.ancevt.asn1.generate.s1ap.normalize;

import com.ancevt.asn1.generate.s1ap.diagnostic.GenerationException;
import com.ancevt.asn1.model.*;
import java.math.BigInteger;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class IntegerValues {
    private IntegerValues() { }
    public static BigInteger evaluate(ValueAssignment assignment) {
        return evaluate(assignment.getValue(), Collections.newSetFromMap(new IdentityHashMap<>()));
    }
    public static BigInteger evaluate(AsnValue value) {
        return evaluate(value, Collections.newSetFromMap(new IdentityHashMap<>()));
    }
    private static BigInteger evaluate(AsnValue value, Set<AsnValue> visiting) {
        if (!visiting.add(value)) throw error("Cyclic integer reference", value.getSourceRange());
        if (value instanceof LiteralValue literal && literal.getIntegerValue() != null) return literal.getIntegerValue();
        if (value instanceof ReferenceValue ref && ref.getReference().getTarget() instanceof ValueAssignment target)
            return evaluate(target.getValue(), visiting);
        throw error("Expected linked INTEGER value", value.getSourceRange());
    }
    public static int id(ObjectFieldSetting setting) {
        BigInteger value;
        if (setting.getValueTarget() instanceof ValueAssignment assignment) value = evaluate(assignment);
        else {
            try { value = new BigInteger(setting.getSourceText().trim()); }
            catch (NumberFormatException e) { throw error("Unresolved protocol ID", setting.getSourceRange()); }
        }
        if (value.signum() < 0 || value.compareTo(BigInteger.valueOf(65535)) > 0)
            throw error("Protocol ID outside 0..65535: " + value, setting.getSourceRange());
        return value.intValueExact();
    }
    private static GenerationException error(String message, SourceRange range) {
        return new GenerationException("UNSUPPORTED_CONSTRAINT", 6, message, range, List.of());
    }
}

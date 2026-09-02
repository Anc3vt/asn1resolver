package com.ancevt.asn1.model;

import java.util.List;

public final class BuiltinType extends AsnType {
    private final Kind kind;
    private final List<NamedNumber> namedBits;

    public BuiltinType(Kind kind, List<NamedNumber> namedBits, List<Constraint> constraints, SourceRange sourceRange) {
        super(sourceRange, constraints);
        this.kind = kind;
        this.namedBits = List.copyOf(namedBits);
    }

    public Kind getKind() { return kind; }

    public List<NamedNumber> getNamedBits() { return namedBits; }

    @Override public String toString() { return kind.displayName; }

    public enum Kind {
        BOOLEAN("BOOLEAN"), NULL("NULL"), REAL("REAL"),
        OCTET_STRING("OCTET STRING"), BIT_STRING("BIT STRING"),
        OBJECT_IDENTIFIER("OBJECT IDENTIFIER"), RELATIVE_OID("RELATIVE-OID"),
        UTF8_STRING("UTF8String"), PRINTABLE_STRING("PrintableString"),
        IA5_STRING("IA5String"), VISIBLE_STRING("VisibleString"),
        NUMERIC_STRING("NumericString"), BMP_STRING("BMPString"),
        UNIVERSAL_STRING("UniversalString"), GENERAL_STRING("GeneralString"),
        TELETEX_STRING("TeletexString"), VIDEOTEX_STRING("VideotexString"),
        GRAPHIC_STRING("GraphicString"), UTC_TIME("UTCTime"),
        GENERALIZED_TIME("GeneralizedTime"), OBJECT_DESCRIPTOR("ObjectDescriptor"),
        ANY("ANY");

        private final String displayName;
        Kind(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
}

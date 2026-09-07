package com.ancevt.asn1.generate.s1ap.catalog;

import com.ancevt.asn1.model.AsnType;
import com.ancevt.asn1.model.SourceRange;
import java.util.List;

public record IeDescriptor(int id, String idName, String asnTypeName, AsnType type,
                           String javaName, String factoryName, List<Usage> usages) {
    public IeDescriptor { usages = List.copyOf(usages); }
    public record Usage(String kind, String criticality, String presence, String owner,
                        List<String> messages, SourceRange sourceRange) {
        public Usage { messages = List.copyOf(messages); }
    }
    public boolean mapped() { return type != null; }
    public boolean valueIe() { return usages.stream().anyMatch(u -> u.kind().equals("Value")); }
}

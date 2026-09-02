package com.ancevt.asn1.model;

/** A named object that can be the target of a resolved ASN.1 reference. */
public interface NamedElement {
    String getName();

    SourceRange getSourceRange();
}

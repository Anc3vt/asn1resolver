package com.ancevt.asn.model;

import java.util.List;

public final class WithSyntaxRule {

    private final List<String> keywords;
    private final String classFieldName;
    private final boolean optional;

    public WithSyntaxRule(List<String> keywords,
                          String classFieldName,
                          boolean optional) {
        this.keywords = List.copyOf(keywords);
        this.classFieldName = classFieldName;
        this.optional = optional;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public String getClassFieldName() {
        return classFieldName;
    }

    public boolean isOptional() {
        return optional;
    }
}
package com.ancevt.asn.model.type;

import com.ancevt.asn.model.ClassField;
import com.ancevt.asn.model.WithSyntaxRule;

import java.util.*;

public final class ClassType {

    private final String name;
    private final Map<String, ClassField> fields;

    private List<WithSyntaxRule> syntaxRules = List.of();

    public ClassType(String name, List<ClassField> fields) {
        this.name = Objects.requireNonNull(name);
        this.fields = new LinkedHashMap<>();
        for (ClassField f : fields) {
            this.fields.put(f.getName(), f);
        }
    }

    public void setSyntaxRules(List<WithSyntaxRule> rules) {
        this.syntaxRules = List.copyOf(rules);
    }

    public List<WithSyntaxRule> getSyntaxRules() {
        return syntaxRules;
    }

    public String getName() {
        return name;
    }

    public Collection<ClassField> getFields() {
        return fields.values();
    }

    public Optional<ClassField> getField(String name) {
        return Optional.ofNullable(fields.get(name));
    }
}
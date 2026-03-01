package com.ancevt.asn.model.type;

import java.util.List;

public final class ChoiceType  extends AbstractType {

    private final List<Field> options;

    public ChoiceType(List<Field> options) {
        this.options = List.copyOf(options);
    }

    public List<Field> getOptions() {
        return options;
    }
}
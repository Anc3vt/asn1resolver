package com.ancevt.asn.ast;

import java.util.List;
import java.util.Objects;

public final class ImportDefinition {

    private final String fromModule;
    private final List<String> symbols;

    public ImportDefinition(String fromModule, List<String> symbols) {
        this.fromModule = Objects.requireNonNull(fromModule);
        this.symbols = List.copyOf(symbols);
    }

    public String getFromModule() {
        return fromModule;
    }

    public List<String> getSymbols() {
        return symbols;
    }

    @Override
    public String toString() {
        return "IMPORTS " + symbols + " FROM " + fromModule;
    }
}
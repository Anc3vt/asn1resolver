package com.ancevt.asn.ast;

import java.util.*;

public final class AsnModel {

    private final Map<String, ModuleDefinition> modules = new LinkedHashMap<>();

    public void addModule(ModuleDefinition module) {
        modules.put(module.getName(), module);
    }

    public Optional<ModuleDefinition> getModule(String name) {
        return Optional.ofNullable(modules.get(name));
    }

    public Collection<ModuleDefinition> getModules() {
        return modules.values();
    }
}
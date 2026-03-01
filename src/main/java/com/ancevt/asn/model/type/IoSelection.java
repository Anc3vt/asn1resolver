package com.ancevt.asn.model.type;

import java.util.Objects;

public final class IoSelection {

    private final String objectSetParamName;
    private final String selectorFieldName; // without '@', e.g. "id", "procedureCode"; nullable

    private IoSelection(String objectSetParamName, String selectorFieldName) {
        this.objectSetParamName = Objects.requireNonNull(objectSetParamName);
        this.selectorFieldName = selectorFieldName;
    }

    public static IoSelection bySet(String objectSetParamName) {
        return new IoSelection(objectSetParamName, null);
    }

    public static IoSelection bySetKey(String objectSetParamName, String selectorFieldName) {
        return new IoSelection(objectSetParamName, Objects.requireNonNull(selectorFieldName));
    }

    public String getObjectSetParamName() {
        return objectSetParamName;
    }

    public String getSelectorFieldName() {
        return selectorFieldName;
    }

    public boolean isKeyed() {
        return selectorFieldName != null;
    }

    @Override
    public String toString() {
        if (!isKeyed()) {
            return "({" + objectSetParamName + "})";
        }
        return "({" + objectSetParamName + "}{@" + selectorFieldName + "})";
    }
}
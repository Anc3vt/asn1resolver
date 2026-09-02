package com.ancevt.asn1;

import com.ancevt.asn1.model.Asn1Document;
import com.ancevt.asn1.model.Component;
import com.ancevt.asn1.model.ConstructedType;
import com.ancevt.asn1.model.EnumeratedType;
import com.ancevt.asn1.model.ReferenceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Asn1ParserTest {
    @Test
    void parsesAndLinksACompactModule() {
        Asn1Document document = Asn1.parse("""
                Demo DEFINITIONS AUTOMATIC TAGS ::= BEGIN

                Limit ::= INTEGER (0..100)
                Color ::= ENUMERATED { red(0), blue(1), ... }
                Item ::= SEQUENCE {
                    id Limit,
                    color Color OPTIONAL,
                    ...
                }

                END
                """, "compact-test.asn");

        var module = document.requireModule("Demo");
        assertEquals(3, module.getAssignments().size());
        assertEquals(0, document.getErrorCount(), () -> document.getDiagnostics().toString());

        var item = (ConstructedType) module.requireType("Item").getType();
        Component id = item.requireComponent("id");
        ReferenceType idType = (ReferenceType) id.getType();
        assertSame(module.requireType("Limit"), idType.getTarget());

        var color = (EnumeratedType) module.requireType("Color").getType();
        assertTrue(color.isExtensible());
        assertEquals(2, color.getItems().size());
        assertTrue(item.isExtensible());
    }
}

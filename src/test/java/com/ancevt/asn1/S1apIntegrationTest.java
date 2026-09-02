package com.ancevt.asn1;

import com.ancevt.asn1.model.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.math.BigInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class S1apIntegrationTest {
    @Test
    void parsesTheCompleteS1apSchemaIntoALinkedGraph() throws Exception {
        Asn1Document document = Asn1.read(Path.of("s1ap.asn"));

        assertEquals(6, document.getModules().size());
        assertEquals(1322, document.getAssignmentCount());
        assertEquals(594, count(document, TypeAssignment.class));
        assertEquals(388, count(document, ValueAssignment.class));
        assertEquals(5, count(document, ObjectClassAssignment.class));
        assertEquals(63, count(document, ObjectAssignment.class));
        assertEquals(272, count(document, ObjectSetAssignment.class));
        assertEquals(686, document.getModules().stream()
                .flatMap(module -> module.getImports().stream())
                .mapToInt(moduleImport -> moduleImport.getSymbols().size()).sum());
        assertEquals(0, document.getErrorCount(), () -> document.getDiagnostics().toString());
        assertTrue(document.getModules().stream()
                .flatMap(module -> module.getImports().stream())
                .allMatch(moduleImport -> moduleImport.isResolved()
                        && moduleImport.getSymbols().stream().allMatch(ImportedSymbol::isResolved)));

        var allObjectDefinitions = document.getModules().stream()
                .flatMap(module -> Stream.concat(
                        module.getObjectAssignments().stream().map(ObjectAssignment::getDefinition),
                        module.getObjectSetAssignments().stream()
                                .flatMap(set -> set.getExpression().getElements().stream())
                                .map(ObjectSetElement::getInlineObject)
                                .filter(java.util.Objects::nonNull)))
                .toList();
        assertEquals(694, allObjectDefinitions.size());
        assertTrue(allObjectDefinitions.stream()
                .allMatch(object -> object.getObjectClass() != null && !object.getSettings().isEmpty()));

        Asn1Module descriptions = document.requireModule("S1AP-PDU-Descriptions");
        var initiating = (ConstructedType) descriptions.requireType("InitiatingMessage").getType();
        var openValue = assertInstanceOf(InformationObjectFieldType.class,
                initiating.requireComponent("value").getType());
        assertEquals("InitiatingMessage", openValue.getFieldName());
        assertNotNull(openValue.getObjectClass());
        assertFalse(openValue.getAlternatives().isEmpty());
        assertTrue(openValue.getAlternatives().stream()
                .anyMatch(alternative -> alternative.typeTarget().getName().equals("HandoverRequired")));

        Asn1Module contents = document.requireModule("S1AP-PDU-Contents");
        var handoverRequired = (ConstructedType) contents.requireType("HandoverRequired").getType();
        var container = assertInstanceOf(ReferenceType.class,
                handoverRequired.requireComponent("protocolIEs").getType());
        assertEquals("ProtocolIE-Container", container.getTarget().getName());
        assertEquals(1, container.getActualParameters().size());
        assertSame(contents.requireAssignment("HandoverRequiredIEs"),
                container.getActualParameters().get(0).getReferences().get(0).getTarget());

        Asn1Module ies = document.requireModule("S1AP-IEs");
        var globalEnbId = (ConstructedType) ies.requireType("Global-ENB-ID").getType();
        var plmnRef = assertInstanceOf(ReferenceType.class,
                globalEnbId.requireComponent("pLMNidentity").getType());
        assertNotNull(plmnRef.getTarget());

        var usageReport = (ConstructedType) ies.requireType("E-RABUsageReportItem").getType();
        var usageCount = assertInstanceOf(IntegerType.class,
                usageReport.requireComponent("usageCountUL").getType());
        Constraint usageRange = usageCount.getConstraints().get(0);
        assertEquals(Constraint.Kind.RANGE, usageRange.getKind());
        assertEquals(new BigInteger("18446744073709551615"),
                usageRange.getChildren().get(1).getLiteralInteger());

        var earfcn = assertInstanceOf(IntegerType.class, ies.requireType("EARFCN").getType());
        assertTrue(earfcn.getConstraints().get(0).isExtensible());
    }

    private static long count(Asn1Document document, Class<? extends Assignment> type) {
        return document.getModules().stream()
                .flatMap(module -> module.getAssignments().stream())
                .filter(type::isInstance)
                .count();
    }
}

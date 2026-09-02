package com.ancevt.asn1.demo;

import com.ancevt.asn1.Asn1;
import com.ancevt.asn1.model.Asn1Document;
import com.ancevt.asn1.model.Asn1Module;
import com.ancevt.asn1.model.Component;
import com.ancevt.asn1.model.ConstructedType;
import com.ancevt.asn1.model.InformationObjectFieldType;
import com.ancevt.asn1.model.ObjectSetAssignment;
import com.ancevt.asn1.model.OpenTypeAlternative;
import com.ancevt.asn1.model.ReferenceType;
import com.ancevt.asn1.model.TypeAssignment;

import java.nio.file.Path;

/** Run this class from IntelliJ IDEA and stop on the marked line. */
public final class DebugMain {
    private DebugMain() { }

    public static void main(String[] args) throws Exception {
        Path input = args.length == 0 ? Path.of("s1ap.asn") : Path.of(args[0]);
        Asn1Document document = Asn1.read(input);

        // A regular reference: component -> reference -> target type assignment.
        Asn1Module ies = document.requireModule("S1AP-IEs");
        TypeAssignment globalEnbId = ies.requireType("Global-ENB-ID");
        ConstructedType sequence = (ConstructedType) globalEnbId.getType();
        Component plmnIdentity = sequence.requireComponent("pLMNidentity");
        TypeAssignment linkedPlmnType = ((ReferenceType) plmnIdentity.getType()).getTarget();

        // An open type: class field + table constraint -> concrete alternatives.
        Asn1Module descriptions = document.requireModule("S1AP-PDU-Descriptions");
        ConstructedType initiatingMessage = (ConstructedType) descriptions
                .requireType("InitiatingMessage").getType();
        InformationObjectFieldType openValue = (InformationObjectFieldType) initiatingMessage
                .requireComponent("value").getType();
        OpenTypeAlternative handover = openValue.getAlternatives().stream()
                .filter(alternative -> alternative.typeTarget().getName().equals("HandoverRequired"))
                .findFirst().orElseThrow();

        // A parameterized reference: actual argument -> linked object set.
        ConstructedType handoverRequired = (ConstructedType) handover.typeTarget().getType();
        ReferenceType protocolIeContainer = (ReferenceType) handoverRequired
                .requireComponent("protocolIEs").getType();
        ObjectSetAssignment handoverIes = (ObjectSetAssignment) protocolIeContainer
                .getActualParameters().get(0).getReferences().get(0).getTarget();

        // Put a breakpoint here and expand any local variable or reference.target.

        System.out.printf("Loaded %d modules, %d assignments, %d linked references, %d errors%n",
                document.getModules().size(), document.getAssignmentCount(),
                document.getReferenceIndex().size(), document.getErrorCount());
        System.out.println("Example link: " + globalEnbId.getName() + "." + plmnIdentity.getName()
                + " -> " + (linkedPlmnType == null ? "unresolved" : linkedPlmnType));
        System.out.printf("Open type: %s -> %s; actual object set: %s (%d direct elements)%n",
                openValue, handover.typeTarget(), handoverIes.getName(),
                handoverIes.getExpression().getElements().size());
    }
}

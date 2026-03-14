package com.ancevt.asn.dev;


import com.ancevt.asn.ast.AsnModel;
import com.ancevt.asn.ast.ModuleDefinition;
import com.ancevt.asn.ast.type.AsnType;
import com.ancevt.asn.ast.type.Field;
import com.ancevt.asn.ast.type.SequenceType;
import com.ancevt.asn.ast.type.TypeRef;
import com.ancevt.asn.parse.Parser;
import com.ancevt.asn.parse.Token;
import com.ancevt.asn.parse.Tokenizer;
import com.ancevt.asn.resolve.AsnLinker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class MainDev {

    public static void main(String[] args) throws IOException {
        String asn = Files.readString(Path.of("s1ap.asn"));

        Tokenizer tokenizer = new Tokenizer(asn);
        List<Token> tokens = tokenizer.tokenize();

        Parser parser = new Parser(tokens, asn);

        AsnModel model = parser.parseModel();

        new AsnLinker(model).link();

        for (ModuleDefinition module : model.getModules()) {
            System.out.println("Module: " + module.getName());
            System.out.println("Types: " + module.getTypes().size());
            System.out.println("Classes: " + module.getClasses().size());
            System.out.println("ObjectSets: " + module.getObjectSets().size());
            System.out.println("Objects: " + module.getObjects().size());
            System.out.println("Values: " + module.getValue("maxPrivateIEs"));
            System.out.println();
        }


        ModuleDefinition moduleDefinition = model.getModule("S1AP-IEs").get();
        AsnType asnType = moduleDefinition.getTypes().get("Global-ENB-ID");

        SequenceType sequenceType = (SequenceType) asnType;

        Field field = sequenceType.getFields().get(0);

        AsnType resolvedType = ((TypeRef) field.getType()).getResolvedType();

        System.out.println();
    }
}

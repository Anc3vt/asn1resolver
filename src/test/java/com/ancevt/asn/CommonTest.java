package com.ancevt.asn;

import com.ancevt.asn.ast.ModuleDefinition;
import com.ancevt.asn.parse.Parser;
import com.ancevt.asn.parse.Tokenizer;
import org.junit.jupiter.api.Test;

public class CommonTest {

    @Test
    void test() {

        String asn = """
                DemoModule DEFINITIONS ::= BEGIN
                
                ProcedureCode ::= INTEGER (0..255)
                
                Criticality ::= ENUMERATED {
                    reject (0),
                    ignore (1),
                    notify (2)
                }
                
                TestPDU ::= CHOICE {
                    initiatingMessage   InitiatingMessage,
                    successfulOutcome   SuccessfulOutcome,
                    unsuccessfulOutcome UnsuccessfulOutcome,
                    ...
                }
                
                InitiatingMessage ::= SEQUENCE {
                    procedureCode   ProcedureCode,
                    criticality     Criticality,
                    value           TestMessage,
                    ...
                }
                
                SuccessfulOutcome ::= SEQUENCE {
                    procedureCode   ProcedureCode,
                    criticality     Criticality,
                    value           TestMessage OPTIONAL
                }
                
                UnsuccessfulOutcome ::= SEQUENCE {
                    procedureCode   ProcedureCode,
                    criticality     Criticality,
                    errorCode       INTEGER (1..16),
                    details         SEQUENCE OF ErrorDetail OPTIONAL
                }
                
                TestMessage ::= SEQUENCE {
                    id          INTEGER,
                    name        UTF8String OPTIONAL,
                    payload     OCTET STRING,
                    flags       SEQUENCE OF BOOLEAN
                }
                
                ErrorDetail ::= SEQUENCE {
                    code        INTEGER,
                    description UTF8String
                }
                
                END
                """;

        Tokenizer tokenizer = new Tokenizer(asn);
        Parser parser = new Parser(tokenizer.tokenize(), asn);
        ModuleDefinition module = parser.parseModule();

        System.out.println("Module: " + module.getName());
    }

    @Test
    void testImports() {
        String asn = """
                DemoModule DEFINITIONS ::= BEGIN
                
                IMPORTS
                    A,
                    B
                FROM ModuleX;
                
                Test ::= INTEGER
                
                END
                """;

        Tokenizer tokenizer = new Tokenizer(asn);
        Parser parser = new Parser(tokenizer.tokenize(), asn);
        ModuleDefinition module = parser.parseModule();

        System.out.println(module.getImports());
    }

}

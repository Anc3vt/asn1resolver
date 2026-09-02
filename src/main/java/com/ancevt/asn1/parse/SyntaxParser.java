package com.ancevt.asn1.parse;

import com.ancevt.asn1.model.ActualParameter;
import com.ancevt.asn1.model.Asn1Document;
import com.ancevt.asn1.model.Asn1Module;
import com.ancevt.asn1.model.AsnType;
import com.ancevt.asn1.model.AsnValue;
import com.ancevt.asn1.model.BuiltinType;
import com.ancevt.asn1.model.CollectionType;
import com.ancevt.asn1.model.CollectionValue;
import com.ancevt.asn1.model.Component;
import com.ancevt.asn1.model.Constraint;
import com.ancevt.asn1.model.ConstructedType;
import com.ancevt.asn1.model.EnumeratedType;
import com.ancevt.asn1.model.EnumerationItem;
import com.ancevt.asn1.model.FormalParameter;
import com.ancevt.asn1.model.ImportedSymbol;
import com.ancevt.asn1.model.InformationObjectFieldType;
import com.ancevt.asn1.model.IntegerType;
import com.ancevt.asn1.model.LiteralValue;
import com.ancevt.asn1.model.ModuleImport;
import com.ancevt.asn1.model.NamedNumber;
import com.ancevt.asn1.model.ObjectAssignment;
import com.ancevt.asn1.model.ObjectClassAssignment;
import com.ancevt.asn1.model.ObjectClassDefinition;
import com.ancevt.asn1.model.ObjectClassField;
import com.ancevt.asn1.model.ObjectDefinition;
import com.ancevt.asn1.model.ObjectSetAssignment;
import com.ancevt.asn1.model.ObjectSetElement;
import com.ancevt.asn1.model.ObjectSetExpression;
import com.ancevt.asn1.model.OidComponent;
import com.ancevt.asn1.model.RawValue;
import com.ancevt.asn1.model.ReferenceType;
import com.ancevt.asn1.model.ReferenceValue;
import com.ancevt.asn1.model.SourceRange;
import com.ancevt.asn1.model.SymbolKind;
import com.ancevt.asn1.model.SymbolReference;
import com.ancevt.asn1.model.TaggingMode;
import com.ancevt.asn1.model.TypeAssignment;
import com.ancevt.asn1.model.ValueAssignment;
import com.ancevt.asn1.model.WithSyntax;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Recursive-descent parser for the schema-oriented ASN.1 subset used by the
 * bundled S1AP specification. The lexer deliberately has no keyword token
 * kind, so all keyword decisions in this class are contextual and
 * case-sensitive.
 *
 * <p>The parser is strict: it never skips an unexpected token as recovery.
 * Defined-syntax information objects are captured losslessly as balanced
 * token bodies and interpreted later by the semantic linker, after their
 * (possibly forward-declared/imported) object classes are available.</p>
 */
final class SyntaxParser {
    private static final Set<String> BUILTIN_TYPE_WORDS = Set.of(
            "BOOLEAN", "NULL", "REAL", "INTEGER", "SEQUENCE", "SET", "CHOICE",
            "ENUMERATED", "OCTET", "BIT", "OBJECT", "PrintableString", "UTF8String",
            "IA5String", "VisibleString", "NumericString", "BMPString",
            "UniversalString", "GeneralString", "TeletexString", "VideotexString",
            "GraphicString", "UTCTime", "GeneralizedTime", "ObjectDescriptor", "ANY"
    );

    private static final Set<String> CONSTRAINT_WORDS = Set.of(
            "SIZE", "MIN", "MAX", "FROM", "WITH", "COMPONENTS", "PRESENT",
            "ABSENT", "OPTIONAL", "CONSTRAINED", "BY", "CONTAINING",
            "ENCODED", "PATTERN", "INCLUDES"
    );

    private final String source;
    private final String sourceName;
    private final List<Token> tokens;
    private final Set<String> objectClassNames;

    private int position;
    private Set<String> currentFormalParameters = Set.of();

    SyntaxParser(String source, String sourceName, List<Token> tokens) {
        this.source = Objects.requireNonNull(source, "source");
        this.sourceName = sourceName == null ? "<memory>" : sourceName;
        this.tokens = List.copyOf(Objects.requireNonNull(tokens, "tokens"));
        if (this.tokens.isEmpty() || this.tokens.get(this.tokens.size() - 1).kind() != TokenKind.EOF) {
            throw new IllegalArgumentException("tokens must end with EOF");
        }
        this.objectClassNames = preScanObjectClassNames(this.tokens);
    }

    Asn1Document parseDocument() {
        position = 0;
        Asn1Document document = new Asn1Document(sourceName, source);
        while (!at(TokenKind.EOF)) {
            document.addModule(parseModule());
        }
        expect(TokenKind.EOF, "end of input");
        return document;
    }

    private Asn1Module parseModule() {
        Token moduleStart = expectIdentifier("module name");
        List<OidComponent> definitiveIdentifier = at(TokenKind.LBRACE)
                ? parseOidComponents()
                : List.of();

        expectWord("DEFINITIONS");

        TaggingMode taggingMode = TaggingMode.UNSPECIFIED;
        if (atWord("EXPLICIT") || atWord("IMPLICIT") || atWord("AUTOMATIC")) {
            Token mode = consume();
            taggingMode = switch (mode.lexeme()) {
                case "EXPLICIT" -> TaggingMode.EXPLICIT;
                case "IMPLICIT" -> TaggingMode.IMPLICIT;
                case "AUTOMATIC" -> TaggingMode.AUTOMATIC;
                default -> throw new AssertionError(mode.lexeme());
            };
            expectWord("TAGS");
        }

        boolean extensibilityImplied = false;
        if (matchWord("EXTENSIBILITY")) {
            expectWord("IMPLIED");
            extensibilityImplied = true;
        }

        expect(TokenKind.ASSIGN, "'::=' in module header");
        expectWord("BEGIN");

        // EXPORTS is absent in S1AP, but accepting and consuming it here keeps
        // the module parser well-delimited for closely related 3GPP modules.
        if (matchWord("EXPORTS")) {
            consumeUntilSemicolon("EXPORTS clause");
        }

        List<ModuleImport> imports = atWord("IMPORTS") ? parseImports() : List.of();
        List<com.ancevt.asn1.model.Assignment> assignments = new ArrayList<>();
        while (!atWord("END")) {
            if (at(TokenKind.EOF)) {
                throw error(current(), "Expected END for module " + moduleStart.lexeme());
            }
            assignments.add(parseAssignment());
        }
        Token moduleEnd = expectWord("END");

        Asn1Module module = new Asn1Module(
                moduleStart.lexeme(),
                definitiveIdentifier,
                taggingMode,
                extensibilityImplied,
                range(moduleStart, moduleEnd)
        );
        imports.forEach(module::addImport);
        assignments.forEach(module::addAssignment);
        return module;
    }

    private List<OidComponent> parseOidComponents() {
        expect(TokenKind.LBRACE, "'{' starting an object identifier");
        List<OidComponent> components = new ArrayList<>();
        while (!at(TokenKind.RBRACE)) {
            Token start = current();
            String name = null;
            String numberOrReference;

            if (at(TokenKind.NUMBER)) {
                Token number = consume();
                numberOrReference = number.lexeme();
                components.add(new OidComponent(null, numberOrReference, range(number, number)));
            } else {
                Token identifier = expectIdentifier("OID component");
                if (match(TokenKind.LPAREN)) {
                    Token value = current();
                    if (!at(TokenKind.NUMBER) && !at(TokenKind.IDENTIFIER)) {
                        throw error(current(), "Expected number or reference in OID component");
                    }
                    consume();
                    Token close = expect(TokenKind.RPAREN, "')' after OID component");
                    name = identifier.lexeme();
                    numberOrReference = value.lexeme();
                    components.add(new OidComponent(name, numberOrReference, range(start, close)));
                } else {
                    numberOrReference = identifier.lexeme();
                    components.add(new OidComponent(null, numberOrReference, range(start, identifier)));
                }
            }
            match(TokenKind.COMMA);
        }
        expect(TokenKind.RBRACE, "'}' ending an object identifier");
        return List.copyOf(components);
    }

    private List<ModuleImport> parseImports() {
        expectWord("IMPORTS");
        List<ModuleImport> groups = new ArrayList<>();

        while (!at(TokenKind.SEMICOLON)) {
            Token groupStart = current();
            List<ImportedSymbol> symbols = new ArrayList<>();
            while (!atWord("FROM")) {
                Token symbolStart = expectIdentifier("imported symbol");
                Token symbolEnd = symbolStart;
                if (match(TokenKind.LBRACE)) {
                    // Parameterized references are imported with an empty {}.
                    symbolEnd = expect(TokenKind.RBRACE, "'}' in parameterized imported symbol");
                }
                symbols.add(new ImportedSymbol(symbolStart.lexeme(), range(symbolStart, symbolEnd)));
                if (!match(TokenKind.COMMA) && !atWord("FROM")) {
                    throw error(current(), "Expected ',' or FROM in IMPORTS clause");
                }
            }

            if (symbols.isEmpty()) {
                throw error(current(), "An IMPORTS group must contain at least one symbol");
            }
            expectWord("FROM");
            Token moduleName = expectIdentifier("source module name");
            List<OidComponent> assignedIdentifier = at(TokenKind.LBRACE)
                    ? parseOidComponents()
                    : List.of();
            Token groupEnd = previous();
            groups.add(new ModuleImport(
                    moduleName.lexeme(),
                    symbols,
                    assignedIdentifier,
                    range(groupStart, groupEnd)
            ));
            match(TokenKind.COMMA);
        }
        expect(TokenKind.SEMICOLON, "';' ending IMPORTS");
        return List.copyOf(groups);
    }

    private com.ancevt.asn1.model.Assignment parseAssignment() {
        Token assignmentStart = expectIdentifier("assignment name");
        String name = assignmentStart.lexeme();

        List<FormalParameter> formalParameters = List.of();
        if (at(TokenKind.LBRACE)) {
            formalParameters = parseFormalParameters();
        }

        if (match(TokenKind.ASSIGN)) {
            if (atWord("CLASS")) {
                if (!formalParameters.isEmpty()) {
                    throw error(current(), "Parameterized object class assignments are not supported here");
                }
                ObjectClassDefinition definition = parseObjectClassDefinition();
                Token end = previous();
                SourceRange assignmentRange = range(assignmentStart, end);
                return new ObjectClassAssignment(
                        name,
                        definition,
                        assignmentRange,
                        text(assignmentRange)
                );
            }

            Set<String> savedFormals = currentFormalParameters;
            currentFormalParameters = formalParameterNames(formalParameters);
            try {
                AsnType type = parseType();
                Token end = previous();
                SourceRange assignmentRange = range(assignmentStart, end);
                return new TypeAssignment(
                        name,
                        formalParameters,
                        type,
                        assignmentRange,
                        text(assignmentRange)
                );
            } finally {
                currentFormalParameters = savedFormals;
            }
        }

        if (!formalParameters.isEmpty()) {
            throw error(current(), "Expected '::=' after formal parameters");
        }

        if (current().kind() != TokenKind.IDENTIFIER) {
            throw error(current(), "Expected governor or '::=' after assignment name");
        }

        Token governorStart = current();
        if (objectClassNames.contains(governorStart.lexeme())) {
            consume();
            SymbolReference classReference = new SymbolReference(
                    null,
                    governorStart.lexeme(),
                    SymbolKind.OBJECT_CLASS,
                    range(governorStart, governorStart)
            );
            expect(TokenKind.ASSIGN, "'::=' after object class governor");

            if (startsWithLowerCase(name)) {
                ObjectDefinition definition = parseObjectDefinition();
                Token end = previous();
                SourceRange assignmentRange = range(assignmentStart, end);
                return new ObjectAssignment(
                        name,
                        classReference,
                        definition,
                        assignmentRange,
                        text(assignmentRange)
                );
            }

            ObjectSetExpression expression = parseObjectSetExpression();
            Token end = previous();
            SourceRange assignmentRange = range(assignmentStart, end);
            return new ObjectSetAssignment(
                    name,
                    classReference,
                    expression,
                    assignmentRange,
                    text(assignmentRange)
            );
        }

        AsnType governor = parseType();
        expect(TokenKind.ASSIGN, "'::=' after value governor");
        AsnValue value = parseValue();
        Token end = previous();
        SourceRange assignmentRange = range(assignmentStart, end);
        return new ValueAssignment(name, governor, value, assignmentRange, text(assignmentRange));
    }

    private List<FormalParameter> parseFormalParameters() {
        expect(TokenKind.LBRACE, "'{' starting formal parameters");
        List<FormalParameter> parameters = new ArrayList<>();

        while (!at(TokenKind.RBRACE)) {
            int segmentStart = position;
            int colon = findAtCurrentLevel(TokenKind.COLON, TokenKind.COMMA, TokenKind.RBRACE);
            if (colon < 0) {
                throw error(current(), "Expected ':' in formal parameter");
            }
            if (colon == segmentStart) {
                throw error(current(), "Missing formal parameter governor");
            }
            if (colon + 1 >= tokens.size() || tokens.get(colon + 1).kind() != TokenKind.IDENTIFIER) {
                throw error(tokens.get(colon), "Expected formal parameter name after ':'");
            }

            Token governorStart = tokens.get(segmentStart);
            Token governorEnd = tokens.get(colon - 1);
            String governorText = source.substring(governorStart.startOffset(), governorEnd.endOffset());

            position = colon + 1;
            Token parameterName = expectIdentifier("formal parameter name");
            if (!at(TokenKind.COMMA) && !at(TokenKind.RBRACE)) {
                throw error(current(), "Unexpected token after formal parameter name");
            }

            boolean classGovernor = objectClassNames.contains(governorText.trim());
            SymbolReference governorReference = null;
            if (segmentStart + 1 == colon && !BUILTIN_TYPE_WORDS.contains(governorStart.lexeme())) {
                governorReference = new SymbolReference(
                        null,
                        governorStart.lexeme(),
                        classGovernor ? SymbolKind.OBJECT_CLASS : SymbolKind.TYPE,
                        range(governorStart, governorStart)
                );
            }
            SymbolKind parameterKind = classGovernor ? SymbolKind.OBJECT_SET : SymbolKind.VALUE;
            parameters.add(new FormalParameter(
                    parameterName.lexeme(),
                    governorText,
                    governorReference,
                    parameterKind,
                    range(governorStart, parameterName)
            ));

            if (!match(TokenKind.COMMA)) {
                break;
            }
        }
        expect(TokenKind.RBRACE, "'}' ending formal parameters");
        return List.copyOf(parameters);
    }

    private AsnType parseType() {
        Token start = current();
        if (atWord("SEQUENCE")) {
            return parseSequenceOrSetType(false);
        }
        if (atWord("SET")) {
            return parseSequenceOrSetType(true);
        }
        if (atWord("CHOICE")) {
            consume();
            return parseConstructedBody(start, ConstructedType.Kind.CHOICE);
        }
        if (atWord("ENUMERATED")) {
            return parseEnumeratedType();
        }
        if (atWord("INTEGER")) {
            return parseIntegerType();
        }
        if (atWord("BIT")) {
            return parseBitOrOctetString(true);
        }
        if (atWord("OCTET")) {
            return parseBitOrOctetString(false);
        }
        if (atWord("OBJECT") && lookaheadWord(1, "IDENTIFIER")) {
            consume();
            expectWord("IDENTIFIER");
            List<Constraint> constraints = parseTrailingConstraints();
            return new BuiltinType(
                    BuiltinType.Kind.OBJECT_IDENTIFIER,
                    List.of(),
                    constraints,
                    range(start, previous())
            );
        }

        BuiltinType.Kind builtinKind = builtinKind(current());
        if (builtinKind != null) {
            consume();
            List<Constraint> constraints = parseTrailingConstraints();
            return new BuiltinType(builtinKind, List.of(), constraints, range(start, previous()));
        }

        if (at(TokenKind.LBRACKET)) {
            return parseTaggedType();
        }

        Token referenceStart = expectIdentifier("type reference");
        String moduleQualifier = null;
        Token referenceName = referenceStart;
        if (match(TokenKind.DOT)) {
            if (match(TokenKind.AMPERSAND)) {
                Token fieldName = expectIdentifier("information object class field");
                SymbolReference classReference = new SymbolReference(
                        null,
                        referenceStart.lexeme(),
                        SymbolKind.OBJECT_CLASS,
                        range(referenceStart, referenceStart)
                );
                List<Constraint> constraints = parseTrailingConstraints();
                Constraint table = null;
                List<Constraint> ordinary = new ArrayList<>();
                for (Constraint constraint : constraints) {
                    if (table == null && constraint.getKind() == Constraint.Kind.TABLE) {
                        table = constraint;
                    } else {
                        ordinary.add(constraint);
                    }
                }
                return new InformationObjectFieldType(
                        classReference,
                        fieldName.lexeme(),
                        table,
                        ordinary,
                        range(start, previous())
                );
            }
            moduleQualifier = referenceStart.lexeme();
            referenceName = expectIdentifier("qualified type reference");
        }

        SymbolReference reference = new SymbolReference(
                moduleQualifier,
                referenceName.lexeme(),
                currentFormalParameters.contains(referenceName.lexeme())
                        ? SymbolKind.FORMAL_PARAMETER
                        : SymbolKind.TYPE,
                range(referenceStart, referenceName)
        );
        List<ActualParameter> actualParameters = at(TokenKind.LBRACE)
                ? parseActualParameters()
                : List.of();
        List<Constraint> constraints = parseTrailingConstraints();
        return new ReferenceType(reference, actualParameters, constraints, range(start, previous()));
    }

    private AsnType parseSequenceOrSetType(boolean set) {
        Token start = consume();
        ConstructedType.Kind constructedKind = set
                ? ConstructedType.Kind.SET
                : ConstructedType.Kind.SEQUENCE;
        CollectionType.Kind collectionKind = set
                ? CollectionType.Kind.SET_OF
                : CollectionType.Kind.SEQUENCE_OF;

        if (at(TokenKind.LBRACE)) {
            return parseConstructedBody(start, constructedKind);
        }

        List<Constraint> constraints = parseTrailingConstraints();
        expectWord("OF");

        String elementName = null;
        // Named collection elements are legal ASN.1. S1AP does not use them,
        // but this lookahead is unambiguous for the supported type starters.
        if (at(TokenKind.IDENTIFIER)
                && startsWithLowerCase(current().lexeme())
                && lookahead(1).kind() == TokenKind.IDENTIFIER
                && isTypeStarter(lookahead(1))) {
            elementName = consume().lexeme();
        }
        AsnType elementType = parseType();
        return new CollectionType(
                collectionKind,
                elementName,
                elementType,
                constraints,
                range(start, previous())
        );
    }

    private ConstructedType parseConstructedBody(Token start, ConstructedType.Kind kind) {
        expect(TokenKind.LBRACE, "'{' starting " + kind);
        List<Component> components = new ArrayList<>();
        boolean extensible = false;
        boolean extensionAddition = false;

        while (!at(TokenKind.RBRACE)) {
            if (match(TokenKind.COMMA)) {
                continue;
            }
            if (at(TokenKind.ELLIPSIS)) {
                consume();
                extensible = true;
                extensionAddition = true;
                match(TokenKind.COMMA);
                continue;
            }

            Token componentStart = current();
            boolean componentsOf = false;
            String name;
            AsnType componentType;
            if (matchWord("COMPONENTS")) {
                expectWord("OF");
                componentsOf = true;
                name = "COMPONENTS OF";
                componentType = parseType();
            } else {
                Token componentName = expectIdentifier("component name");
                name = componentName.lexeme();
                componentType = parseType();
            }

            boolean optional = false;
            AsnValue defaultValue = null;
            if (matchWord("OPTIONAL")) {
                optional = true;
            } else if (matchWord("DEFAULT")) {
                defaultValue = parseValue();
            }

            Token componentEnd = previous();
            components.add(new Component(
                    name,
                    componentType,
                    optional,
                    defaultValue,
                    extensionAddition,
                    componentsOf,
                    range(componentStart, componentEnd)
            ));

            if (!match(TokenKind.COMMA) && !at(TokenKind.RBRACE)) {
                throw error(current(), "Expected ',' or '}' after " + kind + " component");
            }
        }
        Token close = expect(TokenKind.RBRACE, "'}' ending " + kind);
        List<Constraint> constraints = parseTrailingConstraints();
        return new ConstructedType(kind, components, extensible, constraints, range(start, previous()));
    }

    private EnumeratedType parseEnumeratedType() {
        Token start = expectWord("ENUMERATED");
        expect(TokenKind.LBRACE, "'{' starting ENUMERATED");
        List<EnumerationItem> items = new ArrayList<>();
        boolean extensible = false;
        boolean extensionAddition = false;

        while (!at(TokenKind.RBRACE)) {
            if (match(TokenKind.COMMA)) {
                continue;
            }
            if (match(TokenKind.ELLIPSIS)) {
                extensible = true;
                extensionAddition = true;
                continue;
            }

            Token itemStart = expectIdentifier("enumeration item");
            BigInteger numericValue = null;
            SymbolReference definedValue = null;
            Token itemEnd = itemStart;
            if (match(TokenKind.LPAREN)) {
                boolean negative = match(TokenKind.MINUS);
                Token value = current();
                if (at(TokenKind.NUMBER)) {
                    consume();
                    numericValue = new BigInteger((negative ? "-" : "") + value.lexeme());
                } else if (at(TokenKind.IDENTIFIER) && !negative) {
                    consume();
                    definedValue = reference(value, SymbolKind.VALUE);
                } else {
                    throw error(current(), "Expected numeric value or reference for enumeration item");
                }
                itemEnd = expect(TokenKind.RPAREN, "')' after enumeration item value");
            }
            items.add(new EnumerationItem(
                    itemStart.lexeme(),
                    numericValue,
                    definedValue,
                    extensionAddition,
                    range(itemStart, itemEnd)
            ));
            if (!match(TokenKind.COMMA) && !at(TokenKind.RBRACE)) {
                throw error(current(), "Expected ',' or '}' in ENUMERATED");
            }
        }
        expect(TokenKind.RBRACE, "'}' ending ENUMERATED");
        List<Constraint> constraints = parseTrailingConstraints();
        return new EnumeratedType(items, extensible, constraints, range(start, previous()));
    }

    private IntegerType parseIntegerType() {
        Token start = expectWord("INTEGER");
        List<NamedNumber> namedNumbers = at(TokenKind.LBRACE)
                ? parseNamedNumbers()
                : List.of();
        List<Constraint> constraints = parseTrailingConstraints();
        return new IntegerType(namedNumbers, constraints, range(start, previous()));
    }

    private AsnType parseBitOrOctetString(boolean bit) {
        Token start = consume();
        expectWord("STRING");
        List<NamedNumber> namedBits = bit && at(TokenKind.LBRACE)
                ? parseNamedNumbers()
                : List.of();
        List<Constraint> constraints = parseTrailingConstraints();
        return new BuiltinType(
                bit ? BuiltinType.Kind.BIT_STRING : BuiltinType.Kind.OCTET_STRING,
                namedBits,
                constraints,
                range(start, previous())
        );
    }

    private List<NamedNumber> parseNamedNumbers() {
        expect(TokenKind.LBRACE, "'{' starting named numbers");
        List<NamedNumber> numbers = new ArrayList<>();
        while (!at(TokenKind.RBRACE)) {
            if (match(TokenKind.COMMA)) {
                continue;
            }
            Token name = expectIdentifier("named number");
            expect(TokenKind.LPAREN, "'(' after named number");
            boolean negative = match(TokenKind.MINUS);
            Token value = current();
            BigInteger numericValue = null;
            SymbolReference definedValue = null;
            if (at(TokenKind.NUMBER)) {
                consume();
                numericValue = new BigInteger((negative ? "-" : "") + value.lexeme());
            } else if (at(TokenKind.IDENTIFIER) && !negative) {
                consume();
                definedValue = reference(value, SymbolKind.VALUE);
            } else {
                throw error(current(), "Expected number or value reference in named number");
            }
            Token close = expect(TokenKind.RPAREN, "')' ending named number");
            numbers.add(new NamedNumber(
                    name.lexeme(),
                    numericValue,
                    definedValue,
                    range(name, close)
            ));
            if (!match(TokenKind.COMMA) && !at(TokenKind.RBRACE)) {
                throw error(current(), "Expected ',' or '}' in named-number list");
            }
        }
        expect(TokenKind.RBRACE, "'}' ending named numbers");
        return List.copyOf(numbers);
    }

    private AsnType parseTaggedType() {
        Token start = expect(TokenKind.LBRACKET, "'[' starting tag");
        String tagClass = null;
        if (atWord("UNIVERSAL") || atWord("APPLICATION") || atWord("PRIVATE")) {
            tagClass = consume().lexeme();
        }
        Token tagNumber = current();
        if (!at(TokenKind.NUMBER) && !at(TokenKind.IDENTIFIER)) {
            throw error(current(), "Expected tag number or value reference");
        }
        consume();
        expect(TokenKind.RBRACKET, "']' ending tag");
        com.ancevt.asn1.model.TaggedType.Mode mode = com.ancevt.asn1.model.TaggedType.Mode.DEFAULT;
        if (matchWord("EXPLICIT")) {
            mode = com.ancevt.asn1.model.TaggedType.Mode.EXPLICIT;
        } else if (matchWord("IMPLICIT")) {
            mode = com.ancevt.asn1.model.TaggedType.Mode.IMPLICIT;
        }
        AsnType nested = parseType();
        return new com.ancevt.asn1.model.TaggedType(
                tagClass,
                tagNumber.lexeme(),
                mode,
                nested,
                List.of(),
                range(start, previous())
        );
    }

    private List<ActualParameter> parseActualParameters() {
        expect(TokenKind.LBRACE, "'{' starting actual parameters");
        List<ActualParameter> parameters = new ArrayList<>();
        while (!at(TokenKind.RBRACE)) {
            int startIndex = position;
            int endIndex = findActualParameterEnd();
            if (endIndex == startIndex) {
                throw error(current(), "Empty actual parameter");
            }

            Token start = tokens.get(startIndex);
            Token end = tokens.get(endIndex - 1);
            List<SymbolReference> references = collectActualParameterReferences(startIndex, endIndex);
            SourceRange parameterRange = range(start, end);
            parameters.add(new ActualParameter(text(parameterRange), parameterRange, references));
            position = endIndex;

            if (!match(TokenKind.COMMA)) {
                break;
            }
        }
        expect(TokenKind.RBRACE, "'}' ending actual parameters");
        return List.copyOf(parameters);
    }

    private int findActualParameterEnd() {
        int braces = 0;
        int parentheses = 0;
        int brackets = 0;
        int index = position;
        while (index < tokens.size()) {
            TokenKind kind = tokens.get(index).kind();
            if (kind == TokenKind.LBRACE) braces++;
            else if (kind == TokenKind.RBRACE) {
                if (braces == 0 && parentheses == 0 && brackets == 0) return index;
                braces--;
            } else if (kind == TokenKind.LPAREN) parentheses++;
            else if (kind == TokenKind.RPAREN) parentheses--;
            else if (kind == TokenKind.LBRACKET) brackets++;
            else if (kind == TokenKind.RBRACKET) brackets--;
            else if (kind == TokenKind.COMMA && braces == 0 && parentheses == 0 && brackets == 0) {
                return index;
            } else if (kind == TokenKind.EOF) {
                throw error(tokens.get(index), "Unterminated actual parameter list");
            }
            index++;
        }
        throw error(current(), "Unterminated actual parameter list");
    }

    private List<SymbolReference> collectActualParameterReferences(int startIndex, int endIndex) {
        List<SymbolReference> references = new ArrayList<>();
        for (int i = startIndex; i < endIndex; i++) {
            Token token = tokens.get(i);
            if (token.kind() != TokenKind.IDENTIFIER || isActualParameterKeyword(token.lexeme())) {
                continue;
            }
            SymbolKind expectedKind;
            if (currentFormalParameters.contains(token.lexeme())) {
                expectedKind = SymbolKind.FORMAL_PARAMETER;
            } else {
                expectedKind = SymbolKind.ANY;
            }
            references.add(reference(token, expectedKind));
        }
        return List.copyOf(references);
    }

    private List<Constraint> parseTrailingConstraints() {
        List<Constraint> constraints = new ArrayList<>();
        while (at(TokenKind.LPAREN)) {
            constraints.add(parseConstraint());
        }
        return List.copyOf(constraints);
    }

    private Constraint parseConstraint() {
        int startIndex = position;
        expect(TokenKind.LPAREN, "'(' starting constraint");
        int depth = 1;
        while (depth > 0) {
            Token token = current();
            if (token.kind() == TokenKind.EOF) {
                throw error(token, "Unterminated constraint");
            }
            consume();
            if (token.kind() == TokenKind.LPAREN) depth++;
            else if (token.kind() == TokenKind.RPAREN) depth--;
        }
        int endIndex = position;
        return buildConstraint(startIndex, endIndex);
    }

    private Constraint buildConstraint(int startIndex, int endIndex) {
        Token start = tokens.get(startIndex);
        Token end = tokens.get(endIndex - 1);
        int contentStart = start.kind() == TokenKind.LPAREN ? startIndex + 1 : startIndex;
        int contentEnd = end.kind() == TokenKind.RPAREN ? endIndex - 1 : endIndex;

        Constraint.Kind kind = classifyConstraint(contentStart, contentEnd);
        List<Constraint> children = new ArrayList<>();

        if (kind == Constraint.Kind.SIZE
                && contentStart + 1 < contentEnd
                && tokens.get(contentStart + 1).kind() == TokenKind.LPAREN) {
            children.add(buildConstraint(contentStart + 1, contentEnd));
        } else if (kind == Constraint.Kind.UNION) {
            int partStart = contentStart;
            int nesting = 0;
            for (int i = contentStart; i < contentEnd; i++) {
                TokenKind tokenKind = tokens.get(i).kind();
                if (tokenKind == TokenKind.LPAREN || tokenKind == TokenKind.LBRACE || tokenKind == TokenKind.LBRACKET) nesting++;
                else if (tokenKind == TokenKind.RPAREN || tokenKind == TokenKind.RBRACE || tokenKind == TokenKind.RBRACKET) nesting--;
                else if (tokenKind == TokenKind.PIPE && nesting == 0) {
                    if (partStart < i) children.add(buildConstraint(partStart, i));
                    partStart = i + 1;
                }
            }
            if (partStart < contentEnd) children.add(buildConstraint(partStart, contentEnd));
        } else if (kind == Constraint.Kind.RANGE) {
            int nesting = 0;
            int separator = -1;
            for (int i = contentStart; i < contentEnd; i++) {
                TokenKind tokenKind = tokens.get(i).kind();
                if (tokenKind == TokenKind.LPAREN || tokenKind == TokenKind.LBRACE || tokenKind == TokenKind.LBRACKET) nesting++;
                else if (tokenKind == TokenKind.RPAREN || tokenKind == TokenKind.RBRACE || tokenKind == TokenKind.RBRACKET) nesting--;
                else if (tokenKind == TokenKind.RANGE && nesting == 0) { separator = i; break; }
            }
            if (separator > contentStart) children.add(buildConstraint(contentStart, separator));
            if (separator >= 0) {
                int upperEnd = contentEnd;
                for (int i = separator + 1; i < contentEnd; i++) {
                    if (tokens.get(i).kind() == TokenKind.COMMA) { upperEnd = i; break; }
                }
                if (separator + 1 < upperEnd) children.add(buildConstraint(separator + 1, upperEnd));
            }
        }

        List<SymbolReference> references = new ArrayList<>();
        List<String> selectorPath = new ArrayList<>();
        // SIZE and UNION delegate their nested lexical uses to child nodes. This
        // keeps ReferenceIndex at one entry per actual source occurrence.
        if (children.isEmpty()) {
            boolean afterAt = false;
            for (int i = contentStart; i < contentEnd; i++) {
                Token token = tokens.get(i);
                if (token.kind() == TokenKind.AT) {
                    afterAt = true;
                    continue;
                }
                if (afterAt && token.kind() == TokenKind.IDENTIFIER) {
                    selectorPath.add(token.lexeme());
                    afterAt = false;
                    continue;
                }
                if (token.kind() == TokenKind.DOT && !selectorPath.isEmpty()) {
                    afterAt = true;
                    continue;
                }
                if (token.kind() != TokenKind.IDENTIFIER || CONSTRAINT_WORDS.contains(token.lexeme())) {
                    continue;
                }
                // An identifier immediately following '&' is a class-field name,
                // not a module symbol reference.
                if (i > contentStart && tokens.get(i - 1).kind() == TokenKind.AMPERSAND) {
                    continue;
                }
                SymbolKind expectedKind;
                if (currentFormalParameters.contains(token.lexeme())) {
                    expectedKind = SymbolKind.FORMAL_PARAMETER;
                } else if (kind == Constraint.Kind.TABLE && insideBraces(i, contentStart)) {
                    expectedKind = SymbolKind.OBJECT_SET;
                } else {
                    expectedKind = SymbolKind.VALUE;
                }
                references.add(reference(token, expectedKind));
            }
        }

        SourceRange constraintRange = range(start, end);
        return new Constraint(
                kind,
                text(constraintRange),
                constraintRange,
                children,
                references,
                selectorPath
        );
    }

    private Constraint.Kind classifyConstraint(int startIndex, int endIndex) {
        if (startIndex < endIndex && tokens.get(startIndex).isIdentifier("SIZE")) {
            return Constraint.Kind.SIZE;
        }
        boolean braces = false;
        boolean union = false;
        boolean intersection = false;
        boolean range = false;
        for (int i = startIndex; i < endIndex; i++) {
            TokenKind kind = tokens.get(i).kind();
            braces |= kind == TokenKind.LBRACE;
            union |= kind == TokenKind.PIPE;
            intersection |= kind == TokenKind.CARET;
            range |= kind == TokenKind.RANGE;
        }
        if (braces) return Constraint.Kind.TABLE;
        if (union) return Constraint.Kind.UNION;
        if (intersection) return Constraint.Kind.INTERSECTION;
        if (range) return Constraint.Kind.RANGE;
        return Constraint.Kind.SINGLE_VALUE;
    }

    private boolean insideBraces(int tokenIndex, int startIndex) {
        int depth = 0;
        for (int i = startIndex; i < tokenIndex; i++) {
            if (tokens.get(i).kind() == TokenKind.LBRACE) depth++;
            else if (tokens.get(i).kind() == TokenKind.RBRACE) depth--;
        }
        return depth > 0;
    }

    private ObjectClassDefinition parseObjectClassDefinition() {
        Token classStart = expectWord("CLASS");
        expect(TokenKind.LBRACE, "'{' starting CLASS fields");
        List<ObjectClassField> fields = new ArrayList<>();

        while (!at(TokenKind.RBRACE)) {
            if (match(TokenKind.COMMA)) {
                continue;
            }
            Token fieldStart = expect(TokenKind.AMPERSAND, "'&' starting object class field");
            Token fieldName = expectIdentifier("object class field name");
            boolean typeField = startsWithUpperCase(fieldName.lexeme());
            AsnType governor = null;

            if (!typeField || !at(TokenKind.COMMA)
                    && !at(TokenKind.RBRACE)
                    && !atWord("OPTIONAL")) {
                // Upper-case class fields are type fields and have no governor
                // in the S1AP classes. Lower-case fields always carry a type.
                if (!typeField) {
                    governor = parseType();
                }
            }

            boolean unique = false;
            boolean optional = false;
            AsnValue defaultValue = null;
            boolean modifiers = true;
            while (modifiers) {
                if (matchWord("UNIQUE")) {
                    unique = true;
                } else if (matchWord("OPTIONAL")) {
                    optional = true;
                } else if (matchWord("DEFAULT")) {
                    defaultValue = parseValue();
                } else {
                    modifiers = false;
                }
            }

            Token fieldEnd = previous();
            fields.add(new ObjectClassField(
                    fieldName.lexeme(),
                    governor,
                    typeField,
                    unique,
                    optional,
                    defaultValue,
                    range(fieldStart, fieldEnd)
            ));
            if (!match(TokenKind.COMMA) && !at(TokenKind.RBRACE)) {
                throw error(current(), "Expected ',' or '}' after CLASS field");
            }
        }
        expect(TokenKind.RBRACE, "'}' ending CLASS fields");

        WithSyntax withSyntax = null;
        if (matchWord("WITH")) {
            expectWord("SYNTAX");
            withSyntax = parseWithSyntax();
        }
        return new ObjectClassDefinition(fields, withSyntax, range(classStart, previous()));
    }

    private WithSyntax parseWithSyntax() {
        Token start = expect(TokenKind.LBRACE, "'{' starting WITH SYNTAX");
        List<WithSyntax.Element> elements = parseWithSyntaxElements(TokenKind.RBRACE);
        Token end = expect(TokenKind.RBRACE, "'}' ending WITH SYNTAX");
        return new WithSyntax(elements, range(start, end));
    }

    private List<WithSyntax.Element> parseWithSyntaxElements(TokenKind terminator) {
        List<WithSyntax.Element> elements = new ArrayList<>();
        while (!at(terminator)) {
            if (at(TokenKind.EOF)) {
                throw error(current(), "Unterminated WITH SYNTAX template");
            }
            if (at(TokenKind.LBRACKET)) {
                Token start = consume();
                List<WithSyntax.Element> nested = parseWithSyntaxElements(TokenKind.RBRACKET);
                Token end = expect(TokenKind.RBRACKET, "']' ending optional WITH SYNTAX group");
                elements.add(new WithSyntax.OptionalGroup(nested, range(start, end)));
            } else if (at(TokenKind.AMPERSAND)) {
                Token start = consume();
                Token field = expectIdentifier("field in WITH SYNTAX");
                elements.add(new WithSyntax.Field(field.lexeme(), range(start, field)));
            } else {
                Token literal = consume();
                elements.add(new WithSyntax.Literal(literal.lexeme(), range(literal, literal)));
            }
        }
        return List.copyOf(elements);
    }

    private ObjectDefinition parseObjectDefinition() {
        Token start = expect(TokenKind.LBRACE, "'{' starting information object");
        List<String> syntaxTokens = new ArrayList<>();
        List<SourceRange> syntaxRanges = new ArrayList<>();
        int depth = 1;
        Token end = null;

        while (depth > 0) {
            Token token = current();
            if (token.kind() == TokenKind.EOF) {
                throw error(token, "Unterminated information object");
            }
            consume();
            if (token.kind() == TokenKind.LBRACE) {
                depth++;
            } else if (token.kind() == TokenKind.RBRACE) {
                depth--;
                if (depth == 0) {
                    end = token;
                    break;
                }
            }
            syntaxTokens.add(token.lexeme());
            syntaxRanges.add(range(token, token));
        }

        SourceRange objectRange = range(start, Objects.requireNonNull(end));
        return new ObjectDefinition(text(objectRange), objectRange, syntaxTokens, syntaxRanges);
    }

    private ObjectSetExpression parseObjectSetExpression() {
        Token start = expect(TokenKind.LBRACE, "'{' starting object set");
        List<ObjectSetElement> elements = new ArrayList<>();

        while (!at(TokenKind.RBRACE)) {
            if (match(TokenKind.COMMA) || match(TokenKind.PIPE)) {
                continue;
            }
            if (at(TokenKind.ELLIPSIS)) {
                Token marker = consume();
                elements.add(ObjectSetElement.extension(range(marker, marker)));
                continue;
            }
            if (at(TokenKind.LBRACE)) {
                ObjectDefinition object = parseObjectDefinition();
                elements.add(ObjectSetElement.inline(object, object.getSourceRange()));
                continue;
            }
            Token objectReference = expectIdentifier("object or object-set reference");
            SymbolKind expectedKind = startsWithLowerCase(objectReference.lexeme())
                    ? SymbolKind.OBJECT
                    : SymbolKind.OBJECT_SET;
            elements.add(ObjectSetElement.reference(
                    reference(objectReference, expectedKind),
                    range(objectReference, objectReference)
            ));
        }
        Token end = expect(TokenKind.RBRACE, "'}' ending object set");
        return new ObjectSetExpression(elements, range(start, end));
    }

    private AsnValue parseValue() {
        Token start = current();
        if (match(TokenKind.MINUS) || match(TokenKind.PLUS)) {
            Token sign = previous();
            Token number = expect(TokenKind.NUMBER, "number after sign");
            BigInteger value = new BigInteger((sign.kind() == TokenKind.MINUS ? "-" : "") + number.lexeme());
            SourceRange valueRange = range(start, number);
            return LiteralValue.integer(value, text(valueRange), valueRange);
        }
        if (at(TokenKind.NUMBER)) {
            Token number = consume();
            return LiteralValue.integer(
                    new BigInteger(number.lexeme()),
                    number.lexeme(),
                    range(number, number)
            );
        }
        if (at(TokenKind.STRING) || at(TokenKind.BIT_STRING) || at(TokenKind.HEX_STRING)) {
            Token literal = consume();
            LiteralValue.Kind kind = switch (literal.kind()) {
                case STRING -> LiteralValue.Kind.STRING;
                case BIT_STRING -> LiteralValue.Kind.BIT_STRING;
                case HEX_STRING -> LiteralValue.Kind.HEX_STRING;
                default -> throw new AssertionError(literal.kind());
            };
            return new LiteralValue(kind, literal.lexeme(), literal.lexeme(), range(literal, literal));
        }
        if (atWord("TRUE") || atWord("FALSE")) {
            Token literal = consume();
            return new LiteralValue(
                    LiteralValue.Kind.BOOLEAN,
                    Boolean.valueOf(literal.lexeme()),
                    literal.lexeme(),
                    range(literal, literal)
            );
        }
        if (atWord("NULL")) {
            Token literal = consume();
            return new LiteralValue(
                    LiteralValue.Kind.NULL,
                    null,
                    literal.lexeme(),
                    range(literal, literal)
            );
        }
        if (at(TokenKind.IDENTIFIER)) {
            Token valueReference = consume();
            SourceRange valueRange = range(valueReference, valueReference);
            return new ReferenceValue(
                    reference(valueReference, SymbolKind.VALUE),
                    text(valueRange),
                    valueRange
            );
        }
        if (at(TokenKind.LBRACE)) {
            return parseCollectionValueOrRaw();
        }
        throw error(current(), "Expected ASN.1 value");
    }

    private AsnValue parseCollectionValueOrRaw() {
        Token start = expect(TokenKind.LBRACE, "'{' starting value");
        int contentStart = position;
        int depth = 1;
        while (depth > 0) {
            Token token = current();
            if (token.kind() == TokenKind.EOF) throw error(token, "Unterminated collection value");
            consume();
            if (token.kind() == TokenKind.LBRACE) depth++;
            else if (token.kind() == TokenKind.RBRACE) depth--;
        }
        Token end = previous();
        SourceRange valueRange = range(start, end);

        // The S1AP schema only uses braced values as information-object or
        // object-set syntax, which are parsed by their dedicated methods. Keep
        // this fallback lossless for defaults in related input files.
        if (contentStart == position - 1) {
            return new CollectionValue(List.of(), text(valueRange), valueRange);
        }
        return new RawValue(text(valueRange), valueRange);
    }

    private BuiltinType.Kind builtinKind(Token token) {
        if (token.kind() != TokenKind.IDENTIFIER) return null;
        return switch (token.lexeme()) {
            case "BOOLEAN" -> BuiltinType.Kind.BOOLEAN;
            case "NULL" -> BuiltinType.Kind.NULL;
            case "REAL" -> BuiltinType.Kind.REAL;
            case "PrintableString" -> BuiltinType.Kind.PRINTABLE_STRING;
            case "UTF8String" -> BuiltinType.Kind.UTF8_STRING;
            case "IA5String" -> BuiltinType.Kind.IA5_STRING;
            case "VisibleString" -> BuiltinType.Kind.VISIBLE_STRING;
            case "NumericString" -> BuiltinType.Kind.NUMERIC_STRING;
            case "BMPString" -> BuiltinType.Kind.BMP_STRING;
            case "UniversalString" -> BuiltinType.Kind.UNIVERSAL_STRING;
            case "GeneralString" -> BuiltinType.Kind.GENERAL_STRING;
            case "TeletexString" -> BuiltinType.Kind.TELETEX_STRING;
            case "VideotexString" -> BuiltinType.Kind.VIDEOTEX_STRING;
            case "GraphicString" -> BuiltinType.Kind.GRAPHIC_STRING;
            case "UTCTime" -> BuiltinType.Kind.UTC_TIME;
            case "GeneralizedTime" -> BuiltinType.Kind.GENERALIZED_TIME;
            case "ObjectDescriptor" -> BuiltinType.Kind.OBJECT_DESCRIPTOR;
            case "ANY" -> BuiltinType.Kind.ANY;
            default -> null;
        };
    }

    private boolean isTypeStarter(Token token) {
        return token.kind() == TokenKind.LBRACKET
                || token.kind() == TokenKind.IDENTIFIER;
    }

    private boolean isActualParameterKeyword(String value) {
        return BUILTIN_TYPE_WORDS.contains(value)
                || CONSTRAINT_WORDS.contains(value)
                || value.equals("OF")
                || value.equals("TRUE")
                || value.equals("FALSE")
                || value.equals("NULL");
    }

    private Set<String> formalParameterNames(List<FormalParameter> parameters) {
        if (parameters.isEmpty()) return Set.of();
        Set<String> names = new LinkedHashSet<>();
        for (FormalParameter parameter : parameters) names.add(parameter.getName());
        return Set.copyOf(names);
    }

    private static Set<String> preScanObjectClassNames(List<Token> tokens) {
        Set<String> result = new HashSet<>();
        for (int i = 0; i + 2 < tokens.size(); i++) {
            Token name = tokens.get(i);
            if (name.kind() == TokenKind.IDENTIFIER
                    && tokens.get(i + 1).kind() == TokenKind.ASSIGN
                    && tokens.get(i + 2).isIdentifier("CLASS")) {
                result.add(name.lexeme());
            }
        }
        return Set.copyOf(result);
    }

    private int findAtCurrentLevel(TokenKind sought, TokenKind... terminators) {
        int braces = 0;
        int parentheses = 0;
        int brackets = 0;
        for (int i = position; i < tokens.size(); i++) {
            TokenKind kind = tokens.get(i).kind();
            if (braces == 0 && parentheses == 0 && brackets == 0) {
                if (kind == sought) return i;
                for (TokenKind terminator : terminators) {
                    if (kind == terminator) return -1;
                }
            }
            if (kind == TokenKind.LBRACE) braces++;
            else if (kind == TokenKind.RBRACE) braces--;
            else if (kind == TokenKind.LPAREN) parentheses++;
            else if (kind == TokenKind.RPAREN) parentheses--;
            else if (kind == TokenKind.LBRACKET) brackets++;
            else if (kind == TokenKind.RBRACKET) brackets--;
        }
        return -1;
    }

    private void consumeUntilSemicolon(String construct) {
        while (!at(TokenKind.SEMICOLON)) {
            if (at(TokenKind.EOF)) throw error(current(), "Unterminated " + construct);
            consume();
        }
        consume();
    }

    private SymbolReference reference(Token token, SymbolKind expectedKind) {
        return new SymbolReference(null, token.lexeme(), expectedKind, range(token, token));
    }

    private boolean at(TokenKind kind) {
        return current().kind() == kind;
    }

    private boolean atWord(String word) {
        return current().isIdentifier(word);
    }

    private boolean lookaheadWord(int distance, String word) {
        return lookahead(distance).isIdentifier(word);
    }

    private boolean match(TokenKind kind) {
        if (!at(kind)) return false;
        consume();
        return true;
    }

    private boolean matchWord(String word) {
        if (!atWord(word)) return false;
        consume();
        return true;
    }

    private Token expect(TokenKind kind, String expectation) {
        Token token = current();
        if (token.kind() != kind) {
            throw error(token, "Expected " + expectation + ", found " + describe(token));
        }
        return consume();
    }

    private Token expectWord(String word) {
        Token token = current();
        if (!token.isIdentifier(word)) {
            throw error(token, "Expected '" + word + "', found " + describe(token));
        }
        return consume();
    }

    private Token expectIdentifier(String expectation) {
        return expect(TokenKind.IDENTIFIER, expectation);
    }

    private Token current() {
        return tokens.get(Math.min(position, tokens.size() - 1));
    }

    private Token previous() {
        if (position <= 0) return tokens.get(0);
        return tokens.get(position - 1);
    }

    private Token lookahead(int distance) {
        return tokens.get(Math.min(position + distance, tokens.size() - 1));
    }

    private Token consume() {
        Token token = current();
        if (position < tokens.size()) position++;
        return token;
    }

    private SourceRange range(Token start, Token end) {
        return new SourceRange(
                sourceName,
                start.startOffset(),
                end.endOffset(),
                start.startLine(),
                start.startColumn(),
                end.endLine(),
                end.endColumn()
        );
    }

    private String text(SourceRange range) {
        return source.substring(range.startOffset(), Math.min(range.endOffset(), source.length()));
    }

    private Asn1ParseException error(Token token, String message) {
        return new Asn1ParseException(
                message,
                sourceName,
                token.startOffset(),
                token.startLine(),
                token.startColumn()
        );
    }

    private static boolean startsWithLowerCase(String value) {
        return !value.isEmpty() && Character.isLowerCase(value.charAt(0));
    }

    private static boolean startsWithUpperCase(String value) {
        return !value.isEmpty() && Character.isUpperCase(value.charAt(0));
    }

    private static String describe(Token token) {
        return token.kind() == TokenKind.EOF
                ? "end of input"
                : token.kind() + " '" + token.lexeme() + "'";
    }
}

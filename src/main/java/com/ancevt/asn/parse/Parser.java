package com.ancevt.asn.parse;

import com.ancevt.asn.model.*;
import com.ancevt.asn.model.type.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class Parser {

    private final List<Token> tokens;
    private final String originalInput;

    private final java.util.Map<String, Supplier<AsnType>> keywordTypeParsers =
            new java.util.HashMap<>();

    {

        keywordTypeParsers.put("PrintableString",
                () -> withConstraint(new BuiltinType("PrintableString")));

        keywordTypeParsers.put("IA5String",
                () -> withConstraint(new BuiltinType("IA5String")));

        keywordTypeParsers.put("VisibleString",
                () -> withConstraint(new BuiltinType("VisibleString")));

        keywordTypeParsers.put("NumericString",
                () -> withConstraint(new BuiltinType("NumericString")));

        keywordTypeParsers.put("BMPString",
                () -> withConstraint(new BuiltinType("BMPString")));

        keywordTypeParsers.put("GeneralizedTime",
                () -> withConstraint(new BuiltinType("GeneralizedTime")));

        keywordTypeParsers.put("UTCTime",
                () -> withConstraint(new BuiltinType("UTCTime")));

        keywordTypeParsers.put("REAL",
                () -> withConstraint(new BuiltinType("REAL")));

        keywordTypeParsers.put("NULL",
                () -> withConstraint(new BuiltinType("NULL")));

        keywordTypeParsers.put("BOOLEAN", () ->
                withConstraint(new BuiltinType("BOOLEAN")));
        keywordTypeParsers.put("UTF8String", () ->
                withConstraint(new BuiltinType("UTF8String")));

        keywordTypeParsers.put("OCTET", this::parseOctetString);
        keywordTypeParsers.put("BIT", this::parseBitString);
        keywordTypeParsers.put("OBJECT", this::parseObjectIdentifier);

        keywordTypeParsers.put("INTEGER", this::parseIntegerType);
        keywordTypeParsers.put("ENUMERATED", this::parseEnumeratedType);
        keywordTypeParsers.put("SEQUENCE", this::parseSequenceOrSequenceOf);
        keywordTypeParsers.put("CHOICE", this::parseChoiceType);
    }

    private int pos = 0;


    public Parser(List<Token> tokens, String originalInput) {
        this.tokens = tokens;
        this.originalInput = originalInput;
    }

    public AsnModel parseModel() {

        AsnModel model = new AsnModel();

        while (!peek(TokenType.EOF)) {

            int startPos = pos;

            if (match(TokenType.SEMICOLON)) {
                continue;
            }

            ModuleDefinition module = parseModule();
            model.addModule(module);

            if (pos == startPos) {
                throw new IllegalStateException(
                        "Parser made no progress at token: " + peekCurrent()
                );
            }
        }

        return model;
    }

    public ModuleDefinition parseModule() {
        String moduleName = expect(TokenType.IDENTIFIER).text();

        if (match(TokenType.LBRACE)) {
            skipUntilMatchingBrace();
        }

        expectKeyword("DEFINITIONS");

        if (matchKeyword("AUTOMATIC")
                || matchKeyword("EXPLICIT")
                || matchKeyword("IMPLICIT")) {

            expectKeyword("TAGS");
        }

        expect(TokenType.ASSIGN);
        expectKeyword("BEGIN");

        ModuleDefinition module = new ModuleDefinition(moduleName);

        while (!peekKeyword("END") && !peek(TokenType.EOF)) {

            if (match(TokenType.SEMICOLON)) {
                continue;
            }

            if (matchKeyword("IMPORTS")) {
                parseImports(module);
                continue;
            }

            parseAssignment(module);
        }

        expectKeyword("END");
        match(TokenType.SEMICOLON);
        return module;
    }

    private void parseImports(ModuleDefinition module) {

        while (!peek(TokenType.SEMICOLON)) {

            List<String> symbols = new ArrayList<>();

            while (!peekKeyword("FROM")) {

                if (match(TokenType.COMMA)) {
                    continue;
                }

                String symbol = expect(TokenType.IDENTIFIER).text();

                if (match(TokenType.LBRACE)) {
                    skipUntilMatchingBrace();
                }

                symbols.add(symbol);
            }

            expectKeyword("FROM");

            String fromModule = expect(TokenType.IDENTIFIER).text();

            module.addImport(new ImportDefinition(fromModule, symbols));
        }

        expect(TokenType.SEMICOLON);
    }

    private void parseAssignment(ModuleDefinition module) {

        String firstName = expect(TokenType.IDENTIFIER).text();

        List<String> formalParameters = null;

        if (match(TokenType.LBRACE)) {
            formalParameters = parseFormalParameters();
        }

        if (formalParameters != null) {
            module.addTypeFormalParameters(firstName, formalParameters);
        }

        // Type or Class definition
        //   X ::= ...
        if (peek(TokenType.ASSIGN)) {

            expect(TokenType.ASSIGN);

            if (matchKeyword("CLASS")) {
                ClassType clazz = parseClass(firstName);
                module.addClass(firstName, clazz);
                return;
            }

            AsnType type = parseType();
            module.addType(firstName, type);
            return;
        }

        // Object or ObjectSet
        //   objName ClassName ::= { ... }
        Token typeToken = consume();

        if (typeToken.type() != TokenType.IDENTIFIER &&
                typeToken.type() != TokenType.KEYWORD) {

            throw error("Expected type name", typeToken);
        }

        String typeOrClassName = typeToken.text();

        expect(TokenType.ASSIGN);

        if (peek(TokenType.LBRACE)) {
            parseObjectOrSet(module, firstName, typeOrClassName);
            return;
        }

        Object value = parseValueLiteral();
        module.addValue(firstName, value);
    }

    private Object parseValueLiteral() {

        if (peek(TokenType.NUMBER)) {
            return Integer.parseInt(expect(TokenType.NUMBER).text());
        }

        if (peek(TokenType.IDENTIFIER)) {
            return expect(TokenType.IDENTIFIER).text();
        }

        throw error("Unsupported value literal", peekCurrent());
    }

    private List<String> parseFormalParameters() {

        List<String> params = new ArrayList<>();

        while (!peek(TokenType.RBRACE)) {

            Token typeToken = consume();

            if (typeToken.type() != TokenType.IDENTIFIER &&
                    typeToken.type() != TokenType.KEYWORD) {

                throw error("Expected type name in formal parameter", typeToken);
            }

            expect(TokenType.COLON);

            String paramName = expect(TokenType.IDENTIFIER).text();

            params.add(paramName);

            match(TokenType.COMMA);
        }

        expect(TokenType.RBRACE);

        return params;
    }

    private void parseObjectOrSet(ModuleDefinition module,
                                  String name,
                                  String className) {

        expect(TokenType.LBRACE);

        if (peek(TokenType.LBRACE)) {
            parseInlineObjectSet(module, name, className);
            return;
        }

        if (peek(TokenType.IDENTIFIER)) {
            parseObjectSetReferenceUnion(module, name, className);
            return;
        }

        parseSingleObjectWithSyntax(module, name, className);
    }

    private void parseInlineObjectSet(ModuleDefinition module,
                                      String name,
                                      String className) {

        List<ObjectInstance> inlineObjects = new ArrayList<>();

        while (!peek(TokenType.RBRACE)) {

            inlineObjects.add(parseSingleObjectWithSyntaxBody(className));

            match(TokenType.PIPE);
            match(TokenType.COMMA);

            if (match(TokenType.ELLIPSIS)) {
                match(TokenType.COMMA);
            }
        }

        expect(TokenType.RBRACE);

        module.addObjectSet(
                name,
                new ObjectSet(
                        name,
                        className,
                        inlineObjects,
                        List.of()
                )
        );
    }

    private void parseObjectSetReferenceUnion(ModuleDefinition module,
                                              String name,
                                              String className) {

        List<String> references = new ArrayList<>();

        while (!peek(TokenType.RBRACE)) {

            String refName = expect(TokenType.IDENTIFIER).text();

            if (match(TokenType.LBRACE)) {
                parseActualParameters(); // просто пропускаем
            }

            references.add(refName);

            match(TokenType.PIPE);
            match(TokenType.COMMA);

            if (match(TokenType.ELLIPSIS)) {
                match(TokenType.COMMA);
            }
        }

        expect(TokenType.RBRACE);

        module.addObjectSet(
                name,
                new ObjectSet(
                        name,
                        className,
                        List.of(),
                        references
                )
        );
    }

    private List<String> parseActualParameters() {
        List<String> params = new ArrayList<>();

        while (!peek(TokenType.RBRACE)) {

            if (match(TokenType.LBRACE)) {
                String nested = expect(TokenType.IDENTIFIER).text();
                expect(TokenType.RBRACE);
                params.add(nested);
            } else if (peek(TokenType.NUMBER)
                    || peek(TokenType.IDENTIFIER)
                    || peek(TokenType.KEYWORD)) {
                params.add(consume().text());
            } else {
                Token t = peekCurrent();
                throw error("Unexpected token in parameter list", t);
            }

            match(TokenType.COMMA);
        }

        expect(TokenType.RBRACE);

        return params;
    }

    private void parseSingleObjectWithSyntax(ModuleDefinition module,
                                             String name,
                                             String className) {

        ObjectInstance object =
                new ObjectInstance(name, className);

        while (!peek(TokenType.RBRACE)) {

            List<String> keywords = new ArrayList<>();

            while (peek(TokenType.IDENTIFIER) || peek(TokenType.KEYWORD)) {
                keywords.add(consume().text());
            }

            if (keywords.isEmpty()) {
                if (match(TokenType.ELLIPSIS)) {
                    match(TokenType.COMMA);
                    continue;
                }

                throw error("Unexpected token in object assignment", peekCurrent());
            }

            String value = keywords.remove(keywords.size() - 1);

            String syntheticFieldName =
                    "&" + String.join(" ", keywords);

            object.putValue(syntheticFieldName, value);

            match(TokenType.COMMA);
        }

        expect(TokenType.RBRACE);

        module.addObject(name, object);
    }

    private Token peekCurrent() {
        if (pos >= tokens.size()) {
            return tokens.get(tokens.size() - 1); // последний токен (обычно EOF)
        }
        return tokens.get(pos);
    }

    private ObjectInstance parseSingleObjectWithSyntaxBody(String className) {
        expect(TokenType.LBRACE);

        ObjectInstance object = new ObjectInstance("anonymous", className);

        while (!peek(TokenType.RBRACE)) {

            int startPos = pos;

            List<String> keywords = new ArrayList<>();

            while (peek(TokenType.IDENTIFIER) || peek(TokenType.KEYWORD)) {
                keywords.add(consume().text());
            }

            if (keywords.isEmpty()) {
                if (match(TokenType.ELLIPSIS)) {
                    match(TokenType.COMMA);
                    continue;
                }

                throw error("Unexpected token in object assignment", peekCurrent());
            }

            String value = keywords.remove(keywords.size() - 1);
            String syntheticFieldName = "&" + String.join(" ", keywords);

            object.putValue(syntheticFieldName, value);

            match(TokenType.COMMA);

            if (pos == startPos) {
                throw error("Parser stuck inside object body", peekCurrent());
            }
        }

        expect(TokenType.RBRACE);

        return object;
    }

    private ClassType parseClass(String className) {

        expect(TokenType.LBRACE);

        List<ClassField> fields = new ArrayList<>();

        while (!peek(TokenType.RBRACE)) {

            expect(TokenType.AMP);
            String fieldName = "&" + expect(TokenType.IDENTIFIER).text();

            AsnType fieldType = null;

            if (!peekKeyword("UNIQUE")
                    && !peekKeyword("DEFAULT")
                    && !peekKeyword("OPTIONAL")
                    && !peek(TokenType.COMMA)
                    && !peek(TokenType.RBRACE)) {

                fieldType = parseType();
            }

            boolean optional = matchKeyword("OPTIONAL");
            boolean unique = matchKeyword("UNIQUE");

            if (matchKeyword("DEFAULT")) {
                consume();
            }

            fields.add(new ClassField(fieldName, fieldType, unique /* + optional если добавишь */));

            match(TokenType.COMMA);
        }

        expect(TokenType.RBRACE);

        ClassType clazz = new ClassType(className, fields);

        if (matchKeyword("WITH")) {
            expectKeyword("SYNTAX");
            expect(TokenType.LBRACE);
            List<WithSyntaxRule> rules = parseWithSyntaxRules();
            clazz.setSyntaxRules(rules);
        }

        return clazz;
    }

    private AsnParseException error(String message, Token token) {

        StringBuilder sb = new StringBuilder();

        sb.append(message)
                .append(" at line ")
                .append(token.line())
                .append(", column ")
                .append(token.column())
                .append("\n");

        String[] lines = originalInput.split("\n");
        if (token.line() - 1 < lines.length) {

            String lineText = lines[token.line() - 1];

            sb.append(lineText).append("\n");

            sb.append(" ".repeat(Math.max(0, token.column() - 1))).append("^\n");
        }

        return new AsnParseException(sb.toString());
    }

    private List<WithSyntaxRule> parseWithSyntaxRules() {

        List<WithSyntaxRule> rules = new ArrayList<>();

        while (!peek(TokenType.RBRACE)) {

            boolean optional = false;

            if (match(TokenType.LBRACKET)) {
                optional = true;
            }

            List<String> keywords = new ArrayList<>();

            while (!peek(TokenType.AMP)) {
                keywords.add(expect(TokenType.IDENTIFIER).text());
            }

            expect(TokenType.AMP);
            String fieldName = "&" + expect(TokenType.IDENTIFIER).text();

            if (optional) {
                expect(TokenType.RBRACKET);
            }

            rules.add(new WithSyntaxRule(keywords, fieldName, optional));
        }

        expect(TokenType.RBRACE);

        return rules;
    }

    private void skipUntilMatchingBrace() {
        int depth = 1;
        while (depth > 0) {
            Token t = consume();
            if (t.type() == TokenType.LBRACE) depth++;
            if (t.type() == TokenType.RBRACE) depth--;
        }
    }

    private AsnType parseType() {
        if (peek(TokenType.KEYWORD)) {
            String kw = peekCurrent().text();
            var parser = keywordTypeParsers.get(kw);
            if (parser != null) {
                consume();                 // съели keyword
                return parser.get();       // парсим дальше
            }
        }

        return parseRefOrIoFieldRef();
    }

    private AsnType parseRefOrIoFieldRef() {
        String refName = expectIdentifierLike().text();

        List<String> actualParameters = null;
        if (match(TokenType.LBRACE)) {
            actualParameters = parseActualParameters();
        }

        if (match(TokenType.DOT)) {
            expect(TokenType.AMP);
            String fieldName = "&" + expect(TokenType.IDENTIFIER).text();
            IoSelection selection = parseIoSelection();
            return new IoFieldRefType(refName, fieldName, selection);
        }

        TypeRef ref = new TypeRef(refName, actualParameters == null ? List.of() : actualParameters);
        Constraint c = parseConstraintIfPresent();
        if (c != null) ref.setConstraint(c);
        return ref;
    }

    private AsnType parseOctetString() {
        expectKeyword("STRING");
        return withConstraint(new BuiltinType("OCTET STRING"));
    }

    private AsnType parseBitString() {
        expectKeyword("STRING");
        return withConstraint(new BuiltinType("BIT STRING"));
    }

    private AsnType parseObjectIdentifier() {
        expectKeyword("IDENTIFIER");
        return withConstraint(new BuiltinType("OBJECT IDENTIFIER"));
    }

    private AsnType parseIntegerType() {
        List<NamedNumber> namedNumbers = List.of();
        if (match(TokenType.LBRACE)) {
            var list = new java.util.ArrayList<NamedNumber>();
            while (!peek(TokenType.RBRACE)) {
                String name = expect(TokenType.IDENTIFIER).text();
                expect(TokenType.LPAREN);
                String value = expectAnyValue();
                expect(TokenType.RPAREN);
                list.add(new NamedNumber(name, value));
                match(TokenType.COMMA);
            }
            expect(TokenType.RBRACE);
            namedNumbers = list;
        }
        IntegerType t = new IntegerType(namedNumbers);
        Constraint c = parseConstraintIfPresent();
        if (c != null) t.setConstraint(c);
        return t;
    }

    private AsnType parseEnumeratedType() {
        expect(TokenType.LBRACE);
        var values = new java.util.ArrayList<String>();
        while (!peek(TokenType.RBRACE)) {
            if (match(TokenType.ELLIPSIS)) {
                match(TokenType.COMMA);
                continue;
            }
            String name = expect(TokenType.IDENTIFIER).text();
            if (match(TokenType.LPAREN)) skipUntilClosingParen();
            values.add(name);
            match(TokenType.COMMA);
        }
        expect(TokenType.RBRACE);
        EnumeratedType t = new EnumeratedType(values);
        Constraint c = parseConstraintIfPresent();
        if (c != null) t.setConstraint(c);
        return t;
    }

    private AsnType parseChoiceType() {
        expect(TokenType.LBRACE);
        var options = new java.util.ArrayList<Field>();
        while (!peek(TokenType.RBRACE)) {
            if (match(TokenType.ELLIPSIS)) {
                match(TokenType.COMMA);
                continue;
            }
            String name = expect(TokenType.IDENTIFIER).text();
            AsnType type = parseType();
            options.add(new Field(name, type, false));
            match(TokenType.COMMA);
        }
        expect(TokenType.RBRACE);
        ChoiceType t = new ChoiceType(options);
        Constraint c = parseConstraintIfPresent();
        if (c != null) t.setConstraint(c);
        return t;
    }

    private AsnType parseSequenceOrSequenceOf() {
        // у тебя уже есть особенность: constraint перед SEQUENCE OF / SEQUENCE {...}
        Constraint seqConstraint = null;
        if (peek(TokenType.LPAREN)) {
            seqConstraint = parseConstraintIfPresent();
        }

        if (matchKeyword("OF")) {
            AsnType elementType = parseType();
            SequenceOfType t = new SequenceOfType(elementType);
            if (seqConstraint != null) t.setConstraint(seqConstraint);
            return t;
        }

        expect(TokenType.LBRACE);
        var fields = new java.util.ArrayList<Field>();
        while (!peek(TokenType.RBRACE)) {
            if (match(TokenType.ELLIPSIS)) {
                match(TokenType.COMMA);
                continue;
            }
            String fieldName = expect(TokenType.IDENTIFIER).text();
            AsnType fieldType = parseType();
            boolean optional = matchKeyword("OPTIONAL");
            fields.add(new Field(fieldName, fieldType, optional));
            if (!match(TokenType.COMMA)) break;
        }
        expect(TokenType.RBRACE);

        SequenceType t = new SequenceType(fields);
        if (seqConstraint != null) t.setConstraint(seqConstraint);
        return t;
    }

    private AsnType withConstraint(AbstractType t) {
        Constraint c = parseConstraintIfPresent();
        if (c != null) t.setConstraint(c);
        return t;
    }

    private Token expectIdentifierLike() {
        Token t = consume();
        if (t.type() == TokenType.IDENTIFIER ||
                t.type() == TokenType.KEYWORD) {
            return t;
        }
        throw error("Expected identifier or keyword", t);
    }

    private IoSelection parseIoSelection() {

        expect(TokenType.LPAREN);
        expect(TokenType.LBRACE);

        String setName = expect(TokenType.IDENTIFIER).text();

        expect(TokenType.RBRACE);

        if (match(TokenType.LBRACE)) {

            expect(TokenType.AT);
            String selector = expect(TokenType.IDENTIFIER).text();
            expect(TokenType.RBRACE);

            expect(TokenType.RPAREN);

            return IoSelection.bySetKey(setName, selector);
        }

        expect(TokenType.RPAREN);

        return IoSelection.bySet(setName);
    }

    private Constraint parseConstraintIfPresent() {

        if (!match(TokenType.LPAREN)) {
            return null;
        }

        if (matchKeyword("SIZE")) {

            expect(TokenType.LPAREN);

            ValueSetConstraint inner = parseValueSetConstraint();

            expect(TokenType.RPAREN);
            expect(TokenType.RPAREN);

            return new SizeConstraint(inner);
        }

        ValueSetConstraint result = parseValueSetConstraint();

        expect(TokenType.RPAREN);

        return result;
    }

    private ValueSetConstraint parseValueSetConstraint() {

        List<ConstraintElement> elements = new ArrayList<>();
        boolean extensible = false;

        while (true) {

            if (match(TokenType.ELLIPSIS)) {
                extensible = true;
                break;
            }

            String first = expectAnyValue();

            if (match(TokenType.RANGE)) {
                String second = expectAnyValue();
                elements.add(new RangeElement(first, second));
            } else {
                elements.add(new SingleValueElement(first));
            }

            if (match(TokenType.PIPE)) {
                continue;
            }

            if (match(TokenType.COMMA)) {
                if (match(TokenType.ELLIPSIS)) {
                    extensible = true;
                }
                break;
            }

            break;
        }

        return new ValueSetConstraint(elements, extensible);
    }

    private String expectAnyValue() {
        Token t = consume();
        if (t.type() == TokenType.NUMBER || t.type() == TokenType.IDENTIFIER) {
            return t.text();
        }
        throw error("Expected number or identifier in constraint but got " + t.type(), t);
    }

    private void skipUntilClosingParen() {
        int depth = 1;
        while (depth > 0) {
            Token t = consume();
            if (t.type() == TokenType.LPAREN) depth++;
            if (t.type() == TokenType.RPAREN) depth--;
        }
    }

    private Token expect(TokenType type) {
        Token t = consume();
        if (t.type() != type) {
            throw error("Expected " + type + " but got " + t.type(), t);
        }
        return t;
    }

    private boolean match(TokenType type) {
        if (peek(type)) {
            pos++;
            return true;
        }
        return false;
    }

    private boolean peek(TokenType type) {
        if (pos >= tokens.size()) {
            return false;
        }
        return tokens.get(pos).type() == type;
    }

    private void expectKeyword(String text) {
        Token t = consume();
        if (t.type() != TokenType.KEYWORD || !t.text().equals(text)) {
            throw error("Expected keyword '" + text + "' but got '" + t.text() + "'", t);
        }
    }

    private boolean matchKeyword(String text) {
        if (peekKeyword(text)) {
            pos++;
            return true;
        }
        return false;
    }

    private boolean peekKeyword(String text) {
        return tokens.get(pos).type() == TokenType.KEYWORD &&
                tokens.get(pos).text().equals(text);
    }

    private Token consume() {
        if (pos >= tokens.size()) {
            Token last = tokens.get(tokens.size() - 1);
            throw error("Unexpected EOF", last);
        }
        return tokens.get(pos++);
    }
}
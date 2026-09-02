package com.ancevt.asn1.link;

import com.ancevt.asn1.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Resolves the symbolic syntax tree into a directly navigable object graph. */
public final class Asn1Linker {
    private final Asn1Document document;
    private final Set<SymbolReference> reported = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<InformationObjectFieldType> informationObjectFields = new ArrayList<>();

    public Asn1Linker(Asn1Document document) {
        this.document = document;
    }

    public Asn1Document link() {
        checkDuplicateSymbols();
        linkImports();

        for (Asn1Module module : document.getModules()) {
            for (Assignment assignment : module.getAssignments()) {
                linkDeclaration(module, assignment, formalScope(assignment));
            }
        }

        for (Asn1Module module : document.getModules()) {
            for (Assignment assignment : module.getAssignments()) {
                if (assignment instanceof ObjectAssignment object) linkObjectAssignment(module, object);
                if (assignment instanceof ObjectSetAssignment objectSet) linkObjectSetAssignment(module, objectSet);
            }
        }

        for (InformationObjectFieldType fieldType : informationObjectFields) {
            buildOpenTypeAlternatives(fieldType);
        }
        return document;
    }

    private void checkDuplicateSymbols() {
        for (Asn1Module module : document.getModules()) {
            Map<String, Assignment> seen = new LinkedHashMap<>();
            for (Assignment assignment : module.getAssignments()) {
                Assignment previous = seen.putIfAbsent(assignment.getName(), assignment);
                if (previous != null) {
                    error("Duplicate assignment '" + assignment.getName() + "' in module " + module.getName(),
                            assignment.getSourceRange());
                }
            }
        }
    }

    private void linkImports() {
        for (Asn1Module module : document.getModules()) {
            for (ModuleImport moduleImport : module.getImports()) {
                Asn1Module source = document.findModule(moduleImport.getModuleName()).orElse(null);
                if (source == null) {
                    error("Imported module not found: " + moduleImport.getModuleName(), moduleImport.getSourceRange());
                    continue;
                }
                moduleImport.linkTo(source);
                for (ImportedSymbol symbol : moduleImport.getSymbols()) {
                    Assignment target = source.findAssignment(symbol.getName()).orElse(null);
                    if (target == null) {
                        error("Symbol '" + symbol.getName() + "' is not declared by " + source.getName(),
                                symbol.getSourceRange());
                    } else {
                        symbol.linkTo(target);
                    }
                }
            }
        }
    }

    private Map<String, FormalParameter> formalScope(Assignment assignment) {
        if (!(assignment instanceof TypeAssignment typeAssignment)) return Map.of();
        Map<String, FormalParameter> result = new LinkedHashMap<>();
        for (FormalParameter parameter : typeAssignment.getFormalParameters()) result.put(parameter.getName(), parameter);
        return result;
    }

    private void linkDeclaration(Asn1Module module, Assignment assignment, Map<String, FormalParameter> scope) {
        if (assignment instanceof TypeAssignment typeAssignment) {
            for (FormalParameter parameter : typeAssignment.getFormalParameters()) {
                if (parameter.getGovernorReference() != null) {
                    resolve(module, parameter.getGovernorReference(), Map.of(), parameter.getGovernorReference().getExpectedKind(), true);
                }
            }
            linkType(module, typeAssignment.getType(), scope, null);
        } else if (assignment instanceof ValueAssignment valueAssignment) {
            linkType(module, valueAssignment.getGovernor(), scope, null);
            linkValue(module, valueAssignment.getValue(), scope, valueAssignment.getGovernor());
        } else if (assignment instanceof ObjectClassAssignment classAssignment) {
            for (ObjectClassField field : classAssignment.getDefinition().getFields()) {
                if (field.getGovernor() != null) linkType(module, field.getGovernor(), scope, null);
                if (field.getDefaultValue() != null) linkValue(module, field.getDefaultValue(), scope, field.getGovernor());
            }
        }
    }

    private void linkType(Asn1Module module, AsnType type, Map<String, FormalParameter> scope,
                          ConstructedType containingType) {
        for (Constraint constraint : type.getConstraints()) linkConstraint(module, constraint, scope, containingType);

        if (type instanceof ReferenceType referenceType) {
            NamedElement target = resolve(module, referenceType.getReference(), scope, SymbolKind.TYPE, true);
            if (target instanceof TypeAssignment targetType) {
                bindActualParameters(module, referenceType, targetType, scope);
            }
            return;
        }
        if (type instanceof InformationObjectFieldType fieldType) {
            informationObjectFields.add(fieldType);
            NamedElement target = resolve(module, fieldType.getObjectClassReference(), scope, SymbolKind.OBJECT_CLASS, true);
            if (target instanceof ObjectClassAssignment objectClass) {
                ObjectClassField field = objectClass.getDefinition().findField(fieldType.getFieldName()).orElse(null);
                if (field == null) {
                    error("Field '&" + fieldType.getFieldName() + "' not found in object class " + objectClass.getName(),
                            fieldType.getSourceRange());
                } else {
                    fieldType.linkField(field);
                }
            }
            if (fieldType.getTableConstraint() != null) {
                linkConstraint(module, fieldType.getTableConstraint(), scope, containingType);
            }
            return;
        }
        if (type instanceof ConstructedType constructed) {
            for (Component component : constructed.getComponents()) {
                linkType(module, component.getType(), scope, constructed);
                if (component.getDefaultValue() != null) {
                    linkValue(module, component.getDefaultValue(), scope, component.getType());
                }
            }
            return;
        }
        if (type instanceof CollectionType collection) {
            linkType(module, collection.getElementType(), scope, containingType);
            return;
        }
        if (type instanceof TaggedType tagged) {
            linkType(module, tagged.getType(), scope, containingType);
            return;
        }
        if (type instanceof IntegerType integer) {
            for (NamedNumber number : integer.getNamedNumbers()) {
                if (number.getDefinedValue() != null) resolve(module, number.getDefinedValue(), scope, SymbolKind.VALUE, true);
            }
            return;
        }
        if (type instanceof BuiltinType builtin) {
            for (NamedNumber bit : builtin.getNamedBits()) {
                if (bit.getDefinedValue() != null) resolve(module, bit.getDefinedValue(), scope, SymbolKind.VALUE, true);
            }
            return;
        }
        if (type instanceof EnumeratedType enumerated) {
            for (EnumerationItem item : enumerated.getItems()) {
                if (item.getDefinedValue() != null) resolve(module, item.getDefinedValue(), scope, SymbolKind.VALUE, true);
            }
        }
    }

    private void bindActualParameters(Asn1Module module, ReferenceType referenceType,
                                      TypeAssignment targetType, Map<String, FormalParameter> scope) {
        List<ActualParameter> actual = referenceType.getActualParameters();
        List<FormalParameter> formal = targetType.getFormalParameters();
        if (actual.size() != formal.size()) {
            error("Parameterized type " + targetType.getName() + " expects " + formal.size()
                    + " argument(s), got " + actual.size(), referenceType.getSourceRange());
        }
        for (int i = 0; i < Math.min(actual.size(), formal.size()); i++) {
            ActualParameter argument = actual.get(i);
            FormalParameter parameter = formal.get(i);
            argument.bindTo(parameter);
            for (SymbolReference reference : argument.getReferences()) {
                resolve(module, reference, scope, parameter.getParameterKind(), true);
            }
        }
    }

    private void linkConstraint(Asn1Module module, Constraint constraint,
                                Map<String, FormalParameter> scope, ConstructedType containingType) {
        for (SymbolReference reference : constraint.getReferences()) {
            resolve(module, reference, scope, reference.getExpectedKind(), true);
        }
        if (containingType != null) {
            for (String selector : constraint.getSelectorPath()) {
                Component component = containingType.findComponent(selector).orElse(null);
                if (component != null) constraint.addSelectorTarget(component);
                else error("Component relation refers to unknown sibling component '" + selector + "'",
                        constraint.getSourceRange());
            }
        }
        for (Constraint child : constraint.getChildren()) linkConstraint(module, child, scope, containingType);
    }

    private void linkValue(Asn1Module module, AsnValue value,
                           Map<String, FormalParameter> scope, AsnType governor) {
        if (value instanceof ReferenceValue referenceValue) {
            EnumerationItem enumItem = findEnumerationItem(governor, referenceValue.getReference().getName());
            if (enumItem != null) link(referenceValue.getReference(), enumItem);
            else resolve(module, referenceValue.getReference(), scope, SymbolKind.VALUE, true);
        } else if (value instanceof CollectionValue collection) {
            for (CollectionValue.Entry entry : collection.getEntries()) {
                linkValue(module, entry.value(), scope, null);
            }
        }
    }

    private EnumerationItem findEnumerationItem(AsnType governor, String name) {
        if (governor == null) return null;
        AsnType concrete = governor.dereference();
        return concrete instanceof EnumeratedType enumerated ? enumerated.findItem(name).orElse(null) : null;
    }

    private void linkObjectAssignment(Asn1Module module, ObjectAssignment assignment) {
        NamedElement target = resolve(module, assignment.getObjectClassReference(), Map.of(), SymbolKind.OBJECT_CLASS, true);
        if (target instanceof ObjectClassAssignment objectClass) {
            interpretDefinedSyntax(module, assignment.getDefinition(), objectClass.getDefinition());
        }
    }

    private void linkObjectSetAssignment(Asn1Module module, ObjectSetAssignment assignment) {
        NamedElement target = resolve(module, assignment.getObjectClassReference(), Map.of(), SymbolKind.OBJECT_CLASS, true);
        ObjectClassDefinition objectClass = target instanceof ObjectClassAssignment clazz ? clazz.getDefinition() : null;
        for (ObjectSetElement element : assignment.getExpression().getElements()) {
            if (element.getReference() != null) {
                resolve(module, element.getReference(), Map.of(), SymbolKind.ANY, true);
            }
            if (element.getInlineObject() != null && objectClass != null) {
                interpretDefinedSyntax(module, element.getInlineObject(), objectClass);
            }
        }
    }

    private void interpretDefinedSyntax(Asn1Module module, ObjectDefinition object,
                                        ObjectClassDefinition objectClass) {
        if (object.getObjectClass() == objectClass && !object.getSettings().isEmpty()) return;
        object.linkClass(objectClass);
        WithSyntax syntax = objectClass.getWithSyntax();
        if (syntax == null) return;

        List<String> tokens = object.getSyntaxTokens();
        List<WithSyntax.FieldPattern> patterns = syntax.getFieldPatterns();
        List<Match> matches = new ArrayList<>();
        int from = 0;
        for (WithSyntax.FieldPattern pattern : patterns) {
            int position = findPhrase(tokens, pattern.leadingLiterals(), from);
            if (position < 0) {
                if (!pattern.optional()) {
                    error("Required WITH SYNTAX phrase is missing: " + String.join(" ", pattern.leadingLiterals()),
                            object.getSourceRange());
                }
                continue;
            }
            matches.add(new Match(pattern, position, position + pattern.leadingLiterals().size()));
            from = position + pattern.leadingLiterals().size();
        }

        for (int i = 0; i < matches.size(); i++) {
            Match match = matches.get(i);
            int valueStart = match.valueStart;
            int valueEnd = i + 1 < matches.size() ? matches.get(i + 1).phraseStart : tokens.size();
            while (valueEnd > valueStart && (tokens.get(valueEnd - 1).equals(",") || tokens.get(valueEnd - 1).equals("|"))) valueEnd--;
            if (valueStart >= valueEnd) continue;

            ObjectClassField field = objectClass.findField(match.pattern.fieldName()).orElse(null);
            if (field == null) {
                error("WITH SYNTAX refers to undeclared class field '&" + match.pattern.fieldName() + "'",
                        object.getSourceRange());
                continue;
            }
            SourceRange range = mergeRanges(object.getSyntaxTokenRanges().get(valueStart),
                    object.getSyntaxTokenRanges().get(valueEnd - 1));
            String raw = document.sourceText(range).trim();
            ObjectFieldSetting.Kind kind = field.isTypeField()
                    ? ObjectFieldSetting.Kind.TYPE : ObjectFieldSetting.Kind.VALUE;
            String referenceName = firstReferenceToken(tokens, valueStart, valueEnd);
            AsnType inlineBuiltin = field.isTypeField() ? inlineBuiltinType(tokens, valueStart, valueEnd, range) : null;
            SymbolReference reference = referenceName == null || inlineBuiltin != null ? null : new SymbolReference(
                    null, referenceName, field.isTypeField() ? SymbolKind.TYPE : SymbolKind.VALUE, range);
            ObjectFieldSetting setting = new ObjectFieldSetting(field.getName(), kind, raw, range, reference);
            setting.linkField(field);
            setting.setTypeValue(inlineBuiltin);
            object.addSetting(setting);

            if (reference != null) {
                if (field.isTypeField()) {
                    resolve(module, reference, Map.of(), SymbolKind.TYPE, true);
                } else {
                    EnumerationItem enumItem = findEnumerationItem(field.getGovernor(), referenceName);
                    if (enumItem != null) link(reference, enumItem);
                    else resolve(module, reference, Map.of(), SymbolKind.VALUE, true);
                }
            }
        }
    }

    private int findPhrase(List<String> tokens, List<String> phrase, int from) {
        if (phrase.isEmpty()) return from;
        int braces = 0;
        int parentheses = 0;
        for (int i = from; i + phrase.size() <= tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.equals("{")) braces++;
            else if (token.equals("}")) braces--;
            else if (token.equals("(")) parentheses++;
            else if (token.equals(")")) parentheses--;
            if (braces != 0 || parentheses != 0) continue;
            boolean matches = true;
            for (int j = 0; j < phrase.size(); j++) {
                if (!tokens.get(i + j).equals(phrase.get(j))) { matches = false; break; }
            }
            if (matches) return i;
        }
        return -1;
    }

    private String firstReferenceToken(List<String> tokens, int start, int end) {
        for (int i = start; i < end; i++) {
            String token = tokens.get(i);
            if (token.matches("[A-Za-z][A-Za-z0-9-]*")) return token;
        }
        return null;
    }

    private AsnType inlineBuiltinType(List<String> tokens, int start, int end, SourceRange range) {
        if (start >= end) return null;
        String first = tokens.get(start);
        if (first.equals("INTEGER")) return new IntegerType(List.of(), List.of(), range);
        if (first.equals("SEQUENCE") || first.equals("SET") || first.equals("CHOICE")
                || first.equals("ENUMERATED")) {
            return new RawType(document.sourceText(range).trim(), List.of(), range);
        }
        BuiltinType.Kind kind = switch (first) {
            case "NULL" -> BuiltinType.Kind.NULL;
            case "BOOLEAN" -> BuiltinType.Kind.BOOLEAN;
            case "REAL" -> BuiltinType.Kind.REAL;
            case "UTF8String" -> BuiltinType.Kind.UTF8_STRING;
            case "PrintableString" -> BuiltinType.Kind.PRINTABLE_STRING;
            case "IA5String" -> BuiltinType.Kind.IA5_STRING;
            case "VisibleString" -> BuiltinType.Kind.VISIBLE_STRING;
            case "NumericString" -> BuiltinType.Kind.NUMERIC_STRING;
            case "BMPString" -> BuiltinType.Kind.BMP_STRING;
            case "OCTET" -> start + 1 < end && tokens.get(start + 1).equals("STRING")
                    ? BuiltinType.Kind.OCTET_STRING : null;
            case "BIT" -> start + 1 < end && tokens.get(start + 1).equals("STRING")
                    ? BuiltinType.Kind.BIT_STRING : null;
            case "OBJECT" -> start + 1 < end && tokens.get(start + 1).equals("IDENTIFIER")
                    ? BuiltinType.Kind.OBJECT_IDENTIFIER : null;
            case "RELATIVE-OID" -> BuiltinType.Kind.RELATIVE_OID;
            case "ANY" -> BuiltinType.Kind.ANY;
            default -> null;
        };
        return kind == null ? null : new BuiltinType(kind, List.of(), List.of(), range);
    }

    private void buildOpenTypeAlternatives(InformationObjectFieldType fieldType) {
        if (fieldType.getField() == null || !fieldType.getField().isTypeField()) return;
        Constraint table = fieldType.getTableConstraint();
        if (table == null) return;

        ObjectSetAssignment set = null;
        for (SymbolReference reference : allConstraintReferences(table)) {
            if (reference.getTarget() instanceof ObjectSetAssignment targetSet) { set = targetSet; break; }
        }
        if (set == null) return;

        String selectorField = table.getSelectorPath().isEmpty() ? null : table.getSelectorPath().get(0);
        ObjectClassDefinition objectClass = fieldType.getObjectClass() == null
                ? null : fieldType.getObjectClass().getDefinition();
        ObjectClassField uniqueField = selectorField == null || objectClass == null
                ? null : objectClass.findField(selectorField).orElse(null);

        for (ObjectDefinition object : set.getExpression().getAllObjects()) {
            ObjectFieldSetting typeSetting = object.findSetting(fieldType.getFieldName()).orElse(null);
            if (typeSetting == null || !(typeSetting.getValueTarget() instanceof TypeAssignment typeTarget)) continue;
            ObjectFieldSetting selectorSetting = uniqueField == null ? null : object.findSetting(uniqueField.getName()).orElse(null);
            Assignment selectorTarget = selectorSetting != null && selectorSetting.getValueTarget() instanceof Assignment assignment
                    ? assignment : null;
            fieldType.addAlternative(new OpenTypeAlternative(
                    selectorSetting == null ? null : selectorSetting.getSourceText(),
                    selectorTarget, typeTarget, object));
        }
    }

    private List<SymbolReference> allConstraintReferences(Constraint root) {
        List<SymbolReference> result = new ArrayList<>(root.getReferences());
        for (Constraint child : root.getChildren()) result.addAll(allConstraintReferences(child));
        return result;
    }

    private NamedElement resolve(Asn1Module module, SymbolReference reference,
                                 Map<String, FormalParameter> scope, SymbolKind expected,
                                 boolean reportMissing) {
        if (reference.isResolved()) return reference.getTarget();

        if (reference.getModuleQualifier() == null) {
            FormalParameter parameter = scope.get(reference.getName());
            if (parameter != null && (expected == SymbolKind.ANY || expected == SymbolKind.FORMAL_PARAMETER
                    || compatibleFormal(parameter, expected))) {
                link(reference, parameter);
                return parameter;
            }
        }

        Asn1Module searchModule = module;
        if (reference.getModuleQualifier() != null) {
            searchModule = document.findModule(reference.getModuleQualifier()).orElse(null);
        }
        if (searchModule != null) {
            Assignment local = searchModule.findAssignment(reference.getName()).orElse(null);
            if (local != null && compatible(local, expected)) {
                link(reference, local);
                return local;
            }
        }

        if (reference.getModuleQualifier() == null) {
            for (ModuleImport moduleImport : module.getImports()) {
                for (ImportedSymbol imported : moduleImport.getSymbols()) {
                    if (imported.getName().equals(reference.getName()) && imported.getTarget() != null
                            && compatible(imported.getTarget(), expected)) {
                        link(reference, imported.getTarget());
                        return imported.getTarget();
                    }
                }
            }
        }

        if (reportMissing && reported.add(reference)) {
            error("Unresolved " + expected + " reference '" + reference.getQualifiedName()
                    + "' in module " + module.getName(), reference.getSourceRange());
        }
        return null;
    }

    private boolean compatibleFormal(FormalParameter parameter, SymbolKind expected) {
        return parameter.getParameterKind() == expected || expected == SymbolKind.ANY;
    }

    private boolean compatible(Assignment assignment, SymbolKind expected) {
        return expected == SymbolKind.ANY || assignment.getSymbolKind() == expected
                || expected == SymbolKind.VALUE && assignment instanceof ObjectAssignment
                || expected == SymbolKind.OBJECT_SET && assignment instanceof ObjectSetAssignment;
    }

    private void link(SymbolReference reference, NamedElement target) {
        reference.linkTo(target);
        document.getReferenceIndex().add(reference);
    }

    private SourceRange mergeRanges(SourceRange first, SourceRange last) {
        return new SourceRange(first.sourceName(), first.startOffset(), last.endOffset(),
                first.startLine(), first.startColumn(), last.endLine(), last.endColumn());
    }

    private void error(String message, SourceRange range) {
        document.addDiagnostic(new Diagnostic(Diagnostic.Severity.ERROR, message, range));
    }

    private record Match(WithSyntax.FieldPattern pattern, int phraseStart, int valueStart) { }
}

package com.ancevt.asn.resolve;

import com.ancevt.asn.model.*;
import com.ancevt.asn.model.type.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AsnLinker {

    private final AsnModel model;

    public AsnLinker(AsnModel model) {
        this.model = model;
    }

    public void link() {
        for (ModuleDefinition module : model.getModules()) {
            linkTypes(module);
        }

        for (ModuleDefinition module : model.getModules()) {
            linkObjectSets(module);
        }

        for (ModuleDefinition module : model.getModules()) {
            linkObjects(module);
        }

        for (ModuleDefinition module : model.getModules()) {
            linkIoFieldRefs(module);
        }
    }

    private void linkTypes(ModuleDefinition module) {
        for (AsnType type : module.getTypes().values()) {
            linkTypeRecursive(module, type);
        }

        for (ClassType clazz : module.getClasses().values()) {
            for (ClassField field : clazz.getFields()) {
                if (field.getType() != null) {
                    linkTypeRecursive(module, field.getType());
                }
            }
        }
    }

    private void linkTypeRecursive(
            ModuleDefinition module,
            AsnType type
    ) {

        if (type == null) {
            return;
        }

        // TypeRef
        if (type instanceof TypeRef ref) {

            // если уже резолвлен — не трогаем
            if (ref.getResolvedType() != null) {
                return;
            }

            AsnType resolved =
                    resolveType(module, ref.getReferencedName());

            ref.setResolvedType(resolved);

            // важно: линкаем то, на что он ссылается
            linkTypeRecursive(module, resolved);

            return;
        }

        // SEQUENCE
        if (type instanceof SequenceType seq) {
            for (var field : seq.getFields()) {
                linkTypeRecursive(module, field.getType());
            }
            return;
        }

        // SEQUENCE OF
        if (type instanceof SequenceOfType seqOf) {
            linkTypeRecursive(module, seqOf.getElementType());
            return;
        }

        // CHOICE
        if (type instanceof ChoiceType choice) {
            for (var option : choice.getOptions()) {
                linkTypeRecursive(module, option.getType());
            }
            return;
        }

        // IoFieldRefType
        if (type instanceof IoFieldRefType) {
            return;
        }

    }

    private AsnType resolveType(ModuleDefinition module, String name) {

        var local = module.getType(name);
        if (local.isPresent()) {
            return local.get();
        }

        for (var imp : module.getImports()) {
            if (imp.getSymbols().contains(name)) {
                var importedModule = model.getModule(imp.getFromModule())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Imported module not found: " +
                                                imp.getFromModule()));

                return importedModule.getType(name)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Imported type not found: " + name));
            }
        }

        AsnType global = resolveTypeGlobal(name);
        if (global != null) {
            return global;
        }

        throw new IllegalStateException("Unresolved type reference: " + name);
    }


    private void linkObjectSets(ModuleDefinition module) {

        for (ObjectSet set : module.getObjectSets().values()) {

            var resolvedSetRefs = new ArrayList<ObjectSet>();

            for (String refName : set.getReferencedSetNames()) {

                ObjectSet referencedSet = resolveObjectSetGlobal(refName);
                if (referencedSet != null) {
                    resolvedSetRefs.add(referencedSet);
                    continue;
                }

                ObjectInstance object = resolveObjectInstanceGlobal(refName);
                if (object != null) {
                    set.addInlineObject(object);
                    continue;
                }

            }

            set.setResolvedReferences(resolvedSetRefs);
        }
    }

    private ObjectInstance resolveObjectInstanceGlobal(String name) {
        for (ModuleDefinition m : model.getModules()) {
            var obj = m.getObject(name);
            if (obj.isPresent()) {
                return obj.get();
            }
        }
        return null;
    }


    private void linkObjects(ModuleDefinition module) {

        for (var object : module.getObjects().values()) {

            ClassType clazz = resolveClassGlobal(object.getClassName());

            if (clazz == null) {
                throw new IllegalStateException(
                        "Unresolved CLASS: " + object.getClassName()
                );
            }

            object.setResolvedClass(clazz);

            for (String fieldName : object.getValues().keySet()) {

                if (clazz.getField(fieldName).isEmpty()) {
                    throw new IllegalStateException(
                            "Field " + fieldName +
                                    " not found in CLASS " +
                                    clazz.getName()
                    );
                }
            }
        }
    }

    private AsnType resolveTypeGlobal(String name) {
        for (ModuleDefinition m : model.getModules()) {
            var t = m.getType(name);
            if (t.isPresent()) {
                return t.get();
            }
        }
        return null;
    }

    private ClassType resolveClassGlobal(String name) {
        for (ModuleDefinition m : model.getModules()) {
            var c = m.getClass(name);
            if (c.isPresent()) {
                return c.get();
            }
        }
        return null;
    }

    private ObjectSet resolveObjectSetGlobal(String name) {
        for (ModuleDefinition m : model.getModules()) {
            var s = m.getObjectSet(name);
            if (s.isPresent()) {
                return s.get();
            }
        }
        return null;
    }

    private void linkIoFieldRefs(ModuleDefinition module) {
        for (AsnType type : module.getTypes().values()) {
            linkIoRecursive(module, type, Map.of());
        }
    }

    private void linkIoRecursive(
            ModuleDefinition module,
            AsnType type,
            Map<String, ObjectSet> paramMap
    ) {
        if (type instanceof IoFieldRefType ioRef) {
            AsnType resolved = resolveIoField(module, ioRef, paramMap);
            ioRef.setResolvedType(resolved);
            return;
        }

        if (type instanceof SequenceType seq) {
            for (var field : seq.getFields()) {
                linkIoRecursive(module, field.getType(), paramMap);
            }
        }

        if (type instanceof SequenceOfType seqOf) {
            linkIoRecursive(module, seqOf.getElementType(), paramMap);
        }

        if (type instanceof ChoiceType choice) {
            for (var option : choice.getOptions()) {
                linkIoRecursive(module, option.getType(), paramMap);
            }
        }

        if (type instanceof TypeRef ref) {
            if (ref.getResolvedType() != null) {

                ModuleDefinition owner =
                        resolveTypeOwnerModule(ref.getReferencedName());

                if (owner == null) {
                    owner = module;
                }

                var formalParamsOpt =
                        owner.getTypeFormalParameters(ref.getReferencedName());

                if (formalParamsOpt.isPresent()) {

                    List<String> formalParams = formalParamsOpt.get();
                    List<String> actualParams = ref.getActualParameters();

                    Map<String, ObjectSet> newMap = new HashMap<>(paramMap);

                    for (int i = 0; i < formalParams.size(); i++) {

                        if (i < actualParams.size()) {

                            String formal = formalParams.get(i);
                            String actualName = actualParams.get(i);

                            ObjectSet actualSet = resolveObjectSetGlobal(actualName);

                            if (actualSet != null) {
                                newMap.put(formal, actualSet);
                            }
                        }
                    }

                    linkIoRecursive(owner, ref.getResolvedType(), newMap);

                } else {
                    linkIoRecursive(owner, ref.getResolvedType(), paramMap);
                }
            }
        }
    }

    private AsnType resolveIoField(
            ModuleDefinition module,
            IoFieldRefType ref,
            Map<String, ObjectSet> paramMap
    ) {

        ClassType clazz = resolveClassGlobal(ref.getClassName());

        if (clazz == null) {
            ObjectSet set = resolveObjectSetGlobal(ref.getClassName());
            if (set != null) {
                clazz = resolveClassGlobal(set.getClassName());
            }
        }

        if (clazz == null) {
            throw new IllegalStateException(
                    "CLASS not found: " + ref.getClassName()
            );
        }

        ClassType finalClazz = clazz;
        var field = clazz.getField(ref.getClassFieldName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Field " +
                                        ref.getClassFieldName() +
                                        " not found in CLASS " +
                                        finalClazz.getName()
                        ));

        AsnType fieldType = field.getType();

        if (fieldType == null) {
            return resolveIoSelection(module, clazz, field, ref, paramMap);
        }

        if (ref.getSelection() == null) {
            return fieldType;
        }

        return resolveIoSelection(module, clazz, field, ref, paramMap);
    }

    private void collectObjectsRecursively(
            ObjectSet set,
            List<ObjectInstance> out
    ) {
        out.addAll(set.getInlineObjects());

        for (ObjectSet ref : set.getResolvedReferences()) {
            collectObjectsRecursively(ref, out);
        }
    }

    private ModuleDefinition resolveTypeOwnerModule(String typeName) {
        for (ModuleDefinition m : model.getModules()) {
            if (m.getType(typeName).isPresent()) {
                return m;
            }
        }
        return null;
    }

    private AsnType resolveIoSelection(
            ModuleDefinition module,
            ClassType clazz,
            ClassField field,
            IoFieldRefType ref,
            Map<String, ObjectSet> paramMap
    ) {

        String setName = ref.getSelection().getObjectSetParamName();

        ObjectSet set = paramMap.get(setName);

        if (set == null) {
            set = resolveObjectSetGlobal(setName);
        }

        if (set == null) {
            if (!ref.getSelection().isKeyed()) {
                return field.getType();
            }

            return field.getType();
        }


        if (!ref.getSelection().isKeyed()) {
            return field.getType() != null
                    ? field.getType()
                    : resolveIoSelection(module, clazz, field, ref, paramMap);
        }

        String selectorName = ref.getSelection().getSelectorFieldName();
        String selectorFieldKey = "&" + selectorName;

        ClassField selectorField =
                clazz.getField(selectorFieldKey)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Selector field not found in CLASS: " +
                                                selectorFieldKey
                                ));

        if (!selectorField.isUnique()) {
            throw new IllegalStateException(
                    "Selector field is not UNIQUE: " +
                            selectorFieldKey
            );
        }

        var allObjects = new ArrayList<ObjectInstance>();
        collectObjectsRecursively(set, allObjects);

        if (allObjects.isEmpty()) {
            return field.getType();
        }

        var options = new ArrayList<Field>();

        for (ObjectInstance object : allObjects) {

            var selectorOpt = object.getValue(selectorField.getName());
            if (selectorOpt.isEmpty()) {
                continue;
            }

            var valueOpt = object.getValue(field.getName());
            if (valueOpt.isEmpty()) {
                continue;
            }

            Object selectorValue = selectorOpt.get();
            Object value = valueOpt.get();

            if (!(value instanceof String typeName)) {
                continue;
            }

            AsnType resolvedType = resolveType(module, typeName);
            linkTypeRecursive(module, resolvedType);

            options.add(new Field(selectorValue.toString(), resolvedType, false));
        }

        if (options.isEmpty()) {
            return field.getType();
        }
        return new ChoiceType(options);
    }
}

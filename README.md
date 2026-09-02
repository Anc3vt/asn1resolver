# ASN.1 Resolver

Небольшой schema parser для Java 17, который превращает ASN.1-файл в типизированный,
пролинкованный и удобный для отладки object graph. Проект сделан вокруг реального
`s1ap.asn`: один физический файл содержит шесть взаимозависимых модулей, поэтому
сначала разбирается весь документ, а ссылки разрешаются отдельным вторым проходом.

Это анализатор схемы, а не BER/PER-кодек и не генератор Java DTO.

Подробное устройство parser, object model и linker описано в
[`ARCHITECTURE.md`](ARCHITECTURE.md).

## Быстрый старт

```java
import com.ancevt.asn1.Asn1;
import com.ancevt.asn1.model.*;

import java.nio.file.Path;

Asn1Document document = Asn1.read(Path.of("s1ap.asn"));

Asn1Module module = document.requireModule("S1AP-IEs");
TypeAssignment assignment = module.requireType("Global-ENB-ID");
ConstructedType sequence = (ConstructedType) assignment.getType();
Component component = sequence.requireComponent("pLMNidentity");

ReferenceType reference = (ReferenceType) component.getType();
TypeAssignment target = reference.getTarget(); // прямой переход к PLMNidentity
```

Запустите `com.ancevt.asn1.demo.DebugMain` из IntelliJ IDEA и поставьте breakpoint
на строке сразу после `Asn1.read(...)`. В окне Variables удобно раскрывать:

```text
document
 └─ modules
     └─ assignments
         └─ TypeAssignment.type
             └─ ConstructedType.components
                 └─ ReferenceType.reference.target
```

Граф намеренно циклический: assignment ссылается на module, а reference — обратно
на target assignment. `toString()` у моделей короткий и не обходит граф рекурсивно.

## Что находится в модели

- `Asn1Document` — модули, diagnostics и обратный `ReferenceIndex`;
- `Asn1Module` — OID, tagging mode, imports и пять видов assignments;
- `TypeAssignment`, `ValueAssignment`, `ObjectClassAssignment`,
  `ObjectAssignment`, `ObjectSetAssignment`;
- типы `ConstructedType`, `CollectionType`, `IntegerType`, `EnumeratedType`,
  `BuiltinType`, `ReferenceType`, `InformationObjectFieldType` и `TaggedType`;
- ограничения с деревом `SIZE`, range, union и table/component-relation constraint;
- object classes, поля класса, `WITH SYNTAX`, object settings и раскрываемые object sets;
- formal/actual parameters с прямой связью actual argument → formal parameter;
- `SourceRange` на узлах: путь, offsets, строки и колонки исходника.

Каждый `SymbolReference` сохраняет и написанное в исходнике имя, и поле `target`.
Для поиска ссылок в обратную сторону:

```java
var usages = document.getReferenceIndex().getReferencesTo(target);
```

У open type, заданного как `CLASS.&Value ({ObjectSet}{@selector})`, доступны:

- `getObjectClass()` и `getField()`;
- table constraint и привязанный sibling component в `getSelectorTargets()`;
- рассчитанные варианты `getAlternatives()` для конкретного object set.

У параметризованного `ReferenceType` каждый `ActualParameter` содержит
`getFormalParameter()` и пролинкованные `getReferences()`.

## Поддержанный синтаксис

Реализация покрывает конструкции, используемые S1AP/3GPP ASN.1:

- несколько модулей в одном файле, module OID, `AUTOMATIC/IMPLICIT/EXPLICIT TAGS`;
- `IMPORTS ... FROM ...` с разрешением вперед объявленных модулей;
- `SEQUENCE`, `SET`, `CHOICE`, `SEQUENCE OF`, `SET OF`, `COMPONENTS OF`;
- `INTEGER`, `ENUMERATED`, `BIT STRING`, `OCTET STRING`, строки, `NULL`,
  `BOOLEAN`, `REAL`, `OBJECT IDENTIFIER`;
- `OPTIONAL`, `DEFAULT`, extension marker `...`, named numbers;
- `SIZE`, ranges, unions, extensible constraints и symbolic bounds;
- parameterized types, formal/actual parameters и двойные `{{...}}`;
- information object classes, `WITH SYNTAX`, objects и object-set unions;
- class-field references и table/component-relation constraints с `@`.

Редкие конструкции вне этого подмножества могут потребовать расширения parser.
`RawType` и `RawValue` оставлены в модели как lossless-точки расширения; текущий
parser разбирает заявленное подмножество строго. Ошибка синтаксиса сообщает
точные файл, строку и колонку.

## Сборка и запуск

```powershell
mvn clean test
mvn -DskipTests package
java -cp target/classes com.ancevt.asn1.demo.DebugMain s1ap.asn
```

Интеграционный тест полного файла проверяет 6 модулей, 1322 assignments,
686 imports, отсутствие unresolved diagnostics и несколько многоуровневых
переходов через type references, параметры, object sets и open types.

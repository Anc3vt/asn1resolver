# Архитектура ASN.1 Resolver

## 1. Назначение

ASN.1 Resolver читает текст ASN.1-схемы и строит Java object graph, предназначенный
для исследования схемы из прикладного кода и debugger IntelliJ IDEA.

Главное свойство результата — ссылки в исходном ASN.1 не остаются строками. Узел
`ReferenceType`, `ReferenceValue` или другой `SymbolReference` хранит:

- имя ровно в том виде, в котором оно написано в исходнике;
- ожидаемый вид символа;
- позицию употребления в файле;
- прямой `target` на найденное объявление.

Например, после разбора компонента

```asn1
pLMNidentity PLMNidentity
```

поле `Component.type` содержит `ReferenceType`, а
`ReferenceType.reference.target` непосредственно указывает на соответствующий
`TypeAssignment`. Повторный поиск по строковому имени пользователю не нужен.

Проект является анализатором схемы. Он намеренно не решает следующие задачи:

- кодирование и декодирование BER, DER, PER или APER;
- генерация Java DTO для ASN.1 значений;
- валидация сетевого сообщения по схеме;
- полная реализация всех редакций стандартов X.680–X.683.

Поддержанное подмножество выбрано так, чтобы без потерь разобрать находящийся в
корне `s1ap.asn`, включая information object classes, параметризацию и table
constraints.

## 2. Основные архитектурные решения

### 2.1. Отдельные синтаксический и семантический проходы

Парсер не пытается разрешать имена во время чтения. Физический `s1ap.asn`
содержит шесть модулей, причем используемый класс может быть объявлен в модуле,
стоящем ближе к концу файла. Поэтому работа разделена на два больших этапа:

1. lexer и parser строят полный непролинкованный AST;
2. linker индексирует все объявления и заполняет ссылки.

Это устраняет зависимость результата от порядка модулей и объявлений.

### 2.2. Ссылка является отдельным узлом

После linking ссылка не заменяется найденным типом. Сохраняются оба объекта:

```text
ReferenceType
 ├─ reference.name = "PLMNidentity"
 ├─ reference.sourceRange = место употребления
 └─ reference.target ───────────────► TypeAssignment("PLMNidentity")
```

Так одновременно доступны синтаксис употребления и семантическая цель. Это
также позволяет построить обратный `ReferenceIndex`.

### 2.3. Объявления имеют identity

Ссылки ведут на единый объект объявления, а не на его рекурсивную копию. Это
важно для:

- взаимно-рекурсивных типов;
- повторно используемых типов;
- parameterized definitions;
- object-set unions;
- обратной навигации.

В результате граф может быть циклическим. Модель не использует рекурсивные
`equals()`, `hashCode()` или `toString()`.

### 2.4. Defined syntax интерпретируется после linking

Тело information object вида

```asn1
{ ID id-MME-UE-S1AP-ID CRITICALITY reject
  TYPE MME-UE-S1AP-ID PRESENCE mandatory }
```

невозможно надежно сопоставить полям класса, пока не найден соответствующий
`CLASS ... WITH SYNTAX`. Parser поэтому сохраняет сбалансированное token body, а
linker позднее превращает его в `ObjectFieldSetting`.

### 2.5. Минимум runtime-зависимостей

Lexer и recursive-descent parser реализованы внутри проекта. Runtime-модель не
зависит от генератора parser или стороннего ASN.1 compiler. Maven-зависимости
нужны только тестам.

## 3. Общая схема компонентов

```text
Path / String
     │
     ▼
┌──────────────┐
│ Asn1 facade  │  чтение UTF-8, нормализация имени source
└──────┬───────┘
       ▼
┌──────────────┐
│    Lexer     │  текст → immutable List<Token> + EOF
└──────┬───────┘
       ▼
┌──────────────┐
│ SyntaxParser │  tokens → Asn1Document с unresolved references
└──────┬───────┘
       ▼
┌──────────────┐
│ Asn1Linker   │  symbol tables, imports, targets, defined syntax,
│              │  parameter bindings и open-type alternatives
└──────┬───────┘
       ▼
┌──────────────────────────────────────────────────────────────┐
│ Asn1Document                                                │
│ modules → assignments → types/components/constraints        │
│                    └──────── SymbolReference.target ──────┐  │
│ ReferenceIndex ◄──────────────────────────────────────────┘  │
│ diagnostics                                                 │
└──────────────────────────────────────────────────────────────┘
```

Публичный обычный маршрут:

```java
Asn1Document document = Asn1.read(Path.of("s1ap.asn"));
```

Он эквивалентен последовательности:

```java
String source = Files.readString(path, StandardCharsets.UTF_8);
Asn1Document document = Asn1Parser.parse(source, path.toString());
// Внутри: Lexer → SyntaxParser → Asn1Linker.
```

Для экспериментов с собственным resolver доступен
`Asn1Parser.parseUnlinked(source, sourceName)`.

## 4. Структура пакетов

### `com.ancevt.asn1`

`Asn1` — высокоуровневый facade. Он предоставляет:

- `read(Path)` — чтение UTF-8;
- `read(Path, Charset)` — чтение в заданной кодировке;
- `parse(String)` — разбор строки с именем `<memory>`;
- `parse(String, String)` — разбор строки с пользовательским именем source.

### `com.ancevt.asn1.parse`

Содержит внутренний lexer и parser:

- `Lexer`;
- `Token`;
- `TokenKind`;
- `SyntaxParser`;
- публичный facade нижнего уровня `Asn1Parser`;
- `Asn1ParseException`.

`Lexer`, `Token`, `TokenKind` и `SyntaxParser` package-private: внешний код должен
работать с моделью, а не зависеть от деталей token stream.

### `com.ancevt.asn1.model`

Содержит публичную объектную модель. Классы небольшие и специализированные,
чтобы их поля было удобно раскрывать в debugger.

### `com.ancevt.asn1.link`

`Asn1Linker` выполняет семантическое связывание уже построенного документа.

### `com.ancevt.asn1.demo`

`DebugMain` содержит несколько готовых путей по реальному S1AP-графу и точку для
breakpoint.

## 5. Lexer

### 5.1. Результат

`Lexer.lex()` возвращает неизменяемый `List<Token>`, заканчивающийся токеном
`EOF`. Каждый token содержит:

- `TokenKind`;
- исходную `lexeme` без нормализации регистра;
- `sourceName`;
- half-open offsets `[startOffset, endOffset)`;
- начальные и конечные line/column.

Offsets считаются от нуля, строки и колонки — от единицы. Конечная позиция
указывает на первый символ после token.

### 5.2. Лексические правила

Поддерживаются:

- identifiers с ASN.1-дефисами;
- произвольные по длине десятичные числа;
- quoted strings с удвоенной кавычкой;
- binary и hexadecimal strings `'...'B` и `'...'H`;
- longest-match операторы `::=`, `...`, `..`;
- скобки, запятые, `|`, `&`, `.`, `@`, `:`, знаки и прочая пунктуация;
- однострочные комментарии `--`;
- блочные комментарии `/* ... */`;
- CRLF как один перенос строки.

Содержимое B/H-литералов валидируется lexer. Неизвестный символ, незавершенный
комментарий или строка немедленно приводят к `Asn1ParseException` с точной
позицией.

Комментарии и whitespace не становятся AST-узлами. При этом исходный текст
целиком хранится в `Asn1Document`, поэтому текст любого `SourceRange` можно
получить позднее.

## 6. SyntaxParser

### 6.1. Строгий режим

Parser не делает silent recovery и не пропускает неизвестные tokens. Успешный
результат означает, что вход потреблен до `EOF`. Синтаксическая ошибка бросает
`Asn1ParseException` и не превращается в semantic diagnostic.

### 6.2. Предварительное обнаружение object classes

До основного разбора `SyntaxParser` просматривает tokens и собирает имена
объявлений формы:

```asn1
Some-Class ::= CLASS { ... }
```

Это нужно для классификации governed assignments. Левая часть

```asn1
someObject Some-Class ::= { ... }
```

синтаксически похожа на value assignment. Зная, что governor является object
class, parser создает `ObjectAssignment` либо `ObjectSetAssignment`. После
подтвержденного class governor используется стандартное ASN.1 различие регистра:
object reference начинается со строчной буквы, object-set reference — с
прописной.

### 6.3. Разбор документа и модулей

`parseDocument()` читает модули до общего `EOF`. Поэтому один физический файл
может содержать несколько последовательных `DEFINITIONS ... BEGIN ... END`.

Для модуля сохраняются:

- имя;
- definitive object identifier;
- `TaggingMode`;
- `EXTENSIBILITY IMPLIED`;
- imports;
- упорядоченный список assignments;
- полный `SourceRange`.

`EXPORTS` допускается и корректно ограничивается точкой с запятой, но отдельно в
модели сейчас не хранится, поскольку в S1AP он отсутствует.

### 6.4. Imports

Один `ModuleImport` соответствует группе `symbols FROM Module`. Каждый
`ImportedSymbol` сохраняется отдельно. На parser-этапе это только имена;
`sourceModule` и `target` заполняются linker.

Parameterized import вида `ProtocolIE-Container{}` хранит имя символа без
служебной пустой пары фигурных скобок.

### 6.5. Классификация assignments

Базовый класс `Assignment` хранит имя, модуль-владелец, source range и исходный
текст объявления. Конкретные варианты:

| Класс | ASN.1-форма | Основное содержимое |
|---|---|---|
| `TypeAssignment` | `Name ::= Type` | formal parameters и `AsnType` |
| `ValueAssignment` | `name Type ::= Value` | governor и `AsnValue` |
| `ObjectClassAssignment` | `Name ::= CLASS` | `ObjectClassDefinition` |
| `ObjectAssignment` | `name Class ::= { ... }` | class reference и object body |
| `ObjectSetAssignment` | `Name Class ::= { ... }` | class reference и set expression |

Порядок объявлений сохраняется. `Asn1Module` дополнительно предоставляет индекс
по точному case-sensitive имени и typed getters для каждого вида.

### 6.6. Разбор типов

`AsnType` является базовым классом всех типов и всегда содержит `SourceRange` и
список constraints.

| Модель | Представляемый синтаксис |
|---|---|
| `BuiltinType` | BOOLEAN, NULL, REAL, strings, BIT/OCTET STRING, OID, ANY |
| `IntegerType` | INTEGER и named numbers |
| `EnumeratedType` | ENUMERATED, root/extension items |
| `ConstructedType` | SEQUENCE, SET, CHOICE |
| `CollectionType` | SEQUENCE OF, SET OF |
| `ReferenceType` | defined и parameterized type reference |
| `InformationObjectFieldType` | `Class.&field (table constraint)` |
| `TaggedType` | explicit/implicit/default tag вокруг вложенного типа |
| `RawType` | точка расширения модели для lossless unsupported type |

`ConstructedType` содержит упорядоченные `Component`. Для компонента сохраняются
имя, тип, `OPTIONAL`, default value, `COMPONENTS OF` и положение относительно
extension marker.

`EnumeratedType` и `ConstructedType` имеют `isExtensible()`. Элементы после `...`
помечаются как extension additions.

`AsnType.dereference()` следует по цепочке `ReferenceType.target.type` до
конкретного типа. Identity-set предотвращает зацикливание на рекурсивных aliases.

### 6.7. Constraints

Constraint не сводится только к строке. `Constraint` хранит:

- `Kind`: `SIZE`, `RANGE`, `UNION`, `INTERSECTION`, `TABLE`, `SINGLE_VALUE`,
  `CONTENTS` либо `RAW`;
- исходный текст;
- дочерние constraints;
- symbol references для symbolic bounds и object sets;
- `selectorPath` для `@component`;
- связанные `selectorTargets`;
- признак extensibility;
- `BigInteger` для одиночного целого literal.

Например:

```asn1
INTEGER (0..18446744073709551615)
```

представляется `RANGE` с двумя дочерними `SINGLE_VALUE`. Число не ограничивается
диапазоном Java `long`.

Для

```asn1
({IEsSetParam}{@id})
```

создается `TABLE` constraint со ссылкой на object-set/formal parameter и путем
selector `id`. Linker позднее находит sibling `Component("id")` и добавляет его в
`selectorTargets`.

### 6.8. Значения

Иерархия `AsnValue` включает:

- `LiteralValue` для integer, string, bit/hex string, boolean и NULL; enum модели
  также резервирует вид REAL для дальнейшего расширения value parser;
- `ReferenceValue`;
- `CollectionValue`;
- `RawValue` для lossless braced value, не интерпретированного текущим parser.

Integer literals хранятся как `BigInteger`.

### 6.9. Parameterized types

В объявлении

```asn1
ProtocolIE-Container {
    S1AP-PROTOCOL-IES : IEsSetParam
} ::= ...
```

создается `FormalParameter`:

- `name = IEsSetParam`;
- governor text и, если применимо, governor reference;
- ожидаемый `SymbolKind` параметра;
- source range.

В употреблении

```asn1
ProtocolIE-Container {{HandoverRequiredIEs}}
```

создается `ReferenceType` с `ActualParameter`. Parser сохраняет исходный текст
argument и найденные внутри references. Linker после разрешения целевого
`TypeAssignment`:

1. проверяет arity;
2. связывает actual parameter с соответствующим `FormalParameter`;
3. разрешает references аргумента согласно виду formal parameter.

Из debugger доступны оба направления текущего binding:

```java
referenceType.getActualParameters().get(0).getFormalParameter();
referenceType.getParameterBindings();
```

Parameterized type не клонируется для каждого употребления. Конкретный argument,
formal parameter и единое тело definition образуют связанный граф.

## 7. Information object model

### 7.1. Object class

`ObjectClassAssignment` содержит `ObjectClassDefinition`. В нем находятся:

- `ObjectClassField`;
- optional `WithSyntax` template;
- source range.

Поле object class различает:

- type field, например `&Value`;
- fixed-type value field, например `&id ProtocolIE-ID`;
- `UNIQUE`;
- `OPTIONAL`;
- `DEFAULT`.

Синтаксис `WITH SYNTAX` представлен деревом:

- `WithSyntax.Literal`;
- `WithSyntax.Field`;
- `WithSyntax.OptionalGroup`.

Дополнительно строится плоский список `FieldPattern`, удобный для сопоставления
defined-syntax object bodies.

### 7.2. Objects

Во время parsing `ObjectDefinition` содержит:

- полный исходный текст;
- token strings тела;
- source range каждого token;
- пока пустой список settings.

После разрешения class linker сопоставляет leading literals каждого
`FieldPattern` с token body. Участок между двумя найденными pattern становится
значением `ObjectFieldSetting`.

Каждый setting после linking знает:

- поле класса (`field`);
- вид setting; enum модели предусматривает TYPE, VALUE, OBJECT, OBJECT_SET и RAW,
  а текущие S1AP `WITH SYNTAX` templates создают TYPE либо VALUE;
- исходный текст;
- reference и ее target, если setting является именованной ссылкой;
- inline builtin type, если type setting записан непосредственно.

Enum literals вроде `reject` и `mandatory` разрешаются в конкретный
`EnumerationItem`, а не ошибочно ищутся среди module assignments.

### 7.3. Object sets

`ObjectSetExpression` сохраняет ordered elements:

- inline object;
- reference на named object или другой object set;
- extension marker.

Union через `|` сохраняет порядок входящих элементов. Метод `getAllObjects()`
раскрывает linked object/object-set references рекурсивно. Identity-set защищает
от циклов между sets.

### 7.4. Information-object field types и open types

Для типа

```asn1
value S1AP-ELEMENTARY-PROCEDURE.&InitiatingMessage
      ({S1AP-ELEMENTARY-PROCEDURES}{@procedureCode})
```

создается `InformationObjectFieldType`, содержащий:

- ссылку на object class;
- имя class field;
- прямой target `ObjectClassField`;
- table constraint;
- список вычисленных `OpenTypeAlternative`.

После интерпретации всех objects и object sets linker раскрывает конкретный set.
Для каждого object он берет:

- TYPE setting интересующего class field;
- setting selector field, если задан `@selector`;
- найденные targets обоих settings.

Результат — `OpenTypeAlternative` с selector value, selector assignment,
конкретным `TypeAssignment` и исходным `ObjectDefinition`.

Это позволяет пройти по S1AP без строковых lookup:

```text
InitiatingMessage.value
 └─ InformationObjectFieldType
     ├─ objectClass ─► S1AP-ELEMENTARY-PROCEDURE
     ├─ field ───────► &InitiatingMessage
     ├─ table set ───► S1AP-ELEMENTARY-PROCEDURES
     └─ alternatives
         └─ selector id-HandoverPreparation
             └─ typeTarget ─► HandoverRequired
```

Если table set является formal parameter, универсальное definition не получает
один искусственно выбранный concrete set. Concrete object set остается target
соответствующего actual parameter в месте применения.

## 8. Asn1Linker

### 8.1. Порядок проходов

`Asn1Linker.link()` выполняет операции в следующем порядке:

1. проверяет duplicates внутри каждого модуля;
2. разрешает source modules и symbols всех imports;
3. связывает formal governors, типы, constraints и значения всех declarations;
4. связывает class references objects и object sets;
5. интерпретирует defined syntax в `ObjectFieldSetting`;
6. вычисляет open-type alternatives;
7. по мере успешного linking регистрирует каждую `SymbolReference` в
   `ReferenceIndex`.

Критически важно, что шаги 1–2 происходят после parsing всех модулей, а
open-type alternatives строятся только после интерпретации всех object bodies.

### 8.2. Алгоритм разрешения имени

Для неквалифицированной reference используется порядок:

1. formal-parameter scope текущего parameterized assignment;
2. объявление с точным именем в текущем модуле;
3. символ с точным именем среди imports текущего модуля.

Для module-qualified reference lookup выполняется в указанном модуле.

Linker не использует неявный поиск по всем модулям. Межмодульный доступ должен
быть выражен qualifier или `IMPORTS`. Это не позволяет случайно связать ошибочное
имя с одноименным объявлением из постороннего модуля.

Имена case-sensitive. В S1AP одновременно существуют, например, type и object
names, различающиеся только регистром; нормализация через `toLowerCase()`
недопустима.

Найденное объявление проверяется на совместимость с ожидаемым `SymbolKind`.

### 8.3. Ошибки linking

Семантические проблемы добавляются в `Asn1Document.diagnostics`:

- duplicate assignment;
- отсутствующий imported module;
- отсутствующий imported symbol;
- unresolved или несовместимая reference;
- неверное число actual parameters;
- отсутствующее class field;
- ошибочный required fragment `WITH SYNTAX`;
- неизвестный sibling component в `@` relation.

Linker старается продолжить обход после такой ошибки, чтобы за один запуск
получить максимально полный список проблем.

## 9. Корневая модель и навигация

### 9.1. `Asn1Document`

Корневой объект хранит:

- `sourceName` и полный `sourceText`;
- ordered modules;
- module index;
- semantic diagnostics;
- reverse `ReferenceIndex`.

Полезные операции:

```java
document.requireModule("S1AP-IEs");
document.getAssignmentCount();
document.getErrorCount();
document.sourceText(node.getSourceRange());
```

### 9.2. `Asn1Module`

Модуль предоставляет как общий ordered list, так и typed views:

```java
module.getAssignments();
module.getTypeAssignments();
module.getValueAssignments();
module.getObjectClassAssignments();
module.getObjectAssignments();
module.getObjectSetAssignments();
```

Методы `find...` возвращают `Optional`, а `require...` бросают
`NoSuchElementException`. Для debugger и небольших исследовательских программ
обычно удобнее `requireModule`, `requireType`, `requireComponent`.

### 9.3. `ReferenceIndex`

При каждой успешной операции `SymbolReference.linkTo(target)` linker добавляет
reference в индекс по identity target:

```java
List<SymbolReference> usages =
        document.getReferenceIndex().getReferencesTo(typeAssignment);
```

Индекс содержит semantic `SymbolReference`. `ImportedSymbol` хранит собственный
target в `ModuleImport` и не подменяет собой usage reference в этом индексе.

### 9.4. Source ranges

`SourceRange` — half-open диапазон с source name, offsets и line/column. Узлы не
копируют весь исходный файл. Получение точного fragment выполняется через
`SourceRange.textFrom(sourceText)` или `Asn1Document.sourceText(range)`.

Такой подход дает точную диагностику, но не раздувает память повторными копиями
исходника на каждом узле.

## 10. Полный жизненный цикл на примере

Рассмотрим путь от S1AP PDU к набору IE.

### Шаг 1. Чтение

```java
Asn1Document document = Asn1.read(Path.of("s1ap.asn"));
```

Facade читает файл и запускает полный pipeline.

### Шаг 2. Обычная type reference

```java
Asn1Module ies = document.requireModule("S1AP-IEs");
TypeAssignment globalEnbId = ies.requireType("Global-ENB-ID");
ConstructedType sequence = (ConstructedType) globalEnbId.getType();
Component plmn = sequence.requireComponent("pLMNidentity");
TypeAssignment plmnTarget = ((ReferenceType) plmn.getType()).getTarget();
```

`plmnTarget` — тот же объект `TypeAssignment`, который находится в owning
module, а не копия его type.

### Шаг 3. Open type

```java
ConstructedType initiating = (ConstructedType) document
        .requireModule("S1AP-PDU-Descriptions")
        .requireType("InitiatingMessage")
        .getType();

InformationObjectFieldType valueType =
        (InformationObjectFieldType) initiating.requireComponent("value").getType();

OpenTypeAlternative handover = valueType.getAlternatives().stream()
        .filter(a -> a.typeTarget().getName().equals("HandoverRequired"))
        .findFirst()
        .orElseThrow();
```

Здесь `valueType` уже связан с class, class field, table set и selector component.

### Шаг 4. Parameterized container

```java
ConstructedType handoverType = (ConstructedType) handover.typeTarget().getType();
ReferenceType container = (ReferenceType) handoverType
        .requireComponent("protocolIEs")
        .getType();

ActualParameter argument = container.getActualParameters().get(0);
ObjectSetAssignment ieSet = (ObjectSetAssignment)
        argument.getReferences().get(0).getTarget();
```

`ieSet` непосредственно указывает на `HandoverRequiredIEs`.

## 11. Инварианты готового графа

После успешного `Asn1.parse/read` ожидаются следующие свойства:

- документ содержит все модули до физического EOF;
- порядок modules, assignments, components и set elements совпадает с исходником;
- module и symbol names сохраняют регистр;
- каждый resolved `SymbolReference` имеет non-null target совместимого вида;
- каждый resolved import знает source module и target assignment;
- каждый assignment знает owning module;
- actual parameter знает formal parameter;
- parsed object, class которого найден, содержит typed settings;
- table constraint с `@` знает sibling selector component;
- open type с concrete object set содержит вычисленные alternatives;
- semantic ошибки присутствуют в diagnostics, а не скрываются;
- коллекции, выдаваемые наружу, являются неизменяемыми views/copies.

Для `s1ap.asn` интеграционный тест подтверждает:

| Метрика | Значение |
|---|---:|
| Модули | 6 |
| Все assignments | 1322 |
| Type assignments | 594 |
| Value assignments | 388 |
| Object classes | 5 |
| Named objects | 63 |
| Object sets | 272 |
| Imported symbols | 686 |
| Named + inline object definitions | 694 |
| Semantic errors | 0 |

Текущее демонстрационное выполнение строит 4624 linked reference edges.

## 12. Ошибки и диагностика

Есть два разных механизма ошибок.

### Syntax/lexical errors

`Asn1ParseException` немедленно прерывает parsing и содержит:

- source name;
- offset;
- line;
- column;
- описание ожидаемого и найденного token.

Так обрабатываются незавершенные конструкции, неизвестная пунктуация и нарушения
поддержанной грамматики.

### Semantic errors

`Diagnostic` содержит severity, message и `SourceRange`. Linker добавляет такие
ошибки в документ и продолжает работу там, где это безопасно:

```java
for (Diagnostic diagnostic : document.getDiagnostics()) {
    System.err.println(diagnostic);
}
```

`document.getErrorCount()` считает diagnostics уровня `ERROR`.

## 13. Производительность и модель памяти

Для входа из `n` tokens основные проходы линейны относительно размера документа:

- lexer — `O(n)` по символам;
- parser — в основном `O(n)` по tokens;
- registration и обычное linking — близко к `O(n)` при lookup по maps;
- раскрытие object sets и open-type alternatives зависит от числа set edges и
  входящих objects.

Некоторые небольшие локальные операции, например поиск phrases `WITH SYNTAX`,
выполняют линейный поиск внутри отдельного object body. Bodies короткие, поэтому
это проще и надежнее преждевременной глобальной оптимизации.

Основные расходы памяти:

- исходная строка документа;
- token list на время parsing;
- AST/model nodes;
- direct и reverse reference edges.

Parser facade не сохраняет token list в итоговом `Asn1Document`. Повторно
используемые definitions не копируются.

## 14. Thread safety и мутабельность

Построение графа является однопоточным. Linker заполняет mutable link fields:

- `SymbolReference.target`;
- import targets;
- formal/actual bindings;
- object settings;
- selector targets;
- open-type alternatives;
- reverse index.

После возврата из `Asn1.read/parse` граф предполагается read-only по соглашению.
Большинство коллекций наружу отдаются как immutable copies или unmodifiable
views, но отдельные link methods публичны для поддержки custom linker. Одновременный
повторный linking и чтение из других потоков не поддерживаются.

Если документ нужно разделять между потоками, сначала полностью завершите
parsing/linking, затем публикуйте готовую ссылку на `Asn1Document`.

## 15. Ограничения текущей реализации

Parser ориентирован на schema constructs, реально используемые S1AP и близкими
3GPP-модулями. При расширении на произвольный ASN.1 следует отдельно проверить:

- сложные `EXPORTS` и их semantic rules;
- parameterized object classes и governors иных видов;
- module-qualified value и object references во всех контекстах;
- extension addition groups `[[ ... ]]`;
- полный constraint algebra: `EXCEPT`, `ALL EXCEPT`, `WITH COMPONENTS`,
  user-defined constraints;
- полный value notation для OID, SEQUENCE/SET/CHOICE values;
- object syntax без `WITH SYNTAX` и более сложные nested optional groups;
- object-set expressions с полным набором X.682 operators;
- REAL и character-string value syntaxes;
- сохранение comments/trivia как самостоятельного CST.

Неподдержанный синтаксис не должен молча исчезать: правильная стратегия — либо
добавить типизированный узел и parser rule, либо явно сохранить конструкцию через
`RawType`/`RawValue` с точным `SourceRange`.

## 16. Как расширять parser

### Добавление нового token

1. Добавить `TokenKind`.
2. Реализовать longest-match правило в `Lexer` до более коротких вариантов.
3. Добавить unit test с offsets и line/column.
4. Убедиться, что неизвестный символ по-прежнему вызывает exception.

### Добавление нового типа

1. Создать subclass `AsnType` с коротким нерекурсивным `toString()`.
2. Добавить ветку в `SyntaxParser.parseType()`.
3. Сохранить `SourceRange` и trailing constraints.
4. Добавить recursive traversal нового типа в `Asn1Linker.linkType()`.
5. Зарегистрировать все содержащиеся `SymbolReference` через общий linker.
6. Добавить compact unit test и реальный integration fragment.

### Добавление нового constraint

1. Добавить `Constraint.Kind` или отдельную typed-модель.
2. Расширить classification/building в `SyntaxParser`.
3. Не дублировать reference для одного lexical occurrence между parent и child.
4. Добавить связывание special targets в `Asn1Linker.linkConstraint()`.
5. Проверить `BigInteger`, extensibility и исходный текст.

### Добавление нового symbol namespace

1. Добавить `SymbolKind`.
2. Определить правила lookup и совместимости.
3. Обновить assignment/model node, создающий reference.
4. Обновить `Asn1Linker.resolve()` без глобального fallback.
5. Добавить positive и unresolved tests, включая конфликт одинаковых имен.

### Расширение information object syntax

1. Сначала сохранять token body losslessly на parser-этапе.
2. Интерпретировать его только после разрешения class.
3. Создавать `ObjectFieldSetting` с source range каждого значения.
4. Выполнять evaluation object sets только после linking всех member references.
5. Не клонировать recursive declarations для удобства evaluation.

## 17. Тестовая стратегия

### Компактный unit/integration test

`Asn1ParserTest` проверяет небольшой модуль:

- module header;
- INTEGER constraint;
- extensible ENUMERATED;
- SEQUENCE и OPTIONAL;
- extension marker;
- direct `ReferenceType.target`.

Этот тест локализует базовые regressions без разбора большого файла.

### Полный S1AP integration test

`S1apIntegrationTest` является acceptance test архитектуры. Он проверяет:

- точное число модулей и каждого вида assignment;
- все imports и отсутствие semantic errors;
- интерпретацию 694 object definitions;
- обычные type references;
- information-object field и рассчитанные alternatives;
- formal/actual binding parameterized container;
- переход actual argument к concrete object set;
- целое `18446744073709551615` без overflow;
- extensible constraint.

Команды проверки:

```powershell
mvn clean verify
mvn -Passembly -DskipTests package
java -jar target/asn1-resolver.jar s1ap.asn
```

Ожидаемый summary демонстрационной программы:

```text
Loaded 6 modules, 1322 assignments, 4624 linked references, 0 errors
```

## 18. Практика отладки в IntelliJ IDEA

Рекомендуемая точка входа — `com.ancevt.asn1.demo.DebugMain`.

Поставьте breakpoint после вычисления переменных примера и раскрывайте:

```text
document
 ├─ modules
 │   └─ assignments
 │       ├─ module
 │       └─ type
 │           ├─ components
 │           ├─ constraints
 │           └─ reference.target
 ├─ diagnostics
 └─ referenceIndex
```

Полезные выражения Evaluate Expression:

```java
document.requireModule("S1AP-IEs").requireType("Global-ENB-ID")
```

```java
((ConstructedType) document.requireModule("S1AP-IEs")
        .requireType("Global-ENB-ID").getType()).getComponents()
```

```java
document.getReferenceIndex().getReferencesTo(
        document.requireModule("S1AP-IEs").requireType("PLMNidentity"))
```

При просмотре циклического графа следует раскрывать конкретные ветви, а не
пытаться рекурсивно сериализовать весь `Asn1Document`.

## 19. Краткая карта исходников

| Задача | Основной файл |
|---|---|
| Публичное чтение/parse | `src/main/java/com/ancevt/asn1/Asn1.java` |
| Полный pipeline | `src/main/java/com/ancevt/asn1/parse/Asn1Parser.java` |
| Токенизация | `src/main/java/com/ancevt/asn1/parse/Lexer.java` |
| Синтаксический разбор | `src/main/java/com/ancevt/asn1/parse/SyntaxParser.java` |
| Семантическое linking | `src/main/java/com/ancevt/asn1/link/Asn1Linker.java` |
| Корень графа | `src/main/java/com/ancevt/asn1/model/Asn1Document.java` |
| Модули и symbols | `src/main/java/com/ancevt/asn1/model/Asn1Module.java` |
| References | `src/main/java/com/ancevt/asn1/model/SymbolReference.java` |
| Open types | `src/main/java/com/ancevt/asn1/model/InformationObjectFieldType.java` |
| Debugger-сценарий | `src/main/java/com/ancevt/asn1/demo/DebugMain.java` |
| Acceptance test | `src/test/java/com/ancevt/asn1/S1apIntegrationTest.java` |

Эта карта — рекомендуемая последовательность чтения кода: facade → pipeline →
model root → parser → linker → integration test.

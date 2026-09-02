# Техническое задание: генератор Java-классов S1AP Information Element

## 1. Статус и назначение документа

Этот документ является полной постановкой задачи для разработчика или следующего
чата, который будет реализовывать селективный генератор Java-классов S1AP IE на
основе `s1ap.asn` и объектной модели проекта `asn1resolver2`.

Требования сформулированы нормативно:

- **MUST / должен** — обязательное требование приемки;
- **SHOULD / следует** — требование, от которого можно отступить только с
  документированным обоснованием;
- **MAY / может** — необязательная возможность.

Генератор предназначен не для массового автоматического обновления всех S1AP
классов, а для рабочего процесса:

1. разработчик получает заказ со списком нескольких IE;
2. запускает генератор только для этого списка;
3. получает Java-файлы и текстовые вставки для фабрик/регистрации;
4. проверяет результат глазами;
5. при необходимости добавляет предметные удобства вручную;
6. переносит проверенный код в проект `naseps`.

## 2. Исходные проекты и нормативная база

### 2.1. Проект анализатора ASN.1

Репозиторий:

```text
C:\workspace\asn1resolver2
```

Основные точки входа:

```text
src/main/java/com/ancevt/asn1/Asn1.java
src/main/java/com/ancevt/asn1/model/**
src/main/java/com/ancevt/asn1/link/Asn1Linker.java
ARCHITECTURE.md
s1ap.asn
```

Публичный способ загрузки схемы:

```java
Asn1Document document = Asn1.read(pathToS1apAsn);
```

Генератор MUST использовать готовую связанную модель `Asn1Document`. Повторный
парсинг ASN.1 регулярными выражениями, собственным упрощенным parser или чтение
отдельных объявлений из исходного текста запрещены.

Текущий контрольный `s1ap.asn` успешно разбирается со следующими показателями:

```text
modules:                 6
assignments:          1322
type assignments:      594
value assignments:     388
object classes:           5
named objects:            63
object sets:             272
semantic errors:           0
linked references:      4624
```

Эти значения являются regression baseline только для текущего файла. Генератор
не должен зашивать их в рабочую логику.

### 2.2. Целевой проект и стиль API

Репозиторий:

```text
C:\workspace\naseps
```

Целевые пакеты и классы:

```text
tel.core.s1ap.spec.ie.*
tel.core.s1ap.spec.InformationElements
tel.core.s1ap.spec.MessageBuilder
tel.core.s1ap.spec.ProtocolIeId
tel.core.s1ap.core.model.InformationElement
tel.core.s1ap.core.model.InformationElementDecoderRegistry
tel.core.s1ap.core.asn.*
```

Существующие IE-классы являются эталоном общего вида API, но не побитовым
шаблоном. В них есть исторические различия в форматировании, именовании,
валидации и наборе convenience-методов. Генератор MUST использовать один
детерминированный canonical style, описанный ниже. Несовпадающие предметные API
должны задаваться overrides.

Версия Java:

- сам `asn1resolver2` сейчас собирается на Java 17;
- `naseps` собирается на Java 21;
- код генератора SHOULD оставаться совместимым с Java 17;
- сгенерированный код MUST компилироваться в `naseps` на Java 21.

### 2.3. Спецификации

Основная спецификация S1AP:

```text
3GPP TS 36.413 / ETSI TS 136 413, Release 15
```

Правила кодирования:

```text
ITU-T X.691, ASN.1 PER/APER
```

Генерация документации всегда должна быть привязана к точной версии, например
`15.03.00` или `15.11.00`, а не просто к строке `Release 15`.

Текущий локальный текст `C:\workspace\naseps\ts_36413_s1ap.txt` относится к
V15.3.0. Скриншоты целевого оформления используют V15.11.0:

```text
https://www.etsi.org/deliver/etsi_ts/136400_136499/136413/15.11.00_60/ts_136413v151100p.pdf
```

Генератор MUST запрещать неявное смешивание ASN.1 одной версии и индекса
документации другой версии. Точная версия и SHA-256 входного ASN.1 должны
попадать в отчет запуска.

## 3. Цель разработки

Создать CLI-утилиту, которая:

1. читает целиком указанный `s1ap.asn` через `Asn1.read(...)`;
2. строит каталог S1AP IE и Protocol Extension по information object sets;
3. принимает список IE по числовому ID или имени;
4. однозначно разрешает каждый selector в ID, ASN.1 type и Java name;
5. строит транзитивное замыкание необходимых типов;
6. генерирует Java-классы в стиле пакета `tel.core.s1ap.spec.ie`;
7. генерирует текст регистрации decoder;
8. генерирует текст фабричных методов `InformationElements`;
9. генерирует текст fluent-методов `MessageBuilder`;
10. опционально генерирует механическую Javadoc со ссылками на точные разделы
    3GPP/ETSI и применимые пункты X.691;
11. пишет подробный machine-readable и человекочитаемый отчет;
12. никогда не скрывает неоднозначность или неподдержанную ASN.1-конструкцию.

## 4. Не-цели первой версии

В первую версию не входят:

- автоматическая вставка методов внутрь существующих Java-файлов;
- перезапись существующих IE-классов без отдельного явного флага;
- генерация всего S1AP runtime, `Message`, `ProcedureCode` или всего DSL `S1ap`;
- генерация естественно-языковых смысловых описаний полей и getters;
- угадывание MCC/MNC, IP address, TEID, NAS message и другой предметной семантики
  по типу `OCTET STRING` или `BIT STRING`;
- универсальный ASN.1 compiler для произвольных схем вне поддержанного
  `asn1resolver2` подмножества;
- BER/DER/CER;
- загрузка PDF из сети при каждом запуске;
- молчаливое игнорирование extension additions или constraints.

## 5. Термины и модель предметной области

### 5.1. IE descriptor

`IeDescriptor` — нормализованное описание конкретного information object:

```text
numeric ID
ASN.1 id assignment name, например id-eNBname
object class kind: Value или Extension
target ASN.1 type либо inline type
criticality
presence
object set owners
message/procedure contexts
выбранное Java class name
выбранное Java factory method name
documentation references
```

### 5.2. Root IE class

Класс, непосредственно регистрируемый для конкретного ProtocolIE-ID и
возвращаемый фабрикой `InformationElements`.

### 5.3. Dependency class

Класс ASN.1-типа, необходимый root class, но сам не обязательно имеющий отдельный
ProtocolIE-ID. Например, `PlmnIdentity`, `Tac`, item-типы списков или типы полей
`SEQUENCE`.

### 5.4. Structural API и semantic API

Structural API выводится из ASN.1: поля, типы, constraints, enum constants,
optional flags, list bounds, encode/decode.

Semantic API задается человеком: MCC/MNC вместо `byte[3]`, строковый IP-адрес,
hex TEID, NAS object, удобные aliases, сокращенные class names и builders.

Генератор MUST уметь построить корректный structural API без semantic overrides.

## 6. Размещение и архитектура реализации

Рекомендуемое размещение в `asn1resolver2`:

```text
src/main/java/com/ancevt/asn1/generate/s1ap/
    S1apGeneratorMain.java
    cli/
    catalog/
    normalize/
    naming/
    render/
    docs/
    config/
    diagnostic/
```

Допускается отдельный Maven-модуль `s1ap-codegen`, зависящий от resolver. Если
используется один модуль, доменная S1AP-логика все равно должна находиться в
отдельном пакете и не должна проникать в `model`, `parse` или `link`.

Обязательный pipeline:

```text
s1ap.asn
  -> Asn1Document
  -> IeCatalog
  -> selector resolution
  -> normalized generator IR
  -> dependency plan
  -> Java source renderer
  -> snippet renderers
  -> documentation renderer
  -> validation and report
```

Java renderer запрещено вызывать прямо на сырых узлах AST. Между ASN.1 model и
шаблонами MUST существовать нормализованная generator IR. Это необходимо для
aliases, эффективных constraints, overrides и единообразной обработки APER.

## 7. CLI

### 7.1. Основная команда

Предлагаемый интерфейс:

```text
java -jar s1ap-ie-generator.jar generate \
  --asn <path-to-s1ap.asn> \
  --ie <selector> \
  [--ie <selector> ...] \
  --output <directory> \
  [--target-source-root <naseps/src/main/java>] \
  [--package tel.core.s1ap.spec.ie] \
  [--overrides <file>] \
  [--docs-index <file>] \
  [--docs off|best-effort|required] \
  [--extension-policy root-only|known-additions] \
  [--dependency-policy none|missing|closure] \
  [--conflict-policy fail|skip|overwrite] \
  [--report-format text|json|both]
```

На Windows команда может передаваться в одну строку; обратные слеши выше
показывают логическое продолжение.

### 7.2. Передача списка

MUST поддерживаться два способа:

```text
--ie 60 --ie id-Cause --ie ENBname
```

и

```text
--ie-list requested-ies.txt
```

Формат файла списка:

```text
# comments разрешены
60
id-Cause
ENBname
```

Пустые строки игнорируются. Дубликаты selectors дедуплицируются с сохранением
порядка первого появления.

### 7.3. Вспомогательные команды

Должны быть реализованы:

```text
list                 вывести каталог доступных IE
describe <selector>  показать полное разрешение без генерации
validate             разобрать ASN.1, config и docs index без генерации
generate             создать файлы и snippets
```

`list` MUST поддерживать фильтры `--kind value|extension|all` и
`--mapped-only`.

### 7.4. Exit codes

```text
0  успешное выполнение
2  ошибка аргументов CLI
3  ошибка чтения/парсинга ASN.1
4  semantic diagnostics ASN.1 уровня ERROR
5  неизвестный или неоднозначный selector
6  неподдержанная конструкция/codec capability
7  конфликт выходного файла
8  ошибка docs/override configuration
9  внутренняя ошибка генератора
```

## 8. Построение каталога IE

### 8.1. Источники

Каталог MUST строиться из связанных information objects классов:

```text
S1AP-PROTOCOL-IES
S1AP-PROTOCOL-EXTENSION
```

Следует обходить:

- `ObjectAssignment`;
- inline `ObjectDefinition` внутри `ObjectSetAssignment`;
- рекурсивно раскрываемые object sets через `getAllObjects()`;
- object-set references и unions.

### 8.2. Извлечение полей

Для `S1AP-PROTOCOL-IES` используются settings:

```text
&id
&criticality
&Value
&presence
```

Для `S1AP-PROTOCOL-EXTENSION`:

```text
&id
&criticality
&Extension
&presence
```

Имена settings должны разрешаться без учета ведущего `&`, но с учетом регистра
ASN.1 symbol names.

`&id` MUST разрешаться до целого через цепочку:

```text
ObjectFieldSetting
  -> ValueAssignment
  -> LiteralValue или ReferenceValue
  -> BigInteger
```

Значение должно помещаться в `0..65535`. Переполнение или цикл ссылок — ошибка.

`&Value`/`&Extension` может быть:

- ссылкой на `TypeAssignment`;
- inline builtin type, например `OCTET STRING`.

Оба случая обязательны к поддержке.

### 8.3. Дедупликация и конфликты

Один ID может встречаться во многих message object sets. Такие вхождения должны
объединяться в один descriptor при совпадении типа.

Если один ID связан с разными structural types, генератор MUST завершиться с
ошибкой `IE_ID_TYPE_CONFLICT` и показать все source ranges.

Если один ID встречается как `Value` и `Extension`, но тип одинаков, это не
считается конфликтом; оба usage context сохраняются.

Контрольное состояние текущего ASN.1:

```text
ProtocolIE-ID constants:                  279
ID с TYPE/EXTENSION mapping:              271
уникальных ID/type/field-kind mapping:    272
конфликтующих ID -> разные типы:            0
```

В текущей схеме восемь ID объявлены, но не входят в object sets:

```text
3    id-SourceID
38   id-E-RABReleaseItem
49   id-E-RABReleaseItemHOCmd
55   id-GERANtoLTEHOInformationRes
57   id-UTRANtoLTEHOInformationRes
63   id-ServedPLMNs
103  id-E-RABFailedToBeReleasedList
126  id-NAS-DownlinkCount
```

Выбор такого ID без override должен завершаться диагностикой
`ID_DECLARED_BUT_UNMAPPED`, а не эвристическим угадыванием типа по имени.

### 8.4. Usage contexts

Для каждого descriptor следует сохранять все object sets, в которых он
встречается. Через actual parameters и reverse reference index следует, где
возможно, связать object set с message type, например:

```text
id-eNBname
  -> S1SetupRequestIEs
  -> S1SetupRequest

id-eNBname
  -> ENBConfigurationUpdateIEs
  -> ENBConfigurationUpdate
```

Эта информация используется только для отчета и Javadoc. Она не должна менять
кодирование типа.

## 9. Разрешение selectors

### 9.1. Поддерживаемые формы

Selector может быть:

1. десятичным ProtocolIE-ID: `60`;
2. точным ASN.1 id assignment: `id-eNBname`;
3. точным ASN.1 type name: `ENBname`;
4. Java class name: `EnbName`;
5. Java factory method name: `enbName`, если он задан override и уникален.

### 9.2. Порядок разрешения

Порядок:

1. numeric ID;
2. точное case-sensitive имя id assignment;
3. точное case-sensitive имя ASN.1 type;
4. точный Java alias из overrides;
5. нормализованное имя, только если результат единственный.

ASN.1 type может соответствовать нескольким ID. Примеры текущей схемы:

```text
PagingDRX                -> 44, 137
MME-UE-S1AP-ID           -> 0, 88, 158
TransportLayerAddress    -> 131, 155, 184
```

В таком случае type selector MUST считаться неоднозначным. Генератор показывает
кандидатов и требует конкретный ID либо `--all-matches`. Молчаливый выбор первого
запрещен.

## 10. Generator IR и нормализация типов

### 10.1. Общие требования

IR должна быть immutable после построения. Рекомендуемая sealed-иерархия:

```text
GenType
  GenInteger
  GenEnumerated
  GenBitString
  GenOctetString
  GenPrintableString
  GenSequence
  GenChoice
  GenCollection
  GenOpenType
  GenNull
  GenUnsupported
```

Каждый `GenType` хранит:

```text
ASN.1 source name
Java name
source range
effective constraints
extensibility
root/extension partition
representation decision
dependencies
required runtime capabilities
override provenance
```

### 10.2. Aliases

Normalizer MUST проходить цепочки `ReferenceType.target`, не теряя имени
каждого alias. Цикл должен обнаруживаться identity-set.

Constraints на usage/reference и на target type должны объединяться по правилам
ASN.1 subtype constraints. Нельзя просто брать только constraints конечного
`dereference()`.

### 10.3. Symbolic bounds

Bounds вроде `maxnoofE-RABs` должны вычисляться через связанные
`ValueAssignment`, а не через поиск строки. Используется `BigInteger`.

Поддерживаются:

- single value;
- inclusive range;
- `SIZE`;
- union;
- extensible root constraint;
- symbolic lower/upper bounds.

Если constraint нельзя однозначно нормализовать, генерация соответствующего
типа запрещается с указанием source range и исходного текста constraint.

### 10.4. Контрольное множество типов

В текущем `s1ap.asn` после dereference встречаются:

```text
BuiltinType       87
CollectionType    88
ConstructedType  276
EnumeratedType   102
IntegerType       41
RawType            0
```

Из builtins фактически используются:

```text
OCTET STRING       56
BIT STRING         29
PrintableString     2
```

Constructed types:

```text
SEQUENCE 250
CHOICE    26
```

Генератор для контрольной схемы MUST проходить dry-run всех mapped IDs без
`UNHANDLED_ASN_TYPE`. Наличие `RawType`/`RawValue`, влияющего на выбранный тип,
должно приводить к fail-fast диагностике.

## 11. Правила Java-именования

### 11.1. Общие правила

Имена должны вычисляться одним компонентом `JavaNames`; renderer не должен
самостоятельно преобразовывать ASN.1 identifiers.

Canonical преобразования:

```text
MME        -> Mme
ENB/eNB    -> Enb
UE         -> Ue
S1AP       -> S1ap
E-RAB      -> ERab
GTP        -> Gtp
TEID       -> Teid
PLMN       -> Plmn
TAI        -> Tai
TAC        -> Tac
CGI        -> Cgi
CSG        -> Csg
NAS        -> Nas
RRC        -> Rrc
LTE        -> Lte
NR         -> Nr
QoS/QOS    -> Qos
DRX        -> Drx
ID         -> Id
```

Примеры:

```text
MME-UE-S1AP-ID                  -> MmeUeS1apId
ENBname                         -> EnbName
E-RABLevelQoSParameters         -> ERabLevelQosParameters
GTP-TEID                        -> GtpTeid
UEAggregateMaximumBitrate       -> UeAggregateMaximumBitrate
```

Словарь MUST быть конфигурируемым. Override имеет приоритет над алгоритмом.

### 11.2. Имя root class

Для descriptor с одним ID и одним уникальным ASN type имя по умолчанию берется
из ASN type.

Если один ASN type используется несколькими ID, root class name по умолчанию
берется из `id-*`, чтобы не потерять семантическую роль конкретного IE. Override
может явно указать повторное использование одного существующего класса.

Пример обязательного override текущего API:

```text
ID 137, ASN type PagingDRX -> DefaultPagingDrx
```

### 11.3. Factory method

По умолчанию lowerCamelCase от Java class name:

```text
EnbName -> enbName
GtpTeid -> gtpTeid
ERabSetupList -> eRabSetupList либо erabSetupList согласно override
```

Из-за исторических вариантов `eRab`/`erab` overrides для E-RAB методов
рекомендуются обязательными. Генератор не должен создавать alias-overloads без
явной настройки.

### 11.4. Коллизии

Проверяются коллизии:

- имен файлов без учета регистра;
- Java class names;
- factory methods с одинаковой erased signature;
- enum constants;
- Java keywords;
- Windows reserved file names.

Коллизия без override — ошибка.

## 12. Canonical API генерируемых классов

### 12.1. Общая форма

Каждый сгенерированный ASN.1 type class по умолчанию:

```java
public final class SomeIe implements InformationElement {
    private final ...;

    public SomeIe(BitInput in) {
        ...
    }

    public SomeIe(...) {
        ...
    }

    public ... get...() {
        ...
    }

    @Override
    public void encode(BitOutput out) {
        ...
    }

    @Override
    public String toString() {
        ...
    }
}
```

Обязательные свойства:

- class `public final`;
- implements `InformationElement`;
- все data fields `private final`;
- `BitInput` constructor public;
- `encode(BitOutput)` public;
- mutable arrays копируются на входе и выходе;
- lists сохраняются через `List.copyOf`;
- null elements в lists запрещаются;
- validation выполняется в value constructor;
- decode constructor проверяет невозможные/неподдержанные значения;
- исключения decode используют `S1apException` для protocol errors;
- `toString()` не печатает полный огромный byte array; разрешен hex summary или
  length;
- generated code не содержит timestamp, чтобы результат был детерминирован.

Где применимо, следует использовать:

```java
import static tel.core.Validator.validateInRange;
import static tel.core.Validator.validateNotNull;
```

Для `BigInteger` и сложных union constraints допускаются сгенерированные private
validation methods, поскольку существующий `Validator` ограничен `long`.

### 12.2. INTEGER

Representation выбирается по эффективному root range:

```text
помещается в signed int       -> int
не помещается в int,
но помещается в signed long   -> long
иначе                         -> BigInteger
```

Если override требует unsigned convenience representation, он может добавить
`long`, `byte[]` или factory, но canonical lossless representation остается
обязательной.

Генерируются:

- `MIN_VALUE`/`MAX_VALUE`, если это улучшает читаемость;
- range validation;
- aligned или unaligned PER вызов согласно X.691 и позиции типа;
- extension bit для extensible constraints;
- union validation для несмежных допустимых значений.

### 12.3. ENUMERATED

Внутри root class генерируется `public enum Value`:

```java
public enum Value {
    FIRST(0, false),
    SECOND(1, false),
    EXTENSION_VALUE(0, true);

    private final int index;
    private final boolean extensionAddition;
}
```

Число в Java enum — PER index внутри root либо extension partition, а не
обязательно ASN.1 explicit numeric value.

Обязательны:

- все root items в исходном порядке;
- все известные extension items в исходном порядке;
- детерминированное преобразование имени;
- `getCode()` для совместимости стиля, если representation не переопределен;
- `fromRootIndex` и `fromExtensionIndex` либо эквивалент;
- ошибка на неизвестном значении;
- поддержка known extension additions в policy `known-additions`;
- корректный normally-small non-negative whole number для extension index.

### 12.4. OCTET STRING

Canonical field:

```java
private final byte[] bytes;
```

Обязательны defensive copies, size validation и использование
`AsnOctetString`. Fixed-size значение не должно получать лишний length
determinant.

Предметные представления вроде PLMN, GTP-TEID или NAS не генерируются без
override/template.

### 12.5. BIT STRING

Canonical lossless representation:

```java
AsnBitString.Value
```

Допускаемые representation overrides:

```text
int bitmap   для <= 32 bits
long         для <= 64 bits
byte[]       для fixed octet-aligned sizes
custom       существующий класс/шаблон
```

Named bits должны быть доступны как enum/константы или методы проверки, если
они присутствуют в ASN.1. Variable и extensible sizes обязаны сохранять фактическую
bit length.

### 12.6. PrintableString

Canonical representation — `String`. Генератор обязан:

- проверять null;
- проверять SIZE root range;
- использовать `AsnPrintableString`;
- обрабатывать extensible SIZE согласно выбранной extension policy;
- не добавлять предметные getters.

### 12.7. SEQUENCE

Поля генерируются в ASN.1-порядке.

Перед содержимым APER должны корректно обрабатываться:

1. extension-present bit для extensible sequence;
2. optional/default presence bitmap root components;
3. root components;
4. known extension additions в `known-additions` policy;
5. `iE-Extensions` и другие open containers.

Root-only policy:

- encoder пишет отсутствие extension additions;
- decoder при наличии additions либо корректно пропускает open additions и
  затем бросает `S1apException`, либо немедленно бросает до изменения состояния;
- поведение должно быть единообразным и протестированным.

Representation optional fields:

- scalar/reference — nullable field;
- collection с root minimum >= 1 MAY использовать empty immutable list как
  `absent`, если это закреплено профилем;
- collection, допускающая size 0, должна сохранять отличие `absent` от
  `present empty` через nullable field или отдельный presence flag;
- `Optional<T>` в публичных fields/constructors по умолчанию не используется,
  чтобы соответствовать текущему стилю.

### 12.8. CHOICE

Генератор MUST сохранять root и extension alternatives и их PER indexes.

Canonical representation для неоднородного CHOICE:

- `public enum Choice`;
- одно nullable typed field на alternative;
- private canonical constructor;
- public static factory на каждую alternative;
- `getChoice()` и typed getters;
- decode constructor заполняет ровно одну alternative;
- `encode` проверяет invariant «ровно одна alternative».

Override может выбрать flattened API, например используемые сейчас
`EnbIdType + long` или `Cause.Group + int`.

### 12.9. SEQUENCE OF / SET OF

Canonical representation:

```java
private final List<ElementType> values;
```

Генерируются:

- constructor `List<ElementType>`;
- varargs constructor, если signature не конфликтует;
- immutable copy;
- size validation;
- ordered encode/decode;
- правильный constrained length determinant;
- alignment согласно APER.

### 12.10. Parameterized ProtocolIE containers и open types

Обязательна специальная стратегия для:

```asn1
ProtocolIE-Container
ProtocolIE-SingleContainer
ProtocolExtensionContainer
```

Actual parameter должен быть связан с конкретным object set. Для single-item
set генератор должен знать:

```text
item ID
criticality
item Java type
presence
```

Нельзя генерировать container как обычный `List<Object>` или терять open-type
selector.

Для list IE вроде `E-RABSetupListCtxtSURes` генерируется typed
`List<ERabSetupItem>`, а каждый элемент кодируется как protocol field с ID,
criticality и open type, аналогично существующим классам.

Глобальный decoder registry допустим, пока каталог подтверждает отсутствие
конфликтов `ID -> type`. Конфликт должен блокировать генерацию и требовать
context-aware registry design.

## 13. APER runtime capabilities

### 13.1. Общий принцип

Сгенерированный код не должен копировать сложные фрагменты X.691 десятками
разных вариантов. Общие алгоритмы должны находиться в `tel.core.s1ap.core.asn`.

Generator IR для каждого типа формирует набор `requiredCapabilities`. Перед
рендерингом выполняется capability check.

### 13.2. Необходимые расширения runtime

Для универсальной генерации текущего S1AP должны быть реализованы или подтверждены
тестами:

1. constrained whole number с `BigInteger` bounds/value;
2. normally-small non-negative whole number;
3. extensible `ENUMERATED` root/extension indexes;
4. extensible INTEGER/SIZE constraints;
5. APER length determinant и fragmentation;
6. fixed/variable/extensible BIT STRING;
7. fixed/variable/extensible OCTET STRING;
8. extension presence bitmap для SEQUENCE;
9. open type encoding для sequence/choice additions;
10. extension alternative encoding для CHOICE;
11. безопасное пропускание неизвестного open extension при выбранной policy;
12. alignment rules, покрытые внешними vectors.

Рекомендуемые новые helper-классы/методы:

```text
AsnInteger BigInteger overloads
AsnNormallySmallNumber
AsnEnumerated extensible helpers
AsnLengthDeterminant
AsnSequenceExtensions
AsnChoiceExtensions
```

Конкретные имена могут отличаться, но генератор не должен содержать собственную
дублирующую реализацию этих алгоритмов.

### 13.3. Extension policies

`root-only`:

- генерирует и принимает только root alternatives/components/constraint range;
- известные extension additions присутствуют в отчете;
- encoder всегда пишет отсутствие additions;
- decoder отклоняет их предсказуемой ошибкой;
- режим соответствует основной массе текущих ручных S1AP-классов.

`known-additions`:

- поддерживает все additions, объявленные в загруженном ASN.1;
- неизвестные будущие additions могут быть пропущены либо отклонены согласно
  runtime policy, но не интерпретируются как известные;
- этот режим является целевым для полной поддержки pinned Release 15 schema.

В обоих режимах запрещено молча кодировать extension value как root value.

## 14. Dependency planning

### 14.1. Политики

```text
none     генерировать только root class; missing dependency является ошибкой
missing  генерировать root и только отсутствующие dependency classes
closure  генерировать полное транзитивное замыкание независимо от target tree
```

Default: `missing`.

### 14.2. Определение существующих классов

Если передан `--target-source-root`, генератор строит индекс существующих Java
files. Соответствие ASN type -> existing Java class задается overrides либо
canonical naming.

Проверка только наличия файла недостаточна: report MUST показать, почему
dependency считается удовлетворенной.

Генератор не обязан разбирать произвольный Java AST существующих классов, но
может валидировать package/class declaration. Regex, подобный старому
`NasFabricMethodsGenerator`, не должен использоваться для сложного извлечения
конструкторов и generics.

### 14.3. Циклы

Dependency graph строится по identity `TypeAssignment`. Циклы не должны вызывать
рекурсивное копирование или stack overflow. Java-файлы можно рендерить независимо
от topological order, но отчет должен показывать цикл.

## 15. Генерация snippets

### 15.1. Регистрация decoder

Для каждого root Value IE:

```java
register(ProtocolIeId.ENB_NAME, EnbName::new);
```

Строка генерируется только если:

- ID однозначен;
- `ProtocolIeId` constant найден и его numeric value совпадает;
- root class имеет public constructor `(BitInput)`.

Если constant отсутствует, должен появиться отдельный diagnostic и опциональный
`ProtocolIeId.snippet.txt`; использование `UNKNOWN` запрещено.

### 15.2. InformationElements

Для каждого публичного value constructor, разрешенного API profile/override:

```java
public static EnbName enbName(String value) {
    return new EnbName(value);
}
```

`BitInput` constructor никогда не превращается в фабричный метод.

Semantic constructors генерируются только по override. Arrays/lists должны
сохранять defensive behavior конструктора.

### 15.3. MessageBuilder

Минимум два варианта, если signatures различаются:

```java
public MessageBuilder enbName(Criticality criticality, String value) {
    return addField(ProtocolIeId.ENB_NAME, criticality,
            InformationElements.enbName(value));
}

public MessageBuilder enbName(Criticality criticality, EnbName value) {
    return addField(ProtocolIeId.ENB_NAME, criticality, value);
}
```

Для extension-only descriptor `MessageBuilder` snippet по умолчанию не
генерируется, поскольку это не top-level message field. Причина фиксируется в
report.

### 15.4. Выходные snippet files

```text
snippets/decoder-registrations.txt
snippets/InformationElements.methods.txt
snippets/MessageBuilder.methods.txt
snippets/ProtocolIeId.constants.txt       # только при необходимости
```

Это должны быть фрагменты Java без автоматического изменения целевых файлов.

## 16. Overrides

### 16.1. Формат

Рекомендуется YAML либо JSON. Если добавление YAML dependency нежелательно,
обязателен эквивалентный JSON format.

Пример:

```yaml
schemaVersion: 1

acronyms:
  ENB: Enb
  MME: Mme
  TEID: Teid

types:
  PLMNidentity:
    javaClass: PlmnIdentity
    existing: true
    representation: custom
    template: plmn-identity

  GTP-TEID:
    javaClass: GtpTeid
    existing: true
    representation: custom

ies:
  "137":
    javaClass: DefaultPagingDrx
    factoryMethod: defaultPagingDrx

  "51":
    javaClass: ERabSetupList
    factoryMethod: erabSetupList
    protocolIeConstant: E_RAB_SETUP_LIST_CTXT_SU_RES

constructors:
  EnbName:
    expose:
      - parameters:
          - { type: String, name: value }

docs:
  "60":
    primary: s1-setup-request
```

### 16.2. Поддерживаемые override decisions

MUST поддерживаться как минимум:

- Java class name;
- factory method name;
- ProtocolIeId constant name;
- existing/generated/custom type;
- Java representation простого ASN.1 типа;
- reuse другого Java class;
- exposed constructors;
- field Java name;
- enum Java constant aliases;
- CHOICE representation strategy;
- optional collection absence strategy;
- docs primary/multiple references;
- suppression конкретного snippet;
- ручное ID -> ASN type mapping для declared-but-unmapped IDs.

Неизвестный key в config — ошибка, а не warning. Все примененные overrides
перечисляются в report с provenance.

## 17. Javadoc

### 17.1. Режимы

```text
off          Javadoc по спецификациям не генерируется
best-effort  генерируется только при уверенном mapping; пропуски -> warnings
required     отсутствие/неоднозначность mapping -> ошибка
```

Default: `off`, поскольку документация обозначена как опциональная.

### 17.2. Источник metadata

Генерация MUST использовать versioned sidecar index, а не искать раздел в PDF
на каждом запуске.

Пример `spec-index.yaml`:

```yaml
schemaVersion: 1
spec:
  organization: ETSI
  number: TS 136 413
  version: 15.11.0
  release: 15
  url: https://www.etsi.org/deliver/etsi_ts/136400_136499/136413/15.11.00_60/ts_136413v151100p.pdf
  asnSha256: "..."

sections:
  s1-setup-request:
    number: 9.1.8.4
    title: S1 SETUP REQUEST

  global-enb-id:
    number: 9.2.1.37
    title: Global eNB ID

types:
  Global-ENB-ID:
    refs: [global-enb-id]

ies:
  "60":
    refs: [s1-setup-request]
```

Отдельная offline-команда MAY строить черновик индекса из text extraction PDF,
но результат должен проверяться человеком и коммититься как обычный ресурс.

### 17.3. Причина sidecar index

Соответствие не всегда выводится однозначно:

- `ENBname` используется минимум в нескольких messages;
- некоторые inline IE не имеют собственного раздела 9.2;
- названия ASN.1, table rows и заголовков спецификации различаются;
- один type может обслуживать несколько IDs;
- номера пунктов могут меняться между V15.x.

При нескольких применимых sections generator либо выводит все ссылки, либо
использует `primary`, заданный override. Выбор «первого по файлу» запрещен.

### 17.4. Шаблон class Javadoc

```java
/**
 * eNodeB Name IE.
 *
 * <p>See <a href="https://.../ts_136413v151100p.pdf">
 * 9.1.8.4 S1 SETUP REQUEST</a>.
 */
public final class EnbName implements InformationElement {
```

Для primitive encoding MAY добавляться механическая ссылка X.691:

```java
 * <p>See <a href="https://www.itu.int/rec/T-REC-X.691-202102-I/en">
 * ASN.1 PER/APER encoding rules</a>, clause 30, encoding restricted
 * character types.
```

Mapping `ASN kind -> X.691 clause` должен быть отдельной versioned таблицей, а
не разбросанными строками renderer.

### 17.5. Что документировать

При включенной документации:

- class — MUST;
- constructor `(BitInput)` — MUST;
- public value constructors — SHOULD;
- `encode(BitOutput)` — MAY, если нужен единый стиль;
- `InformationElements` factory — MUST;
- `MessageBuilder` method — MUST;
- getters и semantic convenience methods — **не документируются генератором**.

Запрещено придумывать смысловое описание `getMmeCode()`, `getValue()`, MCC/MNC и
т. п. Если description отсутствует, используется только механическое имя IE.

### 17.6. Шаблон decode constructor

```java
/**
 * Decodes an eNodeB Name information element from the specified
 * {@link BitInput}.
 *
 * @param in source {@link BitInput}
 */
public EnbName(BitInput in) {
```

### 17.7. Шаблон фабрики

```java
/**
 * Creates an eNodeB Name information element.
 *
 * @param value eNodeB name
 * @return eNodeB Name information element
 * <p>See <a href="https://.../ts_136413v151100p.pdf">
 * 9.1.8.4 S1 SETUP REQUEST</a>.
 */
```

`@param` description для структурных полей может быть только механическим
преобразованием ASN.1 field name. Смысловой текст допускается лишь из override.

## 18. Выходные файлы и безопасность

### 18.1. Структура output

```text
<output>/
  sources/tel/core/s1ap/spec/ie/*.java
  snippets/*.txt
  reports/generation-report.txt
  reports/generation-report.json
```

### 18.2. Conflict policy

Default: `fail`.

```text
fail       любой существующий target file завершает запуск с кодом 7
skip       существующий file не меняется и попадает в report
overwrite  разрешен только при явном флаге; предыдущий hash пишется в report
```

Даже в `overwrite` generator не должен изменять файлы вне `--output`.

Имена файлов строятся только из проверенных Java identifiers. Path traversal,
абсолютные имена из config и `..` запрещены.

### 18.3. Encoding и line endings

- UTF-8 без BOM;
- LF line endings для детерминированности;
- финальный newline обязателен;
- сортировка imports детерминированная;
- порядок fields соответствует ASN.1;
- порядок root outputs соответствует запросу пользователя;
- dependency outputs сортируются по Java class name;
- timestamp в generated files отсутствует.

### 18.4. Generated marker

В начале Java-файла допускается комментарий:

```java
// Generated from S1AP ASN.1 type ENBname, ProtocolIE-ID 60.
// Review before adding to production sources.
```

`@Generated` не обязателен, поскольку итоговый файл предполагается ручным и
может быть отредактирован после генерации.

## 19. Отчет генерации

Для каждого selector report MUST показывать:

```text
original selector
resolution method
numeric ID
id assignment
object class field kind
ASN.1 root type
Java root class
ProtocolIeId constant
criticality/presence usages
message/object-set contexts
dependency classes: generated/existing/custom
extension policy and encountered additions
runtime capabilities
applied overrides
documentation sections
warnings
output file SHA-256
```

Общий report:

```text
ASN path and SHA-256
ASN source name/version metadata
module/assignment/error counts
generator version
config hashes
created/skipped/conflicting files
diagnostic summary
```

JSON schema report должен быть versioned полем `reportSchemaVersion`.

## 20. Диагностика

Каждая диагностика содержит:

```text
stable code
severity
message
selector, если применимо
ASN source range, если применимо
related source ranges
suggested action
```

Минимальный список stable codes:

```text
ASN_PARSE_ERROR
ASN_SEMANTIC_ERROR
UNKNOWN_SELECTOR
AMBIGUOUS_SELECTOR
ID_DECLARED_BUT_UNMAPPED
IE_ID_TYPE_CONFLICT
UNRESOLVED_TYPE_REFERENCE
UNSUPPORTED_ASN_TYPE
UNSUPPORTED_CONSTRAINT
UNSUPPORTED_EXTENSION_POLICY
MISSING_RUNTIME_CAPABILITY
JAVA_NAME_COLLISION
FACTORY_SIGNATURE_COLLISION
PROTOCOL_IE_CONSTANT_MISSING
PROTOCOL_IE_CONSTANT_VALUE_MISMATCH
DEPENDENCY_NOT_FOUND
OUTPUT_FILE_EXISTS
DOC_VERSION_MISMATCH
DOC_MAPPING_MISSING
DOC_MAPPING_AMBIGUOUS
INVALID_OVERRIDE
```

Generator MUST продолжать анализ независимых selectors, чтобы показать полный
список проблем, но не должен создавать частично успешный output без явного
`--allow-partial`. Default — atomic logical run: при ERROR Java/snippet files не
создаются; report создается.

## 21. Форматирование кода

Renderer MUST выдавать единый читаемый стиль:

- 4 spaces;
- braces как в существующем Java-коде;
- одна public top-level class на файл;
- строки желательно не длиннее 120 символов;
- многострочные constructors/methods форматируются одинаково;
- wildcard import `tel.core.s1ap.spec.ie.*` допустим только в snippets целевых
  facade classes, но не в IE source files;
- unused imports запрещены.

Использование внешнего formatter допускается, но генерация должна работать и
без установленной IDE. Formatter version должна быть pinned.

## 22. Тестовая стратегия

### 22.1. Unit tests

Обязательны tests для:

- selector parsing/resolution;
- recursive integer value evaluation;
- ID/type deduplication;
- конфликтующих mappings;
- alias constraint merging;
- range/size/union normalization;
- acronym/name conversion;
- Java keyword/collision handling;
- dependency closure and cycles;
- override validation and precedence;
- docs index version/hash validation;
- deterministic rendering;
- no-overwrite behavior.

### 22.2. Golden source tests

Golden tests должны покрыть минимум:

```text
MME-UE-S1AP-ID                 constrained INTEGER / long
ENBname                        PrintableString + extensible SIZE
TimeToWait                     ENUMERATED
ForbiddenInterRATs             extensible ENUMERATED dependency
GTP-TEID                       fixed OCTET STRING
Global-ENB-ID                  extensible SEQUENCE + CHOICE + BIT STRING
HandoverRestrictionList        optional fields + nested lists
E-RABSetupListCtxtSURes        SEQUENCE OF ProtocolIE-SingleContainer
S1-Message, ID 225             inline OCTET STRING type
E-RABUsageReportItem           unsigned 64-bit INTEGER dependency
```

Golden files проверяют source text, imports, public API, snippets и Javadoc.

### 22.3. Compilation tests

Сгенерированные sources MUST компилироваться против актуальных классов `naseps`.
Рекомендуется временный Maven test project либо `JavaCompiler` API.

Compilation test должен проверять одновременно:

- generated root classes;
- generated dependencies;
- `InformationElements` snippets после вставки в test facade;
- `MessageBuilder` snippets после вставки в test facade;
- decoder registration snippets.

### 22.4. Codec tests

Одного round-trip недостаточно, потому что симметричная ошибка encoder/decoder
может остаться незамеченной.

Для каждого семейства кодирования нужны:

1. value constructor -> encode -> decode -> equality/assertions;
2. decode известных bytes из внешнего источника;
3. сравнение encoded bytes с независимым APER implementation, Wireshark capture
   или утвержденным test vector;
4. boundary values min/max;
5. invalid values;
6. root/extension boundary;
7. alignment до и после значения;
8. open type/container vectors.

Версия схемы reference implementation должна совпадать с входным ASN.1.

### 22.5. Full-schema dry run

CI MUST выполнять dry-run для всех mapped IDs текущего `s1ap.asn`.

Условия:

- все selectors каталогизируются;
- нет необработанного model node;
- все имена валидны или требуют явного checked-in override;
- dependency graph строится без stack overflow;
- повторный запуск дает идентичные hashes;
- генерация выполняется за разумное время и не имеет квадратичного обхода всего
  документа на каждый selector.

Полная компиляция всех 271 root classes MAY быть отдельным более медленным CI
profile, но должна существовать до объявления универсальной поддержки.

### 22.6. Regression текущих проектов

После изменений должны проходить:

```text
cd C:\workspace\asn1resolver2
mvn clean test

cd C:\workspace\naseps
mvn test
```

Если полный `naseps` suite зависит от platform/native SCTP, допускается отдельно
выделенный S1AP unit profile, но причина skipped tests должна быть отражена.

## 23. Acceptance criteria

Работа считается принятой, если выполнены все пункты:

1. CLI загружает весь `s1ap.asn` через resolver и отказывается работать при
   semantic errors.
2. Список из mixed selectors ID/name корректно разрешается.
3. Неоднозначный type name не выбирается молча.
4. Для mapped ID однозначно определяется ASN type, включая inline type.
5. Для declared-but-unmapped ID выдается специальная ошибка либо применяется
   явный override.
6. Генерируются только заказанные root classes и dependencies согласно policy.
7. Existing target sources не перезаписываются по умолчанию.
8. Генерируемые classes имеют public `BitInput` constructor и `encode`.
9. Поддерживаются все пять фактически используемых семейств типов: INTEGER,
   ENUMERATED, builtin strings, SEQUENCE/CHOICE, SEQUENCE OF.
10. Parameterized single containers генерируются с правильными item ID,
    criticality и open type.
11. Enum root и known extension items сохраняют ASN.1 order/index.
12. Unsupported capability приводит к fail-fast, а не приблизительному коду.
13. Генерируемые sources проходят compilation tests против `naseps`.
14. Representative APER output совпадает с независимыми vectors.
15. Генерируются decoder, `InformationElements` и `MessageBuilder` snippets.
16. Javadoc можно полностью выключить.
17. В `required` docs mode каждый root class/factory/builder имеет проверенную
    versioned spec link.
18. Getters и semantic methods не получают автоматически выдуманную Javadoc.
19. Повторный запуск с одинаковыми inputs дает побайтно одинаковый output.
20. Создается полный text/JSON report с hashes и applied overrides.
21. Все исходные tests `asn1resolver2` и относящиеся к S1AP tests `naseps`
    остаются зелеными.

## 24. Рекомендуемые этапы реализации

### Этап 0. Фиксация baseline

- выбрать точную TS 36.413 V15.x для integration baseline;
- зафиксировать ASN SHA-256;
- решить, остается ли текущая V15.3.0 или схема обновляется до V15.11.0;
- подготовить пустой override и docs metadata formats;
- сохранить текущие parser tests.

### Этап 1. Catalog и CLI без генерации

- `list`, `describe`, `validate`;
- извлечение ID/TYPE/criticality/presence;
- selector resolution;
- text/JSON report;
- regression counts;
- diagnostics.

После этапа должно быть возможно выполнить:

```text
describe 60
```

и увидеть цепочку:

```text
60 -> id-eNBname -> ENBname -> EnbName
```

### Этап 2. IR и простые типы

- aliases и constraints;
- INTEGER;
- ENUMERATED root;
- OCTET STRING;
- BIT STRING;
- PrintableString;
- Java sources;
- compilation/golden tests.

### Этап 3. Constructed types

- SEQUENCE;
- CHOICE;
- SEQUENCE OF;
- optional/default;
- dependency closure;
- root-only extension policy.

### Этап 4. Information object containers и snippets

- ProtocolIE-SingleContainer;
- ProtocolExtensionContainer;
- typed item lists;
- ProtocolIeId validation;
- decoder registrations;
- `InformationElements` methods;
- `MessageBuilder` methods.

### Этап 5. Overrides и semantic templates

- full config validation;
- PLMN/GTP/Transport/NAS examples;
- existing-class reuse;
- naming exceptions;
- constructor exposure.

### Этап 6. Javadoc

- versioned docs index;
- section mapping;
- exact templates;
- X.691 clause mapping;
- off/best-effort/required modes;
- screenshot-equivalent golden output.

### Этап 7. Known additions и широкий APER

- runtime helper expansion;
- BigInteger constrained integers;
- extensible constraints;
- enum/choice/sequence known additions;
- independent APER vectors;
- full-schema generation profile.

## 25. Deliverables

Разработчик должен передать:

1. исходный код generator CLI;
2. immutable generator IR;
3. catalog/selector/naming/config/documentation components;
4. Java и snippet renderers;
5. необходимые APER runtime additions в `naseps` либо отдельный четко
   документированный prerequisite patch;
6. unit, golden, compilation и codec tests;
7. checked-in sample overrides;
8. checked-in spec metadata/docs index для выбранной V15.x;
9. JSON schema generation report;
10. README с примерами запуска;
11. пример результата минимум для `ENBname`, `Global-ENB-ID`,
    `E-RABSetupListCtxtSURes`;
12. список известных ограничений;
13. подтверждение test commands и их результатов.

## 26. Требования к README генератора

README MUST содержать:

- назначение и non-goals;
- prerequisites и Java version;
- точную команду сборки;
- примеры `list`, `describe`, `generate`;
- пример mixed ID/name list;
- объяснение dependency policies;
- объяснение extension policies;
- объяснение conflict policies;
- формат overrides;
- формат docs index;
- описание output tree;
- диагностику неоднозначного selector;
- правила ручной проверки сгенерированного кода;
- запрет считать internal round-trip достаточным доказательством APER correctness.

## 27. Обязательные инженерные принципы

1. **Fail closed.** Неизвестная конструкция блокирует соответствующий output.
2. **No guessing.** Нельзя угадывать ID/type/docs при неоднозначности.
3. **Determinism.** Одинаковые inputs дают одинаковые bytes.
4. **Traceability.** Любой Java field/code branch должен быть связан с ASN.1
   source range в IR/report.
5. **Human review first.** Output создается как черновик для проверки.
6. **No silent overwrite.** Пользовательские Java files защищены по умолчанию.
7. **Schema/version lock.** ASN, docs и vectors относятся к одной версии.
8. **Runtime reuse.** Общий APER алгоритм реализуется один раз в core codec.
9. **External vectors.** Correctness не доказывается только self-round-trip.
10. **Small generated surface.** Обычный заказ не должен порождать сотни
    ненужных files.

## 28. Открытое решение перед началом реализации

До реализации Javadoc и окончательных golden vectors необходимо выбрать точный
baseline:

```text
Вариант A: оставить текущий ASN.1 и ETSI TS 136 413 V15.3.0.
Вариант B: перейти на ETSI TS 136 413 V15.11.0 и взять ASN.1 из той же версии.
```

Рекомендуемый долгосрочный вариант — B, поскольку он соответствует показанным
ссылкам и более поздней версии Release 15. Однако нельзя просто переключить URL:
сначала нужно сравнить ASN.1 приложений, обновить baseline metrics/tests и
зафиксировать новый hash.

До принятия этого решения generator core, catalog, IR и renderers должны быть
version-agnostic; зависимыми от версии остаются только schema input, overrides,
docs index и reference vectors.

## 29. Definition of Done

Задача завершена не тогда, когда generator создает похожий Java-файл, а когда
другой разработчик может выполнить документированную команду со списком IE и
получить:

- компилируемые root/dependency classes;
- корректный APER для проверенных конструкций;
- точные decoder/factory/builder snippets;
- отсутствие неожиданных изменений существующего кода;
- понятные diagnostics для всего, что не может быть выведено автоматически;
- опциональную Javadoc с версионно согласованными ссылками;
- воспроизводимый отчет, позволяющий вручную проверить связь
  `selector -> ID -> ASN.1 type -> Java API -> generated files`.

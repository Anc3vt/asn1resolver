# Генератор S1AP Information Elements

CLI принимает конкретные IE по ID/имени и создаёт Java-классы, зависимости,
фрагменты регистрации decoder, фабрик `InformationElements` и методов
`MessageBuilder`. Результат предназначен для ручной проверки и переноса в
`naseps`. Существующие facade files автоматически не редактируются.

Генератор читает **весь** ASN.1 через `Asn1.read`, использует связанные
information objects и immutable IR. BER/DER, произвольный ASN.1 compiler,
автоматическое обновление production-классов и угадывание предметной семантики
в его задачи не входят.

## Сборка

Нужны Maven 3.9+ и **JDK 21+ для полного набора тестов**. Исходники генератора
компилируются с `--release 17`; полученный CLI работает на JDK 17+.
Генерируемый Java-код проверяется с `--release 21`.

```powershell
cd C:\workspace\asn1resolver2
mvn -Pgenerator clean package
java -jar target/s1ap-ie-generator.jar --help
```

Результат сборки: `target/s1ap-ie-generator.jar`, зависимости включены.
Обычный профиль resolver и его `-Passembly` сохранены.

Существующие файлы целевого проекта и библиотеку `asn` менять не нужно.
Генерируемые IE используют исходный API `BitInput`, `BitOutput`,
`AsnBitString`, `AsnOctetString` и `AsnOpenType`. Недостающие операции APER
включаются в каждый IE как `private static` методы, только по необходимости.
`AsnAper` и новые методы `AsnPrintableString` не требуются.
Для protocol containers тип поля объявляется внутри соответствующего IE
как публичный record `AperField`; отдельного runtime-класса нет.
Подробности проверки совместимости: [runtime/README.md](runtime/README.md).

## Быстрый рабочий сценарий

Посмотреть доступные IE и точное разрешение имени:

```powershell
java -jar target/s1ap-ie-generator.jar list --asn s1ap.asn --kind all --mapped-only
java -jar target/s1ap-ie-generator.jar describe 60 --asn s1ap.asn
java -jar target/s1ap-ie-generator.jar validate --asn s1ap.asn --overrides config/s1ap-overrides.json --docs-index config/spec-index-15.3.0.json
```

`describe 60` показывает `60 → id-eNBname → ENBname → EnbName`, а также
criticality/presence, места в ASN.1 и сообщения, использующие этот IE.

Сгенерировать три IE с полным набором проверяемых зависимостей:

```powershell
java -jar target/s1ap-ie-generator.jar generate --asn s1ap.asn --ie 60 --ie Global-ENB-ID --ie 51 --output out/ies --target-source-root C:/workspace/naseps/src/main/java --overrides config/s1ap-overrides.json --dependency-policy closure
```

Все приведённые CLI-команды записаны одной строкой и подходят для PowerShell.
Ключ `--ie` можно повторять; ID и разные формы имён разрешено смешивать:

```powershell
java -jar target/s1ap-ie-generator.jar generate --asn s1ap.asn --ie 60 --ie id-Cause --ie ENBname --output out/mixed
```

Либо создайте `requested-ies.txt`:

```text
# Заказ
60
Global-ENB-ID
51
```

```powershell
java -jar target/s1ap-ie-generator.jar generate --asn s1ap.asn --ie-list requested-ies.txt --output out/request --overrides config/s1ap-overrides.json
```

Пустые строки и строки, начинающиеся с `#`, пропускаются. Повторные selectors
и повторные ID удаляются с сохранением первого появления.

Готовый проверенный результат для `ENBname`, `Global-ENB-ID`,
`E-RABSetupListCtxtSURes` находится в [examples/basic](examples/basic).

## Как использовать результат

```text
out/ies/
  sources/tel/core/s1ap/spec/ie/*.java
  snippets/decoder-registrations.txt
  snippets/InformationElements.methods.txt
  snippets/MessageBuilder.methods.txt
  reports/generation-report.json
  reports/generation-report.txt
```

1. Откройте report: проверьте selector, ID, ASN type, Java class, зависимости,
   policies, overrides, ссылки и SHA-256 файлов.
2. Проверьте Java API. Он структурный: `byte[]` для OCTET STRING,
   `AsnBitString.Value` для BIT STRING, отдельные типы компонентов, typed lists.
   OPTIONAL по умолчанию представлен `null`. Domain getters автоматически
   не создаются.
3. Перенесите выбранные source files в `naseps/src/main/java` после сравнения
   с существующими классами и их callers. В частности, старый flattened
   `GlobalEnbId(mcc, mnc, ...)` отличается от нового структурного API.
4. Добавьте нужные строки регистрации в блок регистрации `InformationElements`,
   фабрики — в `InformationElements`, fluent methods — в `MessageBuilder`.
   Не вставляйте дубли существующих сигнатур. Сохраните импорты `ProtocolIeId`,
   `tel.core.s1ap.core.model.Criticality` и используемых IE.
5. Соберите `naseps`, запустите S1AP tests, проверьте нужные реальные пакеты.

Пример структурного API из `examples/basic`:

```java
EnbName name = new EnbName("Test eNB");
byte[] encoded = name.encode();
EnbName decoded = new EnbName(new BitInput(encoded));
System.out.println(decoded.getValue());

GlobalEnbId global = new GlobalEnbId(
        new PlmnIdentity(new byte[] {0x52, (byte) 0xf0, (byte) 0x99}),
        EnbId.macroEnbId(new EnbIdMacroEnbId(AsnBitString.Value.fromLong(0x12345, 20))),
        null);
```

Пример decoder registration:

```java
register(ProtocolIeId.ENB_NAME, EnbName::new);
```

Массивы копируются в конструкторе и getter; списки сохраняются через
`List.copyOf`. Ошибочные APER-данные в public decode constructor дают
`S1apException`. Большие INTEGER представлены `BigInteger`; extensible INTEGER
также расширяется до `BigInteger`, чтобы известные extension values оставались
без потерь.

## Политики

`--dependency-policy`:

| Значение | Поведение |
|---|---|
| `missing` (default) | Root classes и отсутствующие зависимости. Если target не указан, зависимости считаются отсутствующими. |
| `none` | Только roots; каждая зависимость должна иметь подходящий existing API в target. |
| `closure` | Всё транзитивное замыкание выбранных roots. Явный `existing/custom/reuse` override всё равно имеет приоритет. |

При reuse проверяется package/class, implements `InformationElement`, public
`(BitInput)` и `encode(BitOutput)` через Java parser из JDK. Report содержит
путь, hash и обнаруженные signatures. Это проверка API; она не доказывает
побитовую корректность вручную написанного existing codec. Зависимости
повторно используемого класса дополнительно не генерируются.

`--extension-policy`:

| Значение | Поведение |
|---|---|
| `root-only` (default) | Кодирует root range/alternatives; constructor и decoder отклоняют extension values. Известные additions видны в IR/report. |
| `known-additions` | Кодирует объявленные ENUMERATED/CHOICE/SEQUENCE additions и extensible constraints; неизвестные open additions отклоняет. |

`iE-Extensions` — самостоятельный root OPTIONAL container. Он сохраняет
типизированные ID/criticality/value mappings и не тождествен extension marker
самой SEQUENCE.

`--conflict-policy`:

| Значение | Поведение |
|---|---|
| `fail` (default) | Любой existing Java/snippet output блокирует запись всего запуска, exit 7. |
| `skip` | Existing output сохраняется, его фактический hash записывается в report. |
| `overwrite` | Явно разрешённая перезапись **только внутри `--output`**, с previous SHA-256 в report. |

`--target-source-root` всегда читается; CLI не изменяет его. Reports — журнал
текущего запуска, поэтому обновляются и при конфликте. Symbolic links и
redirected output paths отклоняются. Запись каждого файла выполняется через
temporary file и atomic move, где это поддерживает файловая система.

Без `--allow-partial` любая ERROR запрещает Java/snippet output: сначала
выполняется весь анализ и проверка конфликтов. Report записывается в любом
случае, если output доступен. `--allow-partial` разрешает результаты независимых
успешных selectors, но не превращает ошибку в exit 0. При аппаратной/I/O ошибке
во время записи транзакция всего каталога не гарантируется.

`--dry-run` выполняет каталогизацию, нормализацию, planning, rendering и
проверки без записи Java/snippet files. Reports сохраняются.

## Неоднозначные и отсутствующие selectors

Приоритет: numeric ID → точное ASN id assignment → точное ASN type → Java alias
→ уникальное нормализованное имя. Регистр точных ASN names сохраняется.

Например, `PagingDRX` относится к ID 44 и 137. Команда возвращает
`AMBIGUOUS_SELECTOR` и кандидатов. Укажите `--ie 137` либо `--all-matches`.
`MME-UE-S1AP-ID` аналогично относится к 0, 88, 158.

ID 3, 38, 49, 55, 57, 63, 103, 126 объявлены в текущем ASN, но не имеют mapping
из object sets. Для них возвращается `ID_DECLARED_BUT_UNMAPPED`; тип по похожему
имени не угадывается.

## Overrides

Поддерживается JSON, `schemaVersion: 1`. Неизвестные property keys и
небезопасные Java identifiers — ошибка. IE override имеет приоритет над type
override, затем используется canonical naming. Acronym dictionary можно
дополнять. `config/s1ap-overrides.json` — профиль для проверенного структурного
output; `config/naseps-existing-overrides.json` — пример reuse предметного API.

```json
{
  "schemaVersion": 1,
  "acronyms": { "ENB": "Enb", "TEID": "Teid" },
  "ies": {
    "137": { "javaClass": "DefaultPagingDrx", "factoryMethod": "defaultPagingDrx" },
    "3": { "asnType": "ENBname", "kind": "Value" }
  },
  "types": {
    "PLMNidentity": { "javaClass": "PlmnIdentity", "existing": true, "representation": "custom" },
    "HandoverRestrictionList": { "optionalCollection": "empty", "fields": { "servingPLMN": "servingPlmn" } }
  },
  "docs": { "60": { "primary": "s1-setup-request" } }
}
```

Mapping ID 3 в примере демонстрирует **формат**, а не предметно корректный выбор
типа. Настоящий `asnType`/`kind` для unmapped ID должен задать разработчик.

Доступные type/IE decisions:

- `javaClass`, `factoryMethod`, `protocolIeConstant`;
- `existing: true`, `reuse: "ClassName"`, `representation: "custom"`;
- widening INTEGER до `long`/`BigInteger`; BIT STRING → `int`/`long`/`byte[]`
  для fixed non-extensible lengths, помещающихся в representation;
- `fields: { "asn-name": "javaName" }`, `enumAliases: { "asn-item": "JAVA_ITEM" }`;
- `choiceStrategy: "structural"|"custom"`; custom требует existing/reuse;
- `optionalCollection: "nullable"|"empty"`; empty разрешён только для root
  OPTIONAL collections с минимумом ≥ 1, публичное поле становится immutable List;
- `suppressSnippets: ["decoder", "factory", "builder"]`; подавляя factory,
  нужно также подавить builder, вызывающий её;
- `asnType` для ручного mapping и явный `kind: "Value"|"Extension"`;
- semantic `template`: `plmn-identity`, `gtp-teid`, `transport-address`,
  `nas-pdu` обозначает reuse проверенного существующего класса, а не вставку
  произвольного Java из config.

`constructors.<JavaClass>.expose` задаёт разрешённые фабрики. Каждая запись:

```json
{ "parameters": [{ "type": "String", "name": "value" }] }
```

Для generated class сигнатура должна присутствовать в structural API. Для
reused class она проверяется по реальному Java AST. Конструктор с `BitInput`
никогда не становится factory. При несовпадении canonical constructor с
existing API требуется явный `constructors.expose`; пример для старого
`GlobalEnbId` включён в existing-profile.

## Javadoc и версия схемы

Зафиксирован текущий **ETSI TS 136 413 V15.3.0**, без замены ASN.1:

```text
SHA-256 e716350a28fe5d5a7bdfdbbc9c8c216924869ca41b489be3dc2ef88e072efabf
```

`--docs off` — default; spec Javadoc полностью выключена.
`best-effort` добавляет только найденные ссылки и предупреждает о пропусках.
`required` требует проверенные ссылки для каждого выбранного root и его
factory/builder. Getters не получают выдуманных описаний.

```powershell
java -jar target/s1ap-ie-generator.jar generate --asn s1ap.asn --ie 60 --ie 59 --ie 51 --output out/documented --overrides config/s1ap-overrides.json --dependency-policy closure --docs required --docs-index config/spec-index-15.3.0.json
```

Индекс содержит `spec` (точная версия, URL, ASN SHA-256), `sections`
(number/title), `types.<name>.refs`, `ies.<id>.refs`, отдельную versioned
таблицу `x691`. Все refs должны существовать. Несколько уверенных refs
выводятся вместе; `docs.<id>.primary` либо `refs` задаются явно в override.
Включённый индекс покрывает только проверенные sections примеров и
HandoverRestrictionList; для остальных IE его нужно дополнить по спецификации.

Новый ASN получает версию через `--asn-version X.Y.Z`, заданную после проверки
источника. При docs mode должны совпасть эта версия и точный hash индекса.
Просто заменить URL V15.3.0 на V15.11.0 недостаточно. PDF во время запуска CLI
из сети не загружается.

## Проверки и ограничения

Результаты выполненных команд: [VALIDATION.md](VALIDATION.md).

```powershell
mvn clean test
```

При другом расположении target:

```powershell
mvn test "-Dnaseps.sourceRoot=C:/other/naseps/src/main/java"
```

По умолчанию тесты используют snapshot **исходного, неизменённого** API
целевого проекта из `runtime/naseps-fixture`, без `AsnAper` и без патча.
CI явно проверяет его: full-schema generation/dry-run, compilation всех 271
root classes и их зависимостей, всех snippets, golden sources и внешних APER
vectors входят в обычный `mvn test`. В текущем baseline closure составляет
678 source files; обычный пример с тремя IE создаёт 15.

33 внешних S1AP vectors скомпилированы pycrate **из того же полного ASN** и
записаны в `src/test/resources/generator/aper-vectors.json`. Они покрывают
INTEGER, enum, строки, SEQUENCE/CHOICE, optional lists, protocol extensions,
single containers и unsigned 64-bit counters. Воспроизведение:

```powershell
python -m pip install pycrate==0.8.1
python tools/reference_vectors.py
```

**Внутренний round-trip не является доказательством APER correctness.** Полная
компиляция схемы также не означает проверку всех значений каждого её типа.
Boundary/fragmentation fixtures, второй кодек и обнаруженные расхождения
описаны в [runtime/README.md](runtime/README.md).

Fail-closed ограничения за пределами проверенного S1AP подмножества:

- `SET`/`SET OF`, `COMPONENTS OF`, RawType, неизвестные parameterized templates;
- сложные constraints (`EXCEPT`, `WITH COMPONENTS`, открытые MIN/MAX bounds),
  пересечение extensible alias constraints, явно ограниченные extension sets;
- DEFAULT поддержан для INTEGER; другие DEFAULT требуют расширения normalizer;
- ENUMERATED с numeric order, отличающимся от source order, требует отдельной
  стратегии переупорядочивания индексов и отклоняется;
- неизвестные будущие ENUMERATED/CHOICE/SEQUENCE/open-container additions;
- sequence extension bitmap fragmentation ≥ 16384 additions;
- semantic API генерируется через явно проверенный existing class;
  произвольные Java templates и автоматическое редактирование facades отсутствуют.

Такие случаи дают диагностику, не приблизительный код. Входной `s1ap.asn`
проходит полный mapped-ID dry-run без этих ошибок с checked-in overrides.

## Диагностика и отчёт

| Exit | Причина |
|---|---|
| 0 | Успех |
| 2 | CLI arguments/list file |
| 3 | Чтение или parsing ASN |
| 4 | Semantic ERROR из resolver |
| 5 | Unknown/ambiguous/unmapped selector, конфликт ID/type |
| 6 | Неподдержанный type/constraint/capability, отсутствующая dependency |
| 7 | Existing output |
| 8 | Override/docs configuration |
| 9 | I/O или внутренняя ошибка генератора |

Практические исправления: `AMBIGUOUS_SELECTOR` → выбрать ID;
`JAVA_NAME_COLLISION` → задать override; `DEPENDENCY_NOT_FOUND` → передать target
или выбрать `missing/closure`; `MISSING_RUNTIME_CAPABILITY` → запустить на JDK;
`DOC_VERSION_MISMATCH` → проверить ASN/version/hash;
`PROTOCOL_IE_CONSTANT_MISSING` → проверить явный override имени константы.

Если константы ID нет в `ProtocolIeId`, класс IE и фабрика создаются,
регистрация decoder использует числовой ID. Метод `MessageBuilder` для этого
IE пропускается с предупреждением `BUILDER_CONSTANT_UNAVAILABLE`: его
существующий `addField` принимает `ProtocolIeId`. Сам enum не меняется.
Это относится к ID 58, 146, 241, 242, 244, 247, 248, 250 в исходном snapshot.
Передавайте `--target-source-root` для проверки констант именно вашего проекта.

`--report-format text|json|both` управляет журналами. Text report — полный
pretty-printed report с заголовком. JSON schema:
[config/generation-report.schema.json](config/generation-report.schema.json).
Report содержит original selectors, resolution methods, usages/source ranges,
immutable IR, dependency evidence, applied overrides, policies, config/input
hashes, diagnostics, planned/created/skipped/conflicting outputs. Timestamps
в generated files отсутствуют; encoding UTF-8, LF и финальный newline.

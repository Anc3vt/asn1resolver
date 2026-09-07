# Проверка IE generator без runtime-патча

Проверено 2026-09-07 на Windows, Maven 3.9.3, JDK 24.0.2.
Generator компилируется с release 17; generated sources — с release 21.

| Команда / проверка | Результат |
|---|---|
| `mvn -Pgenerator package` | 25 tests, 0 failures/errors/skips; JAR собран |
| Compilation suite | 8 tests на исходном API без патча |
| Весь каталог | 279 declared IDs, 271 mapped IDs |
| Все mapped IDs, known-additions, closure | 271 roots, 678 generated sources |
| Компиляция sources и decoder/factory/builder snippets | Успешно с неизменёнными исходниками из `runtime/naseps-fixture` |
| Независимые S1AP vectors | 33 vectors из pycrate 0.8.1, все совпали |
| Golden | 10 representative sources плюс документированный пример и snippets |
| Полный повторный dry-run | SHA-256 всех выходных файлов совпадают |
| Fixture | 15 исходных файлов из git revision в metadata.json; SHA-256 проверены тестом |
| Зависимости | В сгенерированных sources и отчёте нет AsnAper; вызовы новых методов AsnPrintableString отсутствуют |
| Отсутствующие константы ProtocolIeId | Числовая регистрация decoder, фабрика создана, builder snippet пропущен с предупреждением |

В исходном ProtocolIeId нет констант для ID 58, 146, 241, 242, 244, 247, 248,
250. Полная генерация завершается успешно с восемью предупреждениями
`BUILDER_CONSTANT_UNAVAILABLE`; изменения enum не требуются. Генерация
примеров для ID 60, 59, 51 проходит без предупреждений.

Соседний целевой проект и библиотека asn в этой доработке не изменялись.
Новые private static APER-методы размещаются внутри каждого IE. Проверки
кодирования включают unsigned 64-bit counters, расширения ENUMERATED/CHOICE,
SEQUENCE extension additions, контейнеры, строки и списки.

Исходный ASN не изменён:
SHA-256 `e716350a28fe5d5a7bdfdbbc9c8c216924869ca41b489be3dc2ef88e072efabf`.

Это подтверждение перечисленных проверок, а не всех значений произвольного
ASN.1. Ограничения: [GENERATOR_README.md](GENERATOR_README.md),
[runtime/README.md](runtime/README.md).

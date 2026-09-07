# Проверка IE generator

Проверено 2026-09-07 на Windows, Maven 3.9.3, JDK 24.0.2.
Generator компилируется с release 17; generated sources — с release 21.

| Команда / проверка | Результат |
|---|---|
| `mvn clean test` | 24 tests, 0 failures/errors/skips |
| `mvn -Pgenerator package` | 24 tests, сборка `target/s1ap-ie-generator.jar` успешна |
| `mvn test` в `C:\workspace\naseps` | 166 tests, 0 failures/errors, 2 skipped |
| Compilation suite с `-Dnaseps.sourceRoot=runtime/naseps-fixture/src/main/java` | 7 tests, успешно |
| Весь каталог | 279 declared IDs, 271 mapped IDs, 272 ID/type/kind mappings |
| Все mapped IDs, known-additions, closure | 271 roots, 678 generated sources, 0 diagnostics |
| Компиляция всех sources и decoder/factory/builder snippets | Успешно против актуальных Java sources naseps |
| Независимые S1AP vectors | 33 vectors из pycrate 0.8.1, все совпали |
| Golden | 10 representative sources плюс полный документированный пример и snippets |
| Две отдельные JVM, одинаковые inputs | Все 20 файлов примера, включая оба reports, побайтно одинаковы |
| JSON schema / UTF-8 без BOM / LF / fixture hashes | Успешно |
| `git apply --reverse --check runtime/naseps-aper-v1.patch` в naseps | Patch точно соответствует внесённым изменениям |

Пропущены два теста `SgsapLocationUpdateAcceptResponderIntegrationTest`:
платформа/JDK не поддерживает SCTP. Эти же два теста пропускались до изменений.
Все S1AP tests выполнялись.

ASN не изменён: V15.3.0,
SHA-256 `e716350a28fe5d5a7bdfdbbc9c8c216924869ca41b489be3dc2ef88e072efabf`.
Изменения пользователя в `naseps/Validator.java` не редактировались.

Это подтверждение перечисленных проверок, а не доказательство всех значений
произвольного ASN.1. Известные ограничения и расхождения искусственных
пограничных fixtures: [GENERATOR_README.md](GENERATOR_README.md),
[runtime/README.md](runtime/README.md).

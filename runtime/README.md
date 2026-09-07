# APER prerequisite для naseps

Изменения уже внесены в соседний рабочий `C:\workspace\naseps`:

- `AsnAper`: общие алгоритмы aligned PER, capability `AsnAper-v1`;
- `AsnPrintableString.encodeAper/decodeAper`: новые явно настраиваемые методы;
- восемь отсутствовавших констант `ProtocolIeId`;
- `AsnAperTest`: дополнительные codec tests.

Старые codec entry points сохраняют своё поведение. Изменённый пользователем
`Validator.java` не входит в patch.

Для другого checkout сначала проверьте patch:

```powershell
git -C C:\workspace\naseps apply --check C:\workspace\asn1resolver2\runtime\naseps-aper-v1.patch
git -C C:\workspace\naseps apply C:\workspace\asn1resolver2\runtime\naseps-aper-v1.patch
mvn -f C:\workspace\naseps\pom.xml test
```

Если изменения уже применены, повторное применение не нужно. Проверка:

```powershell
git -C C:\workspace\naseps apply --reverse --check C:\workspace\asn1resolver2\runtime\naseps-aper-v1.patch
```

`naseps-fixture` содержит точную копию необходимых Java sources текущего target
с этим patch. Базовый commit и hashes каждого файла указаны в
`naseps-fixture/metadata.json`. Fixture нужен для переносимых CI compilation
tests; при наличии соседнего `naseps` локальные тесты используют его актуальные
sources. Это не замена полному target-проекту.

Обновление snapshot после отдельной проверки изменений runtime:

```powershell
python tools/snapshot_runtime.py C:\workspace\naseps
```

33 checked-in S1AP vectors получены через pycrate 0.8.1, скомпилировавший полный
`s1ap.asn` из этого репозитория. Дополнительные runtime vectors воспроизводятся
`tools/runtime_vectors.py`; для второго независимого кодека установите
`asn1tools==0.167.0` и передайте `--second`.

Зафиксированные расхождения внешних реализаций в искусственных fixtures:

- Для SEQUENCE extension bitmap pycrate 0.8.1 добавляет лишний octet в fixture
  `X`. Наш результат `d03801ff020112` совпадает с asn1tools 0.167.0 и X.691 19.8–19.9.
- Для PrintableString с `SIZE(0..1)` наш результат `d060` совпадает с asn1tools
  и X.691 30.5.7; pycrate дополнительно выравнивает поле.
- Для пустых variable BIT/OCTET STRING обе библиотеки добавляют alignment.
  Runtime следует буквальному X.691 11.9.3.3, note 2: поле нулевой длины не
  добавляет alignment. Эти три corner cases отдельно отражены в `AsnAperTest`;
  они не выдаются за совпавшие внешние S1AP vectors.

Нормативный источник: [ITU-T X.691 (02/2021)](https://www.itu.int/rec/T-REC-X.691-202102-I/en).
Исходники независимых кодеков: [pycrate](https://github.com/P1sec/pycrate),
[asn1tools](https://github.com/eerimoq/asn1tools).

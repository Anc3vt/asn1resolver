# Совместимость с исходной библиотекой asn

Генератор больше не требует `AsnAper`, изменений `AsnPrintableString`
или установки патча в целевой проект. Старый prerequisite patch удалён.

Недостающие алгоритмы APER находятся в шаблоне генератора
`src/main/resources/s1ap-generator/inline-aper.java.txt`. Генератор разбирает
его через javac и включает только используемые члены и их зависимости в
каждый новый IE-класс. Вспомогательные методы — `private static`.
Существующие `AsnOctetString` и `AsnOpenType` используются напрямую;
`BitInput`, `BitOutput`, `AsnBitString.Value`, `InformationElement` и
`S1apException` сохраняют исходный API.

`naseps-fixture` содержит 15 неизменённых исходных файлов из git revision,
указанного в `metadata.json`, включая исходные `AsnPrintableString` и
`ProtocolIeId`. `AsnAper` в fixture отсутствует. SHA-256 каждого файла
проверяется тестом. Соседний рабочий проект не используется по умолчанию.

```sh
mvn -Pgenerator clean verify
```

Компиляционные тесты создают все 678 IE-классов и компилируют их и snippets
с этим API. Классы проверяются внешними APER-векторами. Если в исходном enum
нет ID, decoder регистрируется по числу; builder snippet пропускается с
диагностикой, поскольку существующий addField принимает только ProtocolIeId.

Обновление fixture из **указанного git revision**, без чтения изменённых
рабочих файлов и без изменения целевого проекта:

```sh
python tools/snapshot_runtime.py /path/to/target <git-revision>
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
  Сгенерированный код следует буквальному X.691 11.9.3.3, note 2: поле нулевой длины не
  добавляет alignment. Эти три corner cases исследованы отдельными
  искусственными fixtures; они не выдаются за совпавшие внешние S1AP vectors.

Нормативный источник: [ITU-T X.691 (02/2021)](https://www.itu.int/rec/T-REC-X.691-202102-I/en).
Исходники независимых кодеков: [pycrate](https://github.com/P1sec/pycrate),
[asn1tools](https://github.com/eerimoq/asn1tools).

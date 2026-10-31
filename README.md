# stxt-pdf

**English** | [Español](README.es.md)

Layout engine and PDF generator for [STXT](https://stxt.dev) documents. The document describes what goes on each page and
how it looks. The engine works out where everything goes, splits the content into pages and writes the PDF.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Quarterly report
    Page:
        Paper: A4
        Margin: 50
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
    Section:
        Heading 1:
            Text: Quarterly report
        Paragraph: Sales grew by 12% compared with the previous quarter.
```

The PDF has:

- **Embedded fonts**, with the characters that are used.
- **Tagging** for accessibility: PDF/UA-1, or PDF/UA-2 on request.
- **Bookmarks**, one per heading.

**The engine is strict.** Each element accepts only the properties that make sense in it. No element has a look of its
own: a `Heading 1` is not larger than a `Paragraph` unless a style says so. Anything that would have no effect,
contradicts something else or does not fit is an error with its line.

## Documentation

| | Contents |
|---|---|
| [Examples](#examples) | Eleven real documents, with their PDF and their source |
| [Manual](docs/en/manual/README.md) | How to write a document, from the first one to each element. Knowing STXT is enough ([stxt.dev](https://stxt.dev)) |
| [Reference](docs/en/reference/README.md) | The exact rule for each behavior, the design decisions and the architecture of the code |

The documentation is written in Spanish first ([docs/es](docs/es)) and mirrored in English.

## Examples

| | Document | PDF | Source |
|---|---|---|---|
| [![](test/examples/01-invoice/page-1.png)](test/examples/01-invoice/output.pdf) | **Invoice**: table with a totals footer, cards with rounded corners, inline code | [PDF](test/examples/01-invoice/output.pdf) (1 page) | [STXT](test/examples/01-invoice/input.stxt) |
| [![](test/examples/02-letter/page-1.png)](test/examples/02-letter/output.pdf) | **Business letter**: letterhead, justified text with balanced line breaking, signature | [PDF](test/examples/02-letter/output.pdf) (1 page) | [STXT](test/examples/02-letter/input.stxt) |
| [![](test/examples/03-resume/page-1.png)](test/examples/03-resume/output.pdf) | **Résumé**: full-page grid, circular photo, skill bars | [PDF](test/examples/03-resume/output.pdf) (1 page) | [STXT](test/examples/03-resume/input.stxt) |
| [![](test/examples/04-annual-report/page-1.png)](test/examples/04-annual-report/output.pdf) | **Annual report**: full-bleed cover, numbered chapters, automatic table of contents, charts, two columns | [PDF](test/examples/04-annual-report/output.pdf) (7 pages) | [STXT](test/examples/04-annual-report/input.stxt) |
| [![](test/examples/05-newsletter/page-1.png)](test/examples/05-newsletter/output.pdf) | **Newsletter**: three balanced columns, cropped photos, events table | [PDF](test/examples/05-newsletter/output.pdf) (2 pages) | [STXT](test/examples/05-newsletter/input.stxt) |
| [![](test/examples/06-menu/page-1.png)](test/examples/06-menu/output.pdf) | **Restaurant menu**: custom page size, double frame behind the content | [PDF](test/examples/06-menu/output.pdf) (2 pages) | [STXT](test/examples/06-menu/input.stxt) |
| [![](test/examples/07-contract/page-1.png)](test/examples/07-contract/output.pdf) | **Contract**: clauses with three numbering levels, cross-references, signatures kept together | [PDF](test/examples/07-contract/output.pdf) (3 pages) | [STXT](test/examples/07-contract/input.stxt) |
| [![](test/examples/08-manual/page-1.png)](test/examples/08-manual/output.pdf) | **Manual**: figure with overlaid numbers, notices, preformatted terminal, "see page" references | [PDF](test/examples/08-manual/output.pdf) (4 pages) | [STXT](test/examples/08-manual/input.stxt) |
| [![](test/examples/09-catalog/page-1.png)](test/examples/09-catalog/output.pdf) | **Catalog**: product cards in a grid, diagonal label, product index | [PDF](test/examples/09-catalog/output.pdf) (5 pages) | [STXT](test/examples/09-catalog/input.stxt) |
| [![](test/examples/10-certificate/page-1.png)](test/examples/10-certificate/output.pdf) | **Certificate**: landscape A4, concentric frames, rotated seal, vertical text | [PDF](test/examples/10-certificate/output.pdf) (1 page) | [STXT](test/examples/10-certificate/input.stxt) |
| [![](test/examples/11-timetable/page-1.png)](test/examples/11-timetable/output.pdf) | **Timetable**: table with separated, rounded cells, colors by style | [PDF](test/examples/11-timetable/output.pdf) (1 page) | [STXT](test/examples/11-timetable/input.stxt) |

The documents are in Spanish. The style of each example is in its `styles.stxt`. What each one shows is in the
[index of the examples](test/examples/README.md).

## Usage

Java 17 or later.

```xml
<dependency>
    <groupId>dev.stxt</groupId>
    <artifactId>stxt-pdf</artifactId>
    <version>0.8.0</version>
</dependency>
```

```groovy
implementation 'dev.stxt:stxt-pdf:0.8.0'
```

```java
InlineNode doc = new DocumentReader().read(Path.of("report.stxt"));
try (OutputStream out = Files.newOutputStream(Path.of("report.pdf"))) {
    Converter.create().toPdf(doc, out);
}
```

Fonts and images are looked up in `resources/`. The repository includes IBM Plex Sans, Serif and Mono, under the SIL Open
Font License. Options, errors, limits and use from several threads are described in
[Using from Java](docs/en/manual/common/java-usage.md).

## Development

```bash
mvn test
```

How the engine is built: [architecture](docs/en/reference/architecture.md). How it is tested:
[testing](docs/en/reference/implementation/testing.md).

## License

[MIT](LICENSE).

# stxt-pdf

Motor de layout y generación de PDF a partir de documentos [STXT](https://stxt.dev). El documento describe qué hay en cada
página y cómo se ve. El motor calcula dónde va cada cosa, reparte el contenido en páginas y escribe el PDF.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Informe del trimestre
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
            Text: Informe del trimestre
        Paragraph: Las ventas han crecido un 12 % respecto al trimestre anterior.
```

El PDF lleva:

- **Las fuentes incrustadas**, con los caracteres que se usan.
- **Etiquetado** para la accesibilidad: PDF/UA-1, o PDF/UA-2 si se pide.
- **Marcadores**, uno por encabezado.

**El motor es estricto.** Cada elemento solo admite las propiedades que tienen sentido en él. Ningún elemento tiene un
aspecto propio: un `Heading 1` no es más grande que un `Paragraph` si un estilo no lo dice. Lo que no tendría efecto, se
contradice o no cabe es un error con su línea.

## Documentación

| | Qué hay |
|---|---|
| [Ejemplos](#ejemplos) | Once documentos reales, con su PDF y su fuente |
| [Manual](docs/manual/README.md) | Cómo se escribe un documento, del primero a cada elemento. Basta con saber STXT ([stxt.dev](https://stxt.dev)) |
| [Referencia](docs/referencia/README.md) | La regla exacta de cada comportamiento, las decisiones de diseño y la arquitectura del código |

## Ejemplos

| | Documento | PDF | Fuente |
|---|---|---|---|
| [![](test/examples/01-invoice/page-1.png)](test/examples/01-invoice/output.pdf) | **Factura**: tabla con pie de totales, tarjetas con esquinas redondeadas, código en línea | [PDF](test/examples/01-invoice/output.pdf) (1 página) | [STXT](test/examples/01-invoice/input.stxt) |
| [![](test/examples/02-letter/page-1.png)](test/examples/02-letter/output.pdf) | **Carta comercial**: membrete, texto justificado con reparto equilibrado, firma | [PDF](test/examples/02-letter/output.pdf) (1 página) | [STXT](test/examples/02-letter/input.stxt) |
| [![](test/examples/03-resume/page-1.png)](test/examples/03-resume/output.pdf) | **Currículum**: rejilla a página completa, foto circular, barras de nivel | [PDF](test/examples/03-resume/output.pdf) (1 página) | [STXT](test/examples/03-resume/input.stxt) |
| [![](test/examples/04-annual-report/page-1.png)](test/examples/04-annual-report/output.pdf) | **Informe anual**: portada a sangre, capítulos numerados, índice automático, gráficos, dos columnas | [PDF](test/examples/04-annual-report/output.pdf) (7 páginas) | [STXT](test/examples/04-annual-report/input.stxt) |
| [![](test/examples/05-newsletter/page-1.png)](test/examples/05-newsletter/output.pdf) | **Boletín**: tres columnas equilibradas, fotos recortadas, agenda en tabla | [PDF](test/examples/05-newsletter/output.pdf) (2 páginas) | [STXT](test/examples/05-newsletter/input.stxt) |
| [![](test/examples/06-menu/page-1.png)](test/examples/06-menu/output.pdf) | **Carta de restaurante**: tamaño de página propio, marco doble detrás del contenido | [PDF](test/examples/06-menu/output.pdf) (2 páginas) | [STXT](test/examples/06-menu/input.stxt) |
| [![](test/examples/07-contract/page-1.png)](test/examples/07-contract/output.pdf) | **Contrato**: cláusulas con tres niveles de numeración, referencias, firmas que no se parten | [PDF](test/examples/07-contract/output.pdf) (3 páginas) | [STXT](test/examples/07-contract/input.stxt) |
| [![](test/examples/08-manual/page-1.png)](test/examples/08-manual/output.pdf) | **Manual**: figura con números superpuestos, avisos, terminal preformateado, «véase la página» | [PDF](test/examples/08-manual/output.pdf) (4 páginas) | [STXT](test/examples/08-manual/input.stxt) |
| [![](test/examples/09-catalog/page-1.png)](test/examples/09-catalog/output.pdf) | **Catálogo**: fichas en rejilla, etiqueta en diagonal, índice de productos | [PDF](test/examples/09-catalog/output.pdf) (5 páginas) | [STXT](test/examples/09-catalog/input.stxt) |
| [![](test/examples/10-certificate/page-1.png)](test/examples/10-certificate/output.pdf) | **Certificado**: A4 apaisado, marcos concéntricos, sello girado, texto en vertical | [PDF](test/examples/10-certificate/output.pdf) (1 página) | [STXT](test/examples/10-certificate/input.stxt) |
| [![](test/examples/11-timetable/page-1.png)](test/examples/11-timetable/output.pdf) | **Horario**: tabla con celdas separadas y redondeadas, colores por estilo | [PDF](test/examples/11-timetable/output.pdf) (1 página) | [STXT](test/examples/11-timetable/input.stxt) |

El estilo de cada ejemplo está en su `styles.stxt`. Lo que enseña cada uno está en el [índice de los ejemplos](test/examples/README.md).

## Uso

Java 17 o superior y Maven.

```java
InlineNode doc = new DocumentReader().read(Path.of("informe.stxt"));
try (OutputStream out = Files.newOutputStream(Path.of("informe.pdf"))) {
    Converter.create().toPdf(doc, out);
}
```

Las fuentes y las imágenes se buscan en `resources/`. El repositorio incluye IBM Plex Sans, Serif y Mono, con licencia SIL
Open Font License. Las opciones, los errores, los límites y el uso con varios hilos están en
[Uso desde Java](docs/manual/comun/uso-desde-java.md).

## Desarrollo

```bash
mvn test
```

Cómo está hecho el motor: [arquitectura](docs/referencia/arquitectura.md). Cómo se prueba: [pruebas](docs/referencia/implementacion/pruebas.md).

## Licencia

[MIT](LICENSE).

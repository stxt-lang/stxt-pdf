# Uso desde Java

El motor es una librería Java 17 (Maven: `dev.stxt:stxt-pdf`).

## Convertir un documento

```java
InlineNode doc = new DocumentReader().read(Path.of("informe.stxt"));
try (OutputStream out = Files.newOutputStream(Path.of("informe.pdf"))) {
    Converter.create().toPdf(doc, out);
}
```

`DocumentReader` lee y valida el documento; `Converter` lo maqueta y escribe el PDF. Un `Converter` se crea una vez y se
puede usar desde varios hilos a la vez.

## Opciones

```java
Converter converter = Converter.builder()
        .resources(new Resources(Path.of("mis-recursos")))   // otra carpeta de recursos
        .styles(Path.of("estilos.stxt"))                      // un fichero de estilos aparte
        .limits(Limits.DEFAULT.withMaxPages(500))             // otros límites
        .build();
converter.toPdf(doc, out);
```

| Opción | Por defecto |
|---|---|
| `resources` | La carpeta `resources/` del directorio de trabajo ([Recursos](recursos.md)) |
| `styles` | Ninguno. El fichero de estilos aparte del documento ([Estilos](estilos.md#estilos-en-un-fichero-aparte)) |
| `limits` | 2000 páginas, 25 millones de píxeles por imagen, 20 000 elementos y 64 niveles por SVG |
| `debug` | `false`. Con `true`, pinta el contorno de cada caja |
| `checkContrast` | `true`: un texto que apenas se distingue de su fondo es un error ([Caja](caja.md#contraste)). Con `false`, no se comprueba |

## Errores

Todo error es un `StxtPdfException`, con tres datos: `kind()` (de quién es la culpa), `code()` (el código) y `line()` (la
línea, 0 si no es de una línea). Si la línea es del fichero de estilos aparte, `inStyles()` es `true`.

```java
try {
    converter.toPdf(doc, out);
} catch (StxtPdfException e) {
    switch (e.kind()) {
        case INVALID_DOCUMENT -> System.err.println(e.getMessage());   // el documento tiene un error
        case LIMIT -> System.err.println("Demasiado grande: " + e.getMessage());
        case RESOURCE, INTERNAL -> throw e;                            // la configuración o el motor
    }
}
```

## Más

- Para usar las páginas antes de pintarlas, o escribir también un SVG de cada página: `converter.layOut(doc)` y
  `converter.toSvg(…)`.
- En un servidor sin pantalla, arranca la JVM con `-Djava.awt.headless=true`.

El detalle (hilos, tipos de error, límites) está en la [referencia](../../referencia/implementacion/api.md).

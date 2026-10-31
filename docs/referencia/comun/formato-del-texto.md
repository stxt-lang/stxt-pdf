# Formato del texto

Lo que cambia cómo se ve el texto de una caja sin cambiar su caja: tramos con formato, formato en línea, mayúsculas,
versalitas, interletraje, ligaduras, texto preformateado y rayas. Todas son propiedades de
[`Typography`](tipografia.md): **se heredan** y valen en las reglas de `Styles`.

## Tramos

El texto puede tener partes con formato dentro de una misma línea: **negrita**, *cursiva*, subrayado y tachado,
combinables. Cada parte es un **tramo**. Se escriben con `Markup: markdown` (abajo) o los genera un `Markdown`.

- Cada tramo usa su variante de la fuente, que debe existir (`FONT_VARIANT_MISSING`, con la línea del tramo) y tener sus
  caracteres (`GLYPH_MISSING`). La negrita y la cursiva se **suman** a las de la caja: no se pueden quitar.
- El tamaño, el color, `Line height` y la familia son los de la caja (salvo el código, que usa `Code font`,
  [abajo](#código-en-línea)), así que las líneas no cambian de alto.
- Una palabra con partes de varios tramos (**neg**rita) es una sola palabra: no se parte entre ellos y mide lo que suman
  sus partes.
- El espacio entre dos palabras tiene el formato del tramo de su primer espacio en blanco: un subrayado cubre los espacios
  de dentro de su tramo, pero no el que hay entre dos tramos que no lo llevan. Con `justify`, el espacio extra es del tramo
  de su espacio.
- Un tramo puede llevar el destino de un enlace ([Enlaces](enlaces.md)), que no cambia cómo se mide ni cómo se ve.

> **Decisión:** un tramo es un conjunto de marcas y no un peso y un estilo propios: así `Styles` puede cambiar la fuente
> de la caja sin tocar los tramos. *Alternativa descartada:* tramos con `Bold` e `Italic` explícitos, que podrían deshacer
> el estilo de la caja.

## Formato en línea (`Markup`)

Por defecto el texto de un `Text`, un `Paragraph` o una `Cell` es **literal**: los asteriscos son asteriscos. Con
`Markup: markdown` se interpreta como el Markdown en línea de un [`Markdown`](../componentes/markdown.md), con la misma
sintaxis: `**negrita**`, `*cursiva*`, `++subrayado++`, `~~tachado~~`, `` `código` `` ([abajo](#código-en-línea)) y
`[texto](destino)` ([Enlaces](enlaces.md)).

- Las marcas no son texto: no se miden ni se pintan. Un carácter especial se escribe con una barra delante (`\*`, `\[`,
  `\\`).
- El salto de línea simple de `Content` vale un espacio.
- **Se hereda**: se declara una vez en `For element: Document` o en una regla, y un texto vuelve al literal con `Markup: none`.
- Solo interpreta el formato dentro de la línea. Lo que en un `Markdown` empezaría un bloque es texto:
  `- 5 % de descuento`, `2024. Un buen año`, `# 1 en ventas`, `> 30 años` o una línea `---` se pintan tal cual, sin
  escaparlos, y una lista o un encabezado se escriben en un `Markdown`. Dentro de la línea, una imagen, HTML o un salto
  de línea forzado son `MARKDOWN_UNSUPPORTED`, con la línea del párrafo, y un enlace con título o con un destino
  no admitido, lo mismo que en un `Markdown`.
- No actúa dentro de un `Markdown`, aunque se herede (lo escapado sigue siendo literal), ni en las entradas de un
  `Table of contents`. `Alt` y los demás valores son siempre literales.
- Las referencias (`{page}`, `{page of: id}`) funcionan igual.
- El texto de la caja es el texto sin marcas: es el que lee un lector de pantalla y el que copia un `Table of contents`. Un error
  del texto (un carácter que la fuente no tiene, una referencia desconocida) señala la línea donde empieza el texto.

> **Decisión:** el formato en línea se pide con una propiedad heredable: el texto es literal salvo que se pida, y se pide
> una vez. *Alternativa descartada:* hijos `Run` con su propio `Typography`, mucho más largos de escribir.

> **Decisión:** la sintaxis de los bloques es texto y no un error. Como `Markup` se hereda y se declara una vez para todo
> el documento, un error alcanzaría a cualquier texto, celda o encabezado que empiece por un número con punto, un guion
> o un `#`, y la lista de lo que hay que escapar es larga (`-`, `+`, `*`, `1.`, `1)`, `>`, `#`, una línea `---` o `===`,
> una lista que empieza a media frase). Quien escriba una lista en un texto con `Markup: markdown` la obtiene literal,
> como sin `Markup`. *Alternativa descartada:* `MARKDOWN_UNSUPPORTED` con la línea del bloque, que obligaba a escapar el
> primer carácter.

## Código en línea

Con `Markup: markdown`, o en un `Markdown`, lo que va entre comillas invertidas (`` `mvn test` ``) es **código**: un
tramo con la familia de `Code font`, que conserva el peso y el estilo de la caja y los de las marcas del tramo
(``**`x`**`` es código en negrita). El tamaño, el color y `Line height` son los de la caja, así que las líneas no
cambian de alto, y el espacio en blanco se colapsa como en el resto del texto.

`Code font` es de `Typography` ([Tipografía](tipografia.md)) y se hereda: se declara una vez, en `For element: Document`,
para todo el documento. Sin ella, el código es un error (`CODE_WITHOUT_FONT`), con la línea donde empieza el texto (en
un `Markdown`, la del párrafo). Si la familia no existe, es `FONT_UNKNOWN`, con la línea de `Code font`.

## Mayúsculas (`Case`)

`Case` cambia **lo que se pinta y no lo que se escribe**: un rótulo se escribe como una frase normal y una regla lo pone
en mayúsculas.

- Cambia lo que mide el texto (`ß` pasa a `SS`), así que se aplica antes de repartirlo en líneas.
- `uppercase` y `lowercase` siguen las reglas del `Language` de la caja (en turco, `i` pasa a `İ`). `capitalize` sube la
  primera letra de cada palabra (lo que sigue a un espacio o un salto de línea, también entre dos tramos) y deja el resto
  como está (`iPhone` da `IPhone`).
- `{page}` y `{pages}` no se transforman.
- Un lector de pantalla lee el texto escrito («Emisor» y no «EMISOR»), salvo en un texto con `{page}`, `{pages}` o una
  referencia, donde lee lo pintado.

## Versalitas (`Small caps`)

`Small caps` pinta cada minúscula, pasada a mayúscula, al 80 % del `Size`. Los espacios, las cifras, los signos y las
mayúsculas se quedan como están. Se aplica después de `Case`: con `lowercase` todo son versalitas, con `uppercase` ninguna y con
`capitalize` solo la inicial es grande. El `Size` de la caja no cambia, ni la altura de las líneas ni las rayas. Un lector
de pantalla lee el texto escrito, como con `Case`.

> **Decisión:** las versalitas son sintetizadas y no la característica `smcp` de la fuente, que no tiene ninguna de las
> incluidas. Con `Case: uppercase` no tienen efecto y no es un error: las dos se heredan, y es normal declararlas
> en sitios distintos.

## Interletraje (`Letter spacing`)

Puntos que se suman al avance de cada carácter, también el espacio, **menos el último de cada línea**, para que `right` y
`center` queden alineados con la última letra. Cuenta en el reparto en líneas, en los tamaños intrínsecos y en la
alineación, y `justify` reparte lo que sobra después. Puede ser negativo, y no se comprueba que el avance siga siendo
positivo. Un porcentaje es `VALUE_NOT_ALLOWED`.

El texto que se copia o se extrae del PDF es el de la línea, sin espacios entre las letras
([El PDF por dentro](../implementacion/pdf.md#texto-que-se-lee)).

## Ligaduras (`Ligatures`)

Por defecto cada carácter es un glifo: `fi` son dos. Con `Ligatures: true` se aplican las ligaduras de la fuente, que
juntan algunos pares en un glifo de otro ancho (en IBM Plex Sans y Serif, `fi`, `fl`, `ffi` y `ffl`, y ninguna en IBM Plex
Mono). Lo que se mide es lo que se pinta. Una ligadura no cruza un espacio ni el límite entre dos tramos. Con
`Letter spacing` distinto de 0 no hay ligaduras. No cambia el texto que se lee ni el que se copia.

> **Decisión:** `false` por defecto: sin ligaduras el texto se pinta mucho más rápido (de 17 a 30 veces). *Alternativa
> descartada:* ligaduras siempre, o nunca.

## Texto preformateado

Por defecto el espacio en blanco se colapsa. Con `Preformatted: true` o `wrap` el texto **conserva sus espacios y sus
saltos de línea**: cada línea del texto es una línea de la caja, también las vacías. Una línea en blanco **no** separa
párrafos: el texto entero es un párrafo para las viudas y huérfanas ([Cortes](cortes.md)), así que un bloque largo se
parte entre líneas. Actúa en un `Text`, un `Paragraph` y una `Cell` con texto. Lo normal es una fuente monoespaciada y un
`Content >>`, que conserva la sangría relativa (no los espacios al final de una línea ni las líneas en blanco del final).
Un salto de línea al final del texto no añade una línea vacía.

- **`true`** no parte las líneas: una más ancha que la caja es `TEXT_OVERFLOW`, con la línea del documento donde está.
- **`wrap`** parte las líneas demasiado anchas **entre palabras**, con el reparto voraz. La sangría se conserva y los
  espacios del corte se pierden. Una palabra más ancha que la caja sigue siendo un error.
- **Tabulaciones (`Tab size`).** Cada tabulación lleva a la siguiente columna múltiplo de `Tab size`, contando desde el
  principio de la línea (con `Tab size: 4`, `a⇥b` es `a   b`, y `abcd⇥e`, `abcd    e`). Un valor menor que 1 es
  `VALUE_NOT_ALLOWED`.
- **Alineación.** `justify` se trata como `left`, porque `Align` se hereda y un bloque de código heredaría el error.
  `right` y `center` alinean cada línea por separado.
- `Case`, `Letter spacing`, los tramos, `{page}` y las referencias funcionan como siempre. Con `Markup: markdown` el
  texto se interpreta primero, así que las líneas que se conservan son las de los párrafos.

### Tamaños intrínsecos

Con `Preformatted: true`, el ancho más estrecho y el natural son la línea más ancha. Con `wrap`, el ancho más estrecho es la
palabra más ancha y el natural, la línea más ancha.

> **Decisión:** el texto preformateado es una propiedad heredable y no un elemento propio, que no cabría en una
> `Cell` con valor.

## Subrayado y tachado

Una raya con el color del texto, a lo ancho del tramo (con sus espacios, pero no con el espacio entre dos tramos que no la
llevan). Su posición y su grosor son los que da la variante del tramo, así que la raya de la negrita es más gruesa. El
subrayado va debajo del texto y el tachado, encima. Un lector de pantalla no las anuncia.

## Lo que no hay

- Tramos con otro tamaño, otro color u otra familia (salvo el código), ni que quiten la negrita o la cursiva de la caja.
- Estilos de raya (ondulada, punteada), color propio de la raya ni subrayado que se interrumpe en los descendentes.
- `Case` a anchura completa, interletraje relativo al cuerpo, versalitas con `smcp` y más modos de `Preformatted`.

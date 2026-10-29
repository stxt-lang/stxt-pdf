# Formato del texto

Lo que cambia cómo se ve el texto de una caja sin cambiar su caja: tramos con formato, formato en línea, mayúsculas,
versalitas, interletraje, ligaduras, texto preformateado y rayas. Todas son propiedades de
[`Typography`](tipografia.md): **se heredan** y valen en las reglas de `Styles`.

## Tramos

El texto puede tener partes con formato dentro de una misma línea: **negrita**, *cursiva*, subrayado y tachado,
combinables. Cada parte es un **tramo**.

- Cada tramo mide y se pinta con su variante de la fuente; la negrita y la cursiva se **suman** a las de la caja (una
  palabra en negrita de un texto ya en negrita sigue en negrita).
- El tamaño, el color y `Line height` son los de la caja, así que la altura de las líneas y su línea base no cambian, y la
  familia también, salvo en el código de un `Markdown`, que usa la de `Code font`
  ([Markdown](../componentes/markdown.md#código)).
- Una palabra con partes de varios tramos (**neg**rita) es una sola palabra para el reparto, y mide lo que suman sus
  partes.
- El espacio entre dos palabras tiene el formato del tramo en que está su primer espacio en blanco. Con `justify`, el
  espacio extra de cada espacio pertenece a su tramo.
- Un texto sin formato es un único tramo y se mide y se pinta exactamente igual que una cadena.
- Un tramo puede llevar además el destino de un enlace ([Enlaces](enlaces.md)), que no cambia cómo se mide ni cómo se
  pinta.

Los tramos se escriben con `Inline: markdown` (abajo) o los genera el elemento `Markdown`. En el PDF etiquetado, los
tramos son contenido de su elemento (`P`, `Span`, `H1`…) sin elemento propio.

> **Decisión:** un tramo es un conjunto de marcas (negrita, cursiva, subrayado, tachado), no un peso y un estilo
> propios, así que no puede quitar la negrita de un texto en negrita; deja que `Styles` cambie la fuente de la caja sin
> tocar los tramos. Familia, tamaño, color y `Line height` son los de la caja. *Alternativa descartada:* tramos con
> `Weight` y `Style` explícitos, que podrían deshacer el estilo de la caja.

> **Decisión:** el espacio entre dos palabras tiene el formato de su primer espacio en blanco: un subrayado cubre los
> espacios de dentro de su tramo pero no el espacio entre dos tramos que no lo llevan, y el espacio que se pierde en el
> salto de línea no se pinta. *Alternativa descartada:* el formato del tramo de la palabra anterior, o el subrayado
> continuo entre tramos subrayados.

> **Decisión:** una palabra con partes de varios tramos es una sola palabra y mide la suma de sus partes, sin kerning
> entre ellas. *Alternativa descartada:* permitir el salto de línea entre dos tramos.

> **Decisión:** la variante de cada tramo debe existir: `FONT_VARIANT_MISSING` con la línea del tramo (la del carácter
> en que empieza), y los caracteres de cada tramo se comprueban en su variante (`GLYPH_MISSING`). *Alternativa
> descartada:* un código propio para la variante de un tramo.

## Formato en línea (`Inline`)

Por defecto el texto de un `Text`, un `Paragraph` o una `Cell` es **literal**: los asteriscos son asteriscos. Con
`Inline: markdown` se interpreta como el Markdown en línea de un [`Markdown`](../componentes/markdown.md), con la misma
sintaxis y el mismo subconjunto: `**negrita**`, `*cursiva*`, `++subrayado++`, `~~tachado~~` y `[texto](destino)`
([Enlaces](enlaces.md)), combinables.

- Las marcas no son texto: no se miden, no se pintan y no cuentan para el reparto en líneas. Un carácter especial se
  escribe con una barra delante (`\*`, `\_`, `\[`, `\\`).
- Como `Content` admite varios párrafos, cada uno separado por una línea en blanco es un párrafo; el salto de línea simple
  vale un espacio, igual que sin `Inline`.
- **Se hereda**, así que se declara una vez en `Element: Document` o en una regla de `Styles` (`Element: Paragraph`), y un
  texto concreto vuelve al literal con `Inline: none`.
- Solo admite lo que cabe en una línea: un encabezado (`#`), una lista (`-`, `1.`), una tabla, una cita, código, una
  imagen o HTML dentro de un texto con `Inline: markdown` es `MARKDOWN_UNSUPPORTED`, con la línea del párrafo, como un
  enlace con título o con un destino no admitido (`LINK_SCHEME`); esas cosas van en un `Markdown`. Si el texto va a
  empezar por `-`, `#`, `>` o `1.`, se escapa el primer carácter.
- Dentro de un `Markdown` la propiedad no actúa: sus textos ya vienen interpretados, y lo escapado sigue siendo literal.
- Las referencias (`{page}`, `{page of: id}`) funcionan igual con y sin ella.
- El texto que ve el resto del motor (el PDF etiquetado, el índice de un `Contents`, los mensajes) es el texto sin las
  marcas, y un error del texto (un carácter que la fuente no tiene, una referencia desconocida) señala la línea donde
  empieza el texto.

Actúa en `Text`, `Paragraph` (también como hijo de un encabezado) y `Cell` con valor; `Alt` y los demás valores son
siempre literales.

> **Decisión:** el formato en línea se pide con `Inline: markdown`, una propiedad de `Typography` que se hereda: el texto
> es literal salvo que se pida, así que los documentos que ya existían no cambian. *Alternativa descartada:* hijos `Run`
> con su propio `Typography` (más STXT y mucho más largo), o una propiedad solo del elemento, que habría que repetir en
> cada texto.

> **Decisión:** la sintaxis y el subconjunto son los de `Markdown`, con el mismo parser y la misma traducción, así que un
> enlace con título o con un destino mal escrito da los mismos errores. *Alternativa descartada:* un parser en línea
> propio.

> **Decisión:** solo párrafos: un texto con `Inline: markdown` que empieza como un encabezado, una lista, una cita, código
> o una tabla es un error con la línea del párrafo, y hay que escapar el primer carácter si es literal. *Alternativa
> descartada:* tratar lo que no es un párrafo como texto literal, en silencio.

> **Decisión:** con `Inline: markdown` el texto de la caja es el texto sin marcas y deja de corresponderse línea a línea
> con el del documento, así que los errores del texto señalan la línea donde empieza. *Alternativa descartada:* llevar la
> correspondencia de líneas de cada párrafo, más precisa pero con mucho más código para un caso poco frecuente.

> **Decisión:** `Inline` no actúa dentro de un `Markdown`, aunque lo herede de `Document`: lo escapado en el Markdown debe
> seguir siendo literal. Tampoco en las entradas de un `Contents`. *Alternativa descartada:* un error por declararla
> dentro de un `Markdown`, que sería habitual porque se hereda.

## Mayúsculas (`Transform`)

`Transform` cambia **lo que se pinta, no lo que se escribe**, así que un rótulo se escribe como una frase normal y una
regla lo pone en mayúsculas.

- Se aplica a los tramos **antes** de repartir el texto en líneas, porque cambia lo que miden (`ß` pasa a `SS`); las líneas
  contienen el texto ya transformado y el texto de la caja no cambia.
- `uppercase` y `lowercase` siguen las reglas de mayúsculas de Java con la configuración regional del `Language` de la
  caja (en turco, `i` pasa a `İ`). `capitalize` solo cambia la primera letra de cada palabra (palabra: lo que sigue a un
  espacio o un salto de línea, también entre dos tramos) y deja el resto como está (`iPhone` da `IPhone`).
- Los marcadores `{page}` y `{pages}` no se transforman.
- En el PDF etiquetado, el texto que se lee (`ActualText`) es el escrito, salvo en un texto con `{page}`, `{pages}` o una
  referencia, que cambia con la página: ahí se lee lo pintado.

> **Decisión:** `Transform` se aplica a los tramos antes de repartir en líneas, porque cambia lo que mide el texto; el
> texto escrito se conserva para el PDF etiquetado. *Alternativa descartada:* transformar al construir el árbol, que
> perdería el texto escrito.

> **Decisión:** `capitalize` solo sube la primera letra de cada palabra y deja el resto como está. *Alternativa
> descartada:* pasar el resto de la palabra a minúscula.

> **Decisión:** un elemento con `Transform` o `Small caps` lleva `ActualText` con el texto escrito, para que un lector de
> pantalla lea «Emisor» y no «EMISOR», salvo si su texto contiene `{page}`, `{pages}` o una referencia. *Alternativa
> descartada:* dar el texto sustituido de cada fragmento, que no encaja en un elemento partido entre páginas.

## Versalitas (`Small caps`)

`Small caps` pinta cada minúscula, ya pasada a mayúscula, al 80 % del `Size`; los espacios, las cifras, los signos y las
mayúsculas, no. Se aplica después de `Transform`: con `lowercase` todo son versalitas, con `uppercase` ninguna y con
`capitalize` solo la inicial es grande. No cambia el `Size` de la caja, así que la altura de las líneas y la línea base
son las de siempre, y el subrayado y el tachado, los del tamaño de la caja.

> **Decisión:** las versalitas son sintetizadas: mayúsculas al 80 % del cuerpo, no la característica `smcp` de la fuente
> (ninguna de las incluidas la tiene), y no hay error si la fuente no la tiene. *Alternativa descartada:* usar `smcp` si
> la fuente la tiene.

> **Decisión:** `Small caps` con `Transform: uppercase` no es un error aunque no tenga efecto: son propiedades
> heredables y un documento puede declarar `Small caps` en `Document` y `uppercase` en un solo rótulo. *Alternativa
> descartada:* el error de lo que no tiene efecto, que obligaría a repetir las reglas.

## Interletraje (`Letter spacing`)

Una medida en puntos que se suma al avance de cada carácter, también el espacio. Cuenta en el reparto en líneas (una
palabra mide más, también con `Text wrap: pretty`), en los tamaños intrínsecos, en la alineación y en `justify`, que
reparte lo que sobra después. **El último carácter de cada línea no lo lleva**: el ancho de una línea es lo que suman sus
caracteres menos un interletraje, para que `right` y `center` queden alineados con la última letra. Puede ser negativo
(texto más apretado). Un porcentaje o un texto es `VALUE_NOT_ALLOWED`.

En el PDF el texto sigue siendo seleccionable, aunque un lector puede interpretar una separación grande como un espacio.

> **Decisión:** el interletraje lo lleva cada carácter menos el último de la línea, así que el texto alineado a la
> derecha acaba en la última letra y no en un hueco; puede ser negativo y no se comprueba que el avance siga siendo
> positivo. *Alternativa descartada:* ponerlo también tras el último carácter, o rechazar los valores negativos.

> **Decisión:** `Letter spacing` solo en puntos. *Alternativa descartada:* una fracción del `Size`.

## Ligaduras (`Ligatures`)

Por defecto el texto se pinta con **un glifo por carácter**: `fi` son dos glifos, la `f` y la `i`. Con `Ligatures: true` se
aplica la tabla de sustitución de glifos (`GSUB`) de la fuente, que junta algunos pares en un glifo de otro ancho (en IBM
Plex Sans y Serif, `fi`, `fl`, `ffi`, `ffl`; IBM Plex Mono no tiene ninguna). No cambia lo que se escribe: la estructura
del PDF etiquetado y el texto que se copia son los mismos.

**Lo que se mide es lo que se pinta** en los dos casos: con la opción activa, cada palabra (lo que hay entre dos espacios
en blanco) se mide con los glifos que quedan tras aplicar `GSUB`, que son los que se escriben en el PDF; una ligadura no
cruza un espacio ni el límite entre dos tramos con formato distinto. Con `Letter spacing` distinto de 0 no hay ligaduras.
El SVG de depuración no distingue las dos formas.

> **Decisión:** `Ligatures` en `Typography`, heredable, `false` por defecto: sin ligaduras el texto se pinta entre 17 y 30
> veces más rápido, y con ellas se mide con los mismos glifos que se escriben. *Alternativa descartada:* medir siempre con
> ligaduras, o quitarlas siempre sin opción.

> **Decisión:** con `Letter spacing` distinto de 0 no hay ligaduras aunque `Ligatures` sea `true`: el interletraje separa
> cada carácter. *Alternativa descartada:* medir con ligaduras y pintar sin ellas, que descuadra.

> **Decisión:** las ligaduras se aplican palabra a palabra, con el mismo procesador de `GSUB` que usa PDFBox al pintar
> (`ccmp`, `liga` y `clig` para escritura latina), así que una ligadura no cruza un espacio ni un cambio de tramo.
> *Alternativa descartada:* leer solo la característica `liga` de la tabla, con otro algoritmo que el del PDF.

> **Decisión:** el SVG de depuración no distingue `Ligatures`: el navegador aplica las ligaduras como quiera, y el PDF es
> la salida de verdad.

## Texto preformateado (`White space`)

Por defecto el espacio en blanco se colapsa. Con `White space: pre` o `pre-wrap` el texto **conserva sus espacios y sus
saltos de línea**: cada línea del texto es una línea de la caja, también las vacías (una línea en blanco **no** separa
párrafos), y el texto entero cuenta como un solo párrafo para las viudas y huérfanas ([Cortes](cortes.md)), así que un
bloque largo se parte entre líneas como cualquier texto. Actúa en un `Text`, un `Paragraph` y una `Cell` con texto; lo
normal es dar al bloque una fuente monoespaciada y escribirlo en un `Content >>`, que conserva la sangría relativa de sus
líneas (no los espacios al final de una línea ni las líneas en blanco al final del bloque, que el lector de STXT quita).
Un salto de línea al final del texto no añade una línea vacía.

- **`pre`** no parte las líneas: una más ancha que la caja de contenido es un error (`TEXT_OVERFLOW`, con la línea del
  documento donde está, y el mensaje sugiere `pre-wrap`).
- **`pre-wrap`** parte las líneas demasiado anchas **entre palabras**, con el algoritmo voraz de siempre; los espacios de
  antes de la primera palabra (la sangría) se conservan y los del corte se pierden, porque no se ven. Una palabra más
  ancha que la caja sigue siendo un error.
- **Tabulaciones (`Tab size`).** Cada tabulación se sustituye por los espacios que llevan hasta la siguiente columna
  múltiplo de `Tab size`, contando los caracteres desde el principio de la línea (con `Tab size: 4`, `a⇥b` es `a   b` y
  `abcd⇥e`, `abcd    e`), de modo que las columnas alinean con una fuente monoespaciada. Un tramo con formato no cambia
  la cuenta. Un valor menor que 1 es `VALUE_NOT_ALLOWED`.
- **Alineación.** Las líneas preformateadas **no se justifican** (`justify` se trata como `left`); `right` y `center`
  alinean cada línea por separado.
- `Transform`, `Letter spacing`, los tramos, `{page}` y las referencias funcionan como siempre. Con `Inline: markdown` el
  texto se interpreta primero (los saltos de línea simples de un párrafo valen un espacio, así que las líneas que se
  conservan son las de los párrafos).

### Tamaños intrínsecos

Con `pre`, min-content y max-content son la línea más ancha; con `pre-wrap`, min-content es la palabra más ancha y
max-content, la línea más ancha.

> **Decisión:** el texto preformateado es una propiedad, `White space`, que se hereda y actúa en cualquier texto, con
> tres valores: `normal`, `pre` y `pre-wrap`. *Alternativa descartada:* un elemento `Preformatted`, que no cabría en una
> `Cell` con valor y no se heredaría.

> **Decisión:** todo el texto preformateado es un solo párrafo: las líneas en blanco son líneas, así que las viudas y
> huérfanas se cuentan sobre el bloque entero y un bloque largo se parte entre líneas. *Alternativa descartada:* un
> párrafo por línea de origen, que no se podría cortar sin incumplir las viudas.

> **Decisión:** `pre` no parte y una línea demasiado ancha es un error; `pre-wrap` parte entre palabras y una palabra más
> ancha que la caja sigue desbordando, como en el texto normal. *Alternativa descartada:* recortar lo que se sale.

> **Decisión:** los espacios del corte de `pre-wrap` se pierden: no se ven, y así una línea no desborda por culpa de sus
> espacios finales. *Alternativa descartada:* dejarlos colgando (medirlos pero no contarlos).

> **Decisión:** las líneas preformateadas no se justifican: `justify` se trata como `left` en vez de ser un error, porque
> `Align` se hereda y es normal declararlo en `Element: Document`; un bloque de código heredaría el error. *Alternativa
> descartada:* justificar las líneas partidas de `pre-wrap`, o un error en los dos modos.

> **Decisión:** `Tab size` es un número de columnas: cada tabulación lleva a la siguiente columna múltiplo de `Tab size`,
> como en los editores, con 4 por defecto, el de los ficheros STXT. *Alternativa descartada:* sustituir cada tabulación
> por exactamente `Tab size` espacios, o 8 por defecto.

> **Decisión:** un salto de línea final no añade una línea vacía, y el retorno de carro se ignora.

## Subrayado y tachado

Una raya con el color del texto, a lo ancho del tramo (con sus espacios, pero no el espacio entre dos tramos que no la
llevan). Su posición y su grosor son los de la variante del tramo: `underlinePosition` y `underlineThickness` de la tabla
`post` para el subrayado y `yStrikeoutPosition` y `yStrikeoutSize` de la tabla `OS/2` para el tachado, medidos desde la
línea base hasta la parte de arriba de la raya, así que la raya de la negrita es más gruesa. El subrayado se pinta antes
que el texto y el tachado después. En el PDF etiquetado, las rayas son *artifacts*.

> **Decisión:** el subrayado y el tachado usan las métricas de la fuente (`post` y `OS/2`) leídas como la parte de arriba
> de la raya, que es lo que dice la especificación OpenType, con la variante de cada tramo. *Alternativa descartada:* la
> raya centrada en la posición, o marcarla en el PDF etiquetado en lugar de pintarla como *artifact*.

## Lo que no hay

- Tramos con otro tamaño, otro color u otra familia (salvo el código de un `Markdown`), ni que quiten la negrita o la
  cursiva de la caja.
- Estilos de raya (ondulada, punteada), color propio de la raya ni subrayado que se interrumpe en los descendentes.
- `Transform` a anchura completa, interletraje en unidades relativas, versalitas con `smcp`, más modos de `White space`
  (sin partir líneas pero colapsando espacios, por ejemplo).
- Código en línea en un texto con `Inline: markdown`: no tiene `Code font`, así que sigue siendo `MARKDOWN_UNSUPPORTED`.

# Formato del texto

Lo que cambia cómo se ve el texto de una caja sin cambiar su caja: tramos con formato, formato en línea, tramos con
estilo, superíndices y subíndices, mayúsculas, versalitas, interletraje, ligaduras, texto preformateado y rayas. Todas son propiedades de
[`Typography`](typography.md): **se heredan** y valen en las reglas de `Styles`.

## Tramos

El texto puede tener partes con formato dentro de una misma línea: **negrita**, *cursiva*, subrayado y tachado,
combinables. Cada parte es un **tramo**. Se escriben con `Markup: markdown` (abajo) o los genera un `Markdown`.

- Cada tramo usa su variante de la fuente, que debe existir (`FONT_VARIANT_MISSING`, con la línea del tramo) y tener sus
  caracteres (`GLYPH_MISSING`). La negrita y la cursiva se **suman** a las de la caja: no se pueden quitar. Un tramo en
  negrita tiene el peso 700, o el de la caja si ya es 700 o más (`Weight`, [Tipografía](typography.md#pesos-weight)): en
  un texto de peso 300 la negrita es el 700, y en uno de 900 sigue en 900.
- El tamaño y `Line height` son los de la caja, así que las líneas no cambian de alto. El color y la familia también,
  salvo en el código, que usa `Code font` ([abajo](#código-en-línea)), y en un [tramo con estilo](#tramos-con-estilo).
  Un superíndice y un subíndice se pintan más pequeños dentro de su línea
  ([abajo](#superíndice-y-subíndice-position)).
- Una palabra con partes de varios tramos (**neg**rita) es una sola palabra: no se parte entre ellos y mide lo que suman
  sus partes.
- El espacio entre dos palabras tiene el formato del tramo de su primer espacio en blanco: un subrayado cubre los espacios
  de dentro de su tramo, pero no el que hay entre dos tramos que no lo llevan. Con `justify`, el espacio extra es del tramo
  de su espacio.
- Un tramo puede llevar el destino de un enlace ([Enlaces](links.md)), que no cambia cómo se mide ni cómo se ve.
- Un tramo puede ser una imagen ([abajo](#imágenes-en-línea)).

> **Decisión:** un tramo es un conjunto de marcas y no un peso y un estilo propios: así `Styles` puede cambiar la fuente
> de la caja sin tocar los tramos. *Alternativa descartada:* tramos con `Bold` e `Italic` explícitos, que podrían deshacer
> el estilo de la caja.

> **Decisión:** la negrita de un tramo es el 700, salvo que la caja ya pese más. *Alternativa descartada:* el peso
> siguiente que tenga la familia por encima del de la caja, que haría que la negrita de un texto cambiara al añadir un
> fichero a la carpeta de fuentes.

## Formato en línea (`Markup`)

Por defecto el texto de un `Text`, un `Paragraph` o una `Cell` es **literal**: los asteriscos son asteriscos. Con
`Markup: markdown` se interpreta como el Markdown en línea de un [`Markdown`](../components/markdown.md), con la misma
sintaxis: `**negrita**`, `*cursiva*`, `++subrayado++`, `~~tachado~~`, `` `código` `` ([abajo](#código-en-línea)),
`[texto](destino)` ([Enlaces](links.md)), `[texto]{nombre}` ([abajo](#tramos-con-estilo)) y la llamada a una nota al
pie, `[^id]`, que es un tramo en superíndice con el número de la nota ([Footnote](../components/footnote.md)).

- Las marcas no son texto: no se miden ni se pintan. Un carácter especial se escribe con una barra delante (`\*`, `\[`,
  `\\`).
- El salto de línea simple de `Content` vale un espacio, y una `\` al final de la línea es un salto de línea forzado
  ([Markdown](../components/markdown.md#saltos-de-línea-forzados)).
- **Se hereda**: se declara una vez en `For element: Document` o en una regla, y un texto vuelve al literal con `Markup: none`.
- Solo interpreta el formato dentro de la línea. Lo que en un `Markdown` empezaría un bloque es texto:
  `- 5 % de descuento`, `2024. Un buen año`, `# 1 en ventas`, `> 30 años` o una línea `---` se pintan tal cual, sin
  escaparlos, y una lista o un encabezado se escriben en un `Markdown`. Dentro de la línea, una imagen o HTML son
  `MARKDOWN_UNSUPPORTED`, con la línea del párrafo, y un enlace con título o con un destino no admitido, lo mismo que en
  un `Markdown`.
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

## Tramos con estilo

Con `Markup: markdown`, o en un `Markdown`, un texto entre corchetes seguido, sin nada en medio, del nombre de un estilo
entre llaves, `[12,50 €]{precio}`, es un **tramo con estilo**: toma de la regla `For style: precio` de `Styles`
([Reglas reutilizables](styles.md)) lo que cambia cómo se ven sus letras y no el alto de la línea.

| Propiedad de la regla | En el tramo |
|---|---|
| `Font`, `Bold`, `Weight`, `Italic` | la familia, el peso y el estilo de su fuente, que debe existir |
| `Code font` | la familia del código que hay dentro del tramo |
| `Color` | el color de sus letras y de sus rayas |
| `Case`, `Small caps` | cómo se pintan sus letras ([Mayúsculas](#mayúsculas-case), [Versalitas](#versalitas-small-caps)) |
| `Letter spacing` | el interletraje de sus caracteres, también de sus espacios ([Interletraje](#interletraje-letter-spacing)) |
| `Position` | el tramo en superíndice o en subíndice ([abajo](#superíndice-y-subíndice-position)) |

- Lo que la regla declara **sustituye** a lo de la caja, y lo que no declara es lo de la caja. Si dos reglas
  `For style` con ese nombre (una del documento de estilos aparte y otra del documento) declaran lo mismo, gana la del
  documento, como en una caja.
- **Las marcas se suman al estilo**, estén dentro o fuera de los corchetes: `[**x**]{ligero}` y `**[x]{ligero}**` van en
  negrita aunque la regla diga `Weight: 300`, y la negrita es el peso 700 ([arriba](#tramos)). Para las marcas de al
  lado, los corchetes y las llaves del tramo son signos de puntuación, como los de un enlace, y se sigue la regla de
  CommonMark: `[**€12.50**]{precio}` va en negrita, y en `H**[2]{sub}**O`, donde los asteriscos van entre una letra y
  el tramo, no abren ni cierran y son texto. Pegado a una letra, las marcas se escriben dentro: `H[**2**]{sub}O`.

> **Decisión:** los corchetes y las llaves de un tramo cuentan como signos de puntuación para las marcas de al lado,
> igual que los de un enlace. *Alternativa descartada:* que cuenten como letras (hasta la 0.17.0), con lo que
> `H**[2]{sub}**O` iba en negrita, pero `[**€12.50**]{precio}` pintaba los asteriscos.
- **El nombre** es una sola palabra, sin espacios, la misma que en `For style`. Sin su regla, es `STYLE_WITHOUT_RULE`, con
  la línea donde empieza el texto. Unos corchetes seguidos de otra cosa (`[x] {precio}`, `[x]{a b}`, `[x]{}`) son texto,
  y los números de página y las referencias siguen siendo lo que son: en `[ver]{page}` o `[ver]{page of: id}`, los
  corchetes son texto. Por eso `page` y `pages` no pueden ser el nombre de un tramo.
- **Una regla que declara otra cosa** (`Size`, `Line height`, `Align`, `Markup`, un `Box` o un `Break`) es
  `STYLE_NOT_ALLOWED_IN_RUN` si se usa en un tramo, con la línea de lo que declara. La misma regla puede dar estilo a una
  caja con `Style`, donde todo vale.
- **Enlaces.** Un enlace puede tener un tramo con estilo en su texto (`[[ver precio]{precio}](https://...)`) y un tramo con
  estilo puede tener un enlace (`[ver [aquí](https://...)]{precio}`). Un tramo con estilo dentro de otro es
  `MARKDOWN_UNSUPPORTED`, como uno que empieza en un párrafo, una celda, un encabezado o un elemento de lista y acaba en
  otro. Unos corchetes que una línea en blanco separa son texto.
- **Lo que no es texto de Markdown no forma tramos**: unos corchetes escapados (`\[x\]{precio}`), los que van dentro de
  código, en el destino de un enlace o en una imagen, y los de un bloque de código, que se escriben tal cual.
- Un error del tramo (una familia que no existe, un contraste bajo) señala la línea de la propiedad en la regla. El texto
  que lee un lector de pantalla y el que se copia es el escrito.
- Un texto con Markdown no puede tener los no-caracteres de Unicode U+FDD0 a U+FDEF, que el motor usa para marcar los
  tramos con estilo: es `MARKDOWN_UNSUPPORTED`. Un no-carácter no es texto y ninguna fuente lo tiene.

> **Decisión:** un tramo con estilo toma una regla `For style` y no lleva propiedades propias, como `Style` en una caja: el
> aspecto se declara una vez y se cambia en un sitio. *Alternativa descartada:* atributos en el texto
> (`[x]{color=#B3261E}`), que repartirían los colores por el documento.

> **Decisión:** lo que la regla no puede aplicar a un tramo es un error y no se ignora, aunque la regla se use también en
> cajas. *Alternativa descartada:* tomar de la regla solo lo que vale en un tramo, que haría que `Size` cambiara la caja y
> no el tramo sin ningún aviso.

> **Decisión:** las marcas de Markdown se suman al estilo del tramo dondequiera que se escriban, como se suman a la caja.
> *Alternativa descartada:* que el tramo gane a las marcas de fuera (`**[x]{ligero}**` en peso 300), que obliga a mirar el
> anidamiento para saber cómo se ve una palabra.

## Superíndice y subíndice (`Position`)

`Position: superscript` o `subscript`, en la regla `For style` de un [tramo con estilo](#tramos-con-estilo), pone el
tramo en superíndice o en subíndice: `m[2]{sup}`, `CO[2]{sub}`, `1[er]{sup}`, `Marca[TM]{sup}`.

```stxt
Styles:
    For style: sup
        Typography:
            Position: superscript
    For style: sub
        Typography:
            Position: subscript
Paragraph: Emite 120 g de CO[2]{sub} por m[2]{sup}.
    Typography:
        Markup: markdown
```

| `Position` | Tamaño | Línea base |
|---|---|---|
| `normal` (por defecto) | el de la caja | la de la línea |
| `superscript` | 70 % del de la caja | elevada 0,33 del tamaño de la caja |
| `subscript` | 70 % del de la caja | bajada 0,15 del tamaño de la caja |

- **No cambia el alto de la línea**, como la llamada de una nota al pie ([Footnote](../components/footnote.md#la-llamada)), que
  tiene el tamaño y la altura de un superíndice. El tramo se mide con su tamaño, así que ocupa menos ancho.
- Las letras son las de la fuente, reducidas: no se usan los glifos de superíndice de la fuente ni los caracteres de
  Unicode (`²`, `₂`), que se pueden seguir escribiendo si la fuente los tiene. El texto que se lee y se copia es el
  escrito (`m2`).
- La regla puede declarar además lo que admite un tramo (`Color`, `Font`, `Weight`...), y las marcas se suman: `[**2**]{sub}`
  es un subíndice en negrita.
- **El tachado va con las letras** del tramo, a su altura y con su grosor. **El subrayado se queda en la línea**, para que
  no se rompa bajo una palabra con un superíndice.
- Las versalitas no reducen más el tramo: una letra de un superíndice mide el 70 % con `Small caps` y sin él.
- La llamada de una nota al pie dentro de un subíndice sigue en superíndice.
- **Solo vale en un tramo.** `Position` en una caja, con cualquier valor, es `POSITION_OUTSIDE_RUN`, con la línea de la
  propiedad: en el `Typography` de un elemento, en una regla `For element` o `For id` (al leer la regla), y en una regla
  `For style` que se da a una caja con `Style`. `normal` sirve en una regla anidada, para que un nombre de tramo no sea
  superíndice dentro de un elemento ([Reglas anidadas](styles.md#reglas-anidadas)).

> **Decisión:** el superíndice y el subíndice son una propiedad de la regla de un tramo con estilo y no una marca nueva
> del texto: Markdown no tiene ninguna, y el documento ya pone nombre a sus tramos. *Alternativa descartada:* una sintaxis
> propia (`^2^`, `~2~`), que choca con el tachado y habría que escapar en los textos que usan esos caracteres.

> **Decisión:** el tamaño y el desplazamiento son fijos, los de la llamada de una nota al pie, y el subíndice baja 0,15
> para quedar dentro de la línea. *Alternativa descartada:* dos propiedades más (tamaño y desplazamiento), o `Size` en un
> tramo, que abriría los tramos con cualquier tamaño y con ellos el alto de la línea.

> **Decisión:** `Position` en una caja es un error y no se ignora ni se hereda: una caja entera en superíndice no tiene
> una línea respecto a la que subir. *Alternativa descartada:* que la herede el texto de la caja, que la mezclaría con
> `Overflow: shrink` y con la letra capitular sin ningún uso.

## Código en línea

Con `Markup: markdown`, o en un `Markdown`, lo que va entre comillas invertidas (`` `mvn test` ``) es **código**: un
tramo con la familia de `Code font`, que conserva el peso y el estilo de la caja y los de las marcas del tramo
(``**`x`**`` es código en negrita). El tamaño, el color y `Line height` son los de la caja, así que las líneas no
cambian de alto, y el espacio en blanco se colapsa como en el resto del texto.

`Code font` es de `Typography` ([Tipografía](typography.md)) y se hereda: se declara una vez, en `For element: Document`,
para todo el documento. Dentro de un [tramo con estilo](#tramos-con-estilo) vale el `Code font` de su regla, si lo
declara. Sin ninguna, el código es un error (`CODE_WITHOUT_FONT`), con la línea donde empieza el texto (en un
`Markdown`, la del párrafo). Si la familia no existe, es `FONT_UNKNOWN`, con la línea de `Code font`.

## Imágenes en línea

Con `Markup: markdown`, o en un `Markdown`, `![alt](ruta)` dentro de un texto es una **imagen en línea**: un icono en una
frase, un logotipo pequeño en un título. La ruta es relativa a `images/` de la carpeta de recursos, como la de una
`Image` ([Image](../components/image.md#fichero)); solo PNG, JPEG y SVG (la página de un PDF es `IMAGE_FORMAT`, y una
ruta que no existe, `IMAGE_UNKNOWN`, con la línea del texto). Sola en su párrafo de un `Markdown` sigue siendo una
`Image` ([Markdown](../components/markdown.md#el-subconjunto)).

- **Mide como una palabra.** Es tan alta como el tamaño de la línea (`Size`; con `Overflow: shrink`, el tamaño al que se
  reduce el texto) y tan ancha como su proporción da; no tiene interletraje. Pegada a unas letras forma una palabra con
  ellas, que no se parte; entre espacios es una palabra más, que pasa a la línea siguiente si no cabe. Una imagen más
  ancha que la línea desborda, como una palabra ([Desbordamientos](overflow.md)).
- **Se pinta en su sitio**, centrada en las mayúsculas de la fuente sobre la línea base, como el marcador con imagen de
  una lista ([List](../components/list.md#marcadores-con-imagen)). No cambia el alto de la línea.
- El texto alternativo es el `Alt` de su `Figure` en el PDF etiquetado, dentro del elemento del texto, en el orden de
  lectura; sin él (`![](ruta)`) la imagen es decorativa, un *artifact* ([El PDF por dentro](../implementation/pdf.md#figuras)).
  No forma parte del texto: no se copia, no se busca y no sale en los marcadores ni en los títulos corridos de un
  encabezado.
- Dentro de un enlace, `[![alt](ruta)](destino)`, la imagen es clicable y su `Alt` es lo que se lee del enlace. Ni el
  subrayado del enlace ni el tachado se pintan sobre una imagen. El resto del formato del tramo (negrita, un tramo con
  estilo) no le afecta.
- El título de Markdown (`![alt](ruta "título")`) es un error, como en una `Image`.

```stxt
Paragraph: Incluido ![Sí](icons/check.svg) en todos los planes.
	Typography:
		Markup: markdown
```

> **Decisión:** la imagen mide lo que la letra y se alinea como el marcador con imagen, sin propiedades: una imagen
> en línea es un icono, y un tamaño propio o una alineación propia pedirían una sintaxis que Markdown no tiene.
> *Alternativa descartada:* un alto en el `Alt` o en el título, que mezcla la medida con el texto que se lee.

> **Decisión:** una imagen en línea es un tramo cuyo texto es el carácter de sustitución de objeto (U+FFFC), que
> nunca se pinta ni se lee: así pasa por el reparto en líneas, los tramos y los enlaces como cualquier otro tramo.
> *Alternativa descartada:* una caja `Image` dentro del flujo en línea, que obligaba a un segundo modelo de línea.

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
- Un [tramo con estilo](#tramos-con-estilo) con `Case` en su regla usa el suyo. `capitalize` sigue mirando lo que va antes
  del tramo.

## Versalitas (`Small caps`)

`Small caps` pinta cada minúscula, pasada a mayúscula, al 80 % del `Size`. Los espacios, las cifras, los signos y las
mayúsculas se quedan como están. Se aplica después de `Case`: con `lowercase` todo son versalitas, con `uppercase` ninguna y con
`capitalize` solo la inicial es grande. El `Size` de la caja no cambia, ni la altura de las líneas ni las rayas. Un lector
de pantalla lee el texto escrito, como con `Case`. Un [tramo con estilo](#tramos-con-estilo) con `Small caps` en su regla
usa el suyo.

> **Decisión:** las versalitas son sintetizadas y no la característica `smcp` de la fuente, que no tiene ninguna de las
> incluidas. Con `Case: uppercase` no tienen efecto y no es un error: las dos se heredan, y es normal declararlas
> en sitios distintos.

## Interletraje (`Letter spacing`)

Puntos que se suman al avance de cada carácter, también el espacio, **menos el último de cada línea**, para que `right` y
`center` queden alineados con la última letra. Un [tramo con estilo](#tramos-con-estilo) con `Letter spacing` en su regla
lleva el suyo en cada uno de sus caracteres, y el último de la línea no lleva el de su tramo. Un tramo con interletraje no
tiene ligaduras ([abajo](#ligaduras-ligatures)). Cuenta en el reparto en líneas, en los tamaños intrínsecos y en la
alineación, y `justify` reparte lo que sobra después. Puede ser negativo, y no se comprueba que el avance siga siendo
positivo. Un porcentaje es `VALUE_NOT_ALLOWED`.

El texto que se copia o se extrae del PDF es el de la línea, sin espacios entre las letras
([El PDF por dentro](../implementation/pdf.md#texto-que-se-lee)).

## Ligaduras (`Ligatures`)

Se aplican las ligaduras de la fuente (`Ligatures` vale `true` por defecto), que
juntan algunos pares en un glifo de otro ancho: `fi` es un glifo, y con `Ligatures: false`, dos. Las ligaduras son las de la fuente (en IBM Plex Sans, `fi`; en IBM Plex Serif, `fi` y `fl`; y ninguna en IBM Plex
Mono). Lo que se mide es lo que se pinta. Una ligadura no cruza un espacio ni el límite entre dos tramos. Con
`Letter spacing` distinto de 0, el de la caja o el de un tramo con estilo, no hay ligaduras. No cambia el texto que se lee
ni el que se copia: en el PDF, el glifo de una ligadura es las letras que junta (`fi`), y no el carácter que la fuente
tiene para ese glifo (U+FB01), que una búsqueda de `oficina` no encontraría en todos los visores.

> **Decisión:** `true` por defecto: un texto bien compuesto lleva las ligaduras de su fuente. *Alternativas
> descartadas:* `false` por defecto, que fue la regla hasta la 0.17.0, cuando escribir un texto con ligaduras costaba
> catorce veces más (hoy, 199 ms frente a 101 en un texto corrido de 55 páginas, y lo mismo en un documento con poco
> texto por página); y ligaduras siempre, sin propiedad.

## Texto preformateado

Por defecto el espacio en blanco se colapsa. Con `Preformatted: keep` o `wrap` el texto **conserva sus espacios y sus
saltos de línea**: cada línea del texto es una línea de la caja, también las vacías. Una línea en blanco **no** separa
párrafos: el texto entero es un párrafo para las viudas y huérfanas ([Cortes](breaks.md)), así que un bloque largo se
parte entre líneas. Actúa en un `Text`, un `Paragraph` y una `Cell` con texto. Lo normal es una fuente monoespaciada y un
`Content >>`, que conserva la sangría relativa (no los espacios al final de una línea ni las líneas en blanco del final).
Un salto de línea al final del texto no añade una línea vacía.

- **`keep`** no parte las líneas: una más ancha que la caja es `TEXT_OVERFLOW`, con la línea del documento donde está.
- **`wrap`** parte las líneas demasiado anchas **entre palabras**, con el reparto voraz. La sangría se conserva y los
  espacios del corte se pierden. Una palabra más ancha que la caja sigue siendo un error: en un texto preformateado
  una palabra no se parte tras un guion escrito ni se corta con `Break long words`
  ([Tipografía](typography.md#palabras-largas-break-long-words)).
- **Tabulaciones (`Tab size`).** Cada tabulación lleva a la siguiente columna múltiplo de `Tab size`, contando desde el
  principio de la línea (con `Tab size: 4`, `a⇥b` es `a   b`, y `abcd⇥e`, `abcd    e`). Un valor menor que 1 es
  `VALUE_NOT_ALLOWED`.
- **Alineación.** `justify` se trata como `left`, porque `Align` se hereda y un bloque de código heredaría el error.
  `right` y `center` alinean cada línea por separado.
- `Case`, `Letter spacing`, los tramos, `{page}` y las referencias funcionan como siempre. Con `Markup: markdown` el
  texto se interpreta primero, así que las líneas que se conservan son las de los párrafos.
- **Sangría de primera línea.** `First line indent` desplaza la primera línea del texto, y solo ella; con `wrap`,
  esa línea se parte en lo que deja la sangría ([Tipografía](typography.md#sangría-de-primera-línea-first-line-indent)).
- **Salto de línea forzado.** El carácter U+2028, que es también lo que deja la `\` al final de una línea con
  `Markup: markdown`, acaba la línea como cualquier otro salto
  ([Markdown](../components/markdown.md#saltos-de-línea-forzados)).
- **Otros espacios.** Solo el espacio, la tabulación y los saltos de línea se tratan como tales. Cualquier otro carácter
  de espacio, también U+000B y U+000C, se queda en su línea y la fuente tiene que tenerlo (`GLYPH_MISSING`,
  [Tipografía](typography.md#espacios-y-párrafos)).

### Tamaños intrínsecos

Con `Preformatted: keep`, el ancho más estrecho y el natural son la línea más ancha. Con `wrap`, el ancho más estrecho es la
palabra más ancha y el natural, la línea más ancha.

> **Decisión:** el texto preformateado es una propiedad heredable y no un elemento propio, que no cabría en una
> `Cell` con valor.

## Subrayado y tachado

Una raya con el color del texto, a lo ancho del tramo (con sus espacios, pero no con el espacio entre dos tramos que no la
llevan). Su posición y su grosor son los que da la variante del tramo, así que la raya de la negrita es más gruesa. El
subrayado va debajo del texto y el tachado, encima. Un lector de pantalla no las anuncia.

## Lo que no hay

- Tramos con otro tamaño (salvo el [superíndice y el subíndice](#superíndice-y-subíndice-position), la llamada de una
  nota al pie y la letra capitular de un `Paragraph`, [Text](../components/text.md#letra-capitular-drop-cap)) o con otro
  interlineado, superíndices y subíndices con otro tamaño u otro desplazamiento o con los glifos propios de la fuente, tramos con estilo dentro de otros y
  marcas que quiten la negrita o la cursiva (un tramo con estilo sí puede, con `Weight` o `Italic: false`).
- Estilos de raya (ondulada, punteada), color propio de la raya ni subrayado que se interrumpe en los descendentes.
- `Case` a anchura completa, interletraje relativo al cuerpo, versalitas con `smcp` y más modos de `Preformatted`.
- Imágenes en línea con otro tamaño que el de la letra o alineadas de otra forma, y la página de un PDF o un código de
  barras dentro de un texto.

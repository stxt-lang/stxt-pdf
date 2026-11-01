# `Document`

La raíz de todo documento: `Document`, con el namespace `dev.stxt.pdf`. Tiene exactamente un `Page`, un `Title` y un
`Language`, un `Styles` opcional ([Styles](../common/styles.md)), los `Page overlay` de todo el documento y una o más `Section`,
que tienen el contenido. Un documento no tiene contenido suelto ni `Typography`: la tipografía base va en la regla
`For element: Document`.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Informe
    Page:
        Paper: A4
        Margin: 56
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
    Page overlay:
        Id: footer
        Offset: auto 0 -30 0
        Text: Página {page} de {pages}
    Section:
        Id: front-cover
        Page numbering: none
        Inherit page overlays: false
        Page:
            Margin: 0
        Block:
            Box:
                Height: 100%
                Background: #12315A
    Section:
        Id: body
        Paragraph: El cuerpo del informe empieza en la página 1.
    Section:
        Id: annex
        Page numbering: restart
        Page:
            Orientation: landscape
        Paragraph: El anexo, apaisado, vuelve a la página 1.
```

> **Decisión:** el documento es siempre una secuencia de páginas, así que el área útil tiene siempre alto definido.
> *Alternativa descartada:* un modo continuo para pantalla.

## `Page`

Obligatorio, exactamente uno, hijo de `Document`: es el formato de página de las secciones que no declaran el suyo. Una `Section`
puede declarar el suyo, que hereda del del documento por grupos ([abajo](#el-page-de-una-sección)).

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Paper` | `A3`, `A4`, `A5`, `A6`, `B4`, `B5`, `Letter`, `Legal` | sin valor | Tamaño de papel con nombre, en vertical (tabla de abajo). |
| `Orientation` | `portrait`, `landscape` | `portrait` | Solo con `Paper`: `landscape` cambia el ancho por el alto. |
| `Width` | número > 0 | sin valor | Ancho de la página, sin `Paper`. |
| `Height` | número > 0 | sin valor | Alto de la página, sin `Paper`. |
| `Margin` | 1 a 4 números | `0` | Márgenes de página. Solo medidas, sin porcentajes. |
| `Mirror margins` | `true`, `false` | `false` | En las páginas físicas pares, el margen izquierdo y el derecho se intercambian ([márgenes espejo](#márgenes-espejo)). Con los dos iguales es `MIRROR_MARGINS_WITHOUT_EFFECT`. |

El tamaño de la página se declara **de una sola forma**: un `Paper` (con su `Orientation`, si es apaisada) o un `Width` y
un `Height`. Declarar `Paper` con `Width` o `Height`, `Orientation` sin `Paper`, solo uno de `Width` y `Height`, o ninguna
de las dos formas, es `PAGE_SIZE`. También lo son unos márgenes que no dejan área útil. Los tamaños con nombre:

| `Paper` | Ancho × alto | En puntos | | `Paper` | Ancho × alto | En puntos |
|---|---|---|---|---|---|---|
| `A3` | 297 × 420 mm | 841,89 × 1190,55 | | `B4` | 250 × 353 mm | 708,66 × 1000,63 |
| `A4` | 210 × 297 mm | 595,28 × 841,89 | | `B5` | 176 × 250 mm | 498,9 × 708,66 |
| `A5` | 148 × 210 mm | 419,53 × 595,28 | | `Letter` | 8,5 × 11 in | 612 × 792 |
| `A6` | 105 × 148 mm | 297,64 × 419,53 | | `Legal` | 8,5 × 14 in | 612 × 1008 |

```
Page:
    Paper: A4
    Orientation: landscape
    Margin: 40
```

es una página de 297 × 210 mm, igual que `Width: 297mm` y `Height: 210mm`.

El **área útil** es el rectángulo de la página menos sus márgenes. Es el contenedor de referencia de los elementos de primer
nivel de cada sección y de los `Page overlay`, y su alto es siempre definido.

> **Decisión:** los tamaños ISO 216 son exactos en milímetros (A4 = 210 × 297 mm, 595,28 × 841,89 pt), como en los
> procesadores de texto y en los visores de PDF: una caja de `Width: 210mm` mide lo mismo que la hoja. Letter y Legal
> son exactos en pulgadas. *Alternativa descartada:* redondearlos a puntos enteros (A4 = 595 × 842), que hacía que
> 210 mm no cupieran en un A4.

## `Title` y `Language`

`Document` exige exactamente un `Title`, que es un texto no vacío, y exactamente un `Language`, que es una etiqueta
BCP 47 (`es`, `en-US` o `ca`). Un valor que no lo cumple es `VALUE_NOT_ALLOWED`, y si falta cualquiera de los dos,
`TOO_FEW_CHILDREN`. No
afectan al layout, salvo que `Language` da las reglas de las mayúsculas de `Case`
([Formato del texto](../common/text-formatting.md#mayúsculas-case)). El título es el que muestra el visor, y el idioma,
el que usa un lector de pantalla. Los dos son obligatorios para que el PDF sea accesible ([El PDF](../common/pdf.md)).

## `Section`

Un documento se compone de **secciones**: partes independientes entre sí que se maquetan una detrás de otra. Todo el
contenido está en alguna.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Id` | texto | sin valor | Único en el documento. Es el destino de `Link: #id` y de `{page of: id}` (su primera página). |
| `Style` | nombres separados por espacios | sin valor | Le aplican las reglas `For style` con esos nombres. Como `For id` y `For element: Section`, solo pueden dar `Typography`, que hereda todo su contenido (`STYLE_NOT_ALLOWED` con `Box` o `Break`). |
| `Page` | como el del documento | el del documento | Ver [abajo](#el-page-de-una-sección). |
| `Page numbering` | `continue`, `restart`, `none` | `continue` | Cómo numera sus páginas ([numeración](#numeración-de-las-páginas)). |
| `Page number format` | `decimal`, `lower-roman`, `upper-roman` | `decimal` | Cómo se escriben sus números de página: `1`, `i`, `I`. Con `none` es `PAGE_NUMBER_FORMAT_WITHOUT_NUMBERS`. |
| `Start` | entero ≥ 1 | `1` | Con `restart`, el número de su primera página. Sin `restart` es `START_WITHOUT_RESTART`. |
| `Inherit page overlays` | booleano | `true` | Si lleva los `Page overlay` del documento. Con `false`, solo salen los suyos. |
| `Begin on` | `any`, `odd` | `any` | En qué página física empieza ([doble cara](#empezar-en-página-impar)). En la primera sección, `odd` es `BEGIN_ON_FIRST_SECTION`. |
| `Footnote numbering` | `continue`, `restart` | `continue` | Si sus notas al pie siguen el contador del documento o vuelven a empezar en 1 ([Footnote](footnote.md#numeración)). |
| `Footnote number format` | `decimal`, `lower-alpha`, `upper-alpha`, `lower-roman`, `upper-roman` | `decimal` | Cómo se numeran sus notas al pie, en la llamada y en la nota. |

Su contenido son elementos en flujo, `Frame`, `Overlay`, `Page overlay` y `Footnote` ([Footnote](footnote.md)), que no ocupa
sitio donde se escribe.

- **Una sección empieza una página.** Siempre, aunque la anterior acabe a mitad de página, y la siguiente empieza otra. El
  contenido de una sección no se mezcla con el de otra.
- **Cada sección es la raíz de su layout.** Su caja es el área útil de cada una de sus páginas (el contenedor de referencia
  de sus elementos de primer nivel): sus márgenes y su alto salen de su `Page`, y cada página lleva el suyo. Una
  `Section` no es una caja con `Box` propio: no tiene `Margin`, `Padding` ni fondo, y aísla los márgenes: no
  se fusiona con nada.
- **Una sección sin contenido es `SECTION_EMPTY`**: sin elementos, o solo con `Page overlay`, que no ocupan sitio, no tendría
  ninguna página. Un `Overlay` sí cuenta: una portada hecha solo de cajas posicionadas es válida.
- **Tipografía por sección.** Las reglas que llegan a una sección solo pueden dar `Typography` (`Language` incluido: un
  anexo en inglés), que hereda todo su contenido.
- **Un solo documento.** La jerarquía de encabezados, los índices (`Table of contents`, que listan los encabezados de todas las
  secciones, en orden), `Styles`, los `Id` (únicos en todo el documento) y la convergencia de las referencias son del
  documento y no de la sección.

> **Decisión:** no hay sección implícita para el contenido suelto: la separación está siempre a la vista.

> **Decisión:** una sección sin contenido es un error y no una página en blanco que nadie escribió.

### El `Page` de una sección

Hereda del `Page` del documento **por grupos**: el tamaño (`Paper` con su `Orientation`, o `Width` y `Height`) se hereda
entero o se declara entero, y los márgenes (`Margin` con su `Mirror margins`) se heredan juntos o se declaran juntos: una
sección que declara `Margin` sin `Mirror margins` no los espeja, y `Mirror margins` sin `Margin` es `PAGE_SIZE`. Una sección
que solo declara `Orientation` gira el `Paper` del documento (`Orientation: landscape` sobre un documento A4 es un A4
apaisado). Si el documento declara `Width` y `Height`, no hay papel que girar y es `PAGE_SIZE`. Declarar `Paper` con
`Width` o `Height` sigue siendo `PAGE_SIZE`.

> **Decisión:** herencia por grupos, para que `Orientation: landscape` sola dé el papel del documento apaisado.
> *Alternativa descartada:* un `Page` completo en cada sección.

### `Page overlay` del documento y de la sección

Un `Page overlay` de `Document` sale en las páginas de todas las secciones salvo las que declaran `Inherit page overlays: false`, y uno
de una `Section`, solo en las suyas. `Pages` cuenta dentro del alcance donde se declara
([posicionados](frame-and-overlays.md#pages)). Se colocan en el área útil de cada página, que es la de su sección.
Los del documento se pintan antes que los de la sección. `Inherit page overlays: false` en un documento sin `Page overlay`, que no tiene
efecto, no es un error: una plantilla puede ponerlos después.

Un `Page overlay` del documento no está en ningún flujo: su posición estática es la esquina del área útil.

> **Decisión:** `Pages` cuenta dentro del alcance donde se declara. *Alternativa descartada:* contar siempre las páginas
> físicas del documento.

### Numeración de las páginas

Hay un contador de páginas por documento. Cada sección dice qué hace con él:

- `continue` (por defecto): sus páginas siguen numerándose.
- `restart`: su primera página vuelve a ser la 1.
- `none`: sus páginas no tienen número y no avanzan el contador.

`{page}` es el valor del contador, y `{pages}`, el último valor de su **tanda**, la que empieza en cada `restart`:
con una portada `none` y 6 páginas de cuerpo, la primera página interior es «1 de 6» aunque sea la segunda del documento.

**Lo que se imprime manda.** `{page of: id}`, las entradas de un índice y el pie dicen el mismo número: el del contador. Una
referencia a algo que está en una página sin número es `REFERENCE_NOT_PLACEABLE`, y un texto con `{page}` o `{pages}` que
cae en una página sin número (el `Page overlay` del documento en una portada que lo hereda) es `PAGE_NUMBER_UNAVAILABLE`: nunca se
deja en blanco. Los marcadores, los enlaces y `Pages` siguen la página física. Un encabezado de una sección `none` no puede
estar en un índice (`In table of contents: false`).

Como el contador no avanza en una sección `none`, una sección `continue` detrás de una portada empieza en 1. Con
`Page numbering: restart`, `Start` da el número de la primera página de la sección (`Start: 5`: un documento repartido
en varios ficheros). El visor muestra también el número impreso ([El PDF](../common/pdf.md#páginas)).

**Cómo se escribe.** `Page number format` escribe los números de las páginas de la sección en cifras (`decimal`), en romanos en
minúscula (`lower-roman`: `i`, `ii`, `iv`) o en mayúscula (`upper-roman`). Lo que se imprime sigue mandando: `{page}` y
`{pages}` se escriben con el estilo de la página donde salen, y `{page of: id}` y las entradas de un índice, con el de
la página a la que se refieren. Un prefacio en romanos y un cuerpo que vuelve a empezar en 1:

```
Section:
    Id: preface
    Page number format: lower-roman
    Paragraph: El prefacio: sus páginas son i, ii, iii...
Section:
    Page numbering: restart
    Paragraph: El cuerpo empieza en la página 1.
```

Un `Page number format` en una sección `none`, que no tiene números, es `PAGE_NUMBER_FORMAT_WITHOUT_NUMBERS`. Un `Start` sin `restart`,
que no tendría efecto porque la sección sigue la cuenta de la anterior, es `START_WITHOUT_RESTART`.

> **Decisión:** el estilo es de la sección y no del contador: una sección `continue` en romanos sigue la cuenta de la
> anterior y la escribe en romanos. Solo cifras y romanos, que son los de las páginas de los libros. *Alternativa
> descartada:* letras (`a`, `b`), que no se usan para numerar páginas.

> **Decisión:** lo que se imprime manda, para que el índice y el pie coincidan. Lo que no tiene número es un error y nunca
> un hueco vacío. *Alternativa descartada:* dejar el hueco vacío.

### Empezar en página impar

Para imprimir a doble cara, `Begin on: odd` hace que la sección empiece en una página física impar, la de la derecha: si
la anterior acaba en una impar, acaba con una **página en blanco**. La página física es la posición en el documento (la
primera es la 1) y no el número impreso. Con `any` (por defecto), la sección empieza en la página siguiente.

La página en blanco es **una página más de la sección anterior**:

- Tiene su `Page`, su `Page number format` y su numeración: si la sección numera, avanza el contador y cuenta para `{pages}`
  como cualquier otra, aunque no se imprima. Si es `none`, no tiene número.
- No tiene contenido ni `Page overlay`, ni los de la sección ni los del documento.
- No cuenta para el `Pages` de los `Page overlay` de su sección (`Pages: last` es su última página con contenido). Para los del
  documento, que cuentan las páginas físicas, sí cuenta, aunque no salgan en ella.

```
Section:
    Id: chapter-2
    Begin on: odd
    Heading 1:
        Text: Capítulo 2
```

La primera sección ya empieza en la página 1: `Begin on: odd` en ella no tendría efecto y es `BEGIN_ON_FIRST_SECTION`.

> **Decisión:** la página en blanco es de la sección anterior y cuenta para el número, como en un libro: así los números
> impares impresos caen a la derecha. *Alternativa descartada:* una página sin número que no cuenta, que desplaza la
> paridad de los números.

> **Decisión:** la paridad es la de la página física, que es la que decide el lado al imprimir. Solo `odd`: empezar en
> par casi no se usa y obligaría a una página en blanco antes de la primera sección. *Alternativa descartada:* la
> paridad del número impreso, que no existe en una sección `none` y no siempre coincide con el lado.

### Márgenes espejo

En un libro, el margen interior, el del lomo, es distinto del exterior, y cambia de lado con la página. `Mirror margins:
true` en `Page` lo hace: `Margin` da los márgenes de las páginas físicas **impares**, las de la derecha, y en las **pares** el
margen izquierdo y el derecho se intercambian. El área útil mide lo mismo en las dos caras, así que el contenido se
reparte igual: solo cambia dónde empieza.

```
Page:
    Paper: A5
    Margin: 40 30 50 60
    Mirror margins: true
```

da un margen interior de 60 y uno exterior de 30: en las impares, 60 a la izquierda y 30 a la derecha; en las pares, al
revés. La paridad es la de la página física, la misma de `Begin on`; la página en blanco que deja `Begin on: odd` es par
y lleva los márgenes de las pares, aunque no tenga nada que colocar.

- Los `Page overlay` **no se mueven solos**: se colocan en el área útil de cada página, que es la que cambia de sitio, y
  el que está pegado a su borde izquierdo lo sigue estando en las dos caras. Un folio en el exterior son dos `Page
  overlay`, uno con `Pages: odd` a la derecha y otro con `Pages: even` a la izquierda.
- Una sección lo hereda **junto con `Margin`** ([arriba](#el-page-de-una-sección)): la que declara `Margin` empieza de
  nuevo, sin espejo salvo que lo escriba.
- `Mirror margins: true` con el margen izquierdo igual al derecho no cambia nada y es `MIRROR_MARGINS_WITHOUT_EFFECT`.

> **Decisión:** `Margin` es el de las páginas impares, las de la derecha, que son las primeras de cada sección con
> `Begin on: odd`: así el margen interior es el izquierdo, como se escribe. *Alternativa descartada:* un `Inside` y un
> `Outside` además de `Margin`, que darían dos formas al mismo margen.

> **Decisión:** los `Page overlay` no se espejan: un pie centrado no necesita nada y uno exterior se escribe con `Pages`,
> que ya existe y deja la posición a la vista. *Alternativa descartada:* espejar su `Offset` en las páginas pares, que
> movería también lo que no debe moverse (un logotipo siempre a la izquierda).

## Lo que no hay

- Un fondo de página propio de la sección (una portada a sangre pinta un bloque del tamaño de la página) y una página
  apaisada que no sea una sección entera.
- Páginas con nombre o reglas de página: lo que cambia de una parte a otra es una sección.

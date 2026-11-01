# Estructura de un documento

Un documento describe un **árbol de cajas** con elementos (`Block`, `Stack`, `Grid`, `Text`, etc.) que solo admiten las
propiedades que tienen sentido en ellos. El motor calcula la posición y el tamaño de cada caja en una secuencia de
**páginas** de tamaño fijo, parte entre páginas lo que no cabe y escribe el resultado en un PDF.

## Convenciones

- **Las medidas están en puntos tipográficos** (1 pt = 1/72 de pulgada) **o en milímetros** (`12mm`), que se pasan a
  puntos al leer el documento. Un A4 mide 210 × 297 mm, 595,28 × 841,89 pt.
- **Los nombres de nodos y valores están en inglés**: `Margin`, `Overlay`, `Edges: open`. El texto de un `Text`
  puede estar en cualquier idioma.
- **Los ejes:** *horizontal* es el de `Width` e *izquierda/derecha*, y *vertical*, el de `Height` y *arriba/abajo*. La
  escritura es siempre horizontal, de izquierda a derecha.
- **Las cuatro cajas de una caja** ([La caja](box.md#las-cuatro-cajas)): de contenido, de relleno, de borde y de margen.
  Salvo que se diga otra cosa, las posiciones y los tamaños son los de la **caja de borde**.

## Elementos

Cada elemento solo admite los hijos que tienen sentido en él: sus propiedades y, si es un contenedor, otros elementos. Lo
define el [schema](#schema), así que una propiedad fuera de su sitio (`Column sizes` en un `Stack`, `Offset` en un `Block`) es
un error al leer el documento, también en el editor.

La raíz es `Document`, con exactamente un `Page`, un `Title` y un `Language`, un `Styles` opcional con las reglas
reutilizables ([Styles](styles.md)), los `Page overlay` de todo el documento y una o más `Section` con el contenido
([Document](../components/document.md)). No hay contenido suelto: todo lo que se maqueta va dentro de una sección. Las
reglas pueden venir también de un documento de estilos aparte ([Styles](styles.md#documento-de-estilos-aparte)).

| Elemento | Qué es | Contiene |
|---|---|---|
| `Section` | Parte del documento con su `Page`, sus `Page overlay` y su numeración ([Document](../components/document.md#section)) | Elementos, y su `Page` |
| `Block` | Caja que apila sus hijos en vertical ([Block](../components/block.md)) | Elementos |
| `Stack` | Coloca sus ítems en líneas ([Stack](../components/stack.md)) | `Item`, `Overlay` y `Page overlay` |
| `Grid` | Coloca sus ítems en una rejilla ([Grid](../components/grid.md)) | `Area`, `Overlay` y `Page overlay` |
| `Text` | Texto suelto: un rótulo, una cifra, una línea de una cabecera ([Text](../components/text.md)) | Su texto, en el valor o en `Content`, sin líneas en blanco |
| `Paragraph` | Un párrafo del contenido ([Text](../components/text.md#paragraph)) | Su texto, en el valor o en `Content`, sin líneas en blanco |
| `Heading 1` a `Heading 6` | Encabezado de nivel 1 a 6 ([Encabezados](../components/heading.md)) | Uno o varios `Text` |
| `List` | Lista con marcadores o numerada ([List](../components/list.md)) | Uno o varios `List item` |
| `List item` | Ítem de una `List`: un bloque con su marcador | Elementos, salvo los encabezados |
| `Table` | Tabla con cabecera y pie que se repiten en cada página ([Table](../components/table.md)) | `Header rows`, `Body` y `Footer rows`, en ese orden |
| `Header rows`, `Body`, `Footer rows` | Cabecera, cuerpo y pie de una `Table` | Una o varias `Row` |
| `Row` | Fila de una tabla | Una o varias `Cell` |
| `Cell` | Celda: un texto o un bloque | Su texto en el valor, o elementos salvo los encabezados |
| `Columns` | Bloque cuyo contenido fluye por varias columnas ([Columns](../components/columns.md)) | Elementos en flujo |
| `Markdown` | Contenido escrito en Markdown ([Markdown](../components/markdown.md)) | Su texto en `Content` |
| `Image` | Imagen PNG, JPEG o SVG, o una página de un PDF ([Image](../components/image.md)) | Nada: la ruta va en el valor |
| `Table of contents` | Índice automático ([Table of contents](../components/table-of-contents.md)) | Nada: las entradas las genera el motor |
| `Frame` | Bloque que se desplaza sin mover a sus hermanos ([posicionados](../components/frame-and-overlays.md)) | Elementos |
| `Overlay` | Bloque fuera de flujo, colocado en su contenedor de referencia | Elementos |
| `Page overlay` | Bloque fuera de flujo, colocado en el área útil de las páginas que dice `Pages` | Elementos |
| `Item` | Ítem de un `Stack`: cómo crece, encoge y se alinea | Exactamente un elemento en flujo |
| `Area` | Ítem de un `Grid`: el área que ocupa y cómo se alinea | Exactamente un elemento en flujo |

**Elementos** son todos menos `Section`, `Item`, `Area`, `Header rows`, `Body`, `Footer rows` y `Row`, y cada uno es una
caja del [árbol](#el-árbol-de-cajas). **Elementos en flujo** son los que pueden ser un ítem: todos menos `Overlay` y
`Page overlay`, que salen del flujo, y `List item` y `Cell`, que solo van en una `List` y en una `Row`.

`Item` y `Area` no son cajas: son **envoltorios** que añaden al elemento lo que solo tiene sentido dentro de su
contenedor. `Header rows`, `Body`, `Footer rows` y `Row` tampoco: agrupan las celdas, que sí lo son. Una `Row` con `Id`
o `Box` tiene además una franja que se pinta debajo de sus celdas ([Table](../components/table.md#filas)), pero no
ocupa sitio.

> **Decisión:** `Item` y `Area` son envoltorios con exactamente un elemento en flujo (`SINGLE_ELEMENT`): el tamaño, los
> márgenes y el `Id` van en el elemento, y cada ítem es una sola caja. Todo hijo en flujo de un `Stack` es un `Item`, y de
> un `Grid`, un `Area`, aunque no declare nada. *Alternativa descartada:* envoltorios que son cajas con varios hijos, o
> admitir los elementos sueltos y el envoltorio solo cuando hace falta (dos formas de escribir lo mismo).

### Propiedades y grupos

Las propiedades van agrupadas por su significado: `Box` ([la caja](box.md)), `Typography` ([el texto](typography.md),
que se hereda) y `Break` ([los cortes de página](breaks.md)). Cada grupo va como mucho una vez por elemento, y cada
propiedad, como mucho una vez por grupo. Fuera de los grupos queda lo que define al elemento: las
[propiedades del elemento](#propiedades-del-elemento) y las propias de cada uno (`Gap` de `Stack`, `Offset` de `Overlay` o
`Marker` de `List`).

```
Text: Resultados del trimestre
    Id: title
    Box:
        Margin: 0 0 16 0
    Typography:
        Bold: true
        Size: 20
    Break:
        Keep with next: true
```

> **Decisión:** dentro de un grupo los nombres son cortos (`Size`, `Align`, `Before`) salvo donde chocan con otro nodo:
> `Line breaking` y no `Wrap`, que es de `Stack` y es otra cosa, porque un nombre es el mismo nodo en todo el schema.

> **Decisión:** un nombre dice qué es la cosa en el documento y no el algoritmo que la maqueta (`Columns`, `Balance`), y
> cuando el mundo editorial o un procesador de textos ya tiene uno conocido, se usa ese (`Style` para un estilo con nombre,
> `Table of contents` y `TOC 1` a `TOC 6`, `Row header`, `Corner radius`). Los ejes se nombran por su dirección en la página
> (`Horizontal`, `Vertical`) y no por su papel, y no hay dos nombres casi iguales con significados distintos (por eso
> `Table of contents` y no `Contents`, que se confundía con `Content`). *Alternativa descartada:* nombres del
> mecanismo (`Multicol`, `Relative`, `Absolute`, `Fixed`, `Main axis`), que solo entiende quien ya lo conoce.

### Propiedades del elemento

| Propiedad | Valores | Por defecto | Elementos | Notas |
|---|---|---|---|---|
| `Id` | texto | sin valor | Elementos y `Section` | Identificador para referencias, enlaces, reglas `For id` y mensajes. Único en el documento (`DUPLICATE_ID`). |
| `Style` | nombres separados por espacios | sin valor | Elementos y `Section` | Le aplican las reglas `For style` con esos nombres ([Styles](styles.md)). Cada estilo necesita su regla y no se repite. |
| `In table of contents` | `true`, `false` | `true` | Encabezados | Si el encabezado tiene entrada en los índices ([Encabezados](../components/heading.md#fuera-del-índice)). |
| `Link` | `https://...`, `http://...`, `mailto:...` o `#id` | sin valor | Elementos salvo `Markdown` y `Table of contents` | Hace clicable la caja entera ([Enlaces](links.md)). No se hereda ni cambia el aspecto. |
| `Isolate margins` | booleano | `false` | `Block`, `Frame` | Corta la fusión de márgenes con los hijos ([Block](../components/block.md#fusión-de-márgenes)). |

## Orden y texto

**El orden importa.** El motor coloca los elementos en el orden en que aparecen. El de las propiedades, y su posición
respecto a los elementos, no importa.

**Texto en línea o en bloque.** `Text: una línea` lleva el texto en su valor. El texto largo va en un bloque
`Content >>`, sin caracteres de escape. Un `Text` tiene uno de los dos, nunca ambos ([Text](../components/text.md)).

## Ejemplo

```
Document (dev.stxt.pdf):
    Language: es
    Title: Informe
    Page:
        Paper: A4
        Margin: 40
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
    Section:
        Text: Un título
            Id: title
            Typography:
                Size: 20
        Stack:
            Id: body
            Gap: 10
            Item:
                Grow: 1
                Block:
                    Paragraph: Primer párrafo.
                    Paragraph: Segundo párrafo.
            Item:
                Block:
                    Box:
                        Width: 100
                        Background: #F1EFE8
```

## El árbol de cajas

Cada elemento es una caja, y cada sección es la raíz de sus páginas, con el área útil de cada una. `Item` y `Area` no
crean caja: sus propiedades pasan al elemento que envuelven. No hay cajas anónimas, porque el texto va siempre en su
propio `Text`.

Cada elemento decide **cómo coloca a sus hijos**:

| Elemento | Coloca a sus hijos en flujo |
|---|---|
| `Section` (el área útil), `Block`, `Frame`, `Overlay`, `Page overlay`, encabezados, `List`, `List item`, `Markdown`, `Table of contents`, `Cell` con elementos | En flujo de bloque ([Block](../components/block.md#el-flujo-de-bloque)) |
| `Stack` | En líneas ([Stack](../components/stack.md)) |
| `Grid`, `Table` | En una rejilla ([Grid](../components/grid.md), [Table](../components/table.md)) |
| `Columns` | En columnas, cada una en flujo de bloque ([Columns](../components/columns.md)) |
| `Text`, `Paragraph`, `Cell` con valor | Sin hijos: reparte su texto en líneas ([Tipografía](typography.md#el-reparto-en-líneas)) |
| `Image` | Sin hijos: su contenido es la imagen ([Image](../components/image.md)) |

**Participación.** Cómo participa una caja en su padre lo decide el padre: dentro de un bloque es un bloque apilado, y dentro
de un `Stack` o un `Grid`, un ítem. `Overlay` y `Page overlay` salen del flujo, y un `Frame` participa como cualquier caja y
después se desplaza.

## Schema

El schema es [`src/main/resources/stxt/dev.stxt.pdf.stxt`](../../../../src/main/resources/stxt/dev.stxt.pdf.stxt), con el
namespace `dev.stxt.pdf`. Define cada nodo una sola vez y los hijos que admite. Un mismo nombre es el mismo nodo en todo
el schema (el `Width` de `Page` y el de `Box`), así que `Gap`, `Horizontal` y `Vertical` tienen los mismos
valores en todos los sitios donde van. Lo que no vale en uno de ellos es un error del motor.

Este resumen da los hijos de cada nodo con hijos. Si difiere del fichero, manda el fichero. Cada propiedad va como mucho una
vez, y los elementos, las veces que se quiera.

| Nodo | Hijos |
|---|---|
| `Document` | `Page`, `Title` y `Language` (exactamente uno de cada), `Styles`; `Page overlay`, `Section` (al menos una) |
| `Section` | `Id`, `Style`, `Page`, `Page numbering`, `Page number format`, `Start`, `Inherit page overlays`, `Begin on`; elementos |
| `Page` | `Paper`, `Orientation`, `Width`, `Height`, `Margin` |
| `Styles` | `For element`, `For style`, `For id`, `For column`. Hijo de `Document` o raíz de un documento de estilos aparte |
| `For element`, `For style`, `For id`, `For column` | `Box`, `Typography`, `Break` |
| `Block` | `Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Isolate margins`, `Rotate`; elementos |
| `Stack` | `Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Direction`, `Wrap`, `Gap`, `Horizontal`, `Vertical`, `Line distribution`; `Item`, `Overlay`, `Page overlay` |
| `Grid` | `Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Column sizes`, `Row sizes`, `Gap`, `Areas`, `Flow`, `Horizontal`, `Vertical`; `Area`, `Overlay`, `Page overlay` |
| `Text`, `Paragraph` | `Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Content`, `Rotate` |
| `Heading 1` a `Heading 6` | `Id`, `Style`, `Link`, `In table of contents`, `Box`, `Typography`; `Text` (al menos uno) |
| `List` | `Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Marker` (obligatorio), `Start`; `List item` (al menos uno) |
| `List item` | `Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Number`; elementos salvo los encabezados (al menos uno) |
| `Table` | `Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Column sizes` (obligatorio), `Column align`, `Gap`; `Header rows`, `Body` (obligatorio), `Footer rows` |
| `Header rows`, `Body`, `Footer rows` | `Row` (al menos una) |
| `Row` | `Id`, `Style`, `Box` (solo el aspecto), `Typography`, `Break`; `Cell` (al menos una) |
| `Cell` | `Id`, `Style`, `Link`, `Box`, `Typography`, `Span`, `Row header`; elementos salvo los encabezados |
| `Columns` | `Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Count`, `Min column width`, `Gap`, `Separator`, `Separator color`, `Balance`; elementos en flujo |
| `Markdown` | `Id`, `Style`, `Box`, `Typography`, `Break`, `Bullet markers`, `Number markers`, `Table columns`, `Content` (obligatorio) |
| `Table of contents` | `Id`, `Style`, `Box`, `Typography`, `Break`, `Levels`, `Leader` |
| `Image` | `Id`, `Style`, `Link`, `Box`, `Break`, `Alt`, `Fit`, `Pdf page`, `Rotate` |
| `Frame` | `Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Isolate margins`, `Offset`, `Layer`, `Rotate`, `Rotate origin`; elementos |
| `Overlay` | `Id`, `Style`, `Link`, `Box`, `Typography`, `Offset`, `Layer`, `Rotate`, `Rotate origin`; elementos |
| `Page overlay` | `Id`, `Style`, `Link`, `Box`, `Typography`, `Offset`, `Layer`, `Pages`, `Rotate`, `Rotate origin`; elementos |
| `Item` | `Grow`, `Shrink`, `Base size`, `Horizontal`, `Vertical`, `Layer`; un elemento en flujo |
| `Area` | `Horizontal`, `Vertical`, `Layer`; un elemento en flujo |
| `Box` | `Width`, `Height`, `Min width`, `Max width`, `Min height`, `Max height`, `Aspect ratio`, `Margin`, `Padding`, `Border`, `Border color`, `Corner radius`, `Background` |
| `Typography` | `Font`, `Code font`, `Bold`, `Italic`, `Size`, `Line height`, `Align`, `Color`, `Line breaking`, `Hyphenate`, `Markup`, `Case`, `Letter spacing`, `Small caps`, `Ligatures`, `Preformatted`, `Tab size`, `Numbered`, `Number format`, `Number label`, `Widows`, `Orphans`, `Language` |
| `Break` | `Before`, `After`, `Keep with next`, `Keep with previous`, `Keep together`, `Edges` |

*Elementos* son `Block`, `Stack`, `Grid`, `Text`, `Paragraph`, los encabezados, `List`, `Table`, `Columns`, `Markdown`,
`Table of contents`, `Image`, `Frame`, `Overlay` y `Page overlay`. *Elementos en flujo* son todos menos `Overlay` y `Page overlay`.

Solo llevan valor en su línea las propiedades (no los grupos), `Text`, `Paragraph`, `Cell`, `Image`, `Area` y las reglas de `Styles`.
`Block: algo` es un error. `Content` va siempre en bloque `>>`, y `Areas`, en línea o en bloque. Lo que el schema no puede
expresar (que `Page` declare su tamaño de una sola forma, que un `Item` tenga exactamente un elemento, el orden de las
partes de una tabla) lo comprueba el motor, con los códigos de [Errores](errors.md).

## Lo que no hay

- **Texto suelto entre cajas:** el texto va siempre en un `Text`, un `Paragraph` o una `Cell`.
- **Texto y bloques en una misma línea:** un elemento coloca a sus hijos de una sola forma.
- **Escritura vertical** ni de derecha a izquierda.

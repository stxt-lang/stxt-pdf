# Estructura de un documento

Un documento describe un **árbol de cajas** con elementos (`Block`, `Flex`, `Grid`, `Text`…) que solo admiten las
propiedades que tienen sentido en ellos. El motor calcula la posición y el tamaño de cada caja dentro de una secuencia
de **páginas** de tamaño fijo y escribe el resultado en un PDF. Las cajas que no caben en una página se parten entre
páginas.

## Convenciones

- **Todas las medidas están en puntos tipográficos** (1 pt = 1/72 de pulgada). Un A4 mide 595 × 842 pt.
- **Los nombres de nodos y valores están en inglés**: `Margin`, `Padding`, `Absolute`, `Decoration: slice`. El texto de
  un `Text` puede estar en cualquier idioma.
- **Los ejes:** *horizontal* es el de `Width` e *izquierda/derecha*; *vertical* es el de `Height` y *arriba/abajo*. La
  escritura es siempre horizontal, de izquierda a derecha.
- **Las cuatro cajas de una caja** ([La caja](caja.md#las-cuatro-cajas)): de contenido, de relleno, de borde y de margen.
- **Caja de borde por defecto:** salvo que se diga otra cosa, las posiciones y los tamaños se refieren a la **caja de
  borde**.

## Elementos

Un documento se escribe con **elementos**, y cada elemento solo admite los hijos que tienen sentido en él: sus
propiedades y, si es un contenedor, otros elementos. El [schema](#schema) define qué admite cada uno, así que una
propiedad fuera de su sitio (`Columns` en un `Flex`, `Offset` en un `Block`) es un error de estructura que se detecta
al leer, también en el editor.

El nodo raíz es `Document`, con exactamente un `Media`, un `Title` y un `Language`, un `Styles` opcional con las reglas
reutilizables ([Styles](estilos.md); también la tipografía base que heredan todas las cajas, con `Element: Document`),
los `Fixed` de todo el documento y una o más `Section`, que tienen el contenido ([Document](../componentes/document.md)).
Un documento no tiene contenido suelto: todo lo que se maqueta va dentro de una sección. Las reglas pueden estar además
en un documento de estilos aparte, que se pasa junto al documento al maquetar ([Styles](estilos.md#documento-de-estilos-aparte)).

| Elemento | Qué es | Contiene |
|---|---|---|
| `Section` | Parte independiente del documento, con su `Media`, sus `Fixed` y su numeración ([Document](../componentes/document.md#section)). Hija de `Document` | Elementos, y su `Media` |
| `Block` | Caja que apila sus hijos en vertical ([Block](../componentes/block.md)) | Elementos |
| `Flex` | Contenedor que coloca sus ítems en líneas ([Flex](../componentes/flex.md)) | `Item`, `Absolute` y `Fixed` |
| `Grid` | Contenedor que coloca sus ítems en una rejilla ([Grid](../componentes/grid.md)) | `Area`, `Absolute` y `Fixed` |
| `Text` | Caja de texto ([Text](../componentes/text.md)) | Su texto: en el valor o en `Content` |
| `Paragraph` | Un párrafo: un `Text` con significado ([Text](../componentes/text.md#paragraph)) | Su texto: en el valor o en `Content`, sin líneas en blanco |
| `Heading 1` … `Heading 6` | Encabezado de nivel 1 a 6 ([Encabezados](../componentes/heading.md)) | Uno o varios `Text` |
| `List` | Lista con marcadores o numerada ([List](../componentes/list.md)) | Uno o varios `List item` |
| `List item` | Ítem de una `List`: un bloque con su marcador | Elementos, salvo los encabezados |
| `Table` | Tabla, con cabecera y pie que se repiten en cada página ([Table](../componentes/table.md)) | `Head`, `Body` y `Foot`, en ese orden |
| `Head`, `Body`, `Foot` | Cabecera, cuerpo y pie de una `Table` | Una o varias `Row` |
| `Row` | Fila de una tabla | Una o varias `Cell` |
| `Cell` | Celda de una fila: un texto o un bloque | Su texto en el valor, o elementos salvo los encabezados |
| `Multicol` | Bloque cuyo contenido fluye por varias columnas ([Multicol](../componentes/multicol.md)) | Elementos en flujo |
| `Markdown` | Bloque con contenido escrito en Markdown, traducido a encabezados, párrafos, listas, tablas, bloques de código e imágenes ([Markdown](../componentes/markdown.md)) | Su texto en `Content` |
| `Image` | Imagen PNG, JPEG o SVG de la carpeta de recursos ([Image](../componentes/image.md)) | Nada: la ruta del fichero va en el valor |
| `Contents` | Índice automático: una entrada por encabezado, con su página ([Contents](../componentes/contents.md)) | Nada: las entradas las genera el motor |
| `Relative` | Bloque que se desplaza con `Offset` sin mover a sus hermanos ([posicionados](../componentes/relative-absolute-fixed.md)) | Elementos |
| `Absolute` | Bloque fuera de flujo, colocado con `Offset` en su contenedor de referencia | Elementos |
| `Fixed` | Bloque fuera de flujo, colocado en el área útil y repetido en las páginas que dice `Pages`, de todo el documento o de una sección | Elementos |
| `Item` | Ítem de un `Flex`: cómo crece, encoge y se alinea | Exactamente un elemento en flujo |
| `Area` | Ítem de un `Grid`: el área que ocupa y cómo se alinea en ella | Exactamente un elemento en flujo |

**Elementos** son todos menos `Section`, `Item`, `Area`, `Head`, `Body`, `Foot` y `Row`. **Elementos en flujo** son los
que pueden ser un ítem: `Block`, `Flex`, `Grid`, `Text`, `Paragraph`, los encabezados, `List`, `Table`, `Multicol`,
`Markdown`, `Contents`, `Image` y `Relative`; `Absolute` y `Fixed` salen del flujo y nunca son ítems. `List item` solo va
dentro de una `List`, y `Cell`, dentro de una `Row`. Todos son **elementos de caja**: cada uno es una caja del
[árbol](#el-árbol-de-cajas).

`Item` y `Area` no son cajas: son **envoltorios** que envuelven al elemento que hace de ítem y le añaden lo que solo tiene
sentido dentro de su contenedor. `Head`, `Body`, `Foot` y `Row` tampoco son cajas: agrupan las celdas de una tabla, que
sí lo son.

> **Decisión:** `Item` y `Area` son envoltorios, no cajas, con exactamente un elemento en flujo dentro
> (`SINGLE_ELEMENT`). Las dimensiones, los márgenes y el `Id` van en el elemento; el envoltorio solo lleva lo que
> depende del contenedor, así que cada ítem es una sola caja. *Alternativa descartada:* que `Item` y `Area` fueran cajas
> de bloque con varios hijos, que añade una caja por ítem y hace que un `Flex` dentro de un área ya no se estire a su alto.

> **Decisión:** todo hijo en flujo de un `Flex` es un `Item`, y de un `Grid`, un `Area`, aunque no declare nada
> (`Area:` sin valor se coloca automáticamente). La estructura es siempre la misma y el schema puede comprobarla.
> *Alternativa descartada:* admitir los elementos directamente en `Flex` y `Grid` y el envoltorio solo cuando hace
> falta, que da dos formas de escribir lo mismo.

### Propiedades y grupos

Las propiedades de una caja van agrupadas por su significado: `Box` ([el modelo de caja](caja.md)), `Typography`
([el texto](tipografia.md), que se hereda) y `Break` ([los cortes de página](cortes.md)). Dentro de cada grupo una
propiedad aparece como mucho una vez, y cada grupo, como mucho una vez por elemento. Fuera de los grupos quedan las
propiedades que definen el propio elemento: `Id`, `Class`, `Link`, `Isolate` e `In contents`
([propiedades del elemento](#propiedades-del-elemento)), las de `Flex` y `Grid`, las de `Item` y `Area`, las de los
elementos posicionados y las de listas, tablas, columnas, `Markdown`, imágenes, índices y secciones.

```
Text: Resultados del trimestre
	Id: title
	Box:
		Margin: 0 0 16 0
	Typography:
		Weight: bold
		Size: 20
	Break:
		After: avoid
```

> **Decisión:** dentro de los grupos los nombres son cortos (`Size`, `Weight`, `Style`, `Align`, `Before`, `After`,
> `Inside`) salvo donde chocan con otro nodo del namespace: `Text wrap` no puede ser `Wrap`, que es el booleano de
> `Flex`, porque un nombre es el mismo nodo en todo el schema. *Alternativa descartada:* renombrar `Wrap` de `Flex`
> (por ejemplo, `Multiline`) para poder usar `Wrap` en `Typography`.

> **Decisión:** fuera de los grupos queda lo que define al elemento: `Id`, `Isolate`, `Offset`, `Layer`, las
> propiedades de `Flex`, `Grid`, `Item` y `Area` y las propias de cada elemento. Un grupo `Position` (`Offset`, `Layer`)
> se descartó porque solo existe en los elementos posicionados, que ya lo dicen con su nombre.

### Propiedades del elemento

| Propiedad | Valores | Por defecto | Elementos | Notas |
|---|---|---|---|---|
| `Id` | texto | — | De caja | Identificador para referencias, enlaces, reglas `For id`, mensajes y volcados. Único en el documento (`DUPLICATE_ID`). |
| `Class` | nombres separados por espacios | — | De caja | Clases del elemento: le aplican las reglas `For class` con esos nombres ([Styles](estilos.md)). Cada clase necesita su regla y no se repite. |
| `In contents` | `true`, `false` | `true` | `Heading 1` … `Heading 6` | Si el encabezado tiene entrada en los índices ([Encabezados](../componentes/heading.md#fuera-del-índice)). No se hereda. |
| `Link` | `https://…`, `http://…`, `mailto:…` o `#id` | — | Todos los de caja salvo `Markdown` y `Contents` | Hace clicable la caja entera ([Enlaces](enlaces.md)). No se hereda y no cambia el aspecto. |
| `Isolate` | booleano | `false` | `Block`, `Relative` | Corta el colapso de márgenes con los hijos ([Block](../componentes/block.md#colapso-de-márgenes)). |

## Orden y texto

**El orden importa.** STXT conserva el orden de los nodos, y el motor coloca los elementos en el orden en que aparecen.
El orden de las propiedades, y su posición respecto a los elementos, no importan.

**Texto en línea o en bloque.** `Text: una línea` lleva el texto en su valor; para párrafos y texto largo, el texto va en
un bloque `Content >>`, sin caracteres de escape. Un `Text` tiene uno de los dos, nunca ambos
([Text](../componentes/text.md)).

## Ejemplo

```
Document (dev.stxt.boxes):
	Language: es
	Title: Informe
	Media:
		Paper: A4
		Margin: 40
	Styles:
		Element: Document
			Typography:
				Font: IBM Plex Sans
				Size: 10
	Section:
		Text: Un título
			Id: title
			Typography:
				Size: 20
		Flex:
			Id: body
			Gap: 10
			Item:
				Grow: 1
				Text:
					Content >>
						Primer párrafo.

						Segundo párrafo.
			Item:
				Block:
					Box:
						Width: 100
						Background: #F1EFE8
```

## El árbol de cajas

El motor transforma el documento en un **árbol de cajas**: cada elemento de caja es una caja, y cada sección es la raíz
del layout de sus páginas, con el área útil de cada una. `Item` y `Area` no crean caja: sus propiedades pasan al
elemento que envuelven. No hay cajas anónimas, porque el texto va siempre en su propio `Text`.

Cada elemento determina **cómo coloca a sus hijos**:

| Elemento | Coloca a sus hijos en flujo |
|---|---|
| `Section` (el área útil), `Block`, `Relative`, `Absolute`, `Fixed`, `Heading 1` … `Heading 6`, `List`, `List item`, `Markdown`, `Contents`, `Cell` con elementos | En flujo de bloque ([Block](../componentes/block.md#el-flujo-de-bloque)) |
| `Flex` | En líneas ([Flex](../componentes/flex.md)) |
| `Grid`, `Table` | En una rejilla ([Grid](../componentes/grid.md), [Table](../componentes/table.md)) |
| `Multicol` | En columnas, cada una en flujo de bloque ([Multicol](../componentes/multicol.md)) |
| `Text`, `Paragraph`, `Cell` con valor | No tiene hijos: reparte su texto en líneas ([Tipografía](tipografia.md#el-reparto-en-líneas)) |
| `Image` | No tiene hijos: su contenido es la imagen ([Image](../componentes/image.md)) |

**Participación.** Cómo participa una caja en su padre lo decide el padre: dentro de un bloque es un bloque apilado;
dentro de un `Flex` o un `Grid` (envuelta en `Item` o `Area`), un ítem. La excepción son `Absolute` y `Fixed`, que salen
del flujo del padre. Un `Relative` participa como cualquier caja en flujo y después se desplaza.

## Schema

Fichero: [`src/main/resources/stxt/dev.stxt.boxes.stxt`](../../../src/main/resources/stxt/dev.stxt.boxes.stxt), un schema
`@stxt.schema` con el namespace `dev.stxt.boxes`. Define cada nodo una sola vez, con su tipo, su descripción y los hijos
que admite (cada propiedad, como mucho una vez). Un elemento solo puede tener los hijos que declara su definición, así
que el schema es lo que garantiza que cada propiedad está en un elemento donde tiene efecto.

Este resumen da los hijos de cada nodo con hijos; si difiere del fichero, manda el fichero. Todos los hijos, salvo los
elementos, admiten como mucho uno.

| Nodo | Tipo | Hijos |
|---|---|---|
| `Document` | `GROUP` | `Media` (exactamente uno), `Title` (exactamente uno), `Language` (exactamente uno), `Styles`; `Fixed`, `Section` (al menos una, varias) |
| `Section` | `GROUP` | `Id`, `Class`, `Media`, `Page numbering`, `Inherit fixed`; elementos |
| `Media` | `GROUP` | `Paper`, `Orientation`, `Width`, `Height`, `Margin` (el tamaño, con `Paper` o con `Width` y `Height`: lo comprueba el motor) |
| `Styles` | `GROUP` | `Element`, `For class`, `For id`, `For column` (varios de cada). Hijo de `Document` o raíz de un documento de estilos aparte |
| `Element`, `For class`, `For id`, `For column` | `INLINE` | `Box`, `Typography`, `Break` |
| `Block` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Break`, `Isolate`; elementos |
| `Flex` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Break`, `Direction`, `Wrap`, `Gap`, `Main axis`, `Cross axis`, `Lines`; `Item`, `Absolute`, `Fixed` |
| `Grid` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Break`, `Columns`, `Rows`, `Gap`, `Areas`, `Flow`, `Horizontal`, `Vertical`; `Area`, `Absolute`, `Fixed` |
| `Text`, `Paragraph` | `INLINE` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Break`, `Content` |
| `Heading 1` … `Heading 6` | `GROUP` | `Id`, `Class`, `Link`, `In contents`, `Box`, `Typography`; `Text` (al menos uno, varios) |
| `List` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Break`, `Marker` (obligatorio), `Start`; `List item` (al menos uno, varios) |
| `List item` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Break`, `Number`; elementos salvo los encabezados (al menos uno) |
| `Table` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Break`, `Columns` (obligatorio), `Column align`, `Gap`; `Head`, `Body` (obligatorio), `Foot` |
| `Head`, `Body`, `Foot` | `GROUP` | `Row` (al menos uno, varios) |
| `Row` | `GROUP` | `Class`, `Typography`, `Break`; `Cell` (al menos uno, varios) |
| `Cell` | `INLINE` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Span`, `Header`; elementos salvo los encabezados |
| `Multicol` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Break`, `Count`, `Column width`, `Column gap`, `Rule`, `Rule color`, `Fill`; elementos en flujo |
| `Markdown` | `GROUP` | `Id`, `Class`, `Box`, `Typography`, `Break`, `Bullet`, `Numbering`, `Table columns`, `Code font`, `Content` (obligatorio) |
| `Contents` | `GROUP` | `Id`, `Class`, `Box`, `Typography`, `Break`, `Levels`, `Leader` |
| `Image` | `INLINE` | `Id`, `Class`, `Link`, `Box`, `Break`, `Alt`, `Fit`, `Rotate`, `Rotate origin` |
| `Relative` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Break`, `Isolate`, `Offset`, `Layer`, `Rotate`, `Rotate origin`; elementos |
| `Absolute` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Offset`, `Layer`, `Rotate`, `Rotate origin`; elementos |
| `Fixed` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Offset`, `Layer`, `Pages`, `Rotate`, `Rotate origin`; elementos |
| `Item` | `GROUP` | `Grow`, `Shrink`, `Basis`, `Cross axis`, `Layer`; un elemento en flujo |
| `Area` | `INLINE` | `Horizontal`, `Vertical`, `Layer`; un elemento en flujo |
| `Box` | `GROUP` | `Width`, `Height`, `Min width`, `Max width`, `Min height`, `Max height`, `Aspect ratio`, `Margin`, `Padding`, `Border`, `Border color`, `Radius`, `Background` |
| `Typography` | `GROUP` | `Font`, `Weight`, `Style`, `Size`, `Line height`, `Align`, `Color`, `Text wrap`, `Inline`, `Transform`, `Letter spacing`, `Small caps`, `Ligatures`, `White space`, `Tab size`, `Numbered`, `Widows`, `Orphans`, `Language` |
| `Break` | `GROUP` | `Before`, `After`, `Inside`, `Decoration` |

*Elementos* (los de dentro de una `Section`) son `Block`, `Flex`, `Grid`, `Text`, `Paragraph`, `Heading 1` … `Heading 6`,
`List`, `Table`, `Multicol`, `Markdown`, `Contents`, `Image`, `Relative`, `Absolute` y `Fixed`; *elementos en flujo*, todos
menos `Absolute` y `Fixed`.

Tipos de las propiedades: `Content` es `BLOCK` (solo `>>`); `Areas`, `TEXT` (en línea o en bloque); `Isolate`, `Wrap`,
`Header` e `In contents`, `BOOLEAN`; `Layer`, `Widows`, `Orphans`, `Start`, `Number` y `Count`, `INTEGER`; `Rotate`,
`NUMBER`; las de valores cerrados (`Table columns`, `Fit`, `Leader`, `Pages`, `Paper` y `Orientation` entre ellas), `ENUM`
con los valores de cada página; el resto (`Bullet`, `Numbering`, `Levels`, `Alt`, `Link` y `Rotate origin` entre ellos),
`INLINE` sin hijos, y su forma la comprueba el motor ([Valores](valores.md)). Los `GROUP` no admiten valor:
`Block: algo` es un error.

Lo que el schema no puede expresar lo comprueba el motor al construir el árbol ([Errores](errores.md)): que `Media`
declare su tamaño de una sola forma, que `Item` y `Area` tengan exactamente un elemento (el schema solo limita cada tipo a
uno) y un `List item` al menos uno (el schema solo cuenta por tipo), que un `Text` tenga su texto en un sitio y solo uno,
que la raíz sea `Document` (en un schema cualquier nodo puede ser raíz) y lo que depende de dónde está un elemento: que un
`Text` de un encabezado no tenga `Break`, y la jerarquía y el sitio de los encabezados. En una `Table`, el motor comprueba
además el orden `Head`, `Body`, `Foot`, que una `Cell` no tenga a la vez texto en el valor y elementos, y que cada fila
llene exactamente sus columnas. El contenido de un `Markdown` lo traduce el motor, y lo generado pasa por el schema como
lo que escribe el autor.

Un mismo nombre es el mismo nodo en todo el namespace: el `Width` de `Media` y el de `Box` comparten definición, y solo
cambia la cardinalidad que fija cada padre. Por eso `Cross axis`, `Horizontal`, `Vertical` y `Gap` tienen los mismos
valores en el contenedor y en `Item` o `Area`; `Columns` y `Gap` significan lo mismo en `Grid` y en `Table`, y
`Text wrap` no se acorta a `Wrap`, que ya es de `Flex`.

> **Decisión:** lo que ya rechaza el schema no tiene código propio del motor: `Main axis: stretch` o `Inside: page` son
> errores del valor del schema, y un bloque `>>` donde van hijos lo rechaza el schema con su código de `stxt-core`,
> también en un documento que no se leyó con `DocumentReader` (el motor vuelve a validar contra el schema al construir
> el árbol, [El árbol de cajas](../implementacion/arbol.md#validar-contra-el-schema)).

## Lo que no hay

- **Texto suelto entre cajas:** el texto va siempre en un `Text`, un `Paragraph` o una `Cell`.
- **Cajas que mezclan texto y bloques en una misma línea:** un elemento coloca a sus hijos de una sola forma (la de la
  tabla de arriba) y cómo participa lo decide su padre.
- **Escritura vertical** ni de derecha a izquierda.

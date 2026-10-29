# `Document`

La raíz de todo documento: `Document`, con el namespace `dev.stxt.pdf`. Tiene exactamente un `Media`, un `Title` y un
`Language`, un `Styles` opcional ([Styles](../comun/estilos.md)), los `Fixed` de todo el documento y una o más `Section`,
que tienen el contenido. Un documento no tiene contenido suelto ni `Typography`: la tipografía base va en la regla
`Element: Document`.

```
Document (dev.stxt.pdf):
	Language: es
	Title: Informe
	Media:
		Paper: A4
		Margin: 56
	Styles:
		Element: Document
			Typography:
				Font: IBM Plex Sans
				Size: 10
	Fixed:
		Id: footer
		Offset: auto 0 -30 0
		Text: Página {page} de {pages}
	Section:
		Id: front-cover
		Page numbering: none
		Inherit fixed: false
		Media:
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
		Media:
			Orientation: landscape
		Paragraph: El anexo, apaisado, vuelve a la página 1.
```

> **Decisión:** el medio es siempre una secuencia de páginas: no hay un modo continuo para pantalla, y por eso el área útil
> tiene siempre alto definido y un porcentaje vertical en una caja de primer nivel es válido. *Alternativa descartada:*
> conservar un modo continuo, a costa de un caso especial en la paginación, el desbordamiento y el alto definido.

## `Media`

Obligatorio, exactamente uno, hijo de `Document`: es el medio de las secciones que no declaran el suyo. Una `Section`
puede declarar el suyo, que hereda del del documento por grupos ([abajo](#el-media-de-una-sección)).

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Paper` | `A3`, `A4`, `A5`, `A6`, `B4`, `B5`, `Letter`, `Legal` | — | Tamaño de papel con nombre, en vertical (tabla de abajo). |
| `Orientation` | `portrait`, `landscape` | `portrait` | Solo con `Paper`: `landscape` cambia el ancho por el alto. |
| `Width` | número > 0 | — | Ancho de la página, sin `Paper`. |
| `Height` | número > 0 | — | Alto de la página, sin `Paper`. |
| `Margin` | 1 a 4 números | `0` | Márgenes de página. Solo puntos. |

El tamaño de la página se declara **de una sola forma**: un `Paper` (con su `Orientation`, si es apaisada) o un `Width` y
un `Height`. Declarar `Paper` con `Width` o `Height`, `Orientation` sin `Paper`, solo uno de `Width` y `Height`, o ninguna
de las dos formas, es `MEDIA_SIZE`; también unos márgenes que no dejan área útil. Los tamaños con nombre, en puntos
enteros:

| `Paper` | Ancho × alto (pt) | | `Paper` | Ancho × alto (pt) |
|---|---|---|---|---|
| `A3` | 842 × 1191 | | `B4` | 709 × 1001 |
| `A4` | 595 × 842 | | `B5` | 499 × 709 |
| `A5` | 420 × 595 | | `Letter` | 612 × 792 |
| `A6` | 298 × 420 | | `Legal` | 612 × 1008 |

```
Media:
	Paper: A4
	Orientation: landscape
	Margin: 40
```

es una página de 842 × 595, igual que `Width: 842` y `Height: 595`.

El **área útil** es el rectángulo de la página menos sus márgenes. Es el contenedor de referencia de los elementos de primer
nivel de cada sección y de los `Fixed`, y su alto es siempre definido.

> **Decisión:** el papel se llama `Paper` y no `Size`, que ya es el tamaño de la fuente (cada nombre tiene una sola
> definición en el namespace). Los tamaños son los de ISO 216 redondeados a puntos enteros, como se usan siempre
> (A4 = 595 × 842), y Letter y Legal, exactos. *Alternativa descartada:* los valores exactos con decimales
> (A4 = 595,28 × 841,89), que no coinciden con los documentos que escriben 595 × 842.

> **Decisión:** el tamaño se declara de una sola forma y los errores son todos `MEDIA_SIZE`, que es el tamaño del medio.
> *Alternativa descartada:* un código propio para el papel, o que `Paper` sustituya a `Width` y `Height` en silencio.

## `Title` y `Language`

`Document` exige exactamente un `Title` (un texto no vacío; si no, `VALUE_NOT_ALLOWED`) y exactamente un `Language` (una
etiqueta BCP 47: `es`, `en-US`, `ca`…; si no, `VALUE_NOT_ALLOWED`); si falta cualquiera de los dos, `TOO_FEW_CHILDREN`. No
afectan al layout, salvo que `Language` da las reglas de las mayúsculas de `Transform`
([Formato del texto](../comun/formato-del-texto.md#mayúsculas-transform)). Van al PDF ([El PDF](../comun/pdf.md)) como
título del documento (información, metadatos XMP y el que muestra el visor) e idioma del contenido etiquetado. Los dos son
obligatorios porque son características de accesibilidad: un lector de pantalla necesita el idioma para leer el contenido,
y el visor muestra el título en lugar del nombre del fichero (PDF/UA-1, 7.1).

## `Section`

Un documento se compone de **secciones**: partes independientes entre sí que se maquetan una detrás de otra. Todo el
contenido está en alguna.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Id` | texto | — | Único en el documento. Es el destino de `Link: #id` y de `{page of: id}` (su primera página). No se vuelca como caja. |
| `Class` | nombres separados por espacios | — | Le aplican las reglas `For class` con esos nombres; como `For id` y `Element: Section`, solo pueden dar `Typography`, que hereda todo su contenido (`STYLE_NOT_ALLOWED` con `Box` o `Break`). |
| `Media` | como el del documento | el del documento | Ver [abajo](#el-media-de-una-sección). |
| `Page numbering` | `continue`, `restart`, `none` | `continue` | Cómo numera sus páginas ([numeración](#numeración-de-las-páginas)). |
| `Inherit fixed` | booleano | `true` | Si lleva los `Fixed` del documento. Con `false`, solo salen los suyos. |

Su contenido son elementos en flujo, `Relative`, `Absolute` y `Fixed`.

- **Una sección empieza una página.** Siempre, aunque la anterior acabe a mitad de página; y la siguiente empieza otra. El
  contenido de una sección no se mezcla con el de otra.
- **Cada sección es la raíz de su layout.** Su caja es el área útil de cada una de sus páginas (el contenedor de referencia
  de sus elementos de primer nivel): sus márgenes y su alto salen de su `Media`, y cada página lleva el suyo. Una
  `Section` no es una caja con `Box` propio: no tiene `Margin`, `Padding` ni fondo, y es un contexto de formato de bloque
  que no colapsa con nada.
- **Una sección sin contenido es `SECTION_EMPTY`**: sin elementos, o solo con `Fixed`, que no ocupan sitio, no tendría
  ninguna página. Un `Absolute` sí cuenta: una portada hecha solo de cajas posicionadas es válida.
- **Tipografía por sección.** Las reglas que llegan a una sección solo pueden dar `Typography` (`Language` incluido: un
  anexo en inglés), que hereda todo su contenido.
- **Un solo documento.** La jerarquía de encabezados, los índices (`Contents`, que listan los encabezados de todas las
  secciones, en orden), `Styles`, los `Id` (únicos en todo el documento) y la convergencia de las referencias son del
  documento, no de la sección.

> **Decisión:** `Section` es el único contenido de `Document`: el flujo y los elementos posicionados van dentro, y no hay
> sección implícita para el contenido suelto. Así la separación está siempre a la vista. *Alternativa descartada:* aceptar
> contenido suelto como una sección implícita.

> **Decisión:** el `Id` de una sección es un hijo `Id`, no el valor: en este schema el valor de un elemento es su contenido
> (la ruta de `Image`, la colocación de `Area`) y `Id` es una sola definición en todo el namespace. *Alternativa
> descartada:* `Section: nombre`.

> **Decisión:** las reglas que llegan a una sección solo pueden dar `Typography`. No hay `Background` de página ni número
> inicial (`Start`).

> **Decisión:** una sección sin contenido es un error: una página en blanco que nadie escribió es un valor por defecto
> silencioso. *Alternativa descartada:* una página en blanco.

> **Decisión:** el documento sigue siendo uno para la jerarquía de encabezados, los índices y `Styles`: se comprueban en el
> orden del documento, atravesando las secciones (un `Heading 1` en cada sección es correcto).

### El `Media` de una sección

Hereda del `Media` del documento **por grupos**: el tamaño (`Paper` con su `Orientation`, o `Width` y `Height`) se hereda
entero o se declara entero, y `Margin` se hereda o se declara. Una sección que solo declara `Orientation` gira el `Paper`
del documento (`Orientation: landscape` sobre un documento A4 es un A4 apaisado); si el documento declara `Width` y
`Height` no hay papel que girar y es `MEDIA_SIZE`. Declarar `Paper` con `Width` o `Height` sigue siendo `MEDIA_SIZE`.

> **Decisión:** el `Media` de una sección hereda por grupos: el tamaño es un grupo y `Margin` otro, cada uno heredado entero
> o declarado entero, así que `Orientation: landscape` sola da el papel del documento apaisado. *Alternativa descartada:*
> un `Media` completo en cada sección.

### `Fixed` del documento y de la sección

Un `Fixed` de `Document` sale en las páginas de todas las secciones salvo las que declaran `Inherit fixed: false`; uno de
una `Section`, solo en las suyas. `Pages` cuenta dentro del alcance donde se declara
([posicionados](relative-absolute-fixed.md#pages)). Se colocan en el área útil de cada página, que es la de su sección.
Los del documento se pintan antes que los de la sección. `Inherit fixed: false` en un documento sin `Fixed`, que no tiene
efecto, no es un error: una plantilla puede ponerlos después.

> **Decisión:** un `Fixed` del documento sale en todas las secciones que lo heredan y `Pages` cuenta dentro del alcance
> donde se declara; `Inherit fixed: false` sin `Fixed` en el documento no es un error. *Alternativa descartada:* `Pages`
> contado siempre en páginas físicas del documento.

> **Decisión:** un `Fixed` del documento se coloca, en cada página, como si su posición estática fuera la esquina del área
> útil (no está en ningún flujo), y se pinta antes que los de la sección: los dos no tienen un árbol común que los ordene.

### Numeración de las páginas

Hay un contador de páginas por documento. Cada sección dice qué hace con él: `continue` (por defecto), sus páginas siguen
numerándose; `restart`, su primera página vuelve a ser la 1; `none`, sus páginas no tienen número y no avanzan el
contador. `{page}` es el valor del contador, y `{pages}`, el último valor de su **tanda**, la que empieza en cada `restart`:
con una portada `none` y 6 páginas de cuerpo, la primera página interior es «1 de 6» aunque sea la segunda del documento.

**Lo que se imprime manda.** `{page of: id}`, las entradas de un índice y el pie dicen el mismo número: el del contador. Una
referencia a algo que está en una página sin número es `REFERENCE_NOT_PLACEABLE`, y un texto con `{page}` o `{pages}` que
cae en una página sin número (el `Fixed` del documento en una portada que lo hereda) es `PAGE_NUMBER_UNAVAILABLE`: nunca se
deja en blanco. Los marcadores, los enlaces y `Pages` siguen la página física. Un encabezado de una sección `none` no puede
estar en un índice (`In contents: false`).

> **Decisión:** la numeración es un contador por documento con `continue`, `restart` y `none`, y `{pages}` es el último
> número de la tanda. El nombre no puede ser `Numbering`, que ya es una propiedad de `Markdown`. *Alternativa descartada:*
> un `Start` para empezar en otro número (pendiente).

> **Decisión:** lo que se imprime manda: `{page of: id}` y las entradas de los índices dan el número del contador, no el de
> la página física, para que el índice y el pie coincidan; lo que no tiene número es un error, nunca un hueco vacío. Los
> marcadores y los enlaces siguen la página física, y las etiquetas de página del PDF llevan la numeración impresa.
> *Alternativa descartada:* dejar el hueco vacío.

> **Decisión:** el contador no avanza en una sección `none`, así que una sección `continue` detrás de una portada empieza
> en 1; un `Fixed` de una sección cuenta sus `Pages` con las páginas de la sección, aunque sean las 4 a 6 del documento.

### PDF

Cada página tiene el tamaño del `Media` de su sección. El PDF etiquetado agrupa cada sección en un `Sect` (con `Lang`, si
cambia su idioma), y las etiquetas de página (`PageLabels`) llevan el número impreso, o ninguno en las páginas sin número;
solo se escriben si alguna página no es su número físico ([El PDF](../comun/pdf.md)).

### Volcado

Una sección no se vuelca como caja, aunque tenga `Id`; sus cajas con `Id` sí, con las coordenadas de la página de su
sección ([Pruebas](../implementacion/pruebas.md#el-volcado)).

> **Decisión:** una sección no se vuelca como caja: es la página, no una caja. Su `Id` sí es un destino de `Link` y de
> `{page of: id}`. *Alternativa descartada:* volcar el rango de páginas.

## Lo que no hay

- Empezar una sección en página impar con una página en blanco (impresión a doble cara), un número inicial de la
  numeración (`Start: 5`) u otros formatos de número de página (romanos en el prefacio).
- Un fondo de página propio de la sección (una portada a sangre pinta un bloque del tamaño de la página) y una página
  apaisada que no sea una sección entera.
- Páginas con nombre o reglas de página: lo que cambia de una parte a otra es una sección.

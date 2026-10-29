# Conceptos

## Páginas y puntos

Un documento se maqueta siempre en **páginas** de tamaño fijo, como un PDF impreso. El tamaño y los márgenes de la página
los da `Media` ([Document](../componentes/document.md)). Lo que queda dentro de los márgenes es el **área útil**: el sitio
donde va el contenido.

Todas las medidas están en **puntos** (pt): 1 pt = 1/72 de pulgada, unos 0,35 mm. Un A4 mide 595 × 842 pt; con
`Margin: 40`, el área útil mide 515 × 762. Las medidas se escriben como números: `Width: 200` son 200 puntos.

## Cajas

Cada elemento del documento es una **caja**: un rectángulo con su contenido. Una caja tiene, de dentro afuera:

```
┌──────────── margen ────────────┐
│  ┌─────────── borde ─────────┐ │
│  │  ┌──────── relleno ─────┐ │ │
│  │  │      contenido       │ │ │
│  │  └──────────────────────┘ │ │
│  └───────────────────────────┘ │
└────────────────────────────────┘
```

- el **contenido**: el texto, la imagen o las cajas de dentro;
- el **relleno** (`Padding`): el espacio entre el contenido y el borde, que tiene el fondo de la caja;
- el **borde** (`Border`): la línea alrededor;
- el **margen** (`Margin`): el espacio que separa la caja de las de alrededor, siempre transparente.

`Width` y `Height` miden la caja **con** su relleno y su borde, sin el margen: una caja con `Width: 200` y `Padding: 10`
ocupa 200 puntos de ancho y deja 180 para el contenido. Ver [La caja](caja.md).

## Elementos y propiedades

Un elemento solo admite las propiedades que tienen sentido en él, y el motor lo comprueba al leer: `Columns` en un `Flex`
es un error, porque las columnas son de un `Grid`. Las propiedades de cualquier caja van en tres **grupos**:

| Grupo | Qué tiene | Se hereda |
|---|---|---|
| `Box` | Tamaño, márgenes, relleno, borde, fondo y esquinas | No |
| `Typography` | Fuente, tamaño, interlineado, alineación, color del texto… | **Sí**: lo que declara una caja vale para todo lo que contiene |
| `Break` | Cómo se corta la caja entre páginas | No |

```
Paragraph: Resultados del trimestre
	Box:
		Margin: 0 0 16 0
	Typography:
		Size: 20
		Weight: bold
	Break:
		After: avoid
```

Fuera de los grupos van las propiedades que definen el propio elemento: su `Id`, su `Class`, las columnas de una tabla, el
desplazamiento de una caja posicionada…

**Herencia.** Solo se hereda `Typography`. Si una caja dice `Size: 12`, todo el texto de dentro mide 12 salvo lo que diga
otra cosa. Por eso la fuente del documento se declara una vez, en la regla `Element: Document` de `Styles`, y cada elemento
declara solo lo que cambia.

## Cómo se colocan las cajas

Cada elemento coloca a sus hijos de una forma:

| Elemento | Coloca a sus hijos |
|---|---|
| `Block` (y casi todos los demás: secciones, encabezados, listas, celdas…) | **Uno debajo de otro**, cada uno con todo el ancho disponible |
| `Flex` | En una **fila** (o una columna), repartiéndose el espacio |
| `Grid` y `Table` | En una **rejilla** de filas y columnas |
| `Multicol` | En **columnas** por las que el contenido fluye |
| `Text` y `Paragraph` | No tienen hijos: reparten su texto en **líneas** |

Colocar las cajas una detrás de otra según estas reglas es el **flujo**. El ancho va de fuera adentro: una caja ocupa el
ancho que le deja su padre (o el que declare). El alto va de dentro afuera: una caja mide lo que mide su contenido (o lo que
declare).

**Fuera del flujo** están las cajas **posicionadas**: `Absolute` se coloca en un sitio exacto sin ocupar espacio, `Fixed`
se repite en cada página (cabeceras y pies) y `Relative` se desplaza sin mover a las demás
([posicionados](../componentes/relative-absolute-fixed.md)).

**El contenedor de referencia** de una caja es el rectángulo contra el que se miden sus porcentajes y sus
desplazamientos: para una caja en flujo, el espacio de contenido de su padre; para las posicionadas, el que diga su página.
`Width: 50%` es la mitad del ancho de su contenedor de referencia.

## Secciones

Todo el contenido va dentro de una o más **secciones** (`Section`). Una sección siempre empieza en una página nueva y puede
tener otro tamaño de página, otros márgenes, otras cabeceras y pies y otra numeración: una portada a sangre sin número de
página, el cuerpo numerado desde 1 y un anexo apaisado son tres secciones ([Document](../componentes/document.md#section)).

## Páginas y cortes

Cuando el contenido no cabe en una página, el motor lo **parte**: sigue en la página siguiente. Corta entre cajas, entre
líneas de un texto o entre filas de una tabla, y elige el mejor sitio: no deja una línea suelta de un párrafo al final de
una página, no separa un título de lo que le sigue y respeta lo que pidas con `Break`
([Páginas y cortes](paginas-y-cortes.md)).

## Estricto y predecible

El motor no adivina. Si pides algo que no tiene efecto, o dos cosas que se contradicen, es un **error**, con un código, la
línea del documento y un mensaje que dice cómo arreglarlo:

- una propiedad en un elemento que no la admite;
- un `Margin: auto` que no puede centrar nada;
- un porcentaje de un alto que no se conoce;
- texto que no cabe en su caja, una imagen más ancha que la página…

Lo mismo con lo que no cabe: en lugar de dejar que algo se salga de su caja o de la página, el motor lo dice. Así lo que
lees en el documento es lo que sale en el PDF, siempre igual ([Errores](errores.md)).

## Sin estilos ocultos

Ningún elemento tiene un aspecto por defecto que no se vea en el documento: un `Heading 1` no es más grande que un
`Paragraph` hasta que lo dices, una lista no tiene sangría hasta que se la das y un enlace no es azul. Lo único que el
motor hace por su cuenta es evitar cortar la página justo después de un título.

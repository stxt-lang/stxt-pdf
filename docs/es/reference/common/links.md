# Enlaces

Un enlace lleva a quien lo pulsa a una URL de fuera del documento o a otra caja de él. Se escribe de dos maneras, que
producen lo mismo en el PDF: la propiedad `Link` de una caja, que hace clicable la caja entera, y el enlace de Markdown
(`[texto](destino)`), que hace clicable una parte de una línea, en un `Markdown` o en un texto con `Markup: markdown`.

```
Text: Escribe a soporte@airemas.example
    Link: mailto:soporte@airemas.example

Image: examples/brand/nogal.png
    Link: https://tallernogal.example

Markdown:
    Content >>
        Consulta [las especificaciones](#specs) o [la web](https://airemas.example).
```

## Destino

Una URL `https://...`, `http://...` o `mailto:...` (con servidor o destinatario), o `#id`: el `Id` de una caja del documento.
Cualquier otra cosa es `LINK_SCHEME`, con la línea del `Link` o, en Markdown, la del bloque: un destino vacío, mal escrito,
sin servidor, con otro esquema (`javascript:`, `file:` o `tel:`) o una ruta relativa.

> **Decisión:** solo `https`, `http`, `mailto` y `#id`: un PDF no debería llevar un enlace que ejecute o lea algo en quien
> lo abre. *Alternativa descartada:* cualquier URI, con una lista de esquemas prohibidos.

## Destinos del documento

`#id` lleva a la página y a la esquina superior izquierda de la caja con ese `Id` (de su primer trozo, si está partida),
que puede estar antes o después del enlace. Es `LINK_UNKNOWN`, con la línea del `Link`, si no existe o si está en un
`Page overlay`, que se repite en todas las páginas y no tiene un único sitio. Una `Section` con `Id` lleva a su primera página. Un
encabezado de un `Markdown` es un destino si lleva `{#id}` ([Markdown](../components/markdown.md#encabezados)).

## Dónde se admite `Link`

En todos los elementos salvo `Markdown` (su contenido lleva sus propios enlaces) y `Table of contents` (sus entradas ya llevan el
suyo). Es del elemento, como `Id`: no va en `Box`, `Typography` ni `Break`, ni en las reglas de `Styles`, y no se hereda. Lo
que hay dentro de una caja con enlace es clicable por ella. Si además lleva enlace propio, las dos zonas se solapan y el
visor decide cuál se activa.

> **Decisión:** el enlace es del elemento: por regla de estilo, todas las cajas con ese estilo llevarían al mismo sitio, y
> heredado, un `Link` en `For element: Document` enlazaría todo el documento.

## Aspecto

Un enlace no cambia el aspecto de lo que enlaza: el color y el subrayado los da el autor con su estilo. La excepción es el
enlace de Markdown, que sale **subrayado** sin cambiar el color, porque no hay otra manera de dar estilo a una parte de una
línea. Se puede combinar con negrita y cursiva (`[la **web**](...)`). El enlace es una marca más del tramo
([Formato del texto](text-formatting.md#tramos)): no cambia cómo se miden las palabras ni las líneas.

> **Decisión:** sin aspecto por defecto, salvo el subrayado del de Markdown. *Alternativa descartada:* azul y subrayado
> siempre, o una regla `For element: Link`.

## Zonas

La zona clicable de una caja con `Link` es su **caja de borde**, en cada página por la que pasa. La de un enlace de
Markdown es el rectángulo de cada trozo de cada línea, del alto de la línea. Dos tramos seguidos con el mismo destino (una
parte en negrita dentro de un enlace) son una zona. Un `Page overlay` con `Link` es clicable en todas las páginas, y las filas
repetidas de una tabla, también.

## Accesibilidad

Un lector de pantalla anuncia cada enlace una vez, aunque ocupe varias líneas, con lo que enlaza: el texto de la caja (en
un `Block`, los de dentro unidos por espacios, sin el número de página de una entrada de índice) o el `Alt` de una
imagen. Sin texto, anuncia el destino. El de un `Page overlay` o el de las filas repetidas de una tabla también se anuncia una vez, en su sitio del
documento, aunque sea clicable en todas las páginas. Los enlaces se recorren con el tabulador en el orden de lectura. Cómo
queda en el PDF: [El PDF por dentro](../implementation/pdf.md#enlaces).

## Lo que no hay

- Enlaces con título, definiciones de enlaces aparte (`[x][ref]`) e imágenes dentro de un enlace de Markdown: una imagen
  clicable es una `Image` con `Link`.
- Un texto propio para el enlace distinto de lo que enlaza.

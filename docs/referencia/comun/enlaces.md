# Enlaces

Un enlace lleva a quien lo pulsa a una URL de fuera del documento o a otra caja de él. Se escribe de dos maneras, que
producen lo mismo en el PDF: la propiedad `Link` de una caja, que hace clicable la caja entera, y el enlace de Markdown
(`[texto](destino)`), que hace clicable una parte de una línea, en un `Markdown` o en un texto con `Inline: markdown`.

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

Una URL `https://…`, `http://…` o `mailto:…` (con servidor o destinatario), o `#id`: el `Id` de una caja del documento.
Cualquier otra cosa es un error (`LINK_SCHEME`, con la línea del `Link` o, en Markdown, la del bloque): un destino vacío,
mal escrito, sin servidor, con otro esquema (`javascript:`, `file:`, `tel:`…) o una ruta relativa. El motor no abre nada:
solo escribe la anotación, y un visor no se encuentra con esquemas que ejecuten o lean nada.

> **Decisión:** solo se admiten `https`, `http`, `mailto` y `#id`: ni `tel:`, ni `ftp:`, ni rutas relativas, ni
> `javascript:` ni `file:`. Un PDF no debería llevar un enlace que ejecute o lea algo en quien lo abre. *Alternativa
> descartada:* cualquier URI, con una lista de esquemas prohibidos.

> **Decisión:** `LINK_SCHEME` cubre también el destino mal escrito (vacío, `https://` sin servidor, `mailto:` sin
> destinatario, con espacios o una ruta relativa): la corrección es la misma y el mensaje dice cuál es el problema.
> *Alternativa descartada:* un código aparte para «no es una URL».

## Destinos del documento

`#id` debe ser el `Id` de una caja (`LINK_UNKNOWN` si no existe, con la línea del `Link`), que puede estar antes o después
del enlace, y que no esté en un `Fixed` ni dentro de uno, que se repite en todas las páginas y no tiene un único sitio
(`LINK_UNKNOWN`). En el PDF lleva a la página y a la esquina superior izquierda del primer fragmento de la caja, como un
marcador. Una `Section` con `Id` es un destino: su primera página. Un encabezado de un `Markdown` no tiene `Id`, así que no
se puede enlazar con `#id`; lo enlaza el índice que lo lista ([Contents](../componentes/contents.md)).

> **Decisión:** `#id` solo lleva a cajas con `Id` (y las entradas de un índice, a su encabezado), no a las de un `Fixed`:
> `LINK_UNKNOWN` para los dos casos, con mensajes distintos. *Alternativa descartada:* un código propio para el `Fixed`,
> como `REFERENCE_NOT_PLACEABLE`.

## Dónde se admite `Link`

En todos los elementos de caja salvo `Markdown` (su contenido lleva sus propios enlaces) y `Contents` (sus entradas ya
llevan el suyo): `Block`, `Flex`, `Grid`, `Text`, `Paragraph`, los encabezados, `List`, `List item`, `Table`, `Cell`,
`Multicol`, `Image`, `Relative`, `Absolute` y `Fixed`. No es una propiedad de `Box`, `Typography` ni `Break` y no va en las
reglas de `Styles`: es del elemento, como `Id`. No se hereda: lo que hay dentro de una caja con enlace es clicable por ella,
pero no lleva enlace propio. Si lo lleva, las dos zonas se solapan y el visor decide cuál se activa.

> **Decisión:** el enlace de una caja es una propiedad del elemento, no de un grupo: un enlace por regla de clase haría que
> todas las cajas con esa clase llevaran al mismo sitio, y heredado, un `Typography` con `Link` en `Element: Document`
> enlazaría todo el documento. *Alternativa descartada:* una propiedad de `Typography`, o solo en `Text`, `Paragraph`,
> `Cell` e `Image`.

## Aspecto

Un enlace no cambia el aspecto de lo que enlaza: el color y el subrayado los da el autor con su estilo. La excepción es el
enlace de Markdown, que sale **subrayado**, porque no hay otra manera de darle estilo a una parte de una línea. El
subrayado es el de un tramo ([Formato del texto](formato-del-texto.md#tramos)) y se puede combinar con negrita y cursiva
(`[la **web**](…)`).

> **Decisión:** un enlace no tiene aspecto por defecto, salvo el de Markdown, que se subraya sin cambiar el color.
> *Alternativa descartada:* azul y subrayado siempre, o una regla `Element: Link`.

> **Decisión:** un enlace de Markdown es una marca más del tramo, con su destino: dos enlaces con destinos distintos no se
> unen en un tramo aunque el resto del formato sea igual, y un destino igual en dos tramos seguidos sí (una parte en
> negrita dentro de un enlace es una sola zona). No cambia cómo se miden las palabras ni las líneas.

## Zonas

La zona clicable de una caja con `Link` es su **caja de borde**, una por fragmento: una caja partida entre páginas es
clicable en cada página. La de un enlace de Markdown es el rectángulo de cada tramo de cada línea, del alto de la línea: un
enlace que ocupa dos líneas tiene dos zonas, y los tramos seguidos con el mismo destino son una. Un `Fixed` con `Link` es
clicable en todas las páginas; las filas repetidas de una tabla, también. Las zonas se calculan al pintar: el layout no
sabe nada de los enlaces.

## PDF

Cada zona es una anotación `Link` sin borde, con la acción `URI` o el destino de la página, y como texto (`Contents`) lo
que dice la caja o el tramo, para quien no lo ve: el texto de la caja y de lo que contiene unido por espacios, o el `Alt`
de una imagen; sin texto, el propio destino. Los destinos internos se resuelven al final, así que un enlace puede ir hacia
delante.

> **Decisión:** el texto de la anotación es el texto de la caja (el de un `Block` con varios textos, unidos por un
> espacio; el número de página de una entrada de índice no cuenta) o el `Alt` de la imagen; sin texto, el propio destino.
> Las referencias y los marcadores de página del texto salen sin sustituir. *Alternativa descartada:* un `Alt` propio en
> `Link`.

### PDF etiquetado

El elemento `Link` envuelve lo que se pulsa y lleva cada anotación como referencia a objeto (`OBJR`), como pide PDF/UA:

- *Caja con `Link`:* el `Link` es hijo del elemento de la caja y contiene su contenido: el texto de la caja y los elementos
  de lo que contiene (`P → Link → texto`; `Div → Link → P, P`; `TOCI → Link → título, número`). Una imagen es la
  excepción: su `Link` es el padre de su `Figure` (`Link → Figure`). Una caja partida entre páginas tiene un solo `Link`
  con una `OBJR` por fragmento.
- *Enlace dentro del texto:* el `Link` es hijo del elemento del texto (`P → texto, Link, texto`) y contiene el contenido
  marcado de las palabras del enlace. Es **uno por enlace y fragmento**, aunque ocupe varias líneas: un contenido marcado y
  una `OBJR` por línea. Lo que va antes y después del enlace es otro contenido marcado del mismo párrafo.
- *`Alt`:* un `Link` que contiene algo que leer (texto o una figura con su `Alt`) no lleva `Alt`, porque lo leería un
  lector de pantalla en lugar del contenido; uno sin nada dentro lo lleva con el texto del enlace.
- *Lo que no es contenido* (lo que hay dentro de un `Fixed`, una imagen decorativa y las filas repetidas de una tabla desde
  su segunda página) no tiene a qué envolver: su anotación va a un `Link` hijo del `Document`, sin contenido y con el texto
  del enlace como `Alt`. Un `Fixed` con una web en el pie se anuncia así en cada página.
- Las páginas con anotaciones llevan `Tabs: S`, que recorre las anotaciones en el orden de la estructura.

> **Decisión:** el `Link` del PDF etiquetado envuelve lo que se pulsa: el contenido de la caja, o el contenido marcado de
> las palabras del enlace, que se pinta en secuencias aparte para poder envolverlas. Una imagen es la excepción porque una
> `Figure` no puede contener a su `Link`. *Alternativa descartada:* un `Link` con solo la referencia a la anotación, que no
> cumple PDF/UA.

> **Decisión:** un enlace de varias líneas es un solo `Link` por enlace y fragmento, con un contenido marcado y una `OBJR`
> por línea (las anotaciones siguen siendo una por línea, que es lo que el visor necesita): un lector de pantalla anuncia
> un enlace, no uno por línea. Dos enlaces seguidos con el mismo destino son uno. *Alternativa descartada:* un `Link` por
> línea.

> **Decisión:** un `Link` que contiene algo que leer no lleva `Alt`: el lector de pantalla lo leería en lugar del
> contenido. El texto de la anotación, que PDF/UA exige, se queda siempre.

> **Decisión:** los enlaces de lo que no es contenido van a un `Link` suelto, hijo del `Document`, con su `Alt` y sin
> contenido marcado, porque PDF/UA exige que toda anotación de enlace esté en un `Link`. El pie de cada página con su web
> se anuncia en cada una. *Alternativa descartada:* marcar esas anotaciones como ocultas, que las quita del ratón.

> **Decisión:** las páginas con anotaciones llevan `Tabs: S`, para que el visor recorra los enlaces en el orden de la
> estructura, como pide PDF/UA.

## Volcado

Los enlaces no se vuelcan ([Pruebas](../implementacion/pruebas.md#el-volcado)).

## Lo que no hay

- Enlaces con título, definiciones de enlaces aparte (`[x][ref]`) e imágenes dentro de un enlace de Markdown: una imagen
  clicable es una `Image` con `Link`.
- Enlazar a mano un encabezado de un `Markdown`, que no tiene `Id`.
- El `Link` suelto de un `Fixed` se anuncia al final de la estructura, no en su sitio de lectura.

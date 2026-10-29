# `Image`

Una `Image` pone en el documento una imagen PNG, JPEG o SVG de la carpeta `images/` de los recursos
([Recursos](../comun/recursos.md)). Es una caja sin hijos, como un `Text`, cuyo contenido es la imagen: se coloca, se
dimensiona, se corta entre páginas y se pinta como cualquier caja, con su `Box` (márgenes, relleno, borde, fondo y
esquinas) y su `Break` (sin `Inside` ni `Decoration`, porque no se parte). Admite además `Id`, `Class` y `Link`; no admite
`Typography`, porque no tiene texto.

```
Document (dev.stxt.boxes):
	Language: es
	Title: Informe
	Media:
		Paper: A4
		Margin: 40
	Section:
		Fixed:
			Offset: -30 0 auto auto
			Image: test/square.png
				Box:
					Width: 20
		Image: test/wide.png
			Alt: Un paisaje con montañas y un sol
			Box:
				Width: 100%
				Margin: 0 0 10 0
		Flex:
			Gap: 10
			Item:
				Image: test/tall.jpg
					Alt: Un retrato
					Box:
						Height: 60
			Item:
				Grow: 1
				Image: test/wide.png
					Fit: cover
					Box:
						Height: 60
```

(El ejemplo usa las imágenes de prueba del repositorio, en `resources/images/test/`: un logotipo en la esquina de cada
página, una imagen a todo el ancho y, debajo, un retrato de 45 × 60 y la misma imagen recortada a lo que queda de ancho.)

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| *(valor)* | ruta relativa a `images/` | — (obligatorio) | El fichero PNG, JPEG o SVG: `Image: logos/acme.png`. |
| `Alt` | texto no vacío | — | Texto alternativo: con él, la imagen es una figura del PDF etiquetado; sin él, es decorativa. |
| `Fit` | `contain`, `cover`, `fill` | `contain` | Cómo llena la imagen su caja de contenido si tienen proporciones distintas. |
| `Rotate`, `Rotate origin` | ver [Capas y giro](../comun/capas-y-giro.md#giro-rotate) | — | Gira la imagen al pintarla (un sello, una etiqueta en diagonal). |

No van en las reglas de `Styles`, que solo admiten los grupos.

## Fichero

El valor es la ruta del fichero, relativa a `images/` y siempre hacia abajo ([Recursos](../comun/recursos.md)):
`Image: logos/acme.png` es `resources/images/logos/acme.png`. Se admiten PNG (con transparencia), JPEG y SVG, y el formato
se reconoce por el contenido, no por la extensión (un SVG es un fichero que empieza por `<`). Una `Image` sin ruta, un
fichero que no existe o una ruta que sale de `images/` son un error (`IMAGE_UNKNOWN`); un fichero que no es PNG, JPEG ni un
SVG admitido (GIF, WebP…) o que no se puede leer, también (`IMAGE_FORMAT`). Los ficheros se comprueban antes de maquetar,
con las fuentes, y los errores señalan la línea de la `Image`.

> **Decisión:** una `Image` sin ruta es un error al construir el árbol; que el fichero exista y sea legible se comprueba
> antes de maquetar, con cualquier medidor de texto, porque el tamaño natural hace falta para el layout. Un `Alt` vacío es
> `VALUE_NOT_ALLOWED`. *Alternativa descartada:* comprobar los ficheros solo al escribir el PDF, o aceptar el `Alt` vacío
> como decorativa.

> **Decisión:** el formato se reconoce por el contenido; un XML que no es SVG (otra raíz, o sin el espacio de nombres de
> SVG) es `IMAGE_FORMAT` con un mensaje propio. *Alternativa descartada:* exigir la extensión `.svg`.

## Tamaño natural

Cada píxel mide 0,75 pt (96 píxeles por pulgada), sin mirar la resolución que declare el fichero: una imagen de
200 × 100 píxeles mide 150 × 75 pt. Para otro tamaño se declara `Width` o `Height`. El de un SVG sale de su `width` y
`height` (en `px`, `pt`, `pc`, `in`, `cm` o `mm`; sin unidad, píxeles) o, si faltan, de su `viewBox`, también a 96 píxeles
por pulgada; con solo uno de los dos lados y un `viewBox`, el otro sale de su proporción. Sin tamaño, o con medidas
relativas (`%`, `em`), un SVG no tiene tamaño natural y es un error (`IMAGE_FORMAT`).

> **Decisión:** el tamaño natural de un SVG sale de `width` y `height` con unidad absoluta o, si faltan, del `viewBox`; en
> `%` o `em` no hay tamaño natural. *Alternativa descartada:* exigir `width`, `height` y `viewBox`.

## Dimensionado

La caja de contenido de una imagen tiene la **proporción** del fichero, o la de `Aspect ratio` si se declara, que la
sustituye (no es un error). El relleno y el borde van fuera: no la deforman.

- Con `Width` y `Height` en `auto`, mide su tamaño natural, **también en flujo de bloque**: no se estira al ancho del padre.
  Si no cabe, es un desbordamiento (`BOX_OVERFLOW`); para que ocupe el ancho disponible, se declara `Width: 100%`.
  `Margin: 0 auto` la centra, porque no ocupa todo el ancho.
- Con un eje declarado, o impuesto por el padre (un ítem de `Flex` con `Grow`, una columna de `Grid` con
  `Horizontal: stretch`), el otro sale de la proporción. Con los dos, la caja mide lo declarado y `Fit` decide cómo la llena
  la imagen.
- `Min width`, `Max width`, `Min height` y `Max height` se aplican como en cualquier caja. Si los dos ejes son `auto` y el
  alto choca con su mínimo o su máximo, el ancho se vuelve a deducir del alto limitado: la proporción se conserva.
- Su alto es siempre definido, así que un `Flex` no la estira en el eje cruzado de una fila, ni un `Grid` en vertical; en
  horizontal sí se estira, y el alto sigue a la proporción.
- **Tamaños intrínsecos:** min-content y max-content son el ancho natural o, si declara `Height` en puntos, el que sale de
  él con la proporción. Es un mínimo de verdad: en una pista `1fr` de grid o en un ítem de `Flex` sin `Min width`, la imagen
  no encoge por debajo de él.

> **Decisión:** la proporción es la de la caja de contenido, así que el relleno y el borde no deforman la imagen; un
> `Aspect ratio` declarado sustituye a la del fichero. *Alternativa descartada:* la caja de borde, como en las demás cajas.

> **Decisión:** el alto de una imagen es siempre definido, porque sale de la proporción: no se estira en el eje cruzado de
> un `Flex` en fila ni en vertical en un `Grid`. *Alternativa descartada:* estirarla y dejar que `Fit` la coloque.

> **Decisión:** con los dos ejes `auto`, los mínimos y los máximos se aplican al ancho, el alto sale de la proporción y se
> limita, y si ha cambiado, el ancho se vuelve a deducir de él; si no se pueden cumplir a la vez con la proporción, gana el
> ancho y `Fit` coloca la imagen. *Alternativa descartada:* una tabla completa de casos para cada combinación de límites.

> **Decisión:** los tamaños intrínsecos de una imagen son un mínimo de verdad, también con un `Width` en porcentaje: la
> imagen no encoge por debajo de su ancho natural y, si no cabe, es un desbordamiento. *Alternativa descartada:* un
> min-content de 0 con `Width` en porcentaje, que deja encoger la imagen.

> **Decisión:** `Margin: 0 auto` centra una `Image` sin `Width`, porque mide su ancho natural y no ocupa todo el ancho: es
> la excepción a `MARGIN_AUTO_WITHOUT_EFFECT` en flujo de bloque.

## `Fit`

Si la caja de contenido y la imagen tienen proporciones distintas, `contain` (por defecto) dibuja la imagen entera, lo más
grande que cabe y centrada, y deja ver el fondo a los lados; `cover` la dibuja lo más pequeña que cubre la caja, centrada,
y recorta lo que sobra; `fill` la estira a la caja y la deforma. Con la misma proporción, las tres dan lo mismo. Una imagen
se recorta además con las esquinas redondeadas de su caja de contenido ([La caja](../comun/caja.md#esquinas)).

## Cortes de página

Una imagen no se parte: si no cabe en lo que queda de la página (o de la columna de un `Multicol`), pasa entera a la
siguiente, como una línea de texto. Si no cabe ni en una página entera, es `PAGE_OVERFLOW`, con la línea de su `Height` (o,
sin él, de su `Width`). `Before` y `After` funcionan como en cualquier caja; `Inside` y `Decoration`, que solo cuentan al
partir una caja, son un error (`BREAK_IN_IMAGE`), también si los da una regla de `Styles`.

## Dónde va

Donde va un elemento en flujo: en una sección, `Block`, `Relative`, `Absolute`, `Fixed` (un logotipo que se repite en cada
página), `Multicol`, `List item` (pero no al principio: el marcador necesita una línea con la que alinearse,
`LIST_ITEM_WITHOUT_LINE`), `Cell` y como elemento de un `Item` o un `Area`. En un `Markdown`, una imagen sola en su párrafo
(`![Alt](ruta)`) es una `Image` ([Markdown](markdown.md)).

## SVG

Un SVG es un dibujo vectorial: el PDF lo lleva como rutas, rellenos, degradados y recortes, no como píxeles, así que se ve
nítido a cualquier tamaño y pesa poco; se dimensiona, se coloca (`Fit` incluido) y se corta como cualquier imagen. Es un
fichero XML en el espacio de nombres de SVG, y se admite un subconjunto cerrado: `svg` (sin otro `svg` dentro), `g`, `defs`,
`title`, `desc`, `rect`, `circle`, `ellipse`, `line`, `polyline`, `polygon`, `path`, `linearGradient`, `radialGradient`,
`stop`, `clipPath`, `use`, `text` y `tspan`, con sus atributos de presentación (rellenos y trazos lisos o con degradado,
opacidad, `transform`, recortes). Es un error (`IMAGE_FORMAT`, con la línea del SVG en el mensaje) cualquier otro elemento
(scripts, animación, filtros, máscaras, patrones, marcadores, `image`, `style`, `symbol`, `metadata`…), una propiedad con
efecto fuera del subconjunto (`filter`, `mask`, `marker`), un atributo de otro espacio de nombres (salvo `xlink` y `xml`) o
un manejador de eventos (`onclick`…): nada se ignora en silencio. Tampoco se admite nada de fuera del fichero: un `href` o un
`url(…)` solo pueden llevar a un `#id` del propio SVG, y un DTD o una entidad son un error.

El texto de un SVG (`text`, `tspan`) usa solo las fuentes de `fonts/`, sin sustitución: su `font-family` (solo cuenta la
primera de la lista, y no puede ser una genérica como `sans-serif`), su `font-weight` (`normal` o `bold`) y su `font-style`
(`normal` o `italic`), propios o heredados de sus antepasados en el fichero (un `g`, o el `text` de un `tspan`), deben ser
una variante que exista, con todos los caracteres del texto. Esa variante es la que lo dibuja, siempre: un texto que repite
un `use` conserva la fuente de su sitio en el fichero y no hereda la del `use`, y el resultado no depende del idioma de la
JVM ni de otras carpetas de recursos con una familia del mismo nombre. Se dibuja con los contornos de la fuente: se ve como
cualquier texto del documento, pero no se puede seleccionar ni se extrae del PDF.

> **Decisión:** el subconjunto de SVG es cerrado y estricto, con la línea del SVG en el mensaje: nada se ignora en silencio.
> El precio es que un SVG exportado de un editor suele necesitar limpiarse. *Alternativa descartada:* ignorar lo que no se
> dibuja, o admitir los metadatos y los atributos de los editores como inertes.

> **Decisión:** las referencias de un SVG son solo internas: un `href` o un `url(…)` que no empiece por `#`, un DTD, una
> entidad o una inclusión son errores, y además la librería de dibujo carga sin acceso a nada de fuera.

> **Decisión:** el texto de un SVG usa la primera familia de `font-family`, sin sustitución, y no puede ser una genérica;
> sin `font-family` es un error, porque no hay fuente por defecto. *Alternativa descartada:* probar las familias de la
> lista en orden.

> **Decisión:** el texto de un SVG no es texto del PDF: la librería de dibujo lo pinta como contornos con la fuente de
> `fonts/`, así que se ve igual pero no se puede seleccionar. Lo habitual en un documento accesible es que el texto vaya en
> el documento, el SVG sea un dibujo y `Alt` dé el texto alternativo. *Alternativa descartada:* escribirlo como texto del PDF,
> que la librería de dibujo no permite.

> **Decisión:** el texto de un SVG se dibuja con el fichero de fuente que se comprobó, sin darlo de alta en el sistema: así
> no depende del idioma de la JVM ni de otra carpeta de recursos con una familia del mismo nombre. Un texto que repite un
> `<use>` conserva la fuente de su sitio en el fichero, de modo que lo que se dibuja es lo que se comprobó.
> *Alternativa descartada:* que el motor dibujara el texto de los SVG con sus propias fuentes, que obligaba a reimplementar
> la disposición del texto de SVG.

## Límites

Una imagen no puede pedir más de lo que admiten los [límites](../comun/limites.md): un PNG o un JPEG, más píxeles que
`maxImagePixels` (`IMAGE_TOO_LARGE`), y un SVG, más elementos o niveles que `maxSvgElements` y `maxSvgDepth`
(`SVG_TOO_COMPLEX`), siempre con la línea de la `Image` y antes de maquetar. Un `<use>` cuenta todo lo que repite cada vez
que se usa, y un `<use>` que apunta a un elemento que lo contiene es `IMAGE_FORMAT`.

## Accesibilidad

Con `Alt`, un texto no vacío, la imagen es un elemento `Figure` del PDF etiquetado con ese texto alternativo y su `BBox` (su
caja de contenido en la página). Sin `Alt` es decorativa y se pinta como un *artifact*, igual que los fondos. Dentro de un
`Fixed` es siempre un *artifact*, y en las filas repetidas de una tabla, solo es contenido la primera vez.

> **Decisión:** el `BBox` de la `Figure` es la caja de contenido de la imagen, no la parte visible con `contain`.
> *Alternativa descartada:* el rectángulo dibujado.

> **Decisión:** una `Image` sin `Alt` es decorativa, sin aviso. *Alternativa descartada:* hacer `Alt` obligatorio, con un
> `Alt` vacío para las decorativas, si un olvido del autor resulta ser un problema.

## Salida

El PDF incrusta cada fichero una sola vez, aunque se use en varias páginas, con la resolución del fichero: el JPEG tal cual
y el PNG sin pérdida y con su transparencia; un SVG, como un dibujo vectorial del tamaño del SVG que se escala a su caja. El
SVG de depuración la incrusta como `data:`. En el volcado es una caja más.

> **Decisión:** un SVG es un formulario del PDF de su tamaño natural, dibujado una vez por fichero, que se escala a su caja
> con `Fit` como un PNG (`fill` deforma el dibujo).

## Lo que no hay

- GIF, WebP, TIFF y otros formatos; imágenes dentro de una línea de texto; varias resoluciones de una imagen.
- Colocar la imagen dentro de su caja en otro sitio que el centro, o a su tamaño natural sin escalar.
- En los SVG: patrones, máscaras, filtros, marcadores, imágenes, hojas de estilo y enlaces a otros ficheros.

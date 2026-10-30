# `Image`

Una `Image` pone en el documento una imagen PNG, JPEG o SVG, o una página de un PDF, de la carpeta `images/` de los recursos
([Recursos](../comun/recursos.md)). Es una caja sin hijos, como un `Text`, cuyo contenido es la imagen: se coloca, se
dimensiona, se corta entre páginas y se pinta como cualquier caja, con su `Box` (márgenes, relleno, borde, fondo y
esquinas) y su `Break` (sin `Inside` ni `Edges`, porque no se parte). Admite además `Id`, `Style` y `Link`; no admite
`Typography`, porque no tiene texto.

```
Document (dev.stxt.pdf):
	Language: es
	Title: Informe
	Page:
		Paper: A4
		Margin: 40
	Section:
		Page overlay:
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
| *(valor)* | ruta relativa a `images/` | — (obligatorio) | El fichero PNG, JPEG, SVG o PDF: `Image: logos/acme.png`. |
| `Alt` | texto no vacío | — | Texto alternativo: con él, la imagen es una figura que se lee ([accesibilidad](#accesibilidad)); sin él, es decorativa. |
| `Fit` | `contain`, `cover`, `stretch` | `contain` | Cómo llena la imagen su caja de contenido si tienen proporciones distintas. |
| `Pdf page` | entero ≥ 1 | `1` | La página que se dibuja de un PDF ([PDF](#pdf)). En otro formato es un error. |
| `Rotate` | ver [Capas y giro](../comun/capas-y-giro.md#giro-en-flujo-block-text-paragraph-e-image) | — | Gira la imagen sobre su centro (un sello); ocupa el rectángulo que la envuelve girada. |

No van en las reglas de `Styles`, que solo admiten los grupos.

## Fichero

El valor es la ruta del fichero, relativa a `images/` y siempre hacia abajo ([Recursos](../comun/recursos.md)):
`Image: logos/acme.png` es `resources/images/logos/acme.png`. Se admiten PNG (con transparencia), JPEG, SVG y PDF, y el
formato se reconoce por el contenido, no por la extensión (un SVG es un fichero que empieza por `<`, y un PDF, uno que
empieza por `%PDF-`). Una `Image` sin ruta, un fichero que no existe o una ruta que sale de `images/` son un error
(`IMAGE_UNKNOWN`); un fichero que no es PNG, JPEG, PDF ni un SVG admitido (GIF, WebP…) o que no se puede leer, también
(`IMAGE_FORMAT`). Los errores señalan la línea de la `Image`.

Un XML que no es un SVG es también `IMAGE_FORMAT`. Un `Alt` vacío es `VALUE_NOT_ALLOWED`.

> **Decisión:** el formato se reconoce por el contenido. *Alternativa descartada:* exigir la extensión `.svg`.

## Tamaño natural

Cada píxel mide 0,75 pt (96 píxeles por pulgada), sin mirar la resolución que declare el fichero: una imagen de
200 × 100 píxeles mide 150 × 75 pt. Para otro tamaño se declara `Width` o `Height`. El de un SVG sale de su `width` y
`height` (en `px`, `pt`, `pc`, `in`, `cm` o `mm`; sin unidad, píxeles) o, si faltan, de su `viewBox`, también a 96 píxeles
por pulgada; con solo uno de los dos lados y un `viewBox`, el otro sale de su proporción. Sin tamaño, o con medidas
relativas (`%`, `em`), un SVG no tiene tamaño natural y es un error (`IMAGE_FORMAT`). Un PDF ya está en puntos: su tamaño
natural es el de la página que se dibuja, el de su recorte (lo que enseña un visor) y girado si la página lo está ([PDF](#pdf)).

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
- **Tamaños intrínsecos:** el ancho más estrecho y el natural son los dos el ancho de su tamaño natural o, si declara
  `Height` en puntos, el que sale de él con la proporción. Es un mínimo de verdad: en una pista `*` de grid o en un ítem de `Flex` sin `Min width`, la imagen
  no encoge por debajo de él.

Si los mínimos y los máximos no se pueden cumplir a la vez con la proporción, gana el ancho y `Fit` coloca la imagen.

> **Decisión:** el alto de una imagen sale siempre de la proporción. *Alternativa descartada:* estirarla y dejar que `Fit`
> la coloque.

> **Decisión:** los tamaños intrínsecos son un mínimo de verdad, también con un `Width` en porcentaje. *Alternativa
> descartada:* un ancho más estrecho de 0, que deja encoger la imagen.

## `Fit`

Si la caja de contenido y la imagen tienen proporciones distintas, `contain` (por defecto) dibuja la imagen entera, lo más
grande que cabe y centrada, y deja ver el fondo a los lados; `cover` la dibuja lo más pequeña que cubre la caja, centrada,
y recorta lo que sobra; `stretch` la estira a la caja y la deforma. Con la misma proporción, las tres dan lo mismo. Una imagen
se recorta además con las esquinas redondeadas de su caja de contenido ([La caja](../comun/caja.md#esquinas)).

## Cortes de página

Una imagen no se parte: si no cabe en lo que queda de la página (o de la columna de un `Columns`), pasa entera a la
siguiente, como una línea de texto. Si no cabe ni en una página entera, es `PAGE_OVERFLOW`, con la línea de su `Height` (o,
sin él, de su `Width`). `Before` y `After` funcionan como en cualquier caja; `Inside` y `Edges`, que solo cuentan al
partir una caja, son un error (`BREAK_IN_IMAGE`), también si los da una regla de `Styles`.

## Dónde va

Donde va un elemento en flujo: en una sección, `Block`, `Frame`, `Overlay`, `Page overlay` (un logotipo que se repite en cada
página), `Columns`, `List item` (pero no al principio: el marcador necesita una línea con la que alinearse,
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
un `use` conserva la fuente de su sitio en el fichero y no hereda la del `use`. Se dibuja con los contornos de la fuente: se
ve como cualquier texto del documento, pero no se puede seleccionar ni copiar.

> **Decisión:** el subconjunto de SVG es cerrado y estricto: nada se ignora en silencio, a cambio de que un SVG exportado
> de un editor suela necesitar limpiarse. *Alternativa descartada:* ignorar lo que no se dibuja, o los metadatos de los
> editores.

> **Decisión:** el texto de un SVG usa la primera familia de `font-family`, sin sustitución; sin `font-family` es un error.
> *Alternativa descartada:* probar las familias de la lista en orden.

> **Decisión:** el texto de un SVG no es texto del PDF: se ve igual, pero no se puede seleccionar. En un documento
> accesible, el texto va en el documento, el SVG es un dibujo y `Alt` da el texto alternativo.

## PDF

Una `Image` de un PDF dibuja **una de sus páginas**, la de `Pdf page` (la primera, si no se declara), como una imagen más: se
dimensiona, se coloca, se recorta y se gira como cualquier otra. Sirve para poner en el documento un plano, un gráfico o
una ilustración que vienen en PDF, o un dibujo que el subconjunto de [SVG](#svg) no admite, convertido a PDF; y como
**fondo de página**: un papel con membrete, un formulario preimpreso, en un `Page overlay` detrás del contenido.

La página va como dibujo vectorial, con su contenido tal cual: sus trazados, sus imágenes y su texto, que se ve nítido a
cualquier tamaño y **se puede seleccionar y copiar**, con sus fuentes. Cada página se copia una vez en el PDF, aunque se
use en varias páginas del documento. Lo que no es contenido de la página no se copia:

- **Las anotaciones.** Un enlace de la página no se copia (lo que se ve de él, el texto, sí). Una anotación que se ve, como
  un campo de formulario relleno, un comentario o un sello, no se dibujaría, así que es un error (`IMAGE_FORMAT`): el PDF
  se aplana antes de usarlo, por ejemplo imprimiéndolo a PDF. Quien convierte puede quitar esta comprobación
  (`Check.PDF_IMAGE_ANNOTATIONS`, [Uso desde Java](../implementacion/api.md#comprobaciones)), y entonces la página se
  dibuja sin ellas.
- **El etiquetado.** La página es una imagen: con `Alt` es una figura y un lector de pantalla anuncia ese texto, no el de
  la página; sin `Alt` es decoración, como un fondo ([Accesibilidad](#accesibilidad)). Tampoco se copian sus marcadores.

Las **capas** de la página (su contenido opcional) sí se copian, porque deciden lo que se ve: una capa apagada sigue
apagada, y el visor las enseña en su lista de capas. Una configuración de las capas sin nombre recibe el del fichero
(`planos.pdf`), que PDF/UA pide.

`Pdf page` es la página física, desde 1, no el número que lleva impreso. Una página que el PDF no tiene es un error
(`IMAGE_PAGE`, con la línea de `Pdf page`), y `Pdf page` en un PNG, un JPEG o un SVG, que no tiene páginas, también. Es un error
(`IMAGE_FORMAT`, con la línea de la `Image`) un PDF cifrado, aunque se abra sin contraseña, y una página que usa una
fuente sin incrustar, o sin `ToUnicode` (lo que dice qué carácter es cada glifo; una fuente simple que no es simbólica no
lo necesita): el visor la sustituiría por otra, o su texto no se podría copiar ni leer en voz alta, y el documento no
sería accesible. Quien convierte puede quitar esta comprobación (`Check.PDF_IMAGE_FONTS`), y el PDF puede no cumplir
PDF/UA. La de un PDF cifrado no se puede quitar. Un PDF de más bytes que el límite es `PDF_TOO_LARGE` ([Límites](#límites)).

Un papel de carta con el membrete completo en la primera página y el logotipo pequeño en las demás (las páginas 1 y 2 de
`letterhead.pdf`), detrás del contenido y a sangre: un `Page overlay` con `Layer` negativa que sale del área útil con un
`Offset` negativo igual al margen, y una imagen de `Width` y `Height` al 100 % de él. Los márgenes del documento dejan el
sitio del membrete y del pie, que el contenido no puede pisar:

```
Document (dev.stxt.pdf):
	Language: es
	Title: Carta
	Page:
		Paper: A4
		Margin: 140 72 80 72
	Styles:
		For element: Document
			Typography:
				Font: IBM Plex Serif
				Size: 10.5
				Line height: 16
		For style: letterhead
			Box:
				Width: 100%
				Height: 100%
	Section:
		Page overlay:
			Pages: first
			Offset: -140 -72 -80 -72
			Layer: -1
			Image: examples/letterhead/letterhead.pdf
				Style: letterhead
		Page overlay:
			Pages: not-first
			Offset: -140 -72 -80 -72
			Layer: -1
			Image: examples/letterhead/letterhead.pdf
				Pdf page: 2
				Style: letterhead
		Paragraph: Olot, 20 de octubre de 2025
		Paragraph: Le enviamos la propuesta de renovación del mobiliario del comedor.
		Paragraph: Las condiciones generales van en la página siguiente.
			Break:
				After: page
		Paragraph: Condiciones generales de la propuesta.
```

> **Decisión:** una página de un PDF es una imagen, con todo lo de una imagen, y un papel con membrete es un `Page overlay` con
> esa imagen. *Alternativa descartada:* un elemento propio para insertar páginas enteras de otro PDF en el documento, o un
> fondo de página propio de la sección.

> **Decisión:** se copia el contenido de la página, sin su etiquetado: la `Image` lo etiqueta como figura o como
> decoración. *Alternativa descartada:* injertar el árbol de estructura del PDF de origen, que no siempre existe ni encaja
> en el del documento.

> **Decisión:** una anotación que se ve es un error, no se dibuja su apariencia. *Alternativa descartada:* aplanarla al
> copiarla, que obliga a interpretar cada tipo de anotación; ignorarla en silencio.

> **Decisión:** lo que impide que la página se vea igual o sea accesible (fuentes, anotaciones) se comprueba por
> defecto, y quien convierte puede quitarlo; lo que impide leerla (cifrado, dañado), no. *Alternativa descartada:* una
> propiedad del documento, que pone en el documento una decisión de quien convierte.

> **Decisión:** de las reglas de PDF/UA sobre las fuentes se comprueban las que más fallan en los PDF de otros programas:
> que estén incrustadas y que tengan `ToUnicode`. *Alternativa descartada:* comprobar todas, que es repetir un validador
> de PDF/UA.

## Límites

Una imagen no puede pedir más de lo que admiten los [límites](../comun/limites.md): un PNG o un JPEG, más píxeles que
`maxImagePixels` (`IMAGE_TOO_LARGE`), y un SVG, más elementos o niveles que `maxSvgElements` y `maxSvgDepth`
(`SVG_TOO_COMPLEX`), y un PDF, más bytes que `maxPdfBytes` (`PDF_TOO_LARGE`, 20 MB por defecto), siempre con la línea de
la `Image` y antes de maquetar. Un `<use>` cuenta todo lo que repite cada vez
que se usa, y un `<use>` que apunta a un elemento que lo contiene es `IMAGE_FORMAT`.

## Accesibilidad

Con `Alt`, un texto no vacío, la imagen es una figura que un lector de pantalla anuncia con ese texto. Sin `Alt` es
decorativa y no se lee, igual que los fondos; dentro de un `Page overlay`, siempre, y en las filas repetidas de una tabla, solo
la primera vez. Una página de un PDF, igual: su texto se puede seleccionar, pero el lector de pantalla anuncia el `Alt`,
no lo lee.

> **Decisión:** una `Image` sin `Alt` es decorativa, sin aviso. *Alternativa descartada:* hacer `Alt` obligatorio, con un
> `Alt` vacío para las decorativas, si un olvido del autor resulta ser un problema.

## Salida

El PDF lleva cada fichero una sola vez, aunque se use en varias páginas, con la resolución del fichero: el JPEG tal cual y
el PNG sin pérdida y con su transparencia. Un SVG o una página de un PDF se escalan a su caja como un PNG, pero siguen
siendo un dibujo vectorial nítido a cualquier tamaño (`stretch` deforma el dibujo); una página de un PDF se copia una vez,
aunque salga en varias páginas.

## Lo que no hay

- GIF, WebP, TIFF y otros formatos; imágenes dentro de una línea de texto; varias resoluciones de una imagen.
- Colocar la imagen dentro de su caja en otro sitio que el centro, o a su tamaño natural sin escalar.
- En los SVG: patrones, máscaras, filtros, marcadores, imágenes, hojas de estilo y enlaces a otros ficheros.
- En los PDF: insertar sus páginas como páginas del documento, con su tamaño; sus enlaces, campos de formulario,
  comentarios, marcadores y etiquetado.

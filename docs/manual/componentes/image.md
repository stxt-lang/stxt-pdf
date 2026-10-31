# `Image`

Una imagen PNG, JPEG o SVG, o una página de un PDF, de la carpeta `images/` de los recursos. Es una caja como cualquier
otra: tiene su tamaño, sus márgenes, su borde, sus esquinas, y se coloca en el flujo o en un `Flex`, un `Grid` o un
posicionado.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Catálogo
    Page:
        Paper: A4
        Margin: 40
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
    Section:
        Image: examples/brand/nogal.svg
            Alt: Taller Nogal
            Box:
                Width: 120
                Margin: 0 0 20 0
        Grid:
            Column sizes: * *
            Gap: 16
            Area:
                Image: examples/catalog/silla.jpg
                    Alt: Silla de nogal con asiento de lana
                    Fit: cover
                    Box:
                        Height: 160
                        Corner radius: 6
            Area:
                Image: examples/catalog/mesa.jpg
                    Alt: Mesa de comedor de nogal
                    Fit: cover
                    Box:
                        Height: 160
                        Corner radius: 6
```

## Propiedades

| Propiedad | Qué hace |
|---|---|
| *(valor)* | La ruta del fichero dentro de `images/`: `Image: logos/acme.png`. Obligatoria |
| `Alt` | El texto alternativo, para quien no ve la imagen. Sin `Alt`, la imagen es decorativa y el lector de pantalla la salta |
| `Fit` | Cómo llena la imagen su caja si no tienen la misma proporción: `contain` (entera, con espacio a los lados), `cover` (llena la caja y recorta lo que sobra) o `stretch` (se deforma) |
| `Pdf page` | La página de un PDF que se dibuja, desde 1. Sin `Pdf page`, la primera. Solo en un PDF |
| `Rotate` | Girar la imagen sobre su centro; ocupa el rectángulo que la envuelve girada ([Capas y giro](../comun/capas-y-giro.md#girar-una-caja-rotate)) |

Admite además `Id`, `Style`, `Link`, `Box` y `Break` (sin `Inside` ni `Edges`: no se parte). No admite `Typography`.

## El tamaño

- Sin `Width` ni `Height`, una imagen mide su **tamaño natural**: cada píxel, 0,75 puntos. Una imagen de 400 × 300 píxeles
  mide 300 × 225 puntos. Un SVG mide lo que dicen su `width` y su `height` (o su `viewBox`), y la página de un PDF, lo que
  mide la página (un A4, 595 × 842).
- No se estira sola al ancho de la página: si no cabe, es un error. Para que ocupe todo el ancho, `Width: 100%`.
- Con `Width` o `Height`, el otro lado sale de la proporción de la imagen. Con los dos, la caja mide eso y `Fit` decide cómo
  la llena la imagen.
- `Margin: 0 auto` la centra.
- El relleno y el borde van alrededor: no deforman la imagen.
- En un `Flex` en fila, una imagen no se estira a lo alto: conserva su proporción.

## Entre páginas

Una imagen no se parte: si no cabe en lo que queda de la página, pasa entera a la siguiente. Una imagen más alta que una
página vacía es un error.

## SVG

Un SVG va en el PDF como dibujo vectorial: se ve nítido a cualquier tamaño y pesa poco. Se admite un subconjunto cerrado:
formas, rutas, grupos, degradados, recortes, `use` y texto. Lo demás (scripts, filtros, máscaras, patrones, imágenes, hojas de
estilo, referencias a otros ficheros) es un error, así que un SVG exportado de un editor a veces hay que limpiarlo. El texto
de un SVG tiene que usar una fuente de `fonts/`, y se dibuja como contorno: se ve igual, pero no se puede seleccionar.

## Una página de un PDF

Una `Image` de un PDF dibuja una de sus páginas, la de `Pdf page`, como cualquier otra imagen: un plano, un gráfico o un dibujo
que vienen en PDF, o un SVG que el motor no admite, convertido a PDF con otro programa. Va como dibujo vectorial, nítido a
cualquier tamaño, y su texto se puede seleccionar.

```
Image: planos/sala.pdf
    Pdf page: 2
    Alt: Plano de la sala de reuniones
    Box:
        Width: 100%
```

También sirve de **papel preimpreso**: un membrete, un formulario, un fondo. Se pone en un `Page overlay` detrás del contenido
(`Layer` negativa) que llega al borde de la página con un `Offset` negativo igual al margen, y la imagen ocupa todo el
`Page overlay` con `Width` y `Height` al 100 %. El margen del documento deja libre el sitio del membrete y del pie:

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
    Section:
        Page overlay:
            Offset: -140 -72 -80 -72
            Layer: -1
            Image: examples/letterhead/letterhead.pdf
                Box:
                    Width: 100%
                    Height: 100%
        Paragraph: Olot, 20 de octubre de 2025
        Paragraph: Le enviamos la propuesta de renovación del mobiliario del comedor, con el detalle de cada pieza y su plazo de entrega.
```

Con dos `Page overlay`, uno con `Pages: first` y otro con `Pages: not-first` y `Pdf page: 2`, la primera hoja lleva un membrete y las
siguientes, otro.

Lo que no es el dibujo de la página no se copia: sus enlaces, sus marcadores y su etiquetado. Sus capas, sí. Un PDF con un
campo de formulario relleno o un comentario que se ven es un error: aplánalo antes, por ejemplo imprimiéndolo a PDF.
También lo es un PDF cifrado y uno con fuentes que no van incrustadas o que no dicen qué carácter es cada letra
(`ToUnicode`). Las comprobaciones de las anotaciones y de las fuentes se pueden quitar al convertir
([Uso desde Java](../comun/uso-desde-java.md)); la del cifrado, no.

## Accesibilidad

Una imagen con `Alt` es una figura del PDF etiquetado, con ese texto alternativo. Sin `Alt` es decoración: un fondo, un
adorno. Escribe `Alt` en toda imagen que diga algo. La página de un PDF también: aunque su texto se pueda seleccionar, el
lector de pantalla lee el `Alt`. Un papel preimpreso es decoración: va sin `Alt`.

## Límites

Por seguridad, una imagen no puede tener más de 25 millones de píxeles; un SVG, más de 20 000 elementos ni 64 niveles de
anidado, y un PDF, más de 20 MB ([Uso desde Java](../comun/uso-desde-java.md) para cambiarlo).

Más detalle en la [referencia](../../referencia/componentes/image.md).

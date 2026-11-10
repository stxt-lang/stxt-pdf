# `Block`

Un `Block` agrupa elementos **uno debajo de otro**. Sirve para dar un fondo, un borde o un margen a un grupo, para que un
grupo no se parta entre páginas o para que sus hijos hereden una tipografía.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Aviso
    Page:
        Paper: A4
        Margin: 40
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For style: warning
            Box:
                Margin: 12 0
                Padding: 10 14
                Border: 0 0 0 4
                Border color: #C0392B
                Background: #FBEAE8
                Corner radius: 0 6 6 0
            Break:
                Keep together: true
    Section:
        Paragraph: Antes de instalar el termostato, corta la corriente.
        Block:
            Style: warning
            Paragraph: Atención
                Typography:
                    Bold: true
            Paragraph: Trabajar con la corriente conectada puede causar una descarga eléctrica.
        Paragraph: Después, retira la tapa con cuidado.
```

Admite `Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Isolate margins`, `Trim margins` y `Rotate` (girarlo con todo lo que contiene,
[Capas y giro](../common/layers-and-rotation.md#girar-una-caja-rotate)), y dentro cualquier elemento.

## Cómo coloca a sus hijos

- Cada hijo ocupa **todo el ancho** del `Block` (menos sus márgenes), salvo que declare su `Width`. Uno con `Width` y
  `Margin: 0 auto` queda centrado.
- Los hijos se apilan de arriba abajo, en el orden del documento.
- El `Block` mide de alto lo que suman sus hijos, más su relleno y su borde, salvo que declare `Height`.
- Entre dos hijos, los márgenes verticales no se suman: se queda el mayor ([La caja](../common/box.md#márgenes)).

Así colocan también sus hijos las secciones, los encabezados, las listas, las celdas con elementos y los elementos
posicionados.

## `Isolate margins`

El margen de arriba del primer hijo (y el de abajo del último) sale fuera del `Block` si este no tiene ni borde ni relleno
en ese lado: queda como margen del propio `Block`. Con `Isolate margins: true`, los márgenes de los hijos se quedan dentro. Un
`Block` que es ítem de un `Stack` o un `Grid` ya los deja dentro, así que ahí `Isolate margins: true` es un error.

## `Trim margins`

En un `Block` con fondo, borde o relleno, el margen de arriba del primer hijo y el de abajo del último se quedan dentro,
como un hueco entre el borde y el contenido. `Trim margins: both` los descarta: el contenido empieza y acaba junto al
relleno, tengan el margen que tengan sus párrafos.

```
Block:
    Trim margins: both
    Box:
        Background: #F1EFE8
        Padding: 10
    Paragraph: El primer párrafo empieza junto al relleno.
        Box:
            Margin: 12 0 12 0
    Paragraph: El último acaba junto al relleno; entre los dos quedan 12.
        Box:
            Margin: 12 0 12 0
```

Con `top` o `bottom` solo se descarta el margen de ese lado; `none`, el valor por defecto, no descarta ninguno. Vale
también en un `Frame`, un `List item`, una `Cell` y una `Section`, donde el primer elemento empieza entonces arriba del
área útil. Los márgenes entre los hijos no cambian nunca.

## Una caja a un lado y el texto alrededor (`Side`)

Con `Side: left` o `Side: right`, una `Image`, un `Barcode` o un `Block` con `Width` se coloca a ese lado y los párrafos
que vienen detrás corren a su lado, con las líneas más cortas, hasta que la caja acaba. Es la foto de una noticia o la
figura de un manual. El margen de la caja es la separación con el texto.

```
Block:
    Side: right
    Box:
        Width: 180
        Margin: 0 0 6 14
    Image: examples/newsletter/trail.jpg
        Alt: Un sendero entre prados con montañas al fondo
        Box:
            Width: 100%
    Text: El sendero del Pla de les Eres. Foto: Joan Casas.
        Typography:
            Size: 7.5
Paragraph: Octubre es el mes del hayedo, y estas son tres salidas para disfrutarlo sin multitudes...
Paragraph: El camino del Molí sale del aparcamiento de la fábrica y sigue el río hasta una cascada pequeña.
```

Rodean la caja los párrafos y los textos, también los de un `Block`, un encabezado, un `Markdown` o una lista (una lista,
solo si la caja está a la derecha: sus marcadores van a la izquierda). Una tabla, un `Stack` o un texto con fondo o borde
van debajo de la caja, y cualquier cosa con `Clear: true` también. La caja no se parte entre páginas: si no cabe, pasa a
la siguiente con la primera línea que la rodea.

## Recetas

- **Un recuadro:** un `Block` con `Padding`, `Border` y `Background`.
- **Que no se parta:** `Break` con `Keep together: true` (el bloque de firmas de un contrato).
- **Empezar en página nueva:** `Break` con `Before: page`.
- **Una foto con el texto alrededor:** `Side: left` o `right` en la imagen (o en un `Block` con la foto y su pie).

Más detalle en la [referencia](../../reference/components/block.md).

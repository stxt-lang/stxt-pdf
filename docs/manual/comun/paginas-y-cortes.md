# Páginas y cortes: `Break`

## Cómo se reparte el contenido

El motor llena cada página con el contenido en orden y, cuando no cabe, **corta** y sigue en la página siguiente. Puede
cortar:

- Entre dos cajas que van una debajo de otra.
- Entre dos líneas de un texto.
- Entre dos filas de una tabla o de un `Grid`, o entre dos líneas de un `Flex`.

Siempre corta lo más arriba posible en el árbol: antes de partir una caja por dentro, prueba a cortar entre ella y sus
hermanas. Y elige el **último** sitio de la página que cumple todas las reglas:

1. No dejar menos líneas de un párrafo que `Orphans` al final de la página ni que `Widows` al principio de la siguiente
   ([Tipografía](tipografia.md#viudas-y-huérfanas)).
2. No cortar entre dos cajas que piden ir juntas (`Keep with next`, `Keep with previous`).
3. No partir una caja que pide `Keep together: true`.

Si no hay ningún sitio que cumpla todo, relaja las reglas en ese orden. Al principio de una página siempre coloca algo,
aunque haya que partirlo, para que la paginación termine.

Algunas cosas no se parten nunca: una imagen, una caja girada, una caja posicionada y la cabecera y el pie de una tabla. Si
no caben en lo que queda de página, pasan enteras a la siguiente, y si no caben ni en una página vacía, es un error.

## El grupo `Break`

| Propiedad | Valores | Por defecto | Qué hace |
|---|---|---|---|
| `Before` | `auto`, `page`, `column` | `auto` | `page` empieza la caja en una página nueva |
| `After` | `auto`, `page`, `column` | `auto` | `page` hace que lo siguiente empiece en una página nueva |
| `Keep with next` | `true`, `false` | `false` | Mantiene la caja en la misma página que la siguiente |
| `Keep with previous` | `true`, `false` | `false` | Mantiene la caja en la misma página que la anterior |
| `Keep together` | `true`, `false` | `false` | Intenta no partir la caja |
| `Edges` | `open`, `closed` | `open` | Qué pasa con el borde y el relleno de una caja partida |

`column` es lo mismo que `page` pero dentro de un [`Columns`](../componentes/columns.md): salta a la columna siguiente.
Una caja no puede ir con la siguiente y saltar después de ella a la vez: `Keep with next: true` con `After: page` es un
error, y `Keep with previous: true` con `Before: page`, también.

```
Block:
    Break:
        Keep together: true
    Paragraph: Firma del cliente
    Paragraph: Firma del proveedor
```

- Un salto forzado al principio de una página no hace nada: nunca deja páginas en blanco.
- Los encabezados ya evitan quedarse solos al final de una página: no hace falta `Keep with next: true`
  ([Encabezados](../componentes/heading.md)).
- `Keep together: true` no impide partir una caja que no cabe ni en una página entera.
- Los elementos posicionados `Overlay` y `Page overlay` no admiten `Break`, porque no se parten ni están en el flujo. Tampoco
  lo que hay dentro de ellos o de una caja girada: un `Break` escrito ahí es un error (salvo en las columnas de un
  `Columns`). El que llega de una regla de `Styles` no tiene efecto.

## Cajas partidas

Cuando una caja con borde o relleno se parte entre dos páginas:

- **`Edges: open`** (por defecto): la caja se corta como una hoja de papel. El primer trozo no tiene borde ni relleno
  abajo y el siguiente no los tiene arriba.
- **`Edges: closed`**: cada trozo se pinta como una caja completa, con su borde y su relleno por los cuatro lados.

Un `Height` fijo es el alto total de la caja, sumando todos sus trozos. Los márgenes que tocan un corte desaparecen: el
contenido de la página siguiente empieza arriba del todo.

## Números de página

En cualquier texto, `{page}` escribe el número de la página y `{pages}` el total:

```
Page overlay:
    Offset: auto 0 -30 0
    Text: Página {page} de {pages}
        Typography:
            Align: right
```

Un pie de página como este es un `Page overlay`, que se repite en cada página
([posicionados](../componentes/frame-and-overlays.md#page-overlay)). Qué número lleva cada página lo decide su sección: puede
seguir la cuenta, volver a empezar en 1 o no tener número, como una portada
([Document](../componentes/document.md#numeración-de-las-páginas)). `{pages}` es el último número de la cuenta en curso. Con
una portada sin número y seis páginas de cuerpo, la primera dice «Página 1 de 6».

Para escribir la página donde está otra parte del documento («véase la página 12») está `{page of: id}`
([Referencias y enlaces](referencias-y-enlaces.md)).

Más detalle en la [referencia](../../referencia/comun/cortes.md).

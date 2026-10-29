# Páginas y cortes: `Break`

## Cómo se reparte el contenido

El motor llena cada página con el contenido en orden y, cuando no cabe, **corta** y sigue en la página siguiente. Puede
cortar:

- entre dos cajas que van una debajo de otra;
- entre dos líneas de un texto;
- entre dos filas de una tabla o de un `Grid`, o entre dos líneas de un `Flex`.

Siempre corta lo más arriba posible en el árbol: antes de partir una caja por dentro, prueba a cortar entre ella y sus
hermanas. Y elige el **último** sitio de la página que cumple todas las reglas:

1. no dejar menos líneas de un párrafo que `Orphans` al final de la página ni que `Widows` al principio de la siguiente
   ([Tipografía](tipografia.md#viudas-y-huérfanas));
2. no cortar donde una caja pide `avoid`;
3. no partir una caja que pide `Inside: avoid`.

Si no hay ningún sitio que cumpla todo, relaja las reglas en ese orden. Y al principio de una página siempre coloca algo,
aunque haya que partirlo: un documento siempre avanza.

Algunas cosas no se parten nunca: una imagen, una caja girada, una caja posicionada y la cabecera y el pie de una tabla. Si
no caben en lo que queda de página, pasan enteras a la siguiente, y si no caben ni en una página vacía, es un error.

## El grupo `Break`

| Propiedad | Valores | Por defecto | Qué hace |
|---|---|---|---|
| `Before` | `auto`, `page`, `column`, `avoid` | `auto` | `page` empieza la caja en una página nueva; `avoid` evita cortar justo antes |
| `After` | `auto`, `page`, `column`, `avoid` | `auto` | `page` hace que lo siguiente empiece en una página nueva; `avoid` mantiene la caja con la siguiente |
| `Inside` | `auto`, `avoid` | `auto` | `avoid` intenta no partir la caja |
| `Decoration` | `slice`, `clone` | `slice` | Qué pasa con el borde y el relleno de una caja partida |

`column` es lo mismo que `page` pero dentro de un [`Multicol`](../componentes/multicol.md): salta a la columna siguiente.

```
Block:
	Break:
		Inside: avoid
	Paragraph: Firma del cliente
	Paragraph: Firma del proveedor
```

- Un salto forzado al principio de una página no hace nada: nunca deja páginas en blanco.
- Los encabezados ya evitan quedarse solos al final de una página: no hace falta `After: avoid`
  ([Encabezados](../componentes/heading.md)).
- `Inside: avoid` no impide partir una caja que no cabe ni en una página entera.
- Los elementos posicionados `Absolute` y `Fixed` no admiten `Break`, porque no se parten ni están en el flujo.

## Cajas partidas

Cuando una caja con borde o relleno se parte entre dos páginas:

- con `Decoration: slice` (por defecto), la caja se corta como una hoja de papel: el primer trozo no tiene borde ni relleno
  abajo y el siguiente no los tiene arriba;
- con `Decoration: clone`, cada trozo se pinta como una caja completa, con su borde y su relleno por los cuatro lados.

Un `Height` fijo es el alto total de la caja, sumando todos sus trozos. Los márgenes que tocan un corte desaparecen: el
contenido de la página siguiente empieza arriba del todo.

## Números de página

En cualquier texto, `{page}` escribe el número de la página y `{pages}` el total:

```
Fixed:
	Offset: auto 0 -30 0
	Text: Página {page} de {pages}
		Typography:
			Align: right
```

Un pie de página como este es un `Fixed`, que se repite en cada página
([posicionados](../componentes/relative-absolute-fixed.md#fixed)). Qué número lleva cada página lo decide su sección: puede
seguir la cuenta, volver a empezar en 1 o no tener número, como una portada
([Document](../componentes/document.md#numeración-de-las-páginas)). `{pages}` es el último número de la cuenta en curso: con
una portada sin número y seis páginas de cuerpo, la primera dice «Página 1 de 6».

Para escribir la página donde está otra parte del documento («véase la página 12») está `{page of: id}`
([Referencias y enlaces](referencias-y-enlaces.md)).

Más detalle en la [referencia](../../referencia/comun/cortes.md).

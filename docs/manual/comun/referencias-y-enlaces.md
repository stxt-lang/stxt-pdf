# Referencias y enlaces

## Referencias

Un texto puede escribir datos que calcula el motor:

| Se escribe | Escribe |
|---|---|
| `{page}` | El número de la página donde está el texto |
| `{pages}` | El total de páginas de la cuenta en curso |
| `{page of: id}` | La página donde empieza la caja con ese `Id` |
| `{number of: id}` | El número de un ítem de una lista numerada, o el de un encabezado numerado |

```
Paragraph: El esquema de conexión está en la página {page of: wiring}. Véase la cláusula {number of: pago}.
Block:
	Id: wiring
List:
	Marker: decimal
	List item:
		Id: pago
		Paragraph: El pago se hará a 30 días.
```

- La referencia puede ir antes o después de la caja que nombra.
- El número de página es el que se imprime en esa página (el de su pie), no la posición de la hoja en el PDF.
- Una referencia a un `Id` que no existe es un error, igual que la página de algo que no tiene una sola página (lo que hay
  dentro de un `Page overlay`, que se repite en todas) o de algo que está en una página sin número.
- `{number of: id}` de un ítem da solo el suyo (`3`, `c`, `IV`), y de un encabezado numerado, el completo (`4.2`). Lo demás
  no tiene número.

El motor escribe el número antes de repartir el texto en líneas, así que «página 9» y «página 10» ocupan lo que tienen que
ocupar. Si eso mueve el contenido, vuelve a paginar hasta que todos los números son correctos.

## Enlaces

Un enlace hace clicable una zona del PDF. Lleva a una web, a un correo o a otra parte del documento.

**Una caja entera**, con `Link`:

```
Text: Escríbenos a soporte@airemas.example
	Link: mailto:soporte@airemas.example
Image: examples/brand/nogal.svg
	Alt: Taller Nogal
	Link: https://tallernogal.example
	Box:
		Width: 80
Paragraph: Ver las especificaciones
	Link: #specs
```

**Una parte de una línea**, con la sintaxis de Markdown, en un [`Markdown`](../componentes/markdown.md) o en un texto con
`Markup: markdown`: `Consulta [las especificaciones](#specs) o [la web](https://airemas.example).`

- El destino es una dirección `https://...`, `http://...` o `mailto:...`, o `#id`: la caja con ese `Id`, que puede estar
  en cualquier parte del documento. Cualquier otra cosa es un error.
- Un enlace no cambia el aspecto de lo que enlaza: el color y el subrayado los da su estilo. El enlace de Markdown es la
  excepción: sale subrayado.
- `Link` no se hereda ni va en `Styles`: es de cada elemento.
- Las entradas de un índice ya llevan a su encabezado ([Table of contents](../componentes/table-of-contents.md)).
- En el PDF, cada enlace lleva como texto lo que dice la caja, para quien no lo ve.

Más detalle en la referencia: [referencias](../../referencia/comun/referencias.md) y [enlaces](../../referencia/comun/enlaces.md).

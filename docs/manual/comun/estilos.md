# Estilos: `Styles`

Las reglas de `Styles` dan propiedades a muchos elementos a la vez, para no repetirlas en cada uno. `Styles` va en el
`Document`, antes de las secciones, o en un [fichero aparte](#estilos-en-un-fichero-aparte).

```
Styles:
	For element: Document
		Typography:
			Font: IBM Plex Sans
			Size: 10
	For element: Paragraph
		Box:
			Margin: 0 0 8 0
	For style: note
		Box:
			Background: #F1EFE8
			Padding: 8
	For id: title
		Typography:
			Size: 20
			Bold: true
```

## Reglas

| Regla | A qué se aplica |
|---|---|
| `For element: Document` | A todo el documento: aquí va la tipografía base. Solo admite `Typography` |
| `For element: Paragraph` (o cualquier otro elemento) | A todos los elementos de ese tipo |
| `For style: note` | A los elementos con `Style: note` (un elemento puede tener varios estilos: `Style: note wide`) |
| `For id: title` | Al elemento con `Id: title` |
| `For element: Head cell`, `Body cell`, `Foot cell`, `Odd row cell`, `Even row cell` | A las celdas de una tabla según dónde están ([Table](../componentes/table.md)) |
| `For column: 3` (o `For column: 2 4`) | A las celdas que empiezan en esa columna (o columnas) de una tabla |
| `For element: Code block` | A los bloques de código de un `Markdown` |
| `For element: Contents entry 1` … `6` | A las entradas de un índice, por nivel ([Contents](../componentes/contents.md)) |

Cada regla admite los grupos `Box`, `Typography` y `Break`. Las propiedades propias de un elemento (las columnas de una
tabla, el marcador de una lista, un enlace…) no van en reglas: se escriben en el elemento.

## Quién gana

Si varias cosas dan la misma propiedad, gana, de más a menos fuerte:

1. lo que declara el propio elemento;
2. su regla `For id`;
3. sus reglas `For style` (entre varias, la que va **después** en `Styles`);
4. en las celdas de una tabla, las reglas de su fila, de su columna y de su posición;
5. su regla `For element`;
6. lo que hereda de su padre (solo `Typography`);
7. el valor por defecto.

Las reglas se mezclan propiedad a propiedad: si una regla da `Size` y el elemento declara `Bold`, se aplican las dos. Y
una regla gana a la herencia: con `For element: Text` y `Size: 10`, un texto mide 10 aunque su padre diga `Size: 20`.

Un nombre de `Style` que no tiene su regla `For style` es un error, para que una errata no pase sin verse. En cambio, una
regla que no se aplica a nada no lo es: se puede reutilizar el mismo `Styles` en muchos documentos.

## La tipografía base

`Font` y `Size` no tienen valor por defecto, así que todo documento los declara, normalmente en `For element: Document`. Todo
lo demás lo hereda de ahí.

## Estilos en un fichero aparte

Las reglas pueden ir en un documento propio, con raíz `Styles`, para compartirlas entre varios documentos o dejar el
documento solo con el contenido:

```
Styles (dev.stxt.pdf):
	For element: Document
		Typography:
			Font: IBM Plex Sans
			Size: 10
	For style: panel
		Box:
			Padding: 10
```

El documento no dice dónde está su fichero de estilos: se pasa al convertirlo ([Uso desde Java](uso-desde-java.md)). Si el
documento tiene también su `Styles`, en cada nivel de la lista de arriba gana el del documento, así que sacar el `Styles`
a un fichero no cambia nada. Un error en el fichero de estilos dice su línea así: `línea 8 de los estilos: …`, también
si se ve al maquetar, como una fuente que no existe o una caja que no cabe.

Más detalle en la [referencia](../../referencia/comun/estilos.md).

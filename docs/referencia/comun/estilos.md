# Reglas reutilizables: `Styles`

`Styles`, hijo opcional de `Document` (como mucho uno), declara una vez propiedades que se aplican por defecto a muchos
elementos. Las mismas reglas pueden estar también en un [documento de estilos aparte](#documento-de-estilos-aparte). No
hay cascada: las reglas dan valores por defecto a los elementos de un tipo, de una clase o con un `Id`, y el elemento los
puede sobrescribir.

## Reglas

| Regla | Valor | Se aplica a |
|---|---|---|
| `Element` | `Document` | la raíz: su `Typography` es la base que heredan todas las cajas. Solo admite `Typography` |
| `Element` | nombre de un elemento de caja (`Block`, `Text`, `Heading 1`…) | todos los elementos de ese tipo |
| `Element` | un elemento generado por el motor: `Code block` o `Contents entry 1` … `Contents entry 6` | los bloques de código de un `Markdown` ([Markdown](../componentes/markdown.md#código)) o las entradas de un `Contents` ([Contents](../componentes/contents.md#estilo)); no se escriben en el documento |
| `Element` | una posición en una tabla: `Head cell`, `Body cell`, `Foot cell`, `Odd row cell` o `Even row cell` | las celdas de una `Table` en esa posición ([Table](../componentes/table.md#estilo-de-las-celdas)) |
| `For class` | un nombre, sin espacios | los elementos con ese nombre en su `Class` y las celdas de una `Row` con él en su `Class` |
| `For id` | un `Id`, sin espacios | el elemento con ese `Id` |
| `For column` | una columna (`3`) o un rango de columnas (`2 4`, ambas incluidas) | las celdas de una `Table` que **empiezan** en esas columnas |

Cada regla admite los grupos `Box`, `Typography` y `Break`, como mucho uno de cada; no admite las propiedades propias de un
elemento (`Gap`, `Columns`, `Offset`, `Marker`, `Link`, `Rotate`…).

```
Styles:
	Element: Text
		Typography:
			Align: justify
	For class: note
		Box:
			Background: #F1EFE8
			Padding: 8
	For id: title
		Typography:
			Size: 20
			Weight: bold
Text: Resultados del trimestre
	Id: title
	Typography:
		Align: left
Text: Una nota al margen.
	Class: note
```

> **Decisión:** las reglas solo admiten los grupos: las propiedades propias de un elemento no se podrían validar al
> escribir el documento, porque el tipo de elemento va en el valor de `Element`.

## Prioridad

Cada propiedad toma, de más a menos fuerte:

1. lo que declara el propio elemento;
2. la regla `For id` de su `Id`;
3. las reglas `For class` de sus clases; si varias declaran la misma propiedad, gana la que va **después en `Styles`**, no
   el orden de `Class`;
4. las reglas `For class` de las clases de la `Row` de una celda, con el mismo criterio de orden; entre los puntos 2 y 3,
   la regla `For id` del `Id` de su `Row`;
5. las reglas `For column` de la columna de una celda, con el mismo criterio de orden;
6. las reglas de posición de una celda (`Head cell`, `Body cell`, `Foot cell`, y después `Odd row cell` o
   `Even row cell`);
7. el `Column align` de su tabla, solo para `Align`;
8. la regla `Element` de su tipo;
9. lo heredado (solo `Typography`): para una celda, la de su `Row` y, si no, la de su padre;
10. el valor por defecto.

Las reglas de los puntos 4 a 7 solo se aplican a las celdas de una `Table` ([Table](../componentes/table.md#estilo-de-las-celdas)).

Una regla gana a la herencia: con `Element: Text` y `Size: 10`, un `Text` mide 10 aunque su padre declare `Size: 20`. Las
reglas se mezclan **propiedad a propiedad**, no por grupo: si la regla declara `Size` y el elemento `Weight`, se aplican
las dos. Lo que una regla da a un elemento en `Typography` lo heredan sus descendientes, como si lo hubiera declarado él.
`Item` y `Area` no son cajas: las reglas se aplican al elemento que envuelven. El `Class` y el `Id` de una `Row` aplican
sus reglas a cada una de sus celdas, no a la franja de la fila. `Head`, `Body`, `Foot` y `Row` no se pueden nombrar en `Element` (para dar estilo a las celdas de
cada grupo están las reglas de posición); `Table` y `Cell`, sí. A una `Section` solo le llega `Typography`
([Document](../componentes/document.md#section)).

> **Decisión:** prioridad fija en lugar de especificidad calculada. *Alternativa descartada:* especificidad calculada y
> selectores compuestos, más potentes pero menos predecibles.

## Validación

Lo que aporta una regla se valida igual que si lo declarara el elemento ([Errores](errores.md)), y el error señala la
línea de la propiedad dentro de la regla. Los valores de una regla se comprueban aunque no se aplique a ningún elemento.
Son errores:

- un `Element` que no nombra un elemento de caja, un elemento generado ni una posición en una tabla
  (`STYLE_UNKNOWN_ELEMENT`);
- un `For column` que no es un número (`VALUE_SYNTAX`) o un rango que no va de menor a mayor (`VALUE_NOT_ALLOWED`);
- dos reglas con el mismo selector en el mismo origen, también dos `For column` con el mismo rango (`STYLE_DUPLICATE`);
- una regla que da `Break` a un elemento que no lo admite: `Absolute`, `Fixed`, un encabezado, un `Text` de un encabezado o
  una celda, también con una regla de posición, de columna o de la clase de su `Row` (`STYLE_NOT_ALLOWED`);
- una regla que da a una `Section` algo que no es `Typography` (`STYLE_NOT_ALLOWED`);
- una clase de `Class` sin su regla `For class` (`CLASS_WITHOUT_RULE`).

Un `Element`, un `For class`, un `For id` o un `For column` que no se aplican a ningún elemento no son errores: así las
reglas se pueden reutilizar, y un elemento con ese `Id` o esa clase puede llegar después (de una plantilla, por ejemplo).

> **Decisión:** una regla por tipo puede dar un error en un elemento concreto (un `Margin: auto` que no centra en un ítem
> de `Flex`), con la línea de la regla. *Alternativa descartada:* ignorar lo que no tiene efecto cuando viene de una
> regla.

## La tipografía base

`Document` no admite `Typography`: lo que hereda toda la descendencia (por lo general `Font` y `Size`, que no tienen valor
por defecto) se declara en la regla `Element: Document`, que solo admite `Typography` (con `Box` o `Break`, o con
`Language`, es `STYLE_NOT_ALLOWED`):

```
Styles:
	Element: Document
		Typography:
			Font: IBM Plex Sans
			Size: 10
```

Es la base de la herencia: la regla `Element` de un tipo o lo que declara una caja la cambian para esa caja y sus
descendientes.

> **Decisión:** la base va en una regla, no en `Document`, para que esté con el resto del estilo y se pueda llevar al
> documento de estilos aparte.

## Documento de estilos aparte

Las reglas pueden escribirse en un documento aparte, de raíz `Styles` con el namespace `dev.stxt.pdf`, para quitarlas
del documento principal o compartirlas entre varios:

```
Styles (dev.stxt.pdf):
	Element: Document
		Typography:
			Font: IBM Plex Sans
			Size: 10
	For class: panel
		Box:
			Padding: 10
```

- **Se pasa al maquetar, nunca se busca.** El documento no dice dónde está su documento de estilos y el motor no busca
  ficheros ni carpetas: quien convierte lo lee y lo pasa junto al documento
  ([Uso desde Java](../implementacion/api.md#estilos-aparte)). Es opcional.
- **Vale menos que el documento.** Se admiten las mismas reglas que en el `Styles` del documento. En cada nivel de la
  prioridad (el de `For id`, el de `For class`, el de `Element`…), las reglas del documento de estilos aparte van **antes** y
  las del documento **después**, así que gana el documento, propiedad a propiedad. La prioridad entre niveles no cambia:
  un `For class` del documento aparte sigue ganando a un `Element` del documento. Con esto, sacar el `Styles` de un
  documento a un fichero aparte no cambia el resultado.
- **Cada origen es estricto por separado.** Dos reglas con el mismo selector son un error dentro de un origen
  (`STYLE_DUPLICATE`), pero la misma regla puede estar en los dos. Una clase de `Class` puede tener su `For class` en
  cualquiera de los dos.
- **Errores.** Una regla del documento de estilos se valida como una del documento, y su error lleva la línea **de ese
  fichero** y lo dice: `línea 8 de los estilos: …`. La raíz que no es `Styles` es `DOCUMENT_ROOT`. Los errores que
  dependen de la maquetación (una fuente que no existe, un desbordamiento) señalan la línea de la propiedad sin decir en
  qué fichero está.

> **Decisión:** la prioridad entre los dos orígenes es por nivel, así que sacar el `Styles` a un fichero no cambia nada.
> *Alternativa descartada:* que todas las reglas del fichero valgan menos que cualquiera del documento.

> **Decisión:** el documento no nombra su documento de estilos: lo pasa quien convierte. *Alternativa descartada:* una
> propiedad `Styles file` en `Document`.

## Lo que no hay

- Selectores compuestos (un elemento dentro de otro, varias clases a la vez), combinadores ni prioridades declaradas por el
  autor.
- Estilo de una fila de tabla con `Box` o de una columna entera: se da a las celdas ([Table](../componentes/table.md#lo-que-no-hay)).

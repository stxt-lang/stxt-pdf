# `Block`

Una caja que apila sus hijos en vertical. Admite `Id`, `Class`, `Link`, `Box`, `Typography`, `Break` e `Isolate`, y como
hijos cualquier elemento. Va donde va un elemento en flujo.

```
Block:
	Box:
		Padding: 10
		Background: #F1EFE8
	Paragraph: Primer párrafo.
	Paragraph: Segundo párrafo.
```

## El flujo de bloque

Es como colocan a sus hijos en flujo un `Block`, cada sección (el área útil), `Relative`, `Absolute`, `Fixed`, los
encabezados, `List`, `List item`, `Markdown`, `Contents`, una `Cell` con elementos y cada columna de un `Multicol`. Los hijos
en flujo se apilan en vertical:

1. **Ancho.** Con `Width: auto`, la caja ocupa el ancho del contenedor de referencia menos sus márgenes horizontales. Con
   ancho conocido y los dos márgenes horizontales `auto`, el sobrante se reparte entre ambos y la caja queda **centrada**;
   si solo uno es `auto`, absorbe el sobrante ([La caja](../comun/caja.md#márgenes-auto)).
2. **Posición.** La caja se coloca en la posición del cursor vertical, tras aplicar su margen superior colapsado con el
   anterior.
3. **Altura.** Se calcula su layout y el cursor avanza.

Con `Height: auto`, la altura del contenedor es la posición final del cursor más su relleno y su borde. **El ancho se
propaga de padres a hijos; la altura, de hijos a padres.** Un `Absolute` o un `Fixed` entre los hijos no ocupa sitio
([posicionados](relative-absolute-fixed.md)).

## Colapso de márgenes

Los márgenes **verticales** adyacentes en flujo de bloque se fusionan:

- entre hermanos consecutivos;
- entre un padre y su primer o último hijo, si el padre no tiene borde ni relleno en ese lado y no crea un contexto de
  formato de bloque (el margen del hijo «sale» del padre);
- a través de cajas vacías (sin altura, borde, relleno ni contenido).

El resultado de colapsar varios márgenes es **el mayor positivo más el más negativo**: `20` y `30` dan 30; `20` y `−5`
dan 15; `−10` y `−4` dan −10. En un corte de página, los márgenes que tocan el corte se truncan
([Cortes](../comun/cortes.md#márgenes-en-los-cortes)).

## Contextos de formato de bloque

Aíslan el colapso de márgenes: los márgenes de dentro no salen. Los crean: cada sección, `Absolute` y `Fixed`, los ítems
de `Flex` y `Grid` (también las celdas de una `Table`), las columnas de un `Multicol` y los `Block` y `Relative` con
`Isolate: true`. Los márgenes de los ítems de `Flex` y `Grid` nunca colapsan.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Isolate` | booleano | `false` | Crea un contexto de formato de bloque nuevo: corta el colapso de márgenes con los hijos. Solo en `Block` y `Relative`. |

> **Decisión:** `Isolate` solo está en `Block` y `Relative`: en los demás no tendría efecto (`Absolute`, `Fixed` y los ítems
> ya crean un contexto de formato de bloque; `Flex`, `Grid` y `Text` no tienen hijos en flujo de bloque). Un `Block` que es
> ítem lo admite aunque no tenga efecto, porque el schema no sabe si está en un `Item`.

## Tamaños intrínsecos

El min-content de un `Block` es el máximo del min-content de sus hijos, y su max-content, el máximo de sus max-content
([La caja](../comun/caja.md#tamaños-intrínsecos)).

## Cortes

Un `Block` se parte entre sus hijos, o dentro del hijo que no cabe ([Cortes](../comun/cortes.md)).

## Salida PDF

Un `Block` es un `Div` del PDF etiquetado ([El PDF](../comun/pdf.md#pdf-etiquetado)).

## Lo que no hay

- Cajas que floten a un lado del texto, o texto que rodee a otra caja.
- Recorte de lo que se sale (lo que se sale es un [desbordamiento](../comun/desbordamiento.md)).

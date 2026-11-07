# `Text` y `Paragraph`

Los dos son cajas de texto y se maquetan igual, pero cada uno tiene su papel. Un `Paragraph` es **un párrafo del
contenido**. Un `Text` es **texto suelto** que no es un párrafo del contenido: un rótulo, una cifra, una línea de una
cabecera o de un pie, los trozos de un encabezado, un bloque de código. Ninguno de los dos tiene varios párrafos dentro:
unos párrafos seguidos son varios `Paragraph`, en un `Block` si van juntos.

> **Decisión:** dos elementos con papeles distintos y sin solaparse: el párrafo, que se estila con `For element:
> Paragraph` sin tocar el resto de textos, y el texto suelto. *Alternativa descartada:* un solo elemento de texto, que
> obligaría a estilar los párrafos con `Style`, o un `Text` con varios párrafos, que hacía lo mismo que varios
> `Paragraph`.

## `Text`

Texto suelto, una caja sin hijos: reparte su texto en líneas ([Tipografía](../common/typography.md#el-reparto-en-líneas)).
Admite `Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Content` y `Rotate`, y va donde va un elemento en flujo. Con
`Rotate` se escribe girado (un texto en vertical) y ocupa el rectángulo que lo envuelve. Sin `Width`, no parte sus
líneas ([Capas y giro](../common/layers-and-rotation.md#giro-en-flujo-block-text-paragraph-e-image)).

El texto va en su valor (`Text: Un título`) o en un hijo `Content >>` con un bloque indentado. Uno de los dos es
obligatorio, y no se admiten ambos (`TEXT_CONTENT`). Una línea en blanco es un error (`TEXT_BLANK_LINE`, en la línea de la
línea en blanco), salvo en un texto preformateado (`Preformatted: true` o `wrap`), donde es una línea más del
texto ([Formato del texto](../common/text-formatting.md#texto-preformateado)). Por defecto el
texto es literal, y con `Markup: markdown` admite formato dentro de la línea
([Formato del texto](../common/text-formatting.md#formato-en-línea-markup)).

```
Text: Una línea, en el valor.
Text:
    Content >>
        Un texto largo, en un bloque. Los saltos de línea simples
        dentro del bloque equivalen a espacios.
```

`Font` y `Size` son obligatorios, propios o heredados ([Tipografía](../common/typography.md)).

> **Decisión:** `Content` va siempre en bloque (`Content: algo` es un error): el texto corto va en el valor. Un `Text`
> vacío es un error. *Alternativa descartada:* `Content` también en línea, dos formas de escribir lo mismo.

## `Paragraph`

Un párrafo del contenido: admite lo mismo que un `Text` (`Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Rotate`,
`Clear` y el texto en el valor o en `Content`), y además `Drop cap`, y se maqueta igual. Es **un solo párrafo**: una línea
en blanco en su `Content` es un error (`TEXT_BLANK_LINE`), también en un texto preformateado. Para varios párrafos se
usan varios `Paragraph`. Las reglas `For element: Paragraph` estilan los párrafos sin tocar los textos sueltos.

### Letra capitular (`Drop cap`)

Con `Drop cap: N` (de 2 a 5), la primera letra del párrafo es una **letra capitular**: sale del texto y se compone, con la
fuente del párrafo (su familia, su peso y su cursiva, y el formato del tramo al que pertenece), al cuerpo cuya altura de
mayúsculas va de la altura de mayúsculas de la primera línea a la línea base de la línea `N`; las `N` primeras líneas
empiezan a su derecha, separadas de ella por un espacio del párrafo. El párrafo mide al menos esas `N` líneas, y un corte
de página no cae entre ellas: si no caben en la página con la letra, el párrafo pasa entero a la siguiente.

```
Paragraph: Había una vez, en un valle entre hayedos, un pueblo que cada otoño llenaba su plaza de castañas.
    Drop cap: 3
```

El texto que se lee y se copia es el mismo: la letra sigue siendo la primera de su palabra. La altura de mayúsculas es la
de la fuente (su tabla `OS/2`) y, sin ella, el 70 % del cuerpo. Es el primer carácter tal cual: con `Case: uppercase` se
pone en mayúscula como el resto, y si el párrafo empieza por una comilla, la capitular es la comilla. No va en un párrafo
preformateado (`DROP_CAP_NOT_ALLOWED`) ni fuera de 2 a 5 (`VALUE_NOT_ALLOWED`). Junto a una caja lateral
([Block](block.md#cajas-laterales-side)), la capitular va después de lo que la caja quita a las líneas.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Drop cap` | entero de 2 a 5 | sin valor | Líneas que ocupa la letra capitular. Solo en `Paragraph`. |

`Drop cap` también puede venir de una regla de `Styles` ([Propiedades de elemento](../common/styles.md#propiedades-de-elemento)).

> **Decisión:** la capitular se dimensiona por la altura de mayúsculas, con su cima a la altura de las mayúsculas de la
> primera línea y su base en la línea base de la última que ocupa, como en los procesadores de texto; es la primera
> letra, en la fuente del párrafo, separada del texto un espacio. *Alternativa descartada:* una fuente, un color o varias
> letras propios de la capitular, que piden tramos con otro tamaño
> ([Formato del texto](../common/text-formatting.md#lo-que-no-hay)).

## Dónde va el texto

Además de `Text` y `Paragraph`, tienen texto una `Cell` con valor ([Table](table.md#celdas)) y los `Text` de un encabezado
([Encabezados](heading.md)). Un `Text` es también el bloque de código que genera un `Markdown`
([Markdown](markdown.md#código)).

## Tamaños intrínsecos

El ancho más estrecho es la palabra más ancha, y el natural, el párrafo más largo en una sola línea
([Tipografía](../common/typography.md#tamaños-intrínsecos)).

## Cortes

Un texto, que es un solo párrafo, se parte entre líneas respetando `Widows` y `Orphans` ([Cortes](../common/breaks.md)).

## Accesibilidad

Un lector de pantalla lee un `Text` y un `Paragraph` como un párrafo. Los `Text` de un encabezado son el texto del
encabezado ([El PDF](../common/pdf.md#accesibilidad)).

## Lo que no hay

- Texto fuera de un `Text`, un `Paragraph` o una `Cell`.
- Partir palabras sin `Hyphenate` ni un guion blando: una palabra más ancha que la línea es un error
  ([Tipografía](../common/typography.md#guiones-hyphenate)).

# `Text` y `Paragraph`

Los dos son cajas de texto y se maquetan igual; cada uno tiene un papel. Un `Paragraph` es **un párrafo del
contenido**. Un `Text` es **texto suelto** que no es un párrafo del contenido: un rótulo, una cifra, una línea de una
cabecera o de un pie, los trozos de un encabezado, un bloque de código. Ninguno de los dos tiene varios párrafos dentro:
unos párrafos seguidos son varios `Paragraph`, en un `Block` si van juntos.

> **Decisión:** dos elementos con papeles distintos y sin solaparse: el párrafo, que se estila con `For element:
> Paragraph` sin tocar el resto de textos, y el texto suelto. *Alternativa descartada:* un solo elemento de texto, que
> obligaría a estilar los párrafos con `Style`; y un `Text` con varios párrafos, que hacía lo mismo que varios
> `Paragraph`.

## `Text`

Texto suelto, una caja sin hijos: reparte su texto en líneas ([Tipografía](../comun/tipografia.md#el-reparto-en-líneas)).
Admite `Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Content` y `Rotate`, y va donde va un elemento en flujo. Con
`Rotate` se escribe girado (un texto en vertical) y ocupa el rectángulo que lo envuelve; sin `Width`, no parte sus
líneas ([Capas y giro](../comun/capas-y-giro.md#giro-en-flujo-block-text-paragraph-e-image)).

El texto va en su valor (`Text: Un título`) o en un hijo `Content >>` con un bloque indentado. Uno de los dos es
obligatorio, y no se admiten ambos (`TEXT_CONTENT`). Una línea en blanco es un error (`TEXT_BLANK_LINE`, en la línea de la
línea en blanco), salvo en un texto preformateado (`White space: preserve` o `preserve-wrap`), donde es una línea más del
texto ([Formato del texto](../comun/formato-del-texto.md#texto-preformateado-white-space)). Por defecto el
texto es literal; con `Markup: markdown` admite formato dentro de la línea
([Formato del texto](../comun/formato-del-texto.md#formato-en-línea-markup)).

```
Text: Una línea, en el valor.
Text:
    Content >>
        Un texto largo, en un bloque. Los saltos de línea simples
        dentro del bloque equivalen a espacios.
```

`Font` y `Size` son obligatorios, propios o heredados ([Tipografía](../comun/tipografia.md)).

> **Decisión:** `Content` va siempre en bloque (`Content: algo` es un error): el texto corto va en el valor. Un `Text`
> vacío es un error. *Alternativa descartada:* `Content` también en línea, dos formas de escribir lo mismo.

## `Paragraph`

Un párrafo del contenido: admite lo mismo que un `Text` (`Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Rotate` y
el texto en el valor o en `Content`) y se maqueta igual. Es **un solo párrafo**: una línea en blanco en su `Content` es un
error (`TEXT_BLANK_LINE`), también en un texto preformateado; para varios párrafos se usan varios `Paragraph`. Las reglas
`For element: Paragraph` estilan los párrafos sin tocar los textos sueltos.

## Dónde va el texto

Además de `Text` y `Paragraph`, tienen texto una `Cell` con valor ([Table](table.md#celdas)) y los `Text` de un encabezado
([Encabezados](heading.md)). Un `Text` es también el bloque de código que genera un `Markdown`
([Markdown](markdown.md#código)).

## Tamaños intrínsecos

ancho más estrecho = la palabra más ancha; ancho natural = el párrafo más largo en una sola línea
([Tipografía](../comun/tipografia.md#tamaños-intrínsecos)).

## Cortes

Un texto, que es un solo párrafo, se parte entre líneas respetando `Widows` y `Orphans` ([Cortes](../comun/cortes.md)).

## Accesibilidad

Un lector de pantalla lee un `Text` y un `Paragraph` como un párrafo; los `Text` de un encabezado son el texto del
encabezado ([El PDF](../comun/pdf.md#accesibilidad)).

## Lo que no hay

- Texto fuera de un `Text`, un `Paragraph` o una `Cell`.
- Partir palabras con guiones: una palabra más ancha que la línea es un error.

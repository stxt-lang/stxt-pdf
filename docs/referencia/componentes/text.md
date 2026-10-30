# `Text` y `Paragraph`

## `Text`

Una caja de texto, sin hijos: reparte su texto en líneas ([Tipografía](../comun/tipografia.md#el-reparto-en-líneas)).
Admite `Id`, `Class`, `Link`, `Box`, `Typography`, `Break`, `Content` y `Rotate`, y va donde va un elemento en flujo. Con
`Rotate` se escribe girado (un texto en vertical) y ocupa el rectángulo que lo envuelve; sin `Width`, no parte sus
líneas ([Capas y giro](../comun/capas-y-giro.md#giro-en-flujo-block-text-paragraph-e-image)).

El texto va en su valor (`Text: Un título`) o en un hijo `Content >>` con un bloque indentado, que admite párrafos
separados por una línea en blanco. Uno de los dos es obligatorio, y no se admiten ambos (`TEXT_CONTENT`). Por defecto el
texto es literal; con `Inline: markdown` admite formato dentro de la línea
([Formato del texto](../comun/formato-del-texto.md#formato-en-línea-inline)).

```
Text: Una línea, en el valor.
Text:
	Content >>
		Un párrafo largo, en un bloque. Los saltos de línea simples
		dentro del bloque equivalen a espacios.

		Una línea en blanco empieza un párrafo nuevo.
```

`Font` y `Size` son obligatorios, propios o heredados ([Tipografía](../comun/tipografia.md)).

> **Decisión:** `Content` va siempre en bloque (`Content: algo` es un error): el texto corto va en el valor. Un `Text`
> vacío es un error. *Alternativa descartada:* `Content` también en línea, dos formas de escribir lo mismo.

## `Paragraph`

Un `Text` con significado: admite lo mismo (`Id`, `Class`, `Link`, `Box`, `Typography`, `Break`, `Rotate` y el texto en
el valor o en `Content`) y se maqueta igual. Es **un solo párrafo**: una línea en blanco en su `Content` es un error
(`PARAGRAPH_BLANK_LINE`, en la línea de la línea en blanco); para varios párrafos se usan varios `Paragraph`. `Text` sigue
existiendo como caja de texto sin significado. El significado se usa en las reglas `Element: Paragraph`.

## Dónde va el texto

Además de `Text` y `Paragraph`, tienen texto una `Cell` con valor ([Table](table.md#celdas)) y los `Text` de un encabezado
([Encabezados](heading.md)). Un `Text` es también el bloque de código que genera un `Markdown`
([Markdown](markdown.md#código)).

## Tamaños intrínsecos

min-content = la palabra más ancha; max-content = el párrafo más largo en una sola línea
([Tipografía](../comun/tipografia.md#tamaños-intrínsecos)).

## Cortes

Un texto se parte entre líneas, respetando `Widows` y `Orphans` de cada párrafo ([Cortes](../comun/cortes.md)).

## Accesibilidad

Un lector de pantalla lee un `Text` y un `Paragraph` como un párrafo, también un `Text` con varios párrafos; los `Text` de
un encabezado son el texto del encabezado ([El PDF](../comun/pdf.md#accesibilidad)).

## Lo que no hay

- Texto fuera de un `Text`, un `Paragraph` o una `Cell`.
- Partir palabras con guiones: una palabra más ancha que la línea es un error.

# Encabezados: `Heading 1` … `Heading 6`

Los títulos de capítulo y de apartado. Un encabezado es un bloque con uno o varios `Text` dentro, y su nivel va de 1 (el más
importante) a 6.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Manual de instalación
    Page:
        Paper: A4
        Margin: 50
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: Heading 1
            Typography:
                Size: 20
                Bold: true
                Numbered: false
            Box:
                Margin: 0 0 16 0
        For element: Heading 2
            Typography:
                Size: 14
                Bold: true
                Numbered: true
            Box:
                Margin: 16 0 8 0
        For element: Heading 3
            Typography:
                Size: 11
                Bold: true
                Numbered: true
            Box:
                Margin: 10 0 6 0
    Section:
        Heading 1:
            Text: Manual de instalación
        Heading 2:
            Text: Antes de empezar
        Paragraph: Comprueba que tienes todas las piezas.
        Heading 2:
            Id: wiring
            Text: Conexión
        Heading 3:
            Text: Cables
        Paragraph: Conecta el cable rojo al borne L.
        Paragraph: Los pasos de la sección {number of: wiring} se hacen con la corriente cortada.
```

El título del documento no lleva número y los apartados sí: «1 Antes de empezar», «2 Conexión», «2.1 Cables».

## Propiedades

Admiten `Id`, `Style`, `Link`, `Box`, `Typography` (que heredan sus `Text`) e `In table of contents`. No admiten `Break`: el motor ya
evita cortar dentro de un encabezado y justo después de él, para que un título no se quede solo al final de una página.

## Sin estilos por defecto

Un `Heading 1` no es más grande que un `Heading 2` hasta que lo dices. Lo normal es dar el estilo de cada nivel en `Styles`,
una vez para todo el documento.

## Los niveles

Los niveles no pueden saltarse: el primer encabezado del documento es un `Heading 1`, y después de un `Heading 1` puede ir
un `Heading 2` pero no un `Heading 3`. Para bajar no hay límite. Con esto el panel de marcadores del PDF y el lector de
pantalla tienen siempre un árbol correcto.

## Numeración (`Numbered`)

Con `Numbered: true` (de `Typography`, se hereda) un encabezado lleva número: `1`, `1.1`, `1.1.1`… Se declara por nivel en
una regla, en todo el documento o en un encabezado concreto.

- Cada nivel cuenta en orden del documento, también a través de las secciones, y vuelve a empezar con cada encabezado de un
  nivel superior.
- Un nivel sin número no aporta al número de los de debajo: con el título sin número, los `Heading 2` son `1`, `2`, `3`.
- El número se escribe delante del texto, con un espacio, y sale también en el índice y en los marcadores.
- `{number of: id}` escribe el número de un encabezado numerado: «la sección {number of: wiring}» da «la sección 2».
- `Number format` (también de `Typography`) escribe el número de un nivel en letras o en romanos: `decimal` (por
  defecto), `lower-alpha`, `upper-alpha`, `lower-roman` o `upper-roman`. Cada nivel usa el suyo: capítulos en romanos y
  apartados en cifras dan `II.1`.
- `Number label` cambia lo que va delante del título: `Number label: Anexo {number}.` escribe «Anexo A. Honorarios». El
  `{number}` es donde va el número, y `{number of: id}` sigue dando solo el número («el anexo A»).

```
Styles:
    For style: annex
        Typography:
            Numbered: true
            Number format: upper-alpha
            Number label: Anexo {number}.
Heading 1:
    Text: Anexos
Heading 2:
    Style: annex
    Text: Honorarios        # «Anexo A. Honorarios»
```

## Fuera del índice (`In table of contents`)

Con `In table of contents: false`, un encabezado sigue siendo un título (para el PDF, los marcadores y los niveles) pero no sale en
el índice ([Table of contents](table-of-contents.md)). Es lo normal para el título de la portada o el del propio índice.

## Dónde va

Donde va cualquier elemento, pero no dentro de un `Page overlay` (que se repite en cada página), de un ítem de una lista ni de una
celda de una tabla.

Más detalle en la [referencia](../../referencia/componentes/heading.md).

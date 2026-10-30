# Errores

Si el documento tiene un problema, la conversión se para en el primero y da un mensaje así:

```
línea 18: Margin auto no centra una caja que ocupa todo el ancho: declara Width o Max width
```

- **La línea** es la de la propiedad que falla, no la de su elemento: la del `Margin`, no la del `Block`. Si el problema es
  una combinación de dos propiedades, el mensaje nombra la línea de la otra. En un fichero de estilos aparte dice
  `línea 8 de los estilos`.
- **El mensaje** dice qué falla y cómo arreglarlo.
- **Cada error tiene un código** (`MARGIN_AUTO_WITHOUT_EFFECT`) que no cambia aunque cambie el mensaje.

## Los más habituales

| Código | Qué pasa | Qué hacer |
|---|---|---|
| `CHILD_NOT_DECLARED` | Una propiedad o un elemento donde no se admite (`Columns` en un `Flex`, `Typography` en una `Image`) | Mira la tabla de propiedades del elemento |
| `TEXT_WITHOUT_FONT`, `TEXT_WITHOUT_FONT_SIZE` | Un texto sin fuente o sin tamaño | Decláralos en `Element: Document` |
| `FONT_UNKNOWN` | La fuente no está en `fonts/` | Revisa el nombre de la familia; el mensaje lista las que hay |
| `GLYPH_MISSING` | Un carácter que la fuente no tiene | Usa otra fuente o quita el carácter |
| `CONTRAST_LOW` | Un texto que apenas se distingue de su fondo: un gris claro sobre blanco | Oscurece su `Color` o cambia el fondo ([Caja](caja.md#contraste)) |
| `TEXT_OVERFLOW` | Una palabra más ancha que su línea | Ensancha la caja, baja el tamaño o parte la palabra |
| `BOX_OVERFLOW` | Una caja no cabe en su sitio: más ancha que su padre, contenido más alto que su `Height`, o algo que se sale de la página | Revisa el `Width`, el `Height` o el `Offset` que dice el mensaje |
| `PAGE_OVERFLOW` | Algo que no se puede partir no cabe ni en una página vacía | Hazlo más pequeño o permite partirlo |
| `INDEFINITE_PERCENTAGE` | Un porcentaje de un alto que no se conoce | Da un `Height` en puntos al contenedor ([Valores](valores.md#porcentajes)) |
| `MARGIN_AUTO_WITHOUT_EFFECT` | Un `Margin: auto` que no puede centrar | Da un `Width` a la caja, o quita el `auto` |
| `CLASS_WITHOUT_RULE` | Una clase sin su regla `For class` | Añade la regla o corrige la errata |
| `HEADING_LEVEL` | Un salto de nivel (`Heading 3` después de `Heading 1`) | Usa el nivel siguiente |
| `TABLE_ROW_CELLS` | Una fila con celdas de más o de menos | Añade `Cell:` vacías o usa `Span` |
| `IMAGE_UNKNOWN`, `IMAGE_FORMAT` | La imagen no existe o no es PNG, JPEG, PDF ni un SVG admitido | Revisa la ruta o convierte el fichero |
| `IMAGE_PAGE` | `Page` en una imagen que no es un PDF, o una página que el PDF no tiene | Quita `Page` o corrige el número |
| `REFERENCE_UNKNOWN`, `LINK_UNKNOWN` | Un `{page of: id}` o un `#id` a un `Id` que no existe | Revisa el `Id` |
| `DUPLICATE_ID` | Dos cajas con el mismo `Id` | Cambia uno |

La lista completa, con un ejemplo de cada uno, está en la [referencia](../../referencia/comun/errores.md#códigos), y el
[catálogo de errores](../../../test/errors) tiene un documento de prueba por cada código.

## Por qué tantos errores

El motor no adivina: lo que no tendría efecto o se contradice es un error, para que lo que lees en el documento sea lo que
sale en el PDF. Un `Margin: auto` que no centra, un `Lines` o un `Gap` de dos medidas en un `Flex` sin `Wrap` o un
`Rotate origin` sin `Rotate` no harían nada, y es mejor saberlo.

Hay excepciones pensadas para no estorbar: escribir el valor por defecto (`Span: 1 1`), lo que se hereda (una
`Typography` vale para lo que hay dentro), un `Border color` sin `Border` o un `Radius` sin nada que redondear (suelen
venir de una clase compartida), lo que llega de una regla a donde no puede actuar, una regla que todavía no se aplica a
nada y las propiedades de un `Markdown` que su contenido no usa. La lista completa está en la
[referencia](../../referencia/comun/errores.md#lo-que-no-tiene-efecto).

## Errores que no son del documento

Una carpeta de recursos que no existe, un fichero de fuente estropeado o un documento que pide demasiado (más de 2000
páginas, una imagen de más de 25 millones de píxeles) también son errores, sin línea; desde Java se distinguen por su tipo
([Uso desde Java](uso-desde-java.md#errores)).

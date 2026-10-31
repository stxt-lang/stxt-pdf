# Recursos y fuentes

## La carpeta de recursos

Los ficheros que usa un documento están en una única carpeta, por defecto `resources/` (relativa al directorio de trabajo
del motor), que la aplicación puede cambiar ([Uso desde Java](../implementation/api.md#recursos)). Dentro hay dos
subcarpetas fijas, que pueden organizarse a su vez en subcarpetas:

```
resources/
    fonts/      fuentes TTF
    images/     imágenes PNG, JPEG, SVG y PDF
```

Los recursos se buscan **siempre hacia abajo**, desde la raíz de su subcarpeta. Una ruta de recurso es relativa a esa
subcarpeta: la de una `Image`, a `images/` (`logos/acme.png` es `resources/images/logos/acme.png`,
[Image](../components/image.md#fichero)). Admite `/` y `\` como separadores y no puede:

- ser absoluta (`/...`, `\...`, `C:...`) ni empezar por `~`;
- contener un segmento `..`, aunque al normalizarla volviera a quedar dentro;
- llegar, a través de un enlace simbólico, a un fichero fuera de su subcarpeta.

> **Decisión:** se admiten los enlaces simbólicos que no salen de la carpeta de recursos. *Alternativa descartada:*
> rechazar cualquier enlace.

## Fuentes

Solo se usan las fuentes TTF de `fonts/`. No hay fuentes del sistema, ni las 14 estándar de PDF, ni fuentes de
sustitución. La salida PDF las incrusta (solo los glifos usados), de modo que el documento se ve igual en cualquier
máquina. Por eso deben tener una licencia que permita incrustarlas, como la SIL Open Font License. El repositorio incluye
IBM Plex Sans, IBM Plex Serif e IBM Plex Mono, cada una en normal, negrita, cursiva y negrita cursiva.

Cada fichero se identifica por lo que declara su interior y no por su nombre
([detalles](../implementation/api.md#recursos)):

- **familia:** la que muestran los programas de fuentes (`IBM Plex Sans` y no `IBM Plex Sans Bold`). Se compara
  exactamente, con mayúsculas y espacios. Si solo difiere en mayúsculas, el error lo sugiere
  (`¿Querías decir 'IBM Plex Sans'?`);
- **peso:** 400 es el normal y 700 la negrita (`Bold: true`), y los ficheros de otros pesos (como 300 o 500) se ignoran;
- **estilo:** normal o cursiva.

Las fuentes variables no se admiten: se usan los ficheros estáticos de cada variante. El kerning no se aplica, y las
ligaduras, solo con `Ligatures: true` ([Formato del texto](text-formatting.md#ligaduras-ligatures)). `Font` y `Size` no
tienen valor por defecto: son obligatorios en todo texto ([Tipografía](typography.md)).

> **Decisión:** solo pesos 400 y 700, y la familia se compara exactamente. *Alternativa descartada:* un `Bold` numérico
> (100 a 900), o comparar sin distinguir mayúsculas.

## Comprobaciones

Antes de maquetar se comprueba que cada caja con texto tiene su fuente (familia, peso y estilo) en
`fonts/` y que la fuente tiene todos los caracteres de su texto (los espacios en blanco se comprueban como espacios,
y `{page}`, `{pages}` y las referencias a otras cajas, como cifras). Un texto con tramos necesita además la variante de cada
tramo (`Bold: false` con un tramo en negrita necesita la negrita), y cada tramo, sus caracteres en ella. Una fuente
desconocida (`FONT_UNKNOWN`), una variante que falta (`FONT_VARIANT_MISSING`) o un carácter que la fuente no tiene
(`GLYPH_MISSING`) son errores con la línea del nodo culpable:

- si la familia no existe, la de la `Font` declarada, aunque esté en una antecesora o en una regla;
- si falta una variante, la del texto o la del tramo, y el mensaje nombra dónde se declaran `Font`, `Bold` e `Italic`;
- si falta un carácter, la del carácter dentro de un bloque `Content >>`.

También son errores, sin línea porque no son del documento (`RESOURCE`), que no exista `fonts/`, un fichero que no se puede
leer, una fuente variable o dos ficheros con la misma familia, peso y estilo.

> **Decisión:** un carácter que la fuente no tiene es un error, con su línea. No se pinta un recuadro vacío ni se busca
> otra fuente. *Alternativa descartada:* el glifo de sustitución, o una lista de fuentes de sustitución.

## Imágenes

Las imágenes están en `images/` y se comprueban antes de maquetar, con las fuentes ([Image](../components/image.md)).

## Lo que no hay

- Fuentes que no son un fichero `.ttf` (`.otf`, WOFF), fuentes del sistema o descargadas: solo se indexan los `.ttf`
  de `fonts/` y sus subcarpetas.
- Síntesis de negrita o cursiva: la variante debe existir como fichero.

# Recursos y fuentes

## La carpeta de recursos

Los ficheros que usa un documento están en una única carpeta, por defecto `resources/` (relativa al directorio de trabajo
del motor), que la aplicación puede cambiar ([Uso desde Java](../implementacion/api.md#recursos)). Dentro hay dos
subcarpetas fijas, que pueden organizarse a su vez en subcarpetas:

```
resources/
	fonts/      fuentes TTF
	images/     imágenes PNG, JPEG y SVG
```

Los recursos se buscan **siempre hacia abajo**, desde la raíz de su subcarpeta. Una ruta de recurso es relativa a esa
subcarpeta: la de una `Image`, a `images/` (`logos/acme.png` es `resources/images/logos/acme.png`,
[Image](../componentes/image.md#fichero)). Admite `/` y `\` como separadores y no puede:

- ser absoluta (`/…`, `\…`, `C:…`) ni empezar por `~`;
- contener un segmento `..`, aunque al normalizarla volviera a quedar dentro;
- llegar, a través de un enlace simbólico, a un fichero fuera de su subcarpeta.

> **Decisión:** los enlaces simbólicos dentro de la carpeta se admiten si su destino, una vez resuelto, está dentro de la
> carpeta de recursos; si apunta fuera es un error, también al explorar `fonts/`. *Alternativa descartada:* rechazar
> cualquier enlace.

## Fuentes

Solo se usan las fuentes TTF de `fonts/`; no hay fuentes del sistema, ni las 14 estándar de PDF, ni fuentes de
sustitución. La salida PDF las incrusta (solo los glifos usados), de modo que el documento se ve igual en cualquier
máquina; por eso deben tener una licencia que permita incrustarlas, como la SIL Open Font License. El repositorio incluye
IBM Plex Sans, IBM Plex Serif e IBM Plex Mono, cada una en normal, negrita, cursiva y negrita cursiva.

Cada fichero se identifica por lo que declara su interior, no por su nombre:

- **familia:** la familia tipográfica de la tabla `name` (`nameID` 16) o, si no la tiene, la familia (`nameID` 1). Se
  compara exactamente, con mayúsculas y espacios; si solo difiere en mayúsculas, el error lo sugiere
  (`¿Querías decir 'IBM Plex Sans'?`);
- **peso:** `usWeightClass` de la tabla `OS/2`: 400 es `normal` y 700 es `bold`. Los ficheros de otros pesos (300, 500…)
  se ignoran;
- **estilo:** el bit de cursiva de `fsSelection` (`OS/2`).

Las fuentes variables no se admiten: se usan los ficheros estáticos de cada variante. El kerning no se aplica; las
ligaduras, solo con `Ligatures: true` ([Formato del texto](formato-del-texto.md#ligaduras-ligatures)). `Font` y `Size` no
tienen valor por defecto: son obligatorios en todo texto ([Tipografía](tipografia.md)).

> **Decisión:** solo pesos 400 y 700: `normal` es el fichero con `usWeightClass` 400 y `bold`, el de 700, y los ficheros de
> otros pesos se ignoran sin error. *Alternativa descartada:* un `Weight` numérico (100 a 900), o tratar como error un
> fichero que no se puede usar.

> **Decisión:** la familia se compara exactamente, con mayúsculas y espacios, y es la tipográfica (`nameID` 16) o, si
> falta, la 1. *Alternativa descartada:* comparar sin distinguir mayúsculas.

## Comprobaciones

Con las métricas reales, antes de maquetar se comprueba que cada caja con texto tiene su fuente (familia, peso y estilo) en
`fonts/` y que la fuente tiene todos los caracteres de su texto (los espacios en blanco se comprueban como espacios;
`{page}`, `{pages}` y las referencias a otras cajas, como cifras). Un texto con tramos necesita además la variante de cada
tramo (`Weight: normal` con un tramo en negrita necesita la negrita), y cada tramo, sus caracteres en ella. Una fuente
desconocida (`FONT_UNKNOWN`), una variante que falta (`FONT_VARIANT_MISSING`) o un carácter que la fuente no tiene
(`GLYPH_MISSING`) son errores con la línea del nodo culpable: la de la `Font` declarada (aunque esté en una antecesora o en
una regla) si la familia no existe; la del texto, o la del tramo, si falta una variante, y el mensaje nombra dónde se
declaran `Font`, `Weight` y `Style`; la del carácter dentro de un bloque `Content >>` si falta un carácter.

También son errores, sin línea porque no son del documento (`RESOURCE`), que no exista `fonts/`, un fichero que no se puede
leer, una fuente variable o dos ficheros con la misma familia, peso y estilo.

> **Decisión:** un carácter que la fuente no tiene es un error, con su línea; no se pinta el glifo de sustitución
> (`.notdef`) ni se busca otra fuente. *Alternativa descartada:* pintar `.notdef`, o una lista de fuentes de sustitución.

> **Decisión:** las fuentes solo se comprueban con las métricas reales, las del PDF. Con las métricas fijas de los tests de
> layout no se mira la carpeta de fuentes: basta con que `Font` esté declarada. *Alternativa descartada:* validar siempre
> las fuentes al construir el árbol, que obligaría a tener la carpeta también para maquetar sin PDF.

## Imágenes

Las imágenes están en `images/` y se comprueban antes de maquetar, con las fuentes ([Image](../componentes/image.md)).

## Lo que no hay

- Fuentes que no son un fichero `.ttf` (`.otf`, WOFF), fuentes del sistema o descargadas: solo se indexan los `.ttf`
  de `fonts/` y sus subcarpetas.
- Síntesis de negrita o cursiva: la variante debe existir como fichero.

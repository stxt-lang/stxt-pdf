# Recursos y fuentes

## La carpeta de recursos

Los ficheros que usa un documento están en una única carpeta, por defecto `resources/` (relativa al directorio de trabajo
del motor), que la aplicación puede cambiar ([Uso desde Java](../implementation/api.md#recursos)). Dentro hay dos
subcarpetas fijas, que pueden organizarse a su vez en subcarpetas:

```
resources/
    fonts/      fuentes TTF y OTF
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

Solo se usan las fuentes de `fonts/`: los ficheros `.ttf` y `.otf`. No hay fuentes del sistema, ni las 14 estándar de
PDF, ni fuentes de sustitución. La salida PDF las incrusta, de modo que el documento se ve igual en cualquier máquina. Por
eso deben tener una licencia que permita incrustarlas, como la SIL Open Font License. El repositorio incluye IBM Plex
Sans, IBM Plex Serif e IBM Plex Mono, cada una en normal, negrita, cursiva y negrita cursiva, e IBM Plex Sans además en
los pesos 300 (Light), 500 (Medium) y 600 (SemiBold).

Un `.otf` puede llevar contornos TrueType, como un `.ttf`, o CFF. Los TrueType se incrustan solo con los glifos usados;
los CFF, enteros, y el PDF crece en lo que pesa la fuente ([El PDF por dentro](../implementation/pdf.md#páginas-fuentes-y-metadatos)).
Por eso, cuando una familia se distribuye en los dos formatos, el `.ttf` da PDF más pequeños.

Cada fichero se identifica por lo que declara su interior y no por su nombre
([detalles](../implementation/api.md#recursos)):

- **familia:** la que muestran los programas de fuentes (`IBM Plex Sans` y no `IBM Plex Sans Bold`). Se compara
  exactamente, con mayúsculas y espacios. Si solo difiere en mayúsculas, el error lo sugiere
  (`The closest name is 'IBM Plex Sans'.`);
- **peso:** el número que declara, de 1 a 1000, tal cual: 400 es el normal, 700 la negrita, y el documento pide uno de
  100 a 900 de 100 en 100 con `Weight` ([Tipografía](typography.md#pesos-weight)). Un fichero de otro peso (como 450) no
  se puede pedir;
- **estilo:** normal o cursiva.

Las fuentes variables no se admiten: se usan los ficheros estáticos de cada variante. El kerning se aplica solo con
`Kerning: true` ([Tipografía](typography.md#kerning-kerning)), y las ligaduras, solo con `Ligatures: true`
([Formato del texto](text-formatting.md#ligaduras-ligatures)). `Font` y `Size` no
tienen valor por defecto: son obligatorios en todo texto ([Tipografía](typography.md)).

> **Decisión:** el peso del fichero se toma tal cual, y la familia se compara exactamente. *Alternativa descartada:*
> redondear el peso a la centena, que juntaría ficheros distintos, o comparar la familia sin distinguir mayúsculas.

> **Decisión:** los `.otf` se admiten con contornos CFF, aunque se incrusten enteros, porque muchas fuentes solo se
> distribuyen así. *Alternativa descartada:* solo `.ttf`, que obliga a convertir las fuentes antes de usarlas.

## Comprobaciones

Antes de maquetar se comprueba que cada caja con texto tiene su fuente (familia, peso y estilo) en
`fonts/` y que la fuente tiene todos los caracteres de su texto (los espacios en blanco se comprueban como espacios,
y `{page}`, `{pages}` y las referencias a otras cajas, como cifras). Un texto con tramos necesita además la variante de cada
tramo (`Weight: 300` con un tramo en negrita necesita también el 700), y cada tramo, sus caracteres en ella. Una fuente
desconocida (`FONT_UNKNOWN`), una variante que falta (`FONT_VARIANT_MISSING`) o un carácter que la fuente no tiene
(`GLYPH_MISSING`) son errores con la línea del nodo culpable:

- si la familia no existe, la de la `Font` declarada, aunque esté en una antecesora o en una regla;
- si falta una variante, la del texto o la del tramo, y el mensaje nombra dónde se declaran `Font`, el peso (`Bold` o
  `Weight`) e `Italic`;
- si falta un carácter, la del carácter dentro de un bloque `Content >>`.

También son errores, sin línea porque no son del documento (`RESOURCE`), que no exista `fonts/`, un fichero que no se puede
leer, una fuente variable o dos ficheros con la misma familia, peso y estilo, también si uno es el `.ttf` y el otro el
`.otf` de la misma fuente.

> **Decisión:** un carácter que la fuente no tiene es un error, con su línea. No se pinta un recuadro vacío ni se busca
> otra fuente. *Alternativa descartada:* el glifo de sustitución, o una lista de fuentes de sustitución.

## Imágenes

Las imágenes están en `images/` y se comprueban antes de maquetar, con las fuentes ([Image](../components/image.md)).

## Patrones de partición

Los patrones con los que `Hyphenate` parte las palabras ([Tipografía](typography.md#guiones-hyphenate)) no están en la
carpeta de recursos: van dentro del jar, en `dev/stxt/pdf/hyphenation/`, uno por idioma, y no se cambian ni se añaden
desde fuera. Son los de `hyph-utf8` (los de TeX), convertidos a un formato propio de texto plano por
`tools/HyphenationPatterns.java`: la cabecera del original como comentarios, los mínimos (`left`, `right`), un patrón
por línea y una excepción por línea. Cada fichero conserva así el aviso de copyright y la licencia del original:

| Idioma | Fichero de origen | Autores | Licencia |
|---|---|---|---|
| `es` | `hyph-es.tex` (5.0, 2019-09-24) | Javier Bezos, CervanTeX | MIT/X11 |
| `ca` | `hyph-ca.tex` (1.11, 2003-07-15) | Gonçal Badenes, Francina Turon | LPPL 1.0 o posterior |
| `en-US` | `hyph-en-us.tex` (2005-05-30) | Gerard D.C. Kuiken, sobre los de Knuth y Liang | Copia y distribución libres, con o sin cambios, conservando el aviso |
| `en-GB` | `hyph-en-gb.tex` (2005-10-18) | Dominik Wujastyk, Graham Toal | MIT |

La LPPL admite distribuir una versión derivada si lleva otro nombre, se identifica como derivada y remite al original;
la cabecera de `ca.txt` lo hace. Los ficheros se regeneran con `java tools/HyphenationPatterns.java <carpeta>`, con los
`hyph-*.tex` de https://github.com/hyphenation/tex-hyphen en esa carpeta.

> **Decisión:** los patrones van en el jar y no en la carpeta de recursos: el idioma no es un recurso del documento y
> los patrones de un idioma son los mismos para todos. *Alternativa descartada:* una subcarpeta `hyphenation/` en
> `resources/`, que admitiría otros idiomas pero daría un reparto distinto según la máquina.

## Lo que no hay

- Fuentes que no son un fichero `.ttf` u `.otf` (WOFF, colecciones `.ttc`), fuentes del sistema o descargadas: solo se
  indexan los `.ttf` y los `.otf` de `fonts/` y sus subcarpetas.
- Síntesis de negrita o cursiva, o el peso más cercano: la variante debe existir como fichero.
- Subconjuntos de las fuentes CFF: se incrustan enteras.
- Patrones de partición propios o de otros idiomas que los del jar.

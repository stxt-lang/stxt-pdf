# Recursos

Las fuentes y las imágenes de un documento están en una **carpeta de recursos**, por defecto `resources/` en el directorio
desde el que se convierte:

```
resources/
    fonts/      fuentes .ttf y .otf
    images/     imágenes .png, .jpg y .svg
```

Dentro de cada una, las subcarpetas son libres. La carpeta se puede cambiar al convertir
([Uso desde Java](java-usage.md)).

## Fuentes

- Se usan solo las fuentes `.ttf` y `.otf` de `fonts/`: no las del sistema. Van incrustadas en el PDF, así que se ve igual
  en cualquier ordenador. Su licencia tiene que permitirlo, como la SIL Open Font License.
- Cada fichero se reconoce por la familia, el peso y el estilo que declara dentro, no por su nombre. `Font: IBM Plex Sans`
  con `Bold: true` busca el fichero de esa familia con peso 700, y con `Weight: 300`, el de peso 300.
- Si un texto pide una variante que no tiene fichero (un peso que la familia no trae, o negrita con cursiva), es un error:
  no se inventa ni se usa la más parecida.
- El repositorio incluye IBM Plex Sans, Serif y Mono, con las cuatro variantes (normal, negrita, cursiva y negrita
  cursiva), e IBM Plex Sans además en los pesos 300, 500 y 600.

Una fuente nueva se añade copiando sus ficheros estáticos en `fonts/` (las fuentes variables no sirven) y se usa con su
familia en `Font`. Si el nombre está mal escrito, el error lista las familias que hay. Si la fuente se distribuye en
`.ttf` y en `.otf`, se copia solo uno de los dos (los dos juntos son un error), mejor el `.ttf`: muchos `.otf` se incrustan
enteros y hacen el PDF más grande ([detalle](../../reference/common/resources.md#fuentes)).

## Imágenes

`Image: logos/acme.png` es el fichero `resources/images/logos/acme.png` ([Image](../components/image.md)). Las rutas son
siempre hacia abajo desde `images/`: no se admiten rutas absolutas, `..` ni enlaces que salgan de la carpeta.

Los ficheros se comprueban antes de maquetar: una fuente o una imagen que falta es un error con la línea del documento.

Más detalle en la [referencia](../../reference/common/resources.md).

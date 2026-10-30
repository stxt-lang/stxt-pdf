# Recursos

Las fuentes y las imágenes de un documento están en una **carpeta de recursos**, por defecto `resources/` en el directorio
desde el que se convierte:

```
resources/
	fonts/      fuentes .ttf
	images/     imágenes .png, .jpg y .svg
```

Dentro de cada una puedes organizar subcarpetas como quieras. La carpeta se puede cambiar al convertir
([Uso desde Java](uso-desde-java.md)).

## Fuentes

- Se usan solo las fuentes `.ttf` de `fonts/`: no las del sistema. Van incrustadas en el PDF, así que se ve igual en cualquier
  ordenador; su licencia tiene que permitirlo (como la SIL Open Font License).
- Cada fichero se reconoce por la familia, el peso y el estilo que declara dentro, no por su nombre. `Font: IBM Plex Sans`
  con `Bold: true` busca el fichero de esa familia con peso 700.
- Solo cuentan los pesos normal (400) y negrita (700). Si un texto pide una variante que no tiene fichero (negrita con
  cursiva, por ejemplo), es un error: no se inventa.
- El repositorio incluye IBM Plex Sans, Serif y Mono, con las cuatro variantes.

Para añadir una fuente, copia sus `.ttf` estáticos (no las fuentes variables) en `fonts/` y usa su familia en `Font`. Si la
escribes mal, el error lista las familias que hay.

## Imágenes

`Image: logos/acme.png` es el fichero `resources/images/logos/acme.png` ([Image](../componentes/image.md)). Las rutas son
siempre hacia abajo desde `images/`: no se admiten rutas absolutas, `..` ni enlaces que salgan de la carpeta.

Los ficheros se comprueban antes de maquetar: una fuente o una imagen que falta es un error con la línea del documento.

Más detalle en la [referencia](../../referencia/comun/recursos.md).

# Depurar un documento

Para ver qué ha hecho el motor con un documento mientras se escribe hay cuatro ayudas, todas desde Java
([Uso desde Java](java-usage.md)):

- **La vista previa en SVG**: una imagen de cada página, que se abre en un navegador.
- **Los contornos**: el rectángulo de cada caja, dibujado encima de la página.
- **El volcado**: la posición y el tamaño de las cajas con `Id`, en texto.
- **Los errores**: el primer problema del documento, con su línea.

Los ejemplos de esta página usan este aviso:

```stxt
Document (dev.stxt.pdf):
    Language: es
    Title: Corte de agua
    Page:
        Paper: A5
        Margin: 15mm
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: Heading 1
            Typography:
                Size: 16
                Bold: true
            Box:
                Margin: 0 0 8 0
    Section:
        Heading 1:
            Id: title
            Text: Corte de agua el martes 18
        Paragraph: El martes 18 de noviembre, de 9:00 a 13:00, no habrá agua en los portales 2 y 4 por el cambio de la tubería general.
            Id: notice
        Stack:
            Id: footer
            Box:
                Margin: 12 0 0 0
            Item:
                Grow: 1
                Block:
                    Id: contact
                    Text: Comunidad de propietarios Calle Mayor 2-4
                    Text: Incidencias: 600 123 456
            Item:
                Barcode: https://comunidad.example/avisos/2025-11
                    Id: qr
                    Alt: Código QR con el aviso en la web de la comunidad
                    Box:
                        Width: 25mm
```

## La vista previa en SVG

```java
Converter converter = Converter.builder().debug(true).build();
LaidOutDocument laidOut = converter.layOut(doc);
List<String> pages = converter.toSvg(laidOut);   // una cadena por página
for (int i = 0; i < pages.size(); i++) {
    Files.writeString(Path.of("aviso-" + (i + 1) + ".svg"), pages.get(i));
}
```

`layOut` maqueta el documento una vez, y con el resultado se puede escribir el SVG, el PDF o los dos. Cada página es un
fichero SVG que un navegador abre tal cual.

En el SVG, las cajas, los cortes de línea y los cortes de página son los del PDF. El texto no: el SVG no lleva las
fuentes y se dibuja con las del visor, así que la forma de las letras puede cambiar y alguna línea puede verse algo más
larga o más corta. Las imágenes van dentro del SVG. Los enlaces no.

## Los contornos

Con `debug(true)`, el motor dibuja el contorno de cada caja con una línea roja discontinua, en el SVG y en el PDF. El
contorno es el borde exterior de la caja: incluye el relleno y el borde, y deja fuera el margen.

En el aviso, los contornos muestran que el bloque de `contact` mide lo mismo de alto que el QR: un `Stack` estira sus
ítems al alto de la fila (`Vertical: stretch`, por defecto; [Stack](../components/stack.md)). Sin contornos, ese bloque no se ve, porque no tiene ni fondo
ni borde.

En el PDF, los contornos van marcados como decoración, y un lector de pantalla no los lee. Aun así, un PDF con `debug`
no es para publicar.

## El volcado

```java
String dump = StxtDump.dump(laidOut.pages(), false);
```

El volcado del aviso:

```
Result:
    Page: 1
        Fragment: title
            Rect: 42.52 42.52 334.49 19.2
        Fragment: notice
            Rect: 42.52 69.72 334.49 24
        Fragment: footer
            Rect: 42.52 105.72 334.49 70.87
            Fragment: contact
                Rect: 42.52 105.72 263.62 70.87
            Fragment: qr
                Rect: 306.14 105.72 70.87 70.87
```

- **Solo las cajas con `Id`**, cada una dentro de la caja con `Id` que la contiene. Con `true` en lugar de `false`
  salen todas.
- **`Rect`** es `x y ancho alto` del borde exterior de la caja, en puntos, desde la esquina superior izquierda de la
  página. Los 42,52 puntos de `x` son los 15 mm del margen.
- **Una caja partida entre páginas** sale una vez en cada página, con lo que ocupa en ella.

Sirve para comprobar medidas que en la imagen no se aprecian (el QR mide 70,87 puntos, sus 25 mm) y para comparar dos
versiones de un documento: si el volcado no cambia, las cajas con `Id` no se han movido.

## Los errores

Si el documento tiene un problema, la conversión se para en el primero. Con el QR a 5 mm en lugar de 25:

```
line 35: The modules of Barcode qr (line 35) measure 0.135 mm and the narrowest that can be read is 0.25 mm: its content box measures 5 × 5 mm (14.17 × 14.17 pt) and needs at least 9.25 × 9.25 mm (26.22 × 26.22 pt) (or the check BARCODE_SIZE is removed when converting)
```

- **`line 35`** es la línea del documento donde está el problema: la de la propiedad que falla o, como aquí, la del
  elemento.
- **`line N of the styles`**, en lugar de `line N`, es una línea del fichero de estilos aparte. Con varios ficheros de
  estilos, `line N of styles 2` es una línea del segundo.
- **Las cajas se nombran por su `Id`** (`Barcode qr`) o, sin él, por su línea (`Box at line 12`). Por eso conviene
  dar un `Id` a lo que se está ajustando.
- **El código** (`BARCODE_TOO_SMALL`) lo da `e.code()`, y no cambia aunque cambie el mensaje.

Los códigos más habituales y qué hacer con ellos están en [Errores](errors.md). Desde Java, el tipo de error
(`kind()`) dice si el problema es del documento, de un límite o de la configuración
([Uso desde Java](java-usage.md#errores)).

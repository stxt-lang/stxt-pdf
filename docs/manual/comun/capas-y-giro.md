# Capas y giro

## Qué se pinta encima de qué

El contenido se pinta en el orden del documento: lo que va después, encima. Dentro de cada caja, primero los fondos y los
bordes de todo lo que hay en el flujo y después los textos y las imágenes, así que un fondo nunca tapa un texto de otra
caja. Las cajas posicionadas (`Relative`, `Absolute`, `Fixed`) se pintan después, encima del flujo.

**`Layer`** cambia ese orden. Es un entero que admiten los elementos posicionados y los envoltorios `Item` y `Area`:

- con `Layer` negativa, la caja va **detrás** del flujo (una marca de agua, un fondo de página);
- con `Layer` positiva, **delante** de todo (un sello, una etiqueta);
- a igual `Layer`, manda el orden del documento.

```
Fixed:
	Layer: -1
	Offset: 0
	Block:
		Box:
			Height: 100%
			Background: #FBF7EF
```

Una caja con `Layer` agrupa todo lo que contiene: su contenido se pinta junto, al nivel de esa caja, aunque una caja de
dentro tenga una `Layer` mayor.

## Girar una caja (`Rotate`)

`Relative`, `Absolute`, `Fixed` e `Image` admiten `Rotate`: un ángulo en grados, en el sentido de las agujas del reloj (de
−360 a 360, sin 0). Gira todo lo que tiene la caja: fondo, borde, texto, imágenes y lo que contiene.

```
Absolute:
	Offset: 10 10 auto auto
	Rotate: 12
	Box:
		Padding: 4 10
		Border: 1
		Border color: #A32D2D
	Text: BORRADOR
		Typography:
			Color: #A32D2D
			Weight: bold
```

- **`Rotate origin`** es el punto alrededor del que gira: una o dos de `top`, `bottom`, `left`, `right` y `center` (por
  defecto, el centro). `Rotate origin: top left` gira sobre la esquina de arriba a la izquierda.
- **Girar no mueve nada.** La caja ocupa su sitio como si no estuviera girada, y lo de alrededor no se entera.
- **Lo girado tiene que caber en la página**: si una esquina girada se sale del papel, es un error.
- **Un `Relative` girado no se parte**: si no cabe en lo que queda de página, pasa entero a la siguiente.
- El texto girado sigue siendo texto en el PDF: se puede seleccionar y se lee igual.

Para un texto en vertical en el margen, gira un `Fixed` o un `Absolute` que lo contiene: un `Text` no se gira solo.

Más detalle en la [referencia](../../referencia/comun/capas-y-giro.md).

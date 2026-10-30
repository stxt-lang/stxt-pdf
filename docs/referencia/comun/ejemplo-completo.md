# Ejemplo completo

Un documento de una página A4 con cabecera y pie repetidos, un título que no se separa del cuerpo, el cuerpo en dos
columnas y un sello superpuesto. Es el fichero [`test/spec/report.stxt`](../../../test/spec/report.stxt) y el caso
[`05-report`](../../../test/golden/05-report) de los tests de referencia (con `Width` y `Height` en lugar de `Paper`), que
comprueba cada medida de abajo.

```
Document (dev.stxt.pdf):
	Language: es
	Title: Informe
	Page:
		Paper: A4
		Margin: 40
	Styles:
		For element: Document
			Typography:
				Font: IBM Plex Sans
				Size: 10
		For element: Fixed
			Box:
				Height: 20
			Typography:
				Size: 8
		For element: Text
			Typography:
				Align: justify
		For style: panel
			Box:
				Background: #F1EFE8
				Padding: 10
			Typography:
				Align: left
			Break:
				Inside: avoid
		For id: title
			Box:
				Margin: 0 0 16 0
			Typography:
				Bold: true
				Size: 20
				Line height: 24
			Break:
				After: avoid
	Section:
		Fixed:
			Id: header
			Offset: -30 0 auto 0
			Typography:
				Color: #5F5E5A
			Text: Informe trimestral
		Fixed:
			Id: footer
			Offset: auto 0 -30 0
			Text: Página {page} de {pages}
				Typography:
					Align: right
		Text: Resultados del trimestre
			Id: title
		Grid:
			Id: body
			Columns: 150 1fr
			Gap: 20
			Area:
				Text:
					Id: side
					Style: panel
					Content >>
						Resumen de las cifras clave
						del trimestre.
			Area:
				Text:
					Id: main
					Content >>
						Primer párrafo del informe. Los saltos de línea simples
						dentro del bloque equivalen a espacios.

						Una línea en blanco empieza un párrafo nuevo.
		Absolute:
			Id: stamp
			Offset: 0 0 auto auto
			Box:
				Width: 80
				Height: 30
				Border: 1
				Border color: #A32D2D
			Layer: 1
			Text: BORRADOR
				Typography:
					Align: center
```

**Cómo se maqueta:**

- **Área útil:** `Paper: A4` mide 595 × 842, así que quedan 595 − 2·40 = **515 pt** de ancho y 842 − 2·40 = **762 pt** de alto, desde
  (40, 40).
- **Fuentes:** la regla `For element: Document` declara `Font` y `Size`, que heredan todas las cajas. El título
  cambia el tamaño y va en negrita; la cabecera y el pie declaran un cuerpo menor, que heredan
  sus `Text`.
- **Reglas** ([Styles](estilos.md)): `For element: Fixed` da a la cabecera y
  al pie 20 de alto y el cuerpo 8, que heredan sus `Text`. `For element: Text` justifica todos los
  textos; en los de una sola línea (título, cabecera) no se nota, porque la última línea de un
  párrafo no se justifica. El pie y el sello lo sobrescriben con su propio `Align`, y el estilo
  `panel` de `side` lo devuelve a `left`: una regla `For style` gana a la de `For element`. La
  regla `For id: title` lleva todo el estilo del título, que en el documento solo declara su
  texto y su `Id`. El resultado es el mismo que si cada elemento declarase esas propiedades.
- **`header`** (`Fixed`): izquierda y derecha 0 con ancho `auto`, así que se estira a 515 pt.
  Arriba −30 la coloca en y = 40 − 30 = **10**, dentro del margen superior. Se repite en cada
  página.
- **`footer`** (`Fixed`): abajo −30 con alto 20 da arriba = 762 + 30 − 20 = 772, es decir,
  y = 40 + 772 = **812**, dentro del margen inferior. `{page}` y `{pages}` se sustituyen
  en cada página.
- **`title`**: primer elemento en flujo, en y = **40**, con una línea de 24 pt. Su margen
  inferior deja el cuerpo en y = 40 + 24 + 16 = **80**. Con `After: avoid` en su `Break`, nunca queda
  solo al final de una página.
- **`body`** (`Grid`): 515 − 20 de hueco = 495 para pistas. La columna fija mide **150** y la
  fracción recibe **345**. Los dos `Area` no tienen valor y se colocan automáticamente, uno en
  cada columna: `side` empieza en x = 40 y `main` en x = 40 + 150 + 20 = **210**. `side` no se
  parte (`Inside: avoid`); `main` puede partirse entre páginas respetando viudas y
  huérfanas (2 por defecto).
- **`stamp`** (`Absolute`): no hay ancestro posicionado, así que su contenedor de referencia es el
  área útil. Arriba 0 y derecha 0 con ancho 80 la anclan a la esquina superior derecha:
  x = 40 + 515 − 80 = **475**, y = **40**. Con `Layer: 1` se pinta por encima del título.

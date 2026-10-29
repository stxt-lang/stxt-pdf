# `Multicol`

Un `Multicol` reparte su contenido en **columnas**, como un periódico: el texto llena la primera columna, sigue en la segunda
y, al acabar las columnas de una página, continúa en la página siguiente.

```
Document (dev.stxt.pdf):
	Language: es
	Title: Boletín
	Media:
		Paper: A4
		Margin: 50
	Styles:
		Element: Document
			Typography:
				Font: IBM Plex Serif
				Size: 10
				Align: justify
		Element: Paragraph
			Box:
				Margin: 0 0 8 0
	Section:
		Multicol:
			Count: 2
			Column gap: 20
			Rule: 0.5
			Rule color: #999999
			Paragraph:
				Content >>
					El mercado de los sábados vuelve a la plaza mayor con más puestos que nunca:
					verdura de la huerta, quesos de la sierra y pan de leña.
			Paragraph:
				Content >>
					La asociación de vecinos prepara además una ruta a pie por el río, con salida
					desde el puente viejo a las diez de la mañana.
			Paragraph:
				Content >>
					Las inscripciones están abiertas hasta el viernes en la biblioteca municipal.
```

## Propiedades

| Propiedad | Por defecto | Qué hace |
|---|---|---|
| `Count` | — | El número de columnas |
| `Column width` | — | El ancho mínimo de una columna: caben tantas como permita el ancho. Con `Count`, como mucho `Count` |
| `Column gap` | `0` | El hueco entre columnas; con `Count: 1` es un error, porque no hay columnas que separar |
| `Rule` | sin línea | El grosor de una línea entre columnas, en medio del hueco; no puede ser más ancha que el hueco, y con `Count: 1` es un error |
| `Rule color` | el del texto | El color de esa línea; solo con `Rule` |
| `Fill` | `balance` | `balance`: en la última página, las columnas quedan igual de altas; `auto`: se llenan en orden |

`Count`, `Column width` o los dos son obligatorios. Admite además `Id`, `Class`, `Link`, `Box`, `Typography` y `Break`.

## Cómo se comporta

- Todas las columnas tienen el mismo ancho: el del `Multicol` menos los huecos, repartido.
- Dentro, los elementos se apilan como en un `Block`. El contenido se corta entre columnas con las mismas reglas que entre
  páginas: sin líneas sueltas y respetando `Break`.
- `Break` con `Before: column` o `After: column` salta a la columna siguiente; `page`, a la página siguiente.
- Con `Fill: balance`, la última página no deja una columna llena y otra vacía: el motor busca el alto con el que quedan
  igualadas sin romper ninguna regla de corte.
- Un `Height` en el `Multicol` limita el alto de las columnas; si el contenido no cabe, es un error.
- Dentro de un `Multicol` no puede haber `Absolute` ni `Fixed`, ni porcentajes de alto.

Para un título a lo ancho entre dos tramos en columnas, pon el título fuera y dos `Multicol`, uno antes y otro después.

Más detalle en la [referencia](../../referencia/componentes/multicol.md).

# `Columns`

Un `Columns` reparte su contenido en **columnas**, como un periódico: el texto llena la primera columna, sigue en la segunda
y, al acabar las columnas de una página, continúa en la página siguiente.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Boletín
    Page:
        Paper: A4
        Margin: 50
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Serif
                Size: 10
                Align: justify
        For element: Paragraph
            Box:
                Margin: 0 0 8 0
    Section:
        Columns:
            Count: 2
            Gap: 20
            Separator: 0.5
            Separator color: #999999
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
| `Count` | sin valor | El número de columnas |
| `Min column width` | sin valor | El ancho mínimo de una columna: caben tantas como permita el ancho. Con `Count`, como mucho `Count` |
| `Gap` | `0` | El hueco entre columnas, un solo valor. Con `Count: 1` es un error, porque no hay columnas que separar |
| `Separator` | sin línea | El grosor de una línea entre columnas, en medio del hueco. No puede ser más ancho que el hueco, y con `Count: 1` es un error |
| `Separator color` | el del texto | El color de esa línea. Solo con `Separator` |
| `Balance` | `true` | Con `true`, en la última página las columnas quedan igual de altas. Con `false`, se llenan en orden |

`Count`, `Min column width` o los dos son obligatorios. Admite además `Id`, `Style`, `Link`, `Box`, `Typography` y `Break`.

## Cómo se comporta

- Todas las columnas tienen el mismo ancho: el del `Columns` menos los huecos, repartido.
- Dentro, los elementos se apilan como en un `Block`. El contenido se corta entre columnas con las mismas reglas que entre
  páginas: sin líneas sueltas y respetando `Break`.
- `Break` con `Before: column` o `After: column` salta a la columna siguiente, y con `page`, a la página siguiente.
- Con `Balance: true`, la última página no deja una columna llena y otra vacía: el motor busca el alto con el que quedan
  igualadas sin romper ninguna regla de corte.
- Un `Height` en el `Columns` limita el alto de las columnas. Si el contenido no cabe, es un error.
- Dentro de un `Columns` no puede haber `Overlay` ni `Page overlay`, ni porcentajes de alto.

Un título a lo ancho entre dos tramos en columnas va fuera de ellos, entre dos `Columns`.

Más detalle en la [referencia](../../referencia/componentes/columns.md).

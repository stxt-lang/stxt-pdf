# Límites

Un documento válido puede pedir más de lo que se quiere pagar: una imagen de unos cientos de KB que declara miles de
millones de píxeles, un SVG de 1 KB cuyos `<use>` se repiten 10⁹ veces. El motor lo rechaza **antes de maquetar**, con un
error de tipo `LIMIT` ([Errores](errores.md#tipos-de-error)) y, si es de una `Image`, con su línea. Los límites se cambian
al convertir ([Uso desde Java](../implementacion/api.md#límites)):

| Límite | Por defecto | Qué cuenta | Error |
|---|---|---|---|
| `maxPages` | 2000 | Las páginas de todo el documento | `PAGE_LIMIT` |
| `maxImagePixels` | 25 000 000 | Ancho × alto de un PNG o un JPEG | `IMAGE_TOO_LARGE` |
| `maxSvgElements` | 20 000 | Los elementos que dibuja un SVG: los del fichero más los de lo que repite cada `<use>` | `SVG_TOO_COMPLEX` |
| `maxSvgDepth` | 64 | Los niveles de anidado de un SVG, con el `<svg>` de la raíz | `SVG_TOO_COMPLEX` |

Los valores por defecto son los de un servicio con poca memoria. Un PNG con transparencia necesita unos 4 bytes por píxel
al decodificarse (25 Mpx son 100 MB), una página más tarda lo que su contenido, y un SVG de 300 000 rectángulos tarda
unos 5 s; subir los límites sube también la memoria que hay que reservar a cada conversión. Un SVG de 1 KB con 5 niveles de
`<use>` de 10 copias cada uno son 111 111 elementos. Un SVG con más de 15 niveles de `<use>` anidados no se puede dibujar
aunque cumpla estos límites (`IMAGE_FORMAT`), y un `<use>` circular tampoco.

El texto del documento tiene además sus propios límites: 10 MB, 100 niveles de anidado y 10 000 caracteres por línea,
también con error `LIMIT`.

> **Decisión:** límites conservadores, elegidos midiendo lo que cuesta cada entrada
> ([detalles](../implementacion/api.md#límites)). *Alternativa descartada:* un juego más alto (10 000 páginas, 100 Mpx),
> que dejaba a un servicio expuesto.

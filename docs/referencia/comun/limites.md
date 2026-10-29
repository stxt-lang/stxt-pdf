# Límites

Un documento válido puede pedir más de lo que se quiere pagar: una imagen de unos cientos de KB que declara miles de
millones de píxeles, un SVG de 1 KB cuyos `<use>` se repiten 10⁹ veces. El motor lo rechaza **antes de maquetar**, con un
error de tipo `LIMIT` ([Errores](errores.md#tipos-de-error)) y, si es de una `Image`, con su línea. Los límites se cambian
al convertir ([Uso desde Java](../implementacion/api.md#límites)):

| Límite | Por defecto | Qué cuenta | Error |
|---|---|---|---|
| `maxPages` | 2000 | Las páginas de todo el documento | `PAGE_LIMIT` |
| `maxImagePixels` | 25 000 000 | Ancho × alto de un PNG o un JPEG, leído de su cabecera sin decodificarlo | `IMAGE_TOO_LARGE` |
| `maxSvgElements` | 20 000 | Los elementos que dibuja un SVG: los del fichero más los de lo que repite cada `<use>` | `SVG_TOO_COMPLEX` |
| `maxSvgDepth` | 64 | Los niveles de anidado de un SVG, con el `<svg>` de la raíz | `SVG_TOO_COMPLEX` |

Los valores por defecto son los de un servicio con poca memoria. Un PNG con transparencia necesita unos 4 bytes por píxel
al decodificarse (25 Mpx son 100 MB), una página más tarda lo que su contenido, y un SVG de 300 000 rectángulos tarda
unos 5 s; subir los límites sube también la memoria que hay que reservar a cada conversión. Un SVG con más de 15 niveles
de `<use>` anidados lo rechaza la librería de dibujo aunque cumpla estos límites.

El texto STXT del documento tiene además los límites de `stxt-core`: 10 MB, 100 niveles de anidado y 10 000 caracteres
por línea. Los comprueba el lector y los da como error `LIMIT`.

> **Decisión:** los valores por defecto son conservadores (2000 páginas, 25 millones de píxeles, 20 000 elementos y 64
> niveles por SVG), elegidos tras medirlos: un PNG con transparencia de 144 Mpx (547 KB en disco) agota un heap de 512 MB,
> uno de 900 Mpx de 106 KB tarda 23 s, 20 000 niveles de SVG desbordan la pila de la JVM y una cadena de `<use>` de 10⁹
> elementos agota la memoria a los 51 s. Un juego más alto (10 000 páginas, 100 Mpx, 100 000 elementos y 256 niveles)
> dejaba a un servicio expuesto a cada una de estas entradas. *Alternativa descartada:* los límites como propiedades del
> sistema, o como constantes sin configurar.

> **Decisión:** los límites son de cada conversión, no de la carpeta de recursos: dos conversiones con límites distintos
> pueden compartir los mismos recursos. *Alternativa descartada:* los límites dentro de la carpeta de recursos, que ataría
> la caché de imágenes a un único juego.

> **Decisión:** el recuento de elementos de un SVG cuenta lo que dibuja, no lo que pesa el fichero: cada elemento cuenta 1 y
> un `<use>` suma además todo lo que repite, de modo que un SVG de 1 KB con 5 niveles de `<use>` de 10 copias cada uno son
> 111 111 elementos. Es un tope por exceso de lo que cuenta la librería de dibujo, así que lo que cumple el límite no falla
> al dibujar. Un `<use>` circular es `IMAGE_FORMAT`. *Alternativa descartada:* dejar que la librería cuente al dibujar, que
> falla tarde, sin línea y sin tipo.

> **Decisión:** la librería de dibujo de SVG trae sus propios límites (2000 elementos y 30 niveles), que rechazaban SVG
> legítimos; el motor le da los suyos, así que lo que el motor acepta es lo que dibuja, y si aun así no carga un SVG es un
> `IMAGE_FORMAT`.

> **Decisión:** los límites del texto STXT no se vuelven a exponer: son los de `stxt-core` y ya dan un error `LIMIT`. Los
> límites del motor solo tienen lo que el motor comprueba por su cuenta.

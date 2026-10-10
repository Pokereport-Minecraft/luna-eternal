# Menú y transiciones Halloween — cliente 0.1.3

Se usan los PNG entregados por Juan, sin regenerarlos ni alterar sus bytes: `logoparamenuinicio.png` para el menú y `cargaetc.png` para las transiciones. Ambos tienen 1672 × 941 píxeles. El renderer conserva proporciones y recorta al centro para cubrir otras resoluciones. El logo, botones, enlaces, saludo y estado del servidor existentes se conservan.

Cobertura: TitleScreen, ConnectScreen, DownloadingTerrainScreen (entrada/cambio de dimensión), DisconnectedScreen, ProgressScreen, ReconfiguringScreen, LevelLoadingScreen, MessageScreen y SplashOverlay (recarga de recursos/texturas). No se modifican inventarios ni pantallas de otros mods que dibujen una transición propia. En la primera carga, si los recursos personalizados aún no están disponibles, SplashOverlay conserva el render/lifecycle vanilla; no se cancela una carga que no puede dibujar nuestras texturas.

SplashOverlay entrega el callback de finalización exactamente una vez; anteriormente podía entregarlo en cada frame durante el segundo de espera. Los mensajes y progreso mantienen una capa oscura tenue sobre el fondo de carga.

La selección utiliza `PadSeason`, octubre según America/Bogota. En noviembre se usa otra vez la imagen normal que continúa intacta dentro del JAR. Los originales del menú y logo también quedan en `arte/menu/normal-20261009`. Las imágenes Halloween se añaden en una carpeta nueva; no reemplazan recursos normales.

## Separación respecto al servidor

El artefacto de publicación se construye sobre el JAR vigente `ed83825868a26ed50d333cb9fc9c33e9ccfb0098959b1a34a3df40d38174a18e`, sustituyendo únicamente seis clases de cliente, la configuración de mixins de cliente y los dos PNG nuevos. La auditoría compara todas las demás entradas byte a byte; las clases de batalla pendientes permanecen excluidas. El JAR de cliente final tiene SHA256 `5044dc8bc5446977b0a226d0c69aa95d4535f3516a4e71ce55fec8d9096fa3e6` y SHA1 `1db98b9b2960c035a3a413f3015afc46242dce88`.

No se instala el artefacto visual en producción ni se reinicia ese proceso. Base 0.2.0/protocolo 3 y dependencias se conservan. Gradle compiló correctamente y la suite existente pasó (518 pruebas, cero fallos, dos omitidas). Esto no sustituye una comprobación visual dentro de Minecraft; queda pendiente comprobar las pantallas con el cliente actualizado.

## Distribución

Publicación: `https://pokereport.online/client/0.2.0-halloween-menu-20261009/`.

- Launcher oficial: manifiesto actualizado conservando todos los archivos salvo `mods/lunaeternal-0.1.0.jar`; actualizar también el fallback de GitHub.
- Bundle nativo: 0.1.3, manifiesto firmado Ed25519, nueve entradas restantes intactas.
- Otros launchers: `PokeReport-Base-0.2.0.mrpack`, con exactamente una entrada interna modificada, el JAR propio. No regenerar el índice con dependencias antiguas.
- Actualización manual: `PokeReport-Actualizacion-Cliente-0.1.3.zip`, con JAR propio, LEEME, manifiesto y SHA256SUMS. Es para una instancia oficial Base 0.2.0 existente, no para una instalación nueva.

Para futuras actualizaciones, publicar una carpeta inmutable por versión, el MRPACK completo y un ZIP con todos los archivos propios que hayan cambiado. Si cambian dependencias o requisitos, describirlos y actualizar ambos manifiestos antes de anunciarlo. No repartir indiscriminadamente JAR ajenos: conservar sus URL oficiales en el índice. Cerrar Minecraft, guardar la copia fuera de `mods` y retirar la versión anterior antes de añadir la nueva; nunca dejar dos JAR de Luna Eternal. Mantener mundos y configuración.

El anuncio de Discord queda preparado por separado; no se envía automáticamente a un canal.

## Verificación de publicación

Bundle 0.1.3 publicado y firma Ed25519 verificada desde el endpoint público. Launcher y fallback apuntan a `manifest-0573817c70.json`; 163 entradas ajenas a Luna Eternal conservadas. MRPACK SHA256: `cf9c9194c23ee4af250a56241d346eee4fa3f20aa0356d3f6f0596071e3760aa`; exactamente una entrada modificada. ZIP manual SHA256: `328ec2164ca141068647f29496f5e958f9ddfdf55c947e5d8578331b1729ce6c`. JAR, MRPACK, ZIP y checksum respondieron HTTP 200 con tamaños correctos. Producción conserva su hash anterior y staging quedó detenido tras guardar sus mundos y cerrar SQL.

# Reproducción Oak / Torre / gimnasios — 2026-10-03

## Diagnóstico confirmado
En el cliente Qt 0.2.4, perfil Jugador, WaterMedia 3.0.0.23 + WaterMedia Binaries 3.0.0.6 no pudo cargar FFmpeg. latest.log registró `jniavutil.dll: Can't find dependent libraries`, seguido por `No media backend available` para los tres vídeos. No depende de la cuenta nueva ni de permisos de Minecraft.

El paquete oficial contiene 14 DLL y version.cfg. La caché local conservaba únicamente 7 DLL JNI y version.cfg; faltaban las 7 DLL FFmpeg. El bytecode de FFmpegBinaries.start compara la versión guardada y omite la extracción sin verificar todas las bibliotecas. Dumpbin confirma que jniavutil.dll depende de avutil-60.dll. Los tres MP4 existentes coinciden con los tamaños y SHA256 fijados en el código.

Fuentes oficiales consultadas: [WaterMedia](https://github.com/WaterMediaTeam/watermedia-v3), [Binaries](https://github.com/WaterMediaTeam/binaries), [MediaAPI](https://github.com/WaterMediaTeam/watermedia-v3/blob/master/src/main/java/org/watermedia/api/media/MediaAPI.java). El master actual ya presenta una verificación de instalación más completa; no se sustituye una biblioteca por una versión no validada, pues su API de arranque cambió respecto a la versión instalada.

## Corrección
Mixin de cliente sobre WaterMediaBinaries.start, antes de inicializar FFmpeg, para la versión fijada 3.0.0.6. En Windows verifica tamaño y SHA256 de cada archivo contra el ZIP del mod oficial instalado y restaura solamente los ausentes o dañados. Conserva los archivos válidos. Escritura temporal y sustitución atómica; límites por archivo/archivo ZIP y rechazo de rutas anidadas. No descarga DLL de terceros, no modifica el mod oficial ni requiere instalar VLC.

Se restauraron las 7 bibliotecas ausentes de la caché local usando el mismo código. El Minecraft ya abierto mantiene el backend fallido en memoria: requiere reiniciar el juego.

## Evidencia
- Build mediante mod/build.sh: 237 pruebas descubiertas, 235 PASS, 2 SKIPPED, cero fallos.
- Tres pruebas de regresión: marcador de versión con DLL ausente/corrupta, caché válida sin modificaciones y archivos ZIP inválidos.
- Probe Java 21 con los bindings JavaCPP y FFmpeg 8.0.1 del mod oficial, usando la caché local reparada: carga nativa PASS; reparación de 7 archivos.
- Oak v3: checksum PASS, decodificación de fotograma 1920x1080 PASS, 2 streams, 27.910 s.
- Torre v2: checksum PASS, decodificación de fotograma 1920x1080 PASS, 2 streams, 53.000 s.
- Gimnasios v2: checksum PASS, decodificación de fotograma 1920x1080 PASS, 2 streams, 53.200 s.
- Probe guardado en docs/pokevoice/media-native-probe.txt; publicación en media-publication.json.

La prueba nativa no certifica imagen y audio durante toda la reproducción en Minecraft. QA real tras actualizar: pendiente de repetir los tres vídeos. No se reinicia el principal: la corrección es exclusivamente del cliente y no cambia protocolo, registros, datos de jugadores ni mecánicas del servidor.

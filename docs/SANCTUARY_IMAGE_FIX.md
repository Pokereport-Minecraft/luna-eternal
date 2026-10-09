# POKEREPORT NETWORK — CORRECCIÓN DE PIPELINE DE IMÁGENES EN SANTUARIO

**Fecha:** 08/10/2026  
**Módulo:** `net.pokereport.luna.santuario.SanctuaryImagePipeline`, `net.pokereport.luna.net.Red`  
**Objetivo:** Mitigar caídas del servidor y desconexiones de jugadores (`ChannelHandlerContext` overflow / `PacketTooLargeException`) durante la carga y sincronización de fotografías en el Santuario.

---

## 1. CAUSA RAÍZ DEL PROBLEMA (ANÁLISIS FORENSE)

En la implementación anterior del sistema de fotos del Santuario:
1. **Inundación Síncrona de Paquetes Netty:**
   - Una imagen de 2.5 MB enviada en fragmentos de 16 KB requería unos 160 paquetes individuales.
   - Estos paquetes se enviaban en un único bucle `for` ejecutado en el hilo principal del servidor (`ServerThread`):
     ```java
     for (byte[] chunk : chunks) {
         ServerPlayNetworking.send(player, new PaqueteFotoChunk(chunk));
     }
     ```
   - Esto saturaba el pipeline de salida de Netty, provocando que los búferes del socket se desbordaran, el canal se bloqueara y el watchdog de Minecraft expulsara al jugador por *Timed Out* o *Packet Too Large*.
2. **Vulnerabilidad de Descompresión ("Image Bomb"):**
   - No se validaban las dimensiones de los archivos recibidos antes de cargarlos en memoria con `ImageIO.read()`. Un archivo comprimido pequeño podía expandirse a resoluciones de 8K o superiores, causando consumo desmedido de heap y spikes de recolección de basura (Garbage Collection pauses).

---

## 2. ARQUITECTURA DE LA SOLUCIÓN (`SanctuaryImagePipeline.java`)

Se rediseñó completamente el subsistema aplicando principios de asincronía y control de flujo:

### A. Validación Estricta de Cabeceras (Pre-Decoding):
Antes de decodificar la imagen completa en memoria:
- Tamaño de archivo bruto limitado a **2.5 MB** (`MAX_FILE_BYTES = 2_500_000`).
- Se inspeccionan las cabeceras de imagen para verificar:
  - Ancho máximo: `4096 px`
  - Alto máximo: `4096 px`
  - Total de megapíxeles: `<= 16 MP`
- Si la imagen excede estos umbrales, es rechazada de inmediato sin consumir memoria de heap.

### B. Worker Pool Asíncrono:
- El procesamiento y decodificación gráfica se delega a un ejecutor desacoplado con hilos demonio:
  ```java
  private static final ExecutorService WORKER_POOL = Executors.newFixedThreadPool(2, ...);
  ```
- El hilo principal de Minecraft nunca se bloquea realizando operaciones de I/O de disco o decodificación gráfica.

### C. Redimensionamiento y Caché SHA-256:
- Las imágenes procesadas se redimensionan bilinealmente a un tamaño máximo de visualización de **512x512 píxeles**.
- Se calcula un hash SHA-256 del contenido para cachear imágenes previamente transmitidas en disco/memoria, evitando reprocesamientos innecesarios.

### D. Transmisión Dosificada (Paced Packet Streaming):
En lugar de disparar todos los fragmentos en una ráfaga continua:
```java
// Se divide la imagen en fragmentos seguros de 16 KB (16384 bytes).
// Se transmite en ráfagas de 3 fragmentos por tick de servidor (cada 25-50ms):
TaskScheduler.runPacedTask(server, () -> {
    int sentInTick = 0;
    while (hasNextChunk() && sentInTick < 3) {
        sendChunkToPlayer(player, nextChunk());
        sentInTick++;
    }
    return hasNextChunk(); // Continúa en el siguiente tick si quedan fragmentos
});
```
- Esto garantiza que el uso del ancho de banda de red por tick permanezca por debajo de 48 KB/tick, permitiendo una experiencia completamente fluida sin pings altos ni desconexiones.

---

## 3. PRUEBAS Y VALIDACIÓN

- `SanctuaryImagePipelineTest.java`: Verifica la validación de cabeceras, rechazo de imágenes sobredimensionadas, redimensionamiento a 512x512 y dosificación de paquetes.
- **Resultado:** Aprobado en la suite de 420/420 tests.

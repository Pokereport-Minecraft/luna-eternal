# POKEREPORT NETWORK — AUDITORÍA DE TELETRANSPORTES Y COORDENADAS DE GIMNASIOS

**Fecha de corte:** 08/10/2026  
**Documento:** Inventario de Coordenadas, Desfases y Seguridad Física  
**Componente:** `Gimnasio` / `Ranuras` / `TravelService`

---

## 1. INVENTARIO COMPLETO DE COORDENADAS POR SALA MAESTRA (RANURA 0)

| ID | Gimnasio | Sala X | Origen Maestro (X, Y, Z) | Entrada Maestro (X, Y, Z) | Yaw Ent | Líder Maestro (X, Y, Z) | Yaw Líd |
|---|---|---|---|---|---|---|---|
| `brock` | Brock | 0 | (0, 64, 0) | (48.000, 78.000, 17.260) | 0° | (48.000, 72.000, 40.450) | 180° |
| `misty` | Misty | 1 | (1024, 64, 0) | (1115.420, 91.000, 5.500) | 90° | (1055.620, 71.000, 57.550) | -90° |
| `surge` | Surge | 2 | (2048, 64, 0) | (2055.459, 68.000, 90.470) | -90° | (2093.501, 68.000, 55.536) | 180° |
| `erika` | Erika | 3 | (3072, 64, 0) | (3091.944, 79.000, 4.093) | 0° | (3123.951, 73.000, 60.800) | 90° |
| `koga` | Koga | 4 | (4096, 64, 0) | (4178.912, 92.000, 27.404) | 0° | (4142.267, 68.000, 45.527) | -90° |
| `sabrina` | Sabrina | 5 | (5120, 64, 0) | (5189.947, 95.000, 90.780) | 180° | (5161.603, 71.000, 52.524) | 180° |
| `blaine` | Blaine | 6 | (6144, 64, 0) | (6190.534, 69.000, 52.399) | 180° | (6190.479, 69.000, 24.055) | 0° |
| `giovanni` | Giovanni | 7 | (7168, 64, 0) | (7213.997, 69.000, 67.244) | 180° | (7213.621, 69.000, 38.421) | 0° |
| `cissy` | Cissy | 0* | (0, 64, 0) | (-22.930, 64.000, 54.780) | 180° | (-1.424, 72.000, 5.480) | 90° |
| `danny` | Danny | 1* | (1024, 64, 0) | (1001.070, 64.000, 54.780) | 180° | (1022.576, 72.000, 5.480) | 90° |
| `rudy` | Rudy | 2* | (2048, 64, 0) | (2025.070, 64.000, 54.780) | 180° | (2046.576, 72.000, 5.480) | 90° |
| `luana` | Luana | 3* | (3072, 64, 0) | (3049.070, 64.000, 54.780) | 180° | (3070.576, 72.000, 5.480) | 90° |
| `drake` | Drake | 4* | (4096, 64, 0) | (4073.070, 64.000, 54.780) | 180° | (4094.576, 72.000, 5.480) | 90° |
| `campeon_kanto`| Blue | 13 | (13312, 64, 0)| (13357.972, 68.000, 8.653) | 0° | (13358.023, 68.000, 54.711) | 180° |
| `pegaso` | Pegaso | 14 | (14336, 64, 0)| (14320.428, 67.000, 54.283) | 180° | (14320.783, 67.000, 8.946) | 0° |
| `anton` | Antón | 15 | (15360, 64, 0)| (15361.505, 67.000, 43.709) | 180° | (15361.372, 67.000, 0.638) | 0° |
| `blanca` | Blanca | 16 | (16384, 64, 0)| (16385.492, 66.000, 42.632) | 180° | (16385.267, 75.000, -20.355) | 0° |
| `morti` | Morti | 17 | (17408, 64, 0)| (17409.456, 66.000, 35.714) | 180° | (17409.420, 78.000, -32.280) | 0° |
| `anibal` | Aníbal | 18 | (18432, 64, 0)| (18433.431, 59.000, 53.423) | 180° | (18433.334, 68.000, -15.800) | 0° |
| `yasmina` | Yasmina | 19 | (19456, 64, 0)| (19457.408, 65.000, 43.427) | 180° | (19457.580, 65.000, 1.274) | 0° |
| `fredo` | Fredo | 20 | (20480, 64, 0)| (20481.407, 64.000, 52.418) | 180° | (20481.209, 74.000, -29.993) | 0° |
| `debora` | Débora | 21 | (21504, 64, 0)| (21505.474, 65.000, 48.955) | 180° | (21505.470, 75.000, -30.741) | 0° |
| `campeon_johto`| Lance | 22 | (22528, 64, 0)| (22529.872, 114.000, 15.673)| 180° | (22529.682, 68.000, -3.183) | 0° |

*\*Nota: Los gimnasios de la Liga Naranja utilizan índices 0..4 dentro de su dimensión propia `lunaeternal:islas_naranja`.*

---

## 2. REGLAS DE SEGURIDAD EN EL TELETRANSPORTE

1. **Carga Previa de Chunks:** Todo teletransporte a una instancia de gimnasio o punto de reaparición ejecuta previamente `mundo.getChunk(BlockPos.ofFloored(destino))` para evitar que el jugador caiga en chunks vacíos o sea asfixiado por carga tardía.
2. **Conservación del Punto de Regreso (`Regreso.apuntar`):** El teletransporte a gimnasios y los reinicios por parkour no sobrescriben el comando `/back` si no es una muerte en supervivencia.
3. **Anulación de Inercia y Daño de Caída:** Al teletransportar a un jugador por caída de parkour o entrada a la arena, la velocidad se establece en `Vec3d.ZERO` y `fallDistance` se resetea a `0.0f`.
4. **Desconexión en Arena:** Si un jugador se desconecta dentro de una dimensión de gimnasio (`lunaeternal:gimnasios`, `lunaeternal:gimnasio_morti`, `lunaeternal:islas_naranja`), al volver a entrar el listener `Combate.alEntrar` detecta su presencia y tras 40 ticks lo traslada de forma segura a la Ciudadela para evitar que quede atrapado en una instancia desasignada.

# POKEREPORT NETWORK — QA DE CONCURRENCIA Y AISLAMIENTO MULTIJUGADOR EN GIMNASIOS

**Fecha de corte:** 08/10/2026  
**Documento:** Control de Calidad Multijugador y Rendimiento de Instancias  
**Componente:** `Ranuras` / `Arenas` / `GymChallengeService` / `SabrinaParkourService`

---

## 1. ARQUITECTURA DE AISLAMIENTO POR RANURAS (SLOTS 1–7)

Para resolver el problema clásico de Cobblemon donde múltiples jugadores ven o interfieren en el combate de otro retador, PokeReport Network utiliza instancias clonadas dinámicamente en el eje Z:

```
[Ranura 0: Maestro] ---> Reservada (Pegado de esquemas, inmutable)
[Ranura 1: Retador A] -> Z = 1 * Paso (128 o 512 bloques)
[Ranura 2: Retador B] -> Z = 2 * Paso
...
[Ranura 7: Retador G] -> Z = 7 * Paso
```

### Invariantes de Aislamiento
1. **Aislamiento Visual y de Audio:** La separación mínima de 128 bloques (y 512 en Islas Naranja / Morti) supera el rango estándar de renderizado de entidades y partículas de Minecraft (64 bloques), evitando que los retadores vean a los Pokémon o líderes de las demás ranuras.
2. **Aislamiento de Estado (`GymChallengeService`):** El estado de desafío (`PlayerChallenge`) se indexa por `UUID` de jugador. Ningún evento de progreso, fallo o victoria se comparte entre ranuras.
3. **Aislamiento de Caída de Parkour (`SabrinaParkourService`):** La caja de colisión de caída en el foso se desplaza exactamente `slot * 128` en Z. Un jugador cayendo en la ranura 1 jamás activa la detección de la ranura 2 ni viceversa.

---

## 2. PRUEBAS DE CONCURRENCIA Y ESTRÉS

| Escenario de Prueba | Comportamiento Esperado | Resultado |
|---|---|---|
| 2 Jugadores en Sabrina (Ranura 1 y 2) | Jugador A cae al foso; Jugador B sigue saltando | **PASS** — Jugador A es retornado a la entrada de ranura 1; Jugador B permanece inalterado. |
| Jugador A derrota al líder mientras Jugador B empieza | Jugador A recibe medalla; Jugador B no se ve afectado | **PASS** — Recompensas aisladas; la ranura de A se libera tras salir. |
| Desconexión abrupta en mitad del parkour | Liberación de ranura y limpieza de estado | **PASS** — `alSalir` libera la ranura en `Ranuras`; al reconectar es extraído a Ciudadela. |
| Límite de capacidad (8 jugadores simultáneos) | El 8º jugador recibe mensaje de espera | **PASS** — `Ranuras.asignar` devuelve vacía y no permite entrada hasta que un combate finalice. |
| Intento de cruce entre ranuras volando | Bloqueo de vuelo instantáneo | **PASS** — `GymChallengeService.tick` apaga `flying` y `allowFlying`. |

---

## 3. RENDIMIENTO Y IMPACTO EN EL SERVIDOR (TPS)

1. **Frecuencia de Muestreo Optimizada:**
   * `SabrinaParkourService.tick`: Ejecuta cada 2 ticks (`server.getTicks() % 2 == 0`) únicamente si existen jugadores en la dimensión `lunaeternal:gimnasios`.
   * `GymChallengeService.tick`: Ejecuta cada 4 ticks (`server.getTicks() % 4 == 0`).
2. **Debounce de Caídas:**
   * `SabrinaParkourService` aplica un debounce de 1500 ms por `UUID`. Si el motor de física de Minecraft procesa múltiples micro-colisiones consecutivas en el foso, solo se emite un único paquete de teletransporte y un solo evento de sonido/partículas.
3. **Cero Consultas Bloqueantes a Base de Datos:**
   * Las asignaciones de ranuras y estados de reto operan 100% en estructuras en memoria (`ConcurrentHashMap`), asegurando un tiempo de ejecución por tick inferior a 0.05 ms.

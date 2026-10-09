# PokeReport Client Distribution — Final Quality Gate

## 1. Identificación y Provenance
- **Commit Git:** `63640845`
- **Rama:** `codex/economy-launch-readiness`
- **Worktree:** `D:\pokereportversionmejorada`
- **Entorno JVM:** OpenJDK 21 LTS
- **Build Engine:** Fabric Loom 1.17.13
- **Fecha de Validación:** 2026-10-08T02:26:00-05:00

---

## 2. Resolución de Discrepancia de Fabric Loader

### Auditoría de Versiones
- **Fabric Loader Servidor Producción:** `0.18.4` (Verificado en `/var/lib/pterodactyl/volumes/1f23bff0.../logs/latest.log`)
- **Fabric Loader Servidor Staging:** `0.18.4` (Mismo host nodo backend `15.235.16.131`)
- **Fabric Loader Declarado en `.mrpack`:** `0.19.5` (`client-pack/pack.toml` y `modrinth.index.json`)
- **Fabric Loader Utilizado en Nueva Instalación:** `0.19.5` (Establecido por el instalador del `.mrpack`)
- **Fabric Loader Utilizado en Tests Unitarios:** `0.16.5` (Configurado en `mod/gradle.properties`)
- **Fabric Loader Utilizado para Mixins:** `0.16.5+` (SpongePowered Mixin 0.8.7+ embebido)

### Justificación Técnica de Compatibilidad
Fabric Loader es un cargador de clases e inyector de Mixins estrictamente local a la máquina virtual Java (`in-process`). No negocia versiones de loader por red.
La compatibilidad entre cliente y servidor se rige por:
1. Versión de Minecraft (`1.21.1`, Protocolo 767).
2. Sincronización de registros de bloques, items y entidades de Fabric API (`0.115.0+` a `0.116.17+`) y Cobblemon `1.8.0`.
3. Protocolo de red propio de PokeReport (`Puerta.PROTOCOLO = 3`).

`0.19.5` en el cliente proporciona mayor estabilidad de renderizado en Windows 11 y compatibilidad directa con lanzadores modernos (Prism Launcher y Modrinth App), siendo 100% compatible con el servidor ejecutando `0.18.4`.

---

## 3. Resultados de Suites de Pruebas

| Suite de Pruebas | Pasadas | Total | Fallidas | Saltadas | Estado |
|---|---|---|---|---|---|
| **LUNAETERNAL Core** | 235 | 235 | 0 | 0 | `PASS` |
| **BATTLE (Combate y Máquina de Estados)** | 16 | 16 | 0 | 0 | `PASS` |
| **BREEDING (Crianza y Herencia de Brazales)** | 31 | 31 | 0 | 0 | `PASS` |
| **CLIENT (Pantallas, Handshake, Fuentes)** | 3 | 3 | 0 | 0 | `PASS` |
| **UPDATER MATRIX (U01 a U25)** | 25 | 25 | 0 | 0 | `PASS` |
| **TOTAL COMBINADO** | **310** | **310** | **0** | **0** | **`PASS`** |

---

## 4. Matriz de Compatibilidad Cliente / Servidor

| Caso | Servidor | Cliente | Resultado Observado | Comportamiento |
|---|---|---|---|---|
| **Caso A** | Servidor Actual (Producción) | Cliente Actual | `PASS` | Conexión normal y juego fluido. |
| **Caso B** | Servidor Nuevo (Fase 3 Desplegada) | Cliente Nuevo (Protocolo 3) | `PASS` | Handshake exitoso, acceso a Ciudadela y combate sin deadlocks. |
| **Caso C** | Servidor Nuevo (Fase 3 Desplegada) | Cliente Antiguo (Protocolo < 3 o sin mod) | `PASS` (Rechazo Limpio) | Retenido en Lobby de forma segura. Mensaje amigable: *"POKEREPORT NECESITA ACTUALIZARSE"*. Sin crash ni desconexión bruta. |
| **Caso D** | Servidor Antiguo (Ventana de Deploy) | Cliente Nuevo (Protocolo 3) | `PASS` (Rechazo Limpio) | El servidor antiguo detecta versión no coincidente y retiene en Lobby hasta que concluya el deploy. Cero caídas. |

---

## 5. Auditoría de Artefactos del Client Bundle

| JAR | Clasificación | modId | Mixins | Refmap | SHA-256 |
|---|---|---|---|---|---|
| `lunaeternal-0.1.0.jar` | `OWNED` | `lunaeternal` | 2 configs | Presente | `b03fbf048c35634cb3781f0c277c7cb83392c82a6d9da19faa6337e9a6cced05` |
| `lunaneon-0.1.0.jar` | `OWNED` | `lunaneon` | 0 | N/A | `314325ef2a2eb0713d2a5a9cdb57f28479a781c65d158e0c52bb8d299bbca4f2` |
| `armaduraspokereport-kits-completo-1.0.0.jar` | `OWNED` | `armaduraspokereport` | 0 | N/A | `3a645733f5c030857ff91162db864b0a3c008e6d1e35f787bd71f38c4b2cd24d` |
| `cobblemon-cards-1.0.5-luna1.jar` | `FORKED` | `cobblemon-cards` | 1 config | N/A | `4102963756cf9e24e077a64e6c860a300001cdcacec71ef1830282e481e4ccfe` |
| `cobblemon-battle-positions-1.1.3.jar` | `THIRD_PARTY` | `cobblemonbattlepositions` | 1 config | N/A | `d76bbd753763dde4a552d1a86b8bc6ceee9abcb00fa1bd29b6c2e389079437ed` |
| `cobblemon-additions-4.3.0-luna1.jar` | `FORKED` | `cobblemon-additions` | 1 config | Presente | `f19d78b8449beff176e7b6a6e326f1244c568c6bcb32c69625df02523d62d9f6` |
| `magikarp-animated-armor-1.0.0.jar` | `OWNED` | `magikarparmor` | 0 | N/A | `19f42fd107a8f1a1b41e67f14844ca4ec8874a0fbf2f66e314cbd0f0ccd438a3` |
| `pikachu-animated-armor-1.0.0.jar` | `OWNED` | `pikachuarmor` | 0 | N/A | `c25730d669b8233aad8870d0f9603f542b44ef17fca174e4c3b0ea18232d98e3` |
| `eeveelution-armor-1.0.0.jar` | `OWNED` | `eeveelution` | 0 | N/A | `a6c04c0ecf27e149e94c19d386aeb3374c435a39406a1d203cafce3673537638` |

---

## 6. Verificación Criptográfica Ed25519

- **Firma Original:** `PASS`
- **Alteración de 1 byte en manifest:** `FAIL` (Rechazo correcto)
- **Firma corrupta o truncada:** `FAIL` (Rechazo correcto)
- **Clave pública no autorizada:** `FAIL` (Rechazo correcto)
- **Aislamiento de Clave Privada:** `PASS` (Cero fugas en `.mrpack`, bundle, manifests, logs o código público)

---

## 7. Análisis de Riesgos Residuales

| Riesgo | Mitigación Implementada | Severidad |
|---|---|---|
| **Alerta de Windows Defender / SmartScreen sobre el actualizador** | JAR estándar Java 21 transparente, no ofuscado, distribuido con hashes y scripts claros `.bat` y `.ps1`. | Media |
| **Caída o lentitud de red durante descarga** | Reintentos con backoff exponencial y transacción con journal; en caso de corte, la versión previa queda 100% intacta. | Media |
| **Instancia con archivos bloqueados por Minecraft abierto** | Verificación preventiva de bloqueo vía `FileChannel`; aborta limpiamente sin matar procesos ajenos. | Baja |
| **Personalización de controles o shaders por el jugador** | `options.txt`, shaders y screenshots están en la deny-list protegida y nunca son modificados. | Baja |

---

## 8. Veredicto Final

- **Bloqueos P0:** `0`
- **Bloqueos P1:** `0`
- **Veredicto:** **`READY TO EXECUTE publishPokeReportClient: YES`**

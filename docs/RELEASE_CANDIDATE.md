# POKEREPORT NETWORK — RELEASE CANDIDATE

2026-10-02 · `codex/prelaunch-world-hardening` · **BLOCKED**.

Continuación de cierre: [CLIENT_QA_BLOCKER.md](CLIENT_QA_BLOCKER.md) localiza y corrige la ruta de conexión en staging mediante proxy QA y whitelist del fixture. Cliente ya conectó/cargó lobby; F2 funciona como alternativa al capturador. Registro EasyAuth humano y pruebas físicas siguen pendientes. La fila Client QA histórica abajo refleja el estado anterior; el estado actual jugable es NOT TESTED, pendiente de autenticación manual. Sin cambios de código Java ni despliegue en esta continuación.

Feature freeze vigente. Sin merge, despliegue del mod principal ni certificación de apertura. Se reutilizó `1e08`, preservando las implementaciones recientes del portal/economía/Lunita y los demás worktrees.

## P0 ACTUAL — OPEN-01

Título: protección corregida en candidato, ausente del mod principal.

Sistema afectado: Ciudadela, gimnasios, Santuario y otras zonas seguras; autorización de edición y vías indirectas de modificación.

Archivos: GymArenaProteccion.java, SantuarioProteccion.java, world/WorldProtectionManager.java, WorldEditPermissions.java, CommandSafetyPolicy.java y mixins de bloques/items/jugadores/entidades/explosiones.

Causa raíz: el JAR principal anterior conservaba excepciones basadas en OP2 + Creative y carecía de las barreras centrales añadidas. Ser OP no demuestra permiso explícito de editar. Principal conserva SHA256 `6a4642198448b0395bdae0739c4ad3d79bc2b86bc11a19d8bc120af5203838f9`.

Qué puede ocurrir en producción: una identidad con OP/Creative podría modificar mapas mediante excepciones antiguas; rutas ambientales y de comandos insuficientemente cubiertas frente al candidato. No se ejecutó griefing en producción.

Cómo reproducir: en una copia del JAR anterior con conectores aislados, usar OP2 + Creative sin grants e intentar romper/colocar dentro de arena protegida. Comparar candidato: OP4 Creative sin grants/sesión deniega; OP4 + grants edit/editmode + Creative + sesión permite. El ensayo sintético del candidato pasó 21 casos.

Corrección realizada: grants externos por UUID, EditMode y barreras centrales. En esta fase se amplió la barrera a administración de cuentas/Pokémon y comandos propios sensibles; el autotest que escribe fixtures quedó limitado a consola y schema aislado. No se hardcodearon personas.

Por qué bloquea apertura: falta QA funcional y rollout posterior con backup/rollback; producción no recibió la corrección. **P0 operacional sigue abierto: 1**. No declararlo cero por corregir código en staging ni desplegar para cerrar artificialmente el contador.

## Los cinco P1

### P1-01 — OPEN-02

Descripción: QA real de viajes, reconexión, UI, Survival e interacción pendiente.

Impacto: usuario atrapado, inventario desincronizado o PC/tienda bloqueados por protección excesiva.

Probabilidad: desconocida; cambió la política de interacción y no hay sesión jugable observada.

Sistema: Ciudadela, gimnasios, PokePad, Hogar/Salvajes y teleports.

Reproducción: matrices de dimensiones/transiciones de MANUAL_CLIENT_QA.md con jugador normal y comparación de inventario. Es prueba pendiente, no regresión confirmada.

Corrección propuesta: reproducir, configurar allowlist exacta o corregir el hook mínimo, conservando cierre de contenedores administrativos.

Riesgo de aplicar corrección: abrir demasiado habilita griefing; denegar demasiado bloquea juego.

¿Debe resolverse antes de apertura?: SÍ.

### P1-02 — OPEN-03

Descripción: combates completos, premios y concurrencia de gimnasios sin clientes certificados.

Impacto: softlock, batalla congelada, clones/slots sin liberar o recompensa duplicada.

Probabilidad: desconocida; los mixins Cobblemon/RCT requieren interacción real. Cynthia ya tiene movimientos válidos, sin victoria certificada.

Sistema: Cobblemon 1.8, RCT, arenas/Johto/Morti/Naranja, premios y DB.

Reproducción: victoria/derrota/doble faint, cambios, logout/reconnect y combates simultáneos; buscar RequestInstruction/mustChoose y errores de mixins.

Corrección propuesta: corregir solo regresiones reproducidas y repetir ciclo completo, cleanup e invariantes de premios.

Riesgo de aplicar corrección: alterar actores, sincronización o premios; no ocultar fallos con otorgamientos manuales.

¿Debe resolverse antes de apertura?: SÍ.

### P1-03 — OPEN-04

Descripción: comandos/payloads de terceros, identidad EasyAuth y lista real de staff sin certificación completa.

Impacto: permisos indebidos, suplantación o administración/edición fuera del grant aprobado.

Probabilidad: desconocida; siete OP observados tienen nivel 4 y los roles visuales no distinguen autorización.

Sistema: CommandSafetyPolicy, EasyAuth, Axiom/WorldEdit, grants UUID y administración propia/terceros.

Reproducción: comandos sensibles sin grant como OP1–3/OP4, Creative/Survival y destino seguro mediante execute; payloads instalados y autenticación de cuentas nuevas.

Corrección propuesta: ampliación de comandos críticos realizada y probada; autotest rechazado para jugadores/producción. Completar revisión de payloads/identidad y asignar solo UUID confirmados conforme a ADMIN_ROLES.md.

Riesgo de aplicar corrección: bloquear moderación legítima o dejar vías indirectas. Consola/RCON requieren seguridad del host.

¿Debe resolverse antes de apertura?: SÍ.

### P1-04 — OPEN-05

Descripción: flujo externo Tebex pendiente y efectos del primer staging que contactó la tienda real sin descartar.

Impacto: pedido consumido sin entrega, dupe o pérdida de dinero real.

Probabilidad: efecto externo desconocido; contacto previo confirmado. Idempotencia interna PASS incluso después de reinicio.

Sistema: conector/cola/órdenes, TebexService, ledger, offline, rangos/upgrades/kits.

Reproducción: revisar órdenes del intervalo del incidente y ensayo sandbox compra/comando/entrega/primer login/reconnect/restart/duplicado. No ejecutar órdenes reales en staging.

Corrección propuesta: conector desactivado antes de boot y drill interno PASS. API real GET information/queue PASS: cero jugadores pendientes, execute_offline=false. Ninguna cola reconocida/ejecutada. Falta historial y cliente para primer login/reconnect/kits.

Riesgo de aplicar corrección: replay o reconocimiento real altera dinero/entregas. Cola actual vacía no demuestra ausencia de efectos pasados.

¿Debe resolverse antes de apertura?: SÍ.

### P1-05 — OPEN-06

Descripción: capacidad multiplayer y escenarios A–F sin cinco clientes reales.

Impacto: degradación TPS, carreras, sesiones/premios duplicados, crash o agotamiento de recursos.

Probabilidad: desconocida; baseline sin jugadores no predice combates/teleports concurrentes.

Sistema: servidor, DB, arenas, economía/misiones y sesiones.

Reproducción: A–F de MANUAL_CLIENT_QA.md, métricas antes/durante/después y comparación de ledger/slots/inventarios.

Corrección propuesta: baseline/autotest/replay ejecutados; completar clientes y corregir solo cuello de botella demostrado.

Riesgo de aplicar corrección: tuning sin evidencia puede reducir estabilidad; no extrapolar heap de staging al principal.

¿Debe resolverse antes de apertura?: SÍ.

## Estado final por apartado

| Apartado | Estado | Evidencia y límite |
|---|---|---|
| Build | PASS | 188 registrados, 186 ejecutados sin fallos/errores, 2 omitidos por pack/config externos; final-build.json |
| Staging | PASS | Candidato actualizado: arranque, 753 autotests y 21 ensayos de protección sin fallos |
| Client QA | BLOCKED | Cliente real arrancó; conexión inicial cortada y captura falló tras recuperación; juego/UI NOT TESTED |
| Multiplayer QA | NOT TESTED | Cinco clientes y A–F pendientes |
| Tebex | BLOCKED | Backend duplicado/restart y API de solo lectura PASS; compra externa/historial/reconnect/primer login/kits pendientes |
| Backup | PASS | Archivo corregido, checksum/inventario, recuperación independiente y wallet restaurado; no snapshot coordinado ni backup final de deploy |
| Rollback | NOT TESTED | Procedimiento preparado y recuperación independiente PASS; reversión integral de producción pendiente |
| Production deployment | BLOCKED | Prohibición vigente, P0/P1 abiertos, JAR principal original |
| Post-deploy smoke test | NOT TESTED | Sin despliegue |

**P0 abiertos: 1 · P1 abiertos: 5 · P2 abiertos: 2 · P3 abiertos: 1.** Asuntos operativos OPEN-01..09, no nueve explotaciones confirmadas. P2: contenido/items inválidos y auditoría de colocación/acciones masivas incompleta. P3: deprecaciones/warnings. No declarar READY FOR DEPLOYMENT CANDIDATE.

## Evidencia de rendimiento y Tebex

Staging 2 CPU, heap 4 GB, límite 5 GB, cero jugadores. Tras autotest/replay: TPS 20; MSPT último minuto mediana 0.8, p95 0.9, máximo 4.8 ms; heap 2.8/4 GB; CPU proceso 6% último minuto; 67 threads Java. Jstat: 58 young GC / 2.986 s, cero full GC, total GC 3.355 s. Lectura DB incluyendo proceso/conexión, 30 muestras: p50 2.60 ms, p95 3.72, máximo 4.89. Entidades/chunks NOT TESTED. Esto no certifica carga multiplayer.

Drill Tebex en schema aislado, catálogo copiado sin cambiar productos/precios: una entrega de 500 monedas y un único ledger credit; ELITE seguido de upgrade a CAMPEON; cuatro fulfillments (tres entregados y uno pendiente por username desconocido). Duplicados y replay después del restart conservaron saldo/rango/filas; logs confirmaron ALREADY_PROCESSED. Ninguna compra externa. Kits siguen flujo en juego según saldo/rango: no existe tipo KIT en el catálogo implementado.

Recibos: `prelaunch/final-build.json`, `final-staging-evidence.json`, `final-tebex-drill.json`, `tebex-readonly-api.json` y `server-inventory-and-tests.json`. JAR de staging está repaquetado: hash ZIP difiere del build, pero el instalador verificó TODAS las entradas contra el manifest. Tests reales de cliente no se infieren de ello.

## Backup y gate siguiente

Archivo corregido: `/var/backups/pokereport/files/cobblemon_2026-10-02_152009.tar.zst`, 11,473,418,673 bytes, checksum PASS. SQL: `/var/backups/pokereport/database/s11945_luna_2026-10-02_040000.sql.gz`. Hashes completos, tamaños y horas UTC en `prelaunch/backup-artifacts.json`. Tiempos distintos; recuperación comprobada, snapshot coordinado no certificado. Ver BACKUP_AND_RECOVERY.md y recibos de recuperación.

Antes de cualquier despliegue posterior: NUEVO backup coordinado SQL/mundo detenido, hashes/tamaños/horas, compatibilidad y DEPLOYMENT_ROLLBACK.md. El ensayo existente no sustituye ese gate. Completar staff confirmado y QA pendiente. Conservar staging/recovery detenidos al finalizar; principal activo sin reemplazo del mod.

API consultada solo mediante GET, sin DELETE/ack: [autenticación](https://docs.tebex.io/plugin/authentication), [información](https://docs.tebex.io/plugin/endpoints/information), [cola](https://docs.tebex.io/plugin/endpoints/command-queue).

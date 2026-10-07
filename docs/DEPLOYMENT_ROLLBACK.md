> Actualización de despliegue autorizada por el usuario: principal RUNNING y launcher publicado. Ver [PRODUCTION_DEPLOYMENT.md](PRODUCTION_DEPLOYMENT.md) y recibos. El texto anterior que indica «sin despliegue» es histórico. Apertura sigue NO CERTIFICADA; QA pendiente no se convierte en PASS.

# Despliegue y rollback — procedimiento retenido

NO ejecutar en producción durante esta fase. No hay merge, autorización de despliegue ni certificación de apertura. OPEN-01 y P1 críticos retienen el gate. El rollback completo se ensaya primero en una copia independiente con conectores externos desactivados.

## Identidades verificadas

- Contenedor principal: `1f23bff0-bc4c-4c00-bbcf-2fe81ff666dd`.
- Volumen: `/var/lib/pterodactyl/volumes/1f23bff0-bc4c-4c00-bbcf-2fe81ff666dd`.
- Mod candidato: `mods/lunaeternal-0.1.0.jar`; hash del build en `prelaunch/final-build.json`.
- Mod principal anterior: SHA256 `6a4642198448b0395bdae0739c4ad3d79bc2b86bc11a19d8bc120af5203838f9`, copia local preservada en `C:/Users/JUAN/.codex/prelaunch-backups/2026-10-02/server-current.jar`.
- Base principal: `s11945_luna`, versión observada 56; comprobar nuevamente antes de cualquier intervención.
- Configuración afectada: `config/lunaeternal/world-edit.properties` e `interactions.properties`; preservar config general y catálogo Tebex. Conservar presencia/ausencia original de cada archivo.

## Preparar el punto de reversión

Tras cerrar blockers y obtener ruta autorizada: registrar commit limpio, JAR/hash, versiones de mods/datapacks y migraciones; anunciar mantenimiento, bloquear nuevos accesos y esperar que terminen combates y compras. Detener correctamente desde consola del panel con `stop`, o API Wings desde host; no usar docker stop en el principal, porque Wings lo reinicia. Confirmar contenedor detenido y guardado completo; no usar kill como procedimiento normal.

Con el servidor detenido, generar backup NUEVO de SQL y volumen usando `/usr/local/sbin/pokereport-db-backup` y `/usr/local/sbin/pokereport-files-backup`, después de revisar que funcionen con el contenedor parado. Si el script exige RCON activo, adaptar el procedimiento operativo de backup detenido sin saltarse la copia: dump transaccional consistente + archivo íntegro de volumen. No reabrir el servidor para simular coordinación temporal.

Registrar hora UTC de inicio/fin, paths, SHA256, tamaños y contenido: mundos, DB, configs, permisos/ops/whitelist, mods, lunaeternal, datapacks ZIP y scripts. Verificar checksum, listar el tar y restaurar en destino independiente. El SQL 04:00 y archivos 15:20 del ensayo anterior demuestran recuperabilidad, pero no son un punto coordinado apto para revertir dinero/inventario simultáneamente.

Guardar además una copia pequeña de cada JAR/config que se va a reemplazar en directorio de mantenimiento fuera del volumen; manifest con hashes y ausencias originales. Reponer únicamente archivos previstos, nunca sincronizar un worktree completo sobre el servidor. Arrancar solo después de revisar el manifest y compatibilidad.

## Revertir solo mod/config

Si hay regresión de arranque/protección/interacción y NO hubo migración incompatible, corrupción ni modificación de datos que requiera reversión: cerrar acceso, detener como arriba, preservar logs y estado fallido, reponer el JAR anterior comprobando su hash y las configuraciones exactas del manifest. Si un archivo nuevo no existía, moverlo fuera de `config` a mantenimiento para conservarlo y recuperar la ausencia original. No restaurar mundos ni DB automáticamente por un fallo de interfaz.

Antes de arrancar comprobar compatibilidad del JAR anterior con schema actual, mods, datapacks y datos serializados. La versión 56 observada no prueba que un build futuro sea compatible. Si hubo migración, evaluar su contrato de rollback y ensayo aislado; no bajar solo el JAR a ciegas. El mod anterior carece de las protecciones del candidato: mantener mantenimiento y no abrir al público tras revertirlo.

## Revertir datos y mundo

Cuando hay corrupción/pérdida/dupe que no pueda repararse de forma selectiva comprobable, restaurar el punto coordinado completo compatible, con servidor y procesadores externos detenidos. Preservar primero la copia del incidente, DB y ledger para reconciliación. Extraer el archivo a directorio NUEVO, verificar paths e inventario y restaurar el SQL en schema NUEVO con usuario exclusivo; probar arranque allí con Tebex aislado.

Reponer juntos mundos/playerdata/party/inventario, DB/economía/misiones/recompensas, permisos/config y mods correspondientes al mismo snapshot. Confirmar EasyAuth SQLite íntegro. No mezclar wallets antiguas con inventarios nuevos ni restaurar DB sin filas de idempotencia Tebex del mismo punto. Revisar pedidos reales posteriores al snapshot y reconciliar entrega/chargebacks antes de reactivar el conector; no consumir ni reconocer automáticamente la cola desde una copia de recuperación.

Una vez validada la copia, cambiar al volumen/schema recuperado mediante el mecanismo del panel administrado, manteniendo registro de qué directorio/schema estaba activo. Conservar intacto el destino fallido hasta concluir reconciliación. No publicar credenciales ni copiar configuración de staging al principal.

## Comprobar rollback y smoke

Con acceso restringido: hash/versión del JAR, arranque y DB sin errores, schema compatible, inventario/party/saldos de fixtures conocidos, login autenticado, Ciudadela/protección/interacción/hambre/daño/PokePad, batalla completa, Hogar/Salvaje, economía/ledger, teleports y permisos. Revisar logs y duplicados. Monitorizar TPS/MSPT/heap/GC y confirmar que no hay dos conectores Tebex activos.

PASS exige evidencia por caso y reconciliación de datos; un `Done` del servidor no certifica rollback completo. La restauración independiente del backup corregido pasó arranque y recuperación de wallet; reversión de producción y smoke posteriores permanecen NOT TESTED/BLOCKED.

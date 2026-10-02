# Auditoría forense prelaunch — 2026-10-02



## 1. Estado general



**Certificación: CONDICIONAL / NO GO-LIVE.** Candidato implementado y verificado en staging; no se desplegó el mod al servidor principal. Quedan pruebas con clientes, autorización humana de staff y alcance de terceros. No se certifica invulnerabilidad universal ni carga multiplayer.



Rama `codex/prelaunch-world-hardening`, base `ef9f8832` (portal Oct2), contiene economía y Lunita anteriores. Se inspeccionaron principal b949a2b0 con cambios pendientes, 65d3 ef9f8832, remediación 65a79f03 y actual inicialmente 13bca46a. No se reemplazó ningún otro worktree. Se respaldó historia Git y pendientes antes de cambios. Se conservaron selectivamente los cambios pendientes recientes de EconomyReport/GTS/Market/Red/MixinBlockItem/DownloadingTerrain; se mantuvo PlayerService más nuevo con logging. No se propagó la eliminación de ptero.py ni un ConnectScreen anterior.



Comparación inicial con JAR live: 646 clases idénticas, ninguna clase live ausente; diferencias recientes de portal/client/PlayerService/AutoTest conservadas. El delta final verifica SHA base y TODOS los archivos del build: ninguna entrada live eliminada. Los JSON de dimensiones existentes conservaron semántica. Evidencia: prelaunch/final-build-manifest.json.



## 2. Problemas P0 encontrados



Protección incompleta de mapas: excepciones OP2 Creative en gimnasios/memorial, ausencia de política común de daño/hambre/acciones indirectas y controles consistentes de edición. La gravedad P0 se refiere a abrir públicamente mapas sin desplegar/verificar estas barreras; no equivale a explotación observada.



## 3. Problemas P0 solucionados



Implementación candidata central y mixins/eventos validados: jugadores normales no editan en los ensayos; OP2 no adquiere edición por Creative/nodos; OP4 necesita permisos explícitos y sesión. **Cerrado en candidato, abierto operativamente en producción** hasta despliegue y QA de clientes. No sumar ambas como dos hallazgos distintos.



## 4. Problemas P1



Corregidos en código candidato: acreditación de MARK retirada; comprobación de superposición de clones basada en profundidad real; control explícito de comandos críticos y WorldEdit. Corregido en host: backup omitía ZIP/launcher/listas/scripts.



Registro de asuntos ABIERTOS (una fila = un asunto; pendientes de evidencia no son vulnerabilidades demostradas):



| ID | Nivel | Estado / cierre exigido |

|---|---|---|

| OPEN-01 | P0 | Mod de protección no desplegado ni certificado en principal; cerrar con rollout respaldado + smoke real |

| OPEN-02 | P1 | QA de viajes/reconexión/UI/Survival pendiente; revisar regresiones de contenedores |

| OPEN-03 | P1 | Batallas, recompensas y concurrencia con dos clientes pendientes |

| OPEN-04 | P1 | Cobertura de comandos/payloads de terceros, identidad EasyAuth y UUID de staff sin certificación completa |

| OPEN-05 | P1 | Intento de conexión Tebex desde primera copia QA: revisar cola/entregas en panel y ejecutar sandbox aislado |

| OPEN-06 | P1 | Carga multiplayer no ejecutada; definir capacidad y verificar Spark antes de apertura |

| OPEN-07 | P2 | Logs de items inválidos/Rotom duplicados: localizar y verificar contenido sin borrar assets |

| OPEN-08 | P2 | Auditoría dedicada registra rotura; cobertura de colocación, comandos masivos/WorldEdit y rollback aún incompleta |

| OPEN-09 | P3 | APIs/Gradle deprecados y warnings unchecked; build funciona |



## 5. Protecciones Ciudadela



Lista central incluye Ciudadela y Torre Comercial dentro de ella. Bloqueo de rotura/colocación, items alteradores, entidades decorativas, fuentes ambientales, daño y hambre. Cofre sensible cerrado; puerta permitida en ensayo. Ver WORLD_PROTECTION.md para alcance y configuración pública por coordenada.



## 6. Protecciones Gimnasios



Gimnasios, Morti, Torre e Islas seguros. GymArenaProteccion y SantuarioProteccion comparten autorización; no se reemplazó battle engine ni las dimensiones. Botones/palancas permitidos con puertas existentes. Arenas: profundidad > paso detecta clones solapados; z negativo de plantillas Johto no demuestra solapamiento. Coordenadas/ranuras reales sustituyeron supuestos universales de autotest.



## 7. Roles administrativos



OP nativo comprobado; LuckPerms ausente. Tablist ADMIN y rangos comerciales no son autoridad. OP4 + nodos UUID exactos + Creative + sesión; permisos inválidos deniegan todo. Config vacía por defecto. No se eligieron personas ni se otorgaron nodos automáticamente. Matriz en ADMIN_ROLES.md.



## 8. Seguridad de comandos



Gates en paquetes firmados/no firmados y CommandManager para fuentes jugador. Namespaces normalizados; execute/function/schedule/clone y constructores Luna exigen sesión incluso desde Survival para impedir destinos seguros indirectos; prueba de regresión añadida. Consola conserva autoridad original. Inventario real: 416 raíces / 2247 usos, con Tebex inicial; 2236 tras aislamiento. COMMANDS.md contiene todas las rutas. Inventariado no significa ejecutado ni permiso de terceros certificado. Axiom namespace protegido; otros payloads requieren revisión específica.



## 9. Economía



Se conservaron las fases recientes de economía. EconomyService usa FOR UPDATE, suma exacta, ledger único por clave y rollback; GTS y mercado conservan transacciones/bloqueo y salvaguardas pendientes copiadas. MARK ya retirada por V049: applyInTransaction ahora rechaza antes de crear wallet, sin borrar historial ni saldos. Autotest actualizado demuestra rechazo y mantiene ReportCoin. Lectura principal: 0 wallets negativos, 0 huérfanos, 8 POKEDOLLAR y 7 REPORTCOIN; no MARK activo. No se modificó economía principal.



TebexService revisado: bloqueo por fulfillment, operación transaccional con ledger/saldo, estados/reintentos. Pruebas backend en suite PASS; compra real/replay externo no ejecutados. Principal tenía 18 DELIVERED, 2 CHARGEBACK y 3 REQUIRES_REVIEW en lectura agregada; no atribuir esas filas al incidente QA sin investigar su origen. Lectura posterior: 0 fulfillments recibidos/procesados el día de la auditoría en el backend propio; esto no descarta comandos externos del conector, y el panel sigue pendiente. No publicar IDs de compra ni credenciales.



## 10. Batallas



Cobblemon 1.8.0 y RCT API 0.16 beta/mod 0.19 beta cargan. Logs Cynthia: dragon_claw/swords_dance/stone_edge inválidos; se corrigieron a IDs encontrados en moves.js del Cobblemon instalado. El arranque final no registra movimientos inválidos de Cynthia. Eso valida datos, no victoria/recompensa completa. Guion reproducible con dos clientes en QA_CHECKLIST.md. No cancelar daño Pokémon mediante hooks de jugador vanilla.



## 11. Base de datos



MariaDB accesible por socket del nodo; principal s11945_luna, versión máxima schema 56. Inventario de 69 mods y propiedades públicas, 7 OP de nivel4, online-mode=false (EasyAuth requiere QA de identidad) en prelaunch/server-inventory-and-tests.json. Inventario real de tablas incluye ledger, market, GTS, gym, Tebex, Lunita, return y legacy_mark. Se revisaron estructura e índices de wallet/ledger/fulfillment; idempotency_key UNIQUE presente. Restauración y autotest usan schema prelaunch_20261002 con cuenta restringida. No migraciones nuevas ni resets en principal. Lectura agregada no sustituye reconciliación completa de cada activo ni simulación de caída en producción.



## 12. Rendimiento



Arranque y QA sin usuarios reales, recursos staging 2CPU/5GB (heap4GB). No perfiles representativos multiplayer; no prometer TPS de apertura. El informe anterior de 0 jugadores es parcial y no se elevó a certificación. Hooks tienen decisiones por evento; muestreo void cada20ticks, sin consulta DB por daño/hambre. WorldEditAudit síncrono y plugins de terceros requieren perfil bajo carga.



## 13. Backups



Backup principal reparado y generado; checksum e integridad EasyAuth PASS. Inventario corregido comprobó 3 datapacks ZIP, ops, launcher y script operativo. Restauración antigua arrancó con suplementos documentados. También se extrajo íntegro el nuevo backup en directorio NUEVO, sin suplementos live; ver BACKUP_AND_RECOVERY.md y recibo de recuperación. SQL 04:00 y archivos 15:20 no son snapshot coordinado: límite explícito para consistencia entre sistemas. No afirmar restauración puntual SQL/mundo atómica.



## 14. Build



JDK21.0.12 y build.sh, Gradle/Loom1.17.13; build limpio PASS, 187 JUnit / 0 FAIL. Se corrigió conflicto de remapeo separando goals Enderman. Dependencia battle-extras preservada desde copia principal. Deprecaciones/unchecked no impidieron build. Font E005 mapea a logo_tab.png existente en ambos packs; no rediseño de UI.



## 15. Servidor



Principal: container/volume 1f23bff0-bc4c-4c00-bbcf-2fe81ff666dd, JAR live SHA256 6a4642198448b0395bdae0739c4ad3d79bc2b86bc11a19d8bc120af5203838f9 sin cambios. Staging /srv/pokereport-prelaunch-20261002, localhost25590/RCON25591, DB aislada, auth secret independiente. No mod deploy ni restart principal.



Incidente: primera restauración conservó credenciales del mod Tebex y contactó tienda real. Se detuvo, desactivó conector solo en QA, se corrigió setup para aislar antes del arranque. No ocultar este hecho ni certificar ausencia de efectos externos sin panel. Staging conserva datos para reproducción y debe quedar detenido al cerrar auditoría.



## 16. Archivos modificados



Manifest al final; nuevos componentes WorldProtectionManager, WorldEditPermissions, SafeInteractionPolicy, WorldEditAudit, CommandSafetyPolicy, ProtectionCommands, ProtectionSelfTest y mixins de seguridad; integración mínima en LunaEternal/guardas previas. Cambios económicos recientes preservados, correcciones de pruebas explicadas. No cambios intencionales a launcher/login/sidebar de terceros ni secretos añadidos.



## 17. Comandos nuevos



/pradmin editmode on|off, status, commands, selftest. selftest solo consola y schema exacto QA; no ejecución principal. Autotest también solo se ejecutó en copia. Config de permisos requiere reinicio. Inventario no tiene argumentos/IDs de compras privados.



## 18. Documentación creada



PRELAUNCH_AUDIT.md, COMMANDS.md, ADMIN_ROLES.md, WORLD_PROTECTION.md, QA_CHECKLIST.md, BACKUP_AND_RECOVERY.md. Evidencia sanitizada en docs/prelaunch; scripts operativos de staging/recuperación en tools. Backups y claves permanecen fuera del repositorio.



## 19. Pruebas PASS



187 unitarias; servidor arrancado con mods y DB reales; 753 autotest; 21 ensayos de protección; delta entrada por entrada sin eliminaciones; backup checksum/SQLite e inventario; restauración en destino separado. Los recibos finales identifican qué build recibió cada prueba. No sumar tests a usuarios ni afirmar 960 flujos independientes: hay invariantes relacionados.



## 20. Pruebas FAIL



Los primeros autotests fallaron por reglas viejas de MARK, fixtures orgánicos, recompensa hunt y geometría; se corrigieron criterios para representar contratos actuales, sin desactivar suite. La falta de glifo era real y se reparó. Ensayo inicial setblock falló por ejecución anidada diferida; ahora operación autorizada síncrona evita race de cleanup. Errores iniciales de copia parcial del JAR/RCON antes de readiness fueron del procedimiento, no fallos del candidato final. La excepción deliberada 'a proposito' verifica resiliencia del programador; duplicados de fixtures ejercitan idempotencia, no crash espontáneo.



FAIL final conocido: ninguno en suites ejecutadas. Advertencias de datos/mapas y pendientes de clientes permanecen abiertos; no se esconden bajo ese resultado.



## 21. Riesgos pendientes



Ver registro OPEN-01..09 y QA_CHECKLIST.md. Scanner limitado a texto rastreado no encontró credencial literal confirmada; auth.js access_token es referencia a variable, no secreto embebido. Este resultado no cubre binarios/historia completa ni configura rotación. Los contenedores públicos deben verificarse/configurarse individualmente. Permisos de root/RCON siguen fuera de la barrera de jugador. No existe auditoría completa de acciones masivas ni transición automática de modos. No afirmar que el principal ya recibió estos cambios.



## 22. GO-LIVE BLOCKERS



Cerrar OPEN-01..06, verificar identidad de staff y cliente, resolver impactos de Tebex, carga y batallas. Después desplegar solo candidato exacto con backup coordinado/rollback y smoke del principal. El despliegue queda retenido porque la nueva política de interacción aún no tiene validación con clientes; aplicarla públicamente ahora podría bloquear flujos necesarios. No solicitar una aprobación genérica que sustituya esas pruebas.



**P0 abiertos: 1 · P1 abiertos: 5 · P2 abiertos: 2 · P3 abiertos: 1.** Conteo operacional de asuntos enumerados, no afirmación de nuevas explotaciones confirmadas.



## Manifest de cambios de esta auditoría

- `config/world-edit.properties.example`
- `docs/ADMIN_ROLES.md`
- `docs/BACKUP_AND_RECOVERY.md`
- `docs/COMMANDS.md`
- `docs/PRELAUNCH_AUDIT.md`
- `docs/QA_CHECKLIST.md`
- `docs/WORLD_PROTECTION.md`
- `docs/prelaunch/README.md`
- `docs/prelaunch/build-and-test-receipt.json`
- `docs/prelaunch/final-build-manifest.json`
- `docs/prelaunch/prelaunch-changed-entries.json`
- `docs/prelaunch/prelaunch-jar-comparison.json`
- `docs/prelaunch/recovery-receipt.json`
- `docs/prelaunch/runtime-commands-isolated.txt`
- `docs/prelaunch/runtime-commands-with-tebex.txt`
- `docs/prelaunch/server-inventory-and-tests.json`
- `mod/src/client/java/net/pokereport/luna/client/mixin/DownloadingTerrainScreenMixin.java`
- `mod/src/client/resources/assets/lunaeternal/font/iconos.json`
- `mod/src/client/resources/resourcepacks/recursos/assets/lunaeternal/font/iconos.json`
- `mod/src/main/java/net/pokereport/luna/LunaEternal.java`
- `mod/src/main/java/net/pokereport/luna/command/EconomyReport.java`
- `mod/src/main/java/net/pokereport/luna/command/ProtectionCommands.java`
- `mod/src/main/java/net/pokereport/luna/economy/EconomyService.java`
- `mod/src/main/java/net/pokereport/luna/gts/GtsService.java`
- `mod/src/main/java/net/pokereport/luna/gym/Arenas.java`
- `mod/src/main/java/net/pokereport/luna/gym/GymArenaProteccion.java`
- `mod/src/main/java/net/pokereport/luna/market/MarketService.java`
- `mod/src/main/java/net/pokereport/luna/mixin/CommandSafetyMixin.java`
- `mod/src/main/java/net/pokereport/luna/mixin/LobbyNetworkGuardMixin.java`
- `mod/src/main/java/net/pokereport/luna/mixin/MixinBlockItem.java`
- `mod/src/main/java/net/pokereport/luna/mixin/SafeDecorationMixin.java`
- `mod/src/main/java/net/pokereport/luna/mixin/SafeEndermanMixin.java`
- `mod/src/main/java/net/pokereport/luna/mixin/SafeEndermanPlaceMixin.java`
- `mod/src/main/java/net/pokereport/luna/mixin/SafeExplosionMixin.java`
- `mod/src/main/java/net/pokereport/luna/mixin/SafeFarmlandMixin.java`
- `mod/src/main/java/net/pokereport/luna/mixin/SafeFireMixin.java`
- `mod/src/main/java/net/pokereport/luna/mixin/SafeFluidMixin.java`
- `mod/src/main/java/net/pokereport/luna/mixin/SafeHungerMixin.java`
- `mod/src/main/java/net/pokereport/luna/mixin/SafeLavaMixin.java`
- `mod/src/main/java/net/pokereport/luna/mixin/SafePlayerMixin.java`
- `mod/src/main/java/net/pokereport/luna/mixin/SafeWorldBreakMixin.java`
- `mod/src/main/java/net/pokereport/luna/net/Red.java`
- `mod/src/main/java/net/pokereport/luna/santuario/SantuarioProteccion.java`
- `mod/src/main/java/net/pokereport/luna/test/AutoTest.java`
- `mod/src/main/java/net/pokereport/luna/test/ProtectionSelfTest.java`
- `mod/src/main/java/net/pokereport/luna/world/CommandSafetyPolicy.java`
- `mod/src/main/java/net/pokereport/luna/world/SafeInteractionPolicy.java`
- `mod/src/main/java/net/pokereport/luna/world/TravelService.java`
- `mod/src/main/java/net/pokereport/luna/world/WorldEditAudit.java`
- `mod/src/main/java/net/pokereport/luna/world/WorldEditPermissions.java`
- `mod/src/main/java/net/pokereport/luna/world/WorldProtectionManager.java`
- `mod/src/main/resources/data/rctmod/trainers/luna_cynthia.json`
- `mod/src/main/resources/lunaeternal.client.mixins.json`
- `mod/src/main/resources/lunaeternal.mixins.json`
- `mod/src/test/java/net/pokereport/luna/world/CommandSafetyPolicyTest.java`
- `tools/prelaunch_backup_hardening.py`
- `tools/prelaunch_patch_staging.py`
- `tools/prelaunch_recovery_run.py`
- `tools/prelaunch_recovery_setup.py`
- `tools/prelaunch_recovery_verify.py`
- `tools/prelaunch_staging_run.py`
- `tools/prelaunch_staging_setup.py`

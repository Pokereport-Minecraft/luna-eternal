# Vendedores agrícolas y recuperación del laboratorio de Oak

## Purpose

Completar los dos vendedores solicitados y recuperar los decorativos originales desaparecidos.

## Dependencies

ComercioNpc, Red.AbrirComercio, PokePad/TiendaScreen, TrainerMob, Decorativos y FaunaControl.

## Related Documents

[Despliegue](prelaunch/agriculture-oak-deployment.json), [backup](prelaunch/agriculture-oak-backup.json), [puertas y staff](DOORS_AND_STAFF_ROLES.md).

## Current Status

Desplegado en el principal el 2 de octubre de 2026 (Bogotá), código ddb1fc3c. Build: 186 PASS, 2 SKIPPED; staging: 759 autotests PASS. Arranque principal y consola verificados, 20 TPS en reposo. No cambian catálogo, precios, economía ni protocolo del cliente; no requiere otra descarga.

| NPC | Posición en lunaeternal:ciudadela | Orientación | Destino |
|---|---|---|---|
| Bayas | 104.710 98 -68.682 | Norte / yaw 180 | tienda:bayas |
| Cultivos | 105.650 105 -78.648 | Sur / yaw 0 | tienda:cultivos |

Se usa el mismo sistema y aspecto de los vendedores existentes. `/luna comercio agricultura` coloca únicamente estos dos; se comprobó repetirlo en staging sin duplicados. La reposición elimina solamente entidades con la etiqueta exacta del puesto en su zona. Los cinco puestos anteriores no se recrearon en producción. Ambos nuevos vendedores quedaron únicos y guardados con posición/orientación exactas, NoAI, invulnerabilidad y Persistent de RCT.

Los archivos originales de fauna muestran que los iniciales fueron archivados el 24 de septiembre (hora Bogotá). El control anterior trataba Pokémon sin dueño de Ciudadela como fauna; el control vigente excluye etiquetas luna_* y el marcador decorativo interno. No se cambió la política actual de fauna.

Se recuperaron los cinco NBT originales, con UUID, especie, posición, giro, escala y postura originales: Bulbasaur, Charmander, Squirtle, Omastar y Kabutops. No había Mewtwo en esos archivos del laboratorio. La herramienta [restore_oak_decorations.py](../tools/restore_oak_decorations.py) restringe la recuperación a esos cinco UUID y conserva copia del NBT antes de ejecutarla. Solo invoca summon si el UUID original no está presente en los chunks cargados. Añade el marcador Pokemon.PersistentData.luna_decorativo y Unbattleable para aplicar las protecciones actuales, incluso en los dos fósiles cuyo archivo original no incluía la segunda marca.

La recuperación quedó respaldada en /var/backups/pokereport/oak-recovery-20261003T0206Z. Backup coordinado de SQL y volumen en /var/backups/pokereport/releases/20261003T020652Z, con gzip/zstd y EasyAuth SQLite verificados. Tras el reinicio, los cinco UUID se cargaron y respondieron a consultas de posición. La lectura de los archivos de entidades confirma una sola copia de cada uno, persistencia, NoAI, invulnerabilidad, Unbattleable y marcador de Pokédex. Se retiraron los dos chunks forzados temporales usados para comprobarlos; la lista Forced quedó vacía como antes.

## Last Decision

Recuperar los originales del respaldo, preservando la construcción y la configuración vigente. No inventar nuevas especies o ubicaciones ni reemplazar NPC existentes.

## Next Actions

Confirmación humana de renderizado de los decorativos y clic de ambos vendedores pendiente (NOT TESTED). El servidor conserva evidencia de guardado, recarga y destino configurado, pero esto no sustituye la prueba visual del cliente.

Rollback del código: detener desde Wings, conservar estado actual y restaurar before-agriculture.jar del backup coordinado. Para revertir únicamente la recuperación, retirar exclusivamente los cinco UUID documentados; conservar sus NBT respaldados. No restaurar mundos o economía completos por un ajuste de NPC.

# PC remoto: rango comercial obligatorio

La tecla P (solicitud lumymon:open_pc) y /pc comparten RemotePcAccess. Se elimina la excepción anterior de OP/staff. Solo CAMPEON, MAESTRO y LEYENDA permiten abrir; ENTRENADOR, ELITE, null y todos los rangos administrativos por sí solos lo deniegan. RankService.enCache consulta el rango comercial guardado, no la insignia administrativa de Tablist. Un administrador que tenga además Campeón o superior puede usarlo por ese rango comercial.

Se conserva la comprobación de autenticación/lobby, combate, configuración de LumyMon y enlace de PC. No cambia el acceso físico a PCs ni los permisos de construcción/staff.

JUnit valida todos los rangos con y sin OP, incluido null. Build: 240 PASS / 2 SKIPPED. Matriz del autotest actualizada. QA con Freddysillo y una cuenta Campeón pendiente.

SHA256: 7eebcd18964bc8b29b27aaaa2efc2144036413660bfe6eaf39e38accd6ef91b6.

Staging final: 803 autotests PASS / 0 FAIL. Primera ejecución usaba comprobaciones compiladas con la política antigua (6 fallos esperados); recompilación final y verificación completa correctas.

Producción: desplegado y arranque/TCP/SHA256 PASS. Backup previo del JAR, SQL y configuración en /var/backups/pokereport/preopen-fixes/20261004T025950Z. Configuración y esquema 57 conservados, sin reset. Staging detenido. Sin necesidad de actualizar cliente.

# PC remoto: rango comercial obligatorio

La tecla P (solicitud lumymon:open_pc) y /pc comparten RemotePcAccess. Se elimina la excepción anterior de OP/staff. Solo CAMPEON, MAESTRO y LEYENDA permiten abrir; ENTRENADOR, ELITE, null y todos los rangos administrativos por sí solos lo deniegan. RankService.enCache consulta el rango comercial guardado, no la insignia administrativa de Tablist. Un administrador que tenga además Campeón o superior puede usarlo por ese rango comercial.

Se conserva la comprobación de autenticación/lobby, combate, configuración de LumyMon y enlace de PC. No cambia el acceso físico a PCs ni los permisos de construcción/staff.

JUnit valida todos los rangos con y sin OP, incluido null. Build: 240 PASS / 2 SKIPPED. Matriz del autotest actualizada. QA con Freddysillo y una cuenta Campeón pendiente.

SHA256: 8a0517b6a02bb58ceb1d73766e616ee9f631696c8dfca2c454446b2b556717a7.

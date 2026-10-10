# Revisión del combate Blaine / Leron

Fecha local: 9 de octubre de 2026. Horas siguientes en UTC del 10 de octubre.

## Evidencia de producción

- 01:56:55: LeronLolazoo inicia Blaine, nivel 42, seis Pokémon rivales, pericia 4. Equipo: arcanine/magmar/ninetales/rapidash/flareon/growlithe.
- ID de batalla: `daefeadc-de7d-41d1-a3e8-53408414d935`.
- 01:57:06 y 01:57:16: watchdog detecta DispatchResult bloqueado durante 200 ticks y fuerza GO.
- 01:57:17: aparece Magmar como rival activo, tras Arcanine.
- Desde 01:57:19: se repite `[Invalid choice] Can't switch: You can't switch to an active Pokémon`, actor `00e361ea-7b82-445c-8b6f-2ebd796d6953`.
- 01:57:27: Leron informa en chat que el rival «ataca doble», mientras continúa el error anterior.
- 01:58:51–01:58:56: se liberan/limpian ranuras de Blaine antes del cierre registrado de esa batalla; se necesita revisar quién disparó esa limpieza y sus condiciones. No atribuirlo automáticamente al error de turnos.
- 01:58:58: termina la batalla y Leron muere ahogado; BattlePositions encuentra al entrenador nulo o muerto.

## Qué está y qué no está demostrado

`Gimnasio.esCombateDoble` habilita dobles únicamente para Luana y Giovanni. `Adaptador.formatoDe` utiliza GEN_9_SINGLES para Blaine. La dificultad no añade acciones por turno.

Está confirmado un fallo repetitivo de selección/cambio y una cola bloqueada en este combate. El log disponible no contiene el protocolo completo de Showdown (`|turn|`, `|move|`); por tanto no demuestra dos movimientos del mismo Pokémon dentro del mismo turno ni identifica al actor rechazado como IA o jugador. La correlación temporal no basta para afirmar causalidad.

Puntos a reproducir con protocolo de turnos en staging: `BattleChoiceRecoveryMixin` restaura una solicitud enviada y vuelve a abrirla al rechazo; `TrainerBattleAI.forcedChoice` depende de `isSentOut` y `willBeSwitchedIn`; `PokemonBattleSafetyMixin` fuerza GO y puede descartar un dispatch tras 200 ticks. Revisar sincronización de solicitud, Pokémon activos y cola antes de cambiar esos mecanismos. No se modifica dificultad ni se despliega un parche sin reproducción.

## Solicitud de reparación sin reinicio

Se mantiene el proceso de producción en ejecución. Segunda batalla de Leron: `ebdfca11-3a85-48cc-8d18-9d173d3c2bc8`, inicio 02:00:39 UTC; watchdog bloqueado 02:00:51. A 02:01:11 el jugador informa que el daño mostrado no coincide con el debilitamiento, y a 02:01:28–34 informa un ataque tras sacar otro Pokémon. Tampoco hay protocolo completo de movimientos en este log.

Se inspeccionó el JAR exacto instalado de Fight or Flight 0.11.0: `do_pokemon_attack_in_battle=false`, pero su mecánica alternativa de vida está habilitada. El mixin de daño aplica conversión de vida de entidad a Pokémon; falta demostrar que interviene en estos combates. No se deshabilita el mod ni se cambia su configuración por sospecha. Su clase principal conserva referencias estáticas a la configuración cargada en inicialización; no se encontró un comando de recarga propio que permita aplicar una corrección verificada en caliente.

El bytecode de Cobblemon confirma `/stopbattle <jugador>`: obtiene únicamente la batalla de ese participante y llama `PokemonBattle.stop()`. Sirve para cerrar una pelea afectada, no para corregir el mecanismo que volverá a ejecutarse en la siguiente. No se ejecutó sobre una pelea sana ni se limpiaron ranuras. `/reload` y sustituir el JAR no recargan los mixins de batalla ya cargados. No hay una vía soportada identificada para instalar esa corrección de código sin reiniciar; no se intenta instrumentación experimental sobre el JVM de producción.

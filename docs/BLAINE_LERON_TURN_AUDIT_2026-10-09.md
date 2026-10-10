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

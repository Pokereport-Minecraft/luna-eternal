# Aviso de inicial de Cobblemon

## Purpose

Ocultar «¡Elige tu inicial! / Pulsa la tecla key.keyboard…» solicitado por el usuario, conservando la entrega de iniciales con Oak.

## Dependencies

Cobblemon Fabric 1.8.0, Minecraft 1.21.1 y mixins del cliente LunaEternal. Implementación `StarterToastMixin`, registrada solo en `lunaeternal.client.mixins.json`.

## Related Documents

[Despliegue del principal](PRODUCTION_DEPLOYMENT.md), [QA manual](MANUAL_CLIENT_QA.md), [recibo de publicación](prelaunch/starter-toast-release.json).

## Current Status

La causa es `PartyOverlay.render`: cuando el equipo está vacío y Cobblemon no tiene seleccionado su inicial, añade `starterToast` al gestor de avisos. Este aviso pertenece al selector genérico que está deshabilitado en este servidor; la entrega real usa Oak. Cambiar traducciones o marcar un inicial seleccionado falsamente no resuelve esa discrepancia.

La corrección marca únicamente el indicador visual `attachedToast` antes de cada render del panel. Así no se encola ese aviso, incluso si una sincronización/reconexión reinicia el indicador. El render del equipo continúa, los demás avisos se conservan y no se alteran flags de selección, cuentas, permisos ni Pokémon.

Build PASS: 186 pruebas ejecutadas / 2 omitidas / 0 fallos. Se comprobó contra el bytecode real de Cobblemon el campo booleano y el método de render. El JAR remapeado apunta a `method_1753` con los argumentos correctos. Comparación de todas las entradas con el candidato publicado anterior: solo cambian nueva clase de cliente, configuración de mixins de cliente y manifest del JAR. Ninguna clase de servidor cambia.

Publicación: solo se sustituye la entrada LunaEternal del manifiesto vigente, con assets nuevos por hash y puntero verificado. El servidor no necesita parada ni reinstalación por esta corrección exclusivamente visual.

## Last Decision

Suprimir el aviso genérico en el cliente oficial. No marcar artificialmente `starterSelected`, no cancelar el panel completo y no modificar el flujo de Oak.

## Next Actions

Cerrar Minecraft y volver a iniciar mediante el launcher actualizado. QA visual pendiente: cuenta sin inicial/equipo vacío, entrada y reconexión, ausencia del aviso; abrir Oak, elegir mediante su flujo y comprobar equipo y otros avisos. La compilación y la comprobación de bytecode no equivalen a una captura visual PASS.

Rollback: los assets anteriores están preservados; restaurar el manifiesto previo identificado en el recibo y el puntero previo guardado durante publicación. No restaurar datos del servidor por esta corrección de cliente.

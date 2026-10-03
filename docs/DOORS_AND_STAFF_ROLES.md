# Puertas y roles administrativos dentro de Minecraft

## Purpose

Permitir abrir las puertas de Ciudadela con bloques en la mano y asignar staff mediante comandos del juego, con etiqueta correspondiente.

## Dependencies

WorldProtectionManager, protección de gimnasio/Santuario, ops.json, WorldEditPermissions y Tablist. Las insignias gráficas nuevas requieren el cliente actualizado; sus etiquetas viajan mediante equipos del scoreboard.

## Related Documents

[STAFF_PERMISSION_COMMANDS.md](STAFF_PERMISSION_COMMANDS.md), [PRODUCTION_DEPLOYMENT.md](PRODUCTION_DEPLOYMENT.md), [WORLD_PROTECTION.md](WORLD_PROTECTION.md).

## Current Status

Causa de puertas confirmada por el usuario: con un bloque en mano se rechazaba UseBlockCallback antes de que la puerta recibiera el clic; con mano vacía abría. Un clic normal sobre DoorBlock, TrapdoorBlock o FenceGateBlock pasa a la interacción nativa, conservando la denegación de colocación directa. Shift conserva el bloqueo de uso del objeto; las puertas de hierro mantienen su funcionamiento nativo por mecanismos. Nichos administrativos y contenedores mantienen su política previa.

El comando usa exclusivamente el jugador conectado, resolviendo su UUID real mediante EntityArgumentType.player. Exige consola administrativa o OP4 más pokereport.admin.commands; OP solo, moderador y builder no pueden asignar roles. El jugador objetivo debe haber salido del lobby. Se guardan copias de ops, grants y etiquetas; se reemplazan los grants del objetivo preservando otros UUID, se persiste y se aplica en memoria. Se desactiva la edición, se actualiza el árbol de comandos y el scoreboard. Un fallo intenta revertir archivos, OP y caches. No escribe rangos comerciales en DB.

## Last Decision

Permisos y etiquetas se asignan juntos. Las insignias ADMIN, DEV y MOD existentes se reutilizan. OWNER, HEAD ADMIN y BUILDER tienen imágenes originales basadas en la guía existente: sprites de 28 px de alto, marco metálico biselado, panel de color, letras blancas con contorno oscuro y emblema lateral. Se registran como glifos E018/E019/E01A, renderizados a altura 10 y ascent 8 en las mismas fuentes. El generador reproducible es tools/build_staff_badges.py. Los assets anteriores se conservan intactos.

| Rol | OP | Permisos | Etiqueta |
|---|---|---|---|
| owner | 4 | Administración + edición | OWNER |
| head-admin | 4 | Administración + edición | HEAD ADMIN |
| admin | 4 | Administración + edición | ADMIN |
| developer-edit | 4 | Administración + edición | DEV |
| developer | 4 | Administración, sin edición | DEV |
| builder | 4 | Edición, sin gestión de roles | BUILDER |
| moderator | 3 | Moderación, sin edición ni gestión de roles | MOD |
| player | Sin OP | Retira grants y etiqueta de staff | Rango comercial conservado |

## Uso

Con el jugador conectado y fuera del lobby:

```text
/pradmin rol lista
/pradmin rol asignar NombreJugador moderator
/pradmin rol asignar NombreJugador builder
/pradmin rol asignar NombreJugador developer
/pradmin rol asignar NombreJugador developer-edit
/pradmin rol asignar NombreJugador admin
/pradmin rol asignar NombreJugador head-admin
/pradmin rol asignar NombreJugador owner
/pradmin rol asignar NombreJugador player
```

Elegir un rol por persona. Asignar admin/owner/developer concede capacidad de gestionar otros roles. Los permisos y la etiqueta se aplican al momento y persisten al reiniciar. Para construir con un rol de edición:

```text
/pradmin editmode on
/pradmin editmode off
```

El modo edición se debe activar de nuevo tras reconectar. Bootstrap del primer owner desde consola o herramienta offline con UUID confirmado; un jugador sin grants no puede elevarse.

## Next Actions

Validación y despliegue en curso; recibo de cierre se añadirá tras los ensayos y el arranque principal. Verificación visual del tag y prueba humana de puerta posteriores al despliegue pendientes hasta confirmación del usuario. Los ensayos sintéticos no sustituyen ese registro.

Rollback: servidor detenido desde Wings/panel, preservar estado actual y restaurar juntos JAR, ops.json, world-edit.properties y staff-roles.properties del mismo punto anterior; conservar presencia/ausencia original. No restaurar economía/mundos por este cambio si no hay incidente de datos.

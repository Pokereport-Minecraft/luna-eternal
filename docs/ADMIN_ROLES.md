# Roles administrativos — 2026-10-02

Gestión preparada por solicitud del usuario: [STAFF_PERMISSION_COMMANDS.md](STAFF_PERMISSION_COMMANDS.md), herramienta `tools/staff_permissions.py` con presets, preview, respaldo y revocación. No se asignaron identidades reales. El usuario decidió omitir la confirmación de personas en esta fase; la tabla pendiente se conserva sin inventar staff.

Autoridad comprobada: OP nativo. No hay LuckPerms instalado en el servidor inspeccionado. Tablist llama ADMIN a todos los OP; `testrango` es visual. Los rangos comerciales de RankService no conceden edición. No usar ninguno de esos textos como autorización.

| Identidad | Mapas seguros | Comandos críticos | Survival |
|---|---|---|---|
| Jugador / rango comercial | No edita, incluso Creative | Permisos originales; barrera crítica adicional | Construcción normal |
| OP 1–3 | No edita, incluso con nodos | Barrera crítica deniega | Construcción normal |
| OP 4 sin nodos | No edita | Barrera crítica deniega | Construcción normal |
| OP 4 con nodos edit + editmode | Solo Creative + sesión explícita | Requiere nodo commands adicional | Construcción normal |
| Consola/RCON autenticada | Autoridad del host | Comandos originales, inventario y QA | Autoridad del host |

Archivo servidor: `config/lunaeternal/world-edit.properties`. Formato UUID=nodos separados por coma. Nodos exactos: `pokereport.world.edit`, `pokereport.admin.editmode`, `pokereport.admin.commands`. No comodines. Archivo ausente: se crea vacío; archivo inválido: deniega todos los permisos. Se carga al iniciar; después de modificarlo reiniciar el servidor. La plantilla del repositorio no concede acceso a ninguna persona.

El operador debe confirmar por identidad los UUID de los administradores superiores antes de conceder nodos. No se conceden permisos por apodo ni automáticamente a todos los OP. `/pradmin editmode on` verifica OP4 y los dos nodos, activa Creative y sesión; `off` revoca sesión y usa Adventure en zonas seguras o Survival fuera. Desconexión y parada revocan sesión. La sesión persiste entre dimensiones mientras el jugador siga conectado: no se afirma revocación automática al viajar.

Activaciones/desactivaciones registran nombre, UUID, dimensión y posición en log del servidor; roturas registran además estados anterior/posterior en log dedicado. Revisar permisos de acceso al archivo y al RCON; no publicar credenciales. El ensayo de staging comprobó OP2, OP4, Creative, Survival y edición autorizada. Los rangos humanos concretos y sus UUID siguen pendientes de confirmación operativa.

## STAFF AUTORIZADO PARA EDICIÓN DE MUNDOS

| Rol operativo | Persona confirmada | UUID confirmado | Edición aprobada |
|---|---|---|---|
| OWNER | PENDIENTE | PENDIENTE | PENDIENTE |
| HEAD ADMIN | PENDIENTE | PENDIENTE | PENDIENTE |
| ADMIN | PENDIENTE | PENDIENTE | PENDIENTE |
| BUILDER AUTORIZADO | PENDIENTE | PENDIENTE | PENDIENTE |

No añadir esta tabla ni sus marcadores al archivo operativo. Los títulos humanos no conceden permisos. La implementación actual exige OP4 además de los nodos; un builder OP1–3 no podrá editar. Si se decide autorizarlo, revisar expresamente el alcance de OP4 antes de asignarlo; no elevar todos los builders o moderadores.

Para agregar acceso: confirmar identidad y UUID mediante sesión autenticada/EasyAuth y datos del servidor, registrar quién aprobó el acceso, respaldar `config/lunaeternal/world-edit.properties`, detener el servidor durante mantenimiento y añadir una única línea con el UUID confirmado:

```properties
# Sustituir UUID_CONFIRMADO antes de guardar; este marcador no es un UUID válido.
UUID_CONFIRMADO=pokereport.world.edit,pokereport.admin.editmode
```

Conceder `pokereport.admin.commands` solo si también se aprobaron comandos críticos. Confirmar OP4 en `ops.json`; arrancar, revisar errores de permisos y verificar con esa cuenta: Survival deniega, Creative + EditMode OFF deniega, `/pradmin editmode on` permite únicamente la edición autorizada. Revisar logs de modo y rotura. Los grants se cargan al arrancar: no existe recarga en caliente.

Para retirar acceso: desconectar primero al usuario desde consola (`kick NOMBRE_CONFIRMADO Acceso de edición retirado`) para revocar su sesión; detener el servidor, respaldar el archivo, quitar los nodos o la línea del UUID y reiniciar. Retirar OP también si ya no corresponde (`deop NOMBRE_CONFIRMADO` desde consola). Comprobar que la cuenta no pueda activar EditMode ni modificar en Creative. Si está conectado mientras se revoca, no dejar la sesión antigua activa hasta el siguiente reinicio.

Consola y RCON siguen siendo autoridad del host y deben quedar restringidos a operadores. Auditoría dedicada de colocación y acciones masivas todavía incompleta: OPEN-08.

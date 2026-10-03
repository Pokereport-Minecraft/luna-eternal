> Actualización de despliegue autorizada por el usuario: principal RUNNING y launcher publicado. Ver [PRODUCTION_DEPLOYMENT.md](PRODUCTION_DEPLOYMENT.md) y recibos. El texto anterior que indica «sin despliegue» es histórico. Apertura sigue NO CERTIFICADA; QA pendiente no se convierte en PASS.

# Asignación de staff por comandos

Herramienta: `tools/staff_permissions.py`. Gestiona únicamente el sistema existente: `ops.json` y `config/lunaeternal/world-edit.properties`. No asigna personas ahora, no cambia rangos comerciales, no instala LuckPerms y no despliega el mod.

**El candidato de hardening ya está desplegado.** Confirmar identidad/UUID y verificar el rol con la persona; no se ha asignado staff automáticamente.

| Rol del comando | OP | Permisos explícitos | Edita mapas seguros |
|---|---|---|---|
| `moderator` | 3 | Ninguno de estos grants | No, incluso Creative |
| `builder` | 4 | world.edit + admin.editmode | Solo Creative + EditMode ON |
| `developer` | 4 | admin.commands | No |
| `developer-edit` | 4 | Los tres grants | Solo Creative + EditMode ON |
| `admin`, `head-admin`, `owner` | 4 | Los tres grants | Solo Creative + EditMode ON |

Los nombres de roles son presets de configuración, no un nuevo rango/tablist. Moderator conserva comandos de moderación originales de nivel 3, sujetos a la barrera crítica. Developer tiene comandos administrativos explícitos: no es un rol limitado a revisar código. Builder requiere OP4 por la arquitectura actual; esto no equivale a una certificación completa de todos los comandos/payloads de terceros (OPEN-04). No existe wildcard ni autorización automática por título.

## Preparación

Confirmar UUID de la sesión autenticada del servidor, especialmente en offline/Velocity/EasyAuth; no usar un UUID de Mojang por suposición. Copiar la herramienta al host, por ejemplo a `/root/pokereport-tools/staff_permissions.py` (crear antes ese directorio). Usar consola del host/SSH, no el chat de Minecraft.

Ejemplos para el principal desplegado:

```sh
SERVER=/var/lib/pterodactyl/volumes/1f23bff0-bc4c-4c00-bbcf-2fe81ff666dd
CONTAINER=1f23bff0-bc4c-4c00-bbcf-2fe81ff666dd
TOOL=/root/pokereport-tools/staff_permissions.py
```

Sustituir identidad real confirmada antes de cualquier asignación:

```sh
STAFF_UUID='UUID_CONFIRMADO'
STAFF_NAME='NOMBRE_CONFIRMADO'
```

Los marcadores se rechazan: no crean permisos. La herramienta exige UUID válido y nombre Minecraft válido. Preserva los demás UUID, reemplaza los grants anteriores de la persona al cambiar de rol y rechaza archivos inválidos antes de escribir.

## Consultar y previsualizar

```sh
python3 "$TOOL" --server-root "$SERVER" roles
python3 "$TOOL" --server-root "$SERVER" list
python3 "$TOOL" --server-root "$SERVER" assign --uuid "$STAFF_UUID" --name "$STAFF_NAME" --role moderator
python3 "$TOOL" --server-root "$SERVER" assign --uuid "$STAFF_UUID" --name "$STAFF_NAME" --role builder
python3 "$TOOL" --server-root "$SERVER" assign --uuid "$STAFF_UUID" --name "$STAFF_NAME" --role developer
python3 "$TOOL" --server-root "$SERVER" assign --uuid "$STAFF_UUID" --name "$STAFF_NAME" --role developer-edit
```

Sin `--apply`, esos comandos solo muestran la propuesta. Seleccionar UN rol; no ejecutar todos como asignación sucesiva. Para admin/owner cambiar `--role` al preset correspondiente. `list` muestra nombres y UUID: conservar su salida en canal administrativo.

## Aplicar el rol elegido

Cerrar las sesiones y detener correctamente el servidor durante mantenimiento; los grants se cargan al iniciar y no hay recarga en caliente. La herramienta verifica mediante Docker que el contenedor esté detenido y que monte exactamente el directorio indicado.

```sh
# Detener desde el panel o API Wings; confirmar estado STOPPED.
# No usar docker stop: Wings reinicia el principal.
python3 "$TOOL" --server-root "$SERVER" assign --uuid "$STAFF_UUID" --name "$STAFF_NAME" --role builder --apply --container "$CONTAINER"
python3 "$TOOL" --server-root "$SERVER" list
# Iniciar desde el panel o API Wings después de comprobar ambos archivos.
```

Antes de escribir se respaldan ambos archivos bajo `$SERVER/.staff-permission-backups/FECHA_UTC/`, con manifest que registra presencia original. Un fallo de escritura intenta restaurar ambos archivos. No arrancar si hay error: revisar el backup y que ambos archivos correspondan al mismo estado, especialmente después de una interrupción del proceso/host. No se borra el historial de backups.

## Uso dentro del juego

Cuenta autorizada de edición:

```text
/pradmin editmode status
/pradmin editmode on
/pradmin editmode off
```

ON exige OP4 + edit + editmode y activa Creative/sesión; OFF revoca sesión. Supervivencia, Creative con sesión OFF y OP sin grants no permiten editar mapas seguros. Logout/reinicio revocan la sesión; al reconectar hay que activar ON otra vez. No confundir esto con `/gamemode creative` ni con un rango visual.

## Retirar acceso o bajar a moderador

Para quitar OP y todos los grants de esta persona:

```sh
# Detener desde el panel o API Wings; confirmar estado STOPPED.
# No usar docker stop: Wings reinicia el principal.
python3 "$TOOL" --server-root "$SERVER" revoke --uuid "$STAFF_UUID" --apply --container "$CONTAINER"
# Iniciar desde el panel o API Wings después de comprobar ambos archivos.
```

Para conservar moderación y retirar edición/administración crítica, aplicar `--role moderator`: sustituye los grants anteriores y deja OP3. No basta con borrar un nodo mientras siga activa una sesión vieja: parada/reinicio eliminan esa sesión.

Verificar después con la cuenta afectada: rol/nodos en `list`, denegación de edición y comandos críticos sin autorización, edición solo ON en el rol autorizado y logs. Asignar presets no certifica staff, multiplayer, batallas ni apertura.

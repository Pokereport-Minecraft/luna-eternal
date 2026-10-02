# Roles administrativos — 2026-10-02

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

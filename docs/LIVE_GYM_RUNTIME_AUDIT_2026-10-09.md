# Diagnóstico en vivo: gimnasios y clases antiguas

## Estado guardado

El trabajo anterior está en los commits `e234149d` y `d22ee96e`, publicados en `codex/production-gym-battle-hardening`. Incluye los 72 PNG Halloween, la copia/hash de originales, tipografía, pase nivel 1/100, parkour, mixins y reporte de despliegue. El checkout principal se conserva sin sobrescribir su cambio local. La configuración local no rastreada se conserva fuera de Git.

## Reproducción y evidencia

Sesión en vivo del 9 de octubre (America/Bogota; log UTC del 10 de octubre). El usuario confirma que la caída del parkour ya vuelve al inicio; los problemas actuales son colocar bloques y el clic en Sabrina, también reproducido por un jugador normal con Blaine.

TheJuanCE: dimensión gimnasios, Creative (`playerGameType=1`), editmode autorizado activo. Sabrina posee las etiquetas `luna_lider`, `luna_gym_sabrina`, `luna_arena`, `luna_decorativo`. Blaine también tiene las etiquetas correctas. No se atribuye el problema a un permiso exclusivo del usuario.

En `mods` se detectaron dos candidatos Fabric con `id=lunaeternal`, `version=0.1.0`:

- `lunaeternal-0.1.0.jar`: SHA256 `ed83825868a26ed50d333cb9fc9c33e9ccfb0098959b1a34a3df40d38174a18e`, actualizado y publicado.
- `lunaeternal-0.1.0.PRE-PROGRESSION2.jar`: SHA256 `b03fbf048c35634cb3781f0c277c7cb83392c82a6d9da19faa6337e9a6cced05`, respaldo antiguo con las mismas clases.

El código/clase del JAR actualizado contiene radio Hogar 29.999.984 y el mensaje de liberación de dimensiones custom. El log de arranque real muestra radio 5.000 y el mensaje antiguo sin esa liberación. El archivo instalado correcto no demuestra qué candidato resolvió Fabric. La lista de mods solo mostraba `lunaeternal 0.1.0`, por lo que las comprobaciones anteriores no detectaron el respaldo. También reaparecen avisos placeholder en ese runtime; el test de staging se hizo sin el candidato duplicado.

La consulta vanilla `execute in ... run worldborder get` NO mide el borde de esa dimensión en 1.21.1: `WorldBorderCommand` obtiene siempre el Overworld, confirmado en bytecode. El diagnóstico por dimensión debe leer los objetos `ServerWorld.getWorldBorder()` reales.

## Reparación autorizada y preventiva

Se conserva el respaldo fuera de `mods`, en `backups/quarantined-mods/20261009-duplicate-luna/`, con su hash intacto. Se recarga el proceso tras aviso y guardado; no se restaura ningún mundo, DB ni configuración. La corrección publicada no se modifica ni se reemplaza por una versión antigua.

`tools/audit_mod_jars.py` es un preflight de solo lectura que falla ante IDs Fabric duplicados, incluso con la misma versión. Debe ejecutarse contra el directorio real de producción antes de cada arranque/despliegue. No mueve ni elimina archivos automáticamente.

La confirmación final necesita el mensaje nuevo de arranque, un único candidato Luna, las consultas de borde reales y una interacción del usuario en vivo; no basta con los autotests de staging.

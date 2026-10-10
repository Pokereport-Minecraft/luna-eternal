# Bot PokeReport

Fuente importada del bot real del VPS. Conserva tickets, contribuciones, moderación y bienvenidas. Python 3.10+, `requirements.txt`, OpenSSL. Arranque `python -m discord_bot.runtime`; configuración externa en `POKEREPORT_CONFIG`, credencial únicamente en `DISCORD_TOKEN`, fuera de Git.

Producción: `pokereport-discord.service`, código `/opt/pokereport-discord/current`, configuración `/etc/pokereport-discord/config.json`, datos privados `/var/lib/pokereport-discord`. No sustituir configuración ni `tickets.sqlite`. Comunidad utiliza `community.sqlite` separado, con publicaciones, presupuesto de avisos y perfiles.

Comandos `/empezar`, `/avisos`, `/perfil personaje:Ash`, `/estado-comunidad`, `/publicar-aviso`. Los dos últimos exigen roles administrativos reales; comandos registrados individualmente sin borrar otros. Alias voluntarios por roles reales, revisión cada cinco minutos. El nombre no verifica identidad Minecraft; el propietario y miembros fuera de jerarquía no pueden modificarse.

Actualizaciones del cliente verificadas mediante manifiesto Ed25519 cada diez minutos; cambios de servidor necesitan comunicado administrativo. Sin DMs, sin everyone/here. Mención exclusiva al rol voluntario de actualizaciones como máximo cada 24 horas; sin permiso publica silenciosamente. Una pregunta comunitaria semanal.

Validación: `python -m unittest discover -s tests -v`. Desplegar candidatos inmutables, probar antes de cambiar symlink, respaldar ambas SQLite mediante su API y reiniciar exclusivamente el bot. No reiniciar Minecraft. Reorganización mediante `tools/discord_community.py` y IDs estables; no ejecutar el instalador antiguo para reorganizar. Limpieza histórica puntual autorizada; no hay borrado recurrente.

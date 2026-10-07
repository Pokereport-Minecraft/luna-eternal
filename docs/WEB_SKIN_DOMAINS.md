# Skins web: dominios permitidos

Se comprobó en logs que /skin set web rechazaba cdn.discordapp.com y www.minecraftskins.com por allowedDomains. Se añadieron únicamente cdn.discordapp.com, www.minecraftskins.com y minecraftskins.com, conservando los tres dominios existentes y todos los demás ajustes/permisos.

Backup: /var/backups/pokereport/skin-domains/20261004T033216Z/config.json.

Se ejecutó skin config reload desde consola; latest.log confirma Config has been reloaded. El enlace Discord aportado responde HTTP 200 image/png desde el nodo. No se aplicó una skin a una cuenta específica; el usuario debe repetir su comando. Generación final vía MineSkin y apariencia dentro del juego pendientes.

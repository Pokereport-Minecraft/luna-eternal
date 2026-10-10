# Comunidad Discord — 2026-10-09 America/Bogota

Bot real del VPS importado y preservado. Activo en `/opt/pokereport-discord/releases/20261009-community-v3`. Configuración y backups de tickets/comunidad privados, ambas SQLite con integrity_check satisfactorio. No se reinició Minecraft; entrenadores pendientes por decisión del usuario.

Cinco canales iniciales, nombres legibles con emojis y canales adicionales mediante intereses opcionales. Bienvenidas nuevas en canal secundario. No se eliminaron canales ni roles; normas, guías y paneles de roles conservados.

Limpieza autorizada: 61 mensajes antiguos de bienvenida y 15 avisos antiguos inventariados. Chats y tickets intactos. El anuncio actual del cliente 0.1.3 fue preservado. Copias/planes bajo `build/discord-info-cleanup/`, ignorado por Git. Gestionar apodos y gestionar mensajes en ambos canales confirmados.

Automatización independiente: revisión cada diez minutos del manifiesto Ed25519 oficial, concordancia de versión y disponibilidad/tamaño del JAR y descargas. No recalcula el hash del JAR completo en cada revisión. Ledger persistente y nonce evitan duplicados tras reinicios. Actividad semanal sin recuperar semanas perdidas. Avisos exclusivamente voluntarios, máximo una mención cada 24 horas y publicación silenciosa cuando no hay permiso. Cambios de servidor se informan con `/publicar-aviso`, referencia única y roles Fundador/Admin; no se afirma integrar automáticamente el despliegue del servidor.

`/perfil personaje:Ash`: personaje elegido voluntariamente, prefijo por roles reales, revisión cada cinco minutos de perfiles registrados. No verifica cuentas Minecraft, no asigna roles comerciales ni cambia apodos arbitrariamente. Discord impide modificar al propietario o miembros fuera de jerarquía.

12 pruebas pasan en discord.py 2.7.1 del VPS: constructor de comandos, persistencia, publicación inmutable, presupuesto exacto de 24 horas, firma inválida, publicación incompleta, tamaño, URLs oficiales y perfiles sin menciones/prefijos inyectados. Servicio activo, gateway conectado, tickets preparados, sin errores de comunidad. Prueba interactiva de `/perfil` con un jugador pendiente.

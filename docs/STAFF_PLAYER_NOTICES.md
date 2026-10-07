# Avisos automáticos de jugadores: solo staff

Se filtran únicamente los anuncios automáticos del chat: muerte, entrada, cambio de nombre al entrar, salida y avances. El chat entre jugadores y los mensajes personales de comandos/juego se mantienen.

El receptor debe ser OP real y tener un rango de staff administrativo. Los rangos comerciales no conceden acceso. La ruta común cubre las muertes con reglas de visibilidad de equipos; hay rutas adicionales para reglas de mundo que desactivan el anuncio, sin duplicar la ruta normal. No se modifica la pantalla de muerte ni sus mecánicas.

Validación local: 242 pruebas, 240 PASS, 2 SKIPPED. Clasificación de eventos, exclusión del chat ordinario/feedback y matriz de permisos por rango verificadas. QA visual con cuentas reales pendiente.

JAR SHA256: daadba1436c1ec319b451fda9ad9f3881bb43a7f39a88dc6117de838c9640eb0.

Cambio exclusivamente de servidor: no exige actualizar el launcher ni reemplazar opciones del cliente. No se ejecuta de nuevo el reset de jugadores.

Staging: inicio correcto con mixins obligatorios y 803 autotests PASS / 0 FAIL. No se ejecutan fixtures sobre producción.

Producción: desplegado con backup coordinado /var/backups/pokereport/preopen-fixes/20261004T004046Z (SQL + archivo completo del servidor). Inicio Done confirmado, sin fallos de mixins y puertos TCP 25565/25567 accesibles. Staging detenido después de validar. Configuración de Voice Chat y EasyAuth conservada. QA visual con cuentas reales pendiente.

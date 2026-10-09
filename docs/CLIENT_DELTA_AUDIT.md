# POKEREPORT NETWORK — AUDITORÍA DELTA DEL CLIENTE

**Fecha:** 08/10/2026  
**Objetivo:** Análisis minucioso de compatibilidad cliente-servidor y justificación del nuevo Release Candidate de cliente.

---

## 1. PREGUNTA CLAVE: ¿REQUIERE EL CLIENTE UNA ACTUALIZACIÓN?

### **DICTAMEN: SÍ, ES ESTRICTAMENTE NECESARIO ACTUALIZAR EL CLIENTE.**

### Justificación Técnica:
1. **NPC de Cartas y Pantalla GUI (`Apps.java`):**
   - El nuevo NPC en Ciudadela envía el paquete `PedirAbrirComercio("cartas")`.
   - La versión anterior del cliente en producción no tenía registrado este caso en `Apps.java`, por lo que caía en `default -> {}` y no abría la pantalla interactiva `CartasScreen(null)`. Sin el JAR de cliente actualizado, el NPC de cartas no responde a los clics de los usuarios.
2. **Corrección de Ranuras de Crianza (`CrianzaScreen.java`):**
   - El cliente anterior bloqueaba la interfaz gráfica de crianza asumiendo que los jugadores sin kits de rango solo tenían 1 ranura fija.
   - El código actualizado sincroniza las ranuras dinámicamente con las licencias de entrenador (`TrainerLicense`) y progresión del servidor.
3. **Estabilidad de Combates RCT y Mixins de Batalla:**
   - Se integraron mejoras en `BattleActorChoiceFixMixin` y manejadores de paquetes de combate para prevenir congelamientos de cliente durante la selección de movimientos en gimnasios.
4. **Consistencia de Artefactos y Modpack:**
   - El archivo `lunaeternal-0.1.0.jar` actúa como mod dual (servidor y cliente). Al actualizar el JAR de servidor, el cliente debe coincidir para evitar desincronizaciones en el protocolo de red (canales `CustomPayload`).

---

## 2. DIFERENCIAL DE ARCHIVOS Y ASSETS

| Archivo / Componente | Modificado en Cliente? | Justificación |
| :--- | :--- | :--- |
| `mods/lunaeternal-0.1.0.jar` | Sí | Soporte de NPC de cartas, mixins de combate y GUI crianza. |
| `mods/lunaneon-0.1.0.jar` | No (Sin cambios) | Assets cosméticos y renderizado de partículas intacto. |
| `config/cobblemon-cards.json` | No (Sin cambios) | Configuración de cartas sincronizada. |
| `overrides/servers.dat` | No (Sin cambios) | IP de producción `15.235.16.131` preservada. |

---

## 3. AUDITORÍA DE SEGURIDAD (ANTI-LEAKS)

Se validó con la herramienta de empaquetado que el modpack base y los bundles no contengan ningún mod o archivo de configuración exclusivo de servidor:
- **Mods de servidor auditados y excluidos:**
  - `lootr-fabric` (solo contenido del modpack en cliente si es interfaz visual, pero configs de servidor excluidas)
  - `claimblocks` (excluido)
  - `geyser` / `floodgate` (excluidos)
  - `luckperms` (excluido)
  - `spark` (excluido)
  - `mariadb` / `hikaricp` (excluidos)
- **Archivos sensibles:** Cero presencia de `.env`, `.db`, `token`, claves privadas ni credenciales de base de datos.

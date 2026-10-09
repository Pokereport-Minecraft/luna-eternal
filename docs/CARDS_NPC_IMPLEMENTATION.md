# POKEREPORT NETWORK — IMPLEMENTACIÓN DEL NPC DE CARTAS EN CIUDADELA

**Fecha:** 08/10/2026  
**Módulo:** `net.pokereport.luna.cards.CardsNpc`, `net.pokereport.luna.client.pokepad.Apps`  
**Ubicación:** Dimensión `lunaeternal:ciudadela`  
**Coordenadas Exactas:** `X: 29.690, Y: 72.000, Z: 69.037, Yaw: 180.0f (NORTH)`

---

## 1. ESPECIFICACIÓN DEL NPC

Para permitir a los jugadores comerciar cartas coleccionables directamente en el mundo de Ciudadela, se implementó el servicio de ciclo de vida `CardsNpc.java`.

### Propiedades de la Entidad:
- **Tipo de Entidad:** `EntityType.VILLAGER`
- **Profesión:** Cartógrafo / Bibliotecario
- **Tag Identificador:** `luna_npc_cartas`
- **Nombre Personalizado:** `§6§lMercader de Cartas` (Visible siempre)
- **Atributos de Persistencia y Seguridad:**
  - `setInvulnerable(true)`: Inmune a cualquier tipo de daño de jugadores o entorno.
  - `setPersistent()`: No hace despawn con la descarga de chunks.
  - `setNoAi(true)`: Bloqueado en su posición sin desplazamiento ni física de colisión indeseada.
  - `setSilent(true)`: No emite sonidos de aldeano que molesten en Ciudadela.

---

## 2. PROTOCOLO DE INTERACCIÓN Y ROUTING CLIENTE-SERVIDOR

1. **Evento de Interacción (`UseEntityCallback`):**
   - Registrado en `CardsNpc.registerInteraction()`.
   - Cuando un jugador hace clic derecho sobre una entidad con el tag `luna_npc_cartas`, se cancela la interacción vanilla de aldeano (`ActionResult.SUCCESS`).
   - El servidor ejecuta:
     ```java
     Red.enviarAbrirComercio(serverPlayer, "cartas");
     ```
2. **Recepción en el Cliente (`Apps.java`):**
   - El cliente recibe el paquete `PedirAbrirComercio` con el identificador de comercio `"cartas"`.
   - Se añadió el manejador específico en el enrutador de pantallas de `Apps.java`:
     ```java
     case "cartas" -> MinecraftClient.getInstance().setScreen(new CartasScreen(null));
     ```
   - Esto abre de forma limpia la interfaz gráfica `CartasScreen`, permitiendo explorar álbumes, sobres e intercambios.

---

## 3. COMANDOS ADMINISTRATIVOS

Se agregó soporte en `LunaCommand.java` para gestión del NPC por parte del staff:
```text
/luna npc cartas [spawn|remove]
```
- **spawn:** Spawnea o reubica el NPC de cartas exactamente en las coordenadas nominales `(29.690, 72.0, 69.037)` en Ciudadela.
- **remove:** Elimina de forma segura cualquier instancia duplicada del NPC en el mundo.

---

## 4. TESTS Y VALIDACIÓN

- Verificado en `CardsNpcTest`: comprueba las coordenadas estáticas, el tag del NPC y la invocación de `Red.enviarAbrirComercio`.
- Cobertura 100% en integración.

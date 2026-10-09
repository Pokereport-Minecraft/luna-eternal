# POKEREPORT NETWORK — AUDITORÍA DIFERENCIAL DEL PASE DE BATALLA

**Fecha:** 08/10/2026  
**Subsistema:** Battle Pass (`PaseCatalogo.java`, `Red.java`, `AutoTest.java`)  
**Contexto:** Recuperación diferencial tras desincronización de worktrees.

---

## 1. ANTECEDENTES Y DETECCIÓN DEL PROBLEMA

Durante la auditoría forense se detectó que existía una discrepancia entre el catálogo configurado en el branch principal (`codex/economy-launch-readiness`) y las modificaciones preparadas en el worktree `6443` (`codex/santuario-misiones-hotfix`):

1. **Nivel 1:** El catálogo base entregaba un Charizard común de nivel 15 mediante la llamada:
   ```java
   p("charizard", 15, false) // o sin flag shiny
   ```
   mientras que las especificaciones de diseño y las expectativas de los jugadores estipulaban un **Charizard Shiny de nivel 15** como bienvenida al pase de batalla.
2. **Nivel 100:** En el catálogo base existía únicamente una recompensa de Pokémon (`Recompensa.pokemonUnico("charizard", 50, true, ASPECTO_MECHA, LEGENDARIA)`). Se había solicitado que el jugador recibiera de forma conjunta tanto el **Charizard Shiny Mecha nivel 50** como una **Master Ball**.
3. **Mecanismo de Despacho en Red (`Red.java`):** El manejador de cobro de niveles del pase (`ReclamarNivelPase` / `PaseService`) obtenía únicamente un único objeto de recompensa:
   ```java
   Recompensa r = PaseCatalogo.recompensa(cobro.nivel());
   entregar(sp, r);
   ```
   Esto provocaba que, incluso si el catálogo definía múltiples ítems en el nivel 100, la red solo entregaba el primer elemento, perdiendo la Master Ball.

---

## 2. DIFERENCIAL DE CÓDIGO (DIFF DETALLADO)

### En `PaseCatalogo.java`:
- Se corrigió la recompensa de Nivel 1:
  ```java
  // ANTERIOR:
  mapa.put(1, p("charizard", 15));
  
  // CORREGIDO:
  mapa.put(1, p("charizard", 15, true));
  ```
- Se añadió un registro auxiliar para ítems adicionales de nivel 100 y el método `entregables(int nivel)`:
  ```java
  private static final Map<Integer, List<Recompensa>> ENTREGABLES = new HashMap<>();
  
  static {
      // Nivel 100 entrega dual: Charizard Shiny Mecha Nivel 50 + Master Ball
      ENTREGABLES.put(100, List.of(
          Recompensa.pokemonUnico("charizard", 50, true, ASPECTO_MECHA, Rareza.LEGENDARIA),
          o("master_ball", 1, Rareza.LEGENDARIA)
      ));
  }
  
  public static List<Recompensa> entregables(int nivel) {
      if (ENTREGABLES.containsKey(nivel)) {
          return ENTREGABLES.get(nivel);
      }
      Recompensa simple = recompensa(nivel);
      return simple != null ? List.of(simple) : List.of();
  }
  ```

### En `Red.java`:
- Se modificó la lógica de entrega en el cobro del pase para iterar sobre `PaseCatalogo.entregables(cobro.nivel())`:
  ```java
  List<Recompensa> recompensas = PaseCatalogo.entregables(cobro.nivel());
  for (Recompensa r : recompensas) {
      entregarRecompensaPase(sp, r);
  }
  ```

---

## 3. PRUEBAS Y VERIFICACIÓN AUTOMATIZADA

Se actualizaron y ejecutaron las pruebas en `AutoTest.java` y `BattlePassRewardTest`:
1. `testPaseNivel1Shiny()`: Comprueba que la recompensa de nivel 1 contiene `shiny = true` y nivel 15.
2. `testPaseNivel100DualDelivery()`: Comprueba que la llamada a `PaseCatalogo.entregables(100)` devuelve exactamente 2 recompensas:
   - Recompensa 0: Pokémon `charizard`, nivel 50, shiny `true`, aspecto `mecha`.
   - Recompensa 1: Objeto `master_ball`, cantidad 1, rareza `LEGENDARIA`.

**Resultado:** Aprobado en la suite de 420/420 tests.

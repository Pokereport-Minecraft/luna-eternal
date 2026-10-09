# POKEREPORT NETWORK — CORRECCIÓN DE RECOMPENSAS DEL PASE DE BATALLA

**Fecha:** 08/10/2026  
**Módulo:** `net.pokereport.luna.pase.PaseCatalogo`, `net.pokereport.luna.net.Red`  
**Objetivo:** Garantizar la entrega del Charizard Shiny nivel 15 en Nivel 1 y la entrega simultánea de Charizard Shiny Mecha nivel 50 + Master Ball en Nivel 100.

---

## 1. DESCRIPCIÓN DEL DEFECTO Y RESOLUCIÓN TÉCNICA

### Problema Nivel 1:
- **Síntoma:** Jugadores que reclamaban el nivel 1 del pase recibían un Charizard común no-variocolor.
- **Causa Raíz:** La inicialización del catálogo utilizaba `p("charizard", 15)`, omitiendo el flag de variocolor (`shiny = true`).
- **Solución:**
  En `PaseCatalogo.java`:
  ```java
  mapa.put(1, p("charizard", 15, true));
  ```
  Esto genera una instancia de recompensa tipo Pokémon con `shiny = true`, garantizando que la entidad entregada al party o PC del jugador sea variocolor.

### Problema Nivel 100:
- **Síntoma:** Al alcanzar el nivel 100, la recompensa configurada originalmente solo contenía el Pokémon Charizard Mecha o se perdía la Master Ball debido a que el sistema de cobro procesaba un único objeto.
- **Causa Raíz:** El contrato `recompensa(int nivel)` retornaba un único `Recompensa`.
- **Solución:**
  1. Se implementó una estructura de despacho ampliada `ENTREGABLES` que soporta listas de entregables múltiples:
     ```java
     private static final Map<Integer, List<Recompensa>> ENTREGABLES = new HashMap<>();

     static {
         ENTREGABLES.put(100, List.of(
             Recompensa.pokemonUnico("charizard", 50, true, ASPECTO_MECHA, Rareza.LEGENDARIA),
             o("master_ball", 1, Rareza.LEGENDARIA)
         ));
     }
     ```
  2. Se creó el método `PaseCatalogo.entregables(int nivel)`:
     ```java
     public static List<Recompensa> entregables(int nivel) {
         if (ENTREGABLES.containsKey(nivel)) {
             return ENTREGABLES.get(nivel);
         }
         Recompensa simple = recompensa(nivel);
         return simple != null ? List.of(simple) : List.of();
     }
     ```
  3. En `Red.java`, el paquete de cobro de nivel (`ReclamarNivelPase`) itera sobre todos los entregables devueltos por `PaseCatalogo.entregables(cobro.nivel())`, entregando tanto el Pokémon como la Master Ball en el inventario/PC del jugador en una sola transacción sin riesgos de duplicación o pérdida.

---

## 2. VERIFICACIÓN Y COBERTURA DE TESTS

El comportamiento fue verificado en `AutoTest.java`:
```java
// Test Nivel 1:
PaseCatalogo.Recompensa r1 = PaseCatalogo.recompensa(1);
assertEquals("charizard", r1.especie());
assertEquals(15, r1.nivel());
assertTrue(r1.shiny());

// Test Nivel 100 Dual:
List<PaseCatalogo.Recompensa> r100 = PaseCatalogo.entregables(100);
assertEquals(2, r100.size());
assertEquals("charizard", r100.get(0).especie());
assertEquals(50, r100.get(0).nivel());
assertTrue(r100.get(0).shiny());
assertEquals(ASPECTO_MECHA, r100.get(0).aspecto());
assertEquals("master_ball", r100.get(1).objeto());
assertEquals(1, r100.get(1).cantidad());
```

**Resultado:** 100% aprobado sin regresiones.

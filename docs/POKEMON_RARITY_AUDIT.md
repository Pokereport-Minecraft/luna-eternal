# POKEREPORT NETWORK — AUDITORÍA DE RAREZA DE POKÉMON (GEN 1 & GEN 2)

**Fecha:** 8 de Octubre de 2026  
**Alcance:** Generación 1 (1–151) y Generación 2 (152–251)  
**Objetivo:** Balance de rareza, eliminación de monopolios de captura y ajuste de dificultad F2P.

---

## 1. MATRIZ DE CLASIFICACIÓN POR TIERS

Todos los Pokémon implementados de Generación 1 y 2 se clasifican en una taxonomía unificada gobernada por `ProgressionBalanceConfig`:

### TIER A — COMUNES (Multiplicador: 1.0)
* **Gen 1:** Caterpie, Metapod, Butterfree, Weedle, Kakuna, Beedrill, Pidgey, Pidgeotto, Rattata, Raticate, Spearow, Fearow, Zubat, Golbat, Oddish, Gloom, Paras, Venonat, Diglett, Meowth, Psyduck, Poliwag, Bellsprout, Weepinbell, Geodude, Graveler, Slowpoke, Doduo, Dodrio, Seel, Grimer, Shellder, Krabby, Kingler, Voltorb, Goldeen, Seaking, Staryu, Magikarp.
* **Gen 2:** Sentret, Furret, Hoothoot, Noctowl, Ledyba, Ledian, Spinarak, Ariados, Crobat, Chinchou, Pichu, Cleffa, Igglybuff, Natu, Xatu, Mareep, Flaaffy, Marill, Hoppip, Skiploom, Sunkern, Sunflora, Wooper, Quagsire, Pineco, Dunsparce, Snubbull, Granbull, Qwilfish, Remoraid, Octillery, Delibird, Mantine, Phanpy, Smeargle, Tyrogue, Smoochum, Magby, Swinub.
* **Comportamiento:** Spawns abundantes para dar sensación de mundo vivo y proveer capturas básicas para Pokédex y entrenamiento inicial.

### TIER B — POCO COMUNES (Multiplicador: 0.90)
* **Gen 1:** Ekans, Arbok, Sandshrew, Sandslash, Nidoran♀, Nidorina, Nidoran♂, Nidorino, Clefairy, Vulpix, Jigglypuff, Parasect, Venomoth, Dugtrio, Persian, Golduck, Mankey, Primeape, Growlithe, Poliwhirl, Abra, Kadabra, Machop, Machoke, Tentacool, Tentacruel, Ponyta, Rapidash, Magneton, Farfetch'd, Drowzee, Hypno, Exeggcute, Koffing, Weezing, Rhyhorn, Tangela, Horsea, Seadra.
* **Gen 2:** Ampharos, Azumarill, Sudowoodo, Politoed, Jumpluff, Aipom, Yanma, Quagsire, Murkrow, Slowking, Misdreavus, Wobbuffet, Girafarig, Forretress, Gligar, Steelix, Shuckle, Sneasel, Teddiursa, Ursaring, Slugma, Magcargo, Piloswine, Corsola, Skarmory, Houndour, Houndoom, Kingdra, Donphan, Stantler, Miltank.
* **Comportamiento:** Requieren moverse a biomas específicos (desiertos, pantanos, montañas, cuevas).

### TIER C — RAROS Y ESPECIALES (Multiplicador: 0.65)
* **Gen 1:** Ninetales, Arcanine, Poliwrath, Alakazam, Machamp, Victreebel, Slowbro, Magnemite, Magneton, Cloyster, Gastly, Haunter, Gengar, Onix, Exeggutor, Marowak, Hitmonlee, Hitmonchan, Lickitung, Rhydon, Chansey, Kangaskhan, Mr. Mime, Scyther, Jynx, Electabuzz, Magmar, Pinsir, Tauros, Gyarados, Lapras, Ditto, Eevee, Vaporeon, Jolteon, Flareon, Porygon, Snorlax.
* **Gen 2:** Bellossom, Espeon, Umbreon, Scizor, Heracross, Porygon2, Hitmontop, Blissey.
* **Fósiles Gen 1:** Omanyte, Omastar, Kabuto, Kabutops, Aerodactyl.
* **Comportamiento:** Tasa de aparición notablemente reducida. Fósiles requieren exploración minera y reactivación en máquinas con tasa de shiny corregida (1/4096).

### TIER D — INICIALES (Multiplicador: 0.35)
* **Gen 1:** Bulbasaur, Ivysaur, Venusaur, Charmander, Charmeleon, Charizard, Squirtle, Wartortle, Blastoise.
* **Gen 2:** Chikorita, Bayleef, Meganium, Cyndaquil, Quilava, Typhlosion, Totodile, Croconaw, Feraligatr.
* **Comportamiento:** Raros en estado salvaje (peso efectivo reducido en 65%). Esto asegura que la elección del inicial al inicio de la aventura mantenga valor intrínseco.

### TIER E — PSEUDOLEGENDARIOS (Multiplicador: 0.20)
* **Gen 1:** Dratini, Dragonair, Dragonite.
* **Gen 2:** Larvitar, Pupitar, Tyranitar.
* **Comportamiento:**
  * Dratini ya no inunda los océanos (se anula el peso de 84.0 en bucket uncommon mediante intercepción probabilística).
  * Larvitar pasa a ser un encuentro de alta montaña muy codiciado.
  * Formas finales (Dragonite, Tyranitar) no tienen spawn natural directo o tienen una tasa menor al 0.05.

### TIER S — LEGENDARIOS Y MÍTICOS (Multiplicador: 0.05 / Instanciados)
* **Aves Legendarias:** Articuno, Zapdos, Moltres.
* **Trío Bestias:** Raikou, Entei, Suicune.
* **Dúo de las Torres:** Lugia, Ho-oh.
* **Míticos:** Mew, Celebi.
* **Supremo:** Mewtwo.
* **Regla:** 0 spawns aleatorios por el mapa. Se obtienen a través del sistema de Expediciones, Fragmentos y Misiones de Licencia Maestro/Campeón.

---

## 2. SHINIES — BALIZAMIENTO Y TELEMETRÍA

### 2.1 Tasas Base Auditadas
* Tasa salvaje base: `1 / 16,384` (Cobblemon config).
* Máquina de Fósiles actual: `1 / 100` (ANOMALÍA GRAVE: corregida a `1 / 4,096`).
* Árbol de Miel: `1 / 4,000`.

### 2.2 Métricas de Monitoreo (Dashboard & Logs)
Se implementa el colector `FaunaTelemetry` para auditar diariamente:
1. `shinies_spawned_daily`
2. `shinies_captured_daily`
3. `shinies_by_method` (WILD, BREEDING, FOSSIL, EVENT)
4. `shiny_rate_per_player_hour`
Cualquier desviación superior a 1 shiny por cada 15 horas de juego promedio activa una alerta en logs de seguridad.

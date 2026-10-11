package net.pokereport.luna.test;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.gitlab.srcmc.rctapi.api.models.converter.PokemonModelConverter;
import com.gitlab.srcmc.rctapi.api.errors.RCTErrors;
import net.pokereport.luna.gym.Adaptador;
import net.pokereport.luna.gym.Gimnasio;
import java.util.*;
import java.util.function.BiConsumer;

/** Real registered species/moves/items, restricted by ProtectionSelfTest's staging gate. */
public final class TrainerAdaptationSelfTest {
    private TrainerAdaptationSelfTest() {}
    public static void run(BiConsumer<String, Boolean> check) {
        var converter = new PokemonModelConverter();
        List<List<Pokemon>> profiles = new ArrayList<>();
        for (String[] team : new String[][]{{"totodile", "squirtle", "psyduck", "poliwag", "staryu", "slowpoke"},
                {"chikorita", "oddish", "bellsprout", "bulbasaur", "hoppip", "paras"},
                {"cyndaquil", "houndour", "gastly", "pidgey", "sandshrew", "mareep"}}) {
            var profile = new ArrayList<Pokemon>();
            for (String name : team) profile.add(PokemonProperties.Companion.parse(name + " level=15").create());
            profiles.add(profile);
        }
        int responsive = 0;
        for (var gym : Gimnasio.TODOS) {
            check.accept("Entrenador " + gym.id() + " tiene repertorio adaptativo", !Adaptador.repertorioDe(gym).isEmpty());
            var variants = new HashSet<String>();
            for (var profile : profiles) {
                var team = Adaptador.prepararEquipo(gym, profile);
                check.accept("Entrenador " + gym.id() + " cantidad perfil " + profile.size(),
                        team.size() == Math.min(6, Math.max(Gimnasio.equipoMinimo(gym), profile.size())));
                boolean valid = true;
                for (var model : team) {
                    try {
                        var errors = RCTErrors.create();
                        var pokemon = converter.toTarget(model, errors); errors.check();
                        var legal = pokemon.getForm().getMoves().getAllLegalMoves().stream()
                                .map(com.cobblemon.mod.common.api.moves.MoveTemplate::getName).collect(java.util.stream.Collectors.toSet());
                        valid &= pokemon.getLevel() >= gym.nivel() - 1 && pokemon.getLevel() <= gym.nivel()
                                && pokemon.getMoveSet().getMoves().size() == 4
                                && pokemon.getMoveSet().getMoves().stream().allMatch(m -> legal.contains(m.getName()));
                    } catch (RuntimeException e) { valid = false;
                        net.pokereport.luna.LunaEternal.LOG.error("[TRAINER-QA] {} {}", gym.id(), model.getSpecies(), e);
                    }
                }
                check.accept("Entrenador " + gym.id() + " equipo legal perfil " + profile.size(), valid);
                // All profiles have six members: differences cannot be caused
                // merely by changing the requested team quantity.
                variants.add(team.stream().map(m -> m.getSpecies() + m.getMoveset()).toList().toString());
            }
            if (variants.size() > 1) responsive++;
        }
        check.accept("Los 23 retos responden a perfiles distintos", responsive == Gimnasio.TODOS.size());
        for (var profile : profiles) {
            for (int size = 4; size <= 6; size++) {
                var team = Adaptador.prepararEquipo(Gimnasio.de("giovanni"), profile.subList(0, size));
                check.accept("Giovanni conserva exactamente un Mewtwo con " + size + " participantes",
                        team.stream().filter(p -> "mewtwo".equals(p.getSpecies())).count() == 1);
            }
        }
        var fainted = PokemonProperties.Companion.parse("totodile level=15").create();
        fainted.setCurrentHealth(0);
        var full = new ArrayList<>(profiles.getLast()); full.set(0, fainted);
        check.accept("Brock cuenta los cinco participantes que RCT no excluye por KO", Adaptador.prepararEquipo(Gimnasio.de("brock"), full).size() == 5);
        check.accept("Brock conserva paridad con un Pokémon", Adaptador.prepararEquipo(Gimnasio.de("brock"), profiles.getFirst().subList(0, 1)).size() == 1);
        check.accept("Misty conserva paridad con dos Pokémon", Adaptador.prepararEquipo(Gimnasio.de("misty"), profiles.getFirst().subList(0, 2)).size() == 2);
        check.accept("Adaptación conserva Pokémon originales", fainted.getCurrentHealth() == 0
                && profiles.stream().flatMap(Collection::stream).allMatch(p -> p.getLevel() == 15));
        var mixed = Adaptador.prepararRivalMixto(List.of("alakazam", "arcanine", "blastoise", "venusaur", "scizor", "tyranitar", "dragonite", "snorlax"), profiles.getLast(), 100);
        check.accept("Torre genera seis rivales legales adaptados", mixed.size() == 6 && mixed.stream().allMatch(m -> m.getMoveset().size() == 4));
    }
}

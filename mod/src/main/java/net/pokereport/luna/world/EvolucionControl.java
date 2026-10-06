package net.pokereport.luna.world;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.text.Text;

public class EvolucionControl {
    public static void register() {
        // EVOLUTION_TESTED: Bloquea que el Pokémon alcance el estado "puede evolucionar"
        CobblemonEvents.EVOLUTION_TESTED.subscribe(Priority.HIGHEST, event -> {
            var resultProp = event.getEvolution().getResult();
            if (resultProp != null) {
                String speciesName = resultProp.getSpecies();
                if (speciesName == null) {
                    speciesName = event.getPokemon().getSpecies().getName();
                }
                Species species = PokemonSpecies.getByName(speciesName);
                if (species != null) {
                    int dex = species.getNationalPokedexNumber();
                    String form = resultProp.getForm();
                    if (form == null) form = event.getPokemon().getForm().getName();
                    
                    java.util.Set<String> labels = new java.util.HashSet<>();
                    try {
                        var formData = species.getFormByName(form);
                        if (formData != null) {
                            labels.addAll(formData.getLabels());
                        } else {
                            labels.addAll(species.getLabels());
                        }
                    } catch (Exception e) {}

                    if (!PoliticaFauna.generacionPermitida(dex, form, labels)) {
                        event.setResult(false);
                    }
                }
            }
            return kotlin.Unit.INSTANCE;
        });

        // EVOLUTION_ACCEPTED: Bloquea el intento de confirmar la evolución y notifica al usuario
        CobblemonEvents.EVOLUTION_ACCEPTED.subscribe(Priority.HIGHEST, event -> {
            var resultProp = event.getEvolution().getResult();
            if (resultProp != null) {
                String speciesName = resultProp.getSpecies();
                if (speciesName == null) {
                    speciesName = event.getPokemon().getSpecies().getName();
                }
                Species species = PokemonSpecies.getByName(speciesName);
                if (species != null) {
                    int dex = species.getNationalPokedexNumber();
                    String form = resultProp.getForm();
                    if (form == null) form = event.getPokemon().getForm().getName();
                    
                    java.util.Set<String> labels = new java.util.HashSet<>();
                    try {
                        var formData = species.getFormByName(form);
                        if (formData != null) {
                            labels.addAll(formData.getLabels());
                        } else {
                            labels.addAll(species.getLabels());
                        }
                    } catch (Exception e) {}

                    if (!PoliticaFauna.generacionPermitida(dex, form, labels)) {
                        event.cancel();
                        if (event.getPokemon().getOwnerPlayer() != null) {
                            event.getPokemon().getOwnerPlayer().sendMessage(
                                Text.of("§cEsta evolución estará disponible cuando llegue su generación a PokeReport.")
                            );
                        }
                    }
                }
            }
            return kotlin.Unit.INSTANCE;
        });
    }
}

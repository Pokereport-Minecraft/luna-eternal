package net.pokereport.luna.test;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.pokemon.FormData;

public class TestForms {
    public static void run() {
        Species raichu = PokemonSpecies.getByName("raichu");
        if (raichu != null) {
            System.out.println("Raichu Dex: " + raichu.getNationalPokedexNumber());
            for (FormData form : raichu.getForms()) {
                System.out.println("Form: " + form.getName() + " Labels: " + form.getLabels());
            }
        }
    }
}

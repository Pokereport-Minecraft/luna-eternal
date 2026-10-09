package net.pokereport.luna.pase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class BattlePassDuplicateClaimTest {

    @Test
    @DisplayName("Prevenir doble cobro del mismo nivel de pase")
    void testPrevenirDobleCobro() {
        Set<Integer> reclamados = new HashSet<>();
        reclamados.add(1);
        reclamados.add(5);
        reclamados.add(100);

        // Nivel 1 ya está en reclamados
        assertTrue(reclamados.contains(1), "Nivel 1 debe figurar reclamado");
        assertFalse(reclamados.add(1), "No debe permitir añadir/reclamar nivel 1 por segunda vez");

        // Nivel 2 no está reclamado
        assertFalse(reclamados.contains(2), "Nivel 2 no debe figurar reclamado inicialmente");
        assertTrue(reclamados.add(2), "Debe permitir reclamar nivel 2");
        assertFalse(reclamados.add(2), "No debe permitir reclamar nivel 2 duplicado");
    }

    @Test
    @DisplayName("Validación de rango de niveles entre 1 y MAX")
    void testRangoNivelesValidos() {
        assertNull(PaseCatalogo.de(0), "Nivel 0 debe ser nulo");
        assertNull(PaseCatalogo.de(PaseNivel.MAX + 1), "Nivel superior al máximo debe ser nulo");
        assertNotNull(PaseCatalogo.de(1), "Nivel 1 debe existir");
        assertNotNull(PaseCatalogo.de(PaseNivel.MAX), "Nivel máximo debe existir");
    }
}

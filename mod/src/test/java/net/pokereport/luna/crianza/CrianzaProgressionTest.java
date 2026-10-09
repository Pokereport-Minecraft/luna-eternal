package net.pokereport.luna.crianza;

import net.pokereport.luna.pase.PaseXp;
import net.pokereport.luna.quest.Quest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para la integración de crianza con progresión:
 * - Misión o7_criar ("Eclosiona 5 huevos" con objetivo HATCH)
 * - Vía / Oficio Criador (CRIADOR)
 * - Pase de Batalla (PaseXp.ECLOSION)
 * - Hook de OficiosListener.anotarEclosion() dentro de CrianzaService.ejecutarEntrega()
 */
public class CrianzaProgressionTest {

    @Test
    @DisplayName("El objetivo HATCH está correctamente mapeado en Quest.Objective.Type")
    void testQuestObjectiveTypeHatchExiste() {
        assertNotNull(Quest.Objective.Type.valueOf("HATCH"),
                "El tipo de objetivo HATCH debe existir en Quest.Objective.Type");
    }

    @Test
    @DisplayName("XP de pase de batalla para eclosión está definida y es positiva")
    void testPaseXpEclosionDefinida() {
        long xpPase = PaseXp.ECLOSION;
        assertTrue(xpPase > 0, "PaseXp.ECLOSION debe ser mayor a 0");
    }

    @Test
    @DisplayName("La misión o7_criar en quests.json utiliza el objetivo HATCH con meta 5")
    void testMisionO7CriarEnQuestsJson() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/data/lunaeternal/quests.json")) {
            assertNotNull(in, "quests.json debe estar presente en el classpath de recursos");
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(json.contains("\"id\": \"o7_criar\""), "quests.json debe contener la misión o7_criar");
            assertTrue(json.contains("\"type\": \"HATCH\""), "o7_criar debe apuntar al objetivo HATCH");
            assertTrue(json.contains("\"amount\": 5"), "o7_criar debe tener como meta 5 eclosiones");
            assertTrue(json.contains("\"path\": \"CRIADOR\""), "o7_criar debe recompensar la vía CRIADOR");
        }
    }

    @Test
    @DisplayName("OficiosListener expone anotarEclosion para avanzar HATCH, CRIADOR y Pase")
    void testOficiosListenerAnotarEclosionExiste() throws Exception {
        Path path = Path.of("src/main/java/net/pokereport/luna/progression/OficiosListener.java");
        if (!Files.exists(path)) {
            path = Path.of("mod/src/main/java/net/pokereport/luna/progression/OficiosListener.java");
        }
        assertTrue(Files.exists(path), "OficiosListener.java debe existir");
        String src = Files.readString(path);

        assertTrue(src.contains("public static void anotarEclosion(ServerPlayerEntity jugador)"),
                "OficiosListener debe declarar el método público anotarEclosion");
        assertTrue(src.contains("Path.CRIADOR"), "anotarEclosion debe otorgar XP a Path.CRIADOR");
        assertTrue(src.contains("Quest.Objective.Type.HATCH"), "anotarEclosion debe avanzar el objetivo HATCH");
        assertTrue(src.contains("PaseXp.ECLOSION"), "anotarEclosion debe recompensar PaseXp.ECLOSION");
    }

    @Test
    @DisplayName("CrianzaService invoca OficiosListener.anotarEclosion al entregar la cría con éxito")
    void testCrianzaServiceEnganchaAnotarEclosion() throws Exception {
        Path path = Path.of("src/main/java/net/pokereport/luna/crianza/CrianzaService.java");
        if (!Files.exists(path)) {
            path = Path.of("mod/src/main/java/net/pokereport/luna/crianza/CrianzaService.java");
        }
        assertTrue(Files.exists(path), "CrianzaService.java debe existir");
        String src = Files.readString(path);

        assertTrue(src.contains("OficiosListener.anotarEclosion(player)"),
                "CrianzaService.ejecutarEntrega debe llamar a OficiosListener.anotarEclosion al entregar la cría");
    }
}

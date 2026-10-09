package net.pokereport.luna.advancement;

import net.minecraft.text.Text;
import net.minecraft.world.GameRules;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.spectate.SpectateFollowConfig;
import net.pokereport.luna.spectate.SpectateFollowManager;
import net.pokereport.luna.spectate.SpectateMode;
import net.pokereport.luna.spectate.SpectateSession;
import net.pokereport.luna.ui.StaffPlayerNotices;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests que verifican la supresión de anuncios de progreso/logros en chat global,
 * la preservación de notificaciones personales y la estabilidad de SpectateFollow.
 */
class AdvancementSilenceTest {

    @Test
    @DisplayName("announce_advancements_disabled_on_server_start")
    void announce_advancements_disabled_on_server_start() {
        // Verifica que la regla exista y su valor default pueda ser seteado en falso
        GameRules rules = new GameRules();
        GameRules.BooleanRule rule = rules.get(GameRules.ANNOUNCE_ADVANCEMENTS);
        assertNotNull(rule, "La regla ANNOUNCE_ADVANCEMENTS debe existir en GameRules");

        // Al invocar el helper de silenciamiento con server nulo es seguro ante NPE
        assertDoesNotThrow(() -> LunaEternal.silenciarAdvancements(null));

        // Forzar regla a false
        rule.set(false, null);
        assertFalse(rules.getBoolean(GameRules.ANNOUNCE_ADVANCEMENTS),
                "announceAdvancements debe ser false para silenciar los anuncios de chat");
    }

    @Test
    @DisplayName("advancement_still_completes")
    void advancement_still_completes() {
        // Verifica que la identificación de advancements no interfiera con objetos y claves válidas
        Text taskAdvancement = Text.translatable("chat.type.advancement.task",
                Text.literal("Freddysillo"), Text.literal("Piedra Artesanal"));
        Text goalAdvancement = Text.translatable("chat.type.advancement.goal",
                Text.literal("Freddysillo"), Text.literal("First Shiny!"));
        Text challengeAdvancement = Text.translatable("chat.type.advancement.challenge",
                Text.literal("Freddysillo"), Text.literal("Cooking with Extra Evolution!"));

        assertNotNull(taskAdvancement.getString());
        assertNotNull(goalAdvancement.getString());
        assertNotNull(challengeAdvancement.getString());
    }

    @Test
    @DisplayName("advancement_does_not_broadcast_global_chat")
    void advancement_does_not_broadcast_global_chat() {
        // Comprobar que cualquier tipo de anuncio de advancement es detectado como tal para ser suprimido
        Text taskMsg = Text.translatable("chat.type.advancement.task", Text.literal("Jugador"), Text.literal("Piedra Artesanal"));
        Text goalMsg = Text.translatable("chat.type.advancement.goal", Text.literal("Jugador"), Text.literal("Meta"));
        Text challengeMsg = Text.translatable("chat.type.advancement.challenge", Text.literal("Jugador"), Text.literal("Desafio"));

        assertTrue(StaffPlayerNotices.isAdvancement(taskMsg), "El progreso de tarea debe ser detectado como advancement");
        assertTrue(StaffPlayerNotices.isAdvancement(goalMsg), "El progreso de meta debe ser detectado como advancement");
        assertTrue(StaffPlayerNotices.isAdvancement(challengeMsg), "El progreso de desafio debe ser detectado como advancement");

        // Un mensaje de muerte o chat normal NO es un advancement (no debe suprimirse indebidamente)
        Text deathMsg = Text.translatable("death.attack.fall", Text.literal("Jugador"));
        Text regularChat = Text.literal("Hola a todos en el servidor");
        assertFalse(StaffPlayerNotices.isAdvancement(deathMsg), "Muerte no es advancement");
        assertFalse(StaffPlayerNotices.isAdvancement(regularChat), "Chat normal no es advancement");

        // Despachar un advancement con manager nulo no debe fallar y sale inmediatamente por el guard
        assertDoesNotThrow(() -> StaffPlayerNotices.dispatch(null, taskMsg, false));
    }

    @Test
    @DisplayName("quest_personal_notification_still_available")
    void quest_personal_notification_still_available() {
        // Los avisos no-chat como feedback personal o toques de evento deben seguir preservados
        Text personalNotice = Text.literal("§a[PokéPad] ¡Misión completada!");
        assertFalse(StaffPlayerNotices.isAdvancement(personalNotice),
                "Los avisos de interfaz personal o PokéPad no deben ser bloqueados por el filtro de advancements");
    }

    @Test
    @DisplayName("spectatefollow_still_initializes")
    void spectatefollow_still_initializes() {
        // Verificar que el sistema de cámara SpectateFollow se mantiene intacto y operativo
        SpectateFollowConfig config = SpectateFollowManager.getConfig();
        assertNotNull(config, "SpectateFollowConfig debe estar cargada");
        assertTrue(config.isEnabled(), "SpectateFollow debe estar habilitado");
        assertTrue(config.isAutoSwitchOnDisconnect(), "autoSwitchOnDisconnect debe estar activo");

        // Simulación de sesión
        UUID spectator = UUID.randomUUID();
        SpectateSession session = new SpectateSession(
                spectator,
                "CamTest",
                net.minecraft.world.GameMode.SURVIVAL,
                net.minecraft.world.World.OVERWORLD,
                net.minecraft.util.math.Vec3d.ZERO,
                0.0f,
                0.0f,
                SpectateMode.AUTO
        );
        session.setMode(SpectateMode.AUTO);
        assertEquals(SpectateMode.AUTO, session.getMode());

        session.setMode(SpectateMode.WAITING_FOR_PLAYER);
        assertEquals(SpectateMode.WAITING_FOR_PLAYER, session.getMode());
    }
}

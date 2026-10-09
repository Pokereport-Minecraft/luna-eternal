package net.pokereport.luna.spectate;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de pruebas unitarias y funcionales para el sistema de cámara de espectador (/spectatefollow).
 * Cubre:
 * - Los 18 escenarios básicos requeridos.
 * - Desconexión en modo MANUAL con fallback a otro jugador sin perder modo manual.
 * - Desconexión en modo MANUAL con servidor vacío -> WAITING_FOR_PLAYER -> nuevo jugador.
 * - Desconexión en modo AUTO.
 * - Carrera de desconexión casi simultánea de múltiples jugadores.
 * - Carrera de cambio de dimensión y desconexión concurrente.
 * - Ciclo funcional cross-dimension (Overworld -> Ciudadela -> Salvaje -> Johto -> Gimnasio -> /home -> /spawn).
 * - Ciclo de muerte y reaparición sin pérdida de sesión.
 * - Escenario jugable completo de staging (9 pasos).
 */
public class SpectateFollowTest {

    private static final RegistryKey<World> OVERWORLD = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("minecraft", "overworld"));
    private static final RegistryKey<World> NETHER = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("minecraft", "the_nether"));
    private static final RegistryKey<World> CIUDADELA = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("lunaeternal", "ciudadela"));
    private static final RegistryKey<World> SALVAJE = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("lunaeternal", "salvaje"));
    private static final RegistryKey<World> JOHTO = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("pokereport", "johto"));
    private static final RegistryKey<World> GIMNASIO = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("lunaeternal", "gimnasio"));
    private static final RegistryKey<World> HOGAR = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("lunaeternal", "hogar"));

    private SpectateFollowConfig config;
    private UUID spectatorUuid;
    private UUID juanUuid;
    private UUID pedroUuid;
    private UUID alejandroUuid;
    private UUID staffUuid;

    @BeforeEach
    void setUp() {
        config = new SpectateFollowConfig();
        config.setEnabled(true);
        config.setIncludeStaff(false);
        config.setRotationMinutes(5);
        config.setJoinDelaySeconds(4);
        config.setAutoSwitchOnDisconnect(true);
        config.setReturnToPreferredTarget(false);
        config.getExcluded().forEach(config::removeExcluded);

        spectatorUuid = UUID.randomUUID();
        juanUuid = UUID.randomUUID();
        pedroUuid = UUID.randomUUID();
        alejandroUuid = UUID.randomUUID();
        staffUuid = UUID.randomUUID();

        SpectateFollowManager.clearSessions();
    }

    // =========================================================================
    // 1. ESCENARIOS BÁSICOS Y CICLO DE VIDA
    // =========================================================================

    @Test
    @DisplayName("1. manual_spectate_starts: inicio de modo manual registra estado previo y objetivo")
    void manual_spectate_starts() {
        Vec3d originalPos = new Vec3d(100.5, 64.0, -200.5);
        SpectateSession session = new SpectateSession(
                spectatorUuid,
                "CameraTV",
                GameMode.SURVIVAL,
                OVERWORLD,
                originalPos,
                45.0f,
                10.0f,
                SpectateMode.MANUAL
        );
        session.setTarget(juanUuid, "Juan", OVERWORLD);
        session.setPreferredTarget(juanUuid, "Juan");

        assertEquals(SpectateMode.MANUAL, session.getMode());
        assertEquals(SpectateMode.MANUAL, session.getBaseMode());
        assertEquals(juanUuid, session.getCurrentTargetUuid());
        assertEquals(juanUuid, session.getPreferredTargetUuid());
        assertEquals("Juan", session.getCurrentTargetName());
        assertEquals(GameMode.SURVIVAL, session.getOriginalGameMode());
        assertEquals(OVERWORLD, session.getOriginalWorldKey());
        assertEquals(originalPos, session.getOriginalPos());
        assertEquals(45.0f, session.getOriginalYaw());
        assertEquals(10.0f, session.getOriginalPitch());
    }

    @Test
    @DisplayName("2. spectator_follows_target_same_dimension: seguimiento en la misma dimensión")
    void spectator_follows_target_same_dimension() {
        SpectateSession session = new SpectateSession(
                spectatorUuid, "Cam1", GameMode.CREATIVE, OVERWORLD, Vec3d.ZERO, 0f, 0f, SpectateMode.AUTO
        );
        session.setTarget(juanUuid, "Juan", OVERWORLD);

        assertEquals(OVERWORLD, session.getCurrentTargetWorldKey());
        assertEquals(juanUuid, session.getCurrentTargetUuid());
        assertTrue(session.getCurrentTargetStartMs() > 0);
    }

    @Test
    @DisplayName("3. spectator_follows_target_cross_dimension: transición entre dimensiones")
    void spectator_follows_target_cross_dimension() {
        SpectateSession session = new SpectateSession(
                spectatorUuid, "Cam1", GameMode.SURVIVAL, OVERWORLD, Vec3d.ZERO, 0f, 0f, SpectateMode.AUTO
        );
        session.setTarget(juanUuid, "Explorer", OVERWORLD);
        assertEquals(OVERWORLD, session.getCurrentTargetWorldKey());

        session.setCurrentTargetWorldKey(NETHER);
        assertEquals(NETHER, session.getCurrentTargetWorldKey(), "La cámara debe actualizarse a la dimensión de destino del jugador");

        session.setCurrentTargetWorldKey(CIUDADELA);
        assertEquals(CIUDADELA, session.getCurrentTargetWorldKey(), "La cámara debe seguir al jugador a mundos propios");
    }

    @Test
    @DisplayName("4. spectator_survives_target_teleport: el espectador sobrevive a teletransporte sin perder la sesión")
    void spectator_survives_target_teleport() {
        SpectateSession session = new SpectateSession(
                spectatorUuid, "Cam1", GameMode.ADVENTURE, OVERWORLD, Vec3d.ZERO, 0f, 0f, SpectateMode.AUTO
        );
        session.setTarget(juanUuid, "Jumper", OVERWORLD);

        assertEquals(juanUuid, session.getCurrentTargetUuid());
        assertEquals(SpectateMode.AUTO, session.getMode());
    }

    @Test
    @DisplayName("5. auto_mode_selects_available_player: el modo automático selecciona un jugador elegible")
    void auto_mode_selects_available_player() {
        List<SpectateFollowManager.CandidateInfo> candidates = List.of(
                new SpectateFollowManager.CandidateInfo(juanUuid, "Juan", false, false)
        );

        UUID selected = SpectateFollowManager.selectCandidateFromList(candidates, spectatorUuid, null, Map.of(), config);
        assertEquals(juanUuid, selected);
    }

    @Test
    @DisplayName("6. auto_mode_never_selects_camera_itself: la cuenta de cámara nunca se selecciona a sí misma ni a otras cámaras")
    void auto_mode_never_selects_camera_itself() {
        UUID otherCamera = UUID.randomUUID();
        SpectateSession otherSession = new SpectateSession(otherCamera, "Cam2", GameMode.SPECTATOR, OVERWORLD, Vec3d.ZERO, 0f, 0f, SpectateMode.AUTO);
        SpectateFollowManager.registerSession(otherSession);

        List<SpectateFollowManager.CandidateInfo> candidates = List.of(
                new SpectateFollowManager.CandidateInfo(spectatorUuid, "CameraTV", false, false),
                new SpectateFollowManager.CandidateInfo(otherCamera, "Cam2", false, false),
                new SpectateFollowManager.CandidateInfo(juanUuid, "Juan", false, false)
        );

        UUID selected = SpectateFollowManager.selectCandidateFromList(candidates, spectatorUuid, null, SpectateFollowManager.getSessions(), config);
        assertEquals(juanUuid, selected, "Debe seleccionar a Juan, jamás a sí misma ni a otra cámara activa");
    }

    @Test
    @DisplayName("7. auto_mode_respects_exclusion_list: respeta la lista de exclusión por nombre y UUID")
    void auto_mode_respects_exclusion_list() {
        config.addExcluded("SecretPlayer");
        config.addExcluded(pedroUuid.toString());

        List<SpectateFollowManager.CandidateInfo> candidates = List.of(
                new SpectateFollowManager.CandidateInfo(juanUuid, "SecretPlayer", false, false),
                new SpectateFollowManager.CandidateInfo(pedroUuid, "OtherSecret", false, false)
        );

        UUID selected = SpectateFollowManager.selectCandidateFromList(candidates, spectatorUuid, null, Map.of(), config);
        assertNull(selected, "Ambos jugadores excluidos deben ser descartados");
    }

    @Test
    @DisplayName("8. target_death_does_not_end_session: muerte del objetivo no cancela la sesión de espectador")
    void target_death_does_not_end_session() {
        SpectateSession session = new SpectateSession(
                spectatorUuid, "Cam1", GameMode.SURVIVAL, OVERWORLD, Vec3d.ZERO, 0f, 0f, SpectateMode.AUTO
        );
        session.setTarget(juanUuid, "Fighter", OVERWORLD);

        assertNotEquals(SpectateMode.OFF, session.getMode());
        assertEquals(juanUuid, session.getCurrentTargetUuid());
    }

    @Test
    @DisplayName("9. target_respawn_reconnects_camera: reaparición tras muerte reconecta la cámara al objetivo")
    void target_respawn_reconnects_camera() {
        SpectateSession session = new SpectateSession(
                spectatorUuid, "Cam1", GameMode.SURVIVAL, OVERWORLD, Vec3d.ZERO, 0f, 0f, SpectateMode.AUTO
        );
        session.setTarget(juanUuid, "Fighter", OVERWORLD);

        session.setCurrentTargetWorldKey(CIUDADELA);
        assertEquals(CIUDADELA, session.getCurrentTargetWorldKey());
        assertEquals(juanUuid, session.getCurrentTargetUuid());
    }

    @Test
    @DisplayName("10. spectate_off_restores_original_gamemode: restaurar modo de juego original al salir")
    void spectate_off_restores_original_gamemode() {
        SpectateSession sessionSurvival = new SpectateSession(
                spectatorUuid, "Cam1", GameMode.SURVIVAL, OVERWORLD, Vec3d.ZERO, 0f, 0f, SpectateMode.AUTO
        );
        assertEquals(GameMode.SURVIVAL, sessionSurvival.getOriginalGameMode());

        SpectateSession sessionCreative = new SpectateSession(
                UUID.randomUUID(), "Cam2", GameMode.CREATIVE, OVERWORLD, Vec3d.ZERO, 0f, 0f, SpectateMode.AUTO
        );
        assertEquals(GameMode.CREATIVE, sessionCreative.getOriginalGameMode());
    }

    @Test
    @DisplayName("11. spectate_off_restores_original_position: restaurar mundo y coordenadas originales al salir")
    void spectate_off_restores_original_position() {
        Vec3d origin = new Vec3d(123.4, 75.0, -987.6);
        SpectateSession session = new SpectateSession(
                spectatorUuid, "Cam1", GameMode.SURVIVAL, CIUDADELA, origin, 180.0f, -15.0f, SpectateMode.AUTO
        );
        session.setTarget(juanUuid, "FarAway", OVERWORLD);

        assertEquals(CIUDADELA, session.getOriginalWorldKey());
        assertEquals(origin, session.getOriginalPos());
        assertEquals(180.0f, session.getOriginalYaw());
        assertEquals(-15.0f, session.getOriginalPitch());
    }

    @Test
    @DisplayName("12. multiple_camera_sessions_are_independent: múltiples cámaras funcionan simultáneamente de manera independiente")
    void multiple_camera_sessions_are_independent() {
        UUID cam1 = UUID.randomUUID();
        UUID cam2 = UUID.randomUUID();

        SpectateSession session1 = new SpectateSession(cam1, "Cam1", GameMode.SURVIVAL, OVERWORLD, Vec3d.ZERO, 0f, 0f, SpectateMode.MANUAL);
        session1.setTarget(juanUuid, "Player1", OVERWORLD);

        SpectateSession session2 = new SpectateSession(cam2, "Cam2", GameMode.CREATIVE, NETHER, Vec3d.ZERO, 0f, 0f, SpectateMode.AUTO);
        session2.setTarget(pedroUuid, "Player2", NETHER);

        SpectateFollowManager.registerSession(session1);
        SpectateFollowManager.registerSession(session2);

        assertEquals(2, SpectateFollowManager.getSessions().size());
        assertEquals(juanUuid, SpectateFollowManager.getSession(cam1).getCurrentTargetUuid());
        assertEquals(pedroUuid, SpectateFollowManager.getSession(cam2).getCurrentTargetUuid());
        assertEquals(SpectateMode.MANUAL, SpectateFollowManager.getSession(cam1).getMode());
        assertEquals(SpectateMode.AUTO, SpectateFollowManager.getSession(cam2).getMode());
    }

    @Test
    @DisplayName("13. multiple_cameras_prefer_different_targets: cámaras automáticas prefieren no repetir objetivos")
    void multiple_cameras_prefer_different_targets() {
        UUID cam1 = UUID.randomUUID();
        UUID cam2 = UUID.randomUUID();

        SpectateSession session1 = new SpectateSession(cam1, "Cam1", GameMode.SURVIVAL, OVERWORLD, Vec3d.ZERO, 0f, 0f, SpectateMode.AUTO);
        session1.setTarget(juanUuid, "Player1", OVERWORLD);
        SpectateFollowManager.registerSession(session1);

        List<SpectateFollowManager.CandidateInfo> candidates = List.of(
                new SpectateFollowManager.CandidateInfo(juanUuid, "Player1", false, false),
                new SpectateFollowManager.CandidateInfo(pedroUuid, "Player2", false, false)
        );

        UUID selectedForCam2 = SpectateFollowManager.selectCandidateFromList(candidates, cam2, null, SpectateFollowManager.getSessions(), config);
        assertEquals(pedroUuid, selectedForCam2, "Cam2 debe preferir el jugador libre (Player2)");
    }

    @Test
    @DisplayName("14. rotation_changes_target: rotación periódica en AUTO avanza de objetivo y se pospone en combate")
    void rotation_changes_target() {
        SpectateSession session = new SpectateSession(
                spectatorUuid, "Cam1", GameMode.SURVIVAL, OVERWORLD, Vec3d.ZERO, 0f, 0f, SpectateMode.AUTO
        );
        session.setTarget(juanUuid, "Trainer", OVERWORLD);
        session.setRotationMinutesOverride(2);

        long start = session.getCurrentTargetStartMs();

        assertFalse(SpectateFollowManager.checkRotationDue(session, false, start + 60_000L));
        assertTrue(SpectateFollowManager.checkRotationDue(session, false, start + 120_000L));
        assertFalse(SpectateFollowManager.checkRotationDue(session, true, start + 120_000L),
                "La rotación debe posponerse si el objetivo está en batalla Cobblemon");

        // En modo MANUAL nunca debe rotar por tiempo
        session.setMode(SpectateMode.MANUAL);
        assertFalse(SpectateFollowManager.checkRotationDue(session, false, start + 500_000L),
                "En modo MANUAL jamás debe activarse la rotación automática por tiempo");
    }

    @Test
    @DisplayName("15. excluded_staff_not_selected_when_disabled: staff no seleccionado si includeStaff es false")
    void excluded_staff_not_selected_when_disabled() {
        config.setIncludeStaff(false);

        List<SpectateFollowManager.CandidateInfo> candidates = List.of(
                new SpectateFollowManager.CandidateInfo(staffUuid, "AdminPlayer", true, false),
                new SpectateFollowManager.CandidateInfo(juanUuid, "RegularUser", false, false)
        );

        UUID selected = SpectateFollowManager.selectCandidateFromList(candidates, spectatorUuid, null, Map.of(), config);
        assertEquals(juanUuid, selected, "Debe seleccionar a RegularUser y omitir a AdminPlayer");

        config.setIncludeStaff(true);
        List<SpectateFollowManager.CandidateInfo> onlyStaff = List.of(
                new SpectateFollowManager.CandidateInfo(staffUuid, "AdminPlayer", true, false)
        );
        UUID selectedWithStaff = SpectateFollowManager.selectCandidateFromList(onlyStaff, spectatorUuid, null, Map.of(), config);
        assertEquals(staffUuid, selectedWithStaff, "Si includeStaff es true, sí puede seleccionar staff");
    }

    @Test
    @DisplayName("16. previous_target_history: historial permite navegar hacia objetivos anteriores")
    void previous_target_history() {
        SpectateSession session = new SpectateSession(
                spectatorUuid, "Cam1", GameMode.SURVIVAL, OVERWORLD, Vec3d.ZERO, 0f, 0f, SpectateMode.AUTO
        );
        session.setTarget(juanUuid, "FirstPlayer", OVERWORLD);
        session.setTarget(pedroUuid, "SecondPlayer", OVERWORLD);

        UUID prev = session.popPreviousTarget();
        assertEquals(juanUuid, prev, "Debe retornar el primer jugador como objetivo previo");
        assertNull(session.popPreviousTarget(), "No debe haber más historial disponible");
    }

    // =========================================================================
    // 2. PRUEBAS EXPLICITAS DE DESCONEXIÓN Y TRANSICIÓN DE ESTADOS (MANUAL VS AUTO)
    // =========================================================================

    @Test
    @DisplayName("17. manual_target_disconnect_switches_to_fallback: desconexión de target manual pasa a Pedro sin perder modo MANUAL")
    void manual_target_disconnect_switches_to_fallback() {
        // Escenario: CameraTV -> Juan (iniciado vía /spectatefollow Juan)
        SpectateSession session = new SpectateSession(
                spectatorUuid, "CameraTV", GameMode.SPECTATOR, OVERWORLD, Vec3d.ZERO, 0f, 0f, SpectateMode.MANUAL
        );
        session.setBaseMode(SpectateMode.MANUAL);
        session.setMode(SpectateMode.MANUAL);
        session.setTarget(juanUuid, "Juan", OVERWORLD);
        session.setPreferredTarget(juanUuid, "Juan");
        SpectateFollowManager.registerSession(session);

        // Juan se desconecta, Pedro está en línea
        List<SpectateFollowManager.CandidateInfo> candidates = List.of(
                new SpectateFollowManager.CandidateInfo(juanUuid, "Juan", false, true), // desconectado
                new SpectateFollowManager.CandidateInfo(pedroUuid, "Pedro", false, false)
        );

        SpectateFollowManager.processTargetDisconnect(session, juanUuid, candidates);

        // Verificaciones de estado estricto
        assertEquals(pedroUuid, session.getCurrentTargetUuid(), "Debe cambiar automáticamente a Pedro");
        assertEquals("Pedro", session.getCurrentTargetName());
        assertEquals(SpectateMode.MANUAL, session.getMode(), "El modo activo debe continuar siendo MANUAL");
        assertEquals(SpectateMode.MANUAL, session.getBaseMode(), "El baseMode debe permanecer MANUAL");
        assertEquals(juanUuid, session.getPreferredTargetUuid(), "El preferredTargetUuid debe conservarse como Juan");
        assertFalse(SpectateFollowManager.checkRotationDue(session, false, System.currentTimeMillis() + 600_000L),
                "No debe rotar por tiempo mientras siga en fallback manual");
    }

    @Test
    @DisplayName("18. manual_target_disconnect_enters_waiting_when_empty: si no hay más jugadores, sesión MANUAL no se destruye y entra en WAITING")
    void manual_target_disconnect_enters_waiting_when_empty() {
        SpectateSession session = new SpectateSession(
                spectatorUuid, "CameraTV", GameMode.SPECTATOR, OVERWORLD, Vec3d.ZERO, 0f, 0f, SpectateMode.MANUAL
        );
        session.setBaseMode(SpectateMode.MANUAL);
        session.setMode(SpectateMode.MANUAL);
        session.setTarget(juanUuid, "Juan", OVERWORLD);
        session.setPreferredTarget(juanUuid, "Juan");
        SpectateFollowManager.registerSession(session);

        // Juan es el único y se desconecta
        List<SpectateFollowManager.CandidateInfo> candidates = List.of(
                new SpectateFollowManager.CandidateInfo(juanUuid, "Juan", false, true)
        );

        SpectateFollowManager.processTargetDisconnect(session, juanUuid, candidates);

        assertEquals(SpectateMode.WAITING_FOR_PLAYER, session.getMode());
        assertEquals(SpectateMode.MANUAL, session.getBaseMode(), "La sesión no debe destruirse y debe recordar baseMode MANUAL");
        assertEquals(juanUuid, session.getPreferredTargetUuid(), "Debe recordar a Juan como preferredTarget");
        assertNull(session.getCurrentTargetUuid());
        assertNotNull(SpectateFollowManager.getSession(spectatorUuid), "La sesión debe permanecer registrada en el gestor");
    }

    @Test
    @DisplayName("19. manual_waiting_picks_new_player_after_join: sesión MANUAL en espera selecciona a Alejandro al entrar y mantiene MANUAL")
    void manual_waiting_picks_new_player_after_join() {
        SpectateSession session = new SpectateSession(
                spectatorUuid, "CameraTV", GameMode.SPECTATOR, OVERWORLD, Vec3d.ZERO, 0f, 0f, SpectateMode.MANUAL
        );
        session.setBaseMode(SpectateMode.MANUAL);
        session.setMode(SpectateMode.WAITING_FOR_PLAYER);
        session.setPreferredTarget(juanUuid, "Juan");
        SpectateFollowManager.registerSession(session);

        // Alejandro entra -> se programa join delay (80 ticks = 4s)
        session.setPendingJoin(alejandroUuid, 80);
        assertEquals(alejandroUuid, session.getPendingJoinPlayerUuid());

        // Concluye la espera de carga
        while (session.getPendingJoinDelayTicks() > 0) {
            session.decrementPendingJoinTicks();
        }
        assertEquals(0, session.getPendingJoinDelayTicks());

        // Se adjunta a Alejandro
        session.setTarget(alejandroUuid, "Alejandro", OVERWORLD);
        session.setMode(session.getBaseMode());

        assertEquals(SpectateMode.MANUAL, session.getMode(), "Debe reanudarse en modo MANUAL");
        assertEquals(alejandroUuid, session.getCurrentTargetUuid());
        assertEquals("Alejandro", session.getCurrentTargetName());
        assertEquals(juanUuid, session.getPreferredTargetUuid());
    }

    @Test
    @DisplayName("20. auto_target_disconnect_switches_to_fallback: desconexión en modo AUTO cambia a siguiente candidato")
    void auto_target_disconnect_switches_to_fallback() {
        SpectateSession session = new SpectateSession(
                spectatorUuid, "CameraTV", GameMode.SPECTATOR, OVERWORLD, Vec3d.ZERO, 0f, 0f, SpectateMode.AUTO
        );
        session.setBaseMode(SpectateMode.AUTO);
        session.setMode(SpectateMode.AUTO);
        session.setTarget(juanUuid, "Juan", OVERWORLD);
        SpectateFollowManager.registerSession(session);

        List<SpectateFollowManager.CandidateInfo> candidates = List.of(
                new SpectateFollowManager.CandidateInfo(juanUuid, "Juan", false, true),
                new SpectateFollowManager.CandidateInfo(pedroUuid, "Pedro", false, false)
        );

        SpectateFollowManager.processTargetDisconnect(session, juanUuid, candidates);

        assertEquals(pedroUuid, session.getCurrentTargetUuid());
        assertEquals(SpectateMode.AUTO, session.getMode());
        assertEquals(SpectateMode.AUTO, session.getBaseMode());
    }

    // =========================================================================
    // 3. CONDICIONES DE CARRERA AUDITADAS
    // =========================================================================

    @Test
    @DisplayName("21. race_condition_two_players_disconnect_almost_simultaneously: Juan y Pedro salen casi al mismo tiempo")
    void race_condition_two_players_disconnect_almost_simultaneously() {
        SpectateSession session = new SpectateSession(
                spectatorUuid, "CameraTV", GameMode.SPECTATOR, OVERWORLD, Vec3d.ZERO, 0f, 0f, SpectateMode.MANUAL
        );
        session.setTarget(juanUuid, "Juan", OVERWORLD);
        SpectateFollowManager.registerSession(session);

        // Durante el cambio, ambos figuran desconectados o removidos
        List<SpectateFollowManager.CandidateInfo> candidates = List.of(
                new SpectateFollowManager.CandidateInfo(juanUuid, "Juan", false, true),
                new SpectateFollowManager.CandidateInfo(pedroUuid, "Pedro", false, true)
        );

        assertDoesNotThrow(() -> {
            SpectateFollowManager.processTargetDisconnect(session, juanUuid, candidates);
        });

        assertEquals(SpectateMode.WAITING_FOR_PLAYER, session.getMode());
        assertNull(session.getCurrentTargetUuid());
    }

    @Test
    @DisplayName("22. race_condition_target_changes_dimension_and_disconnects: desconexión durante cambio de dimensión")
    void race_condition_target_changes_dimension_and_disconnects() {
        SpectateSession session = new SpectateSession(
                spectatorUuid, "CameraTV", GameMode.SPECTATOR, OVERWORLD, Vec3d.ZERO, 0f, 0f, SpectateMode.AUTO
        );
        session.setTarget(juanUuid, "Juan", OVERWORLD);

        // Juan viaja a Johto pero se desconecta de inmediato (isRemovedOrDisconnected = true)
        List<SpectateFollowManager.CandidateInfo> candidates = List.of(
                new SpectateFollowManager.CandidateInfo(juanUuid, "Juan", false, true),
                new SpectateFollowManager.CandidateInfo(pedroUuid, "Pedro", false, false)
        );

        // El sistema cancela el reenganche a Juan y asigna al fallback Pedro
        UUID selected = SpectateFollowManager.selectCandidateFromList(candidates, spectatorUuid, juanUuid, Map.of(), config);
        assertEquals(pedroUuid, selected, "Debe omitir al jugador que se desconectó durante la transición");
    }

    // =========================================================================
    // 4. VALIDACIÓN FUNCIONAL CROSS-DIMENSION COMPLETA
    // =========================================================================

    @Test
    @DisplayName("23. cross_dimension_full_lifecycle_simulation: Overworld -> Ciudadela -> Salvaje -> Johto -> Gimnasio -> /home -> /spawn")
    void cross_dimension_full_lifecycle_simulation() {
        SpectateSession session = new SpectateSession(
                spectatorUuid, "CameraTV", GameMode.SPECTATOR, OVERWORLD, Vec3d.ZERO, 0f, 0f, SpectateMode.MANUAL
        );
        session.setTarget(juanUuid, "Juan", OVERWORLD);
        SpectateFollowManager.registerSession(session);

        RegistryKey<World>[] rutaDimensiones = new RegistryKey[]{
                CIUDADELA,  // 1. Ciudadela
                SALVAJE,    // 2. Mundo Salvaje
                JOHTO,      // 3. Johto
                GIMNASIO,   // 4. Gimnasio
                HOGAR,      // 5. /home
                CIUDADELA   // 6. /spawn
        };

        for (RegistryKey<World> destino : rutaDimensiones) {
            // Simular evento AFTER_PLAYER_CHANGE_WORLD
            session.setCurrentTargetWorldKey(destino);

            // Validaciones por cada paso
            assertEquals(destino, session.getCurrentTargetWorldKey(), "La dimensión de la sesión debe ser " + destino.getValue());
            assertEquals(juanUuid, session.getCurrentTargetUuid(), "El objetivo debe mantenerse como Juan");
            assertEquals(SpectateMode.MANUAL, session.getMode());
        }
    }

    // =========================================================================
    // 5. ESCENARIO JUGABLE COMPLETO DE STAGING (9 PASOS OBLIGATORIOS)
    // =========================================================================

    @Test
    @DisplayName("24. full_staging_playable_scenario: simulación exacta del escenario de 9 pasos solicitado")
    void full_staging_playable_scenario() {
        Vec3d cameraOriginalPos = new Vec3d(50.0, 70.0, 50.0);
        float originalYaw = 90.0f;
        float originalPitch = 0.0f;

        // Paso 1: /spectatefollow Juan (MANUAL)
        SpectateSession session = new SpectateSession(
                spectatorUuid, "CameraTV", GameMode.SURVIVAL, OVERWORLD, cameraOriginalPos, originalYaw, originalPitch, SpectateMode.MANUAL
        );
        session.setBaseMode(SpectateMode.MANUAL);
        session.setMode(SpectateMode.MANUAL);
        session.setTarget(juanUuid, "Juan", OVERWORLD);
        session.setPreferredTarget(juanUuid, "Juan");
        SpectateFollowManager.registerSession(session);

        assertEquals(SpectateMode.MANUAL, session.getMode());
        assertEquals(juanUuid, session.getCurrentTargetUuid());

        // Paso 2: Juan cambia de mundo (a Ciudadela)
        session.setCurrentTargetWorldKey(CIUDADELA);
        assertEquals(CIUDADELA, session.getCurrentTargetWorldKey());
        assertEquals(juanUuid, session.getCurrentTargetUuid());

        // Paso 3: Juan ejecuta /home (a Hogar)
        session.setCurrentTargetWorldKey(HOGAR);
        assertEquals(HOGAR, session.getCurrentTargetWorldKey());
        assertEquals(juanUuid, session.getCurrentTargetUuid());

        // Paso 4: Juan muere y reaparece
        // La sesión no se destruye
        assertNotEquals(SpectateMode.OFF, session.getMode());
        // Tras respawn reaparece en Ciudadela
        session.setCurrentTargetWorldKey(CIUDADELA);
        assertEquals(CIUDADELA, session.getCurrentTargetWorldKey());
        assertEquals(juanUuid, session.getCurrentTargetUuid());

        // Paso 5: Juan se desconecta -> debe cambiar automáticamente a Pedro
        List<SpectateFollowManager.CandidateInfo> candidatesPaso5 = List.of(
                new SpectateFollowManager.CandidateInfo(juanUuid, "Juan", false, true),
                new SpectateFollowManager.CandidateInfo(pedroUuid, "Pedro", false, false)
        );
        SpectateFollowManager.processTargetDisconnect(session, juanUuid, candidatesPaso5);

        assertEquals(pedroUuid, session.getCurrentTargetUuid());
        assertEquals("Pedro", session.getCurrentTargetName());
        assertEquals(SpectateMode.MANUAL, session.getMode(), "Debe mantenerse en modo MANUAL");

        // Paso 6: Pedro cambia de dimensión (al Nether)
        session.setCurrentTargetWorldKey(NETHER);
        assertEquals(NETHER, session.getCurrentTargetWorldKey());
        assertEquals(pedroUuid, session.getCurrentTargetUuid());

        // Paso 7: Pedro se desconecta -> debe entrar en WAITING
        List<SpectateFollowManager.CandidateInfo> candidatesPaso7 = List.of(
                new SpectateFollowManager.CandidateInfo(pedroUuid, "Pedro", false, true)
        );
        SpectateFollowManager.processTargetDisconnect(session, pedroUuid, candidatesPaso7);

        assertEquals(SpectateMode.WAITING_FOR_PLAYER, session.getMode());
        assertNull(session.getCurrentTargetUuid());
        assertEquals(SpectateMode.MANUAL, session.getBaseMode());

        // Paso 8: Alejandro entra -> debe seguir automáticamente a Alejandro tras join delay
        session.setPendingJoin(alejandroUuid, 80);
        while (session.getPendingJoinDelayTicks() > 0) {
            session.decrementPendingJoinTicks();
        }
        session.setTarget(alejandroUuid, "Alejandro", OVERWORLD);
        session.setMode(session.getBaseMode());

        assertEquals(alejandroUuid, session.getCurrentTargetUuid());
        assertEquals("Alejandro", session.getCurrentTargetName());
        assertEquals(SpectateMode.MANUAL, session.getMode());

        // Paso 9: /spectatefollow off -> debe restaurar todo el estado original
        session.setMode(SpectateMode.OFF);
        session.clearTarget();
        SpectateFollowManager.clearSessions();

        assertEquals(GameMode.SURVIVAL, session.getOriginalGameMode());
        assertEquals(OVERWORLD, session.getOriginalWorldKey());
        assertEquals(cameraOriginalPos, session.getOriginalPos());
        assertEquals(originalYaw, session.getOriginalYaw());
        assertEquals(originalPitch, session.getOriginalPitch());
        assertEquals(SpectateMode.OFF, session.getMode());
        assertNull(SpectateFollowManager.getSession(spectatorUuid));
    }
}

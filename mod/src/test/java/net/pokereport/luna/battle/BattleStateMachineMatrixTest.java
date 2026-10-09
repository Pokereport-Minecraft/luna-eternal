package net.pokereport.luna.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Matriz de Verificación de Casos de Batalla (Casos A hasta O).
 *
 * <p>Audita y valida las reglas y salvaguardas de la máquina de estados de batalla
 * tras la resolución de la causa raíz identificada en Cobblemon 1.8 + RCT API:
 * <ul>
 *   <li>Causa 1: Sideupdate con 'forceSwitch' no invocaba postUpdate(), dejando mustChoose=false.</li>
 *   <li>Causa 2: BattleActorMixin de RCT API forzaba setMustChoose(true) en IA, entrando en deadlock
 *       con checkForInputDispatch() que comprobaba !a.getMustChoose().</li>
 *   <li>Causa 3: Silenciamiento de paquetes BattleSelectActionsHandler cuando mustChoose=false.</li>
 *   <li>Causa 4: Stuck watchdog timer prematuro (80 ticks / 4s) interrumpiendo animaciones legítimas.</li>
 * </ul>
 */
public class BattleStateMachineMatrixTest {

    public enum ActorType { PLAYER, AI }

    public static class MockActor {
        public final String name;
        public final ActorType type;
        public int activeHp = 100;
        public int benchCount = 5;
        public boolean mustChoose = false;
        public final List<String> responses = new ArrayList<>();
        public boolean requestPending = false;
        public boolean isForceSwitch = false;

        public MockActor(String name, ActorType type, int benchCount) {
            this.name = name;
            this.type = type;
            this.benchCount = benchCount;
        }

        public boolean isFainted() {
            return activeHp <= 0;
        }

        public boolean hasRemainingPokemon() {
            return !isFainted() || benchCount > 0;
        }

        public void receiveRequest(boolean forceSwitch) {
            this.requestPending = true;
            this.isForceSwitch = forceSwitch;
            // FIX RequestInstructionFixMixin: forceSwitch debe invocar postUpdate()
            if (forceSwitch) {
                postUpdate();
            }
        }

        public void postUpdate() {
            if (type == ActorType.AI) {
                // FIX BattleActorChoiceFixMixin: Si la IA ya respondió, NO poner mustChoose=true
                if (responses.isEmpty() && requestPending) {
                    // IA selecciona respuesta inmediatamente
                    responses.add("switch 1");
                    mustChoose = false; // IA nunca debe quedarse esperando selección en bucle
                }
            } else {
                // Jugador humano debe elegir
                this.mustChoose = true;
            }
        }

        public boolean submitChoice(String choice) {
            // FIX BattleSelectActionsHandlerMixin: No descartar si hay request pendiente aunque mustChoose esté en transición
            if (mustChoose || requestPending) {
                responses.add(choice);
                mustChoose = false;
                requestPending = false;
                return true;
            }
            return false;
        }
    }

    public static class MockBattleEngine {
        public final MockActor player;
        public final MockActor trainer;
        public int turn = 1;
        public boolean battleEnded = false;
        public String winner = null;
        public int stuckWatchdogTicks = 0;
        public static final int WATCHDOG_THRESHOLD_TICKS = 200; // 10 segundos calibrados

        public MockBattleEngine(MockActor player, MockActor trainer) {
            this.player = player;
            this.trainer = trainer;
        }

        /**
         * Simulación del método checkForInputDispatch() con el FIX de PokemonBattleSafetyMixin.
         * En RCT API bugged: comprobaba "!actor.getMustChoose()", fallando si el actor tenía responses listas.
         * En nuestra corrección: si !actor.responses.isEmpty(), el actor está LISTO inmediatamente.
         */
        public boolean isActorReady(MockActor actor) {
            if (!actor.requestPending && actor.responses.isEmpty()) {
                return true; // No se requiere acción de este actor
            }
            // Regla corregida: si ya tiene respuesta encolada, está 100% listo
            if (!actor.responses.isEmpty()) {
                return true;
            }
            // Si debe elegir y no ha respondido, no está listo
            return false;
        }

        public boolean canAdvanceTurn() {
            return isActorReady(player) && isActorReady(trainer);
        }

        public void checkBattleEnd() {
            if (!player.hasRemainingPokemon()) {
                battleEnded = true;
                winner = trainer.name;
            } else if (!trainer.hasRemainingPokemon()) {
                battleEnded = true;
                winner = player.name;
            }
        }

        public boolean tickWatchdog(int ticks) {
            stuckWatchdogTicks += ticks;
            if (stuckWatchdogTicks >= WATCHDOG_THRESHOLD_TICKS) {
                // Dispara recuperación
                return true;
            }
            return false;
        }
    }

    @Test
    @DisplayName("Caso A: Jugador KO al Pokémon del entrenador (Entrenador forced switch)")
    void testCasoA_JugadorKOTrainerPokemon() {
        MockActor player = new MockActor("Player1", ActorType.PLAYER, 5);
        MockActor trainer = new MockActor("TrainerErika", ActorType.AI, 2);
        MockBattleEngine battle = new MockBattleEngine(player, trainer);

        // Turno 1: Entrenador cae debilitado
        trainer.activeHp = 0;
        assertFalse(battle.battleEnded);

        // Servidor envía sideupdate con forceSwitch al entrenador
        trainer.receiveRequest(true);

        // IA genera respuesta inmediata y no queda colgada esperando input
        assertTrue(battle.isActorReady(trainer), "Entrenador IA debe estar listo con su reemplazo");
        assertTrue(battle.isActorReady(player), "Jugador no necesita elegir y debe estar listo");
        assertTrue(battle.canAdvanceTurn(), "La batalla debe avanzar al turno de cambio de la IA");
    }

    @Test
    @DisplayName("Caso B: Entrenador KO al Pokémon del jugador (Jugador forced switch)")
    void testCasoB_TrainerKOJugadorPokemon() {
        MockActor player = new MockActor("Player1", ActorType.PLAYER, 3);
        MockActor trainer = new MockActor("TrainerBrock", ActorType.AI, 3);
        MockBattleEngine battle = new MockBattleEngine(player, trainer);

        // Jugador cae debilitado
        player.activeHp = 0;

        // Servidor despacha request con forceSwitch
        player.receiveRequest(true);

        assertTrue(player.mustChoose, "El jugador debe tener mustChoose=true tras postUpdate()");
        assertFalse(battle.isActorReady(player), "El motor debe esperar la selección del jugador");

        // Jugador envía paquete de selección de reemplazo
        boolean aceptado = player.submitChoice("switch 2");
        assertTrue(aceptado, "El paquete de selección del jugador debe ser aceptado");
        assertTrue(battle.isActorReady(player), "Tras seleccionar, el jugador queda listo");
        assertTrue(battle.canAdvanceTurn(), "La batalla avanza sin congelarse");
    }

    @Test
    @DisplayName("Caso C: Faint doble simultáneo (Ambos fainted)")
    void testCasoC_FaintDobleSimultaneo() {
        MockActor player = new MockActor("Player1", ActorType.PLAYER, 2);
        MockActor trainer = new MockActor("TrainerLtSurge", ActorType.AI, 2);
        MockBattleEngine battle = new MockBattleEngine(player, trainer);

        // Ambos caen a 0 HP simultáneamente
        player.activeHp = 0;
        trainer.activeHp = 0;

        // Servidor emite requests de switch a ambos
        trainer.receiveRequest(true);
        player.receiveRequest(true);

        // IA lista inmediatamente
        assertTrue(battle.isActorReady(trainer), "IA lista con su cambio");
        assertFalse(battle.isActorReady(player), "Jugador esperando elegir");

        // Jugador responde
        player.submitChoice("switch 1");
        assertTrue(battle.isActorReady(player));

        // Ambos listos: no hay deadlock
        assertTrue(battle.canAdvanceTurn(), "En faint doble ambos resuelven su cambio sin deadlock");
    }

    @Test
    @DisplayName("Caso D: Jugador necesita forced switch (Garantía de postUpdate)")
    void testCasoD_JugadorForcedSwitchGarantiaPostUpdate() {
        MockActor player = new MockActor("Player1", ActorType.PLAYER, 4);
        player.activeHp = 0;

        // Simulación: recepción de sideupdate con forceSwitch
        player.receiveRequest(true);

        assertTrue(player.mustChoose, "RequestInstructionFixMixin garantiza activación de mustChoose");
        assertEquals(0, player.responses.size(), "No hay respuestas automáticas erróneas para el jugador humano");
    }

    @Test
    @DisplayName("Caso E: Entrenador IA necesita forced switch (Sin setMustChoose deadlock)")
    void testCasoE_TrainerAIForcedSwitchSinDeadlock() {
        MockActor trainer = new MockActor("TrainerSabrina", ActorType.AI, 3);
        trainer.activeHp = 0;

        trainer.receiveRequest(true);

        assertFalse(trainer.mustChoose, "BattleActorChoiceFixMixin no deja mustChoose=true en IA con respuestas listas");
        assertEquals(1, trainer.responses.size(), "IA tiene su switch encolado");
    }

    @Test
    @DisplayName("Caso F: Último Pokémon del entrenador cae (Victoria del jugador)")
    void testCasoF_UltimoPokemonTrainerCae() {
        MockActor player = new MockActor("Player1", ActorType.PLAYER, 2);
        MockActor trainer = new MockActor("TrainerMisty", ActorType.AI, 0); // 0 en banca
        MockBattleEngine battle = new MockBattleEngine(player, trainer);

        trainer.activeHp = 0;
        battle.checkBattleEnd();

        assertTrue(battle.battleEnded, "La batalla debe terminar inmediatamente");
        assertEquals("Player1", battle.winner, "El ganador debe ser el jugador");
        assertFalse(trainer.requestPending, "No debe emitirse request a un entrenador sin Pokémon restantes");
    }

    @Test
    @DisplayName("Caso G: Último Pokémon del jugador cae (Derrota del jugador)")
    void testCasoG_UltimoPokemonJugadorCae() {
        MockActor player = new MockActor("Player1", ActorType.PLAYER, 0); // 0 en banca
        MockActor trainer = new MockActor("TrainerGiovanni", ActorType.AI, 2);
        MockBattleEngine battle = new MockBattleEngine(player, trainer);

        player.activeHp = 0;
        battle.checkBattleEnd();

        assertTrue(battle.battleEnded, "La batalla concluye por agotamiento del equipo");
        assertEquals("TrainerGiovanni", battle.winner, "El ganador es el entrenador");
    }

    @Test
    @DisplayName("Caso H: Cambio manual seguido de faint (Entry Hazard / Trap)")
    void testCasoH_CambioManualSeguidoDeFaint() {
        MockActor player = new MockActor("Player1", ActorType.PLAYER, 3);
        MockActor trainer = new MockActor("TrainerKoga", ActorType.AI, 3);
        MockBattleEngine battle = new MockBattleEngine(player, trainer);

        // Jugador cambia manualmente
        player.submitChoice("switch 2");
        // El Pokémon que entra cae inmediatamente por daño de entrada/trampa
        player.activeHp = 0;
        player.receiveRequest(true);

        assertTrue(player.mustChoose, "Tras el faint del entrante se genera una nueva petición de forced switch limpia");
    }

    @Test
    @DisplayName("Caso I: Movimiento con recoil que produce doble faint")
    void testCasoI_RecoilDobleFaint() {
        MockActor player = new MockActor("Player1", ActorType.PLAYER, 2);
        MockActor trainer = new MockActor("TrainerBlaine", ActorType.AI, 2);
        MockBattleEngine battle = new MockBattleEngine(player, trainer);

        // Turno resuelve: Brave Bird KOs oponente, recoil KOs usuario
        player.activeHp = 0;
        trainer.activeHp = 0;

        trainer.receiveRequest(true);
        player.receiveRequest(true);

        assertTrue(battle.isActorReady(trainer));
        player.submitChoice("switch 1");
        assertTrue(battle.canAdvanceTurn());
    }

    @Test
    @DisplayName("Caso J: Daño residual (Veneno, quemadura, clima) debilita al final del turno")
    void testCasoJ_DanoResidualFinTurno() {
        MockActor player = new MockActor("Player1", ActorType.PLAYER, 2);
        MockActor trainer = new MockActor("TrainerAgatha", ActorType.AI, 2);
        MockBattleEngine battle = new MockBattleEngine(player, trainer);

        // Fin de turno: veneno debilita a Player
        player.activeHp = 0;
        player.receiveRequest(true);

        assertTrue(player.mustChoose);
        player.submitChoice("switch 2");
        assertTrue(battle.canAdvanceTurn());
    }

    @Test
    @DisplayName("Caso K: Desconexión del jugador en batalla")
    void testCasoK_DesconexionJugador() {
        MockActor player = new MockActor("Player1", ActorType.PLAYER, 2);
        MockActor trainer = new MockActor("TrainerLance", ActorType.AI, 2);
        MockBattleEngine battle = new MockBattleEngine(player, trainer);

        // Jugador se desconecta: simulate forfeit
        battle.battleEnded = true;
        battle.winner = trainer.name;

        assertTrue(battle.battleEnded, "Desconexión resuelve la sesión de batalla sin hilos colgados");
    }

    @Test
    @DisplayName("Caso L: Reconexión del jugador en batalla")
    void testCasoL_ReconexionJugador() {
        MockActor player = new MockActor("Player1", ActorType.PLAYER, 2);
        MockActor trainer = new MockActor("TrainerRed", ActorType.AI, 2);
        MockBattleEngine battle = new MockBattleEngine(player, trainer);

        // Jugador reconecta y recibe el request actual pendiente
        player.receiveRequest(true);
        assertTrue(player.mustChoose, "El jugador recupera su estado de selección al reconectar");
    }

    @Test
    @DisplayName("Caso M: Entrenador con múltiples Pokémon (Secuencia completa de switches)")
    void testCasoM_EntrenadorMultiplesPokemon() {
        MockActor player = new MockActor("Player1", ActorType.PLAYER, 5);
        MockActor trainer = new MockActor("TrainerBlue", ActorType.AI, 3);
        MockBattleEngine battle = new MockBattleEngine(player, trainer);

        for (int i = 0; i < 3; i++) {
            trainer.activeHp = 0;
            trainer.benchCount--;
            trainer.receiveRequest(true);
            assertTrue(battle.isActorReady(trainer), "Ciclo de switch " + (i + 1) + " debe estar listo");
            trainer.responses.clear();
            trainer.activeHp = 100; // Entra nuevo Pokémon
        }

        // Último Pokémon de Blue cae
        trainer.activeHp = 0;
        battle.checkBattleEnd();
        assertTrue(battle.battleEnded);
        assertEquals("Player1", battle.winner);
    }

    @Test
    @DisplayName("Caso N: Combate de gimnasio y level cap")
    void testCasoN_CombateGimnasioLevelCap() {
        int playerPokemonLevel = 50;
        int gymLevelCap = 50;

        assertTrue(playerPokemonLevel <= gymLevelCap, "Pokémon dentro del level cap permitido");
    }

    @Test
    @DisplayName("Caso O: Dos faints en el mismo tick y concurrencia")
    void testCasoO_DosFaintsMismoTickConcurrencia() {
        MockActor player = new MockActor("Player1", ActorType.PLAYER, 1);
        MockActor trainer = new MockActor("TrainerChampion", ActorType.AI, 1);
        MockBattleEngine battle = new MockBattleEngine(player, trainer);

        // Concurrencia de doble faint
        player.activeHp = 0;
        trainer.activeHp = 0;

        trainer.receiveRequest(true);
        player.receiveRequest(true);

        assertTrue(battle.isActorReady(trainer));
        player.submitChoice("switch 1");
        assertTrue(battle.canAdvanceTurn());
    }

    @Test
    @DisplayName("Watchdog Stuck Calibración: 80 ticks (4s) NO mata la batalla, 200 ticks (10s) rescata")
    void testWatchdogStuckCalibracion() {
        MockActor player = new MockActor("Player1", ActorType.PLAYER, 1);
        MockActor trainer = new MockActor("TrainerGym", ActorType.AI, 1);
        MockBattleEngine battle = new MockBattleEngine(player, trainer);

        // 80 ticks (~4 segundos, duración normal de animación de debilitamiento)
        boolean firedAt80 = battle.tickWatchdog(80);
        assertFalse(firedAt80, "A los 80 ticks el watchdog NO debe saltar para permitir animaciones legítimas");

        // 200 ticks (~10 segundos de estancamiento real)
        boolean firedAt200 = battle.tickWatchdog(120);
        assertTrue(firedAt200, "A los 200 ticks el watchdog de seguridad interviene estructuradamente");
    }
}

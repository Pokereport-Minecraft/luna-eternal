package net.pokereport.luna.spectate;

/**
 * Modos de operación para una sesión de cámara de espectador.
 */
public enum SpectateMode {
    /**
     * Modo manual: siguiendo a un jugador específico seleccionado explícitamente.
     */
    MANUAL,

    /**
     * Modo automático: seleccionando jugadores elegibles y rotando según configuración.
     */
    AUTO,

    /**
     * Modo en espera: no hay jugadores válidos en línea; se reanudará automáticamente al entrar uno.
     */
    WAITING_FOR_PLAYER,

    /**
     * Sesión inactiva o finalizada.
     */
    OFF
}

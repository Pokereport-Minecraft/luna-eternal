package net.pokereport.luna.battle;

/** Per-actor recovery; retained requests never outlive their battle actor. */
public interface BattleChoiceRecovery {
    void pokeReport$rememberSubmittedRequest();
    boolean pokeReport$recoverRejectedChoice(String error);
}

package net.pokereport.luna.battle;

/** Observe progress without releasing barriers or removing battle operations. */
public final class BattleQueueWatchdog {
    public enum Result { OK, WARN, ABORT }
    private Object barrier;
    private Object head;
    private int turn;
    private int stalledTicks;

    public Result observe(Object currentBarrier, Object currentHead, int currentTurn,
                          boolean blocked, boolean waitingForPlayer) {
        if (!blocked || waitingForPlayer || barrier != currentBarrier
                || head != currentHead || turn != currentTurn) {
            stalledTicks = 0;
            barrier = currentBarrier;
            head = currentHead;
            turn = currentTurn;
            return Result.OK;
        }
        stalledTicks++;
        if (stalledTicks >= 1200) return Result.ABORT;
        return stalledTicks == 200 || stalledTicks == 600 ? Result.WARN : Result.OK;
    }
}

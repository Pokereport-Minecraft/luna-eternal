package net.pokereport.luna.battle;

import java.util.function.Consumer;

/** JVM-fatal conditions are deliberately allowed through; recoverable battle failures are isolated. */
public final class BattleFailureBoundary {
    private BattleFailureBoundary() {}
    public static void run(Runnable operation, Consumer<Throwable> failure) {
        try {
            operation.run();
        } catch (RuntimeException | LinkageError | StackOverflowError error) {
            failure.accept(error);
        }
    }
}

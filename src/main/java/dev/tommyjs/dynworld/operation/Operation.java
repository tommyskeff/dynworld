package dev.tommyjs.dynworld.operation;

public interface Operation {

    boolean execute(long maxTimeMillis);

    default void execute() {
        execute(0);
    }

}

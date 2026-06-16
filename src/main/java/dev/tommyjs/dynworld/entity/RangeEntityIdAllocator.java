package dev.tommyjs.dynworld.entity;

final class RangeEntityIdAllocator implements EntityIdAllocator {

    private final int min;
    private final int max;
    private int next;

    RangeEntityIdAllocator(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min " + min + " > max " + max);
        }
        this.min = min;
        this.max = max;
        this.next = min;
    }

    @Override
    public synchronized int acquire() {
        if (next > max) {
            throw new IllegalStateException("entity id range [" + min + ", " + max + "] exhausted");
        }
        return next++;
    }

    @Override
    public synchronized void release(int id) {
        if (id == next - 1) {
            next--;
        }
    }

}

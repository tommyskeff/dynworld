package dev.tommyjs.dynworld.entity;

public interface EntityIdAllocator {

    static EntityIdAllocator range(int min, int max) {
        return new RangeEntityIdAllocator(min, max);
    }

    int acquire();

    void release(int id);

}

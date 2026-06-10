package dev.tommyjs.dynworld.region;

import org.jetbrains.annotations.NotNull;

public final class PasteOptions {

    private boolean ignoreAir = false;
    private boolean refresh = true;

    public static @NotNull PasteOptions create() {
        return new PasteOptions();
    }

    public boolean ignoreAir() {
        return ignoreAir;
    }

    public @NotNull PasteOptions ignoreAir(boolean ignoreAir) {
        this.ignoreAir = ignoreAir;
        return this;
    }

    public boolean refresh() {
        return refresh;
    }

    public @NotNull PasteOptions refresh(boolean refresh) {
        this.refresh = refresh;
        return this;
    }

}

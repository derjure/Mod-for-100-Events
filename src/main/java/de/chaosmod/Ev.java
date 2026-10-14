package de.chaosmod;

import java.util.function.Consumer;

public final class Ev {
    public final int id;
    public final String name;
    public boolean positive, permanent;
    public int[] fx = {};
    public int fxMs = 0;
    public Consumer<Run> start = r -> {}, tick = r -> {}, end = r -> {};

    public Ev(int id, String name) { this.id = id; this.name = name; }
    public Ev pos() { positive = true; return this; }
    public Ev perm() { permanent = true; return this; }
    public Ev fx(int... f) { fx = f; return this; }
    public Ev ms(int m) { fxMs = m; return this; }
    public Ev start(Consumer<Run> c) { start = c; return this; }
    public Ev tick(Consumer<Run> c) { tick = c; return this; }
    public Ev end(Consumer<Run> c) { end = c; return this; }
    public String label() { return (positive ? "✨ " : "") + (permanent ? "🔒 " : "") + name; }
}

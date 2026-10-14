package de.chaosmod;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/** Eine laufende Ereignis-Instanz für genau einen Spieler. */
public final class Run {
    public final Ev ev;
    public final ServerPlayer p;
    public final ServerLevel level;
    public final MinecraftServer server;
    public final boolean first;
    public final long durMs;
    public final Map<BlockPos, BlockState> restore = new HashMap<>();
    public int n, a, b, count;
    public double lx, ly, lz, dx, dy, dz, sx, sy, sz;
    public float syaw, spitch;
    public boolean failed, ok, prev;
    public String answer;
    public Object obj;

    public Run(Ev ev, ServerPlayer p, MinecraftServer s, boolean first, long durMs) {
        this.ev = ev; this.p = p; this.server = s; this.first = first; this.durMs = durMs;
        this.level = (ServerLevel) p.level();
        lx = sx = p.getX(); ly = sy = p.getY(); lz = sz = p.getZ();
        syaw = p.getYRot(); spitch = p.getXRot();
    }

    public int sec() { return (int) Math.ceil(durMs / 1000.0) + 1; }
}

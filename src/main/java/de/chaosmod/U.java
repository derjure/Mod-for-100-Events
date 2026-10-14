package de.chaosmod;

import java.util.Locale;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Hilfsmethoden. Vieles läuft über Vanilla-Befehle, das ist am stabilsten gegen API-Änderungen. */
final class U {
    static final Random R = new Random();
    private U() {}

    static ServerLevel lv(ServerPlayer p) { return (ServerLevel) p.level(); }

    static void cmd(MinecraftServer s, String c) {
        try {
            s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
        } catch (Throwable t) {
            ChaosMod.LOG.warn("Befehl fehlgeschlagen: {}", c, t);
        }
    }

    /** "execute as <spieler> at @s run <c>" */
    static void run(ServerPlayer p, String c) { cmd(lv(p).getServer(), "execute as " + p.getUUID() + " at @s run " + c); }

    /** "execute as <spieler> at @s <rest>" (rest beginnt mit einem execute-Unterbefehl, z.B. "rotated ~ 0 ... run ...") */
    static void exec(ServerPlayer p, String rest) { cmd(lv(p).getServer(), "execute as " + p.getUUID() + " at @s " + rest); }

    static void eff(ServerPlayer p, String id, int sec, int amp) {
        run(p, "effect give @s minecraft:" + id + " " + Math.max(1, sec) + " " + amp + " true");
    }

    static void summon(ServerPlayer p, String type, String nbt, double left, double fwd) {
        exec(p, String.format(Locale.ROOT,
                "rotated ~ 0 positioned ^%.1f ^ ^%.1f positioned over motion_blocking_no_leaves run summon minecraft:%s ~ ~ ~ %s",
                left, fwd, type, nbt));
    }

    static String owner(ServerPlayer p) {
        long m = p.getUUID().getMostSignificantBits(), l = p.getUUID().getLeastSignificantBits();
        return String.format(Locale.ROOT, "[I;%d,%d,%d,%d]", (int) (m >> 32), (int) m, (int) (l >> 32), (int) l);
    }

    static void launch(ServerPlayer p, double x, double y, double z) {
        p.setDeltaMovement(x, y, z);
        p.connection.send(new ClientboundSetEntityMotionPacket(p));
    }

    static void msg(ServerPlayer p, String s) { p.sendSystemMessage(Component.literal(s)); }

    static void title(ServerPlayer p, String t, String sub, int sec) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(5, sec * 20, 10));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(sub)));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(t)));
    }

    static <T extends Entity> T spawn(ServerLevel l, EntityType<T> t, double x, double y, double z) {
        T e = t.create(l, EntitySpawnReason.EVENT);
        if (e == null) return null;
        e.snapTo(x, y, z, R.nextFloat() * 360f, 0f);
        l.addFreshEntity(e);
        return e;
    }

    static void name(Entity e, String n) {
        e.setCustomName(Component.literal(n));
        e.setCustomNameVisible(true);
    }

    static void dropItem(ServerPlayer p, ItemStack s) {
        if (s.isEmpty()) return;
        ItemEntity ie = new ItemEntity(lv(p), p.getX(), p.getY() + 0.5, p.getZ(), s);
        ie.setDefaultPickUpDelay();
        lv(p).addFreshEntity(ie);
    }

    /** Block setzen und den alten Zustand merken (wird am Ende des Ereignisses zurückgesetzt). */
    static void set(Run r, BlockPos pos, BlockState st) {
        BlockPos ip = pos.immutable();
        if (r.level.getBlockEntity(ip) != null) return;
        r.restore.putIfAbsent(ip, r.level.getBlockState(ip));
        r.level.setBlockAndUpdate(ip, st);
    }

    static void restore(Run r) {
        for (Map.Entry<BlockPos, BlockState> en : r.restore.entrySet()) r.level.setBlockAndUpdate(en.getKey(), en.getValue());
        r.restore.clear();
    }

    static Vec3 v(double x, double y, double z) { return new Vec3(x, y, z); }
}

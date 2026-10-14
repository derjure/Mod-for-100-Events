package de.chaosmod;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChaosMod implements ModInitializer {
    public static final String ID = "chaosmod";
    public static final Logger LOG = LoggerFactory.getLogger(ID);

    /** Alle 20 Sekunden: 10 s warten (grün, zeigt das nächste Ereignis), 10 s laufen (rot). */
    static final long WAIT = 10_000, RUN = 10_000;
    static final Random RND = new Random();
    static final List<Run> runs = new ArrayList<>();
    static final Map<UUID, ServerBossEvent> bars = new HashMap<>();
    static final Map<UUID, ServerPlayer> barOwner = new HashMap<>();
    static final Map<UUID, ArrayDeque<Vec3>> history = new HashMap<>();
    static final Map<UUID, Integer> lastDist = new HashMap<>();
    static final Map<UUID, String> trend = new HashMap<>();
    static boolean running, bday;
    static long phaseStart, runLen = RUN, lastSample;
    static Ev next, current, last;
    static int nextMult = 1;
    static BlockPos journeyPos;

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(FxPayload.TYPE, FxPayload.CODEC);
        ServerTickEvents.END_SERVER_TICK.register(ChaosMod::tick);

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String text = message.signedContent().trim();
            for (Run r : runs)
                if (r.p == sender && r.answer != null && !r.ok && text.equalsIgnoreCase(r.answer)) {
                    r.ok = true;
                    U.msg(sender, "✔ Richtig!");
                }
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
            for (Run r : runs) {
                if (r.p != player) continue;
                if (r.ev.id == 15 && state.getBlock().asItem() != net.minecraft.world.item.Items.AIR)
                    Block.popResource(world, pos, new ItemStack(state.getBlock().asItem(), 64));
                if (r.ev.id == 38 && r.obj == state.getBlock() && ++r.count >= 5 && !r.ok) {
                    r.ok = true;
                    U.msg(r.p, "✔ Geschafft!");
                }
            }
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            for (Run r : runs)
                if (r.ev.id == 52 && r.p == player) {
                    Vec3 l = player.getLookAngle();
                    U.launch(r.p, -l.x * 1.6, 0.45, -l.z * 1.6);
                }
            return InteractionResult.PASS;
        });

        // Test-Befehl: /chaos event <1-100>  setzt das NÄCHSTE Ereignis
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("chaos").then(Commands.literal("event")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1, 100)).executes(c -> {
                            int id = IntegerArgumentType.getInteger(c, "id");
                            if (!running) { next = Events.ALL.get(id - 1); phaseStart = 0; }
                            return 1;
                        })))));

        ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
            for (Run r : runs) { try { U.restore(r); } catch (Throwable ignored) { } }
            runs.clear();
        });
    }

    static Ev pick() {
        Ev e;
        do { e = Events.ALL.get(RND.nextInt(Events.ALL.size())); } while (e == last || e == current);
        return e;
    }

    static void tick(MinecraftServer s) {
        List<ServerPlayer> players = s.getPlayerList().getPlayers();
        long now = System.currentTimeMillis();
        if (next == null) { next = pick(); phaseStart = now; }
        if (players.isEmpty()) { if (!running) phaseStart = now; return; }

        boolean sample = now - lastSample >= 1000;
        if (sample) {
            lastSample = now;
            for (ServerPlayer p : players) {
                ArrayDeque<Vec3> h = history.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
                h.addLast(p.position());
                while (h.size() > 31) h.removeFirst();
            }
        }

        if (!running && now - phaseStart >= WAIT) {
            running = true; phaseStart = now; runLen = RUN * nextMult; nextMult = 1; current = next;
            startEvent(s, current, players);
        } else if (running) {
            for (Run r : new ArrayList<>(runs)) {
                r.n++;
                r.dx = r.p.getX() - r.lx; r.dy = r.p.getY() - r.ly; r.dz = r.p.getZ() - r.lz;
                try { r.ev.tick.accept(r); } catch (Throwable t) { LOG.error("tick " + r.ev.id, t); }
                r.lx = r.p.getX(); r.ly = r.p.getY(); r.lz = r.p.getZ();
            }
            if (now - phaseStart >= runLen) finish(now);
        }

        if (bday && s.getTickCount() % 4 == 0) {
            String sel = "execute as @e[type=minecraft:creeper,tag=chaos_bday] at @s if entity @a[distance=..3] run ";
            U.cmd(s, sel + "particle minecraft:firework ~ ~1 ~ 1 1 1 0.2 80 force");
            U.cmd(s, sel + "playsound minecraft:entity.firework_rocket.blast master @a ~ ~ ~ 3 1");
            U.cmd(s, sel + "kill @s");
        }

        updateBars(players, now, sample);
    }

    static void startEvent(MinecraftServer s, Ev ev, List<ServerPlayer> players) {
        boolean first = true;
        for (ServerPlayer p : new ArrayList<>(players)) {
            Run r = new Run(ev, p, s, first, runLen);
            first = false;
            runs.add(r);
            try {
                for (int f : ev.fx) ServerPlayNetworking.send(p, new FxPayload(f, ev.fxMs > 0 ? ev.fxMs : (int) runLen, 0));
                ev.start.accept(r);
            } catch (Throwable t) { LOG.error("start " + ev.id, t); }
        }
    }

    static void startDouble(MinecraftServer s) {
        List<Ev> pool = new ArrayList<>(Events.ALL);
        pool.removeIf(e -> e.id == 69 || e.id == 70);
        Collections.shuffle(pool);
        List<ServerPlayer> pl = s.getPlayerList().getPlayers();
        for (int i = 0; i < 2; i++) {
            startEvent(s, pool.get(i), pl);
            for (ServerPlayer p : pl) U.msg(p, "Doppeltes Event #" + (i + 1) + ": " + pool.get(i).label());
        }
    }

    static void finish(long now) {
        for (Run r : runs) {
            try { r.ev.end.accept(r); } catch (Throwable t) { LOG.error("end " + r.ev.id, t); }
            try { U.restore(r); } catch (Throwable t) { LOG.error("restore " + r.ev.id, t); }
        }
        runs.clear();
        running = false; phaseStart = now; last = current; next = pick();
    }

    static void updateBars(List<ServerPlayer> players, long now, boolean sample) {
        Set<UUID> online = new HashSet<>();
        for (ServerPlayer p : players) {
            UUID id = p.getUUID();
            online.add(id);
            ServerBossEvent b = bars.computeIfAbsent(id, k -> new ServerBossEvent(Component.literal("Chaos"),
                    BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS));
            if (barOwner.get(id) != p) { b.removeAllPlayers(); b.addPlayer(p); barOwner.put(id, p); }

            String journey = "";
            if (journeyPos != null) {
                int d = (int) p.position().distanceTo(Vec3.atCenterOf(journeyPos));
                if (sample) {
                    Integer old = lastDist.put(id, d);
                    if (old != null) trend.put(id, d < old ? "wärmer" : d > old ? "kälter" : "gleich");
                }
                journey = "   |   Schatz: " + d + " m (" + trend.getOrDefault(id, "...") + ")";
                if (d < 4) {
                    journeyPos = null;
                    U.title(p, "Schatz gefunden!", "Die Herobrine-Reise ist vorbei.", 4);
                    U.run(p, "xp add @s 30 levels");
                }
            }

            if (!running) {
                double el = Math.min(1.0, (now - phaseStart) / (double) WAIT);
                long rem = Math.max(0, (WAIT - (now - phaseStart) + 999) / 1000);
                b.setColor(BossEvent.BossBarColor.GREEN);
                b.setProgress((float) el);
                b.setName(Component.literal("Nächstes: " + next.label() + "  –  in " + rem + "s" + journey));
            } else {
                double left = Math.max(0.0, 1.0 - (now - phaseStart) / (double) runLen);
                long rem = Math.max(0, (runLen - (now - phaseStart) + 999) / 1000);
                b.setColor(BossEvent.BossBarColor.RED);
                b.setProgress((float) left);
                b.setName(Component.literal("LÄUFT: " + current.label() + "  –  " + rem + "s" + journey));
            }
        }
        bars.entrySet().removeIf(en -> {
            if (online.contains(en.getKey())) return false;
            en.getValue().removeAllPlayers();
            barOwner.remove(en.getKey());
            return true;
        });
    }
}

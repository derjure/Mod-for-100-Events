package de.chaosmod;

import static de.chaosmod.U.*;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Alle 100 Ereignisse. */
public final class Events {
    public static final List<Ev> ALL = new ArrayList<>();

    private static Ev e(int id, String n) { Ev v = new Ev(id, n); ALL.add(v); return v; }

    private static final Block[] WOOL = {Blocks.WHITE_WOOL, Blocks.ORANGE_WOOL, Blocks.MAGENTA_WOOL, Blocks.LIGHT_BLUE_WOOL,
            Blocks.YELLOW_WOOL, Blocks.LIME_WOOL, Blocks.PINK_WOOL, Blocks.CYAN_WOOL, Blocks.PURPLE_WOOL, Blocks.BLUE_WOOL,
            Blocks.GREEN_WOOL, Blocks.RED_WOOL};
    private static final Block[] RAINBOW = {Blocks.RED_WOOL, Blocks.ORANGE_WOOL, Blocks.YELLOW_WOOL, Blocks.LIME_WOOL,
            Blocks.LIGHT_BLUE_WOOL, Blocks.BLUE_WOOL, Blocks.PURPLE_WOOL};
    private static final Block[] ORES = {Blocks.IRON_ORE, Blocks.GOLD_ORE, Blocks.DIAMOND_ORE, Blocks.COPPER_ORE,
            Blocks.EMERALD_ORE, Blocks.LAPIS_ORE, Blocks.REDSTONE_ORE, Blocks.COAL_ORE};
    private static final String[] RIDE = {"pig", "cow", "sheep", "horse", "llama", "goat", "strider", "ravager", "hoglin",
            "polar_bear", "villager", "iron_golem"};
    private static final String[] VSOUND = {"ambient", "yes", "no", "trade", "celebrate", "hurt"};

    // ---------- Helfer ----------
    static <T extends Entity> T spawnAhead(Run r, EntityType<T> t, double dist) {
        Vec3 d = Vec3.directionFromRotation(0, r.p.getYRot());
        double x = r.p.getX() + d.x * dist, z = r.p.getZ() + d.z * dist;
        int y = r.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
        return spawn(r.level, t, x, y, z);
    }

    static void floor(Run r, int rad, Supplier<BlockState> st) {
        BlockPos c = r.p.blockPosition();
        for (int dx = -rad; dx <= rad; dx++)
            for (int dz = -rad; dz <= rad; dz++) {
                if (dx * dx + dz * dz > rad * rad) continue;
                for (int dy = 0; dy >= -3; dy--) {
                    BlockPos pos = c.offset(dx, dy - 1, dz);
                    BlockState s = r.level.getBlockState(pos);
                    if (!s.isAir() && s.getFluidState().isEmpty()) { set(r, pos, st.get()); break; }
                }
            }
    }

    static void fail(Run r, int dmg, String why) {
        if (r.failed) return;
        r.failed = true;
        run(r.p, "damage @s " + dmg + " minecraft:magic");
        msg(r.p, "✖ " + why);
    }

    static void endCheck(Run r, int dmg, String why) { if (!r.ok) fail(r, dmg, why); }

    static void ch(Run r, String text) { title(r.p, "CHALLENGE", text, 3); }

    static void sweepAnvils(Run r) {
        BlockPos c = r.p.blockPosition();
        for (int dx = -9; dx <= 9; dx++) for (int dz = -9; dz <= 9; dz++) for (int dy = -8; dy <= 4; dy++) {
            BlockPos q = c.offset(dx, dy, dz);
            BlockState s = r.level.getBlockState(q);
            if (s.is(Blocks.ANVIL) || s.is(Blocks.CHIPPED_ANVIL) || s.is(Blocks.DAMAGED_ANVIL))
                r.level.setBlockAndUpdate(q, Blocks.AIR.defaultBlockState());
        }
    }

    static void restoreAt(Run r, String sound) { run(r.p, "playsound " + sound + " master @s ~ ~ ~ 1 1"); }

    static {
        // ===== WESEN =====
        e(1, "Kevin der Warden").perm().start(r -> {
            summon(r.p, "warden", "{CustomName:\"Kevin\",CustomNameVisible:1b,PersistenceRequired:1b}", 0, 40);
            msg(r.p, "Du hörst ein Herz schlagen...");
        }).tick(r -> {
            int gap = Math.max(8, 26 - r.n / 12);
            if (r.n % gap == 0) run(r.p, String.format(Locale.ROOT,
                    "playsound minecraft:entity.warden.heartbeat master @s ~ ~ ~ %.1f %.2f",
                    0.4f + Math.min(3f, r.n / 40f), 0.8f + r.n / 400f));
        });

        e(2, "Dein Schatten-Ich").perm().start(r -> {
            Entity z = spawnAhead(r, EntityType.ZOMBIE, 7);
            if (z == null) return;
            name(z, r.p.getName().getString());
            if (z instanceof Mob m) {
                m.setPersistenceRequired();
                for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                        EquipmentSlot.FEET, EquipmentSlot.MAINHAND})
                    m.setItemSlot(s, r.p.getItemBySlot(s).copy());
                if (m.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) m.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            }
        });

        e(3, "Creeper-Geburtstag").perm().start(r -> {
            String[] n = {"Geburtstagskind", "Party-Paul", "Konfetti-Karl", "Böllerbert"};
            for (int i = 0; i < 4; i++)
                summon(r.p, "creeper", "{CustomName:\"" + n[i] + "\",CustomNameVisible:1b,PersistenceRequired:1b,Tags:[\"chaos_bday\"]}", (i - 1.5) * 3, 8);
            ChaosMod.bday = true;
        });

        e(4, "Gummi-Schwert").start(r -> { eff(r.p, "weakness", r.sec(), 4); ch(r, "Dein Schlag macht fast keinen Schaden!"); });

        e(5, "Here's Johnny").perm().start(r ->
                summon(r.p, "vindicator", "{CustomName:\"Johnny\",CustomNameVisible:1b,PersistenceRequired:1b}", 0, 7));

        e(6, "Schweine-Revolte").perm().start(r -> {
            run(r.p, "execute as @e[type=minecraft:pig,distance=..40] at @s run summon minecraft:zombified_piglin ~ ~ ~ {PersistenceRequired:1b}");
            run(r.p, "kill @e[type=minecraft:pig,distance=..40]");
            summon(r.p, "zombified_piglin", "{PersistenceRequired:1b}", -2, 6);
            summon(r.p, "zombified_piglin", "{PersistenceRequired:1b}", 2, 6);
        });

        e(7, "Reittier-Roulette").perm().start(r -> {
            String t = RIDE[R.nextInt(RIDE.length)];
            run(r.p, "summon minecraft:" + t + " ~ ~ ~ {Tags:[\"chaos_ride\"],PersistenceRequired:1b}");
            run(r.p, "ride @s mount @e[tag=chaos_ride,limit=1,sort=nearest]");
            title(r.p, "Reittier-Roulette!", "Du reitest: " + t, 3);
        });

        e(8, "Tempo: Tickrate x10").start(r -> { if (r.first) cmd(r.server, "tick rate 200"); })
                .end(r -> { if (r.first) cmd(r.server, "tick rate 20"); });

        e(9, "Wandlauf").fx(Fx.WALL).start(r -> ch(r, "Lauf gegen eine Wand und halte W!"));
        e(10, "Doppel-Jump").fx(Fx.JUMP2).start(r -> ch(r, "Du kannst 2x springen!"));

        // ===== POSITIV =====
        e(11, "Haustier-Party").pos().start(r -> {
            String o = "{Owner:" + owner(r.p) + ",PersistenceRequired:1b}";
            String[] t = {"wolf", "wolf", "cat", "cat", "parrot", "parrot"};
            for (int i = 0; i < t.length; i++) summon(r.p, t[i], o, (i - 2.5) * 1.5, 3);
        });
        e(12, "Unverwundbar").pos().start(r -> { eff(r.p, "resistance", r.sec(), 4); eff(r.p, "strength", r.sec(), 2); });
        e(13, "Festmahl").pos().start(r -> {
            run(r.p, "effect give @s minecraft:instant_health 1 10 true");
            run(r.p, "effect give @s minecraft:saturation 5 5 true");
            run(r.p, "give @s minecraft:golden_apple 3");
        });
        e(14, "Elytra-Flug").pos().start(r -> {
            r.obj = r.p.getItemBySlot(EquipmentSlot.CHEST).copy();
            r.p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
            run(r.p, "give @s minecraft:firework_rocket 8");
        }).end(r -> {
            if (r.obj instanceof ItemStack old) r.p.setItemSlot(EquipmentSlot.CHEST, old);
            run(r.p, "clear @s minecraft:firework_rocket");
        });
        e(15, "Glückstag: Block-Stacks").pos().start(r -> msg(r.p, "Jeder abgebaute Block droppt einen ganzen Stack!"));

        // ===== HEROBRINE =====
        e(16, "Herobrine: Sichtung").start(r -> {
            Entity z = spawnAhead(r, EntityType.ZOMBIE, 28);
            if (z == null) return;
            r.obj = z;
            if (z instanceof Mob m) { m.setNoAi(true); m.setPersistenceRequired(); }
            z.setSilent(true); z.setInvulnerable(true); z.setGlowingTag(true);
        }).tick(r -> {
            if (!(r.obj instanceof Entity z) || z.isRemoved()) return;
            Vec3 to = z.getEyePosition().subtract(r.p.getEyePosition()).normalize();
            if (r.n > 10 && r.p.getLookAngle().dot(to) > 0.96) {
                cmd(r.server, String.format(Locale.ROOT, "particle minecraft:large_smoke %.1f %.1f %.1f 0.3 0.8 0.3 0.02 30 force", z.getX(), z.getY() + 1, z.getZ()));
                z.discard();
                r.obj = null;
            }
        }).end(r -> { if (r.obj instanceof Entity z) z.discard(); });

        e(17, "Herobrine joined the game").start(r -> {
            if (r.first) r.server.getPlayerList().broadcastSystemMessage(Component.literal("Herobrine joined the game").withStyle(ChatFormatting.YELLOW), false);
        }).tick(r -> {
            if (r.n > 40 && r.n % 12 == 0)
                exec(r.p, "rotated ~ 0 run playsound minecraft:block.gravel.step master @s ^ ^ ^-2 1 0.8");
        });

        e(18, "Dunkelheit V").start(r -> eff(r.p, "darkness", r.sec(), 4));

        e(19, "Herobrine: Botschaft").perm().start(r -> {
            String[][] t = {{"ER", "SIEHT", "DICH", ""}, {"GEH NICHT", "IN DIE", "HOEHLE", ""}, {"ICH WAR", "SCHON", "HIER", ""}, {"DREH DICH", "NICHT", "UM", ""}};
            String[] s = t[R.nextInt(t.length)];
            StringBuilder m = new StringBuilder();
            for (int i = 0; i < 4; i++) m.append(i > 0 ? "," : "").append('"').append(s[i]).append('"');
            exec(r.p, "rotated ~ 0 positioned ^1 ^ ^2 positioned over motion_blocking_no_leaves run setblock ~ ~ ~ minecraft:oak_sign[rotation="
                    + R.nextInt(16) + "]{front_text:{messages:[" + m + "]}}");
        });

        e(20, "Maus-Chaos x10").fx(Fx.SENS10);

        e(21, "Herobrine Journey").perm().start(r -> {
            if (!r.first) return;
            double ang = R.nextDouble() * Math.PI * 2, dist = 50 + R.nextInt(50);
            int x = (int) (r.p.getX() + Math.cos(ang) * dist), z = (int) (r.p.getZ() + Math.sin(ang) * dist);
            int y = Math.max(-50, r.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 3);
            run(r.p, "setblock " + x + " " + y + " " + z + " minecraft:chest");
            run(r.p, "loot insert " + x + " " + y + " " + z + " loot minecraft:chests/buried_treasure");
            run(r.p, "loot insert " + x + " " + y + " " + z + " loot minecraft:chests/simple_dungeon");
            ChaosMod.journeyPos = new BlockPos(x, y, z);
            for (var pl : r.server.getPlayerList().getPlayers()) title(pl, "Herobrine Journey", "Eine Kiste wurde vergraben. Folge der Bar!", 5);
        });

        e(22, "Lichter aus").start(r -> {
            BlockPos c = r.p.blockPosition();
            for (int dx = -10; dx <= 10; dx++) for (int dy = -5; dy <= 5; dy++) for (int dz = -10; dz <= 10; dz++) {
                BlockPos q = c.offset(dx, dy, dz);
                BlockState s = r.level.getBlockState(q);
                if (s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH) || s.is(Blocks.SOUL_TORCH) || s.is(Blocks.SOUL_WALL_TORCH) || s.is(Blocks.LANTERN))
                    set(r, q, Blocks.AIR.defaultBlockState());
            }
            eff(r.p, "darkness", r.sec(), 1);
        });

        e(23, "Der rote Mond").fx(Fx.REDMOON).start(r -> { if (r.first) cmd(r.server, "time set midnight"); })
                .tick(r -> { if (r.n % 40 == 0) exec(r.p, "rotated ~ 0 run playsound minecraft:block.bell.use master @s ^ ^ ^-30 4 0.6"); })
                .end(r -> { if (r.first) cmd(r.server, "time set day"); });

        // ===== MINI-CHALLENGES =====
        e(24, "Challenge: Statue").start(r -> ch(r, "Nicht bewegen!")).tick(r -> {
            if (r.n > 20 && Math.hypot(r.p.getX() - r.sx, r.p.getZ() - r.sz) > 0.6) fail(r, 5, "Du hast dich bewegt!");
        });
        e(25, "Challenge: Nicht springen").start(r -> ch(r, "Nicht springen!")).tick(r -> {
            if (r.dy > 0.3 && !r.p.isInWater() && !r.p.onClimbable()) fail(r, 5, "Du bist gesprungen!");
        });
        e(26, "Challenge: Nur rückwärts").start(r -> ch(r, "Nur rückwärts laufen!")).tick(r -> {
            double yaw = Math.toRadians(r.p.getYRot());
            double f = r.dx * -Math.sin(yaw) + r.dz * Math.cos(yaw);
            if (r.a > 0) r.a--;
            if (f > 0.06 && r.a == 0) { r.a = 20; run(r.p, "damage @s 1 minecraft:magic"); msg(r.p, "✖ Vorwärts!"); }
        });
        e(27, "Challenge: Schleichen").start(r -> ch(r, "Schleichen ist Pflicht!")).tick(r -> {
            if (r.a > 0) r.a--;
            boolean moving = Math.hypot(r.dx, r.dz) > 0.03;
            if (moving && !r.p.isShiftKeyDown() && r.a == 0) { r.a = 20; run(r.p, "damage @s 1 minecraft:magic"); msg(r.p, "✖ Schleichen!"); }
        });
        e(28, "Challenge: Bleib in der Luft").start(r -> {
            ch(r, "5 Sekunden nicht den Boden berühren!");
            eff(r.p, "jump_boost", r.sec(), 4); eff(r.p, "slow_falling", r.sec(), 0);
        }).tick(r -> {
            if (!r.p.onGround()) r.a++; else r.a = 0;
            r.b = Math.max(r.b, r.a);
            if (r.b >= 100) r.ok = true;
        }).end(r -> endCheck(r, 6, "Du warst zu kurz in der Luft!"));
        e(29, "Challenge: Hüpfer-Quote").start(r -> ch(r, "10 Sprünge in 10 Sekunden!")).tick(r -> {
            if (r.prev && r.dy > 0.3) r.count++;
            r.prev = r.p.onGround();
            if (r.count >= 10) r.ok = true;
        }).end(r -> endCheck(r, 6, "Zu wenige Sprünge!"));
        e(30, "Challenge: Kopfrechnen").start(r -> {
            int a = 2 + R.nextInt(9), b = 2 + R.nextInt(9);
            if (R.nextBoolean()) { r.answer = String.valueOf(a * b); title(r.p, "Tippe das Ergebnis in den Chat:", a + " × " + b, 10); }
            else { r.answer = String.valueOf(a * 7 + b); title(r.p, "Tippe das Ergebnis in den Chat:", a + " × 7 + " + b, 10); }
        }).end(r -> endCheck(r, 5, "Falsch oder zu langsam!"));
        e(31, "Challenge: Code-Tippen").start(r -> {
            String ch = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++) sb.append(ch.charAt(R.nextInt(ch.length())));
            r.answer = sb.toString();
            title(r.p, "Tippe diesen Code in den Chat:", r.answer, 10);
        }).end(r -> endCheck(r, 5, "Code nicht eingegeben!"));
        e(32, "Challenge: Hotbar-Roulette").start(r -> { r.a = R.nextInt(9); ch(r, "Wechsle auf Slot " + (r.a + 1)); }).tick(r -> {
            if (r.n % 20 == 15 && r.p.getInventory().getSelectedSlot() != r.a) { run(r.p, "damage @s 1 minecraft:magic"); msg(r.p, "✖ Falscher Slot!"); }
            if (r.n % 20 == 0) { r.a = R.nextInt(9); title(r.p, "Slot " + (r.a + 1), "Jetzt wechseln!", 1); }
        });
        e(33, "Challenge: Nicht umschauen").start(r -> ch(r, "Kamera nicht drehen!")).tick(r -> {
            if (r.n == 10) { r.syaw = r.p.getYRot(); r.spitch = r.p.getXRot(); }
            if (r.n > 10 && (Math.abs(net.minecraft.util.Mth.wrapDegrees(r.p.getYRot() - r.syaw)) > 20 || Math.abs(r.p.getXRot() - r.spitch) > 20))
                fail(r, 5, "Du hast dich umgeschaut!");
        });
        e(34, "Bildschirm 9:16").fx(Fx.RATIO);
        e(35, "Umgekehrte Schwerkraft").start(r -> run(r.p, "attribute @s minecraft:gravity base set -0.005"))
                .end(r -> { run(r.p, "attribute @s minecraft:gravity base set 0.08"); eff(r.p, "slow_falling", 12, 0); });
        e(36, "Challenge: Höhenflug").start(r -> { ch(r, "Klettere 10 Blöcke hoch!"); run(r.p, "give @s minecraft:cobblestone 16"); }).tick(r -> {
            if (r.p.getY() >= r.sy + 10 && !r.ok) { r.ok = true; msg(r.p, "✔ Geschafft!"); }
        }).end(r -> endCheck(r, 6, "Nicht hoch genug!"));
        e(37, "Challenge: Sprint 30 Blöcke").start(r -> ch(r, "Lauf 30 Blöcke weit!")).tick(r -> {
            if (Math.hypot(r.p.getX() - r.sx, r.p.getZ() - r.sz) >= 30 && !r.ok) { r.ok = true; msg(r.p, "✔ Geschafft!"); }
        }).end(r -> endCheck(r, 6, "Zu langsam!"));
        e(38, "Challenge: Block-Jäger").start(r -> {
            BlockState s = r.level.getBlockState(r.p.blockPosition().below());
            r.obj = s.getBlock();
            ch(r, "Baue 5x ab: " + s.getBlock().getName().getString());
        }).end(r -> endCheck(r, 6, "Zu wenige Blöcke!"));

        // ===== KÖRPER & PHYSIK =====
        e(39, "Katapult").start(r -> { launch(r.p, 0, 2.4, 0); eff(r.p, "slow_falling", 14, 0); });
        e(40, "Levitations-Pech").start(r -> eff(r.p, "levitation", 3, 9));
        e(41, "Speed-Wahnsinn").start(r -> eff(r.p, "speed", r.sec(), 9));
        e(42, "Riesen-Du").start(r -> run(r.p, "attribute @s minecraft:scale base set 3"))
                .end(r -> run(r.p, "attribute @s minecraft:scale base set 1"));
        e(43, "Mini-Du").start(r -> run(r.p, "attribute @s minecraft:scale base set 0.3"))
                .end(r -> run(r.p, "attribute @s minecraft:scale base set 1"));
        e(44, "Rückwärtsgang").tick(r -> {
            Vec3 l = r.p.getLookAngle(), m = r.p.getDeltaMovement();
            launch(r.p, m.x - l.x * 0.1, m.y, m.z - l.z * 0.1);
        });
        e(45, "Schluckauf").tick(r -> {
            if (r.n % 20 == 0) {
                Vec3 m = r.p.getDeltaMovement();
                launch(r.p, m.x, 0.45, m.z);
                run(r.p, "playsound minecraft:entity.player.burp master @s ~ ~ ~ 1 1.2");
            }
        });
        e(46, "Invertierte Steuerung").fx(Fx.INVERT);
        e(47, "Zufalls-Teleport").start(r -> run(r.p, "spreadplayers ~ ~ 5 50 false @s"));
        e(48, "Glaskasten").start(r -> {
            BlockPos c = r.p.blockPosition();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy <= 3; dy++) {
                boolean wall = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                if ((wall && dy <= 2) || dy == 3) {
                    BlockPos q = c.offset(dx, dy, dz);
                    if (r.level.getBlockState(q).isAir()) set(r, q, Blocks.GLASS.defaultBlockState());
                }
            }
        });
        e(49, "Honig-Pech").start(r -> eff(r.p, "slowness", r.sec(), 6)).tick(r -> {
            if (r.n % 20 == 0) run(r.p, "particle minecraft:falling_honey ~ ~1 ~ 0.3 0.5 0.3 0 8 force");
        });
        e(50, "Betrunken").start(r -> eff(r.p, "nausea", r.sec(), 1));
        e(51, "Schlafwandler").tick(r -> {
            if (r.n % 10 == 0) { double a = R.nextDouble() * 6.283; launch(r.p, Math.cos(a) * 0.35, 0.1, Math.sin(a) * 0.35); }
        });
        e(52, "Rückstoß-Party").start(r -> msg(r.p, "Jeder Schlag wirft DICH weg!"));
        e(53, "Matrix-Modus").start(r -> { if (r.first) cmd(r.server, "tick rate 5"); })
                .end(r -> { if (r.first) cmd(r.server, "tick rate 20"); });

        // ===== WELT =====
        e(54, "Der Boden ist Lava").tick(r -> {
            if (r.n % 3 != 0) return;
            BlockPos c = r.p.blockPosition().below();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos q = c.offset(dx, 0, dz);
                BlockState s = r.level.getBlockState(q);
                if (!s.isAir() && s.getFluidState().isEmpty()) set(r, q, Blocks.LAVA.defaultBlockState());
            }
        });
        e(55, "Ambosshagel").tick(r -> {
            if (r.n % 8 != 0) return;
            double a = R.nextDouble() * 6.283, d = R.nextDouble() * 5;
            exec(r.p, String.format(Locale.ROOT, "positioned ~%.1f ~12 ~%.1f run summon minecraft:falling_block ~ ~ ~ {BlockState:{Name:\"minecraft:anvil\"},Time:1,DropItem:0b,HurtEntities:1b,FallHurtMax:30,FallHurtAmount:1.5f}",
                    Math.cos(a) * d, Math.sin(a) * d));
        }).end(Events::sweepAnvils);
        e(56, "Gewitter").start(r -> { if (r.first) cmd(r.server, "weather thunder 30s"); }).tick(r -> {
            if (r.n % 25 == 0) {
                double a = R.nextDouble() * 6.283, d = 5 + R.nextDouble() * 5;
                run(r.p, String.format(Locale.ROOT, "summon minecraft:lightning_bolt ~%.1f ~ ~%.1f", Math.cos(a) * d, Math.sin(a) * d));
            }
        }).end(r -> { if (r.first) cmd(r.server, "weather clear"); });
        e(57, "Plötzliche Nacht").start(r -> { if (r.first) cmd(r.server, "time set midnight"); })
                .end(r -> { if (r.first) cmd(r.server, "time set day"); });
        e(58, "Slime-Boden").start(r -> run(r.p, "attribute @s minecraft:fall_damage_multiplier base set 0")).tick(r -> {
            if (r.n > 2 && r.p.onGround()) {
                Vec3 m = r.p.getDeltaMovement();
                launch(r.p, m.x, 0.9, m.z);
                run(r.p, "playsound minecraft:entity.slime.squish master @s ~ ~ ~ 1 1");
            }
        }).end(r -> run(r.p, "attribute @s minecraft:fall_damage_multiplier base set 1"));
        e(59, "Eisbahn").tick(r -> { if (r.n % 5 == 0) floor(r, 5, Blocks.BLUE_ICE::defaultBlockState); });
        e(60, "Wollmuster").tick(r -> { if (r.n % 10 == 0) floor(r, 4, () -> WOOL[R.nextInt(WOOL.length)].defaultBlockState()); });
        e(61, "TNT-Run").start(r -> r.obj = new HashMap<BlockPos, Integer>()).tick(r -> {
            @SuppressWarnings("unchecked") Map<BlockPos, Integer> pend = (Map<BlockPos, Integer>) r.obj;
            BlockPos under = r.p.blockPosition().below();
            BlockState s = r.level.getBlockState(under);
            if (!s.isAir() && s.getFluidState().isEmpty() && !pend.containsKey(under)) pend.put(under.immutable(), r.n + 12);
            Iterator<Map.Entry<BlockPos, Integer>> it = pend.entrySet().iterator();
            while (it.hasNext()) {
                var en = it.next();
                if (r.n >= en.getValue()) { set(r, en.getKey(), Blocks.AIR.defaultBlockState()); it.remove(); }
            }
        });
        e(62, "Überflutung").start(r -> {
            BlockPos c = r.p.blockPosition();
            for (int dx = -6; dx <= 6; dx++) for (int dy = -1; dy <= 5; dy++) for (int dz = -6; dz <= 6; dz++) {
                if (dx * dx + dy * dy + dz * dz > 36) continue;
                BlockPos q = c.offset(dx, dy, dz);
                if (r.level.getBlockState(q).isAir()) set(r, q, Blocks.WATER.defaultBlockState());
            }
        });
        e(63, "Spinnweben-Falle").start(r -> {
            BlockPos c = r.p.blockPosition();
            for (int dx = -3; dx <= 3; dx++) for (int dy = 0; dy <= 3; dy++) for (int dz = -3; dz <= 3; dz++) {
                BlockPos q = c.offset(dx, dy, dz);
                if (r.level.getBlockState(q).isAir() && R.nextInt(100) < 45) set(r, q, Blocks.COBWEB.defaultBlockState());
            }
        });
        e(64, "Schneesturm").start(r -> { if (r.first) cmd(r.server, "weather rain 30s"); })
                .tick(r -> { if (r.n % 20 == 0) floor(r, 6, Blocks.SNOW_BLOCK::defaultBlockState); })
                .end(r -> { if (r.first) cmd(r.server, "weather clear"); });
        e(65, "Villager-Chor").tick(r -> {
            if (r.n % 5 == 0) run(r.p, String.format(Locale.ROOT, "playsound minecraft:entity.villager.%s master @s ~ ~ ~ 1.5 %.2f",
                    VSOUND[R.nextInt(VSOUND.length)], 0.7 + R.nextDouble() * 0.8));
        });
        e(66, "Wasserfall vom Himmel").start(r -> {
            BlockPos q = r.p.blockPosition().above(14);
            if (r.level.getBlockState(q).isAir()) set(r, q, Blocks.WATER.defaultBlockState());
        });
        e(67, "Bedrock-Mauer").start(r -> {
            Direction d = r.p.getDirection(), side = d.getClockWise();
            BlockPos base = r.p.blockPosition().relative(d, 3);
            for (int w = -2; w <= 2; w++) for (int h = 0; h <= 3; h++) {
                BlockPos q = base.relative(side, w).above(h);
                if (r.level.getBlockState(q).isAir()) set(r, q, Blocks.BEDROCK.defaultBlockState());
            }
        });
        e(68, "Anti: 30 Sekunden zurück").start(r -> {
            ArrayDeque<Vec3> h = ChaosMod.history.get(r.p.getUUID());
            if (h == null || h.isEmpty()) return;
            Vec3 o = h.peekFirst();
            run(r.p, String.format(Locale.ROOT, "tp @s %.2f %.2f %.2f", o.x, o.y, o.z));
        });
        e(69, "Doppeltes Event").start(r -> { if (r.first) ChaosMod.startDouble(r.server); });
        e(70, "Aufpassen...").start(r -> {
            ChaosMod.nextMult = 2;
            title(r.p, "Aufpassen...", "Das nächste Ereignis dauert doppelt so lang!", 4);
        });
        e(71, "Regenbogen").perm().start(r -> {
            Direction d = r.p.getDirection(), side = d.getClockWise();
            BlockPos ctr = r.p.blockPosition().relative(d, 50);
            int gy = r.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ctr.getX(), ctr.getZ());
            for (int band = 0; band < RAINBOW.length; band++) {
                double rad = 22 + band;
                for (double a = 0; a <= Math.PI; a += 1.0 / (rad * 1.2)) {
                    int off = (int) Math.round(Math.cos(a) * rad);
                    int y = (int) Math.round(gy - 4 + Math.sin(a) * rad);
                    BlockPos q = new BlockPos(ctr.getX() + side.getStepX() * off, y, ctr.getZ() + side.getStepZ() * off);
                    if (r.level.getBlockState(q).isAir()) r.level.setBlockAndUpdate(q, RAINBOW[band].defaultBlockState());
                }
            }
        });
        e(72, "Meteoreinschlag").perm().start(r -> {
            Vec3 d = Vec3.directionFromRotation(0, r.p.getYRot());
            int cx = (int) Math.floor(r.p.getX() + d.x * 20), cz = (int) Math.floor(r.p.getZ() + d.z * 20);
            int cy = r.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz) - 1;
            for (int dx = -5; dx <= 5; dx++) for (int dy = -5; dy <= 5; dy++) for (int dz = -5; dz <= 5; dz++) {
                int d2 = dx * dx + dy * dy + dz * dz;
                BlockPos q = new BlockPos(cx + dx, cy + dy, cz + dz);
                if (d2 <= 16) r.level.setBlockAndUpdate(q, Blocks.AIR.defaultBlockState());
                else if (d2 <= 30 && !r.level.getBlockState(q).isAir() && R.nextInt(100) < 40)
                    r.level.setBlockAndUpdate(q, ORES[R.nextInt(ORES.length)].defaultBlockState());
            }
            r.level.setBlockAndUpdate(new BlockPos(cx, cy - 4, cz), Blocks.MAGMA_BLOCK.defaultBlockState());
            cmd(r.server, "particle minecraft:explosion_emitter " + cx + " " + (cy + 1) + " " + cz + " 0 0 0 0 1 force");
            run(r.p, "playsound minecraft:entity.generic.explode master @s ~ ~ ~ 4 0.7");
        });
        e(73, "Zeitraffer").start(r -> { }).tick(r -> {
            if (r.first) cmd(r.server, "time add 60");
            if (r.n % 10 == 0) {
                BlockPos c = r.p.blockPosition();
                for (int i = 0; i < 15; i++)
                    BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), r.level, c.offset(R.nextInt(17) - 8, R.nextInt(5) - 2, R.nextInt(17) - 8));
            }
        });

        // ===== BILDSCHIRM & CLIENT =====
        e(74, "1 FPS").fx(Fx.FPS1);
        e(75, "Disco-Bildschirm").fx(Fx.DISCO);
        e(76, "Spiegelwelt").fx(Fx.MIRROR);
        e(77, "FOV-Jojo").fx(Fx.FOV_YOYO);
        e(78, "Fisheye-Blick").fx(Fx.FISHEYE);
        e(79, "Tunnelblick").fx(Fx.TUNNEL);
        e(80, "Schieflage").fx(Fx.TILT);
        e(81, "Kamera-Karussell").fx(Fx.CAROUSEL);
        e(82, "Windows-Update").fx(Fx.WINDOWS).start(r -> title(r.p, "Windows wird aktualisiert", "37 % abgeschlossen. Schalte den Computer nicht aus.", r.sec()));
        e(83, "Pixel-Modus").fx(Fx.PIXEL);
        e(84, "Nebelwand").fx(Fx.FOG);
        e(85, "Sonnenfinsternis").fx(Fx.ECLIPSE);
        e(86, "Zoom-Bomb").fx(Fx.ZOOM);
        e(87, "HUD weg").fx(Fx.NOHUD);
        e(88, "Maus-Sensitivität 500 %").fx(Fx.SENS5);

        // ===== ITEMS =====
        e(89, "Hotbar-Mixer").start(r -> {
            List<ItemStack> l = new ArrayList<>();
            for (int i = 0; i < 9; i++) l.add(r.p.getInventory().getItem(i).copy());
            Collections.shuffle(l);
            for (int i = 0; i < 9; i++) r.p.getInventory().setItem(i, l.get(i));
        });
        e(90, "Inventar ausgeleert").start(r -> {
            for (int i = 0; i < r.p.getInventory().getContainerSize(); i++) {
                ItemStack s = r.p.getInventory().getItem(i);
                if (!s.isEmpty()) { dropItem(r.p, s.copy()); r.p.getInventory().setItem(i, ItemStack.EMPTY); }
            }
        });
        e(91, "Essen verdorben").start(r -> {
            for (int i = 0; i < r.p.getInventory().getContainerSize(); i++) {
                ItemStack s = r.p.getInventory().getItem(i);
                if (!s.isEmpty() && s.get(DataComponents.FOOD) != null && !s.is(Items.ROTTEN_FLESH))
                    r.p.getInventory().setItem(i, new ItemStack(Items.ROTTEN_FLESH, s.getCount()));
            }
        });
        e(92, "Fairer Tausch").start(r -> {
            ItemStack s = r.p.getMainHandItem();
            if (!s.isEmpty()) r.p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, Math.min(64, s.getCount())));
        });
        e(93, "Kürbiskopf").start(r -> {
            r.obj = r.p.getItemBySlot(EquipmentSlot.HEAD).copy();
            run(r.p, "item replace entity @s armor.head with minecraft:carved_pumpkin[enchantments={\"minecraft:binding_curse\":1}]");
        }).end(r -> { if (r.obj instanceof ItemStack old) r.p.setItemSlot(EquipmentSlot.HEAD, old); });
        e(94, "Rüstung fällt ab").start(r -> {
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                dropItem(r.p, r.p.getItemBySlot(s).copy());
                r.p.setItemSlot(s, ItemStack.EMPTY);
            }
        });
        e(95, "Taschendieb").start(r -> {
            List<Integer> used = new ArrayList<>();
            for (int i = 0; i < r.p.getInventory().getContainerSize(); i++) if (!r.p.getInventory().getItem(i).isEmpty()) used.add(i);
            if (used.isEmpty()) return;
            int i = used.get(R.nextInt(used.size()));
            r.p.getInventory().setItem(i, new ItemStack(Items.POISONOUS_POTATO, Math.max(1, r.p.getInventory().getItem(i).getCount())));
        });
        e(96, "Werkzeug zerbröselt").start(r -> {
            for (int i = 0; i < r.p.getInventory().getContainerSize(); i++) {
                ItemStack s = r.p.getInventory().getItem(i);
                if (s.isDamageableItem()) s.setDamageValue(Math.min(s.getMaxDamage() - 1, s.getDamageValue() + s.getMaxDamage() / 10));
            }
        });

        // ===== CHAOS & FAKE =====
        e(97, "Fake-Creeper-Zischen").start(r -> exec(r.p, "rotated ~ 0 run playsound minecraft:entity.creeper.primed master @s ^ ^ ^-1.5 1.5 1"));
        e(98, "Fake-Lag").tick(r -> {
            if (r.n % 40 == 20) r.obj = new Vec3(r.p.getX(), r.p.getY(), r.p.getZ());
            if (r.n % 40 == 0 && r.obj instanceof Vec3 o)
                run(r.p, String.format(Locale.ROOT, "tp @s %.2f %.2f %.2f", o.x, o.y, o.z));
        });
        e(99, "MLG Water Bucket").start(r -> {
            BlockPos c = r.p.blockPosition();
            int up = 0;
            while (up < 40 && r.level.getBlockState(c.above(up + 3)).isAir()) up++;
            run(r.p, "tp @s ~ ~" + up + " ~");
            r.obj = r.p.getMainHandItem().copy();
            r.p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            title(r.p, "MLG!", "Rette dich mit dem Wassereimer!", 4);
        }).end(r -> {
            if (r.obj instanceof ItemStack old && (r.p.getMainHandItem().is(Items.BUCKET) || r.p.getMainHandItem().is(Items.WATER_BUCKET)))
                r.p.setItemInHand(InteractionHand.MAIN_HAND, old);
        });
        e(100, "Fake-Kick").fx(Fx.KICK).ms(3000).start(r -> title(r.p, "Du wurdest vom Server geworfen", "Verbindung verloren", 3));

        ALL.sort(Comparator.comparingInt(x -> x.id));
        for (int i = 0; i < ALL.size(); i++) if (ALL.get(i).id != i + 1) throw new IllegalStateException("Event-ID-Luecke bei " + (i + 1));
        // Dauerhafte Ereignisse (🔒) wurden mit .perm() markiert.
    }

    private Events() {}
}

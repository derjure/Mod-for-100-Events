package de.chaosmod;

import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Client-Effekte: Overlays, 1 FPS, Kamera- und Steuerungs-Spielereien. */
public class ChaosClient implements ClientModInitializer {
    private static final Map<Integer, Long> ACTIVE = new HashMap<>();
    private static Integer savedFov, savedRender;
    private static float lastYaw, lastPitch;
    private static boolean haveLast, prevJump;
    private static int jumpsLeft;

    static boolean on(int fx) {
        Long e = ACTIVE.get(fx);
        return e != null && System.currentTimeMillis() < e;
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FxPayload.TYPE,
                (payload, ctx) -> ACTIVE.put(payload.fx(), System.currentTimeMillis() + payload.durationMs()));
        ClientTickEvents.END_CLIENT_TICK.register(ChaosClient::tick);
        // Vor der Boss-Bar zeichnen, damit die Bar immer sichtbar bleibt.
        HudElementRegistry.attachElementBefore(VanillaHudElements.BOSS_BAR,
                Identifier.fromNamespaceAndPath(ChaosMod.ID, "overlay"), (g, dt) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (on(Fx.FPS1)) { try { Thread.sleep(900); } catch (InterruptedException ignored) { } }
                    int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
                    for (int[] q : rects(w, h)) g.fill(q[0], q[1], q[2], q[3], q[4]);
                });
    }

    private static List<int[]> rects(int w, int h) {
        List<int[]> o = new ArrayList<>();
        long now = System.currentTimeMillis();
        if (on(Fx.DISCO)) o.add(new int[]{0, 0, w, h, (0x55 << 24) | Mth.hsvToRgb((now % 2000) / 2000f, 0.9f, 1f)});
        if (on(Fx.ECLIPSE)) o.add(new int[]{0, 0, w, h, 0xB0808080});
        if (on(Fx.REDMOON)) o.add(new int[]{0, 0, w, h, 0x55FF0000});
        if (on(Fx.TUNNEL)) {
            for (int i = 1; i <= 8; i++) {
                int tx = i * w / 36, ty = i * h / 36;
                int c = 0x50000000;
                o.add(new int[]{0, 0, w, ty, c});
                o.add(new int[]{0, h - ty, w, h, c});
                o.add(new int[]{0, ty, tx, h - ty, c});
                o.add(new int[]{w - tx, ty, w, h - ty, c});
            }
        }
        if (on(Fx.PIXEL)) {
            long seed = now / 120;
            for (int i = 0; i < w; i += 40) for (int j = 0; j < h; j += 40) {
                Random rnd = new Random(seed * 131 + i * 977L + j);
                o.add(new int[]{i, j, i + 40, j + 40, (0x50 << 24) | rnd.nextInt(0xFFFFFF)});
            }
        }
        if (on(Fx.RATIO)) {
            int cw = (int) (h * 9f / 16f);
            if (w > cw) {
                o.add(new int[]{0, 0, (w - cw) / 2, h, 0xFF000000});
                o.add(new int[]{w - (w - cw) / 2, 0, w, h, 0xFF000000});
            }
        }
        if (on(Fx.NOHUD)) o.add(new int[]{w / 2 - 98, h - 62, w / 2 + 98, h, 0xFF000000});
        if (on(Fx.WINDOWS)) o.add(new int[]{0, 0, w, h, 0xFF0078D7});
        if (on(Fx.KICK)) o.add(new int[]{0, 0, w, h, 0xFF000000});
        return o;
    }

    private static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) { haveLast = false; return; }
        var o = mc.options;
        long t = System.currentTimeMillis();

        // Kamera: Sensitivität, Spiegel, Karussell, Schieflage
        float yaw = p.getYRot(), pitch = p.getXRot();
        boolean rot = on(Fx.SENS10) || on(Fx.SENS5) || on(Fx.MIRROR) || on(Fx.CAROUSEL) || on(Fx.TILT);
        if (!rot || !haveLast) {
            lastYaw = yaw; lastPitch = pitch; haveLast = true;
        } else {
            float mult = on(Fx.SENS10) ? 10f : on(Fx.SENS5) ? 5f : 1f;
            if (on(Fx.MIRROR)) mult = -mult;
            float ny = lastYaw + Mth.wrapDegrees(yaw - lastYaw) * mult;
            float np = Mth.clamp(lastPitch + (pitch - lastPitch) * mult, -90f, 90f);
            if (on(Fx.CAROUSEL)) ny += 2.5f;
            if (on(Fx.TILT)) { np = Mth.clamp(np + Mth.sin(t / 250f) * 1.5f, -90f, 90f); ny += Mth.cos(t / 330f) * 1.2f; }
            p.setYRot(ny); p.setXRot(np);
            lastYaw = ny; lastPitch = np;
        }

        // Doppel-Sprung
        if (on(Fx.JUMP2)) {
            boolean jd = o.keyJump.isDown();
            if (p.onGround()) jumpsLeft = 1;
            else if (jd && !prevJump && jumpsLeft > 0) {
                Vec3 m = p.getDeltaMovement();
                p.setDeltaMovement(m.x, 0.5, m.z);
                jumpsLeft--;
            }
            prevJump = jd;
        }
        // Wandlauf
        if (on(Fx.WALL) && p.horizontalCollision && o.keyUp.isDown()) {
            Vec3 m = p.getDeltaMovement();
            p.setDeltaMovement(m.x, 0.28, m.z);
        }
        // Invertierte Steuerung (Gegenimpuls)
        if (on(Fx.INVERT)) {
            float f = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
            float s = (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0);
            if (f != 0 || s != 0) {
                double r = Math.toRadians(p.getYRot());
                double fx = -Math.sin(r), fz = Math.cos(r), lx = Math.cos(r), lz = Math.sin(r);
                Vec3 m = p.getDeltaMovement();
                p.setDeltaMovement(m.x - (f * fx + s * lx) * 0.2, m.y, m.z - (f * fz + s * lz) * 0.2);
            }
        }

        // FOV-Effekte
        if (on(Fx.FOV_YOYO) || on(Fx.FISHEYE) || on(Fx.ZOOM)) {
            if (savedFov == null) savedFov = o.fov().get();
            int v = on(Fx.FISHEYE) ? 110 : on(Fx.ZOOM) ? 30 : (int) (70 + 40 * Math.sin(t / 400.0));
            o.fov().set(Mth.clamp(v, 30, 110));
        } else if (savedFov != null) { o.fov().set(savedFov); savedFov = null; }

        // Nebelwand
        if (on(Fx.FOG)) {
            if (savedRender == null) savedRender = o.renderDistance().get();
            o.renderDistance().set(2);
        } else if (savedRender != null) { o.renderDistance().set(savedRender); savedRender = null; }
    }
}

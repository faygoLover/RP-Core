package faygolover.rpcore.client.effect;

import faygolover.flrender.render.PostFx;
import faygolover.rpcore.RpCore;
import faygolover.rpcore.effect.EffectSync;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;


/**
 * «Помехи»: у всех, кто рядом с носителем, экран как у камеры в зоне радиации — частицы засвечивают случайные
 * точки и короткие чёрточки, лёгкое зерно; плюс шипение и щелчки счётчика Гейгера ({@link InterferenceSound}).
 * Сила — от расстояния до ближайшего носителя (радиус 12 блоков на уровень эффекта),
 * плавно нарастает и спадает. У самого носителя ничего нет. Свой проход поверх кадра после мира (до руки и
 * интерфейса), не занимает слот постэффекта игры (его держит RP Medicine); с шейдерпаком — после его кадра.
 */
public final class Interference {
    private Interference() {}

    private static final ResourceLocation CHAIN = new ResourceLocation(RpCore.MODID, "shaders/post/interference.json");
    private static final double RADIUS = 12.0;

    private static final PostFx FX = new PostFx(CHAIN);
    private static float target, strength, strengthO;

    static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        target = mc.level == null || mc.player == null ? 0f : compute(mc);
        strengthO = strength;
        // Нарастает быстрее, чем спадает.
        strength += (target - strength) * (target > strength ? 0.35f : 0.12f);
        if (strength < 0.002f) strength = 0f;
        InterferenceSound.tick(strength);
    }

    private static float compute(Minecraft mc) {
        Entity self = mc.getCameraEntity();
        if (self == null) return 0f;
        Vec3 eye = self.getEyePosition();
        float best = 0f;
        for (int[] en : ClientEffects.entries()) {
            if ((en[1] & EffectSync.INTERFERENCE) == 0 || en[0] == self.getId()) continue;
            Entity src = mc.level.getEntity(en[0]);
            if (src == null || src.isRemoved()) continue;
            int level = Math.max(1, en[1] >> 8 & 15);
            double r = RADIUS * level;
            double d = src.getEyePosition().distanceTo(eye);
            if (d >= r) continue;
            float s = (float) Math.pow(1.0 - d / r, 1.4);
            if (s > best) best = s;
        }
        return Math.min(1f, best);
    }

    /** Текущая сила помех (0–1). */
    static float current() {
        return strength;
    }

    static void onRenderLevel(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        float s = strengthO + (strength - strengthO) * e.getPartialTick();
        if (s <= 0.002f) return;
        FX.process(e.getPartialTick(), fx -> {
            fx.safeGetUniform("Strength").set(s);
            // Свои часы: «Time» прохода Minecraft сбрасывается каждую секунду — помехи повторялись узором.
            fx.safeGetUniform("Clock").set((float) ((System.nanoTime() / 1_000_000L) % 1_000_000L) / 1000f);
        });
    }
}

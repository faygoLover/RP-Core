package faygolover.rpcore.client.effect;

import com.mojang.blaze3d.systems.RenderSystem;
import faygolover.rpcore.RpCore;
import faygolover.rpcore.effect.EffectSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.util.List;

/**
 * «Помехи»: у всех, кто рядом с носителем, экран рябит, как камера в зоне радиации — зерно, дрожь строк, разрывы,
 * сдвиг цветов, бегущая полоса. Сила — от расстояния до ближайшего носителя (радиус 12 блоков на уровень эффекта),
 * плавно нарастает и спадает. У самого носителя ничего нет. Свой проход поверх кадра после мира (до руки и
 * интерфейса), не занимает слот постэффекта игры (его держит RP Medicine); с шейдерпаком — после его кадра.
 */
public final class Interference {
    private Interference() {}

    private static final ResourceLocation CHAIN = new ResourceLocation(RpCore.MODID, "shaders/post/interference.json");
    private static final double RADIUS = 12.0;

    private static PostChain chain;
    private static boolean failed;
    private static int w = -1, h = -1;
    private static float target, strength, strengthO;

    static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        target = mc.level == null || mc.player == null ? 0f : compute(mc);
        strengthO = strength;
        // Нарастает быстрее, чем спадает.
        strength += (target - strength) * (target > strength ? 0.35f : 0.12f);
        if (strength < 0.002f) strength = 0f;
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

    static void onRenderLevel(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        float s = strengthO + (strength - strengthO) * e.getPartialTick();
        if (s <= 0.002f || failed) return;
        Minecraft mc = Minecraft.getInstance();
        var main = mc.getMainRenderTarget();
        try {
            if (chain == null || main.width != w || main.height != h) {
                if (chain != null) chain.close();
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, CHAIN);
                chain.resize(main.width, main.height);
                w = main.width;
                h = main.height;
            }
            List<PostPass> passes = ObfuscationReflectionHelper.getPrivateValue(PostChain.class, chain, "f_110009_");
            if (passes != null && !passes.isEmpty()) passes.get(0).getEffect().safeGetUniform("Strength").set(s);
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            chain.process(e.getPartialTick());
            main.bindWrite(false);
            RenderSystem.enableDepthTest();
        } catch (Exception ex) {
            failed = true;
            RpCore.LOGGER.error("RP Core: проход «Помехи» не загрузился", ex);
        }
    }
}

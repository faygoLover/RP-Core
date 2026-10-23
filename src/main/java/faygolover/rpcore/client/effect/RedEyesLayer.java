package faygolover.rpcore.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import faygolover.rpcore.RpCore;
import faygolover.rpcore.client.CoreClientConfig;
import faygolover.rpcore.effect.EffectSync;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * «Красные глаза». Два вида ({@link CoreClientConfig#EYES_STYLE}):
 * <ul>
 *     <li><b>шары</b> (по умолчанию) — на месте каждого глаза ярко светящийся красный шар: круг, всегда повёрнутый к
 *     зрителю, с горячим светлым ядром и мягким краем, вокруг — ореол и широкое слабое свечение, медленно и неровно
 *     «дышат» (как Комета в Zone Artifacts). Шар выдвинут к зрителю — его не закрывают 3D Skin Layers / Fresh
 *     Animations, а стены и сама голова сзади закрывают;</li>
 *     <li><b>плоские</b> (0.8.6) — красное светящееся пятно на лице (закрывает глаза скина и анимированные глаза
 *     текстурпаков), вплотную к лицу (в шлеме — сразу перед шлемом), тусклее ореол и слабый расходящийся свет вперёд;
 *     рисуется чуть «подтянутым» к камере ({@link EyeRenderTypes}).</li>
 * </ul>
 */
public class RedEyesLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation SPOT = new ResourceLocation(RpCore.MODID, "textures/entity/eye_spot.png");
    private static final ResourceLocation GLOW = new ResourceLocation(RpCore.MODID, "textures/entity/eye_glow.png");
    private static final ResourceLocation BEAM = new ResourceLocation(RpCore.MODID, "textures/entity/eye_beam.png");
    private static final ResourceLocation SPHERE = new ResourceLocation(RpCore.MODID, "textures/entity/eye_sphere.png");
    private static final ResourceLocation BLOOM = new ResourceLocation(RpCore.MODID, "textures/entity/eye_bloom.png");
    private static final float PX = 1f / 16f;
    /** Глаза на лице (пиксели головы, как у скина Стива): x = ±2, y = −3.5. */
    private static final float EYE_Y = -3.5f;
    private static final float SPOT_SIZE = 2.0f, GLOW_SIZE = 4.6f;
    /** Луч: длина и ширина у глаза и в конце (пиксели головы). */
    private static final float BEAM_LEN = 22f, BEAM_W0 = 1.2f, BEAM_W1 = 9f;
    /** Шар: радиусы самого шара, ореола и широкого свечения; насколько выдвинут к зрителю (пиксели головы). */
    private static final float SPHERE_R = 1.35f, BLOOM_R = 3.6f, HALO_R = 8f, SPHERE_PULL = 1.8f;

    public RedEyesLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing, float limbAmount,
                       float partial, float age, float headYaw, float headPitch) {
        if ((ClientEffects.mask(p) & EffectSync.RED_EYES) == 0 || p.isInvisible()) return;
        pose.pushPose();
        getParentModel().head.translateAndRotate(pose);
        boolean helmet = !p.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
        // В интерфейсе (инвентарь) — плоская проекция.
        boolean gui = com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix().m23() == 0f;
        if (CoreClientConfig.EYES_STYLE.get() == CoreClientConfig.EyesStyle.FLAT) renderFlat(pose, buffers, helmet, gui, age);
        else renderSphere(pose, buffers, helmet, gui, age, p.getId());
        pose.popPose();
    }

    /**
     * Шары: центры глаз переводятся в пространство камеры, и там рисуются круги, параллельные экрану (в мире
     * камера — в начале координат, в инвентаре смотрит вдоль −z). Размер — по масштабу головы, так что у детей и в
     * инвентаре глаза на своих местах.
     */
    private static void renderSphere(PoseStack pose, MultiBufferSource buffers, boolean helmet, boolean gui, float age, int id) {
        Matrix4f m = pose.last().pose();
        float z = (helmet ? -5.0f : -4.0f) * PX;
        float unit = m.transformDirection(PX, 0, 0, new Vector3f()).length();
        float phase = id * 0.37f;
        float breath = 1f + 0.06f * (0.6f * (float) Math.sin(age * 0.07f + phase) + 0.4f * (float) Math.sin(age * 0.173f + phase * 2.3f));
        float pull = SPHERE_PULL * unit;
        Vector3f[] eyes = new Vector3f[2];
        for (int i = 0; i < 2; i++) {
            Vector3f c = m.transformPosition((i == 0 ? -2f : 2f) * PX, EYE_Y * PX, z, new Vector3f());
            if (gui) c.z += pull;
            else {
                float len = c.length();
                if (len > 1.0E-4f) c.mul(1f - Math.min(pull / len, 0.5f));
            }
            eyes[i] = c;
        }
        VertexConsumer core = buffers.getBuffer(RenderType.entityTranslucentEmissive(SPHERE));
        for (Vector3f c : eyes) disc(core, c, SPHERE_R * unit * breath, 1f);
        // Ореол и широкое свечение складываются со сценой, порядок не важен.
        VertexConsumer glow = buffers.getBuffer(RenderType.eyes(BLOOM));
        for (Vector3f c : eyes) {
            disc(glow, c, BLOOM_R * unit * breath, 0.8f);
            disc(glow, c, HALO_R * unit * (0.96f + 0.04f * breath), 0.2f * breath);
        }
    }

    /** Квадрат с центром {@code c}, параллельный экрану (координаты камеры), с обеих сторон. */
    private static void disc(VertexConsumer vc, Vector3f c, float r, float k) {
        float[][] q = {{c.x - r, c.y - r, 0, 0}, {c.x - r, c.y + r, 0, 1}, {c.x + r, c.y + r, 1, 1}, {c.x + r, c.y - r, 1, 0}};
        for (int i = 0; i < 4; i++) dv(vc, q[i], c.z, k);
        for (int i = 3; i >= 0; i--) dv(vc, q[i], c.z, k);
    }

    private static void dv(VertexConsumer vc, float[] p, float z, float k) {
        vc.vertex(p[0], p[1], z).color(k, k, k, 1f).uv(p[2], p[3]).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(0, 0, 1).endVertex();
    }

    /** Плоские глаза (вид 0.8.6). */
    private static void renderFlat(PoseStack pose, MultiBufferSource buffers, boolean helmet, boolean gui, float age) {
        // Почти вплотную к лицу (в шлеме — к лицевой части шлема): пятно глаза ровное, ореол чуть впереди.
        float z = (helmet ? -5.04f : -4.04f) * PX;
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float flicker = 0.9f + 0.1f * (float) Math.sin(age * 0.3f);

        // В интерфейсе обычные типы: «подтягивание» к камере сдвинуло бы глаза по экрану.
        java.util.function.Function<ResourceLocation, RenderType> spotType = gui ? RenderType::entityTranslucentEmissive : EyeRenderTypes.SPOT;
        java.util.function.Function<ResourceLocation, RenderType> glowType = gui ? RenderType::eyes : EyeRenderTypes.GLOW;
        VertexConsumer spot = buffers.getBuffer(spotType.apply(SPOT));
        for (float cx : new float[]{-2f, 2f}) face(spot, m, n, cx, EYE_Y, SPOT_SIZE, z, 1f);
        VertexConsumer glow = buffers.getBuffer(glowType.apply(GLOW));
        // Ореол — один слой, тусклее самого глаза; свет вперёд — слабый.
        for (float cx : new float[]{-2f, 2f}) face(glow, m, n, cx, EYE_Y, GLOW_SIZE, z - 0.02f * PX, 0.6f * flicker);
        VertexConsumer beam = buffers.getBuffer(glowType.apply(BEAM));
        for (float cx : new float[]{-2f, 2f}) beam(beam, m, n, cx, EYE_Y, z - 0.05f * PX, 0.3f * flicker);
    }

    /** Квадрат в плоскости лица с центром (cx, cy), с обеих сторон. */
    private static void face(VertexConsumer vc, Matrix4f m, Matrix3f n, float cx, float cy, float size, float z, float k) {
        float x0 = (cx - size / 2) * PX, x1 = (cx + size / 2) * PX, y0 = (cy - size / 2) * PX, y1 = (cy + size / 2) * PX;
        both(vc, m, n, new float[][]{{x0, y0, z, 0, 0}, {x0, y1, z, 0, 1}, {x1, y1, z, 1, 1}, {x1, y0, z, 1, 0}}, k);
    }

    /** Луч вперёд (−z головы): две скрещенные полосы, расширяются от глаза к концу. */
    private static void beam(VertexConsumer vc, Matrix4f m, Matrix3f n, float cx, float cy, float z0, float k) {
        float z1 = z0 - BEAM_LEN * PX;
        float w0 = BEAM_W0 / 2 * PX, w1 = BEAM_W1 / 2 * PX, x = cx * PX, y = cy * PX;
        both(vc, m, n, new float[][]{{x - w0, y, z0, 0, 0}, {x - w1, y, z1, 0, 1}, {x + w1, y, z1, 1, 1}, {x + w0, y, z0, 1, 0}}, k);
        both(vc, m, n, new float[][]{{x, y - w0, z0, 0, 0}, {x, y - w1, z1, 0, 1}, {x, y + w1, z1, 1, 1}, {x, y + w0, z0, 1, 0}}, k);
    }

    private static void both(VertexConsumer vc, Matrix4f m, Matrix3f n, float[][] q, float k) {
        for (int i = 0; i < 4; i++) v(vc, m, n, q[i], k);
        for (int i = 3; i >= 0; i--) v(vc, m, n, q[i], k);
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float[] p, float k) {
        vc.vertex(m, p[0], p[1], p[2]).color(k, k, k, 1f).uv(p[3], p[4]).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(n, 0, 0, -1).endVertex();
    }
}

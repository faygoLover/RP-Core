package faygolover.rpcore.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import faygolover.rpcore.RpCore;
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

/**
 * «Красные глаза»: глаза светятся чисто красным. На месте глаз — красное светящееся пятно (оно же закрывает глаза
 * скина и анимированные глаза текстурпаков), вплотную к лицу (в шлеме — сразу перед шлемом). От каждого глаза вперёд
 * идёт расходящийся луч, как у фонарика в пыльной комнате: у глаза свет сильнее всего (складывается почти до белого),
 * дальше рассеивается. Свечение и лучи складываются со сценой ({@link RenderType#eyes}) — видны в темноте и с
 * шейдерпаком.
 */
public class RedEyesLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation SPOT = new ResourceLocation(RpCore.MODID, "textures/entity/eye_spot.png");
    private static final ResourceLocation GLOW = new ResourceLocation(RpCore.MODID, "textures/entity/eye_glow.png");
    private static final ResourceLocation BEAM = new ResourceLocation(RpCore.MODID, "textures/entity/eye_beam.png");
    private static final float PX = 1f / 16f;
    /** Глаза на лице (пиксели головы, как у скина Стива): x = ±2, y = −3.5. */
    private static final float EYE_Y = -3.5f;
    private static final float SPOT_SIZE = 2.2f, GLOW_SIZE = 3.6f;
    /** Луч: длина и ширина у глаза и в конце (пиксели головы). */
    private static final float BEAM_LEN = 22f, BEAM_W0 = 1.2f, BEAM_W1 = 9f;

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
        float z = (helmet ? -5.06f : -4.52f) * PX;
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float flicker = 0.9f + 0.1f * (float) Math.sin(age * 0.3f);

        VertexConsumer spot = buffers.getBuffer(RenderType.entityTranslucentEmissive(SPOT));
        for (float cx : new float[]{-2f, 2f}) face(spot, m, n, cx, EYE_Y, SPOT_SIZE, z, 1f);
        VertexConsumer glow = buffers.getBuffer(RenderType.eyes(GLOW));
        for (float cx : new float[]{-2f, 2f}) {
            face(glow, m, n, cx, EYE_Y, GLOW_SIZE, z - 0.02f * PX, flicker);
            face(glow, m, n, cx, EYE_Y, GLOW_SIZE * 0.6f, z - 0.03f * PX, flicker);
        }
        VertexConsumer beam = buffers.getBuffer(RenderType.eyes(BEAM));
        for (float cx : new float[]{-2f, 2f}) beam(beam, m, n, cx, EYE_Y, z - 0.05f * PX, 0.55f * flicker);
        pose.popPose();
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

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
 * «Красные глаза»: на месте глаз — два крупных раскалённых красных круга-прожектора. Сам круг непрозрачный и светится
 * в полную силу (закрывает глаза скина и анимированные глаза текстурпаков), вокруг — два слоя красного свечения,
 * которые складываются со светом сцены, как глаза паука ({@link RenderType#eyes}) — видны в темноте и с шейдерпаком.
 * В шлеме — перед шлемом.
 */
public class RedEyesLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation DISC = new ResourceLocation(RpCore.MODID, "textures/entity/eye_disc.png");
    private static final ResourceLocation GLOW = new ResourceLocation(RpCore.MODID, "textures/entity/eye_glow.png");
    private static final float PX = 1f / 16f;
    /** Центры глаз на лице (пиксели головы, как у скина Стива): x = ±2, y = −3.5. */
    private static final float EYE_Y = -3.5f;
    private static final float DISC_SIZE = 2.9f, GLOW_IN = 6.5f, GLOW_OUT = 10f;

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
        float z = (helmet ? -5.35f : -4.65f) * PX;
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float pulse = 0.88f + 0.12f * (float) Math.sin(age * 0.3f);

        VertexConsumer disc = buffers.getBuffer(RenderType.entityTranslucentEmissive(DISC));
        for (float cx : new float[]{-2f, 2f}) quad(disc, m, n, cx, EYE_Y, DISC_SIZE, z, 1f, 1f, 1f, 1f);
        VertexConsumer glow = buffers.getBuffer(RenderType.eyes(GLOW));
        for (float cx : new float[]{-2f, 2f}) {
            quad(glow, m, n, cx, EYE_Y, GLOW_IN, z - 0.02f * PX, pulse, pulse, pulse, 1f);
            quad(glow, m, n, cx, EYE_Y, GLOW_OUT, z - 0.04f * PX, 0.55f * pulse, 0.55f * pulse, 0.55f * pulse, 1f);
        }
        pose.popPose();
    }

    /** Квадрат {@code size}×{@code size} пикселей головы с центром (cx, cy), в плоскости лица, с обеих сторон. */
    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, float cx, float cy, float size, float z, float r, float g, float b, float a) {
        float x0 = (cx - size / 2) * PX, x1 = (cx + size / 2) * PX, y0 = (cy - size / 2) * PX, y1 = (cy + size / 2) * PX;
        v(vc, m, n, x0, y0, z, 0, 0, r, g, b, a);
        v(vc, m, n, x0, y1, z, 0, 1, r, g, b, a);
        v(vc, m, n, x1, y1, z, 1, 1, r, g, b, a);
        v(vc, m, n, x1, y0, z, 1, 0, r, g, b, a);
        v(vc, m, n, x1, y0, z, 1, 0, r, g, b, a);
        v(vc, m, n, x1, y1, z, 1, 1, r, g, b, a);
        v(vc, m, n, x0, y1, z, 0, 1, r, g, b, a);
        v(vc, m, n, x0, y0, z, 0, 0, r, g, b, a);
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float vv, float r, float g, float b, float a) {
        vc.vertex(m, x, y, z).color(r, g, b, a).uv(u, vv).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(n, 0, 0, -1).endVertex();
    }
}

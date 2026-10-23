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
 * «Красные глаза»: на лице игрока два светящихся красных глаза (там, где глаза у скина Стива) с ореолом. Рисуются
 * как глаза паука ({@link RenderType#eyes}) — светятся в темноте и с шейдерпаком. В шлеме — перед шлемом, видно
 * сквозь него.
 */
public class RedEyesLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation GLOW = new ResourceLocation(RpCore.MODID, "textures/entity/eye_glow.png");
    private static final float PX = 1f / 16f;

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
        float z = (helmet ? -5.3f : -4.6f) * PX;
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(GLOW));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float flicker = 0.9f + 0.1f * (float) Math.sin(age * 0.35f);
        for (float cx : new float[]{-2f, 2f}) {
            // Ядро — по размеру глаза скина (2×1), ореол — шире.
            quad(vc, m, n, (cx - 1.1f) * PX, (-4.1f) * PX, (cx + 1.1f) * PX, (-2.9f) * PX, z, 1f, 0.25f, 0.2f);
            quad(vc, m, n, (cx - 2.6f) * PX, (-5.3f) * PX, (cx + 2.6f) * PX, (-1.7f) * PX, z - 0.01f * PX, 0.55f * flicker, 0.04f, 0.03f);
        }
        pose.popPose();
    }

    /** Квадрат в плоскости лица, с обеих сторон. */
    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, float x0, float y0, float x1, float y1, float z, float r, float g, float b) {
        v(vc, m, n, x0, y0, z, 0, 0, r, g, b);
        v(vc, m, n, x0, y1, z, 0, 1, r, g, b);
        v(vc, m, n, x1, y1, z, 1, 1, r, g, b);
        v(vc, m, n, x1, y0, z, 1, 0, r, g, b);
        v(vc, m, n, x1, y0, z, 1, 0, r, g, b);
        v(vc, m, n, x1, y1, z, 1, 1, r, g, b);
        v(vc, m, n, x0, y1, z, 0, 1, r, g, b);
        v(vc, m, n, x0, y0, z, 0, 0, r, g, b);
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float vv, float r, float g, float b) {
        vc.vertex(m, x, y, z).color(r, g, b, 1f).uv(u, vv).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(n, 0, 0, -1).endVertex();
    }
}

package faygolover.rpcore.client.effect;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * Типы отрисовки «Красных глаз»: как у глаз паука и светящихся сущностей, но чуть «подтянутые» к камере (вид
 * сжимается к камере на 2 %: на экране всё на месте, а по глубине — ближе на 0,06 блока в 3 блоках). Так глаза на
 * поверхности лица не прячутся за объёмным вторым слоем скина (3D Skin Layers, Fresh Animations), а стены их
 * по-прежнему закрывают.
 */
final class EyeRenderTypes extends RenderType {
    private EyeRenderTypes() {
        super("rpcore_dummy", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, false, () -> {}, () -> {});
    }

    private static final LayeringStateShard PULL = new LayeringStateShard("rpcore_pull_to_camera", () -> {
        PoseStack mv = RenderSystem.getModelViewStack();
        mv.pushPose();
        mv.scale(0.98f, 0.98f, 0.98f);
        RenderSystem.applyModelViewMatrix();
    }, () -> {
        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();
    });

    /** Само пятно глаза: ровный цвет в полную силу, не зависит от света. */
    static final Function<ResourceLocation, RenderType> SPOT = Util.memoize(tex -> create("rpcore_eye_spot",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new TextureStateShard(tex, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOverlayState(OVERLAY)
                    .setLayeringState(PULL)
                    .createCompositeState(false)));

    /** Ореол и свет вперёд: складываются со сценой. */
    static final Function<ResourceLocation, RenderType> GLOW = Util.memoize(tex -> create("rpcore_eye_glow",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTextureState(new TextureStateShard(tex, false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setLayeringState(PULL)
                    .createCompositeState(false)));
}

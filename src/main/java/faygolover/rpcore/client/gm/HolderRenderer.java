package faygolover.rpcore.client.gm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import faygolover.rpcore.gm.HolderBlockEntity;
import faygolover.rpcore.gm.WallHolderBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Предмет в держателе. Настенный: flat — как в рамке (прижат к стене), block — блок в натуральную величину задом к
 * стене. Сферический: item — как выброшенный предмет, flat — плоский спрайт, block — блок в натуральную величину.
 * Поворот, наклон, вращение и покачивание — из настроек держателя.
 */
public class HolderRenderer implements BlockEntityRenderer<HolderBlockEntity> {
    public HolderRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(HolderBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = be.item;
        if (stack.isEmpty() || be.getLevel() == null) return;
        float time = be.getLevel().getGameTime() + partial;
        float spin = be.spin * time / 20f;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        BlockState state = be.getBlockState();
        boolean block = "block".equals(be.render) && stack.getItem() instanceof BlockItem;
        if (state.getBlock() instanceof WallHolderBlock) {
            Direction facing = state.getValue(WallHolderBlock.FACING);
            // Как ItemFrameRenderer: повернуть «лицом» от стены, предмет у самой стены.
            float xRot = facing.getAxis().isVertical() ? -90f * facing.getAxisDirection().getStep() : 0f;
            float yRot = facing.getAxis().isHorizontal() ? facing.get2DDataValue() * 90f : 0f;
            pose.mulPose(Axis.XP.rotationDegrees(xRot));
            pose.mulPose(Axis.YP.rotationDegrees(180f - yRot));
            if (block) {
                float s = be.scale;
                pose.translate(0, 0, 0.5 - s / 2);
                pose.mulPose(Axis.ZP.rotationDegrees(be.rot + spin));
                pose.mulPose(Axis.YP.rotationDegrees(180f));
                renderBlock(stack, s, pose, buffers, light);
            } else {
                pose.translate(0, 0, 0.4375);
                pose.mulPose(Axis.ZP.rotationDegrees(be.rot + spin));
                float s = 0.5f * be.scale;
                pose.scale(s, s, s);
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                        pose, buffers, be.getLevel(), (int) be.getBlockPos().asLong());
            }
        } else {
            if (be.bob) pose.translate(0, Mth.sin(time / 10f) * 0.05f * be.scale, 0);
            pose.mulPose(Axis.YP.rotationDegrees(be.rot + spin));
            pose.mulPose(Axis.XP.rotationDegrees(be.tilt));
            if (block) {
                renderBlock(stack, be.scale, pose, buffers, light);
            } else if ("flat".equals(be.render)) {
                float s = 0.5f * be.scale;
                pose.scale(s, s, s);
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                        pose, buffers, be.getLevel(), (int) be.getBlockPos().asLong());
            } else {
                // Как выброшенный предмет: GROUND — модели сами ставят свой размер, центр модели в центре блока.
                float s = be.scale;
                pose.scale(s, s, s);
                pose.translate(0, -0.125, 0);
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY,
                        pose, buffers, be.getLevel(), (int) be.getBlockPos().asLong());
            }
        }
        pose.popPose();
    }

    /** Блок предмета в натуральную величину ({@code scale} = 1 — целый блок), центр — в текущей точке. */
    private static void renderBlock(ItemStack stack, float scale, PoseStack pose, MultiBufferSource buffers, int light) {
        BlockState bs = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        pose.scale(scale, scale, scale);
        pose.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(bs, pose, buffers, light, OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY, null);
    }

    @Override
    public boolean shouldRenderOffScreen(HolderBlockEntity be) {
        return be.scale > 1.0f;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}

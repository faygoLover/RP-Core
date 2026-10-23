package faygolover.rpcore.client.gm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import faygolover.rpcore.gm.GmBlock;
import faygolover.rpcore.gm.GmConfigurable;
import faygolover.rpcore.gm.GmMarkerSet;
import faygolover.rpcore.gm.GmTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Подсветка ведущему: с меткой в руке ({@link GmTools#MARKERS}) видны контуры всех «ГМских штук» в радиусе 48 и
 * подписи (имя пластины или портала, предмет держателя). Игрокам без метки не рисуется рамка выделения этих блоков.
 */
public final class GmOverlay {
    private GmOverlay() {}

    private static final double RADIUS_SQR = 48 * 48;
    private static final double LABEL_SQR = 16 * 16;

    static void onHighlight(RenderHighlightEvent.Block e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        BlockState state = mc.level.getBlockState(e.getTarget().getBlockPos());
        if (state.getBlock() instanceof GmBlock && !GmTools.holdsMarker(mc.player)) e.setCanceled(true);
    }

    static void onRenderLevel(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || !GmTools.holdsMarker(player)) return;
        BlockEntity[] all = GmMarkerSet.snapshot();
        if (all.length == 0) return;
        Vec3 cam = e.getCamera().getPosition();
        PoseStack pose = e.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        CollisionContext ctx = CollisionContext.of(player);
        for (BlockEntity be : all) {
            if (be.isRemoved() || be.getLevel() != mc.level) continue;
            BlockPos pos = be.getBlockPos();
            if (pos.getCenter().distanceToSqr(cam) > RADIUS_SQR) continue;
            BlockState state = be.getBlockState();
            if (!(state.getBlock() instanceof GmBlock gb)) continue;
            VoxelShape shape = state.getShape(mc.level, pos, ctx);
            AABB box = shape.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : shape.bounds();
            box = box.move(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z).inflate(0.002);
            int c = gb.markerColor();
            LevelRenderer.renderLineBox(pose, lines, box, (c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, 0.9f);
        }
        buffers.endBatch(RenderType.lines());
        // Подписи — вблизи, поверх всего.
        Font font = mc.font;
        for (BlockEntity be : all) {
            if (be.isRemoved() || be.getLevel() != mc.level || !(be instanceof GmConfigurable gc)) continue;
            String label = gc.markerLabel();
            if (label.isEmpty()) continue;
            Vec3 at = be.getBlockPos().getCenter().add(0, 0.7, 0);
            if (at.distanceToSqr(cam) > LABEL_SQR) continue;
            pose.pushPose();
            pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
            pose.mulPose(e.getCamera().rotation());
            pose.scale(-0.025f, -0.025f, 0.025f);
            Matrix4f m = pose.last().pose();
            float x = -font.width(label) / 2f;
            font.drawInBatch(label, x, 0, 0xFFFFFFFF, false, m, buffers, Font.DisplayMode.SEE_THROUGH, 0x60000000, LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
        buffers.endBatch();
    }
}

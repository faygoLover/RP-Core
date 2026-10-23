package faygolover.rpcore.gm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Невидимая нажимная пластина: без модели, коллизии, звука шагов и рамки выделения. Ведущий с меткой в руке видит
 * её контур, может выделить, сломать и открыть настройки (ПКМ меткой). Замечает только тех, кого велено
 * ({@link HiddenPlateBlockEntity#trigger}) в своей области, и отпускает в тот же тик, как они сошли.
 */
public class HiddenPlateBlock extends Block implements EntityBlock, GmBlock {
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    private static final VoxelShape GM_SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public HiddenPlateBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(POWER);
    }

    @Override
    public int markerColor() {
        return 0xFFB020;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return GmTools.markerContext(ctx) ? GM_SHAPE : Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return true;
    }

    /** Стрелка — куда смотрел поставивший (по сторонам света; точнее — в настройках). */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        if (placer != null && level.getBlockEntity(pos) instanceof HiddenPlateBlockEntity be) {
            be.yaw = net.minecraft.util.Mth.wrapDegrees(Math.round(placer.getYRot() / 90f) * 90f);
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!GmTools.holdsMarker(player)) return InteractionResult.PASS;
        if (!level.isClientSide && player instanceof ServerPlayer sp) GmSettings.open(sp, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
        return state.getValue(POWER);
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
        return dir == Direction.UP ? state.getValue(POWER) : 0;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        boolean wasPowered = state.getValue(POWER) > 0;
        super.onRemove(state, level, pos, newState, moved);
        if (!state.is(newState.getBlock()) && wasPowered) {
            level.updateNeighborsAt(pos, this);
            level.updateNeighborsAt(pos.below(), this);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HiddenPlateBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != GmRegistry.HIDDEN_PLATE_BE.get() ? null
                : (BlockEntityTicker<T>) (BlockEntityTicker<HiddenPlateBlockEntity>) HiddenPlateBlockEntity::serverTick;
    }
}

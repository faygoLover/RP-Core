package faygolover.rpcore.gm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Настенный держатель — как рамка: предмет прижат к грани блока-опоры ({@link #FACING} — куда смотрит предмет,
 * опора сзади). Сломали опору — предмет выпадает, держатель пропадает.
 */
public class WallHolderBlock extends HolderBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public WallHolderBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(COLLIDE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(FACING);
    }

    @Override
    public int markerColor() {
        return 0x7CFF6A;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState().setValue(FACING, ctx.getClickedFace());
        return canSurvive(s, ctx.getLevel(), ctx.getClickedPos()) ? s : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos support = pos.relative(state.getValue(FACING).getOpposite());
        BlockState s = level.getBlockState(support);
        return !s.isAir() && !s.canBeReplaced() && !(s.getBlock() instanceof HolderBlock);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (dir == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape shape(BlockState state, @Nullable HolderBlockEntity be) {
        float size = be == null ? 0.6f : be.size();
        double half = Math.min(size, 1.0) / 2;
        double depth = be != null && "block".equals(be.render) ? Math.min(size, 1.0) : 1.0 / 16;
        return box(state.getValue(FACING), half, depth);
    }

    /** Квадрат со стороной {@code 2*half} у стены, выступает на {@code depth} в сторону {@code facing}. */
    static VoxelShape box(Direction facing, double half, double depth) {
        double lo = 0.5 - half, hi = 0.5 + half;
        double a = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 0 : 1 - depth;
        double b = a + depth;
        return switch (facing.getAxis()) {
            case X -> Shapes.box(a, lo, lo, b, hi, hi);
            case Y -> Shapes.box(lo, a, lo, hi, b, hi);
            case Z -> Shapes.box(lo, lo, a, hi, hi, b);
        };
    }
}

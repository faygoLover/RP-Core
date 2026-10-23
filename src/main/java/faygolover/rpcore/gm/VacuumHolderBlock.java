package faygolover.rpcore.gm;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Сферический держатель в вакууме: предмет висит в центре блока, опора не нужна. */
public class VacuumHolderBlock extends HolderBlock {
    public VacuumHolderBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(COLLIDE, false));
    }

    @Override
    public int markerColor() {
        return 0xC080FF;
    }

    @Override
    protected VoxelShape shape(BlockState state, @Nullable HolderBlockEntity be) {
        double half = Math.min(be == null ? 0.5f : be.size(), 1.0) / 2;
        return Shapes.box(0.5 - half, 0.5 - half, 0.5 - half, 0.5 + half, 0.5 + half, 0.5 + half);
    }
}

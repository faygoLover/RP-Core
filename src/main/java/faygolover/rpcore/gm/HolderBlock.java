package faygolover.rpcore.gm;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Держатель предмета: сам невидим (предмет рисует {@code HolderRenderer}), не ломается ничем, кроме ведущего,
 * не сдвигается поршнем. ПКМ — забрать предмет целиком (со всеми данными); ведущий кладёт предмет ПКМ по пустому
 * держателю, а меткой в руке открывает настройки. Коллизия для движения — по настройке ({@link #COLLIDE}).
 */
public abstract class HolderBlock extends Block implements EntityBlock, GmBlock {
    public static final BooleanProperty COLLIDE = BooleanProperty.create("collide");

    protected HolderBlock(Properties props) {
        super(props);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(COLLIDE);
    }

    /** Форма видимой части при этом держателе. */
    protected abstract VoxelShape shape(BlockState state, @Nullable HolderBlockEntity be);

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shape(state, level.getBlockEntity(pos) instanceof HolderBlockEntity be ? be : null);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(COLLIDE) ? getShape(state, level, pos, ctx) : Shapes.empty();
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
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
        return !state.getValue(COLLIDE);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (GmTools.holdsMarker(player)) {
            if (!level.isClientSide && player instanceof ServerPlayer sp) GmSettings.open(sp, pos);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!(level.getBlockEntity(pos) instanceof HolderBlockEntity be)) return InteractionResult.PASS;
        if (level.isClientSide) return be.item.isEmpty() && player.getItemInHand(hand).isEmpty() ? InteractionResult.PASS : InteractionResult.SUCCESS;
        boolean gm = GmTools.isGm(player);
        if (be.item.isEmpty()) {
            ItemStack held = player.getItemInHand(hand);
            if (!gm || held.isEmpty()) return InteractionResult.PASS;
            be.setItem(player.getAbilities().instabuild ? held.copy() : held.split(held.getCount()));
            return InteractionResult.CONSUME;
        }
        if ("gm".equals(be.takeBy) && !gm) return InteractionResult.PASS;
        ItemStack give = be.item.copy();
        boolean stays = be.taken();
        if (!player.getInventory().add(give)) player.drop(give, false);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.4f);
        if (!stays) level.removeBlock(pos, false);
        return InteractionResult.CONSUME;
    }

    /** Держатель убрали не взятием (сломали опору, ведущий сломал) — предмет выпадает. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof HolderBlockEntity be && !be.item.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, be.item.copy());
            be.item = ItemStack.EMPTY;
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HolderBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != GmRegistry.HOLDER_BE.get() ? null
                : (BlockEntityTicker<T>) (BlockEntityTicker<HolderBlockEntity>) HolderBlockEntity::serverTick;
    }
}

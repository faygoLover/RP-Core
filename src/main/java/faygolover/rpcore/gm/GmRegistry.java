package faygolover.rpcore.gm;

import faygolover.rpcore.RpCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Блоки и предметы RP Core: «ГМские штуки» (невидимая пластина, настенный и сферический держатели) и общая вкладка
 * креатива «RP: разное» ({@link #MISC_TAB_KEY}) — в неё кладут свои немногие предметы и другие моды семейства.
 */
public final class GmRegistry {
    private GmRegistry() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RpCore.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RpCore.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RpCore.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RpCore.MODID);

    /** Вкладка «RP: разное». Другие моды: {@code BuildCreativeModeTabContentsEvent}, {@code event.getTabKey() == MISC_TAB_KEY}. */
    public static final ResourceKey<CreativeModeTab> MISC_TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            new ResourceLocation(RpCore.MODID, "misc"));

    /** Невидимо, не ломается ничем, кроме ведущего (в творческом), не двигается поршнем, без добычи. */
    private static BlockBehaviour.Properties gmBlock() {
        return BlockBehaviour.Properties.of().noOcclusion().strength(-1.0f, 3600000.0f).noLootTable()
                .pushReaction(PushReaction.BLOCK).sound(SoundType.STONE)
                .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false).isRedstoneConductor((s, l, p) -> false);
    }

    public static final RegistryObject<Block> HIDDEN_PLATE = BLOCKS.register("hidden_plate",
            () -> new HiddenPlateBlock(gmBlock().noCollission()));
    public static final RegistryObject<Block> WALL_HOLDER = BLOCKS.register("wall_holder",
            () -> new WallHolderBlock(gmBlock().dynamicShape()));
    public static final RegistryObject<Block> VACUUM_HOLDER = BLOCKS.register("vacuum_holder",
            () -> new VacuumHolderBlock(gmBlock().dynamicShape()));

    public static final RegistryObject<Item> HIDDEN_PLATE_ITEM = ITEMS.register("hidden_plate",
            () -> new BlockItem(HIDDEN_PLATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> WALL_HOLDER_ITEM = ITEMS.register("wall_holder",
            () -> new BlockItem(WALL_HOLDER.get(), new Item.Properties()));
    public static final RegistryObject<Item> VACUUM_HOLDER_ITEM = ITEMS.register("vacuum_holder",
            () -> new BlockItem(VACUUM_HOLDER.get(), new Item.Properties()));
    /** Бирки (метки на предметах, {@link ItemMarks}): выдаёт ГМ, без рецептов. */
    public static final RegistryObject<Item> OWNER_TAG = ITEMS.register("owner_tag",
            () -> new ItemMarks.TagItem(new Item.Properties().stacksTo(64), true));
    public static final RegistryObject<Item> NUMBER_TAG = ITEMS.register("number_tag",
            () -> new ItemMarks.TagItem(new Item.Properties().stacksTo(64), false));

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<HiddenPlateBlockEntity>> HIDDEN_PLATE_BE = BLOCK_ENTITIES.register("hidden_plate",
            () -> BlockEntityType.Builder.of(HiddenPlateBlockEntity::new, HIDDEN_PLATE.get()).build(null));
    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<HolderBlockEntity>> HOLDER_BE = BLOCK_ENTITIES.register("holder",
            () -> BlockEntityType.Builder.of(HolderBlockEntity::new, WALL_HOLDER.get(), VACUUM_HOLDER.get()).build(null));

    public static final RegistryObject<CreativeModeTab> MISC_TAB = TABS.register("misc", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.rpcore.misc"))
            .icon(() -> new ItemStack(VACUUM_HOLDER_ITEM.get()))
            .displayItems((params, out) -> {
                // Видимость — как у всех предметов семейства: в сборке с замком их видят только ведущие.
                out.accept(HIDDEN_PLATE_ITEM.get());
                out.accept(WALL_HOLDER_ITEM.get());
                out.accept(VACUUM_HOLDER_ITEM.get());
                out.accept(OWNER_TAG.get());
                out.accept(NUMBER_TAG.get());
            })
            .build());

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        TABS.register(modBus);
    }
}

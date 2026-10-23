package faygolover.rpcore.effect;

import faygolover.rpcore.RpCore;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Эффекты RP Core (0.8.1) и зелья к ним. Частиц у носителя нет никогда (см. {@link EffectSync}).
 * <ul>
 *   <li>«Красные глаза» — глаза игрока горят красным, видно и сквозь шлем. Зелье: неловкое + светящийся чернильный
 *   мешок (3 мин), с красной пылью — 8 мин.</li>
 *   <li>«Помехи» — на носителе не видно ничего, а у тех, кто рядом, по экрану рябь и помехи, тем сильнее, чем ближе
 *   (радиус 12 блоков, II — 24). Зелье: неловкое + осколок аметиста (3 мин), с пылью — 8 мин, со светокаменной
 *   пылью — «Помехи II» (1,5 мин).</li>
 * </ul>
 */
public final class RpEffects {
    private RpEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, RpCore.MODID);
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, RpCore.MODID);

    public static final RegistryObject<MobEffect> RED_EYES = EFFECTS.register("red_eyes", () -> new Plain(MobEffectCategory.NEUTRAL, 0xD01818));
    public static final RegistryObject<MobEffect> INTERFERENCE = EFFECTS.register("interference", () -> new Plain(MobEffectCategory.NEUTRAL, 0x6E7F74));

    public static final RegistryObject<Potion> RED_EYES_POTION = POTIONS.register("red_eyes",
            () -> new Potion("red_eyes", new MobEffectInstance(RED_EYES.get(), 3600)));
    public static final RegistryObject<Potion> LONG_RED_EYES_POTION = POTIONS.register("long_red_eyes",
            () -> new Potion("red_eyes", new MobEffectInstance(RED_EYES.get(), 9600)));
    public static final RegistryObject<Potion> INTERFERENCE_POTION = POTIONS.register("interference",
            () -> new Potion("interference", new MobEffectInstance(INTERFERENCE.get(), 3600)));
    public static final RegistryObject<Potion> LONG_INTERFERENCE_POTION = POTIONS.register("long_interference",
            () -> new Potion("interference", new MobEffectInstance(INTERFERENCE.get(), 9600)));
    public static final RegistryObject<Potion> STRONG_INTERFERENCE_POTION = POTIONS.register("strong_interference",
            () -> new Potion("interference", new MobEffectInstance(INTERFERENCE.get(), 1800, 1)));

    /** Эффект без своего действия: всё — в отрисовке у клиентов. */
    private static final class Plain extends MobEffect {
        Plain(MobEffectCategory category, int color) {
            super(category, color);
        }
    }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
        POTIONS.register(modBus);
    }

    /** Рецепты варки (из FMLCommonSetupEvent → enqueueWork). */
    public static void registerBrewing() {
        brew(Potions.AWKWARD, Items.GLOW_INK_SAC, RED_EYES_POTION.get());
        brew(RED_EYES_POTION.get(), Items.REDSTONE, LONG_RED_EYES_POTION.get());
        brew(Potions.AWKWARD, Items.AMETHYST_SHARD, INTERFERENCE_POTION.get());
        brew(INTERFERENCE_POTION.get(), Items.REDSTONE, LONG_INTERFERENCE_POTION.get());
        brew(INTERFERENCE_POTION.get(), Items.GLOWSTONE_DUST, STRONG_INTERFERENCE_POTION.get());
    }

    private static void brew(Potion from, ItemLike ingredient, Potion to) {
        for (var bottle : new net.minecraft.world.item.Item[]{Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION}) {
            ItemStack in = PotionUtils.setPotion(new ItemStack(bottle), from);
            ItemStack out = PotionUtils.setPotion(new ItemStack(bottle), to);
            BrewingRecipeRegistry.addRecipe(StrictNBTIngredient.of(in), Ingredient.of(ingredient), out);
        }
    }
}

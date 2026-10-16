package faygolover.rpcore.client.access;

import faygolover.rpcore.RpCore;
import faygolover.rpcore.access.RpAccess;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.CreativeModeTabs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** JEI для замка семейства: без доступа прячет предметы RP-модов и рецепты с ними, с доступом возвращает. */
@JeiPlugin
public class CoreJeiPlugin implements IModPlugin {
    private static final List<RecipeType<? extends Recipe<?>>> RECIPE_TYPES = List.of(RecipeTypes.CRAFTING, RecipeTypes.SMELTING,
            RecipeTypes.BLASTING, RecipeTypes.SMOKING, RecipeTypes.CAMPFIRE_COOKING, RecipeTypes.STONECUTTING, RecipeTypes.SMITHING);

    private static IJeiRuntime runtime;
    private static boolean hidden;
    private static final List<ItemStack> HIDDEN_STACKS = new ArrayList<>();
    private static final Map<RecipeType<?>, List<Recipe<?>>> HIDDEN_RECIPES = new LinkedHashMap<>();

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(RpCore.MODID, "access");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        hidden = false;
        HIDDEN_STACKS.clear();
        HIDDEN_RECIPES.clear();
        ClientAccess.setJeiRefresh(CoreJeiPlugin::refresh);
        refresh();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        ClientAccess.setJeiRefresh(() -> {});
    }

    private static void refresh() {
        if (runtime == null || !RpAccess.lockEnabled()) return;
        try {
            if (!RpAccess.isUnlocked() && !hidden) hide();
            else if (RpAccess.isUnlocked() && hidden) show();
        } catch (Exception e) {
            RpCore.LOGGER.error("RP Core: JEI не обновлён", e);
        }
    }

    private static void hide() {
        var im = runtime.getIngredientManager();
        List<ItemStack> stacks = im.getAllIngredients(VanillaTypes.ITEM_STACK).stream().filter(RpAccess::isHidden).toList();
        if (!stacks.isEmpty()) im.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, stacks);
        HIDDEN_STACKS.addAll(stacks);
        var level = Minecraft.getInstance().level;
        var rm = runtime.getRecipeManager();
        for (var type : RECIPE_TYPES) hideRecipes(rm, type, level != null ? level.registryAccess() : null);
        hidden = true;
    }

    private static <R extends Recipe<?>> void hideRecipes(IRecipeManager rm, RecipeType<R> type, net.minecraft.core.RegistryAccess access) {
        List<R> list = rm.createRecipeLookup(type).get()
                .filter(r -> RpAccess.isHiddenNamespace(r.getId().getNamespace())
                        || access != null && RpAccess.isHidden(r.getResultItem(access)))
                .toList();
        if (list.isEmpty()) return;
        rm.hideRecipes(type, list);
        HIDDEN_RECIPES.put(type, new ArrayList<>(list));
    }

    @SuppressWarnings("unchecked")
    private static void show() {
        var im = runtime.getIngredientManager();
        var helper = im.getIngredientHelper(VanillaTypes.ITEM_STACK);
        // Что вернуть: всё спрятанное плюс предметы, которых в JEI не было с самого начала (креатив уже был без них, а сейчас пересобран).
        Set<String> present = new HashSet<>();
        for (ItemStack s : im.getAllIngredients(VanillaTypes.ITEM_STACK)) {
            if (RpAccess.isHidden(s)) present.add(helper.getUniqueId(s, UidContext.Ingredient).toString());
        }
        List<ItemStack> add = new ArrayList<>();
        List<ItemStack> candidates = new ArrayList<>(HIDDEN_STACKS);
        candidates.addAll(CreativeModeTabs.searchTab().getDisplayItems());
        for (ItemStack s : candidates) {
            if (!RpAccess.isHidden(s)) continue;
            if (present.add(helper.getUniqueId(s, UidContext.Ingredient).toString())) add.add(s);
        }
        if (!add.isEmpty()) im.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, add);
        HIDDEN_STACKS.clear();
        var rm = runtime.getRecipeManager();
        HIDDEN_RECIPES.forEach((type, list) -> rm.unhideRecipes((RecipeType<Recipe<?>>) type, list));
        HIDDEN_RECIPES.clear();
        hidden = false;
    }
}

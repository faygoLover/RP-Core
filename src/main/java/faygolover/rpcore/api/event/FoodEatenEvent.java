package faygolover.rpcore.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Игрок съел или выпил (сервер). Шлёт мод питания (RP Culinary) — уже посчитанный состав порции. RP Medicine
 * по углеводам поднимает сахар диабетика. {@code food} — что съедено (может быть пустым: блюдо-блок, торт).
 */
public class FoodEatenEvent extends PlayerEvent {
    private final ItemStack food;
    private final double kcal, protein, fat, carbs, vitamins, water;

    public FoodEatenEvent(Player player, ItemStack food, double kcal, double protein, double fat, double carbs, double vitamins, double water) {
        super(player);
        this.food = food;
        this.kcal = kcal;
        this.protein = protein;
        this.fat = fat;
        this.carbs = carbs;
        this.vitamins = vitamins;
        this.water = water;
    }

    public ItemStack food() {
        return food;
    }

    public double kcal() {
        return kcal;
    }

    public double protein() {
        return protein;
    }

    public double fat() {
        return fat;
    }

    /** Углеводы, г. */
    public double carbs() {
        return carbs;
    }

    public double vitamins() {
        return vitamins;
    }

    /** Вода, проценты 0–100. */
    public double water() {
        return water;
    }
}

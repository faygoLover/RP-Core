# Общая база напитков, еды и курева RP Core (data/rpcore/rpcore/consumables): вода, кофеин, алкоголь, никотин.
# Читают RP Culinary, RP Medicine, RP Stamina, RP Perks, RP Psyche. Формат — javadoc faygolover.rpcore.api.Consumables.
# Запуск: python scripts/gen_consumables.py — перезаписывает папку consumables целиком.
import json
import os
import shutil

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "main", "resources", "data", "rpcore", "rpcore", "consumables")


def ns(n, *ids):
    return [f"{n}:{i}" for i in ids]


FILES = {}


def add(name, items=None, **props):
    obj = {}
    if items:
        obj["items"] = items
    for k in ("right_click", "effects"):
        if k in props:
            obj[k] = props.pop(k)
    obj.update(props)
    FILES[name] = obj


# ------------------------------------------------------------------ вода (без калорий: вода не кормит)
add("water_bottle", ["minecraft:potion"], potion="minecraft:water", water=35, calories=False)
add("water_bottles", ns("sa_core", "water_bottle") + ns("survival_instinct", "bottle_of_water")
    + ns("legendarysurvivaloverhaul", "purified_water_bottle", "canteen", "canteen_purified", "large_canteen", "large_canteen_purified"),
    water=35, calories=False)
add("water_dirty", ns("sa_core", "dirty_water_bottle"), water=30, calories=False)
add("water_gallon", ns("survival_instinct", "gallon_of_water"), water=70, calories=False)

# ------------------------------------------------------------------ молоко
add("milk", ["minecraft:milk_bucket"] + ns("survival_instinct", "milk"), water=25)
add("milk_gallon", ns("survival_instinct", "gallon_of_milk"), water=50)
add("milk_canned", ns("sa_combat", "canned_evaporated_milk"), water=10)

# ------------------------------------------------------------------ соки
add("juices", ns("vinery", "apple_juice", "red_grapejuice", "white_grapejuice", "red_jungle_grapejuice",
                 "white_jungle_grapejuice", "red_savanna_grapejuice", "white_savanna_grapejuice",
                 "red_taiga_grapejuice", "white_taiga_grapejuice")
    + ns("legendarysurvivaloverhaul", "apple_juice", "beetroot_juice", "cactus_juice", "carrot_juice",
         "chorus_fruit_juice", "glistering_melon_juice", "glow_berries_juice", "golden_apple_juice",
         "golden_carrot_juice", "melon_juice", "pumpkin_juice", "sweet_berries_juice")
    + ns("sa_combat", "apple_juice_box", "berry_juice_box", "grape_juice_box", "orange_juice_box",
         "peach_juice_bottle", "pineapple_juice_bottle")
    + ns("survival_instinct", "apple_juice", "orange_juice")
    + ns("farmersdelight", "melon_juice"),
    water=25)

# ------------------------------------------------------------------ газировка
add("soda", ns("sa_combat", "fanta_grape_soda", "fanta_orange_soda")
    + ns("survival_instinct", "blue_soda", "lemon_soda", "red_soda")
    + ns("alexscaves", "purple_soda_bottle")
    + ns("scpo", "carmin_soda", "cotton_soda_can", "stitch_soda", "redacted_soda_can")
    + ns("cybernetic_system", "soda_absorption", "soda_haste", "soda_strenght"),
    water=20)
add("cola", ns("sa_combat", "cola"), water=20, caffeine="trace")
add("guarana", ns("sa_combat", "guarana_soda"), water=20, caffeine="weak")

# ------------------------------------------------------------------ кофеин
add("coffee", ns("herbalbrews", "coffee") + ns("sa_combat", "coffee") + ns("scpo", "papercup_coffee"),
    water=20, caffeine="strong")
add("coffee_milk", ns("herbalbrews", "milk_coffee"), water=20, caffeine=0.8)
add("coffee_can", ns("scpo", "coffee_can"), water=20, caffeine=0.8)
add("energy_drink", ns("sa_combat", "energy_drink") + ns("survival_instinct", "energy_can"), water=20, caffeine=1.0)
add("energy_monster", ns("survival_instinct", "monster_can"), water=20, caffeine=1.3)
add("tea_caffeine", ns("herbalbrews", "black_tea", "green_tea", "oolong_tea", "yerba_mate_tea"), water=25, caffeine="weak")
add("tea_herbal", ns("herbalbrews", "hibiscus_tea", "lavender_tea", "rooibos_tea")
    + ns("farm_and_charm", "strawberry_tea", "strawberry_tea_cup", "nettle_tea", "nettle_tea_cup", "ribwort_tea", "ribwort_tea_cup"),
    water=25)
add("cocoa", ns("farmersdelight", "hot_cocoa") + ns("alexscaves", "hot_chocolate_bottle"), water=20, caffeine="trace")
add("chocolate", ["minecraft:cookie"] + ns("sa_combat", "chocolate_bar") + ns("survival_instinct", "chocolate_bar")
    + ns("farmersdelight", "chocolate_pie_slice"), caffeine="trace")

# ------------------------------------------------------------------ алкоголь
add("beer", ns("brewery", "beer_barley", "beer_haley", "beer_hops", "beer_nettle", "beer_oat", "beer_wheat")
    + ns("sa_combat", "beer", "duff_beer") + ns("survival_instinct", "beer"),
    water=15, alcohol="weak")
add("cider", ns("vinery", "apple_cider", "kelp_cider", "mead") + ns("farmersdelight", "apple_cider"),
    water=15, alcohol="weak")
add("wine", ns("vinery", "aegis_wine", "apple_wine", "bolvar_wine", "bottle_mojang_noir", "chenet_wine", "cherry_wine",
               "chorus_wine", "clark_wine", "cristel_wine", "glowing_wine", "jellie_wine", "lilitu_wine", "magnetic_wine",
               "mellohi_wine", "noir_wine", "red_wine", "solaris_wine", "stal_wine", "strad_wine", "villagers_fright")
    + ns("sa_combat", "wine") + ns("survival_instinct", "wine"),
    water=5, alcohol="medium")
add("strong_alcohol", ns("brewery", "whiskey_ak", "whiskey_carrasconlabel", "whiskey_cristelwalker", "whiskey_highland_hearth",
                         "whiskey_jamesons_malt", "whiskey_jojannik", "whiskey_lilitusinglemalt", "whiskey_maggoallan",
                         "whiskey_smokey_reverie", "dark_brew")
    + ns("sa_combat", "vodka", "whiskey") + ns("survival_instinct", "whiskey") + ns("zombie_extreme", "vodka", "whiskey"),
    water=-5, alcohol="strong")

# ------------------------------------------------------------------ никотин
add("tobacconist", effects=["tobacconistmod:nicotine"], nicotine=0.4, cooldown_seconds=30)
add("dirty_stuff_tobacco", right_click=ns("the_dirty_stuff",
    "black_light_cigarette", "black_slim_cigar", "blue_cigar", "blue_cigarette", "cigar", "cigarette",
    "light_blue_cigar", "light_blue_cigarette", "light_gray_cigar", "light_gray_cigarette",
    "light_gray_light_cigarette", "light_gray_slim_cigar", "light_gray_ultralight_cigarette",
    "light_gray_ultraslim_cigar", "magenta_cigar", "magenta_cigarette", "orange_cigar", "orange_cigarette",
    "orange_ultralight_cigarette", "orange_ultraslim_cigar", "pink_ultralight_cigarette", "pink_ultraslim_cigar",
    "red_cigar", "red_cigarette", "red_ultralight_cigarette", "red_ultraslim_cigar", "white_light_cigarette",
    "white_slim_cigar", "yellow_cigar", "yellow_cigarette"), nicotine=1.0, cooldown_seconds=60)

# ------------------------------------------------------------------ прочее, что немного поит
add("honey", ["minecraft:honey_bottle"], water=10)
add("melon", ["minecraft:melon_slice"], water=5)
add("vanilla_soups", ["minecraft:mushroom_stew", "minecraft:beetroot_soup", "minecraft:rabbit_stew", "minecraft:suspicious_stew"], water=10)

if __name__ == "__main__":
    if os.path.isdir(OUT):
        shutil.rmtree(OUT)
    os.makedirs(OUT)
    seen = {}
    for name, obj in FILES.items():
        for k in ("items", "right_click"):
            for i in obj.get(k, []):
                if (k, i) in seen and "potion" not in obj:
                    raise SystemExit(f"{i} в {seen[(k, i)]} и {name}")
                seen[(k, i)] = name
        with open(os.path.join(OUT, name + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(obj, f, ensure_ascii=False, indent=2)
            f.write("\n")
    print(f"consumables: {len(FILES)} файлов, {len(seen)} предметов")

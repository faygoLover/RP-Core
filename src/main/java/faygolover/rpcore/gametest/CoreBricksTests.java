package faygolover.rpcore.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import faygolover.rpcore.RpCore;
import faygolover.rpcore.api.BodyProvider;
import faygolover.rpcore.api.MindProvider;
import faygolover.rpcore.api.RpCoreAPI;
import faygolover.rpcore.api.TraitProvider;
import faygolover.rpcore.api.Worn;
import faygolover.rpcore.api.data.Matcher;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Малые общие механизмы RP Core 0.15: надетое ({@link Worn}) без Curios, эталон времени тела, {@link Matcher},
 * розетки с ответами по умолчанию (тело, психика, знания, свет), выдача достижения по id.
 */
@GameTestHolder(RpCore.MODID)
@PrefixGameTestTemplate(false)
public final class CoreBricksTests {
    private CoreBricksTests() {}

    static final String T = "platform";

    /** Без Curios (тесты Core идут без него) все методы отвечают «ничего не надето» и не падают. */
    @GameTest(template = T, batch = "core_bricks_1")
    public static void wornWithoutCurios(GameTestHelper h) {
        ServerPlayer p = PlayerStoreTests.player(h);
        try {
            if (Worn.loaded()) { h.fail("Тест рассчитан на запуск без Curios"); return; }
            if (Worn.any(p, s -> true) || Worn.anyIn(p, List.of("head"), s -> true)) h.fail("Без Curios что-то «надето»");
            if (!Worn.all(p, s -> true).isEmpty() || !Worn.all(p, s -> true, "pda").isEmpty()) h.fail("Без Curios список надетого не пуст");
            if (!Worn.first(p, "head").isEmpty() || !Worn.inSlot(p, "head").isEmpty()) h.fail("Без Curios в слоте что-то есть");
            Worn.touched(p, "head");
        } finally {
            PlayerStoreTests.remove(h, p);
        }
        h.succeed();
    }

    /** Эталон времени тела: скорость — это ход времени тела, делённый на эталон. */
    @GameTest(template = T, batch = "core_bricks_2")
    public static void bodyRate(GameTestHelper h) {
        if (RpCoreAPI.BODY_REFERENCE != 8.0) h.fail("Эталон времени тела изменился: под 8 подобраны числа механик");
        if (RpCoreAPI.bodyRate() != RpCoreAPI.bodyScale() / 8.0) h.fail("bodyRate не равен bodyScale / эталон");
        h.succeed();
    }

    /** Matcher: id, тег, одна строка, массив, пусто, мусор. */
    @GameTest(template = T, batch = "core_bricks_3")
    public static void matcher(GameTestHelper h) {
        var stone = Items.STONE.builtInRegistryHolder();
        var planks = Items.OAK_PLANKS.builtInRegistryHolder();
        Matcher<Item> byId = Matcher.parse(new JsonPrimitive("minecraft:stone"), Registries.ITEM);
        if (byId.isEmpty() || !byId.matches(stone) || byId.matches(planks)) h.fail("Одна строка с id разобрана не так");
        Matcher<Item> byTag = Matcher.parse(new JsonPrimitive("#minecraft:planks"), Registries.ITEM);
        if (!byTag.matches(planks) || byTag.matches(stone)) h.fail("Тег разобран не так");
        JsonArray arr = new JsonArray();
        arr.add(" minecraft:stone ");
        arr.add("#minecraft:planks");
        arr.add("Это Не Id");
        Matcher<Item> both = Matcher.parse(arr, Registries.ITEM);
        if (!both.matches(stone) || !both.matches(planks) || both.matches(Items.DIRT.builtInRegistryHolder()))
            h.fail("Массив из id и тега разобран не так");
        Matcher<Item> none = Matcher.parse(null, Registries.ITEM);
        if (!none.isEmpty() || none.matches(stone)) h.fail("Пустой список должен быть пустым и ни с чем не совпадать");
        Matcher<Item> junk = Matcher.of(List.of("Это Не Id"), Registries.ITEM);
        if (!junk.isEmpty()) h.fail("Мусорная строка попала в список");
        if (!Matcher.of(List.of("minecraft:dirt"), Registries.ITEM).matches(Items.DIRT.builtInRegistryHolder())) h.fail("Matcher.of не работает");
        h.succeed();
    }

    /** Новые вызовы тела и черт отвечают «не умею», пока мод их не реализовал: старые поставщики не ломаются. */
    @GameTest(template = T, batch = "core_bricks_4")
    public static void providerDefaults(GameTestHelper h) {
        ServerPlayer p = PlayerStoreTests.player(h);
        try {
            BodyProvider body = new BodyProvider() {
                @Override
                public boolean handles(ResourceLocation feature) {
                    return false;
                }

                @Override
                public float condition(Player player) {
                    return 1;
                }
            };
            if (body.down(p) != BodyProvider.DownState.NONE || body.withdrawal(p) || body.craving(p) || body.formerAddiction(p) != null
                    || body.antipsychotic(p) != 0 || body.infect(p, 50, 2)) h.fail("Новые вызовы тела по умолчанию отвечают не «не умею»");
            if (BodyProvider.DownState.CLINICAL.ordinal() != 3 || BodyProvider.DownState.FAINT.ordinal() != 1)
                h.fail("Порядок DownState изменился: на него опирается перевод с числового уровня");
            ResourceLocation know = new ResourceLocation(RpCore.MODID, "test_knowledge");
            if (new TraitProvider() {}.teach(p, know) != -1) h.fail("teach по умолчанию не −1");
            if (RpCoreAPI.grantKnowledge(p, know) != -1) h.fail("Без мода черт знание «выдано»");
            int[] calls = {0};
            RpCoreAPI.registerProvider(new TraitProvider() {
                @Override
                public int teach(ServerPlayer player, ResourceLocation trait) {
                    if (!trait.equals(know)) return -1;
                    return calls[0]++ == 0 ? 1 : 0;
                }
            });
            if (RpCoreAPI.grantKnowledge(p, know) != 1 || RpCoreAPI.grantKnowledge(p, know) != 0) h.fail("Ответ мода черт не дошёл");
            if (RpCoreAPI.grantKnowledge(p, new ResourceLocation(RpCore.MODID, "other")) != -1) h.fail("Чужое знание «выдано»");
        } finally {
            PlayerStoreTests.remove(h, p);
        }
        h.succeed();
    }

    /** Психика и свет: без поставщика — «нет», с поставщиком — вызов доходит; ноль и минус не передаются. */
    @GameTest(template = T, batch = "core_bricks_5")
    public static void mindAndLight(GameTestHelper h) {
        ServerPlayer p = PlayerStoreTests.player(h);
        try {
            if (RpCoreAPI.stressImpulse(p, 5) || RpCoreAPI.sanityHit(p, 5)) h.fail("Без мода психики вызов «принят»");
            double[] got = {0, 0};
            RpCoreAPI.registerMind(new MindProvider() {
                @Override
                public void stressImpulse(ServerPlayer player, double amount) {
                    got[0] += amount;
                }

                @Override
                public void sanityHit(ServerPlayer player, double amount) {
                    got[1] += amount;
                }
            });
            if (!RpCoreAPI.stressImpulse(p, 5) || !RpCoreAPI.sanityHit(p, 2)) h.fail("С модом психики вызов не принят");
            RpCoreAPI.stressImpulse(p, 0);
            RpCoreAPI.sanityHit(p, -3);
            if (got[0] != 5 || got[1] != 2) h.fail("До мода психики дошло не то: " + got[0] + ", " + got[1]);
            RpCoreAPI.registerMind(null);

            if (RpCoreAPI.carriesLight(p)) h.fail("Без поставщиков игрок «несёт свет»");
            ResourceLocation a = new ResourceLocation(RpCore.MODID, "test_light_a"), b = new ResourceLocation(RpCore.MODID, "test_light_b");
            RpCoreAPI.registerLight(a, e -> false);
            RpCoreAPI.registerLight(b, e -> e == p);
            if (!RpCoreAPI.carriesLight(p)) h.fail("Один поставщик сказал «да», а общий ответ — «нет»");
            RpCoreAPI.registerLight(b, e -> false);
            if (RpCoreAPI.carriesLight(p)) h.fail("Повторная регистрация под тем же id не заменила поставщика");
        } finally {
            PlayerStoreTests.remove(h, p);
        }
        h.succeed();
    }

    /** Достижение по id: ванильное выдаётся целиком, повтор — тоже «да», несуществующее — «нет». */
    @GameTest(template = T, batch = "core_bricks_6")
    public static void grantAdvancement(GameTestHelper h) {
        ServerPlayer p = PlayerStoreTests.player(h);
        try {
            ResourceLocation id = new ResourceLocation("minecraft", "story/mine_stone");
            var adv = p.server.getAdvancements().getAdvancement(id);
            if (adv == null) { h.fail("В тестовом мире нет ванильного достижения " + id); return; }
            if (p.getAdvancements().getOrStartProgress(adv).isDone()) h.fail("Достижение есть у нового игрока до выдачи");
            if (!RpCoreAPI.grantAdvancement(p, id)) h.fail("Существующее достижение не выдано");
            if (!p.getAdvancements().getOrStartProgress(adv).isDone()) h.fail("После выдачи достижение не получено");
            if (!RpCoreAPI.grantAdvancement(p, id)) h.fail("Повторная выдача ответила «нет»");
            if (RpCoreAPI.grantAdvancement(p, new ResourceLocation(RpCore.MODID, "no_such_advancement"))) h.fail("Несуществующее достижение «выдано»");
        } finally {
            PlayerStoreTests.remove(h, p);
        }
        h.succeed();
    }
}

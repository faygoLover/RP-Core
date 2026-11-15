package faygolover.rpcore.gametest;

import com.mojang.authlib.GameProfile;
import faygolover.rpcore.RpCore;
import faygolover.rpcore.api.data.PersistentTag;
import faygolover.rpcore.api.data.PlayerStore;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * Данные игрока с общим жизненным циклом ({@link PlayerStore}, {@link PersistentTag}): имя ветки в сохранении,
 * копия при смерти и возврате из Края, повторная отправка клиенту, уборка при выходе, версия формата.
 * Все тесты в одной очереди и идут по одному: у них общий журнал вызовов.
 */
@GameTestHolder(RpCore.MODID)
@PrefixGameTestTemplate(false)
public final class PlayerStoreTests {
    private PlayerStoreTests() {}

    static final String T = "platform";
    private static int counter;

    static ServerPlayer player(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer p = new ServerPlayer(server, h.getLevel(), new GameProfile(UUID.randomUUID(), "rpc-test-" + (counter++)));
        Connection conn = new Connection(PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(conn);
        server.getPlayerList().placeNewPlayer(conn, p);
        p.setGameMode(GameType.SURVIVAL);
        Vec3 pos = h.absoluteVec(new Vec3(2.5, 1, 2.5));
        p.moveTo(pos.x, pos.y, pos.z, 0, 0);
        p.setOnGround(true);
        return p;
    }

    static void remove(GameTestHelper h, ServerPlayer p) {
        if (!p.isRemoved()) h.getLevel().getServer().getPlayerList().remove(p);
    }

    private static boolean logged(String line) {
        synchronized (CoreTestData.LOG) {
            return CoreTestData.LOG.contains(line);
        }
    }

    /** Данные висят на игроке и пишутся в его файл под именем хранилища — на этом держится совместимость сохранений. */
    @GameTest(template = T, batch = "core_store_1")
    public static void savedUnderStoreId(GameTestHelper h) {
        ServerPlayer p = player(h);
        ServerPlayer q = player(h);
        try {
            CoreTestData d = CoreTestData.STORE.get(p);
            if (d == null) { h.fail("У игрока нет данных хранилища"); return; }
            d.value = 7;
            d.note = "привет";
            CompoundTag saved = p.saveWithoutId(new CompoundTag());
            CompoundTag caps = saved.getCompound("ForgeCaps");
            if (!caps.contains(CoreTestData.ID.toString())) h.fail("В ForgeCaps нет ветки " + CoreTestData.ID + ": " + caps.getAllKeys());
            CompoundTag branch = caps.getCompound(CoreTestData.ID.toString());
            if (branch.getInt("value") != 7 || !branch.getString("note").equals("привет")) h.fail("Ветка записана не так: " + branch);
            if (branch.contains(PlayerStore.VERSION_KEY)) h.fail("У хранилища без версии появилось поле версии");
            q.load(saved);
            CoreTestData qd = CoreTestData.STORE.get(q);
            if (qd == null || qd.value != 7 || !qd.note.equals("привет")) h.fail("После чтения файла игрока данные другие");
        } finally {
            remove(h, p);
            remove(h, q);
        }
        h.succeed();
    }

    /** Смерть: правило мода получает wasDeath = true и решает, что переносить. */
    @GameTest(template = T, batch = "core_store_2")
    public static void cloneOnDeath(GameTestHelper h) {
        ServerPlayer p = player(h);
        CoreTestData d = CoreTestData.STORE.get(p);
        if (d == null) { h.fail("У игрока нет данных хранилища"); return; }
        d.value = 5;
        d.note = "шрам";
        CoreTestData.LOG.clear();
        ServerPlayer n = h.getLevel().getServer().getPlayerList().respawn(p, false);
        try {
            CoreTestData nd = CoreTestData.STORE.get(n);
            if (nd == null) { h.fail("После возрождения нет данных"); return; }
            if (!nd.note.equals("шрам")) h.fail("То, что должно пережить смерть, потеряно: " + nd.note);
            if (nd.value != 0) h.fail("То, что смерть сбрасывает, осталось: " + nd.value);
            if (!logged("CLONE death")) h.fail("Правило копирования не вызвано со смертью: " + CoreTestData.LOG);
            if (!logged("RESPAWN " + n.getGameProfile().getName())) h.fail("После возрождения данные не отправлены заново: " + CoreTestData.LOG);
        } finally {
            remove(h, n);
        }
        h.succeed();
    }

    /** Возврат из Края: wasDeath = false, переносится всё. */
    @GameTest(template = T, batch = "core_store_3")
    public static void cloneOnEndReturn(GameTestHelper h) {
        ServerPlayer p = player(h);
        CoreTestData d = CoreTestData.STORE.get(p);
        if (d == null) { h.fail("У игрока нет данных хранилища"); return; }
        d.value = 5;
        d.note = "шрам";
        CoreTestData.LOG.clear();
        ServerPlayer n = h.getLevel().getServer().getPlayerList().respawn(p, true);
        try {
            CoreTestData nd = CoreTestData.STORE.get(n);
            if (nd == null || nd.value != 5 || !nd.note.equals("шрам")) h.fail("После возврата из Края данные изменились");
            if (!logged("CLONE end")) h.fail("Правило копирования не вызвано как возврат из Края: " + CoreTestData.LOG);
        } finally {
            remove(h, n);
        }
        h.succeed();
    }

    /** Вход, смена измерения и выход доходят до хранилища. */
    @GameTest(template = T, batch = "core_store_4")
    public static void loginDimensionLogout(GameTestHelper h) {
        CoreTestData.LOG.clear();
        ServerPlayer p = player(h);
        String name = p.getGameProfile().getName();
        if (!logged("LOGIN " + name)) h.fail("Вход не дошёл до хранилища: " + CoreTestData.LOG);
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(p, p.level().dimension(), p.level().dimension()));
        if (!logged("DIMENSION " + name)) h.fail("Смена измерения не дошла до хранилища: " + CoreTestData.LOG);
        remove(h, p);
        if (!logged("FORGET " + name)) h.fail("Выход не дошёл до хранилища: " + CoreTestData.LOG);
        h.succeed();
    }

    /** Версия формата пишется в тег; старый тег без версии проходит миграцию; cloneAll переносит всё. */
    @GameTest(template = T, batch = "core_store_5")
    public static void dataVersionAndMigration(GameTestHelper h) {
        PlayerStore<CoreTestData.Versioned> store = CoreTestData.VERSIONED;
        CoreTestData.Versioned a = store.create();
        a.value = 4;
        CompoundTag t = store.write(a);
        if (t.getInt(PlayerStore.VERSION_KEY) != 2) h.fail("Версия формата не записана: " + t);
        CoreTestData.Versioned b = store.create();
        store.read(b, t);
        if (b.value != 4) h.fail("Тег текущей версии прочитан не так: " + b.value);
        CompoundTag old = new CompoundTag();
        old.putInt("old", 3);
        CoreTestData.Versioned c = store.create();
        store.read(c, old);
        if (c.value != 30) h.fail("Старый тег не прошёл миграцию: " + c.value);
        if (old.contains("value2")) h.fail("Миграция испортила исходный тег");

        ServerPlayer p = player(h);
        CoreTestData.Versioned d = store.get(p);
        if (d == null) { h.fail("У игрока нет версионированных данных"); return; }
        d.value = 9;
        ServerPlayer n = h.getLevel().getServer().getPlayerList().respawn(p, false);
        try {
            CoreTestData.Versioned nd = store.get(n);
            if (nd == null || nd.value != 9) h.fail("cloneAll не перенёс данные при смерти");
        } finally {
            remove(h, n);
        }
        h.succeed();
    }

    /** Раздел в getPersistentData: ваниль его при возрождении не переносит, переносит правило. */
    @GameTest(template = T, batch = "core_store_6")
    public static void persistentTag(GameTestHelper h) {
        PersistentTag tag = CoreTestData.TAG;
        ServerPlayer p = player(h);
        if (tag.peek(p) != null) h.fail("Раздел появился сам, до первого обращения");
        CompoundTag t = tag.of(p);
        t.putBoolean("asleep", true);
        t.putInt("debt", 3);
        CompoundTag saved = p.saveWithoutId(new CompoundTag());
        if (!saved.getCompound("ForgeData").contains(tag.key())) h.fail("Раздела нет в ForgeData файла игрока");
        ServerPlayer n = h.getLevel().getServer().getPlayerList().respawn(p, false);
        ServerPlayer m = null;
        try {
            CompoundTag nt = tag.peek(n);
            if (nt == null) { h.fail("Раздел не перенесён при смерти"); return; }
            if (nt.getInt("debt") != 3 || nt.getBoolean("asleep")) h.fail("Правило смерти применено не так: " + nt);
            nt.putBoolean("asleep", true);
            m = h.getLevel().getServer().getPlayerList().respawn(n, true);
            CompoundTag mt = tag.peek(m);
            if (mt == null || mt.getInt("debt") != 3 || !mt.getBoolean("asleep")) h.fail("При возврате из Края раздел изменился: " + mt);
        } finally {
            remove(h, n);
            if (m != null) remove(h, m);
        }
        h.succeed();
    }

    /** Поздняя регистрация и неполное описание — понятная ошибка, а не тихая потеря данных. */
    @GameTest(template = T, batch = "core_store_7")
    public static void lateOrIncompleteRegistrationFails(GameTestHelper h) {
        try {
            PersistentTag.of("rpcore_test_late").cloneAll().register();
            h.fail("Поздняя регистрация раздела прошла без ошибки");
        } catch (IllegalStateException expected) {
            // так и должно быть
        }
        try {
            PersistentTag.of("rpcore_test_norule").register();
            h.fail("Раздел без правила clone зарегистрирован");
        } catch (IllegalStateException expected) {
            // так и должно быть
        }
        h.succeed();
    }
}

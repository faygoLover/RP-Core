package faygolover.rpcore.access;

import faygolover.rpcore.RpCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLEnvironment;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Properties;
import java.util.Set;

/**
 * Замок семейства RP (Core 0.6.0, только локальная сборка). Если в jar Core есть {@code rpcore_lock.properties}
 * (его кладёт сборка, когда рядом с build.gradle лежит {@code lock.local.properties}), предметы модов семейства
 * и Zone Artifacts (сам мод от Core не зависит) скрыты из креатива и JEI, пока у игрока нет доступа. Доступ открывают: dev-окружение, верный ключ в
 * {@code config/rpcore-client.toml} или выдача с выделенного сервера (права ведущего). Без файла замка всё как раньше.
 */
public final class RpAccess {
    private RpAccess() {}

    private static final String DEFAULT_NAMESPACES = "rpcore,rpperks,rpstamina,rpmedicine,rpculinary,rppda,rpsports,rpfixes,fl_zone_arts";

    private static final boolean LOCKED;
    private static final Set<String> NAMESPACES;
    private static final String SALT;
    private static final String HASH;
    private static final int ITERATIONS;

    private static volatile boolean keyOk;
    private static volatile boolean serverGrant;

    static {
        Properties p = new Properties();
        boolean found = false;
        try (InputStream in = RpAccess.class.getResourceAsStream("/rpcore_lock.properties")) {
            if (in != null) {
                p.load(in);
                found = true;
            }
        } catch (Exception e) {
            RpCore.LOGGER.error("RP Core: rpcore_lock.properties не прочитан", e);
        }
        SALT = p.getProperty("salt", "");
        HASH = p.getProperty("hash", "");
        ITERATIONS = Integer.parseInt(p.getProperty("iterations", "100000"));
        NAMESPACES = Set.of(p.getProperty("namespaces", DEFAULT_NAMESPACES).split("\\s*,\\s*"));
        boolean inDev = !FMLEnvironment.production && !Boolean.getBoolean("rpcore.lock_in_dev");
        LOCKED = found && !HASH.isEmpty() && !inDev;
    }

    /** Замок есть в этой сборке и не снят в dev-окружении. */
    public static boolean lockEnabled() {
        return LOCKED;
    }

    /** Игроку видны предметы семейства (на клиенте). Без замка — всегда да. */
    public static boolean isUnlocked() {
        return !LOCKED || keyOk || serverGrant;
    }

    /** Предмет мода семейства — его прячет замок. */
    public static boolean isHidden(ItemStack stack) {
        return !stack.isEmpty() && NAMESPACES.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
    }

    public static boolean isHiddenNamespace(String namespace) {
        return NAMESPACES.contains(namespace);
    }

    /** Проверить ключ из конфига клиента. Возвращает true, если доступ от этого изменился. */
    public static boolean setKey(String key) {
        boolean before = isUnlocked();
        keyOk = LOCKED && key != null && !key.isBlank() && matches(key.trim());
        return before != isUnlocked();
    }

    /** Выдача доступа сервером (ведущий на выделенном сервере). Возвращает true, если доступ от этого изменился. */
    public static boolean setServerGrant(boolean grant) {
        boolean before = isUnlocked();
        serverGrant = grant;
        return before != isUnlocked();
    }

    private static boolean matches(String key) {
        try {
            var spec = new PBEKeySpec(key.toCharArray(), SALT.getBytes(StandardCharsets.UTF_8), ITERATIONS, 256);
            byte[] got = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(got, HexFormat.of().parseHex(HASH));
        } catch (Exception e) {
            RpCore.LOGGER.error("RP Core: ключ не проверен", e);
            return false;
        }
    }
}

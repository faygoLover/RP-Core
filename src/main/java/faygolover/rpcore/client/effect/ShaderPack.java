package faygolover.rpcore.client.effect;

import java.lang.reflect.Method;

/** Включён ли шейдерпак Oculus и идёт ли сейчас его проход теней (без жёсткой зависимости, через IrisApi). */
final class ShaderPack {
    private ShaderPack() {}

    private static Object api;
    private static Method inUse, shadowPass;
    private static boolean probed;

    private static void probe() {
        if (probed) return;
        probed = true;
        try {
            Class<?> c = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            api = c.getMethod("getInstance").invoke(null);
            inUse = c.getMethod("isShaderPackInUse");
            shadowPass = c.getMethod("isRenderingShadowPass");
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Нет Oculus.
        }
    }

    static boolean inUse() {
        return ask(true);
    }

    static boolean shadowPass() {
        return ask(false);
    }

    private static boolean ask(boolean pack) {
        probe();
        Method m = pack ? inUse : shadowPass;
        if (m == null) return false;
        try {
            return (Boolean) m.invoke(api);
        } catch (ReflectiveOperationException | RuntimeException e) {
            inUse = shadowPass = null;
            return false;
        }
    }
}

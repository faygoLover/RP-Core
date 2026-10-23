package faygolover.rpcore.client.effect;

import faygolover.flrender.api.FlRender;
import faygolover.flrender.api.Quality;
import faygolover.flrender.api.Technique;
import faygolover.rpcore.RpCore;

/** Техники RP Core во вкладке FL Render (подписи — в lang: {@code flrender.technique.rpcore.<ключ>}). */
final class CoreTechniques {
    private CoreTechniques() {}

    /** «Красные глаза» — светящиеся шары с ореолом; выключено — плоские пятна (как в 0.8.6). */
    static final Technique EYE_SPHERES = FlRender.technique(RpCore.MODID, "eye_spheres", Quality.MEDIUM);

    /** Зарегистрировать (загрузкой класса) — до того, как строится вкладка настроек. */
    static void init() {
    }
}

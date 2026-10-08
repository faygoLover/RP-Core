package faygolover.rpcore.client.hud;

/** Якорь элемента HUD: угол или середина экрана, от которой отсчитывается смещение. */
public enum Anchor {
    TOP_LEFT(0f, 0f), TOP_CENTER(0.5f, 0f), TOP_RIGHT(1f, 0f),
    CENTER_LEFT(0f, 0.5f), CENTER(0.5f, 0.5f), CENTER_RIGHT(1f, 0.5f),
    BOTTOM_LEFT(0f, 1f), BOTTOM_CENTER(0.5f, 1f), BOTTOM_RIGHT(1f, 1f);

    public final float fx;
    public final float fy;

    Anchor(float fx, float fy) {
        this.fx = fx;
        this.fy = fy;
    }

    public Anchor next() {
        Anchor[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    /** По имени, без учёта регистра; неизвестное — {@code def}. */
    public static Anchor parse(String name, Anchor def) {
        for (Anchor a : values()) if (a.name().equalsIgnoreCase(name)) return a;
        return def;
    }
}

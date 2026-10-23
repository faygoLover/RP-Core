package faygolover.rpcore.client.effect;

import faygolover.rpcore.effect.RpEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Звук «Помех»: шипение помех (громче ближе к носителю) и щелчки счётчика Гейгера (чаще ближе: от пары в секунду
 * до треска). Звучит у того, кто рядом, не у носителя — сила та же, что у картинки ({@link Interference}).
 */
final class InterferenceSound {
    private InterferenceSound() {}

    private static final RandomSource RND = RandomSource.create();
    private static Hiss hiss;

    static void tick(float s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        if (s > 0.02f && (hiss == null || hiss.isStopped())) {
            hiss = new Hiss();
            mc.getSoundManager().play(hiss);
        }
        if (s <= 0.02f) return;
        // Щелчки — случайный поток: в среднем 1…25 в секунду (как у счётчика от слабого фона до сильного).
        double perTick = (1.0 + 24.0 * s * s) / 20.0;
        int n = 0;
        double p = RND.nextDouble();
        double l = Math.exp(-perTick), acc = l;
        while (p > acc && n < 8) {
            n++;
            l *= perTick / n;
            acc += l;
        }
        for (int i = 0; i < n; i++) {
            float vol = 0.25f + 0.45f * s + RND.nextFloat() * 0.1f;
            float pitch = 0.9f + RND.nextFloat() * 0.25f;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(RpEffects.INTERFERENCE_CLICK.get(), pitch, vol));
        }
    }

    /** Закольцованное шипение, громкость идёт за силой помех, гаснет само. */
    private static final class Hiss extends AbstractTickableSoundInstance {
        Hiss() {
            super(RpEffects.INTERFERENCE_STATIC.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01f;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            float target = Interference.current();
            if (target <= 0.005f) {
                volume -= 0.05f;
                if (volume <= 0.01f) stop();
                return;
            }
            float want = 0.06f + 0.5f * target * target;
            volume += (want - volume) * 0.2f;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}

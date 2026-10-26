package faygolover.rpcore.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Логины рядом с РП-именами (Fakename) — только для админов (уровень прав 2+). Клавиша (по умолчанию F8)
 * включает и выключает: «Иван Петров (faygoLover)» над головой, в Tab и в чате. Всё на клиенте: логин каждого игрока
 * клиент и так знает из списка игроков.
 */
public final class ShowLogins {
    private ShowLogins() {}

    public static final KeyMapping KEY = new KeyMapping("key.rpcore.show_logins", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, "key.categories.rpcore");

    private static boolean on;
    /** Tab: что было до нашей приписки и что поставили мы (сервер может прислать новое имя — тогда оно новое «было»). */
    private static final Map<UUID, Component[]> TAB = new HashMap<>();

    static void onKeys(RegisterKeyMappingsEvent e) {
        e.register(KEY);
    }

    private static boolean allowed() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && p.hasPermissions(2);
    }

    private static boolean active() {
        return on && allowed();
    }

    static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (KEY.consumeClick()) {
            if (mc.player == null) continue;
            if (!allowed()) {
                mc.player.displayClientMessage(Component.translatable("rpcore.logins.admins_only").withStyle(ChatFormatting.GRAY), true);
                on = false;
                continue;
            }
            on = !on;
            mc.player.displayClientMessage(Component.translatable(on ? "rpcore.logins.on" : "rpcore.logins.off").withStyle(ChatFormatting.GRAY), true);
        }
        if (mc.player != null && mc.player.tickCount % 20 == 0) updateTab(mc.getConnection());
    }

    /** Tab: приписать логин (или вернуть как было). */
    private static void updateTab(@Nullable ClientPacketListener conn) {
        if (conn == null) return;
        boolean act = active();
        for (PlayerInfo info : conn.getOnlinePlayers()) {
            UUID id = info.getProfile().getId();
            Component[] mem = TAB.get(id);
            Component cur = info.getTabListDisplayName();
            if (mem != null && cur != mem[1]) mem = null; // сервер прислал новое имя
            if (!act) {
                if (mem != null) info.setTabListDisplayName(mem[0]);
                TAB.remove(id);
                continue;
            }
            if (mem != null) continue;
            Component base = cur != null ? cur : Component.literal(info.getProfile().getName());
            if (base.getString().equals(info.getProfile().getName())) continue; // РП-имени нет — нечего пояснять
            Component dec = base.copy().append(login(info.getProfile().getName()));
            info.setTabListDisplayName(dec);
            TAB.put(id, new Component[]{cur, dec});
        }
    }

    private static Component login(String name) {
        return Component.literal(" (" + name + ")").withStyle(ChatFormatting.GRAY);
    }

    static void onNameTag(RenderNameTagEvent e) {
        if (!active() || !(e.getEntity() instanceof Player p)) return;
        String name = p.getGameProfile().getName();
        if (e.getContent().getString().contains(name)) return;
        e.setContent(e.getContent().copy().append(login(name)));
    }

    /** Чат: после каждого имени игрока (в нём подсказка «сущность» с его UUID) — логин. */
    static void onChat(ClientChatReceivedEvent e) {
        if (!active()) return;
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        if (conn == null) return;
        e.setMessage(decorate(e.getMessage(), conn));
    }

    private static Component decorate(Component c, ClientPacketListener conn) {
        ComponentContents contents = c.getContents();
        if (contents instanceof TranslatableContents tr && tr.getArgs().length > 0) {
            Object[] args = tr.getArgs().clone();
            for (int i = 0; i < args.length; i++) if (args[i] instanceof Component a) args[i] = decorate(a, conn);
            contents = new TranslatableContents(tr.getKey(), tr.getFallback(), args);
        }
        MutableComponent out = MutableComponent.create(contents).setStyle(c.getStyle());
        for (Component sib : c.getSiblings()) out.append(decorate(sib, conn));
        HoverEvent hover = c.getStyle().getHoverEvent();
        if (hover != null && hover.getAction() == HoverEvent.Action.SHOW_ENTITY) {
            HoverEvent.EntityTooltipInfo info = hover.getValue(HoverEvent.Action.SHOW_ENTITY);
            PlayerInfo pi = info != null ? conn.getPlayerInfo(info.id) : null;
            if (pi != null && !c.getString().contains(pi.getProfile().getName())) out.append(login(pi.getProfile().getName()));
        }
        return out;
    }
}

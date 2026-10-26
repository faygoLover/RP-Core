package faygolover.rpcore.gm;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Метки на предметах (решения Alex 26.10): номер («№ 000123», счётчик на мир) и владелец (РП-имя на момент выдачи).
 * Хранятся в своём поле NBT {@code rpcore_mark}; строка в описании только показывает их — наковальня и правка
 * описания их не снимут и не подделают. Ставят ГМ командой {@code /mark} или игрок биркой (бирка в основной руке,
 * предмет — во второй, ПКМ; бирка тратится). Снять и поменять метку — только ГМ.
 */
public final class ItemMarks {
    private ItemMarks() {}

    public static final String TAG = "rpcore_mark";

    /** Бирка: «Именная» (владелец — тот, кто вешает) или «Номерная» (следующий номер мира). */
    public static class TagItem extends Item {
        private final boolean owner;

        public TagItem(Properties props, boolean owner) {
            super(props);
            this.owner = owner;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack tag = player.getItemInHand(hand);
            if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(tag);
            if (level.isClientSide) return InteractionResultHolder.success(tag);
            ServerPlayer sp = (ServerPlayer) player;
            ItemStack target = sp.getOffhandItem();
            if (target.isEmpty() || target.getItem() instanceof TagItem) {
                say(sp, "rpcore.mark.need_offhand");
                return InteractionResultHolder.fail(tag);
            }
            CompoundTag mark = target.getTagElement(TAG);
            if (mark != null && mark.contains(owner ? "owner" : "number")) {
                say(sp, owner ? "rpcore.mark.has_owner" : "rpcore.mark.has_number");
                return InteractionResultHolder.fail(tag);
            }
            if (owner) setOwner(target, sp);
            else setNumber(target, nextNumber(sp.server));
            if (!sp.getAbilities().instabuild) tag.shrink(1);
            say(sp, owner ? "rpcore.mark.owner_set" : "rpcore.mark.number_set");
            return InteractionResultHolder.consume(tag);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(owner ? "item.rpcore.owner_tag.desc" : "item.rpcore.number_tag.desc").withStyle(ChatFormatting.GRAY));
        }
    }

    private static void say(ServerPlayer sp, String key) {
        sp.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
    }

    public static void setOwner(ItemStack st, ServerPlayer owner) {
        CompoundTag m = st.getOrCreateTagElement(TAG);
        m.putString("owner", owner.getDisplayName().getString());
        m.putUUID("owner_id", owner.getUUID());
    }

    public static void setNumber(ItemStack st, String number) {
        st.getOrCreateTagElement(TAG).putString("number", number);
    }

    /** Снять одну метку ({@code owner} или {@code number}). */
    public static void clear(ItemStack st, String what) {
        CompoundTag m = st.getTagElement(TAG);
        if (m == null) return;
        m.remove(what);
        if (what.equals("owner")) m.remove("owner_id");
        if (m.isEmpty()) st.removeTagKey(TAG);
    }

    /** Следующий номер мира: «№ 000123». */
    public static String nextNumber(MinecraftServer server) {
        Counter c = server.overworld().getDataStorage().computeIfAbsent(Counter::load, Counter::new, "rpcore_marks");
        c.next++;
        c.setDirty();
        return String.format("№ %06d", c.next);
    }

    private static final class Counter extends SavedData {
        int next;

        static Counter load(CompoundTag t) {
            Counter c = new Counter();
            c.next = t.getInt("next");
            return c;
        }

        @Override
        public CompoundTag save(CompoundTag t) {
            t.putInt("next", next);
            return t;
        }
    }

    /** Строки меток в описании предмета (клиент). */
    public static void onTooltip(ItemTooltipEvent e) {
        CompoundTag m = e.getItemStack().getTagElement(TAG);
        if (m == null) return;
        if (m.contains("number")) e.getToolTip().add(Component.literal(m.getString("number")).withStyle(ChatFormatting.GOLD));
        if (m.contains("owner"))
            e.getToolTip().add(Component.translatable("rpcore.mark.owner_line", m.getString("owner")).withStyle(ChatFormatting.GOLD));
    }

    // ---------------------------------------------------------------- /mark (ГМ, предмет в основной руке)

    public static void onCommands(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("mark").requires(s -> s.hasPermission(2))
                .then(Commands.literal("id")
                        .executes(c -> number(c, null))
                        .then(Commands.literal("clear").executes(c -> clearCmd(c, "number")))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(c -> number(c, StringArgumentType.getString(c, "text")))))
                .then(Commands.literal("owner")
                        .then(Commands.literal("clear").executes(c -> clearCmd(c, "owner")))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> owner(c, EntityArgument.getPlayer(c, "player"))))));
    }

    private static ItemStack held(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ItemStack st = c.getSource().getPlayerOrException().getMainHandItem();
        if (st.isEmpty()) throw new net.minecraft.commands.CommandRuntimeException(Component.translatable("rpcore.mark.need_mainhand"));
        return st;
    }

    private static int number(CommandContext<CommandSourceStack> c, @Nullable String text) throws CommandSyntaxException {
        ItemStack st = held(c);
        String n = text == null || text.equalsIgnoreCase("auto") ? nextNumber(c.getSource().getServer()) : text;
        setNumber(st, n);
        c.getSource().sendSuccess(() -> Component.translatable("rpcore.mark.number_set_to", n), true);
        return 1;
    }

    private static int owner(CommandContext<CommandSourceStack> c, ServerPlayer p) throws CommandSyntaxException {
        ItemStack st = held(c);
        setOwner(st, p);
        c.getSource().sendSuccess(() -> Component.translatable("rpcore.mark.owner_set_to", p.getDisplayName()), true);
        return 1;
    }

    private static int clearCmd(CommandContext<CommandSourceStack> c, String what) throws CommandSyntaxException {
        clear(held(c), what);
        c.getSource().sendSuccess(() -> Component.translatable("rpcore.mark.cleared"), true);
        return 1;
    }
}

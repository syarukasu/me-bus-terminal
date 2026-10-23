package com.syaru.meomnibus.block;

import appeng.core.AEConfig;
import appeng.items.tools.powered.WirelessTerminalItem;
import com.syaru.meomnibus.menu.TerminalAccess;
import com.syaru.meomnibus.platform.Platform;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public abstract class BaseWirelessTerminal extends WirelessTerminalItem {
    protected BaseWirelessTerminal() {
        super(AEConfig.instance().getWirelessTerminalBattery(), new Properties().stacksTo(1));
    }

    protected abstract TerminalAccess access(ServerPlayer player, int slot, ItemStack stack);

    public boolean openRemote(Player player, int slot) {
        if (!(player instanceof ServerPlayer serverPlayer) || slot < 0
                || slot >= player.getInventory().getContainerSize()) return false;
        var stack = player.getInventory().getItem(slot);
        if (!stack.is(this)) return false;
        var access = access(serverPlayer, slot, stack);
        if (!access.canAccess(player)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("omnibus.wireless_unavailable"), true);
            return false;
        }
        Platform.open(serverPlayer, access);
        return true;
    }

    protected boolean heldBy(Player player, int slot, ItemStack stack) {
        return player.isAlive() && !player.isSpectator()
                && player.getInventory().getItem(slot) == stack && stack.is(this);
    }

    protected static boolean permitted(Player player, appeng.api.networking.IGridNode node) {
        return node!=null && node.isActive()
                && node.getOwner() instanceof net.minecraft.world.level.block.entity.BlockEntity be
                && !be.isRemoved() && be.getLevel()==player.level()
                && appeng.util.Platform.hasPermissions(new appeng.api.util.DimensionalBlockPos(be),player);
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
        return openRemote(player, slot) ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }
}

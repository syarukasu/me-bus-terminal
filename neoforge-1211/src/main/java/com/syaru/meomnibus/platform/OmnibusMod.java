package com.syaru.meomnibus.platform;

import com.syaru.meomnibus.block.*;
import com.syaru.meomnibus.menu.OmnibusTerminalMenu;
import com.syaru.meomnibus.network.OmnibusGridService;
import appeng.api.networking.GridServices;
import appeng.api.ids.AECreativeTabIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.fml.common.Mod;

@Mod(Platform.ID)
public final class OmnibusMod {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,Platform.ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,Platform.ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,Platform.ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,Platform.ID);
    public static final net.neoforged.neoforge.registries.DeferredHolder<Block,OmnibusTerminalBlock> BLOCK=BLOCKS.register("terminal",OmnibusTerminalBlock::new);
    public static final net.neoforged.neoforge.registries.DeferredHolder<Item,appeng.items.parts.PartItem<OmnibusTerminalPart>> ITEM=ITEMS.register("terminal",()->new appeng.items.parts.PartItem<>(new Item.Properties(),OmnibusTerminalPart.class,OmnibusTerminalPart::new));
    public static final net.neoforged.neoforge.registries.DeferredHolder<Item,WirelessItem> WIRELESS=ITEMS.register("wireless_terminal",WirelessItem::new);
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>,BlockEntityType<OmnibusTerminalBlockEntity>> ENTITY=ENTITIES.register("terminal",()->BlockEntityType.Builder.of(OmnibusTerminalBlockEntity::new,BLOCK.get()).build(null));
    public static final net.neoforged.neoforge.registries.DeferredHolder<MenuType<?>,MenuType<OmnibusTerminalMenu>> MENU=MENUS.register("terminal",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(OmnibusTerminalMenu::fromNetwork));
    public OmnibusMod(net.neoforged.bus.api.IEventBus bus) {
        appeng.api.parts.PartModels.registerModels(appeng.items.parts.PartModelsHelper.createModels(OmnibusTerminalPart.class));
        
        BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);MENUS.register(bus);
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event)->
            event.registerBlockEntity(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST,ENTITY.get(),(be,context)->be));
        bus.addListener(Network::register);
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event)->
            event.registerItem(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM,
                (stack,context)->new appeng.items.tools.powered.powersink.PoweredItemCapabilities(stack,WIRELESS.get()),WIRELESS.get()));
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->{
            BLOCK.get().setBlockEntity(OmnibusTerminalBlockEntity.class,ENTITY.get(),null,null);
            appeng.blockentity.AEBaseBlockEntity.registerBlockEntityItem(ENTITY.get(),ITEM.get());
            GridServices.register(OmnibusGridService.class,OmnibusGridService.class);
            appeng.api.features.GridLinkables.register(WIRELESS.get(),appeng.items.tools.powered.WirelessTerminalItem.LINKABLE_HANDLER);
        }));
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event)->{
            if(event.getTabKey()==AECreativeTabIds.MAIN) { event.accept(ITEM.get());event.accept(WIRELESS.get()); }
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event)->{
            if(event.getLevel().getBlockEntity(event.getPos()) instanceof BaseTerminal terminal && !event.getEntity().isShiftKeyDown()) {
                if(event.getEntity() instanceof ServerPlayer player) Platform.open(player,terminal);
                event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true);
            }
        });
        if(net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) ClientRegistration.register(bus);
    }
}

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
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.common.Mod;

@Mod(Platform.ID)
public final class OmnibusMod {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,Platform.ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,Platform.ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,Platform.ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,Platform.ID);
    public static final net.minecraftforge.registries.RegistryObject<OmnibusTerminalBlock> BLOCK=BLOCKS.register("terminal",OmnibusTerminalBlock::new);
    public static final net.minecraftforge.registries.RegistryObject<appeng.items.parts.PartItem<OmnibusTerminalPart>> ITEM=ITEMS.register("terminal",()->new appeng.items.parts.PartItem<>(new Item.Properties(),OmnibusTerminalPart.class,OmnibusTerminalPart::new));
    public static final net.minecraftforge.registries.RegistryObject<WirelessItem> WIRELESS=ITEMS.register("wireless_terminal",WirelessItem::new);
    public static final net.minecraftforge.registries.RegistryObject<BlockEntityType<OmnibusTerminalBlockEntity>> ENTITY=ENTITIES.register("terminal",()->BlockEntityType.Builder.of(OmnibusTerminalBlockEntity::new,BLOCK.get()).build(null));
    public static final net.minecraftforge.registries.RegistryObject<MenuType<OmnibusTerminalMenu>> MENU=MENUS.register("terminal",()->net.minecraftforge.common.extensions.IForgeMenuType.create(OmnibusTerminalMenu::fromNetwork));
    public OmnibusMod() {
        appeng.api.parts.PartModels.registerModels(appeng.items.parts.PartModelsHelper.createModels(OmnibusTerminalPart.class));
        var bus=net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);MENUS.register(bus);
        Network.register();
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->{
            BLOCK.get().setBlockEntity(OmnibusTerminalBlockEntity.class,ENTITY.get(),null,null);
            appeng.blockentity.AEBaseBlockEntity.registerBlockEntityItem(ENTITY.get(),ITEM.get());
            GridServices.register(OmnibusGridService.class,OmnibusGridService.class);
            appeng.api.features.GridLinkables.register(WIRELESS.get(),appeng.items.tools.powered.WirelessTerminalItem.LINKABLE_HANDLER);
        }));
        bus.addListener((net.minecraftforge.event.BuildCreativeModeTabContentsEvent event)->{
            if(event.getTabKey()==AECreativeTabIds.MAIN) { event.accept(ITEM.get());event.accept(WIRELESS.get()); }
        });
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event)->{
            if(event.getLevel().getBlockEntity(event.getPos()) instanceof BaseTerminal terminal && !event.getEntity().isShiftKeyDown()) {
                if(event.getEntity() instanceof ServerPlayer player) Platform.open(player,terminal);
                event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true);
            }
        });
        if(net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) ClientRegistration.register(bus);
    }
}

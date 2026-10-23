package com.syaru.meomnibus.platform;
public final class ClientRegistration {
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        bus.addListener((net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event)->
            event.register(new appeng.client.render.StaticItemColor(appeng.api.util.AEColor.TRANSPARENT),OmnibusMod.ITEM.get()));
        bus.addListener((net.minecraftforge.client.event.ModelEvent.RegisterAdditional event)->
            com.syaru.meomnibus.client.OmnibusAppearance.MODELS.forEach(name->
                event.register(com.syaru.meomnibus.client.OmnibusAppearance.modelId(name))));
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event)->
            event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(OmnibusMod.MENU.get(),com.syaru.meomnibus.client.OmnibusTerminalScreen::new)));
    }
    public static net.minecraft.client.resources.model.BakedModel presentationModel(net.minecraft.resources.ResourceLocation id) {
        return net.minecraft.client.Minecraft.getInstance().getModelManager().getModel(id);
    }
}

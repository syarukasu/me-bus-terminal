package com.syaru.meomnibus.platform;
public final class ClientRegistration {
    public static void register(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event)->
            event.register(new appeng.client.render.StaticItemColor(appeng.api.util.AEColor.TRANSPARENT),OmnibusMod.ITEM.get()));
        bus.addListener((net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event)->
            com.syaru.meomnibus.client.OmnibusAppearance.MODELS.forEach(name->
                event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(
                    com.syaru.meomnibus.client.OmnibusAppearance.modelId(name)))));
        bus.addListener((net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event)->
            event.register(OmnibusMod.MENU.get(),com.syaru.meomnibus.client.OmnibusTerminalScreen::new));
    }
    public static net.minecraft.client.resources.model.BakedModel presentationModel(net.minecraft.resources.ResourceLocation id) {
        return net.minecraft.client.Minecraft.getInstance().getModelManager().getModel(
            net.minecraft.client.resources.model.ModelResourceLocation.standalone(id));
    }
}

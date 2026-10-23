package com.syaru.meomnibus.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.syaru.meomnibus.platform.ClientRegistration;
import com.syaru.meomnibus.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Mod-local presentation models; actual AE2 items and world models remain untouched. */
public final class OmnibusAppearance {
    public static final List<String> MODELS = List.of("storage_bus", "import_bus", "export_bus",
            "capacity_card", "crafting_card", "energy_card", "equal_distribution_card", "fuzzy_card",
            "inverter_card", "redstone_card", "speed_card", "void_card");
    private static final ResourceLocation STATES = Platform.id("textures/gui/ae2_1211/states.png");
    private static final Vector3f GUI_LIGHT_0,GUI_LIGHT_1;
    static {
        // Use the 1.21.1 GUI light transform on both engines, including its Y reflection.
        var lights=new Matrix4f().scaling(1,-1,1)
                .rotateYXZ(1.0821041f,3.2375858f,0)
                .rotateYXZ((float)(-Math.PI/8),(float)(Math.PI*3/4),0);
        GUI_LIGHT_0=lights.transformDirection(new Vector3f(.2f,1,-.7f).normalize());
        GUI_LIGHT_1=lights.transformDirection(new Vector3f(-.2f,1,.7f).normalize());
    }

    private OmnibusAppearance() { }

    public static ResourceLocation modelId(String name) {
        return Platform.id("item/gui/" + name);
    }

    public enum ActionIcon {
        COPY(80,176), PASTE(112,0), CLEAR(96,0);
        final int u,v;
        ActionIcon(int u,int v) { this.u=u;this.v=v; }
        public void render(GuiGraphics g,int x,int y) {
            g.blit(STATES,x,y,u,v,16,16,256,256);
        }
    }

    public static void renderItem(GuiGraphics g,ItemStack stack,int x,int y) {
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(!id.getNamespace().equals("ae2") || !MODELS.contains(id.getPath())) {
            g.renderItem(stack,x,y);return;
        }
        var mc=Minecraft.getInstance();
        var model=ClientRegistration.presentationModel(modelId(id.getPath()));
        g.flush();
        var pose=g.pose();pose.pushPose();
        boolean flat=!model.usesBlockLight();
        try {
            pose.translate(x+8,y+8,150);
            var normal=new org.joml.Matrix3f(pose.last().normal());
            pose.scale(16,-16,16);
            // 1.20.1 and 1.21.1 handle reflected-scale normals differently.
            pose.last().normal().set(normal).scale(1,-1,1);
            if(flat)Lighting.setupForFlatItems();else RenderSystem.setShaderLights(GUI_LIGHT_0,GUI_LIGHT_1);
            mc.getItemRenderer().render(stack,ItemDisplayContext.GUI,false,pose,
                    mc.renderBuffers().bufferSource(),15728880,OverlayTexture.NO_OVERLAY,model);
            mc.renderBuffers().bufferSource().endBatch();
        } finally {
            Lighting.setupFor3DItems();
            pose.popPose();
        }
    }
}

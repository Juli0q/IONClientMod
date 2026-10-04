package de.ionnetwork.client.forge;

import de.ionnetwork.client.IonClient;
import de.ionnetwork.client.IonSteamKeyboard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = IonClient.MOD_ID, name = "ION Client", version = IonClient.VERSION, useMetadata = true, clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]",
        guiFactory = "de.ionnetwork.client.forge.IonGuiFactory")
public class IonClientForge {

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        IonClient.init("Forge", Loader.instance().getConfigDir());
        MinecraftForge.EVENT_BUS.register(this);
    }

    /** Moves the chat messages up with the chat input while Steam's keyboard is docked (see GuiChatMixin). */
    @SubscribeEvent
    public void onRenderChat(RenderGameOverlayEvent.Chat event) {
        if (Minecraft.getMinecraft().currentScreen instanceof GuiChat) {
            event.posY -= IonSteamKeyboard.chatLift(event.resolution.getScaledHeight());
        }
    }
}

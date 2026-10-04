package de.ionnetwork.client.forge;

import de.ionnetwork.client.gui.IonSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

import java.util.Set;

/** Makes the "Config" button in Forge's mod list open the ION Client settings. */
public class IonGuiFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraft) {
    }

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return IonSettingsScreen.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }
}

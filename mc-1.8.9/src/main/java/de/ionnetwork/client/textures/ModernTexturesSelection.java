package de.ionnetwork.client.textures;

import de.ionnetwork.client.IonClient;
import de.ionnetwork.client.IonSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.settings.GameSettings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the modern texture pack's place in the selected resource packs and the
 * {@link IonSettings#modernTextures()} toggle in step, both ways: the setting adds or removes the
 * pack, and choosing it in the Resource Packs screen changes the setting.
 *
 * <p>A pack the setting adds goes below every other selected pack, so the player's own packs
 * still win over it.
 */
public final class ModernTexturesSelection {

    private ModernTexturesSelection() {
    }

    /**
     * At startup, before Minecraft builds its pack list from the options: adds the pack to them or
     * takes it out, as the setting says. A pack still being built is added once it is ready.
     * Forge ignores saving the options while the game loads; this runs on every start, and the
     * next save writes the change down.
     */
    public static void applyToOptions(GameSettings options, File gameDir) {
        boolean listed = options.resourcePacks.contains(ModernTextures.PACK_NAME);
        boolean wanted = IonSettings.modernTextures();
        if (wanted && !listed && ModernTextures.packFile(gameDir).isFile()) {
            options.resourcePacks.add(0, ModernTextures.PACK_NAME);
        } else if (!wanted && listed) {
            options.resourcePacks.remove(ModernTextures.PACK_NAME);
            options.incompatibleResourcePacks.remove(ModernTextures.PACK_NAME);
        }
    }

    /**
     * In game: selects or drops the pack as the setting says and reloads the resources, the way
     * the Resource Packs screen's Done does. Does nothing when the selection already matches, or
     * while the pack is not built yet. Main thread only.
     */
    public static void apply(Minecraft mc) {
        ResourcePackRepository repository = mc.getResourcePackRepository();
        List<ResourcePackRepository.Entry> selected = new ArrayList<>(repository.getRepositoryEntries());
        ResourcePackRepository.Entry current = find(selected);
        boolean wanted = IonSettings.modernTextures();
        if (wanted == (current != null)) {
            return;
        }
        if (wanted) {
            repository.updateRepositoryEntriesAll();
            ResourcePackRepository.Entry pack = find(repository.getRepositoryEntriesAll());
            if (pack == null) {
                return;
            }
            selected.add(0, pack);
        } else {
            selected.remove(current);
        }

        repository.setRepositories(selected);
        GameSettings options = mc.gameSettings;
        options.resourcePacks.clear();
        options.incompatibleResourcePacks.clear();
        for (ResourcePackRepository.Entry entry : selected) {
            options.resourcePacks.add(entry.getResourcePackName());
            if (entry.func_183027_f() != 1) {
                options.incompatibleResourcePacks.add(entry.getResourcePackName());
            }
        }
        options.saveOptions();
        mc.refreshResources();
    }

    /** Called with every new pack selection, so a change made in the Resource Packs screen reaches the setting. */
    public static void selectionChanged(List<ResourcePackRepository.Entry> selected) {
        boolean on = find(selected) != null;
        if (on == IonSettings.modernTextures()) {
            return;
        }
        IonSettings.setModernTextures(on);
        try {
            IonSettings.save();
        } catch (IOException e) {
            IonClient.LOGGER.warn("Could not save the ION Client settings", e);
        }
    }

    private static ResourcePackRepository.Entry find(List<ResourcePackRepository.Entry> entries) {
        for (ResourcePackRepository.Entry entry : entries) {
            if (ModernTextures.PACK_NAME.equals(entry.getResourcePackName())) {
                return entry;
            }
        }
        return null;
    }
}

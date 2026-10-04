package de.ionnetwork.client.gui;

import de.ionnetwork.client.IonEntryStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** "ION" in the logo gradient followed by plain text, e.g. the settings title and the options button. */
public final class IonBrandText {

    private IonBrandText() {
    }

    public static MutableComponent of(String rest) {
        MutableComponent text = Component.empty();
        String brand = IonEntryStyle.BRAND_WORD;
        for (int i = 0; i < brand.length(); i++) {
            text.append(Component.literal(String.valueOf(brand.charAt(i))).withColor(IonEntryStyle.brandLetterColor(i) & 0xFFFFFF));
        }
        return text.append(Component.literal(rest));
    }
}

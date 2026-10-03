package de.ionnetwork.client.gui;

import de.ionnetwork.client.IonBrand;
import de.ionnetwork.client.IonClient;
import de.ionnetwork.client.IonEntryStyle;
import de.ionnetwork.client.IonEntryStyle.Status;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The ION Network row in the multiplayer screen.
 *
 * <p>Extends the vanilla entry so the screen's join logic ({@code connectToSelected}) and the
 * Forge server-compatibility checks treat it as a normal saved server, but draws itself in the
 * website's look: a dark card with a gradient stripe, the server's own icon, the name with an OFFICIAL
 * pill, the live MOTD, and the address with a breathing status dot.
 */
public final class IonServerEntry extends ServerListEntryNormal {

    private static final ResourceLocation UNKNOWN_SERVER = new ResourceLocation("textures/misc/unknown_server.png");
    private static final ResourceLocation ICON_LOCATION = new ResourceLocation(IonClient.MOD_ID, "servers/ion/icon");
    private static final ExecutorService PINGER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ION Server Pinger");
        thread.setDaemon(true);
        return thread;
    });

    private final GuiMultiplayer screen;
    private final Minecraft mc;
    private final ServerData data;
    private DynamicTexture icon;
    private String lastIconData;
    private long lastClick;

    public IonServerEntry(GuiMultiplayer screen, ServerData data) {
        super(screen, data);
        this.screen = screen;
        this.data = data;
        this.mc = Minecraft.getMinecraft();
    }

    @Override
    public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected) {
        ensurePinging();

        FontRenderer font = mc.fontRendererObj;
        long now = Minecraft.getSystemTime();
        Status status = IonEntryStyle.status(data.field_78841_f, data.pingToServer);

        // The card covers the slot exactly as vanilla's selection box does, so a selected ION
        // entry shows a thin periwinkle frame instead of vanilla's grey one.
        int left = x - 2;
        int top = y - 2;
        int right = x - 2 + listWidth;
        int bottom = y + 34;
        boolean hovered = mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
        Gui.drawRect(left, top, right, bottom, isSelected || hovered ? IonEntryStyle.CARD_FILL_ACTIVE : IonEntryStyle.CARD_FILL);
        if (isSelected) {
            drawFrame(left, top, right, bottom, IonBrand.AQUA);
        }

        // Gradient stripe on the left edge, in eight pixel-art steps.
        int steps = 8;
        int stripeHeight = bottom - top;
        for (int i = 0; i < steps; i++) {
            int segTop = top + stripeHeight * i / steps;
            int segBottom = top + stripeHeight * (i + 1) / steps;
            Gui.drawRect(left, segTop, left + IonEntryStyle.STRIPE_WIDTH, segBottom, IonBrand.gradientAt(i / (float) (steps - 1)));
        }

        // Icon: the favicon the server sends with its ping, or vanilla's unknown-server
        // placeholder until it arrives.
        String iconData = data.getBase64EncodedIconData();
        if (!Objects.equals(iconData, lastIconData)) {
            lastIconData = iconData;
            uploadIcon(iconData);
        }
        int iconX = x + IonEntryStyle.STRIPE_WIDTH + IonEntryStyle.GAP;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawTextureAt(iconX, y, icon != null ? ICON_LOCATION : UNKNOWN_SERVER);

        // Line 1: "ION" in the logo gradient, "Network" in white; player count and ping on the right.
        int textX = x + IonEntryStyle.TEXT_X;
        int line1 = y + IonEntryStyle.LINE_1_Y;
        int cursor = textX;
        for (int i = 0; i < IonEntryStyle.BRAND_WORD.length(); i++) {
            String letter = String.valueOf(IonEntryStyle.BRAND_WORD.charAt(i));
            font.drawStringWithShadow(letter, cursor, line1, IonEntryStyle.brandLetterColor(i));
            cursor += font.getStringWidth(letter);
        }
        font.drawStringWithShadow(IonEntryStyle.BRAND_REST, cursor, line1, IonBrand.WHITE);

        int bars;
        int sheetColumn = 0;
        String pingTooltip;
        if (status == Status.PINGING) {
            sheetColumn = 1;
            bars = IonEntryStyle.pingingFrame(now, slotIndex);
            pingTooltip = "Pinging...";
        } else {
            bars = IonEntryStyle.pingBars(data.pingToServer);
            pingTooltip = status == Status.OFFLINE ? "(no connection)" : data.pingToServer + "ms";
        }
        int pingX = x + listWidth - IonEntryStyle.PING_ICON_RIGHT_INSET;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(Gui.icons);
        Gui.drawModalRectWithCustomSizedTexture(pingX, y, sheetColumn * 10, 176 + bars * 8, 10, 8, 256.0F, 256.0F);

        String population = status == Status.ONLINE ? IonEntryStyle.stripFormatting(data.populationInfo) : "";
        int populationWidth = font.getStringWidth(population);
        if (!population.isEmpty()) {
            font.drawString(population, pingX - 2 - populationWidth, line1, IonBrand.AQUA);
        }

        // Lines 2 and 3: the MOTD, wrapped to two lines like the vanilla entries, with the
        // server's own formatting.
        int maxTextWidth = pingX - 2 - populationWidth - IonEntryStyle.GAP - textX;
        List<String> motdLines = font.listFormattedStringToWidth(data.serverMOTD, maxTextWidth);
        for (int i = 0; i < Math.min(motdLines.size(), 2); i++) {
            font.drawString(motdLines.get(i), textX, y + IonEntryStyle.LINE_2_Y + font.FONT_HEIGHT * i, IonBrand.TEXT_MUTED);
        }

        // Tooltips, matching vanilla's hit boxes.
        int relX = mouseX - x;
        int relY = mouseY - y;
        if (relX >= listWidth - 15 && relX <= listWidth - 5 && relY >= 0 && relY <= 8) {
            screen.setHoveringText(pingTooltip);
        } else if (!population.isEmpty() && relX >= listWidth - populationWidth - 15 - 2 && relX <= listWidth - 15 - 2 && relY >= 0 && relY <= 8
                && data.playerList != null && !data.playerList.isEmpty()) {
            screen.setHoveringText(data.playerList);
        }
    }

    /** Mirrors vanilla's private {@code prepareServerIcon}, reusing the texture across screen opens. */
    private void uploadIcon(String base64) {
        if (base64 == null) {
            releaseIcon();
            return;
        }
        try {
            BufferedImage image = TextureUtil.readBufferedImage(new ByteArrayInputStream(Base64.getMimeDecoder().decode(base64)));
            if (image == null || image.getWidth() != 64 || image.getHeight() != 64) {
                throw new IOException("Icon must be a 64x64 PNG");
            }
            if (icon == null) {
                ITextureObject existing = mc.getTextureManager().getTexture(ICON_LOCATION);
                if (existing instanceof DynamicTexture) {
                    icon = (DynamicTexture) existing;
                } else {
                    icon = new DynamicTexture(64, 64);
                    mc.getTextureManager().loadTexture(ICON_LOCATION, icon);
                }
            }
            image.getRGB(0, 0, 64, 64, icon.getTextureData(), 0, 64);
            icon.updateDynamicTexture();
        } catch (IOException | IllegalArgumentException e) {
            IonClient.LOGGER.error("Invalid icon for {}", data.serverIP, e);
            data.setBase64EncodedIconData(null);
            releaseIcon();
        }
    }

    private void releaseIcon() {
        if (icon != null) {
            mc.getTextureManager().deleteTexture(ICON_LOCATION);
            icon = null;
        }
    }

    private void drawFrame(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, top + 1, color);
        Gui.drawRect(left, bottom - 1, right, bottom, color);
        Gui.drawRect(left, top, left + 1, bottom, color);
        Gui.drawRect(right - 1, top, right, bottom, color);
    }

    /** Dispatches the ping the way vanilla's entry does, with the same error wording. */
    private void ensurePinging() {
        if (data.field_78841_f) {
            return;
        }
        data.field_78841_f = true;
        data.pingToServer = -2L;
        data.serverMOTD = "";
        data.populationInfo = "";
        PINGER.submit(() -> {
            try {
                screen.getOldServerPinger().ping(data);
            } catch (UnknownHostException e) {
                data.pingToServer = -1L;
                data.serverMOTD = EnumChatFormatting.DARK_RED + "Can't resolve hostname";
            } catch (Exception e) {
                data.pingToServer = -1L;
                data.serverMOTD = EnumChatFormatting.DARK_RED + "Can't connect to server.";
            }
        });
    }

    @Override
    public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
        screen.selectServer(slotIndex);
        long now = Minecraft.getSystemTime();
        if (now - lastClick < 250L) {
            screen.connectToSelected();
        }
        lastClick = now;
        return false;
    }
}

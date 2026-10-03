package de.ionnetwork.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import de.ionnetwork.client.IonBrand;
import de.ionnetwork.client.IonClient;
import de.ionnetwork.client.IonEntryStyle;
import de.ionnetwork.client.IonEntryStyle.Status;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.Util;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The ION Network row in the multiplayer screen.
 *
 * <p>Extends the vanilla entry so the screen's join logic treats it as a normal saved server,
 * but draws itself in the website's look: a dark card with a gradient stripe, the server's own icon, the
 * name with an OFFICIAL pill, the live MOTD, and the address with a breathing status dot.
 */
public final class IonServerEntry extends ServerSelectionList.OnlineServerEntry {

    private static final ResourceLocation PING_UNREACHABLE_SPRITE = ResourceLocation.withDefaultNamespace("server_list/ping_unreachable");
    private static final ResourceLocation[] PING_SPRITES = {
            ResourceLocation.withDefaultNamespace("server_list/ping_5"),
            ResourceLocation.withDefaultNamespace("server_list/ping_4"),
            ResourceLocation.withDefaultNamespace("server_list/ping_3"),
            ResourceLocation.withDefaultNamespace("server_list/ping_2"),
            ResourceLocation.withDefaultNamespace("server_list/ping_1"),
    };
    private static final ResourceLocation[] PINGING_SPRITES = {
            ResourceLocation.withDefaultNamespace("server_list/pinging_1"),
            ResourceLocation.withDefaultNamespace("server_list/pinging_2"),
            ResourceLocation.withDefaultNamespace("server_list/pinging_3"),
            ResourceLocation.withDefaultNamespace("server_list/pinging_4"),
            ResourceLocation.withDefaultNamespace("server_list/pinging_5"),
    };
    private static final Component CANT_RESOLVE_TEXT = Component.translatable("multiplayer.status.cannot_resolve").withStyle(ChatFormatting.DARK_RED);
    private static final Component CANT_CONNECT_TEXT = Component.translatable("multiplayer.status.cannot_connect").withStyle(ChatFormatting.DARK_RED);
    private static final ExecutorService PINGER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ION Server Pinger");
        thread.setDaemon(true);
        return thread;
    });

    private final ServerSelectionList list;
    private final JoinMultiplayerScreen screen;
    private final Minecraft minecraft;
    private final ServerData data;
    /** The favicon from the ping. Keyed apart from vanilla's so the parent's own copy never clashes. */
    private final FaviconTexture icon;
    private byte[] lastIconBytes;
    private long lastClick;

    public IonServerEntry(ServerSelectionList list, JoinMultiplayerScreen screen, ServerData data) {
        list.super(screen, data);
        this.list = list;
        this.screen = screen;
        this.data = data;
        this.minecraft = Minecraft.getInstance();
        this.icon = FaviconTexture.forServer(minecraft.getTextureManager(), "ionclient/" + data.ip);
    }

    @Override
    public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
        ensurePinging();

        Font font = minecraft.font;
        long now = Util.getMillis();
        Status status = statusOf(data);
        boolean selected = list.getSelected() == this;

        // The card covers the slot exactly as vanilla's selection frame does, so a selected ION
        // entry shows a thin periwinkle frame instead of vanilla's grey one.
        int cardLeft = left - 2;
        int cardTop = top - 2;
        int cardRight = left - 2 + width;
        int cardBottom = top + height + 2;
        graphics.fill(cardLeft, cardTop, cardRight, cardBottom, selected || hovering ? IonEntryStyle.CARD_FILL_ACTIVE : IonEntryStyle.CARD_FILL);
        if (selected) {
            drawFrame(graphics, cardLeft, cardTop, cardRight, cardBottom, IonBrand.AQUA);
        }

        // Gradient stripe on the left edge, in eight pixel-art steps.
        int steps = 8;
        int stripeHeight = cardBottom - cardTop;
        for (int i = 0; i < steps; i++) {
            int segTop = cardTop + stripeHeight * i / steps;
            int segBottom = cardTop + stripeHeight * (i + 1) / steps;
            graphics.fill(cardLeft, segTop, cardLeft + IonEntryStyle.STRIPE_WIDTH, segBottom, IonBrand.gradientAt(i / (float) (steps - 1)));
        }

        // Icon: the favicon the server sends with its ping, or vanilla's unknown-server
        // placeholder until it arrives.
        byte[] iconBytes = data.getIconBytes();
        if (!Arrays.equals(iconBytes, lastIconBytes)) {
            if (uploadIcon(iconBytes)) {
                lastIconBytes = iconBytes;
            } else {
                data.setIconBytes(null);
            }
        }
        int iconX = left + IonEntryStyle.STRIPE_WIDTH + IonEntryStyle.GAP;
        drawIcon(graphics, iconX, top, icon.textureLocation());

        // Line 1: "ION" in the logo gradient, "Network" in white; player count and ping on the right.
        int textX = left + IonEntryStyle.TEXT_X;
        int line1 = top + IonEntryStyle.LINE_1_Y;
        int cursor = textX;
        for (int i = 0; i < IonEntryStyle.BRAND_WORD.length(); i++) {
            String letter = String.valueOf(IonEntryStyle.BRAND_WORD.charAt(i));
            graphics.drawString(font, letter, cursor, line1, IonEntryStyle.brandLetterColor(i), true);
            cursor += font.width(letter);
        }
        graphics.drawString(font, IonEntryStyle.BRAND_REST, cursor, line1, IonBrand.WHITE, true);

        ResourceLocation pingSprite;
        Component pingTooltip;
        if (status == Status.PINGING) {
            pingSprite = PINGING_SPRITES[IonEntryStyle.pingingFrame(now, index)];
            pingTooltip = Component.translatable("multiplayer.status.pinging");
        } else if (status == Status.OFFLINE) {
            pingSprite = PING_UNREACHABLE_SPRITE;
            pingTooltip = Component.translatable("multiplayer.status.no_connection");
        } else {
            pingSprite = PING_SPRITES[IonEntryStyle.pingBars(data.ping)];
            pingTooltip = Component.translatable("multiplayer.status.ping", data.ping);
        }
        int pingX = left + width - IonEntryStyle.PING_ICON_RIGHT_INSET;
        graphics.blitSprite(pingSprite, pingX, top, 10, 8);

        String population = status == Status.ONLINE ? IonEntryStyle.stripFormatting(data.status.getString()) : "";
        int populationWidth = font.width(population);
        if (!population.isEmpty()) {
            graphics.drawString(font, population, pingX - 2 - populationWidth, line1, IonBrand.AQUA, false);
        }

        // Lines 2 and 3: the MOTD, wrapped to two lines like the vanilla entries, with the
        // server's own formatting.
        int maxTextWidth = pingX - 2 - populationWidth - IonEntryStyle.GAP - textX;
        List<FormattedCharSequence> motdLines = font.split(data.motd, maxTextWidth);
        for (int i = 0; i < Math.min(motdLines.size(), 2); i++) {
            graphics.drawString(font, motdLines.get(i), textX, top + IonEntryStyle.LINE_2_Y + font.lineHeight * i, IonBrand.TEXT_MUTED, false);
        }

        // Tooltips, matching vanilla's hit boxes.
        int relX = mouseX - left;
        int relY = mouseY - top;
        if (relX >= width - 15 && relX <= width - 5 && relY >= 0 && relY <= 8) {
            screen.setTooltipForNextRenderPass(pingTooltip);
        } else if (!population.isEmpty() && relX >= width - populationWidth - 15 - 2 && relX <= width - 15 - 2 && relY >= 0 && relY <= 8
                && data.playerList != null && !data.playerList.isEmpty()) {
            screen.setTooltipForNextRenderPass(data.playerList.stream().map(Component::getVisualOrderText).toList());
        }
    }

    /** Mirrors vanilla's private {@code uploadServerIcon}. Returns false if the bytes are not a valid icon. */
    private boolean uploadIcon(byte[] bytes) {
        if (bytes == null) {
            icon.clear();
            return true;
        }
        try {
            icon.upload(NativeImage.read(bytes));
            return true;
        } catch (Throwable e) {
            IonClient.LOGGER.error("Invalid icon for {}", data.ip, e);
            return false;
        }
    }

    private static Status statusOf(ServerData data) {
        return switch (data.state()) {
            case INITIAL, PINGING -> Status.PINGING;
            case UNREACHABLE -> Status.OFFLINE;
            case INCOMPATIBLE, SUCCESSFUL -> Status.ONLINE;
        };
    }

    private static void drawFrame(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left, top, right, top + 1, color);
        graphics.fill(left, bottom - 1, right, bottom, color);
        graphics.fill(left, top, left + 1, bottom, color);
        graphics.fill(right - 1, top, right, bottom, color);
    }

    /** Dispatches the ping the way vanilla's entry does, with the same error wording. */
    private void ensurePinging() {
        if (data.state() != ServerData.State.INITIAL) {
            return;
        }
        data.setState(ServerData.State.PINGING);
        data.motd = CommonComponents.EMPTY;
        data.status = CommonComponents.EMPTY;
        PINGER.submit(() -> {
            try {
                screen.getPinger().pingServer(data, () -> {
                }, () -> {
                    data.setState(data.protocol == SharedConstants.getCurrentVersion().getProtocolVersion()
                            ? ServerData.State.SUCCESSFUL : ServerData.State.INCOMPATIBLE);
                });
            } catch (UnknownHostException e) {
                data.setState(ServerData.State.UNREACHABLE);
                data.motd = CANT_RESOLVE_TEXT;
            } catch (Exception e) {
                data.setState(ServerData.State.UNREACHABLE);
                data.motd = CANT_CONNECT_TEXT;
            }
        });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        screen.setSelected(this);
        long now = Util.getMillis();
        if (now - lastClick < 250L) {
            screen.joinSelectedServer();
        }
        lastClick = now;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // The pinned entry is never reordered, so Shift+Up/Down do nothing here.
        return false;
    }

    @Override
    public void close() {
        icon.close();
        super.close();
    }

    @Override
    public Component getNarration() {
        return Component.literal(IonBrand.SERVER_NAME + ", " + data.motd.getString());
    }
}

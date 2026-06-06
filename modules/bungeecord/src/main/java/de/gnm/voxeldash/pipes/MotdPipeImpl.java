package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.motd.Motd;
import de.gnm.voxeldash.api.entities.motd.MotdCapabilities;
import de.gnm.voxeldash.api.helper.MotdHelper;
import de.gnm.voxeldash.api.helper.motd.LegacyMotdSerializer;
import de.gnm.voxeldash.api.pipes.MotdPipe;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class MotdPipeImpl implements MotdPipe, Listener {

    private volatile BaseComponent[] components;
    private volatile Favicon favicon;

    public MotdPipeImpl() {
        apply(MotdHelper.getMotd());
        reloadIcon();
    }

    @Override
    public MotdCapabilities getCapabilities() {
        return new MotdCapabilities(2, true, true, true, true, false);
    }

    @Override
    public void apply(Motd motd) {
        String legacy = LegacyMotdSerializer.toLegacy(motd, true);
        this.components = TextComponent.fromLegacyText(legacy);
    }

    @Override
    public void applyIcon(byte[] png) {
        reloadIcon();
    }

    private void reloadIcon() {
        if (!MotdHelper.hasIcon()) {
            this.favicon = null;
            return;
        }
        try {
            BufferedImage image = ImageIO.read(MotdHelper.getIconFile());
            this.favicon = image != null ? Favicon.create(image) : null;
        } catch (Throwable t) {
            this.favicon = null;
        }
    }

    @EventHandler
    public void onPing(ProxyPingEvent event) {
        if (event.getResponse() == null) {
            return;
        }
        if (components != null && components.length > 0) {
            event.getResponse().setDescriptionComponent(new TextComponent(components));
        }
        if (favicon != null) {
            event.getResponse().setFavicon(favicon);
        }
    }
}

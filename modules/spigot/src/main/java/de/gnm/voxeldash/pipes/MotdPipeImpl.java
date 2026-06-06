package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.motd.Motd;
import de.gnm.voxeldash.api.entities.motd.MotdCapabilities;
import de.gnm.voxeldash.api.helper.MotdHelper;
import de.gnm.voxeldash.api.helper.PropertyHelper;
import de.gnm.voxeldash.api.helper.motd.LegacyMotdSerializer;
import de.gnm.voxeldash.api.pipes.MotdPipe;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;

/**
 * Spigot/Paper MOTD pipe. Renders the canonical model to a legacy {@code §}
 * string (including {@code §x} hex sequences and {@code \n} for two lines) and
 * applies it live through the {@link ServerListPingEvent} — no restart needed.
 */
public class MotdPipeImpl implements MotdPipe, Listener {

    private volatile String legacy;
    private volatile CachedServerIcon icon;

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
        this.legacy = LegacyMotdSerializer.toLegacy(motd, true);
        PropertyHelper.setProperty("motd", LegacyMotdSerializer.toLegacy(motd, false));
    }

    @Override
    public void applyIcon(byte[] png) {
        reloadIcon();
    }

    private void reloadIcon() {
        if (!MotdHelper.hasIcon()) {
            this.icon = null;
            return;
        }
        try {
            this.icon = Bukkit.loadServerIcon(MotdHelper.getIconFile());
        } catch (Throwable t) {
            this.icon = null;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPing(ServerListPingEvent event) {
        if (legacy != null && !legacy.isEmpty()) {
            event.setMotd(legacy);
        }
        if (icon != null) {
            try {
                event.setServerIcon(icon);
            } catch (Throwable ignored) {
            }
        }
    }
}

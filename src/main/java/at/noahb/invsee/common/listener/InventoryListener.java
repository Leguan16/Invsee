package at.noahb.invsee.common.listener;

import at.noahb.invsee.InvseePlugin;
import at.noahb.invsee.invsee.session.InvseeSession;
import com.destroystokyo.paper.MaterialTags;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public record InventoryListener(InvseePlugin instance) implements Listener {

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        this.instance.getInvseeSessionManager().removeSubscriberFromSession(event.getPlayer());
        this.instance.getEnderseeSessionManager().removeSubscriberFromSession(event.getPlayer());
        handle(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        boolean setEmpty = false;

        if (event.getClickedInventory() != null && event.getClickedInventory().getSize() == 45 && event.getSlot() > 41) {
            event.setCancelled(true);
            return;
        }

        if (InvseeSession.Placeholders.isPlaceholder(event.getCurrentItem())) {
            if (InvseeSession.Placeholders.isCursorPlaceholder(event.getCurrentItem()) || InvseeSession.Placeholders.isCursorPlaceholder(event.getCursor())) {
                event.setCancelled(true);
                return;
            }
            if (MaterialTags.ARMOR.isTagged(event.getCursor()) || InvseeSession.Placeholders.isOffHandPlaceholder(event.getCurrentItem())) {
                setEmpty = true;
            } else {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getClickedInventory() != null && event.getClickedInventory().getSize() == 45 && event.getSlot() >= 36 && event.getSlot() < 40) {
            if (!MaterialTags.ARMOR.isTagged(event.getCursor()) && !event.getCursor().isEmpty()) {
                event.setCancelled(true);
                return;
            }

            if (!InvseeSession.ArmorSlot.values()[39 - event.getSlot()].checkIfItemFitsSlot(event.getCursor()) && !event.getCursor().isEmpty()) {
                event.setCancelled(true);
                return;
            }
        }

        if (InventoryAction.NOTHING == event.getAction()) {
            return;
        }

        if (setEmpty) event.setCurrentItem(ItemStack.empty());
        handle(event.getWhoClicked());
    }

    @EventHandler
    public void onPickupItem(EntityPickupItemEvent event) {
        handle(event.getEntity());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (InvseeSession.Placeholders.isPlaceholder(event.getOldCursor())) {
            event.setCancelled(true);
            return;
        }
        handle(event.getWhoClicked());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        handle(event.getPlayer());
    }

    private void handle(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }
        player.getScheduler().run(this.instance, scheduledTask -> this.instance.getInvseeSessionManager().updateContent(player), null);
        player.getScheduler().run(this.instance, scheduledTask -> this.instance.getEnderseeSessionManager().updateContent(player), null);
    }
}

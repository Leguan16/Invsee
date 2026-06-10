package at.noahb.invsee.invsee.session;

import at.noahb.invsee.InvseePlugin;
import at.noahb.invsee.common.session.Session;
import com.destroystokyo.paper.MaterialSetTag;
import com.destroystokyo.paper.MaterialTags;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;


public class InvseeSession implements Session {

    private final UUID uuid;
    private final Set<UUID> subscribers;
    private final Inventory inventory;
    private final ReentrantLock lock = new ReentrantLock();
    private final Cache<UUID, Player> playerCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .build();

    public InvseeSession(OfflinePlayer offlinePlayer, UUID subscriber) {
        this.uuid = offlinePlayer.getUniqueId();
        this.subscribers = new HashSet<>();

        String name = offlinePlayer.getName() == null ? "unknown" : offlinePlayer.getName();
        this.inventory = InvseePlugin.getInstance().getServer().createInventory(this, 45, text(name).append(text("'s inventory")));

        updateSubscriberInventory();
        addSubscriber(subscriber);
    }

    @Override
    public UUID getUniqueIdOfObservedPlayer() {
        return this.uuid;
    }

    @Override
    public Set<UUID> getSubscribers() {
        return this.subscribers;
    }

    @Override
    public @NonNull Inventory getInventory() {
        return this.inventory;
    }

    private PlayerInventory getPlayerInventory(OfflinePlayer offlinePlayer) {
        if (offlinePlayer instanceof Player player) {
            return player.getInventory();
        }

        Optional<Player> player = getPlayerOffline(offlinePlayer);

        return player.map(Player::getInventory).orElse(null);
    }

    @Override
    public void removeSubscriber(UUID subscriber) {
        this.subscribers.remove(subscriber);
    }

    @Override
    public void updateSubscriberInventory() {
        update(() -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(this.uuid);
            PlayerInventory playerInv = getPlayerInventory(offlinePlayer);

            if (playerInv == null) {
                return;
            }

            for (int i = 0; i < 41; i++) {
                this.inventory.setItem(i, playerInv.getItem(i));
            }
            if (offlinePlayer instanceof Player player) {
                this.inventory.setItem(41, player.getItemOnCursor());
            }
            replaceEmptyPlaceholderSpots();
        });
    }

    private void replaceEmptyPlaceholderSpots() {
        if (this.inventory.getItem(36) == null) this.inventory.setItem(36, Placeholders.BOOTS);
        if (this.inventory.getItem(37) == null) this.inventory.setItem(37, Placeholders.LEGGINGS);
        if (this.inventory.getItem(38) == null) this.inventory.setItem(38, Placeholders.CHESTPLATE);
        if (this.inventory.getItem(39) == null) this.inventory.setItem(39, Placeholders.HELMET);
        if (this.inventory.getItem(40) == null) this.inventory.setItem(40, Placeholders.OFF_HAND);
        if (this.inventory.getItem(41) == null) this.inventory.setItem(41, Placeholders.CURSOR);
        for (int i = 42; i < 45; i++) {
            if (this.inventory.getItem(i) == null) this.inventory.setItem(i, Placeholders.NO_USAGE);
        }
    }

    @Override
    public boolean hasSubscriber(UUID uuid) {
        return this.subscribers.contains(uuid);
    }

    @Override
    public void updateObservedInventory() {
        update(() -> {
            OfflinePlayer offlinePlayer = InvseePlugin.getInstance().getServer().getOfflinePlayer(uuid);
            PlayerInventory playerInventory = getPlayerInventory(offlinePlayer);
            if (playerInventory == null) {
                return;
            }
            for (int i = 0; i < playerInventory.getSize(); i++) {
                if (Placeholders.isPlaceholder(this.inventory.getItem(i))) continue;
                playerInventory.setItem(i, this.inventory.getItem(i));
            }

            if (!Placeholders.isPlaceholder(this.inventory.getItem(41)) && offlinePlayer instanceof Player player) {
                player.setItemOnCursor(this.inventory.getItem(41));
            }

            replaceEmptyPlaceholderSpots();
        });
    }

    @Override
    public ReentrantLock getLock() {
        return this.lock;
    }

    @Override
    public void cache(Player player) {
        this.playerCache.put(this.uuid, player);
    }

    @Override
    public Player getCachedPlayer() {
        return this.playerCache.getIfPresent(this.uuid);
    }

    @Override
    public boolean isSubscriber(@NotNull UUID whoClicked) {
        return this.subscribers.contains(whoClicked);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        InvseeSession session = (InvseeSession) object;
        return Objects.equals(uuid, session.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    public enum ArmorSlot {
        HELMET(MaterialTags.HELMETS),
        CHESTPLATE(MaterialTags.CHESTPLATES),
        LEGGINGS(MaterialTags.LEGGINGS),
        BOOTS(MaterialTags.BOOTS);

        private final MaterialSetTag tag;

        ArmorSlot(MaterialSetTag tag) {
            this.tag = tag;
        }

        public boolean checkIfItemFitsSlot(ItemStack itemStack) {
            return this.tag.isTagged(itemStack);
        }
    }

    public static class Placeholders {
        static final ItemStack HELMET = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        static final ItemStack CHESTPLATE = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        static final ItemStack LEGGINGS = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        static final ItemStack BOOTS = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        static final ItemStack OFF_HAND = new ItemStack(Material.BARRIER);
        static final ItemStack CURSOR = new ItemStack(Material.BARRIER);
        static final ItemStack NO_USAGE = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);

        static final NamespacedKey OFF_HAND_KEY = new NamespacedKey(InvseePlugin.getInstance(), "offhand");
        static final NamespacedKey CURSOR_KEY = new NamespacedKey(InvseePlugin.getInstance(), "cursor");
        static final NamespacedKey INVSEE_KEY = new NamespacedKey(InvseePlugin.getInstance(), "invsee");
        static {
            List<Component> lore = List.of(text("empty", RED).decoration(ITALIC, false));
            HELMET.editMeta(itemMeta -> {
                itemMeta.displayName(text("Helmet slot", GOLD).decoration(ITALIC, false));
                itemMeta.lore(lore);
                itemMeta.getPersistentDataContainer().set(INVSEE_KEY, PersistentDataType.BOOLEAN, true);
            });
            CHESTPLATE.editMeta(itemMeta -> {
                itemMeta.displayName(text("Chestplate slot", GOLD).decoration(ITALIC, false));
                itemMeta.lore(lore);
                itemMeta.getPersistentDataContainer().set(INVSEE_KEY, PersistentDataType.BOOLEAN, true);
            });
            LEGGINGS.editMeta(itemMeta -> {
                itemMeta.displayName(text("Leggings slot", GOLD).decoration(ITALIC, false));
                itemMeta.lore(lore);
                itemMeta.getPersistentDataContainer().set(INVSEE_KEY, PersistentDataType.BOOLEAN, true);
            });
            BOOTS.editMeta(itemMeta -> {
                itemMeta.displayName(text("Boots slot", GOLD).decoration(ITALIC, false));
                itemMeta.lore(lore);
                itemMeta.getPersistentDataContainer().set(INVSEE_KEY, PersistentDataType.BOOLEAN, true);
            });
            OFF_HAND.editMeta(itemMeta -> {
                itemMeta.displayName(text("Off Hand", GOLD).decoration(ITALIC, false));
                itemMeta.lore(lore);
                itemMeta.getPersistentDataContainer().set(INVSEE_KEY, PersistentDataType.BOOLEAN, true);
                itemMeta.getPersistentDataContainer().set(OFF_HAND_KEY, PersistentDataType.BOOLEAN, true);
            });
            CURSOR.editMeta(itemMeta -> {
                itemMeta.displayName(text("Cursor", GOLD).decoration(ITALIC, false));
                itemMeta.lore(lore);
                itemMeta.getPersistentDataContainer().set(INVSEE_KEY, PersistentDataType.BOOLEAN, true);
                itemMeta.getPersistentDataContainer().set(CURSOR_KEY, PersistentDataType.BOOLEAN, true);
            });
            NO_USAGE.editMeta(itemMeta -> {
                itemMeta.displayName(Component.empty());
                itemMeta.getPersistentDataContainer().set(INVSEE_KEY, PersistentDataType.BOOLEAN, true);
            });
        }

        public static boolean isOffHandPlaceholder(ItemStack itemStack) {
            return itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().getPersistentDataContainer().has(OFF_HAND_KEY, PersistentDataType.BOOLEAN);
        }

        public static boolean isCursorPlaceholder(ItemStack itemStack) {
            return itemStack.hasItemMeta() && itemStack.getItemMeta().getPersistentDataContainer().has(CURSOR_KEY, PersistentDataType.BOOLEAN);
        }

        public static boolean isPlaceholder(ItemStack itemStack) {
            return itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().getPersistentDataContainer().has(INVSEE_KEY, PersistentDataType.BOOLEAN);
        }
    }

}
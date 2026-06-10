package at.noahb.invsee.endersee.session;

import at.noahb.invsee.InvseePlugin;
import at.noahb.invsee.common.session.Session;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static net.kyori.adventure.text.Component.text;

public class EnderseeSession implements Session {

    private final UUID uuid;

    private final Set<UUID> subscribers;

    private final Inventory enderchest;
    private final Cache<UUID, Player> playerCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .build();
    private final ReentrantLock lock = new ReentrantLock();

    public EnderseeSession(OfflinePlayer offlinePlayer, UUID subscriber) {
        this.uuid = offlinePlayer.getUniqueId();
        this.subscribers = new HashSet<>();

        String name = offlinePlayer.getName() == null ? "unknown" : offlinePlayer.getName();
        this.enderchest = InvseePlugin.getInstance().getServer().createInventory(this, InventoryType.ENDER_CHEST, text(name).append(text("'s enderchest")));

        updateSubscriberInventory();
        addSubscriber(subscriber);
    }

    private Inventory getEnderChest(OfflinePlayer offline) {
        if (offline instanceof Player player) {
            return player.getEnderChest();
        }

        Optional<Player> player = getPlayerOffline(offline);
        return player.map(HumanEntity::getEnderChest).orElse(null);
    }

    @Override
    public UUID getUniqueIdOfObservedPlayer() {
        return this.uuid;
    }

    @Override
    public void updateObservedInventory() {
        update(() -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(this.uuid);
            Inventory enderChest = getEnderChest(offlinePlayer);
            if (enderChest == null) {
                return;
            }
            for (int i = 0; i < InventoryType.ENDER_CHEST.getDefaultSize(); i++) {
                enderChest.setItem(i, this.enderchest.getItem(i));
            }
        });
    }

    @Override
    public void updateSubscriberInventory() {
        update(() -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            Inventory enderChest = getEnderChest(offlinePlayer);

            if (enderChest == null) {
                return;
            }

            for (int i = 0; i < InventoryType.ENDER_CHEST.getDefaultSize(); i++) {
                this.enderchest.setItem(i, enderChest.getItem(i));
            }
        });
    }

    @Override
    public Set<UUID> getSubscribers() {
        return this.subscribers;
    }

    @Override
    public @NonNull Inventory getInventory() {
        return this.enderchest;
    }

    @Override
    public void removeSubscriber(UUID subscriber) {
        this.subscribers.remove(subscriber);
    }

    @Override
    public boolean hasSubscriber(UUID subscriber) {
        return this.subscribers.contains(subscriber);
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
        EnderseeSession that = (EnderseeSession) object;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}

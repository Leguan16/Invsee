package at.noahb.invsee;

import at.noahb.invsee.common.listener.InventoryListener;
import at.noahb.invsee.common.listener.LuckPermsListener;
import at.noahb.invsee.endersee.command.EnderseeCommand;
import at.noahb.invsee.endersee.session.EnderseeSessionManager;
import at.noahb.invsee.invsee.command.InvseeCommand;
import at.noahb.invsee.invsee.session.InvseeSessionManager;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class InvseePlugin extends JavaPlugin {

    private static InvseePlugin instance;
    private InvseeSessionManager invseeSessionManager;
    private EnderseeSessionManager enderseeSessionManager;
    private LuckPerms luckPerms;
    private Command enderseeCommand;
    private Command invseeCommand;

    public static InvseePlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        instance = this;
        this.invseeSessionManager = new InvseeSessionManager(instance);
        this.enderseeSessionManager = new EnderseeSessionManager(instance);
        registerCommands();
        getServer().getPluginManager().registerEvents(new InventoryListener(instance), this);

        RegisteredServiceProvider<LuckPerms> provider;

        try {
            provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        } catch (NoClassDefFoundError e) {
            provider = null;
            getLogger().warning("LuckPerms not found. Some features might not work.");
        }

        if (provider != null) {
            this.luckPerms = provider.getProvider();
            new LuckPermsListener(this, this.luckPerms);
        }

        if (getServer().getPluginManager().getPermission(Constants.LOOKUP_UNSEEN_PERMISSION) == null) {
            getServer().getPluginManager().addPermission(new Permission(Constants.LOOKUP_UNSEEN_PERMISSION));
        }
    }

    private void registerCommands() {
        this.invseeCommand = new InvseeCommand(this);
        getServer().getCommandMap().register("invsee", invseeCommand);
        this.enderseeCommand = new EnderseeCommand(this);
        getServer().getCommandMap().register("endersee", enderseeCommand);
    }

    public InvseeSessionManager getInvseeSessionManager() {
        return this.invseeSessionManager;
    }

    public EnderseeSessionManager getEnderseeSessionManager() {
        return this.enderseeSessionManager;
    }

    public Command getEnderseeCommand() {
        return this.enderseeCommand;
    }

    public Command getInvseeCommand() {
        return this.invseeCommand;
    }
}

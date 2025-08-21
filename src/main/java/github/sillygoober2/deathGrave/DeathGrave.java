package github.sillygoober2.deathGrave;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DeathGrave extends JavaPlugin implements Listener {

    Map<UUID, ItemStack[]> graves = new HashMap<>();
    Map<Location, UUID> gravesLocation = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        World world = Bukkit.getWorlds().getFirst();
        //world.setGameRule(GameRule.KEEP_INVENTORY, true);

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Inventory inventory = player.getInventory();
        Location location = player.getLocation();
        Block block = location.getBlock();

        block.setType(Material.PLAYER_HEAD, false);

        Skull skull = (Skull) block.getState();
        skull.setOwningPlayer(player);
        skull.update();

        UUID graveId = UUID.randomUUID();
        graves.put(graveId, player.getInventory().getContents().clone());
        gravesLocation.put(block.getLocation(), graveId);
        Bukkit.broadcast(Component.text("CREATED ID: "+graveId));

        player.getInventory().clear();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event){
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if(block == null) return;

        if(block.getType() != Material.PLAYER_HEAD) return;
        Location location = block.getLocation();

        if(!gravesLocation.containsKey(location)) return;
        getLogger().info("found in map");

        Skull skull = (Skull) block.getState();
        String ownerName = skull.getOwningPlayer() != null
                ? skull.getOwningPlayer().getName()
                : "Unknown";

        UUID graveId = gravesLocation.get(location);

        Inventory customInventory = Bukkit.createInventory(
                new GraveHolder(graveId),
                54,
                Component.text(ownerName+"'s Grave"));
        customInventory.setContents(graves.get(graveId));

        event.getPlayer().openInventory(customInventory);
        Bukkit.broadcast(Component.text("OPENED INV ID: "+graveId));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event){
        if(event.getInventory().getHolder() instanceof GraveHolder holder){
            UUID graveId = holder.getGraveId();
            Bukkit.broadcast(Component.text("CLOSED INV ID: "+graveId));
        }
    }
}

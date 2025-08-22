package github.sillygoober2.deathGrave;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DeathGrave extends JavaPlugin implements Listener {

    Map<UUID, Grave> graves = new HashMap<>();

    @Override
    public void onEnable() {
        getLogger().info("DeathGrave was initialized");
        Bukkit.getPluginManager().registerEvents(this, this);

        if(!getDataFolder().exists()){
            getLogger().info("Creating File for DeathGrave Plugin");
            getDataFolder().mkdirs();
        }

        File gravesFile = new File(getDataFolder(), "graves.yml");
        if(!gravesFile.exists()){
            try {
                gravesFile.createNewFile();
                getLogger().warning("CREATEED NEW FILE");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if(gravesFile.length() > 0){
            YamlConfiguration config = YamlConfiguration.loadConfiguration(gravesFile);

            for (String key : config.getKeys(false)) {
                UUID graveId = UUID.fromString(key);

                String playerName = config.get(key+".owner").toString();
                Location location = (Location) config.get(key+".location");
                List<ItemStack> list = (List<ItemStack>) config.get(key+".contents");
                long deathTime = config.getLong(key+".deathTime");

                ItemStack[] items = list.toArray(new ItemStack[0]);

                Grave grave = new Grave(playerName, location, deathTime, graveId, items);
                graves.put(graveId, grave);
            }
        }
    }

    @Override
    public void onDisable() {
        if(getDataFolder().exists()){
            File gravesFile = new File(getDataFolder(), "graves.yml");
            if(gravesFile.exists()){
                YamlConfiguration config = new YamlConfiguration();
                for(Map.Entry<UUID, Grave> entry : graves.entrySet()){
                    UUID graveId = entry.getKey();
                    Grave grave = entry.getValue();
                    String path = graveId.toString();

                    config.set(path+".owner", grave.getName());
                    config.set(path+".location", grave.getLocation());
                    config.set(path+".contents", grave.getItems());
                    config.set(path+".deathTime", grave.getDeathTime());

                }
                try {
                    config.save(gravesFile);
                    getLogger().warning("SAVED NEW FILE");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void removeGrave(Block block){
        if(!(block.getType() == Material.PLAYER_HEAD)) return;

        Location location = block.getLocation();

        UUID graveId = null;
        Grave graveData = null;

        for(Map.Entry<UUID, Grave> entry : graves.entrySet()) {
            if(entry.getValue().getLocation().equals(location)) {
                graveId = entry.getKey();
                graveData = entry.getValue();
                break;
            }
        }

        if(graveId == null) return;

        ItemStack[] graveContents = graveData.getItems();
        if(graveContents != null){
            World world = block.getWorld();
            for(ItemStack item : graveContents) {
                if(item != null && item.getType() != Material.AIR){
                    world.dropItemNaturally(location, item);
                }
            }
        }

        graves.remove(graveId);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Inventory inventory = player.getInventory();

        boolean hasItems = false;

        for(ItemStack item : inventory.getContents()){
            if(item != null && item.getType() != Material.AIR){
                hasItems = true;
                break;
            }
        }

        if(!hasItems) return;

        Location location = player.getLocation();
        Block block = location.getBlock();

        block.setType(Material.PLAYER_HEAD, false);

        Skull skull = (Skull) block.getState();
        skull.setOwningPlayer(player);
        skull.update();

        UUID graveId = UUID.randomUUID();
        Grave grave = new Grave(
                player.getName(),
                block.getLocation(),
                System.currentTimeMillis(),
                graveId,
                inventory.getContents()
        );
        graves.put(graveId, grave);

        event.getDrops().clear();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event){
        if(event.getAction().isLeftClick()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if(block == null) return;

        if(block.getType() != Material.PLAYER_HEAD) return;
        Location location = block.getLocation();

        UUID graveId = null;
        Grave graveData = null;

        for(Map.Entry<UUID, Grave> entry : graves.entrySet()) {
            if(entry.getValue().getLocation().equals(location)) {
                graveId = entry.getKey();
                graveData = entry.getValue();
                break;
            }
        }

        if(graveId == null) return;

        String ownerName = graveData.getName();

        Inventory customInventory = Bukkit.createInventory(
                new GraveHolder(graveId),
                54,
                Component.text(ownerName+"'s Grave"));
        customInventory.setContents(graveData.getItems());

        event.getPlayer().openInventory(customInventory);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof GraveHolder holder) {
            UUID graveId = holder.getGraveId();
            Grave grave = graves.get(graveId);
            ItemStack[] closedItems = event.getInventory().getContents();

            boolean isEmpty = true;

            for(ItemStack item : closedItems){
                if(item != null && item.getType() != Material.AIR) {
                    isEmpty = false;
                    break;
                }
            }

            if(isEmpty) {
                event.getPlayer().getWorld().setType(grave.getLocation(),Material.AIR);
                graves.remove(graveId);
                return;
            };

            if (grave != null) {
                grave.setContents(event.getInventory().getContents());
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event){
        Block block = event.getBlock();

        removeGrave(block);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event){
        List<Block> blocks = event.blockList();

        for(Block block : blocks) {
            if(block.getType() != Material.PLAYER_HEAD) continue;

            removeGrave(block);
        }
    }
}

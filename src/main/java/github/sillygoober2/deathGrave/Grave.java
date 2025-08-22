package github.sillygoober2.deathGrave;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Grave {
    private final String playerName;
    private final Location location;
    private final long deathTime;
    private final UUID graveId;
    private ItemStack[] items;

    public Grave(String playerName, Location location, long deathTime, UUID graveId, ItemStack[] items) {
        this.playerName = playerName;
        this.location = location;
        this.deathTime = deathTime;
        this.graveId = graveId;
        this.items = items;
    }

    public void setContents(ItemStack[] newContents){
        this.items = newContents.clone();
    }

    public String getName() {return playerName;}
    public Location getLocation() {return location;}
    public long getDeathTime() {return deathTime;}
    public UUID getGraveId() {return graveId;}
    public ItemStack[] getItems() {return items;}
}

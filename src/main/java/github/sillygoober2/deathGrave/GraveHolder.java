package github.sillygoober2.deathGrave;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class GraveHolder implements InventoryHolder {
    private final UUID graveId;

    public GraveHolder(UUID graveId) {
        this.graveId = graveId;
    }
    public UUID getGraveId() {return graveId;}

    @Override
    public Inventory getInventory(){
        return null;
    }
}

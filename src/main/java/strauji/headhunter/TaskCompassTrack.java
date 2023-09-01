package strauji.headhunter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.UUID;

public class TaskCompassTrack extends BukkitRunnable {

    private HeadHunter pluginInstance;
    FileConfiguration headHunter;
    private Player player;
    public TaskCompassTrack(HeadHunter p , FileConfiguration headHunter, Player helder) {
        this.headHunter = headHunter;
        this.player = helder;
        this.pluginInstance = p;
    }

    @Override
    public void run() {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if(itemStack.getType() == Material.COMPASS){
            NamespacedKey tracked  = new NamespacedKey(pluginInstance, "tracked_id");
            CompassMeta meta = (CompassMeta) itemStack.getItemMeta();
            Location lodestone = (player.getLocation().add((new Vector(1000*Math.random(),0,1000*Math.random()).rotateAroundX(Math.random()*180))));

            if(meta.getPersistentDataContainer().has(tracked, PersistentDataType.STRING)) {
                String tracked_id = meta.getPersistentDataContainer().get(tracked, PersistentDataType.STRING);
                String headTracked = tracked_id + ".head.";
                Location loc = null;
                if( headHunter.contains(headTracked + "position")){
                    loc = (Location) headHunter.get(headTracked + "position");
                }
                CompassMeta compassMeta = (CompassMeta) itemStack.getItemMeta();
                compassMeta.setLodestoneTracked(false);
                boolean carried = Boolean.valueOf(headHunter.get(headTracked + "inInventory").toString());
                if (carried) { //dropped, on inventory, carried by another player
                    String carrierId = headHunter.get(headTracked + "last_wielder").toString();
                    UUID carrierUUID = UUID.fromString(carrierId);
                    Object carrier = Bukkit.getEntity(carrierUUID);
                    if(loc != null && loc.getBlock().getState() instanceof   InventoryHolder){
                        lodestone = loc;
                    }else if (carrier instanceof InventoryHolder) {
                        lodestone = (((Entity) carrier).getLocation());
                    }
                }else if(Boolean.valueOf(headHunter.get(headTracked + "dropped").toString())){
                    Object last_wielder = headHunter.get(headTracked+"last_wielder");
                    assert last_wielder != null;
                    String suuid = last_wielder.toString();
                    UUID duuid = UUID.fromString(suuid);
                    Entity target = Bukkit.getEntity(duuid);
                    lodestone =  (target.getLocation()); //its on a block like chest
                }else if( Boolean.valueOf(headHunter.get(headTracked + "placed").toString())) {
                    lodestone = (loc); //its on a block like chest
                }else{
                    String carrierId = headHunter.get(headTracked + "last_wielder").toString();
                    UUID carrierUUID = UUID.fromString(carrierId);
                    Entity carrier = Bukkit.getEntity(carrierUUID);
                    if (carrier instanceof Player) {
                        lodestone = (carrier.getLocation());

                    }

                }
                if (player.getLocation().distanceSquared(lodestone) < pluginInstance.compassBufferRadiusSquared){
                    lodestone = (player.getLocation().add((new Vector(1000*Math.random(),0,1000*Math.random()).rotateAroundX(Math.random()*180))));

                }
                compassMeta.setLodestone(lodestone);
                itemStack.setItemMeta(compassMeta);
            }

        }else{
            cancel();
        }
    }

}
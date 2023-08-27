package strauji.headhunter;

import com.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CustomEventHandler implements Listener {
    private HeadHunter pluginInstance = (HeadHunter) Bukkit.getPluginManager().getPlugin("HeadHunter");
    FileConfiguration headHunter;
    @EventHandler //This should give the player their head when they first join only
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (headHunter == null) headHunter =  headHunter = pluginInstance.getHeadHunter();
        headHunter.set(player.getName()+".uuid", player.getUniqueId().toString());
        if (true || !headHunter.contains(player.getUniqueId()+".received.head")){
            headHunter.set(player.getUniqueId()+".received.head", true);
            Bukkit.broadcastMessage("Welcome to the server, " +player.getDisplayName() + ", as an ONE TIME reward");
            Bukkit.broadcastMessage("we're putting a bounty on your head, so get ready!");
            VoodoPlayerHead item = new VoodoPlayerHead(player);
            // Give the player our items (comma-seperated list of all ItemStack)
            player.getInventory().addItem(item.getInternalReference());
        }
        if(headHunter.contains(player.getUniqueId()+".head.impendingDoom")){ //Makes the player face their fate if they were killed while offline
            if(headHunter.get(player.getUniqueId()+".head.impendingDoom").equals(true)){
                Doom(player);
                headHunter.set(player.getUniqueId()+".head.impendingDoom", false);
                pluginInstance.SaveToDisk();
            }


        }


    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e){
        Player player = e.getPlayer();
        PlayerInventory inventory = player.getInventory();
        if(inventory.contains(Material.PLAYER_HEAD)){

            inventory.all(Material.PLAYER_HEAD).forEach((key, value)->
            {

                if(RemoveHead(value, player)){
                    inventory.remove(value);
                }
            }
        );

        }
    }
    @EventHandler //This should check if the head was dropped
    public void onPlayerDropItem (PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        Item drop = e.getItemDrop();
        if(drop.getItemStack().getType() == Material.PLAYER_HEAD){
          //  HeadHunted(drop.getItemStack());


        }
    }
    @EventHandler
    public void onItemDespawn(ItemDespawnEvent e){
        ItemStack item = e.getEntity().getItemStack();
        if(item.getType() == Material.PLAYER_HEAD){
            HeadHunted(item);
        }
    }
    @EventHandler
    public  void onEntityCombust(EntityCombustEvent e){
        if  (e.getEntity().getType() == EntityType.DROPPED_ITEM){
            ItemStack item = ((Item)e.getEntity()).getItemStack();
            HeadHunted(item);

        }

    }
    @EventHandler
    public void onEntityDamage(EntityDamageEvent e){
        if  (e.getEntity().getType() == EntityType.DROPPED_ITEM){
            ItemStack item = ((Item)e.getEntity()).getItemStack();
            HeadHunted(item);
            item.setType(Material.AIR);
        }
    }
    @EventHandler
    public void onBlockPistonExtend(BlockPistonExtendEvent e){
        for (Block E : e.getBlocks()) {
            if(CheckIfVoodoHead(E)){
                e.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onBlockPistonRetract(BlockPistonRetractEvent e){
        for (Block E : e.getBlocks()) {
            if(CheckIfVoodoHead(E)){
                e.setCancelled(true);
            }
        }


    }
    @EventHandler
    public void onBlockFromTo(BlockFromToEvent e){
        Block to_block = e.getToBlock();
        if(CheckIfVoodoHead(to_block)){
            e.setCancelled(true);
        }
    }
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e){
        Block block = e.getBlock();
        if(CheckIfVoodoHead(block)){
            e.setDropItems(false);
            Player breaker = e.getPlayer();
            BlockHeadToSkull(breaker, block);
            breaker.getWorld().getBlockAt(block.getLocation()).setType(Material.AIR);
        }


    }
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e){
        List<Block> blockListCopy = new ArrayList<Block>();
        blockListCopy.addAll(e.blockList());
        for (Block E : blockListCopy) {
            if(CheckIfVoodoHead(E)) e.blockList().remove(E);
        }

    }
    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent e){
        ItemStack slot0 = e.getInventory().getItem(0);
        ItemStack slot1 = e.getInventory().getItem(1);
        String name = e.getInventory().getRenameText();
        if(slot0 != null && slot0.getType() == Material.PLAYER_HEAD){
            if(headHunter.contains(name+".uuid")){ //TODO make sure that the player can't use a voodoo head
                String renamed = "The head of "+ name;
                String suuid = headHunter.get(name+".uuid").toString();
                UUID uuid = UUID.fromString(suuid);
                OfflinePlayer newplayer = Bukkit.getOfflinePlayer(uuid);
                VoodoPlayerHead item = new VoodoPlayerHead(newplayer);
                e.getInventory().setRepairCost(32);
                e.getInventory().setMaximumRepairCost(32);
                e.setResult(item.getInternalReference());
                Bukkit.broadcastMessage("An unholy mark have been laid upon this cursed land! The " + newplayer.getName() + " have been ressurected!");

            }
        }
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e){
        //TODO prevent players from placing head on hoppers, dispensers, enderchests and so
    }
    private boolean CheckIfVoodoHead(Block skull){
        CustomBlockData customBlockData = new CustomBlockData(skull, pluginInstance);
        NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");
        return  (skull.getType() == Material.PLAYER_HEAD && customBlockData.has(owner_id, PersistentDataType.STRING));
    }
    private boolean RemoveHead(ItemStack head, Player player){
        NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");
        NamespacedKey last_wielder_id  = new NamespacedKey(pluginInstance, "last_wielder");
        ItemMeta headMeta = head.getItemMeta();
        Location pos= player.getLocation();
        if(headMeta.getPersistentDataContainer().has(owner_id, PersistentDataType.STRING)) {
            World world = player.getWorld();
            Block emptySpace = getNearestEmptySpace(pos, 10, world);
            if(emptySpace != null){
                emptySpace.setType(Material.PLAYER_HEAD);
                String owner = headMeta.getPersistentDataContainer().get(owner_id, PersistentDataType.STRING);
                String last_wielder = headMeta.getPersistentDataContainer().get(last_wielder_id, PersistentDataType.STRING);
                UUID uuid_owner = UUID.fromString(owner);
                OfflinePlayer owner_player =  Bukkit.getOfflinePlayer(uuid_owner);
                SkullMeta meta = (SkullMeta) headMeta;
                CustomBlockData customBlockData = new CustomBlockData(emptySpace, pluginInstance);
                customBlockData.set(owner_id, PersistentDataType.STRING, owner);
                customBlockData.set(last_wielder_id, PersistentDataType.STRING, last_wielder);
                Skull skull_block  =(Skull) emptySpace.getState();
                skull_block.setOwningPlayer(owner_player);
                headHunter.set(player.getUniqueId()+".head.position", skull_block.getBlock().getLocation());
                skull_block.update();
                pluginInstance.SaveToDisk();
                return true;
            }
        }
        return false;
    }
    public Block getNearestEmptySpace(Location pos, int maxradius, World world) {
        for(int i = -maxradius; i< maxradius; i++){
            for(int j = -maxradius; j< maxradius; j++){
                for(int w = -maxradius; w< maxradius; w++){
                    Location deviation = new Location(world, i, j, w);
                    Location new_location = pos.clone();
                    Block bl = world.getBlockAt(new_location.add(deviation));
                    if (bl.getType() == Material.AIR){

                        return bl;
                    }
                }
            }
        }
        return null;
    }
    private  void BlockHeadBeGone(Block skull){
        BlockHeadHunted(skull);
        skull.getWorld().getBlockAt(skull.getLocation()).setType(Material.AIR);
    }
    private void BlockHeadToSkull(Player player, Block skull){
        BlockState blockState = skull.getState();
        CustomBlockData customBlockData = new CustomBlockData(skull, pluginInstance);
        NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");
        if (customBlockData.has(owner_id, PersistentDataType.STRING)) {
            NamespacedKey last_wielder  = new NamespacedKey(pluginInstance, "last_wielder");
            String owner =customBlockData.get(owner_id, PersistentDataType.STRING);
            UUID owner_uuid = UUID.fromString(owner);
            OfflinePlayer affected = Bukkit.getOfflinePlayer(owner_uuid);
            if(affected.hasPlayedBefore()) {
                VoodoPlayerHead item = new VoodoPlayerHead(affected);
                player.getInventory().addItem(item.getInternalReference());
                headHunter.set(player.getUniqueId()+".head.position", player.getLocation());
                headHunter.set(player.getUniqueId()+".head.last_wielder", player.getUniqueId().toString());
            }
        }
    }
    private void BlockHeadHunted(Block block){
        BlockState blockState = block.getState();

        CustomBlockData customBlockData = new CustomBlockData(block, pluginInstance);
        NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");
        if (customBlockData.has(owner_id, PersistentDataType.STRING)){


            NamespacedKey last_wielder  = new NamespacedKey(pluginInstance, "last_wielder");

            String owner =customBlockData.get(owner_id, PersistentDataType.STRING);
            UUID owner_uuid = UUID.fromString(owner);
            OfflinePlayer affected = Bukkit.getOfflinePlayer(owner_uuid);
            if(affected.hasPlayedBefore()){
                if(affected.isOnline() ){
                    Doom(affected.getPlayer());

                }else{
                    pluginInstance.getHeadHunter().set(affected.getUniqueId()+".head.impendingDoom", true);
                    pluginInstance.getHeadHunter().set(affected.getUniqueId()+".head.destroyed", true);
                }
            }
        }
    }
    private void HeadHunted(ItemStack head){
        NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");
        NamespacedKey last_wielder  = new NamespacedKey(pluginInstance, "last_wielder");
        ItemMeta headMeta = head.getItemMeta();
        if(headMeta.getPersistentDataContainer().has(owner_id, PersistentDataType.STRING)){
            String owner = headMeta.getPersistentDataContainer().get(owner_id, PersistentDataType.STRING);
            String last_wieldr = headMeta.getPersistentDataContainer().get(last_wielder, PersistentDataType.STRING);
            UUID uuid_owner = UUID.fromString(owner);
            UUID uuid_killer = UUID.fromString(last_wieldr);
            OfflinePlayer affected =  Bukkit.getOfflinePlayer(uuid_owner);
            OfflinePlayer killer = Bukkit.getOfflinePlayer(uuid_killer);
            if(affected.hasPlayedBefore()){
                if(affected.isOnline()){
                    Doom(affected.getPlayer());
                }else{ //TRACK THE HEAD
                    pluginInstance.getHeadHunter().set(affected.getUniqueId()+".head.impendingDoom", true);
                    pluginInstance.getHeadHunter().set(affected.getUniqueId()+".head.destroyed", true);
                }

            }

        }




    }
    private void Doom(Player affected){
        if(affected.getPlayer().getGameMode() != GameMode.SPECTATOR){
            affected.getPlayer().setHealth(0);
            affected.getPlayer().setGameMode(GameMode.SPECTATOR); //then remove from the game
            affected.getPlayer().saveData();
            Bukkit.broadcastMessage("The " + affected.getDisplayName() + "'s head have been claimed!");
        }
    }
}

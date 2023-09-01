package strauji.headhunter;

import com.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class CustomEventHandler implements Listener {
    private HeadHunter pluginInstance = (HeadHunter) Bukkit.getPluginManager().getPlugin("HeadHunter");
    FileConfiguration headHunter;
    @EventHandler //This should give the player their head when they first join only
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (headHunter == null) headHunter = pluginInstance.getHeadHunter();

        if (!headHunter.contains(player.getUniqueId()+".receivedHead") ||!Boolean.valueOf(headHunter.get(player.getUniqueId()+".receivedHead").toString())){
            pluginInstance.resetPlayer(player.getUniqueId());
            headHunter.set(player.getName()+".uuid", player.getUniqueId().toString());
            headHunter.set(player.getUniqueId()+".receivedHead", true);

            Bukkit.broadcastMessage(String.format(pluginInstance.getConfig().get("msg."+pluginInstance.languageId+".firstLogin").toString(),player.getDisplayName()));

            VoodoPlayerHead item = new VoodoPlayerHead(player, false);
            player.getInventory().addItem(item.getInternalReference());
            UpdateHeadTracker(item, (OfflinePlayer) player);
        }else{
            if(headHunter.contains(player.getUniqueId()+".head.impendingDoom")){ //Makes the player face their fate if they were killed while offline
                if(headHunter.get(player.getUniqueId()+".head.impendingDoom").equals(true)){
                    Doom(player);
                    headHunter.set(player.getUniqueId()+".head.impendingDoom", false);
                    pluginInstance.SaveToDisk();
                }
            }
            if((!player.getWorld().getPVP() && pluginInstance.resurrectOffPVP)){
                resurrectPlayer(player.getUniqueId());
            }else if (  !CheckIfHeadExists(player.getUniqueId().toString()) && player.getGameMode() == GameMode.SURVIVAL) {

                Doom(player);
            }

        }


    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e){
        Player player = e.getPlayer();
        PlayerInventory inventory = player.getInventory();
        if (headHunter == null) headHunter = pluginInstance.getHeadHunter();
        if(inventory.contains(Material.PLAYER_HEAD)){
            inventory.all(Material.PLAYER_HEAD).forEach((key, value)->
            {
                if(RemoveHead(value, player)){

                    inventory.remove(value);
                }
            }
        );

        }
        ItemStack helmet = inventory.getHelmet();
        ItemStack chest = inventory.getChestplate();
        ItemStack boots = inventory.getBoots();
        ItemStack leggings = inventory.getLeggings();
        ItemStack offhand = inventory.getItemInOffHand();

        if(null != helmet && RemoveHead(helmet, player)) inventory.setHelmet(null);
        if(null != offhand && RemoveHead(offhand, player)) inventory.setItemInOffHand(null);
        if(null != chest && RemoveHead(chest, player)) inventory.setChestplate(null);
        if(null != boots && RemoveHead(boots, player)) inventory.setBoots(null);
        if(null != leggings && RemoveHead(leggings, player)) inventory.setLeggings(null);
    }


    public void resurrectPlayer(UUID uuid){
        Object doomed =  headHunter.get(uuid+".head.doomed");
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if(CheckIfHeadExists(uuid.toString()) ){



            if(pluginInstance.banKilledPlayer && player.isBanned()){
                String nick =player.getName();
                if(Bukkit.getBanList(BanList.Type.NAME).getBanEntry(nick).getSource() == "HEADHUNTER"){
                    Bukkit.getBanList(BanList.Type.NAME).pardon(nick);
                }
            }
            if(player.isOnline()) {
                if ((player.getPlayer().getGameMode() == GameMode.SPECTATOR) ) {
                    player.getPlayer().setGameMode(GameMode.SURVIVAL);
                }
            }
            headHunter.set(uuid+".head.doomed", false);
        }else if(pluginInstance.resurrectOffPVP && !player.getPlayer().getWorld().getPVP() && player.isOnline() ){
            if ((player.getPlayer().getGameMode() == GameMode.SPECTATOR) ) {
                player.getPlayer().setGameMode(GameMode.SURVIVAL);
            }
        }

    }
    public void updateDoomed(UUID uuid){
        String owner = uuid.toString();
        //pluginInstance.resetPlayer(uuid);
        headHunter.set(owner+".head.destroyed", true);
        if(pluginInstance.increaseDoomLevel){
            headHunter.set(uuid.toString()+".head.doomLevel",pluginInstance.incrementPerDoom+ Integer.valueOf(headHunter.get(uuid.toString()+".head.doomLevel").toString()));
        }
        headHunter.set(owner+".receivedHead", true);
    }
    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent e){ //Prevent non players from picking up the head
        Entity entity = e.getEntity();
        ItemStack itemStack = e.getItem().getItemStack();
        if(CheckIfVoodoHead(itemStack)) {
            if (entity.getType() != EntityType.PLAYER) {
                e.setCancelled(true);
            } else {
                Player player = (Player) entity; //Cast it to player just so we can pinpoit it's location and uniqueid
                UpdateHeadTracker(itemStack, player);
            }
        }

    }
    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent e){
        ItemStack itemStack  = e.getItem();
        if(CheckIfVoodoHead(itemStack)) e.setCancelled(true); //This should prevent the head from being moved preventing exploits with shulkers and enderchests
    }
    @EventHandler
    public  void onItemSpawn(ItemSpawnEvent e){
        ItemStack itemStack = e.getEntity().getItemStack();

        if(CheckIfVoodoHead(itemStack)) {
            UpdateHeadTracker(itemStack, e.getEntity().getUniqueId());

        }
    }
    @EventHandler
    public void onItemDespawn(ItemDespawnEvent e){
        if  (!e.getEntity().isDead() && e.getEntity().getType() == EntityType.DROPPED_ITEM){
            preventNonPVPHeadHunt(e.getEntity());
        }
    }
    @EventHandler
    public  void onEntityCombust(EntityCombustEvent e){
        if  (!e.getEntity().isDead() && e.getEntity().getType() == EntityType.DROPPED_ITEM){
            preventNonPVPHeadHunt(e.getEntity());
        }

    }
    @EventHandler
    public void onEntityDamage(EntityDamageEvent e){

        if  (!e.getEntity().isDead() && e.getEntity().getType() == EntityType.DROPPED_ITEM){
            preventNonPVPHeadHunt(e.getEntity());
        }
    }
    public void preventNonPVPHeadHunt(Entity e){
        ItemStack item = ((Item)e).getItemStack();
        if(CheckIfVoodoHead(item)){
            if(!pluginInstance.ObeyPVP || e.getWorld().getPVP() ){
                HeadHunted(item);
                e.getWorld().strikeLightning(e.getLocation());
            }else{
                placeHead(item, getNearestEmptySpace(e.getLocation(), 20, e.getWorld()).getLocation());
            }
            e.remove();
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
            NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");
            CustomBlockData customBlockData = new CustomBlockData(block, pluginInstance);
            String owner =customBlockData.get(owner_id, PersistentDataType.STRING);
            UUID owner_uuid = UUID.fromString(owner);
            OfflinePlayer affected = Bukkit.getOfflinePlayer(owner_uuid);
            e.setDropItems(false);
            Player breaker = e.getPlayer();
            if(((!pluginInstance.ObeyPVP || e.getBlock().getWorld().getPVP() )) || breaker.getUniqueId() == affected.getUniqueId()){
                BlockHeadToSkull(breaker, block);
                breaker.getWorld().getBlockAt(block.getLocation()).setType(Material.AIR);
            }else{
                e.setCancelled(true);
            }

        }
    }
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e){
        List<Block> blockListCopy = new ArrayList<Block>(e.blockList());
        for (Block E : blockListCopy) {
            try {
                if (CheckIfVoodoHead(E)) e.blockList().remove(E);
            }catch (Exception j){
                e.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e){
        ItemStack item = e.getItemDrop().getItemStack();
        if(CheckIfVoodoHead(item)){
            UpdateHeadTracker(item, e.getPlayer());
        }
    }
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e){
        ItemStack item = e.getItemInHand();
       if(CheckIfVoodoHead(item)) placeHead(item, e.getBlock().getLocation());
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent e){
        ItemStack slot0 = e.getInventory().getItem(0);
        ItemStack slot1 = e.getInventory().getItem(1);
        String name = e.getInventory().getRenameText();

        if(slot0 != null){
            if(!CheckIfVoodoHead(slot0) && slot0.getType() == Material.PLAYER_HEAD){
                if( headHunter.contains(name+".uuid")){
                    String suuid = headHunter.get(name+".uuid").toString();
                    UUID uuid = UUID.fromString(suuid);
                    OfflinePlayer newplayer = Bukkit.getOfflinePlayer(uuid);
                    boolean destroyed = !CheckIfHeadExists(suuid);
                    if(destroyed && pluginInstance.allowRessurecting){
                        VoodoPlayerHead item = new VoodoPlayerHead(newplayer, false);
                        e.getInventory().setRepairCost(pluginInstance.minDoomLevel+Integer.valueOf(headHunter.get(suuid+".head.doomLevel").toString()));
                        e.getInventory().setMaximumRepairCost(pluginInstance.maxDoomLevel);
                        e.setResult(item.getInternalReference());
                    } else if (pluginInstance.allowDecoy) {
                        VoodoPlayerHead item = new VoodoPlayerHead(newplayer, true);
                        e.setResult(item.getInternalReference());
                    }
                }
            }
            if(slot0.getType() == Material.COMPASS){
                if( headHunter.contains(name+".uuid")){
                    if(pluginInstance.allowHeadCompass){
                        String suuid = headHunter.get(name+".uuid").toString();
                        UUID uuid = UUID.fromString(suuid);
                        OfflinePlayer newplayer = Bukkit.getOfflinePlayer(uuid);
                        CompassMeta compassMeta =(CompassMeta) slot0.getItemMeta();
                        ItemStack newcompass = new ItemStack(Material.COMPASS,1);
                        NamespacedKey tracked  = new NamespacedKey(pluginInstance, "tracked_id");
                        compassMeta.getPersistentDataContainer().set(tracked, PersistentDataType.STRING,suuid);


                        compassMeta.setDisplayName(String.format(pluginInstance.getConfig().get("msg."+pluginInstance.languageId+".compassName").toString(),newplayer.getName()));
                        e.getInventory().setRepairCost(pluginInstance.compassCost);
                        e.getInventory().setMaximumRepairCost(pluginInstance.compassCost);
                        newcompass.setItemMeta(compassMeta);
                        newcompass.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);
                        e.setResult(newcompass);

                    }
                }
            }


        }
    }
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent e){
        BukkitTask task = new TaskCompassTrack(pluginInstance, headHunter, e.getPlayer()).runTaskTimer(pluginInstance, 5, 10);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e){
        ItemStack itemStack = e.getCursor();

        if(CheckIfVoodoHead(itemStack)){
            Inventory inventory= e.getClickedInventory();
            if(inventory.getType() == InventoryType.ENDER_CHEST || inventory.getType()== InventoryType.SHULKER_BOX){
                e.setCancelled(true);
            }
            UpdateHeadTracker(itemStack, (OfflinePlayer) e.getWhoClicked());

        }

    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e){
        Inventory inventory = e.getInventory();

        inventory.all(Material.PLAYER_HEAD).forEach((key, value)-> {
            if (CheckIfVoodoHead(value)) {
                UpdateHeadTracker((ItemStack) value, (OfflinePlayer) e.getPlayer(), inventory);
            }
        });

    }
    @EventHandler(priority = EventPriority.HIGHEST )
    public void onInventoryClose(InventoryCloseEvent e){
        Inventory inventory = e.getInventory();
        Player player = (Player) e.getPlayer();
        if(inventory.getType() == InventoryType.ENDER_CHEST || inventory.getType()== InventoryType.SHULKER_BOX){
           if(inventory.contains(Material.PLAYER_HEAD)){

               inventory.all(Material.PLAYER_HEAD).forEach((key, value)->
                       {
                           if(InventoryDropHead(value, player)){
                               inventory.remove(value);
                           }
                       }
               );
           }
        }else{
            if(inventory.contains(Material.PLAYER_HEAD)) { //Register it as in inventory

                inventory.all(Material.PLAYER_HEAD).forEach((key, value)-> {
                           UpdateHeadTracker(value, player, inventory);
                });

            }
        }
        player.getInventory().all(Material.PLAYER_HEAD).forEach((key, value)-> {
            UpdateHeadTracker(value, player);
        });
    }
    private boolean CheckIfVoodoHead(Block skull){
        CustomBlockData customBlockData = new CustomBlockData(skull, pluginInstance);
        NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");

        return  (skull.getType() == Material.PLAYER_HEAD && customBlockData.has(owner_id, PersistentDataType.STRING));
    }
    private boolean CheckIfVoodoHead(ItemStack head){
        boolean isHead = head.getType() == Material.PLAYER_HEAD;
        if (!isHead) return false;
        NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");
        NamespacedKey last_wielder_id  = new NamespacedKey(pluginInstance, "last_wielder");
        ItemMeta headMeta = head.getItemMeta();
        boolean hasOwner = headMeta.getPersistentDataContainer().has(owner_id, PersistentDataType.STRING);
        return (hasOwner);
    }
    private boolean InventoryDropHead(ItemStack head,Player cause){
        if(CheckIfVoodoHead(head)){
            cause.getWorld().dropItem(cause.getLocation(), head);
            UpdateHeadTracker(head, (OfflinePlayer) cause);
            return true;
        }
        return false;
    }
    private boolean RemoveHead(ItemStack head, Player player){
        if (head.getType() == Material.AIR) return true; //sometimes a bit of air slips in
        Location pos= player.getLocation();
        Block emptySpace = getNearestEmptySpace(pos, 10, player.getWorld());
        emptySpace = placeHead(head, emptySpace.getLocation());
        NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");
        CustomBlockData customBlockData = new CustomBlockData(emptySpace, pluginInstance);
        String owner_uuid = customBlockData.get(owner_id, PersistentDataType.STRING).toString();
        String player_uuid =((OfflinePlayer)player).getUniqueId().toString();


        return (player_uuid.equals(owner_uuid));
    }
    public Block placeHead(ItemStack head, Location pos){
        if(CheckIfVoodoHead(head)){
                Block emptySpace = pos.getBlock();
                NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");
                emptySpace.setType(Material.PLAYER_HEAD);
                ItemMeta headMeta = head.getItemMeta();
                String owner = headMeta.getPersistentDataContainer().get(owner_id, PersistentDataType.STRING);
                UUID uuid_owner = UUID.fromString(owner);
                OfflinePlayer owner_player =  Bukkit.getOfflinePlayer(uuid_owner);
                SkullMeta meta = (SkullMeta) headMeta;
                CustomBlockData customBlockData = new CustomBlockData(emptySpace, pluginInstance);
                customBlockData.set(owner_id, PersistentDataType.STRING, owner);
                Skull skull_block  =(Skull) emptySpace.getState();
                skull_block.setOwningPlayer(owner_player);
                skull_block.update();
                UpdateHeadTracker(emptySpace);
                return emptySpace;

            }
        return pos.getBlock();
    }
    public Block getNearestEmptySpace(Location pos, int maxradius, World world) {
        for(int i = -1; i< maxradius*2; i++){
            for(int j = -1; j< maxradius*2; j++){
                for(int w = -1; w< maxradius*2; w++){
                    int multi = (i < maxradius/2)? 1:-1;
                    int multj = (j < maxradius/2)? 1:-1;
                    int multw = (w < maxradius/2)? 1:-1;
                    Location deviation = new Location(world, multi*i, multj*j, multw*w);
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

    private void BlockHeadToSkull(Player player, Block skull){
        CustomBlockData customBlockData = new CustomBlockData(skull, pluginInstance);
        NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");
        if (customBlockData.has(owner_id, PersistentDataType.STRING)) {
            String owner =customBlockData.get(owner_id, PersistentDataType.STRING);
            UUID owner_uuid = UUID.fromString(owner);
            OfflinePlayer affected = Bukkit.getOfflinePlayer(owner_uuid);
            if(affected.hasPlayedBefore()) {
                VoodoPlayerHead item = new VoodoPlayerHead(affected, false);
                if(player.getInventory().firstEmpty() != -1){ //Try to look for an open space in inv. if there's none it will return -1
                    player.getInventory().addItem(item.getInternalReference());
                }else {//then we will drop the head as an item, because the head shouldn't be deleted
                   player.getWorld().dropItem(player.getLocation(), item);
                }
                UpdateHeadTracker(item.getInternalReference(), player);

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
                    headHunter.set(affected.getUniqueId()+".head.impendingDoom", true);

                }
                headHunter.set(affected.getUniqueId()+".head.destroyed", true);
            }

        }
    }
    private void UpdateHeadTracker(ItemStack head, UUID entity_id ){
        if(CheckIfVoodoHead(head)){
            NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");

            ItemMeta headMeta = head.getItemMeta();
            String owner = headMeta.getPersistentDataContainer().get(owner_id, PersistentDataType.STRING);

            //   UUID uuid_owner = UUID.fromString(owner);

            headHunter.set(owner+".head.destroyed", false);
            headHunter.set(owner+".head.impendingDoom", false);
            headHunter.set(owner+".head.inInventory", false);
            headHunter.set(owner+".head.dropped", true);
            headHunter.set(owner+".head.last_wielder", entity_id.toString());
            headHunter.set(owner+".head.placed", false);
            resurrectPlayer(UUID.fromString(owner));
            pluginInstance.SaveToDisk();
        }
    }
    private void UpdateHeadTracker(ItemStack head, OfflinePlayer wielder ){
        if(CheckIfVoodoHead(head)){
            NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");

            ItemMeta headMeta = head.getItemMeta();
            String owner = headMeta.getPersistentDataContainer().get(owner_id, PersistentDataType.STRING);

            headHunter.set(owner+".head.destroyed", false);
            headHunter.set(owner+".head.last_wielder", wielder.getUniqueId().toString());
            headHunter.set(owner+".head.position", new Location(null, 0.0,0.0,0.0));
            headHunter.set(owner+".head.impendingDoom", false);
            headHunter.set(owner+".head.inInventory", false);
            headHunter.set(owner+".head.dropped", false);
            headHunter.set(owner+".head.placed", false);
            resurrectPlayer(UUID.fromString(owner));
            pluginInstance.SaveToDisk();
        }
    }
    private void UpdateHeadTracker(ItemStack head, OfflinePlayer wielder, Inventory inventory ){
        if(CheckIfVoodoHead(head)){
            NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");

            ItemMeta headMeta = head.getItemMeta();
            String owner = headMeta.getPersistentDataContainer().get(owner_id, PersistentDataType.STRING);

            //   UUID uuid_owner = UUID.fromString(owner);
            headHunter.set(owner+".head.destroyed", false);
            if(inventory.getHolder() instanceof Entity ){
                headHunter.set(owner+".head.last_wielder", ((Entity) inventory.getHolder()).getUniqueId().toString());
                headHunter.set(owner+".head.position", null);
            }else{
                headHunter.set(owner+".head.last_wielder", wielder.getUniqueId().toString());
                headHunter.set(owner+".head.position", inventory.getLocation());
            }

            headHunter.set(owner+".head.inInventory", true);
            headHunter.set(owner+".head.dropped", false);
            headHunter.set(owner+".head.placed", false);
            headHunter.set(owner+".head.impendingDoom",false);
            resurrectPlayer(UUID.fromString(owner));
            pluginInstance.SaveToDisk();
        }
    }
    private void UpdateHeadTracker(Block block){
        if(CheckIfVoodoHead(block)){
            BlockState blockState = block.getState();

            CustomBlockData customBlockData = new CustomBlockData(block, pluginInstance);
            NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");
            NamespacedKey last_wielder  = new NamespacedKey(pluginInstance, "last_wielder");

            String owner =customBlockData.get(owner_id, PersistentDataType.STRING);
            headHunter.set(owner+".head.destroyed", false);
            headHunter.set(owner+".head.inInventory", false);
            headHunter.set(owner+".head.position", block.getLocation());
            headHunter.set(owner+".head.impendingDoom",false);
            headHunter.set(owner+".head.dropped", false);
            headHunter.set(owner+".head.placed", true);
            resurrectPlayer(UUID.fromString(owner));
            pluginInstance.SaveToDisk();
        }
    }
    private boolean CheckIfHeadExists(String uuid){
        String uuid_head = uuid+".head.";
        Location loc = (Location)headHunter.get(uuid_head+"position");

        if((boolean) headHunter.get(uuid_head+"destroyed")) return false;
        if((boolean)headHunter.get(uuid_head+"inInventory")) {
            AtomicBoolean ret = new AtomicBoolean(false);
            if(loc != null){
                Block target = loc.getBlock();

                if(target.getState() instanceof InventoryHolder ){
                   Inventory inventory = (((InventoryHolder)target.getState()).getInventory());
                   inventory.all(Material.PLAYER_HEAD).forEach((key, value)->
                   {
                   if(CheckIfVoodoHead(value)){
                       NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");
                       ItemMeta headMeta = value.getItemMeta();
                       String head_uuid = (String)headMeta.getPersistentDataContainer().get(owner_id, PersistentDataType.STRING);
                       if(Bukkit.getOfflinePlayer(uuid).equals(Bukkit.getOfflinePlayer(head_uuid))) {
                            ret.set(true);
                       }
                    }
                   });
                }
                return  ret.get();
            }else{
                Object last_wielder = headHunter.get(uuid_head+"last_wielder");
                assert last_wielder != null;
                String suuid = last_wielder.toString();
                UUID duuid = UUID.fromString(suuid);
                Entity target = Bukkit.getEntity(duuid);

                if(null != target){
                    if(target instanceof InventoryHolder){
                       Inventory inventory =  ((InventoryHolder) target).getInventory();
                       inventory.all(Material.PLAYER_HEAD).forEach((key, value)->
                        {
                            if(CheckIfVoodoHead(value)){
                                NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");
                                ItemMeta headMeta = value.getItemMeta();
                                String head_uuid = (String)headMeta.getPersistentDataContainer().get(owner_id, PersistentDataType.STRING);
                                if(Bukkit.getOfflinePlayer(uuid).equals(Bukkit.getOfflinePlayer(head_uuid))) {
                                    ret.set(true);
                                }
                            }
                        });
                        return  ret.get();
                    }else{
                        return false;
                    }
                }else{
                    return false;
                }
            }
        }
        if((boolean) headHunter.get(uuid_head+"placed")){

            if(loc == null) return false;
            return CheckIfVoodoHead(loc.getBlock());
        }
        if((boolean) headHunter.get(uuid_head+"dropped")){
            Object last_wielder = headHunter.get(uuid_head+"last_wielder");
            assert last_wielder != null;
            String suuid = last_wielder.toString();
            UUID duuid = UUID.fromString(suuid);
            Entity target = Bukkit.getEntity(duuid);
            if(target == null) return false;

            if(target.getLastDamageCause() != null && target.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.VOID) return false;
            return (target.isValid() && !target.isDead());

        }
        if(headHunter.contains(uuid_head+"last_wielder")){
            Object last_wielder = headHunter.get(uuid_head+"last_wielder");
            assert last_wielder != null;
            String suuid = last_wielder.toString();
            UUID duuid = UUID.fromString(suuid);
            Entity target = Bukkit.getEntity(duuid);

            if(target == null) return false;
            AtomicBoolean ret = new AtomicBoolean(false);
            if(target instanceof  InventoryHolder){
                ((InventoryHolder) target).getInventory().all(Material.PLAYER_HEAD).forEach((integer, head) -> {
                    if(CheckIfVoodoHead(head)){
                        NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");
                        ItemMeta headMeta = head.getItemMeta();
                            if(headMeta.getPersistentDataContainer().has(owner_id, PersistentDataType.STRING)){
                                String owner = headMeta.getPersistentDataContainer().get(owner_id, PersistentDataType.STRING);
                                if (Bukkit.getOfflinePlayer(uuid).equals(Bukkit.getOfflinePlayer(owner))) ret.set(true);
                            }
                    }
                });
                return ret.get();
            }
        }
        return false;
    }

    private void Doom(Player affected){

        if(affected.getPlayer().getGameMode() != GameMode.SPECTATOR){



            if (pluginInstance.banKilledPlayer){
                Bukkit.getBanList(BanList.Type.NAME).addBan(affected.getName(), pluginInstance.getConfig().get("msg."+pluginInstance.languageId+".banMessage").toString(),null, "HEADHUNTER");
                affected.kickPlayer(pluginInstance.kickMessage);
            }else{
                affected.getPlayer().setHealth(0);
                affected.getPlayer().setGameMode(GameMode.SPECTATOR); //then remove from the game
            }
            headHunter.set(affected.getUniqueId().toString()+".head.doomed", true);
            Bukkit.broadcastMessage(String.format(pluginInstance.getConfig().get("msg."+pluginInstance.languageId+".headClaimed").toString(), affected.getDisplayName()));
            updateDoomed(affected.getUniqueId());

            pluginInstance.SaveToDisk();
            affected.getPlayer().saveData();
        }
    }
}

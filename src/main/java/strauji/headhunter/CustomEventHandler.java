package strauji.headhunter;

import com.jeff_media.customblockdata.CustomBlockData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CustomEventHandler implements Listener {
    private HeadHunter pluginInstance = (HeadHunter) Bukkit.getPluginManager().getPlugin("HeadHunter");
    FileConfiguration headHunter;
    @EventHandler //This should give the player their head when they first join only
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if(player.isOp() || player.hasPermission("headhunter.unhuntable")) return;
        if (headHunter == null) headHunter = pluginInstance.getHeadHunter();

        if((!headHunter.contains(player.getUniqueId()+".grace"))
                && pluginInstance.getConfig().contains("playerGraceSec"))
        {
            headHunter.set(player.getName() + ".uuid", player.getUniqueId().toString());
            headHunter.set(player.getUniqueId()+".grace", pluginInstance.getConfig().get("playerGraceSec"));
            int graceLeft =  Integer.parseInt(pluginInstance.getConfig().get("playerGraceSec").toString());
            player.setMetadata("grace", new FixedMetadataValue(pluginInstance,graceLeft ));
            startGraceTimer(1, player);

        }else if(headHunter.contains(player.getUniqueId()+".grace") &&
                (Integer.parseInt(headHunter.get(player.getUniqueId()+".grace").toString()) > 0))
        {
            int graceLeft =  (Integer.parseInt(headHunter.get(player.getUniqueId()+".grace").toString()));
            player.setMetadata("grace", new FixedMetadataValue(pluginInstance,graceLeft ));
            startGraceTimer(1, player);
        }

        else{
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
                if(headHunter.contains(player.getUniqueId().toString()+".head.safeRespawn"))
                    if(!Boolean.valueOf(headHunter.get(player.getPlayer().getUniqueId().toString()+".head.safeRespawn").toString()))
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
        if(CheckIfVoodoHead(helmet))
            if(RemoveHead(helmet, player)) inventory.setHelmet(null);
        if(CheckIfVoodoHead(offhand))
            if( RemoveHead(offhand, player)) inventory.setItemInOffHand(null);
        if(CheckIfVoodoHead(chest))
            if(RemoveHead(chest, player)) inventory.setChestplate(null);
        if(CheckIfVoodoHead(boots))
            if(RemoveHead(boots, player)) inventory.setBoots(null);
        if(CheckIfVoodoHead(leggings))
            if( RemoveHead(leggings, player)) inventory.setLeggings(null);
        if(player.hasMetadata("grace")) {
            headHunter.set(player.getUniqueId()+".grace", player.getMetadata("grace").get(0).asInt());
            pluginInstance.SaveToDisk();
        }
    }

    public void startGraceTimer( int interval, Player player){
        new BukkitRunnable(){
            @Override
            public void run() {
                if(player.hasMetadata("grace")){
                    if(!player.isOnline()){
                        this.cancel();
                        return;
                    }
                    int graceLeft = player.getMetadata("grace").get(0).asInt()-1;
                    player.removeMetadata("grace", pluginInstance);
                    if(graceLeft > 0){
                        player.setMetadata("grace", new FixedMetadataValue(pluginInstance, graceLeft));
                        int hours = (int) Math.floor((float)graceLeft/3600);
                        int minutes = (int) Math.floor(graceLeft - hours*3600)/60;
                        int seconds = (graceLeft-hours*3600) % 60;
                        sendActionbar(player, String.format(pluginInstance.getConfig().get("msg."
                                + pluginInstance.languageId + ".graceLeft").toString(),
                              hours, minutes, seconds ));
                    }else{ //Grace period ended
                        if (!headHunter.contains(player.getUniqueId()+".receivedHead")
                                ||!Boolean.valueOf(headHunter.get(player.getUniqueId()+".receivedHead").toString()))
                        {
                            pluginInstance.resetPlayer(player.getUniqueId());

                            headHunter.set(player.getUniqueId() + ".receivedHead", true);
                            player.sendMessage(String.format(pluginInstance.getConfig()
                                    .get("msg." + pluginInstance.languageId + ".firstLogin")
                                    .toString(), player.getDisplayName()));
                            VoodoPlayerHead item = new VoodoPlayerHead(player, false);
                            player.getInventory().addItem(item.getInternalReference());
                            UpdateHeadTracker(item, (OfflinePlayer) player);
                            headHunter.set(player.getUniqueId()+".grace", 0);
                            this.cancel();
                        }
                    }
                }
            }
        }.runTaskTimer(pluginInstance, interval, interval* 20L);

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
    private void sendActionbar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(format(message)));
    }
    private String format(String arg) {
        return ChatColor.translateAlternateColorCodes('&', arg);
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
       // if(e.getEntity() instanceof Item item){
            if(CheckIfVoodoHead(e.getEntity().getItemStack())) e.setCancelled(true);

    }
    @EventHandler
    public  void onEntityCombust(EntityCombustEvent e){
        if  (!e.getEntity().isDead() && e.getEntity().getType() == EntityType.DROPPED_ITEM){
            preventNonPVPHeadHunt(e.getEntity());
        }

    }
    @EventHandler
    public void onEntityDamage(EntityDamageEvent e){
        //if(e.getEntity() instanceof Item item){
     //       if(CheckIfVoodoHead(item.getItemStack())) e.setCancelled(true);
      //  }

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
                placeHead(item, e.getLocation());
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
           // if(((!pluginInstance.ObeyPVP || e.getBlock().getWorld().getPVP() )) || breaker.getUniqueId() == affected.getUniqueId()){

            BlockHeadToSkull(breaker, block);



          //  }else{
          //     e.setCancelled(true);
           // }
            breaker.getWorld().getBlockAt(block.getLocation()).getState().update(true,true);
        }
    }
    @EventHandler
    public void onPlayerDeathEvent(PlayerDeathEvent e){
        if(null == e.getEntity().getKiller()){
            e.getDrops().forEach(itemStack -> {
                if(CheckIfVoodoHead(itemStack)){
                    NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");
                    ItemMeta headMeta = itemStack.getItemMeta();
                    boolean hasOwner = headMeta.getPersistentDataContainer().has(owner_id, PersistentDataType.STRING);
                    if(hasOwner){
                        String owner =headMeta.getPersistentDataContainer().get(owner_id, PersistentDataType.STRING);

                        if(owner.equals(e.getEntity().getUniqueId().toString())){
                            headHunter.set(e.getEntity().getUniqueId().toString()+".head.safeRespawn", true);
                        }
                    }
                }
            });
            e.getDrops().removeIf((ItemStack item) -> (CheckIfVoodoHead(item)));

           // for(int i = 0; i< e.getDrops().size(); i++){
            //    if(CheckIfVoodoHead(e.getDrops().get(i)))
              //      e.getDrops().remove(i);
         //   }
        }


    }
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e){
        if(headHunter.contains(e.getPlayer().getUniqueId().toString()+".head.safeRespawn"))
            if(Boolean.valueOf(headHunter.get(e.getPlayer().getUniqueId().toString()+".head.safeRespawn").toString())){
                headHunter.set(e.getPlayer().getUniqueId().toString()+".head.safeRespawn", false);
                VoodoPlayerHead item = new VoodoPlayerHead(e.getPlayer(), false);
                e.getPlayer().getInventory().addItem(item.getInternalReference());
                UpdateHeadTracker(item, (OfflinePlayer) e.getPlayer());
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
    public void onPlayerInteract(PlayerInteractEvent e){
        if(CheckIfVoodoHead(e.getPlayer().getItemInUse())) e.setCancelled(true);
    }
    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e){
        if(CheckIfVoodoHead(e.getPlayer().getItemInUse())) e.setCancelled(true);
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
        if(itemStack == null) return;
        if(CheckIfVoodoHead(itemStack)){
            Inventory inventory= e.getClickedInventory();

            try{
                boolean validInventory =  inventory.getType() == InventoryType.FURNACE
                        || inventory.getType() == InventoryType.SMOKER || inventory.getType() == InventoryType.BARREL ||
                        inventory.getType() == InventoryType.CHEST || inventory.getType() == InventoryType.PLAYER ||
                        inventory.getType() == InventoryType.CREATIVE || inventory.getType() == InventoryType.DROPPER ||
                        inventory.getType() == InventoryType.DISPENSER ;
                if(e.getView().getTitle().toLowerCase().contains("ender") || e.getView().getTitle().toLowerCase().contains("shulker"))
                    validInventory = false;
                if(!validInventory){
                    e.setCancelled(true);
                }
            }catch (Exception ignored){
                e.setCancelled(true);
            }

            UpdateHeadTracker(itemStack, (OfflinePlayer) e.getWhoClicked());

        }

    }
    @EventHandler
    public void onCraftItem(CraftItemEvent e){
        if(e.getInventory().contains(Material.PLAYER_HEAD)){
            e.getInventory().all(Material.PLAYER_HEAD).forEach((integer, itemStack) -> {
                if(CheckIfVoodoHead(itemStack)){
                    e.setCancelled(true);
                    return;

                }
            });
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
        boolean validInventory =  inventory.getType() == InventoryType.FURNACE
                || inventory.getType() == InventoryType.SMOKER || inventory.getType() == InventoryType.BARREL ||
                inventory.getType() == InventoryType.CHEST || inventory.getType() == InventoryType.PLAYER ||
                inventory.getType() == InventoryType.CREATIVE || inventory.getType() == InventoryType.DROPPER ||
                inventory.getType() == InventoryType.DISPENSER ;
        if(e.getView().getTitle().toLowerCase().contains("ender") || e.getView().getTitle().toLowerCase().contains("shulker"))
            validInventory = false;

        if( !validInventory){
           if(inventory.contains(Material.PLAYER_HEAD) ){

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
    @EventHandler
    private void PlayerArmorStandManipulateEvent(PlayerArmorStandManipulateEvent e){
        if(CheckIfVoodoHead(e.getPlayerItem())) e.setCancelled(true);
    }
    private boolean CheckIfVoodoHead(Block skull){
        CustomBlockData customBlockData = new CustomBlockData(skull, pluginInstance);
        NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");

        return  (skull.getType() == Material.PLAYER_HEAD && customBlockData.has(owner_id, PersistentDataType.STRING));
    }
    private boolean CheckIfVoodoHead(ItemStack head){
        boolean isHead = null != head && head.getType() == Material.PLAYER_HEAD;
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

        Block emptySpace = player.getLocation().getBlock();
        if(emptySpace.getType() == Material.PLAYER_HEAD) emptySpace
                = getNearestEmptySpace(emptySpace.getLocation(), 5, player.getWorld());
        NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");
        CustomBlockData customBlockData = new CustomBlockData(placeHead(head, emptySpace.getLocation()), pluginInstance);
        String owner_uuid = customBlockData.get(owner_id, PersistentDataType.STRING);
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
                    if (bl.getType() == Material.AIR || !bl.getType().isSolid()){
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
                player.getWorld().getBlockAt(skull.getLocation()).setType(Material.AIR);
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

            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(System.currentTimeMillis());

            String date = c.get(Calendar.DAY_OF_MONTH)+"-"+c.get(Calendar.MONTH)+"-"+c.get(Calendar.YEAR);
            String time = c.get(Calendar.HOUR_OF_DAY)+":"+c.get(Calendar.MINUTE)+":"+c.get(Calendar.SECOND);


            if(affected.hasPlayedBefore()){
                if(affected.isOnline()){
                    Doom(affected.getPlayer());
                }else{ //TRACK THE HEAD
                    headHunter.set(affected.getUniqueId()+".head.impendingDoom", true);
                    try{
                        headHunter.set(affected.getUniqueId()+".head.killer", killer.getName());
                    }catch (Exception ignored){
                        headHunter.set(affected.getUniqueId()+".head.killer", killer.getUniqueId());
                    }



                    headHunter.set(affected.getUniqueId()+".head.time", date + " " + time );

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
        boolean doomable = affected.getGameMode() != GameMode.SPECTATOR;
        doomable = doomable || affected.getGameMode() != GameMode.CREATIVE;
        doomable = doomable || !affected.isOp();
        doomable = doomable || !affected.hasPermission("headhunter.unhuntable");
        if(doomable){



            if (pluginInstance.banKilledPlayer){
                String killer = "n/a";
                try{
                    killer =  ChatColor.RED+""+ChatColor.BOLD
                            +headHunter.get(affected.getUniqueId().toString()+".head.killer").toString()+ChatColor.RESET;
                }catch (Exception ignored){}

                Bukkit.getBanList(BanList.Type.NAME).addBan(affected.getName(), String.format(pluginInstance.getConfig()
                        .get("msg." + pluginInstance.languageId + ".banMessage").toString(),
                        killer,
                        headHunter.get(affected.getUniqueId().toString()+".head.time")
                        ),null, "HEADHUNTER");
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

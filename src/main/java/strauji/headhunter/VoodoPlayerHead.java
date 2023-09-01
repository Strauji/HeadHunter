package strauji.headhunter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Skull;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class VoodoPlayerHead extends ItemStack {
    HeadHunter pluginInstance = (HeadHunter) Bukkit.getPluginManager().getPlugin("HeadHunter");
    private ItemStack internalReference;
    private SkullMeta meta;
    public VoodoPlayerHead(Player player, boolean fake){
        internalReference = new ItemStack(Material.PLAYER_HEAD, 1);

        meta = (SkullMeta) internalReference.getItemMeta();
        if(meta != null){
            if(!fake){
                meta.setDisplayName(String.format(pluginInstance.getConfig().get("msg."+pluginInstance.languageId+".headName").toString(), player.getName()));
                NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");
                meta.getPersistentDataContainer().set(owner_id, PersistentDataType.STRING, player.getUniqueId().toString());
                NamespacedKey last_wielder  = new NamespacedKey(pluginInstance, "last_wielder");
                meta.getPersistentDataContainer().set(last_wielder, PersistentDataType.STRING, player.getUniqueId().toString());
                List<String> rand = new ArrayList<>();
                rand.add(String.valueOf(Math.random()));
                meta.setLore(rand);
                internalReference.addUnsafeEnchantment(Enchantment.LOYALTY, 1);
            }

            meta.setOwningPlayer((OfflinePlayer) player);
            internalReference.setItemMeta(meta);

        }

    }
    public  VoodoPlayerHead(OfflinePlayer player, boolean fake){
        internalReference = new ItemStack(Material.PLAYER_HEAD, 1);

        meta = (SkullMeta) internalReference.getItemMeta();
        if(meta != null){
            if(!fake){
                meta.setDisplayName(String.format(pluginInstance.getConfig().get("msg."+pluginInstance.languageId+".headName").toString(), player.getName()));
                NamespacedKey owner_id  = new NamespacedKey(pluginInstance, "owner_id");
                meta.getPersistentDataContainer().set(owner_id, PersistentDataType.STRING, player.getUniqueId().toString());
                NamespacedKey last_wielder  = new NamespacedKey(pluginInstance, "last_wielder");
                meta.getPersistentDataContainer().set(last_wielder, PersistentDataType.STRING, player.getUniqueId().toString());
                List<String> rand = new ArrayList<>();
                rand.add(String.valueOf(Math.random()));
                meta.setLore(rand);
                internalReference.addUnsafeEnchantment(Enchantment.LOYALTY, 1);
            }

            meta.setOwningPlayer( player);
            internalReference.setItemMeta(meta);

        }


    }
    public ItemStack getInternalReference(){
        return  internalReference;
    }

}

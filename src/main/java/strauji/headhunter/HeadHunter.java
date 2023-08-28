package strauji.headhunter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ShapedRecipe;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class HeadHunter extends JavaPlugin {
    private File customConfigFile;
    private FileConfiguration customConfig;
    public boolean ObeyPVP = false;
    public int minDoomLevel = 1;
    public int maxDoomLevel = 32;
    public boolean increaseDoomLevel = true;
    public boolean allowHeadCrafting = true;
    public boolean allowRessurecting = true;
    public boolean banKilledPlayer = false;
    public String kickMessage = "WASTED!";

    @Override
    public void onEnable() {
        getLogger().info("Beest sure thy headeth is eft!");
        //Events bellow
        getServer().getPluginManager().registerEvents(new CustomEventHandler(), this);
        //Commands bellow
        this.getCommand("checkheads").setExecutor(new CheckHeads());
        HeadHunterConfig();
        RegisterPlayerHeadRecipe();
    }
    @Override
    public void onDisable(){
            SaveToDisk();

    }
    public FileConfiguration getHeadHunter() {
        return this.customConfig;
    }
    private void RegisterPlayerHeadRecipe() {
        // Our custom variable which we will be changing around.
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        NamespacedKey key = new NamespacedKey(this, "skull");

        ShapedRecipe recipe = new ShapedRecipe(key, item);

        recipe.shape("DZD", "ZTZ", "DZD");
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('Z', Material.ROTTEN_FLESH);
        recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);

        Bukkit.addRecipe(recipe);
    }
    private void HeadHunterConfig() {
        customConfigFile = new File(getDataFolder(), "HeadHunter.yml");
        if (!customConfigFile.exists()) {
            customConfigFile.getParentFile().mkdirs();
            saveResource("HeadHunter.yml", false);

        }

        customConfig = new YamlConfiguration();
        try {
            customConfig.load(customConfigFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();

        }
    }
    public void SaveToDisk() {
        try {
            customConfig.save(customConfigFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    }

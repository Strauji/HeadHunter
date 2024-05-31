package strauji.headhunter;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ShapedRecipe;
import java.io.File;

import java.io.IOException;
import static java.util.Map.entry;
import java.util.Map;
import java.util.UUID;

public class HeadHunter extends JavaPlugin {

    private File customConfigFile;
    private FileConfiguration customConfig;
    public boolean ObeyPVP = true;
    public int minDoomLevel = 1;
    public int maxDoomLevel = 32;
    public boolean increaseDoomLevel = true;
    public int incrementPerDoom = 1;
    public  boolean warnPVP = true;
    public boolean allowHeadCrafting = true;
    public boolean allowRessurecting = true;
    public boolean allowHeadCompass = true;
    public int compassBufferRadius = 250;
    public int compassCost = 15;
    public  int compassBufferRadiusSquared = 250*250;
    public  boolean allowDecoy = true;
    public boolean banKilledPlayer = false;
    public boolean unbanOffPVP = true;
    public boolean resurrectOffPVP = false;

    public String kickMessage = "WASTED!";
    public String languageId = "en_us";
    private Map<String, Object> defaultValues  = Map.ofEntries(
            entry("language.selected", languageId),
            entry("msg.en_us.404player","The player %s haven't played on this server yet!" ),
            entry("msg.pt_br.404player","O jogador %s nunca jogou nesse servidor!" ),
            entry("msg.en_us.headClaimed","The head of %s have been claimed!" ),
            entry("msg.pt_br.headClaimed","%s teve sua cabeça destruída!" ),
            entry("msg.en_us.headName", "The head of %s"),
            entry("msg.pt_br.headName", "A cabeça de %s"),
            entry("msg.pt_br.firstLogin", "Bem vindo ao servidor, %s! Como presentinho de boas vindas você recebeu a sua cabeça!\nProteja ela como se sua vida dependesse disso...\nPois ela depende. "),
            entry("msg.en_us.firstLogin", "Welcome to the server, %s! As a newcomer gift, you've received your own head!\nProtect it as if your life depends on it...\nBecause it does. "),
            entry("msg.en_us.banMessage", "You've been killed by %s at %s! You'll be allowed back when the PVP ends or if someone resurrects you!"),
            entry("msg.pt_br.banMessage", "Você foi morto por %s na data %s! Você poderá se juntar novamente ao servidor quando o PVP acabar ou se alguém te ressucitar!"),
            entry("msg.pt_br.headDestroyed", "Essa cabeça não pôde ser encontrada!"),
            entry("msg.en_us.headDestroyed", "That head is nowhere to be found!"),
            entry("msg.en_us.headCarried", "That head is being safely carried by someone"),
            entry("msg.pt_br.headCarried", "Essa cabeça está sendo seguramente carregada por alguém"),
            entry("msg.pt_br.headPlaced", "Essa cabeça está em algum lugar por aí"),
            entry("msg.en_us.headPlaced", "This head is somewhere out there"),
            entry("msg.en_us.headDropped", "This head is floating somewhere out there"),
            entry("msg.pt_br.headDropped", "Essa cabeça está flutuando em algum lugar por aí"),
            entry("msg.pt_br.headMissing", "Irineu, vc não sabe, nem eu."),
            entry("msg.en_us.headMissing", "Sorry fam, dunno"),
            entry("msg.en_us.pvpEnabled", "The PVP mode have been enabled!"),
            entry("msg.en_us.pvpDisabled", "The PVP mode have been disabled!"),
            entry("msg.pt_br.pvpEnabled", "O modo PVP foi ativado!"),
            entry("msg.pt_br.pvpDisabled", "O modo PVP foi desativado!"),
            entry("msg.pt_br.resetPlayerSuccess", "O jogador %s foi redefinido com sucesso!"),
            entry("msg.pt_br.resetPlayerFailure", "O jogador %s não pôde ser redefinido, verifique se ele já jogou nesse servidor antes"),
            entry("msg.en_us.resetPlayerSuccess", "The player %s have been successfully reset!"),
            entry("msg.en_us.resetPlayerFailure", "The player %s couldn't be reset! Check if they have logged in this server atleast once before"),
            entry("msg.en_us.pvpStateNotChanged", "The PVP mode was already set to %s"),
            entry("msg.pt_br.pvpStateNotChanged", "O modo PVP já estava definido como %s"),
            entry("msg.en_us.compassName", "Tracking device for %s"),
            entry("msg.pt_br.compassName", "Rastreador de %s"),
            entry("msg.pt_br.graceLeft", "Imunidade acaba em %sh %sm %ss"),
            entry("msg.en_us.graceLeft", "Grace period ends in %sh %sm %ss"),

            entry("obeyPVP",true),
            entry("minDoomLevel",1),
            entry("maxDoomLevel",32),
            entry("increaseDoomLevel",true),
            entry("incrementPerDoom",1),
            entry("warnPVP",true),
            entry("allowHeadCrafting",true),
            entry("allowResurrecting",true),
            entry("allowHeadCompass",true),
            entry("allowDecoy",true),
            entry("banDoomedPlayer",true),
            entry("unbanOffPVP",true),
            entry("resurrectOffPVP", false),
            entry("kickMessage",kickMessage),
            entry("compassBufferRadius",250),
            entry("compassCost",15),
            entry("playerGraceSec", 360)


    );
    @Override
    public void onEnable() {
        getLogger().info("Beest sure thy headeth is eft!");
        //Events bellow
        getServer().getPluginManager().registerEvents(new CustomEventHandler(), this);
        //Commands bellow
        this.getCommand("checkhead").setExecutor(new CheckHeads());
        this.getCommand("togglepvp").setExecutor(new TogglePVP());
        this.getCommand("resetplayer").setExecutor(new ResetPlayer());
        PluginConfig();
        HeadHunterConfig();
        if(allowHeadCrafting) RegisterPlayerHeadRecipe();
        int pluginId = 19730; // <-- Replace with the id of your plugin!
        Metrics metrics = new Metrics(this, pluginId);

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

    private void PluginConfig(){

        if(!this.getConfig().contains("language.selected")){
            defaultValues.forEach((s, o) -> getConfig().set(s, o));
            saveConfig();
        }
        languageId = safeGetConfig("language.selected").toString();
        ObeyPVP =Boolean.valueOf(safeGetConfig("obeyPVP").toString());
        minDoomLevel = Integer.valueOf(safeGetConfig("minDoomLevel").toString());
        maxDoomLevel = Integer.valueOf(safeGetConfig("maxDoomLevel").toString());
        increaseDoomLevel =Boolean.valueOf(safeGetConfig("increaseDoomLevel").toString());
        incrementPerDoom =Integer.valueOf(safeGetConfig("incrementPerDoom").toString());
        warnPVP = Boolean.valueOf(safeGetConfig("warnPVP").toString());
        allowHeadCrafting = Boolean.valueOf(safeGetConfig("allowHeadCrafting").toString());
        allowRessurecting = Boolean.valueOf(safeGetConfig("allowResurrecting").toString());
        allowHeadCompass =Boolean.valueOf(safeGetConfig("allowHeadCompass").toString());
        allowDecoy = Boolean.valueOf(safeGetConfig("allowDecoy").toString());
        banKilledPlayer =  Boolean.valueOf(safeGetConfig("banDoomedPlayer").toString());
        unbanOffPVP =  Boolean.valueOf(safeGetConfig("unbanOffPVP").toString());
        resurrectOffPVP =  Boolean.valueOf(safeGetConfig("resurrectOffPVP").toString());
        kickMessage = safeGetConfig("kickMessage").toString();
        compassBufferRadius = Integer.valueOf(safeGetConfig("compassBufferRadius").toString());
        compassCost = Integer.valueOf(safeGetConfig("compassCost").toString());
        compassBufferRadiusSquared = compassBufferRadius*compassBufferRadius;
    }
    public Object safeGetConfig(String path){

        if(!getConfig().contains(path) ){
            if(defaultValues.containsKey(path)){
                getConfig().set(path, defaultValues.get(path));
            }else{
                throw new RuntimeException("Somehow you tried to read a configuration line that doesn't exist. Are you sure this plugin wasn't tampered?");

            }

        }
        return getConfig().get(path);
    }
    public void SaveToDisk() {
        try {
            customConfig.save(customConfigFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    public boolean resetPlayer(UUID uuid){
        if(Bukkit.getOfflinePlayer(uuid).isOnline() || Bukkit.getOfflinePlayer(uuid).hasPlayedBefore()){
            String owner = uuid.toString();
            getHeadHunter().set(owner+".head.destroyed", false);
            getHeadHunter().set(owner+".head.inInventory", false);
            getHeadHunter().set(owner+".head.position", null);
            getHeadHunter().set(owner+".head.impendingDoom",false);
            getHeadHunter().set(owner+".head.last_wielder", uuid.toString());
            getHeadHunter().set(owner+".head.dropped", false);
            getHeadHunter().set(owner+".head.placed", false);
            getHeadHunter().set(owner+".head.doomLevel", 1);
            getHeadHunter().set(owner+".receivedHead", false);
            getHeadHunter().set(uuid+".head.doomed", false);
            SaveToDisk();
            return  true;
        }
        return  false;
    }
}

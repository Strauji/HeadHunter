package strauji.headhunter;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
public class TogglePVP implements CommandExecutor {
    private final HeadHunter pluginInstance = (HeadHunter) Bukkit.getPluginManager().getPlugin("HeadHunter");
    FileConfiguration headHunter = null;
    @Override
    public boolean onCommand( CommandSender commandSender,  Command command,  String s, String[] strings) {
        if (headHunter == null) headHunter = pluginInstance.getHeadHunter();
        if (strings.length > 0) {
            if (commandSender instanceof Player) {
                boolean value = (Boolean.valueOf(strings[0]));
                boolean oldValue = ((Player) commandSender).getWorld().getPVP();
                if(value == oldValue){
                    commandSender.sendMessage(String.format( pluginInstance.getConfig().get("msg."+pluginInstance.languageId+".pvpStateNotChanged").toString(), ""+value));
                    return true;
                }
                ((Player) commandSender).getWorld().setPVP(value);
                if(pluginInstance.warnPVP ){
                    String off = pluginInstance.getConfig().get("msg."+pluginInstance.languageId+".pvpDisabled").toString();
                    String on = pluginInstance.getConfig().get("msg."+pluginInstance.languageId+".pvpEnabled").toString();
                    String message = value? on: off;
                    Bukkit.broadcastMessage(ChatColor.GRAY+ message);
                }
                if(!value && pluginInstance.unbanOffPVP){
                    for (Object o : Bukkit.getBannedPlayers().toArray()) {
                        if(o instanceof  OfflinePlayer){
                            BanList banList = Bukkit.getBanList(BanList.Type.NAME);
                            String source = banList.getBanEntry(((OfflinePlayer) o).getName()).getSource();
                            System.out.println(source.equals("HEADHUNTER") + " "+ source);
                            if (source.equals("HEADHUNTER")){
                                banList.pardon(((OfflinePlayer) o).getName());
                            }
                        }
                    }
                }
                return true;
            }
        }
        return  false;
    }
}
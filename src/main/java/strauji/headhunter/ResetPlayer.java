package strauji.headhunter;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.UUID;
public class ResetPlayer implements CommandExecutor {
    private final HeadHunter pluginInstance = (HeadHunter) Bukkit.getPluginManager().getPlugin("HeadHunter");
    FileConfiguration headHunter = null;
    @Override
    public boolean onCommand( CommandSender commandSender,  Command command,  String s, String[] strings) {
        if (strings.length > 0) {
            if (headHunter == null) headHunter = pluginInstance.getHeadHunter();
            if( headHunter.contains(strings[0]+".uuid")) {
                UUID uuid = UUID.fromString(headHunter.get(strings[0] + ".uuid").toString());
                if(pluginInstance.resetPlayer(uuid)){
                    commandSender.sendMessage(String.format( pluginInstance.getConfig().get("msg."+pluginInstance.languageId+".resetPlayerSuccess").toString(), strings[0]));
                    return  true;
                    }
            }
            commandSender.sendMessage(String.format( pluginInstance.getConfig().get("msg."+pluginInstance.languageId+".resetPlayerFailure").toString(), strings[0]));
            return false;
        }
        return false;
    }
}
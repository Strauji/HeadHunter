package strauji.headhunter;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import org.bukkit.entity.Player;


public class CheckHeads implements CommandExecutor {
    private HeadHunter pluginInstance = (HeadHunter) Bukkit.getPluginManager().getPlugin("HeadHunter");
    FileConfiguration headHunter = null;
    @Override

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (headHunter == null) headHunter = pluginInstance.getHeadHunter();

        if(args.length > 0){
            if(sender instanceof Player){
                Player player = (Player) sender;

                String name = args[0];
                if( headHunter.contains(name+".uuid")){
                    String uuid_head = headHunter.get(name+".uuid").toString();
                    OfflinePlayer player1 = Bukkit.getOfflinePlayer(uuid_head);
                    String message = "";
                    Location loc = (Location)headHunter.get(uuid_head+".position");
                    /////

                    if((boolean) headHunter.get(uuid_head+".head.destroyed"))message = String.format(pluginInstance.getConfig().get("msg."+pluginInstance.languageId+".headDestroyed").toString(), player1.getName());
                    else if((boolean)headHunter.get(uuid_head+".head.inInventory")) message = String.format(pluginInstance.getConfig().get("msg."+pluginInstance.languageId+".headCarried").toString(), player1.getName());

                    else if((boolean) headHunter.get(uuid_head+".head.placed"))message = String.format(pluginInstance.getConfig().get("msg."+pluginInstance.languageId+".headPlaced").toString(), player1.getName());
                    else if((boolean) headHunter.get(uuid_head+".head.dropped"))message = String.format(pluginInstance.getConfig().get("msg."+pluginInstance.languageId+".headDropped").toString(), player1.getName());
                    else if(headHunter.contains(uuid_head+".head.last_wielder"))message = String.format(pluginInstance.getConfig().get("msg."+pluginInstance.languageId+".headCarried").toString(), player1.getName());

                    else message = pluginInstance.getConfig().get("msg."+pluginInstance.languageId+".headMissing").toString();
                    player.sendMessage(message);
                    /////
                }else{
                    player.sendMessage(String.format(pluginInstance.getConfig().get("msg."+pluginInstance.languageId+".404player").toString(), Bukkit.getName()));
                }

                }



                }
        return true;
    }

}

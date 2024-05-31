![logo](https://i.imgur.com/BwiI2Ds.png)
# Enhance Your Server with HeadHunter 
## A Unique PvP Game Mode
 
 Are you a Minecraft server owner looking to elevate your players' experience with something truly exceptional? Look no further than HeadHunter, a one-of-a-kind PvP game mode designed to add excitement and depth to your server.
 
### How does HeadHunter works?
 - Upon a player first joins the server, they will receive a very special item, their own head, and the whole HeadHunter gameplay revolves around that item, so the player must protect it with all their might, because their life depends on it
 - The Head may be placed as a block, which makes it nigh-invulnerable, only being able to be picked up by players
	 * -&gt;The Head's block doesn't need a specific tool to be mined, nor does it need Silk Touch
	 * -&gt;The Head's block cannot be destroyed by explosion, nor pushed/pulled by pistons
	 * -&gt;Water and Lava treat it as a normal non-flammable block
* The Head may also be held as an item, but be aware: That's is the most vulnerable form 
	* -&gt;The Head cannot be removed from the game to keep it's owner safe
		* -&gt;&gt;Logging off with it in inventory will cause it to be placed as a block close to where the player left
		* -&gt;&gt;Enderchests and Shulkers won't accept it, spitting it as an item drop
		* -&gt;&gt;Dispensers, Hoppers and any other thing that moves inventory won't work with it inside	
	* -&gt;The head can be stored in normal chests and others inventories that usually would be accessible by others
	* -&gt;Lava, fire, explosions, cacti contact and others form of item damage will instantly destroy the Head and doom it's owner
	* -&gt;The Head may be dropped as a normal item, but if it despawn it's owner will also be doomed
*  A doomed player may be resurrected if someone craft and enchant it's head, as long as the plugin is set to **allowResurrecting**(default: true) or alternatively if the server's manager disable the PVP and the plugin is set to **resurrectOffPVP**(default: false), but be aware, the former will re-doom the player when the PVP is set on again
       * -&gt;The first part of the resurrection is the default player head crafting, this plugin have a recipe for that if set to **allowHeadCrafting**(default: true), but any other way of obtaining it will work, even other plugin/mod
	![Crafting recipe](https://i.imgur.com/GVGbQUG.png)
      * -&gt;The second part is enchanting, the head must be taken to an anvil, and there the benefactor must set the head's name as *EXACTLY* the same as the player to be resurrected, matching case and etc.
		* -&gt;&gt;The enchanting cost is set by the doomed player's **doomLevel**(default: 1), the plugin may be configured to increment it by enabling **increaseDoomLevel**(default: true), which will increase every time that player face their fate, the increment amount is set in **incrementPerDoom**(default: 1), it also may have a minimun, set by **minDoomLevel**(default: 1), and a maximum amount, set by **maxDoomLevel**(default: 32)
![Enchanting a head](https://i.imgur.com/8Kav4UJ.png)
			* Note: Trying to craft the head of a player that is not doomed may have two results: If the plugin is set to **allowDecoy**(default: true), a decoy head will be created, otherwise, the original head will just be renamed, with the cost being the default one
![A non enchanted decoy head](https://i.imgur.com/gUlLBqR.png)
- To make things more interesting, the plugin may also be set to **allowHeadCompass**(default: true), which allow any player to craft a special compass that will track the target's head no matter where it's stored if used correctly, again, be aware, this isn't a fool proof item that will boost the user to an easy hunting, it will stop working if inside the sphere with the radius set by **compassBufferRadius**(default: 250), a buffer zone to make things fair
       * -&gt;The enchanting works like the head's one, take it to an anvil and rename it to the Target's *Exactly* name
	![Enchanting a compass](https://i.imgur.com/fSldMyH.png)
* When doomed, the player will either be killed, kicked and banned if **banDoomedPlayer**(default: true) or killed and have the gamemode to spectator. They will stay this way until someone revives them or until the PVP is toggled off and **unBanOffPVP**(default: true) is set to true
       * -&gt;The kick message may be customized changing the **kickMessage**(default: "Wasted!")
### Commands 
- /checkhead &lt; target name &gt; - Will return a vaguely tell the state of the target's head,  is set to be usable by anyone by default
- /togglepvp &lt; true| false&gt; - A command that will enable or disable the PVP on the world the sender is, if **warnPVP**(default: true) is enabled it will broadcast the state change, is set to only be usable by OPs by default
- /resetplayer &lt; player name &gt; - Will give a fresh start to the player, making the plugin think they never played in the server before. *Caution* using it if the player head is still existing
### Permissions
-	*headhunter.** - Access to the whole three commands listed above
-	*headhunter.checkheads* - Allow the use of /checkhead command
-	*headhunter.togglepvp* - Allow the use of /togglepvp command
-	*headhunter.resetplayer* - Allow the use of /resetplayer command
-	*headhunter.unhuntable* - Allow you bypass the ban/spectator mechanics

**Changes:**

[02/14/2024]:
```
- Having a Player head in your inventory when you leave will no longer deletes your armor(Sorry guys, i wasn't thinking properly when i first wrote that snippet)

- Leaving with a Player head in your inventory will cause the plugin to try to put it at your feet's coordinates at first, if the space is occupied, it will then scan the are around it to find an empty space, trying to put the head in the first non solid block

- You can no longer put the non fake player head in armor stands

- You may set a grace period in configurations, the playerGraceSec entry should be set to a value, in seconds, higher than 0 to enable it(For reference, 360 = 6 minutes). It will delay the delivery of the player's head, allowing the player to gear itself and so before risking their head

```

  
**Support the Developer:**  
If you enjoy Headhunters and would like to support its continued development and updates, you can [Buy me a coffee](https://ko-fi.com/Y8Y3OVDPD "Buy me a coffee") on Ko-fi. Your feedback and support goes a long way in helping me bring more exciting features and improvements to the plugin.

### Disclaimer
This plugin supports both US English(en_us) and Brazilian Portuguese(pt_br), you can change that in **language.selected**(default:en_us)

Every bold text followed by (default:..) is a configuration option that is found in the plugins/HeadHunter/config.yml file.

This plugin uses [Bstats](https://bstats.org) to gather anonymous data about how many servers currently have this plugin enabled. No identifying data is gathered. 

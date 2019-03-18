package club.imaginears.core.events;

import club.imaginears.core.utils.AnvilGUI;
import club.imaginears.core.utils.Chat;
import club.imaginears.core.utils.Console;
import club.imaginears.core.utils.GUIs;
import com.google.common.collect.Lists;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class Build implements Listener {

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (!club.imaginears.core.commands.Build.buildMode.contains(e.getPlayer().getName())) {
            e.setCancelled(true);
        }
    }


    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (!club.imaginears.core.commands.Build.buildMode.contains(e.getPlayer().getName())) {
            e.setCancelled(true);
            return;
        }

    }

}

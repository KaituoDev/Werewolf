package fun.kaituo.werewolf;

import fun.kaituo.gameutils.GameUtils;
import fun.kaituo.gameutils.event.PlayerChangeGameEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;


public class Werewolf extends JavaPlugin implements Listener {
    private GameUtils gameUtils;
    static List<Player> players;
    static int potionSpawnPeriod;
    static int werewolfNumber;
    static int voteTime;

    public static WerewolfGame getGameInstance() {
        return WerewolfGame.getInstance();
    }

    @EventHandler
    public void onButtonClicked(PlayerInteractEvent pie) {
        if (!pie.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        if (!pie.getClickedBlock().getType().equals(Material.OAK_BUTTON)) {
            return;
        }
        if (pie.getClickedBlock().getLocation().equals(new Location(gameUtils.getWorld(), -1000, 42, -1008))) {
            WerewolfGame.getInstance().startGame();
        }
    }

    @EventHandler
    public void changeGameOptions(PlayerInteractEvent pie) {
        if (pie.getClickedBlock() == null) {
            return;
        }
        Location location = pie.getClickedBlock().getLocation();
        long x = location.getBlockX();
        long y = location.getBlockY();
        long z = location.getBlockZ();
        if (x == -1001 && y == 41 && z == -1008) {
            switch (potionSpawnPeriod) {
                case 15:
                case 30:
                case 45:
                case 60:
                case 75:
                case 90:
                case 105:
                    potionSpawnPeriod += 15;
                    break;
                case 120:
                    potionSpawnPeriod = 15;
                    break;
                default:
                    break;
            }
            Sign sign = (Sign) pie.getClickedBlock().getState();
            sign.setLine(2, "药水生成间隔为 " + potionSpawnPeriod + " 秒");
            sign.update();
        } else if (x == -1000 && y == 41 && z == -1008) {
            switch (werewolfNumber) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    werewolfNumber += 1;
                    break;
                case 8:
                    werewolfNumber = 1;
                    break;
                default:
                    break;
            }
            Sign sign = (Sign) pie.getClickedBlock().getState();
            sign.setLine(2, "狼人数量为 " + werewolfNumber);
            sign.update();
        } else if (x == -999 && y == 41 && z == -1008) {
            switch (voteTime) {
                case 10:
                case 20:
                case 30:
                case 40:
                case 50:
                case 60:
                case 70:
                case 80:
                    voteTime += 10;
                    break;
                case 90:
                    voteTime = 10;
                    break;
                default:
                    break;
            }
            Sign sign = (Sign) pie.getClickedBlock().getState();
            sign.setLine(2, "投票时间为 " + voteTime + " 秒");
            sign.update();
        }
    }

    public void onEnable() {
        gameUtils = (GameUtils) Bukkit.getPluginManager().getPlugin("GameUtils");
        players = new ArrayList<>();
        Bukkit.getPluginManager().registerEvents(this, this);
        potionSpawnPeriod = 60;
        werewolfNumber = 2;
        voteTime = 20;
        Sign sign = (Sign) gameUtils.getWorld().getBlockAt(-1001, 41, -1008).getState();
        sign.setLine(2, "药水生成间隔为 " + potionSpawnPeriod + " 秒");
        sign.update();
        Sign sign2 = (Sign) gameUtils.getWorld().getBlockAt(-1000, 41, -1008).getState();
        sign2.setLine(2, "狼人数量为 " + werewolfNumber);
        sign2.update();
        Sign sign3 = (Sign) gameUtils.getWorld().getBlockAt(-999, 41, -1008).getState();
        sign3.setLine(2, "投票时间为 " + voteTime + " 秒");
        sign3.update();
        gameUtils.registerGame(getGameInstance());
    }

    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll((Plugin) this);
        for (Player p: Bukkit.getOnlinePlayers()) {
            if (gameUtils.getPlayerGame(p) == getGameInstance()) {
                Bukkit.dispatchCommand(p, "join Lobby");
            }
        }
        gameUtils.unregisterGame(getGameInstance());
    }

}
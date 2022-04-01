package fun.kaituo;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import fun.kaituo.event.PlayerChangeGameEvent;
import fun.kaituo.event.PlayerEndGameEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;

import java.io.IOException;
import java.util.*;

import static fun.kaituo.GameUtils.*;

public class WerewolfGame extends Game implements Listener {
    private static final WerewolfGame instance = new WerewolfGame((Werewolf) Bukkit.getPluginManager().getPlugin("Werewolf"));
    int potionSpawnPeriod, werewolfNumber, voteTime;
    int countDownSeconds = 5;
    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    Scoreboard werewolf = Bukkit.getScoreboardManager().getNewScoreboard();
    List<Player> humans = new ArrayList<>();
    List<Player> werewolves = new ArrayList<>();
    List<Player> werewolvesCopy = new ArrayList<>();
    List<Player> playersAlive = new ArrayList<>();
    List<Player> playersWhoUsedButton = new ArrayList<>();
    List<Player> playersWhoUsedButton2 = new ArrayList<>();
    HashMap<Player, Integer> voteMap = new HashMap<>();
    HashMap<Player, Player> initialVote = new HashMap<>();
    BukkitTask voteTask;
    Team team;
    boolean running = false;
    ItemStack dragon_breath = generateItemStack(Material.DRAGON_BREATH, "§b血清", 1);
    ItemStack sword = generateItemStack(Material.IRON_AXE, "管制刀具(误)", 1);
    ItemStack barrier = generateItemStack(Material.BARRIER, "弃权", 1);
    Location[]
            locations = new Location[]{
            new Location(world, -994, 64, -1007),
            new Location(world, -1019, 96, -1004),
            new Location(world, -994, 80, -1007),
            new Location(world, -1007, 80, -1007),
            new Location(world, -1023, 80, -983),
            new Location(world, -1023, 80, -989),
            new Location(world, -1023, 80, -1001),
            new Location(world, -1023, 80, -1007),
            new Location(world, -978, 80, -1007),
            new Location(world, -978, 80, -1001),
            new Location(world, -978, 80, -995),
            new Location(world, -978, 80, -989),
            new Location(world, -978, 80, -983),
            new Location(world, -1007, 72, -1007),
            new Location(world, -994, 72, -1007),
            new Location(world, -982, 96, -1004),
            new Location(world, -1023, 72, -983),
            new Location(world, -1023, 72, -989),
            new Location(world, -1023, 72, -995),
            new Location(world, -1023, 72, -1001),
            new Location(world, -1023, 72, -1007),
            new Location(world, -978, 72, -983),
            new Location(world, -978, 72, -989),
            new Location(world, -978, 72, -995),
            new Location(world, -978, 72, -1001),
            new Location(world, -978, 72, -1007),
            new Location(world, -1007, 64, -1007),
            new Location(world, -994, 64, -1007),
            new Location(world, -1023, 64, -983),
            new Location(world, -1023, 64, -989),
            new Location(world, -1023, 64, -995),
            new Location(world, -1023, 64, -1001),
            new Location(world, -1023, 64, -1007),
            new Location(world, -978, 64, -983),
            new Location(world, -978, 64, -989),
            new Location(world, -978, 64, -995),
            new Location(world, -978, 64, -1001),
            new Location(world, -978, 64, -1007)
    };
    int dragonBreathNumber = 0;
    int dragonBreathNeeded = 0;
    PacketAdapter pa;
    ProtocolManager pm = ProtocolLibrary.getProtocolManager();


    private WerewolfGame(Werewolf plugin) {
        this.plugin = plugin;
        initializeGame(plugin, "Werewolf", "§c狼人杀§f-§7精神病栋", new Location(world, -1000, 40, -1000, 180, 0), new BoundingBox(-1071, 61, -1065, -931, 100, -925));
        initializeButtons(new Location(world, -1000, 42, -1008), BlockFace.SOUTH,
                new Location(world, -994, 41, -1000), BlockFace.WEST);
        players = Werewolf.players;
        werewolf.registerNewObjective("werewolf", "dummy", "狼人杀-精神病栋");
        werewolf.getObjective("werewolf").setDisplaySlot(DisplaySlot.SIDEBAR);
        pa = new PacketAdapter(plugin, PacketType.Play.Server.PLAYER_INFO) {
            @Override
            public void onPacketSending(PacketEvent e) {
                if (!getPlayers().contains(e.getPlayer())) {
                    return;
                }
                if (e.getPlayer().getGameMode().equals(GameMode.SPECTATOR)) {
                    return;
                }

                if (!e.getPacket().getPlayerInfoAction().read(0).equals(EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE)) {
                    return;
                }
                e.setCancelled(true);
            }
        };
    }

    public static WerewolfGame getInstance() {
        return instance;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent ede) {
        if (ede.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
            ede.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent pdie) {
        if (players.contains(pdie.getPlayer())) {
            pdie.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent pie) {

        if (pie.getClickedBlock() == null) {
            return;
        }
        Location location = pie.getClickedBlock().getLocation();
        long x = location.getBlockX();
        long y = location.getBlockY();
        long z = location.getBlockZ();
        if ((x == -993 && y == 65 && z == -995) || (x == -1008 && y == 65 && z == -995)) {
            if (playersWhoUsedButton.contains(pie.getPlayer())) {
                return;
            }
            playersWhoUsedButton.add(pie.getPlayer());
            voteMap.clear();
            initialVote.clear();
            Inventory inventory = ((Chest) world.getBlockAt(-1000, 33, -1003).getState()).getBlockInventory();
            inventory.clear();
            for (int i = 0; i < playersAlive.size(); i++) {
                ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) headItem.getItemMeta();
                skullMeta.setOwningPlayer(playersAlive.get(i));
                skullMeta.setDisplayName(playersAlive.get(i).getName());
                headItem.setItemMeta(skullMeta);
                inventory.setItem(i, headItem);
            }
            inventory.setItem(26, barrier);
            for (Player p : players) {
                p.teleport(new Location(world, -1000, 33, -1000, 180, 0));
                p.sendMessage("§a" + pie.getPlayer().getName() + " 召集了紧急集合！");
                p.sendMessage("§a你有" + voteTime + " 秒投票时间，不投则为弃权！");
            }
            voteTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                HashMap<Player, Integer> voteMapCopy = new HashMap<>(voteMap);
                Player playerToBeKilled = null;
                int votes = 0;
                for (Map.Entry<Player, Integer> entry : voteMapCopy.entrySet()) {
                    if (entry.getValue() > votes) {
                        playerToBeKilled = entry.getKey();
                        votes = entry.getValue();
                    }
                }
                if (playerToBeKilled == null) {
                    for (Player p : players) {
                        p.sendMessage("§e没有人被放逐，游戏继续！");
                        p.teleport(new Location(world, -1000.0, 64, -981.0));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 4, false, false));
                    }
                    return;
                }
                for (Map.Entry<Player, Integer> entry : voteMapCopy.entrySet()) {
                    if (entry.getKey().equals(playerToBeKilled)) {
                        continue;
                    }
                    if (entry.getValue() == votes) {
                        for (Player p : players) {
                            p.sendMessage("§e没有人被放逐，游戏继续！");
                            p.teleport(new Location(world, -1000.0, 64, -981.0));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 4, false, false));
                        }
                        return;
                    }
                }
                playerToBeKilled.setHealth(0);
                for (Player p : players) {
                    p.sendMessage("§c" + playerToBeKilled.getName() + "被放逐了！");
                    p.teleport(new Location(world, -1000.0, 64, -981.0));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 4, false, false));
                }

            }, 20 * voteTime);

        } else if (x == -1000 && y == 34 && z == -997) {
            if (!world.getBlockAt(-995, 41, -1007).isBlockPowered()) {
                if (playersWhoUsedButton2.contains(pie.getPlayer())) {
                    return;
                }
            }
            playersWhoUsedButton2.add(pie.getPlayer());
            for (Player p : players) {
                p.sendMessage("§a" + pie.getPlayer().getName() + "提前结束了投票！");
            }
            voteTask.cancel();
            HashMap<Player, Integer> voteMapCopy = new HashMap<>(voteMap);
            Player playerToBeKilled = null;
            int votes = 0;
            for (Map.Entry<Player, Integer> entry : voteMapCopy.entrySet()) {
                if (entry.getValue() > votes) {
                    playerToBeKilled = entry.getKey();
                    votes = entry.getValue();
                }
            }
            if (playerToBeKilled == null) {
                for (Player p : players) {
                    p.sendMessage("§e没有人被放逐，游戏继续！");
                    p.teleport(new Location(world, -1000.0, 64, -981.0));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 4, false, false));
                }
                return;
            }
            for (Map.Entry<Player, Integer> entry : voteMapCopy.entrySet()) {
                if (entry.getKey().equals(playerToBeKilled)) {
                    continue;
                }
                if (entry.getValue() == votes) {
                    for (Player p : players) {
                        p.sendMessage("§e没有人被放逐，游戏继续！");
                        p.teleport(new Location(world, -1000.0, 64, -981.0));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 4, false, false));
                    }
                    return;
                }
            }
            playerToBeKilled.setHealth(0);
            for (Player p : players) {
                p.sendMessage("§c" + playerToBeKilled.getName() + "被放逐了！");
                p.teleport(new Location(world, -1000.0, 64, -981.0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 4, false, false));
            }
        }
    }

    @EventHandler
    public void onClickingCorpse(PlayerArmorStandManipulateEvent pasme) {
        if (!players.contains(pasme.getPlayer())) {
            return;
        }
        if (pasme.getRightClicked().getHelmet().getType().equals(Material.PLAYER_HEAD)) {
            pasme.getRightClicked().remove();
            voteMap.clear();
            initialVote.clear();
            Inventory inventory = ((Chest) world.getBlockAt(-1000, 33, -1003).getState()).getBlockInventory();
            inventory.clear();
            for (int i = 0; i < playersAlive.size(); i++) {
                ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) headItem.getItemMeta();
                skullMeta.setOwningPlayer(playersAlive.get(i));
                skullMeta.setDisplayName(playersAlive.get(i).getName());
                headItem.setItemMeta(skullMeta);
                inventory.setItem(i, headItem);
            }
            inventory.setItem(26, barrier);
            for (Player p : players) {
                p.teleport(new Location(world, -1000, 33, -1000, 180, 0));
                p.sendMessage("§c" + pasme.getPlayer().getName() + " 发现了 " + ((SkullMeta) pasme.getRightClicked().getHelmet().getItemMeta()).getOwningPlayer().getName() + " 的尸体！");
                p.sendMessage("§a你有" + voteTime + " 秒投票时间，不投则为弃权！");
            }
            voteTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                HashMap<Player, Integer> voteMapCopy = new HashMap<>(voteMap);
                Player playerToBeKilled = null;
                int votes = 0;
                for (Map.Entry<Player, Integer> entry : voteMapCopy.entrySet()) {
                    if (entry.getValue() > votes) {
                        playerToBeKilled = entry.getKey();
                        votes = entry.getValue();
                    }
                }
                if (playerToBeKilled == null) {
                    for (Player p : players) {
                        p.sendMessage("§e没有人被放逐，游戏继续！");
                        p.teleport(new Location(world, -1000.0, 64, -981.0));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 4, false, false));
                    }
                    return;
                }
                for (Map.Entry<Player, Integer> entry : voteMapCopy.entrySet()) {
                    if (entry.getKey().equals(playerToBeKilled)) {
                        continue;
                    }
                    if (entry.getValue() == votes) {
                        for (Player p : players) {
                            p.sendMessage("§e没有人被放逐，游戏继续！");
                            p.teleport(new Location(world, -1000.0, 64, -981.0));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 4, false, false));
                        }
                        return;
                    }
                }
                playerToBeKilled.setHealth(0);
                for (Player p : players) {
                    p.sendMessage("§c" + playerToBeKilled.getName() + "被放逐了！");
                    p.teleport(new Location(world, -1000.0, 64, -981.0));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 4, false, false));
                }

            }, 20 * voteTime);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent edbee) {
        if (!players.contains(edbee.getDamager())) {
            return;
        }
        if (humans.contains(edbee.getDamager())) {
            edbee.setCancelled(true);
            return;
        }
        if (edbee.getDamager() instanceof Player) {
            if (edbee.getDamager().getLocation().toVector().distance(edbee.getEntity().getLocation().toVector()) > 3) {
                edbee.setCancelled(true);
            } else if (!((Player) edbee.getDamager()).getInventory().getItemInMainHand().getType().equals(Material.IRON_AXE)) {
                edbee.setCancelled(true);
            } else if (edbee.getDamager().getLocation().getY() < 40) {
                edbee.setCancelled(true);
            } else {
                edbee.setCancelled(true);
                if (edbee.getEntity() instanceof Player) {
                    if (!world.getBlockAt(-995, 41, -1007).isBlockPowered()) {
                        if (((Player) edbee.getEntity()).hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                            return;
                        }
                    }
                    double health = ((Player) edbee.getEntity()).getHealth() - edbee.getDamage();
                    if (health < 0) {
                        health = 0;
                    }
                    ((Player) edbee.getEntity()).setHealth(health);
                    ((Player) edbee.getDamager()).addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 200, false, false));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent pde) {
        if (!players.contains(pde.getEntity())) {
            return;
        }
        if (pde.getEntity().getLocation().getY() >= 38) {
            if (playersAlive.contains(pde.getEntity())) {
                Location l = pde.getEntity().getLocation();
                l.setY(l.getY() - 0.65);
                ArmorStand head = (ArmorStand) world.spawnEntity(l, EntityType.ARMOR_STAND);
                ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) headItem.getItemMeta();
                skullMeta.setOwningPlayer(pde.getEntity());
                headItem.setItemMeta(skullMeta);
                head.setBasePlate(false);
                head.setSmall(true);
                head.getEquipment().setHelmet(headItem);
                head.setGravity(false);
                head.setCustomName(pde.getEntity().getName());
                head.setCustomNameVisible(false);
                head.setInvisible(true);
            }
        }
        werewolves.remove(pde.getEntity());
        humans.remove(pde.getEntity());
        playersAlive.remove(pde.getEntity());
        pde.getEntity().getInventory().clear();
        pde.getEntity().setGameMode(GameMode.SPECTATOR);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent ice) {
        if (!(ice.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!players.contains(ice.getWhoClicked())) {
            return;
        }
        if (ice.getWhoClicked().getLocation().getY() < 40) {
            ice.setCancelled(true);
            if (ice.getCurrentItem() == null) {
                return;
            }
            if (ice.getWhoClicked().getGameMode().equals(GameMode.SPECTATOR)) {
                return;
            }
            if (ice.getCurrentItem().getType().equals(Material.PLAYER_HEAD)) {
                Player owner = (Player) ((SkullMeta) ice.getCurrentItem().getItemMeta()).getOwningPlayer();
                Player clicker = (Player) ice.getWhoClicked();
                ice.getWhoClicked().sendMessage("§f你投票给 §c" + owner.getName());
                voteMap.putIfAbsent(owner, 0);
                if (initialVote.get(clicker) == null) { //没投过票
                    voteMap.put(owner, voteMap.get(owner) + 1);
                    initialVote.put(clicker, owner);
                } else { //投过票
                    if (initialVote.get(clicker).equals(owner)) {
                        return;
                    } else {
                        voteMap.put(initialVote.get(clicker), voteMap.get(initialVote.get(clicker)) - 1);
                        voteMap.put(owner, voteMap.get(owner) + 1);
                        initialVote.put(clicker, owner);
                    }
                }
            } else if (ice.getCurrentItem().getType().equals(Material.BARRIER)) {
                ice.getWhoClicked().sendMessage("§c你弃权了！");
                Player clicker = (Player) ice.getWhoClicked();
                if (initialVote.get(clicker) == null) { //没投过票
                    return;
                } else {
                    voteMap.put(initialVote.get(clicker), voteMap.get(initialVote.get(clicker)) - 1);
                    initialVote.put(clicker, null);
                }

            }
        } else {
            ice.setCancelled(true);
            if (werewolves.contains(ice.getWhoClicked())) {
                return;
            }
            if (ice.getCurrentItem() == null) {
                return;
            }
            if (ice.getCurrentItem().getType().equals(Material.DRAGON_BREATH)) {
                dragonBreathNumber += ice.getCurrentItem().getAmount();
                ice.getInventory().clear();
                ice.getWhoClicked().getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 1));
            }
            werewolf.getObjective("werewolf").getScore("现有血清").setScore(dragonBreathNumber);
        }
    }

    @EventHandler
    public void preventRegen(EntityRegainHealthEvent erhe) {
        if (!(erhe.getEntity() instanceof Player)) {
            return;
        }
        if (!(players.contains(erhe.getEntity()))) {
            return;
        }
        if (!((Player) erhe.getEntity()).getGameMode().equals(GameMode.ADVENTURE)) {
            return;
        }
        if (erhe.getRegainReason().equals(EntityRegainHealthEvent.RegainReason.SATIATED)) {
            erhe.setCancelled(true);
        } else if (erhe.getRegainReason().equals(EntityRegainHealthEvent.RegainReason.EATING)) {
            erhe.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerChangeGame(PlayerChangeGameEvent pcge) {
        players.remove(pcge.getPlayer());
        humans.remove(pcge.getPlayer());
        werewolves.remove(pcge.getPlayer());
        playersAlive.remove(pcge.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent pqe) {
        players.remove(pqe.getPlayer());
        humans.remove(pqe.getPlayer());
        werewolves.remove(pqe.getPlayer());
        playersAlive.remove(pqe.getPlayer());
    }

    private ItemStack generateItemStack(Material material, String name, int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public List<Player> getPlayers() {
        return players;
    }

    private void endGame() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Entity e : world.getNearbyEntities(new BoundingBox(-1500, 60, -1500, -500, 256, -500))) {
                if (e instanceof ArmorStand) {
                    e.remove();
                }
            }
            clearChests();
            removeSpectateButton();
            placeStartButton();
            HandlerList.unregisterAll(this);
        }, 100);
        players.clear();
        playersWhoUsedButton.clear();
        humans.clear();
        werewolves.clear();
        playersAlive.clear();
        team.unregister();
        running = false;
        gameUUID = UUID.randomUUID();
        cancelGameTasks();
    }

    private void clearChests() {
        for (Location l : locations) {
            ((Chest) world.getBlockAt(l).getState()).getBlockInventory().clear();
            //Bukkit.broadcastMessage(l.toString() + "的方块是§b" + world.getBlockAt(l).getType());
        }
    }

    @Override
    protected void initializeGameRunnable() {
        gameRunnable = () -> {

            potionSpawnPeriod = Werewolf.potionSpawnPeriod;
            werewolfNumber = Werewolf.werewolfNumber;
            voteTime = Werewolf.voteTime;
            team = werewolf.registerNewTeam("werewolf");
            team.setNameTagVisibility(NameTagVisibility.NEVER);
            team.setCanSeeFriendlyInvisibles(false);
            team.setAllowFriendlyFire(true);
            players.addAll(getPlayersNearHub(50, 50, 50));
            while (werewolves.size() < werewolfNumber) {
                if (players.size() < werewolfNumber) {
                    for (Player p : players) {
                        p.sendMessage("§c总人数小于狼人数量！");
                    }
                    players.clear();
                    werewolves.clear();
                    team.unregister();
                    return;
                }
                Player p = players.get(random.nextInt(players.size()));
                if (!werewolves.contains(p)) {
                    werewolves.add(p);
                    team.addPlayer(p);
                }
            }
            for (Player p : players) {
                if (!werewolves.contains(p)) {
                    humans.add(p);
                    team.addPlayer(p);
                }
            }
            if (players.size() < 3) {
                for (Player p : players) {
                    p.sendMessage("§c至少需要3人才能开始游戏！");
                }
                players.clear();
                humans.clear();
                werewolves.clear();
                team.unregister();
            } else if (players.size() > 26) {
                for (Player p : players) {
                    p.sendMessage("§c玩家人数至多为26！");
                }
                players.clear();
                humans.clear();
                werewolves.clear();
                team.unregister();
            } else if (humans.size() <= werewolves.size()) {
                for (Player p : players) {
                    p.sendMessage("§c人类数量必须大于狼人！");
                }
                players.clear();
                humans.clear();
                werewolves.clear();
                team.unregister();
            } else {
                running = true;
                pm.addPacketListener(pa);
                playersAlive.addAll(players);
                dragonBreathNeeded = humans.size();
                dragonBreathNumber = 0;
                werewolvesCopy.addAll(werewolves);
                werewolf.getObjective("werewolf").getScore("需要血清").setScore(dragonBreathNeeded);
                werewolf.getObjective("werewolf").getScore("现有血清").setScore(0);
                removeStartButton();
                startCountdown(countDownSeconds);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Bukkit.getPluginManager().registerEvents(instance, plugin);
                    placeSpectateButton();
                    for (Player p : werewolves) {
                        p.sendMessage("§c你是狼人！");
                        String string = "";
                        for (int i = 0; i < werewolves.size(); i++) {
                            string += (" " + werewolves.get(i).getName());
                        }
                        p.sendMessage("§c狼人玩家有 " + string);
                        p.getInventory().setItem(4, sword);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 999999, 15, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 999999, 200, false, false));
                    }
                    for (Player p : players) {
                        p.setScoreboard(werewolf);
                        p.teleport(new Location(world, -1000.0, 64, -981.0));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 4, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 999999, 0, false, false));
                    }
                }, 20 * countDownSeconds);

                taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                    for (Player p : players) {
                        p.sendMessage("§a血清已刷新！");
                    }

                    if (!world.getBlockAt(-995, 41, -1007).isBlockPowered()) { //普通模式
                        ((Chest) (world.getBlockAt(locations[random.nextInt(locations.length)]).getState())).getBlockInventory().addItem(dragon_breath);
                    } else { //手速模式
                        for (Location l : locations) {
                            ((Chest) world.getBlockAt(l).getState()).getBlockInventory().addItem(dragon_breath);
                        }
                    }
                }, 20 * countDownSeconds + 20 * potionSpawnPeriod, 20 * potionSpawnPeriod));

                taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                    if (dragonBreathNumber >= dragonBreathNeeded) {
                        pm.removePacketListener(pa);
                        List<Player> humansCopy = new ArrayList<>(humans);
                        List<Player> playersCopy = new ArrayList<>(players);
                        for (Player p : humansCopy) {
                            spawnFireworks(p);
                        }
                        for (Player p : playersCopy) {
                            p.sendTitle("§e血清数量到达标准，人类获胜！", null, 5, 50, 5);
                            String string = "";
                            for (int i = 0; i < werewolvesCopy.size(); i++) {
                                string += (" " + werewolvesCopy.get(i).getName());
                            }
                            p.sendMessage("§b狼人玩家有 " + string);
                            p.resetPlayerWeather();
                            p.resetPlayerTime();
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                p.teleport(hubLocation);
                                Bukkit.getPluginManager().callEvent(new PlayerEndGameEvent(p, instance));
                            }, 100);
                        }
                        endGame();
                        return;
                    }
                    if (werewolves.size() <= 0) {
                        pm.removePacketListener(pa);
                        List<Player> humansCopy = new ArrayList<>(humans);
                        List<Player> playersCopy = new ArrayList<>(players);
                        for (Player p : humansCopy) {
                            spawnFireworks(p);
                        }
                        for (Player p : playersCopy) {
                            p.sendTitle("§e狼人被消灭，人类获胜！", null, 5, 50, 5);
                            String string = "";
                            for (int i = 0; i < werewolvesCopy.size(); i++) {
                                string += (" " + werewolvesCopy.get(i).getName());
                            }
                            p.sendMessage("§b狼人玩家有 " + string);
                            p.resetPlayerWeather();
                            p.resetPlayerTime();
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                p.teleport(hubLocation);
                                Bukkit.getPluginManager().callEvent(new PlayerEndGameEvent(p, instance));
                            }, 100);
                        }
                        endGame();
                        return;
                    }
                    if (humans.size() <= 0) {
                        pm.removePacketListener(pa);
                        List<Player> werewolvesCopy = new ArrayList<>(werewolves);
                        List<Player> playersCopy = new ArrayList<>(players);
                        for (Player p : werewolvesCopy) {
                            spawnFireworks(p);
                        }
                        for (Player p : playersCopy) {
                            p.sendTitle("§e无人类幸存，狼人获胜！", null, 5, 50, 5);
                            String string = "";
                            for (int i = 0; i < werewolvesCopy.size(); i++) {
                                string += (" " + werewolvesCopy.get(i).getName());
                            }
                            p.sendMessage("§b狼人玩家有 " + string);
                            p.resetPlayerWeather();
                            p.resetPlayerTime();
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                p.teleport(hubLocation);
                                Bukkit.getPluginManager().callEvent(new PlayerEndGameEvent(p, instance));
                            }, 100);
                        }
                        endGame();
                        return;
                    }
                }, 100, 1));
            }
        };
    }

    @Override
    protected void savePlayerQuitData(Player p) throws IOException {
        PlayerQuitData quitData = new PlayerQuitData(p, this, gameUUID);
        quitData.getData().put("team", whichGroup(p));
        setPlayerQuitData(p.getUniqueId(), quitData);
        players.remove(p);
        humans.remove(p);
        werewolves.remove(p);
        playersAlive.remove(p);
    }

    private List<Player> whichGroup(Player p) {
        if (humans.contains(p)) {
            return humans;
        } else if (werewolves.contains(p)) {
            return werewolves;
        } else {
            return null;
        }
    }

    @Override
    protected void rejoin(Player p) {
        if (!running) {
            p.sendMessage("§c游戏已经结束！");
            return;
        }
        if (!getPlayerQuitData(p.getUniqueId()).getGameUUID().equals(gameUUID)) {
            p.sendMessage("§c游戏已经结束！");
            return;
        }
        PlayerQuitData pqd = getPlayerQuitData(p.getUniqueId());
        pqd.restoreBasicData(p);
        players.add(p);
        team.addPlayer(p);
        if (pqd.getGameMode().equals(GameMode.ADVENTURE)) {
            playersAlive.add(p);
        }
        p.setScoreboard(werewolf);
        if (pqd.getData().get("team") != null) {
            ((List<Player>) pqd.getData().get("team")).add(p);
        }
        setPlayerQuitData(p.getUniqueId(), null);
    }
}

package io.github.runelynx.runicparadise.faith.modules;

import io.github.runelynx.runicparadise.RunicParadise;
import io.github.runelynx.runicparadise.RunicUtilities;
import io.github.runelynx.runicparadise.faith.FaithCore;
import io.github.runelynx.runicparadise.faith.SummonableMob;
import io.github.runelynx.runicparadise.faith.SummoningDropChance;
import io.github.runelynx.runicuniverse.RunicMessaging;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import static io.github.runelynx.runicparadise.faith.FaithCore.*;
import static org.bukkit.Bukkit.getServer;

public class SummoningSystem {

    private Boolean active = false;

    public SummoningSystem() {
        if (activate()) {
            this.active = true;
        } else {
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4Failed to load Faith Module: Summoning System"));
        }
    }

    private Boolean activate() {

        if (this.active) {
            //Must deactivate first if you want to activate again
            return true;
        }

        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[FAITH STARTUP] SUMMONING: Activating ...");
        registerArenaLocations();
        if (faithCoreSummoningLocations.size() != 3) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[FAITH STARTUP] SUMMONING: *FAILURE* Could not register all 3 summoning system locations!");
            return false;
        }

        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[FAITH STARTUP] SUMMONING: Successfully parsed summoning system locations");

        // Components must be registered before Mobs !!
        registerSummoningComponents();
        registerSummoningMobs();

        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[FAITH STARTUP] SUMMONING: Activation complete!");
        this.active = true;
        return true;
    }

    public Boolean reactivate() {
        if (this.active) {
            // Module is running - so deactivate first
            if (deactivate()) {
                if (activate()) {
                    return true;
                }
            }
        } else {
            // Module is not running, so just activate
            if (activate()) {
                return true;
            }
        }
        return false;
    }

    public Boolean deactivate() {

        this.active = false;
        return true;
    }

    private void registerArenaLocations() {
        ConfigurationSection summonLocSection = FaithCore.getFaithConfig().getConfigurationSection("Faith.SummoningSystem.ArenaZone");
        Set<String> summonLocConfigList = summonLocSection.getKeys(false);


        faithCoreSummoningLocations.put("ArenaPos1", new Location(
                Bukkit.getWorld(summonLocSection.getString("World")),
                summonLocSection.getInt("X1"),
                summonLocSection.getInt("Y1"),
                summonLocSection.getInt("Z1")));

        faithCoreSummoningLocations.put("ArenaPos2", new Location(
                Bukkit.getWorld(summonLocSection.getString("World")),
                summonLocSection.getInt("X2"),
                summonLocSection.getInt("Y2"),
                summonLocSection.getInt("Z2")));

        ConfigurationSection summonSpawnSection = FaithCore.getFaithConfig().getConfigurationSection("Faith.SummoningSystem.ArenaZone.ArenaSpawnLocation");
        Set<String> summonSpawnConfigList = summonSpawnSection.getKeys(false);

        faithCoreSummoningLocations.put("ArenaSpawnLocation", new Location(
                Bukkit.getWorld(summonSpawnSection.getString("World")),
                summonSpawnSection.getInt("X"),
                summonSpawnSection.getInt("Y"),
                summonSpawnSection.getInt("Z")));
    }

    private Boolean addSummoningItem(String id, String name, String lore1, String lore2, String lore3, String lore4, String lore5,
                                     String itemType, List<String> dropList) {

        ArrayList<String> loreList = RunicUtilities.processLoreStringsToArray(lore1, lore2, lore3, lore4, lore5);


        ItemStack item = new ItemStack(Material.valueOf(itemType));
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(loreList);

        if (FaithCore.faithCoreItemDataKeys.get("SummoningItem") == null) {
            Bukkit.getLogger().log(Level.INFO, "SummoningItem key is indeed null. :(");
        }

        meta.getPersistentDataContainer().set(FaithCore.faithCoreItemDataKeys.get("SummoningItem"), PersistentDataType.STRING, id);

        item.setItemMeta(meta);

        for (String itemStr : dropList) {
            String[] strings = itemStr.split(";");
            //Debug

            FaithCore.faithCoreSummoningComponents.put(id, item);
            FaithCore.faithCoreSummoningDrops.put(EntityType.valueOf(strings[0]), new SummoningDropChance(id, Double.parseDouble(strings[1])));
        }

        return true;
    }

    private Boolean registerSummoningComponents() {

        ConfigurationSection summonItemSection = FaithCore.getFaithConfig().getConfigurationSection("Faith.SummoningSystem.Items");
        Set<String> summonItemConfigList = summonItemSection.getKeys(false);
        int count = 0;

        for (String summonItemKey : summonItemConfigList) {

            ItemStack item = new ItemStack(Material.valueOf(summonItemSection.getString(summonItemKey + ".Type")));

            addSummoningItem(summonItemKey,
                    summonItemSection.getString(summonItemKey + ".DisplayName"),
                    summonItemSection.getString(summonItemKey + ".Lore1"),
                    summonItemSection.getString(summonItemKey + ".Lore2"),
                    summonItemSection.getString(summonItemKey + ".Lore3"),
                    summonItemSection.getString(summonItemKey + ".Lore4"),
                    summonItemSection.getString(summonItemKey + ".Lore5"),
                    summonItemSection.getString(summonItemKey + ".Type"),
                    summonItemSection.getStringList(summonItemKey + ".Drops"));

            count++;
        }

        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[FAITH STARTUP] SUMMONING: Successfully parsed " + count + " summoning components");
        return true;
    }

    private Boolean registerSummoningMobs() {

        // Register SF category that's needed for registering the summon ball recipes
        Category category = registerSlimefunCategoryForFaith();

        ConfigurationSection summonMobsSection = FaithCore.getFaithConfig()
                .getConfigurationSection("Faith.SummoningSystem.Mobs");
        Set<String> summonMobsConfigList = summonMobsSection.getKeys(false);
        int count = 0;

        for (String summonMobsKey : summonMobsConfigList) {

            SummonableMob mob = new SummonableMob(
                    summonMobsKey,
                    summonMobsSection.getString(summonMobsKey + ".Name"),
                    EntityType.valueOf(summonMobsSection.getString(summonMobsKey + ".Type")),
                    summonMobsSection.getInt(summonMobsKey + ".Health"),
                    summonMobsSection.getStringList(summonMobsKey + ".Effects"),
                    summonMobsSection.getBoolean(summonMobsKey + ".Glowing"),
                    summonMobsSection.getBoolean(summonMobsKey + ".Invisible"),
                    summonMobsSection.getInt(summonMobsKey + ".Faith"),
                    summonMobsSection.getInt(summonMobsKey + ".Difficulty"),
                    summonMobsSection.getInt(summonMobsKey + ".Zeal"));

            faithCoreSummonableMobs.put(summonMobsKey, mob);


            ArrayList<ItemStack> items = new ArrayList<ItemStack>();

            for (String itemStr : summonMobsSection.getStringList(summonMobsKey + ".Recipe")) {
                String[] strings = itemStr.split(";");

                if (strings[0].equalsIgnoreCase("CUSTOM")) {

                    int counter = 0;
                    while (counter < Integer.valueOf(strings[2])) {
                        items.add(faithCoreSummoningComponents.get(strings[1]));
                        counter++;
                    }
                } else if (strings[0].equalsIgnoreCase("VANILLA")) {
                    int counter = 0;
                    while (counter < Integer.valueOf(strings[2])) {
                        items.add(new ItemStack(Material.valueOf(strings[1])));
                        counter++;
                    }
                }
            }

            registerSlimefunItemsForFaith(
                    category,
                    summonMobsKey,
                    ChatColor.BLUE + "Faith" +
                            ChatColor.DARK_AQUA + "Ball " +
                            ChatColor.DARK_GRAY + "[" + summonMobsSection.getString(summonMobsKey + ".Name") +
                            ChatColor.DARK_GRAY + "]",
                    new ItemStack[]{
                            items.get(0), items.get(1), items.get(2),
                            items.get(3), items.get(4), items.get(5),
                            items.get(6), items.get(7), items.get(8)},
                    mob.getDifficulty()
            );

            count++;
        }

        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[FAITH STARTUP] SUMMONING: Successfully parsed " + count + " summoning mobs; mobs & SF faith ball recipes are registered!");

        return true;
    }

    private static Category registerSlimefunCategoryForFaith() {

        NamespacedKey categoryId = new NamespacedKey(RunicParadise.getInstance(), "RunicFaith");
        me.mrCookieSlime.Slimefun.cscorelib2.item.CustomItem categoryItem =
                new me.mrCookieSlime.Slimefun.cscorelib2.item.CustomItem(
                        Material.HEART_OF_THE_SEA, "&9Faith&3Balls", "", "&a> Click to open");

        Category category = new Category(categoryId, categoryItem);

        category.register();

        return category;
    }

    public static void givePlayerSummoningComponents(Player p) {
        Collection<ItemStack> items = faithCoreSummoningComponents.values();

        for (ItemStack i : items) {
            p.getLocation().getWorld().dropItemNaturally(p.getLocation(), i);
        }

        String message = "&1Hi! &2You &3just &4got &5some &6items. &7This &8message &9is &aa &btest &c:)";
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message)));

    }

    public static void registerSlimefunItemsForFaith(Category category, String mobId, String ballName, ItemStack[] items,  int diff) {

        SlimefunItemStack sfis =  new SlimefunItemStack(mobId, Material.HEART_OF_THE_SEA, ballName);
        ItemMeta sfisMeta = sfis.getItemMeta();
        sfisMeta.getPersistentDataContainer().set(FaithCore.faithCoreItemDataKeys.get("FaithBall"), PersistentDataType.STRING, mobId);

        ArrayList<String> lore = new ArrayList<String>();
        switch (diff) {
            case 1:
                lore.add(ChatColor.GREEN + "" + ChatColor.ITALIC + "The ball is completely peaceful...");
                lore.add(ChatColor.GREEN + "" + ChatColor.ITALIC + "you wonder if anything is inside.");
                lore.add("");
                lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "Summon the being inside at the summoning arena!");
                break;
            case 2:
                lore.add(ChatColor.AQUA + "" + ChatColor.ITALIC + "You feel something stirring inside...");
                lore.add(ChatColor.AQUA + "" + ChatColor.ITALIC + "doesn't feel like much of a threat though.");
                lore.add("");
                lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "Summon the being inside at the summoning arena!");
                break;
            case 3:
                lore.add(ChatColor.YELLOW + "" + ChatColor.ITALIC + "It feels like something's kicking");
                lore.add(ChatColor.YELLOW + "" + ChatColor.ITALIC + "around in there. What could it be?");
                lore.add("");
                lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "Summon the being inside at the summoning arena!");
                break;
            case 4:
                lore.add(ChatColor.RED + "" + ChatColor.ITALIC + "Whatever's inside is speaking into");
                lore.add(ChatColor.RED + "" + ChatColor.ITALIC + "your mind, though you don't understand");
                lore.add(ChatColor.RED + "" + ChatColor.ITALIC + "its words. It must have some decent power!");
                lore.add("");
                lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "Summon the being inside at the summoning arena!");
                break;
            case 5:
                lore.add(ChatColor.LIGHT_PURPLE + "" + ChatColor.ITALIC + "You feel the ball tugging at your");
                lore.add(ChatColor.LIGHT_PURPLE + "" + ChatColor.ITALIC + "soul and driving fear directly into");
                lore.add(ChatColor.LIGHT_PURPLE + "" + ChatColor.ITALIC + "your mind. Proceed with caution!");
                lore.add("");
                lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "Summon the being inside at the summoning arena!");
                break;
        }
        sfisMeta.setLore(lore);
        sfis.setItemMeta(sfisMeta);

        SlimefunItem faithSummoningBall = new SlimefunItem(
                category,
                sfis,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                items);
        faithSummoningBall.register(RunicParadise.getInstance());

    }


    public static void spawnSummoningMob(PlayerInteractEvent event) {

        Location eventLoc = event.getClickedBlock().getLocation();
        Location arenaPos1 = faithCoreSummoningLocations.get("ArenaPos1");
        Location arenaPos2 = faithCoreSummoningLocations.get("ArenaPos2");

        // Check if the type matches what we expect for a faith ball
        if (event.getPlayer().getEquipment().getItemInMainHand() != null &&
                event.getPlayer().getEquipment().getItemInMainHand().getType() == Material.HEART_OF_THE_SEA) {
            ItemStack held = event.getPlayer().getEquipment().getItemInMainHand();

            // Check if there's meta
            if (held.hasItemMeta()) {
                ItemMeta meta = held.getItemMeta();
                PersistentDataContainer container = meta.getPersistentDataContainer();

                // Check if this is a valid faith ball
                if (container.has(FaithCore.faithCoreItemDataKeys.get("FaithBall"), PersistentDataType.STRING)) {

                    String mobName = container.get(FaithCore.faithCoreItemDataKeys.get("FaithBall"), PersistentDataType.STRING);
                    SummonableMob mob = faithCoreSummonableMobs.get(mobName);

                    if (eventLoc.getWorld().equals(arenaPos1.getWorld()) &&
                            eventLoc.getX() >= arenaPos1.getX() && eventLoc.getX() <= arenaPos2.getX() &&
                            eventLoc.getY() >= arenaPos1.getY() && eventLoc.getY() <= arenaPos2.getY() &&
                            eventLoc.getZ() >= arenaPos1.getZ() && eventLoc.getZ() <= arenaPos2.getZ()) {
                        // Everything checks out - proceed to spawn!

                        Bukkit.getLogger().log(Level.INFO, event.getPlayer().getName() + " is spawning a " + mobName + "!");

                        Location spawnLoc = faithCoreSummoningLocations.get("ArenaSpawn");
                        LivingEntity e = (LivingEntity) spawnLoc.getWorld().spawnEntity(spawnLoc, mob.getType());
                        e.setHealth(mob.getHealth());
                        e.setGlowing(mob.getGlowing());
                        if (mob.getInvis()) {
                            e.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 36000, 1, false,false ));
                            Bukkit.getLogger().log(Level.INFO, "Making " + mobName + " invisible!");
                        }
                        if (mob.getEffects() != null && mob.getEffects().size() > 0) {
                            for (String effectStr : mob.getEffects()) {
                                String[] strings = effectStr.split(";");
                                e.addPotionEffect(new PotionEffect(PotionEffectType.getByName(strings[0]), 36000, Integer.valueOf(strings[1]), false, false));
                                Bukkit.getLogger().log(Level.INFO, "Adding potion effect " + strings[0] + " with amplifier level " + strings[1] + " to " + mobName);
                            }
                        }

                        e.getPersistentDataContainer().set(FaithCore.faithCoreItemDataKeys.get("SummonedKillZealReward"), PersistentDataType.INTEGER, mob.getZeal());
                        e.getPersistentDataContainer().set(FaithCore.faithCoreItemDataKeys.get("SummonedKillFaithReward"), PersistentDataType.INTEGER, mob.getFaith());


                    } else {
                        // Player has a valida faith ball but isn't in the summoning arena
                        RunicMessaging.sendMessage(event.getPlayer(), RunicMessaging.RunicFormat.FAITH,
                                ChatColor.GRAY + "FaithBalls must be activated in the summoning arena");
                    }
                }
            }
        }



    }


}

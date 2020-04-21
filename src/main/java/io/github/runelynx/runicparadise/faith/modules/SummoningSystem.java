package io.github.runelynx.runicparadise.faith.modules;

import io.github.runelynx.runicparadise.RunicParadise;
import io.github.runelynx.runicparadise.RunicUtilities;
import io.github.runelynx.runicparadise.faith.FaithCore;
import io.github.runelynx.runicparadise.faith.SummonableMob;
import io.github.runelynx.runicparadise.faith.SummoningDropChance;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import static io.github.runelynx.runicparadise.faith.FaithCore.*;

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

        Bukkit.getLogger().log(Level.INFO, "~~~ Activating faith module - faithsummonarena ~~~");
        registerArenaLocations();
        if (faithCoreSummoningLocations.size() != 3) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4Could not register all 3 Summoning System Arena Locations"));
            return false;
        }

        registerSummoningMobs();

        registerSummoningComponents();

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
        FaithCore.faithCoreSummoningComponents.clear();
        FaithCore.faithCoreSummoningDrops.clear();
        FaithCore.faithCoreSummoningLocations.clear();
        FaithCore.faithCoreSummonableMobs.clear();
        this.active = false;
        return true;
    }

    private void registerArenaLocations() {
        ConfigurationSection summonLocSection = FaithCore.getFaithConfig().getConfigurationSection("Faith.SummoningSystem.ArenaZone");
        Set<String> summonLocConfigList = summonLocSection.getKeys(false);


        faithCoreSummoningLocations.put("ArenaPos1", new Location(
                Bukkit.getWorld(summonLocSection.getString(summonLocSection.getString("World"))),
                Integer.valueOf(summonLocSection.getString(summonLocSection.getString("X1"))),
                Integer.valueOf(summonLocSection.getString(summonLocSection.getString("Y1"))),
                Integer.valueOf(summonLocSection.getString(summonLocSection.getString("Z1")))));

        faithCoreSummoningLocations.put("ArenaPos2", new Location(
                Bukkit.getWorld(summonLocSection.getString(summonLocSection.getString("World"))),
                Integer.valueOf(summonLocSection.getString(summonLocSection.getString("X2"))),
                Integer.valueOf(summonLocSection.getString(summonLocSection.getString("Y2"))),
                Integer.valueOf(summonLocSection.getString(summonLocSection.getString("Z2")))));

        ConfigurationSection summonSpawnSection = FaithCore.getFaithConfig().getConfigurationSection("Faith.SummoningSystem.ArenaZone.ArenaSpawnLocation");
        Set<String> summonSpawnConfigList = summonSpawnSection.getKeys(false);

        faithCoreSummoningLocations.put("ArenaSpawn", new Location(
                Bukkit.getWorld(summonLocSection.getString(summonLocSection.getString("World"))),
                Integer.valueOf(summonLocSection.getString(summonLocSection.getString("X"))),
                Integer.valueOf(summonLocSection.getString(summonLocSection.getString("Y"))),
                Integer.valueOf(summonLocSection.getString(summonLocSection.getString("Z")))));
    }

    private Boolean addSummoningItem(String id, String name, String lore1, String lore2, String lore3,
                                     String itemType, List<String> dropList) {

        ArrayList<String> loreList = RunicUtilities.processLoreStringsToArray(lore1, lore2, lore3);

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
            Bukkit.getLogger().log(Level.INFO, strings[0] + " " + strings[1]);

            FaithCore.faithCoreSummoningComponents.put(id, item);
            FaithCore.faithCoreSummoningDrops.put(EntityType.valueOf(strings[0]), new SummoningDropChance(id, Double.parseDouble(strings[1])));
        }

        return true;
    }

    private Boolean registerSummoningComponents() {

        Bukkit.getLogger().log(Level.INFO, "~~~ Activating faith module - faithsummonitems ~~~");
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4RED WORDS!"));

        ConfigurationSection summonItemSection = FaithCore.getFaithConfig().getConfigurationSection("Faith.SummoningSystem.Items");
        Set<String> summonItemConfigList = summonItemSection.getKeys(false);

        for (String summonItemKey : summonItemConfigList) {

            ItemStack item = new ItemStack(Material.valueOf(summonItemSection.getString(summonItemKey + ".Type")));

            addSummoningItem(summonItemKey,
                    summonItemSection.getString(summonItemKey + ".DisplayName"),
                    summonItemSection.getString(summonItemKey + ".Lore1"),
                    summonItemSection.getString(summonItemKey + ".Lore2"),
                    summonItemSection.getString(summonItemKey + ".Lore3"),
                    summonItemSection.getString(summonItemKey + ".Type"),
                    summonItemSection.getStringList(summonItemKey + ".Drops"));
        }

        return true;
    }

    private Boolean registerSummoningMobs() {

        // Register SF category that's needed for registering the summon ball recipes
        Category category = registerSlimefunCategoryForFaith();

        ConfigurationSection summonMobsSection = FaithCore.getFaithConfig()
                .getConfigurationSection("Faith.SummoningSystem.Mobs");
        Set<String> summonMobsConfigList = summonMobsSection.getKeys(false);

        for (String summonMobsKey : summonMobsConfigList) {

            SummonableMob mob = new SummonableMob(
                    summonMobsKey,
                    summonMobsSection.getString(summonMobsKey + "Name"),
                    EntityType.valueOf(summonMobsSection.getString(summonMobsKey + "Type")),
                    summonMobsSection.getInt(summonMobsKey + "Health"),
                    summonMobsSection.getStringList(summonMobsKey + "Effects"),
                    summonMobsSection.getBoolean(summonMobsKey + "Glowing"),
                    summonMobsSection.getBoolean(summonMobsKey + "Invisible"),
                    summonMobsSection.getInt(summonMobsKey + "Faith"),
                    summonMobsSection.getInt(summonMobsKey + "Zeal"));

            faithCoreSummonableMobs.put(summonMobsKey, mob);


            ArrayList<ItemStack> items = new ArrayList<ItemStack>();

            for (String itemStr : summonMobsSection.getStringList(summonMobsKey + "Recipe")) {
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
                    summonMobsSection.getString(summonMobsKey + "Name"),
                    new ItemStack[]{
                            items.get(0), items.get(1), items.get(2),
                            items.get(3), items.get(4), items.get(5),
                            items.get(6), items.get(7), items.get(8)}
            );
        }

        return true;
    }

    private static Category registerSlimefunCategoryForFaith() {

        NamespacedKey categoryId = new NamespacedKey(RunicParadise.getInstance(), "RunicFaith");
        me.mrCookieSlime.Slimefun.cscorelib2.item.CustomItem categoryItem =
                new me.mrCookieSlime.Slimefun.cscorelib2.item.CustomItem(
                        Material.HEART_OF_THE_SEA, "&9Faith$3Balls", "", "&a> Click to open");

        Category category = new Category(categoryId, categoryItem);

        category.register();

        return category;
    }

    public static void registerSlimefunItemsForFaith(Category category, String mobId, String ballName, ItemStack[] items) {


        SlimefunItem faithSummoningBall = new SlimefunItem(
                category,
                new SlimefunItemStack(mobId, Material.HEART_OF_THE_SEA, ballName),
                RecipeType.ENHANCED_CRAFTING_TABLE,
                items);
        faithSummoningBall.register(RunicParadise.getInstance());

    }

    private void registerSummoningBallRecipe(String mobId, String mobName, int difficulty, List<String> recipeList) {


        ItemStack ball = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta meta = ball.getItemMeta();

        meta.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "Faith" + ChatColor.AQUA + "Ball " +
                ChatColor.translateAlternateColorCodes('&', mobName));

        ArrayList<String> loreList = new ArrayList<String>();
        loreList.add(ChatColor.GOLD + "This is a faith ball!");
        loreList.add(ChatColor.GOLD + "We need some faithy text here.");
        loreList.add(ChatColor.GOLD + "Yes, we definitely do!");

        meta.getPersistentDataContainer().set(FaithCore.faithCoreItemDataKeys.get("FaithBallMobId"), PersistentDataType.STRING, mobId);

        ball.setItemMeta(meta);

        // create a NamespacedKey for your recipe
        NamespacedKey key = new NamespacedKey(RunicParadise.getInstance(), mobId);

        // Create our custom recipe variable
        ShapelessRecipe recipe = new ShapelessRecipe(key, ball);


        Bukkit.addRecipe(recipe);

        Bukkit.getLogger().log(Level.INFO, "    Adding custom summoning recipe from faith config: " + mobId);

    }

    private void spawnSummoningMob(String id, String name, String type, int health, int difficulty,
                                   int strength, int speed, int regeneration, int jump, int resistance, boolean glowing,
                                   boolean invisibility, int faith, int zeal, List<String> itemList) {
        Location spawnLoc = faithCoreSummoningLocations.get("ArenaSpawn");
        LivingEntity e = (LivingEntity) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.valueOf(type));
    }


}

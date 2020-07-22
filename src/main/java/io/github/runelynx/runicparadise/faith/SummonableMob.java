package io.github.runelynx.runicparadise.faith;

import org.bukkit.entity.EntityType;

import java.util.List;

public class SummonableMob {
    String id;
    String name;
    EntityType type;
    int health;
    List<String> effects;
    Boolean glowing;
    Boolean invisibility;
    int faith;
    int zeal;
    int difficulty;


    public SummonableMob(String id, String name, EntityType type, int health,
                          List<String> effects,
                         Boolean glowing, Boolean invisibility,
                          int faith, int difficulty, int zeal) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.health = health;
        this.effects = effects;
        this.glowing = glowing;
        this.invisibility = invisibility;
        this.faith = faith;
        this.zeal = zeal;
        this.difficulty = difficulty;
    }

    public String getName() {
        return this.name;
    }
    public EntityType getType() {
        return this.type;
    }
    public int getHealth() {
        return this.health;
    }
    public List<String> getEffects() { return this.effects; }
    public Boolean getGlowing() { return this.glowing; }
    public Boolean getInvis() { return this.invisibility; }
    public int getZeal() {
        return this.zeal;
    }
    public int getFaith() {
        return this.faith;
    }
    public int getDifficulty() { return this.difficulty; }
}




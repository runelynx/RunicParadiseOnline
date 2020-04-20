package io.github.runelynx.runicparadise.faith;

public class SummoningDropChance {
    String summoningComponentID = "";
    Double dropChance = 0.00;

    public SummoningDropChance(String summItemID, Double dropChnc) {
       this.summoningComponentID = summItemID;
       this.dropChance = dropChnc;
    }

    public String getSummoningComponentID() {
        return this.summoningComponentID;
    }

    public Double getDropChance() {
        return this.dropChance;
    }


}

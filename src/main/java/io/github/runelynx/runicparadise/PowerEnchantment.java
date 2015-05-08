package io.github.runelynx.runicparadise;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.ItemStack;

public class PowerEnchantment extends Enchantment {

	public PowerEnchantment(int id) {
		super(id);
	}

	@Override
	public boolean canEnchantItem(ItemStack item) {
		return true; // can it enchant items?
	}

	@Override
	public boolean conflictsWith(Enchantment other) {
		return false; // will it conflict? In this case, nope
	}

	@Override
	public EnchantmentTarget getItemTarget() {
		return null; // you can define a target here, I just setted it to
						// null...
	}

	@Override
	public int getMaxLevel() {
		return 3; // the maxmimum level.
	}

	@Override
	public String getName() {
		return "Power Absorption: Beasts";
	}

	@Override
	public int getId() {
		return 210;// the id, in this case I choose 69 because no serious person
					// would ever choose that number, lucky me! Oh yeah, the max
					// id = 256
	}

	@Override
	public int getStartLevel() {
		return 1; // the start level of your enchantment
	}

}

package io.github.runelynx.runicparadise;

enum CustomItemType {
	RUNESTONE_REGENERATION_INGREDIENT("rp:runestone_regeneration_ingredient")
	;

	private String key;

	CustomItemType(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}
}

class CustomItem {

}

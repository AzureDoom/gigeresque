package com.bvanseg.gigeresque.common.item;

import com.bvanseg.gigeresque.common.Gigeresque;
import com.bvanseg.gigeresque.common.entity.Entities;
import com.bvanseg.gigeresque.common.fluid.Fluids;
import com.bvanseg.gigeresque.common.item.group.ItemGroups;
import com.bvanseg.gigeresque.common.util.GigeresqueInitializer;
import com.bvanseg.gigeresque.common.util.InitializationTimer;

import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class Items implements GigeresqueInitializer {
	private Items() {
	}

	private static Items instance;

	synchronized public static Items getInstance() {
		if (instance == null)
			instance = new Items();
		return instance;
	}

	public static final BucketItem BLACK_FLUID_BUCKET = new BucketItem(Fluids.BLACK_FLUID_STILL,
			new Item.Settings().recipeRemainder(net.minecraft.item.Items.BUCKET).maxCount(1).group(ItemGroups.GENERAL));

	public static final SurgeryKitItem SURGERY_KIT = new SurgeryKitItem(
			new Item.Settings().maxDamage(4).group(ItemGroups.GENERAL));

	@Override
	public void initialize() {
		InitializationTimer.initializingBlock("Items", this::initializeImpl);
	}

	private void initializeImpl() {
		Registry.register(Registry.ITEM, new Identifier(Gigeresque.MOD_ID, "black_fluid_bucket"), BLACK_FLUID_BUCKET);

		if (Gigeresque.config.features.surgeryKit) {
			Registry.register(Registry.ITEM, new Identifier(Gigeresque.MOD_ID, "surgery_kit"), SURGERY_KIT);
		}

		registerSpawnEgg("alien", Entities.ALIEN, 0x2C2B27, 0x4D4B3F);
		registerSpawnEgg("aquatic_alien", Entities.AQUATIC_ALIEN, 0x404345, 0x949597);
		registerSpawnEgg("aquatic_chestburster", Entities.AQUATIC_CHESTBURSTER, 0xDED29D, 0x2C2B27);
		registerSpawnEgg("chestburster", Entities.CHESTBURSTER, 0xDED29D, 0x2C2B27);
		registerSpawnEgg("egg", Entities.EGG, 0x554E45, 0x4D4932);
		registerSpawnEgg("facehugger", Entities.FACEHUGGER, 0xC7B986, 0x516B21);
		registerSpawnEgg("runner_alien", Entities.RUNNER_ALIEN, 0x3E230B, 0x623C25);
		registerSpawnEgg("runnerburster", Entities.RUNNERBURSTER, 0xDED29D, 0x2C2B27);
		DispenserBehavior.registerDefaults();
	}

	private void registerSpawnEgg(String path, EntityType<? extends MobEntity> entityType, int primaryColor,
			int secondaryColor) {
		Registry.register(Registry.ITEM, new Identifier(Gigeresque.MOD_ID, "%s_spawn_egg".formatted(path)),
				new SpawnEggItem(entityType, primaryColor, secondaryColor,
						new Item.Settings().group(ItemGroups.GENERAL)));
	}
}
package mods.cybercat.gigeresque.common.entity.impl;

import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mod.azure.azurelib.animatable.GeoEntity;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager.ControllerRegistrar;
import mod.azure.azurelib.core.animation.Animation.LoopType;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.util.AzureLibUtil;
import mods.cybercat.gigeresque.common.block.GIgBlocks;
import mods.cybercat.gigeresque.common.config.ConfigAccessor;
import mods.cybercat.gigeresque.common.entity.AlienEntity;
import mods.cybercat.gigeresque.common.entity.ai.pathing.CrawlerNavigation;
import mods.cybercat.gigeresque.common.entity.ai.tasks.FleeFireTask;
import mods.cybercat.gigeresque.common.entity.helper.GigAnimationsDefault;
import mods.cybercat.gigeresque.common.tags.GigTags;
import mods.cybercat.gigeresque.common.util.EntityUtils;
import mods.cybercat.gigeresque.common.util.GigVibrationListener;
import mods.cybercat.gigeresque.interfacing.Eggmorphable;
import mods.cybercat.gigeresque.interfacing.Host;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.AnimalPanic;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.BrainActivityGroup;
import net.tslat.smartbrainlib.api.core.SmartBrainProvider;
import net.tslat.smartbrainlib.api.core.behaviour.FirstApplicableBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.OneRandomBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.misc.Idle;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.MoveToWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.path.SetRandomWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.path.SetWalkTargetToAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.InvalidateAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetPlayerLookTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetRandomLookTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.TargetOrRetaliate;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.custom.NearbyBlocksSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.HurtBySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyLivingEntitySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyPlayersSensor;

public class HammerpedeEntity extends AlienEntity implements GeoEntity, SmartBrainOwner<HammerpedeEntity> {

	private final AnimatableInstanceCache cache = AzureLibUtil.createInstanceCache(this);
	private final CrawlerNavigation landNavigation = new CrawlerNavigation(this, level);

	public HammerpedeEntity(EntityType<? extends Monster> entityType, Level world) {
		super(entityType, world);
		this.dynamicGameEventListener = new DynamicGameEventListener<GigVibrationListener>(
				new GigVibrationListener(new EntityPositionSource(this, this.getEyeHeight()), 48, this));
		navigation = landNavigation;
	}

	@Override
	public void registerControllers(ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "livingController", 5, event -> {
			var isDead = this.dead || this.getHealth() < 0.01 || this.isDeadOrDying();
			if (event.isMoving() && !isDead && !this.swinging && !this.isAggressive())
				return event.setAndContinue(GigAnimationsDefault.WALK);
			if (event.isMoving() && !isDead && !this.swinging && this.isAggressive())
				return event.setAndContinue(GigAnimationsDefault.WALK_HOSTILE);
			else if (((this.getTarget() != null && this.doHurtTarget(getTarget()))) && !isDead)
				return event.setAndContinue(RawAnimation.begin().then("attack", LoopType.LOOP));
			else if (isDead && !this.swinging)
				return event.setAndContinue(GigAnimationsDefault.DEATH);
			else if (this.getTarget() != null && !event.isMoving() && !isDead)
				return event.setAndContinue(RawAnimation.begin().thenLoop("idle_alert"));
			else
				return event.setAndContinue(GigAnimationsDefault.IDLE);
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	@Override
	protected Brain.Provider<?> brainProvider() {
		return new SmartBrainProvider<>(this);
	}

	@Override
	protected void customServerAiStep() {
		tickBrain(this);
	}

	@Override
	public List<ExtendedSensor<HammerpedeEntity>> getSensors() {
		return ObjectArrayList.of(new NearbyPlayersSensor<>(), new NearbyLivingEntitySensor<HammerpedeEntity>()
				.setPredicate((target, entity) -> !((target instanceof AlienEntity || target instanceof Warden
						|| target instanceof ArmorStand)
						|| (target.getVehicle() != null
								&& target.getVehicle().getSelfAndPassengers().anyMatch(AlienEntity.class::isInstance))
						|| (target instanceof AlienEggEntity) || ((Host) target).isBleeding()
						|| ((Eggmorphable) target).isEggmorphing() || (EntityUtils.isFacehuggerAttached(target))
						|| (target.getFeetBlockState().getBlock() == GIgBlocks.NEST_RESIN_WEB_CROSS))
						&& !ConfigAccessor.isTargetBlacklisted(FacehuggerEntity.class, target) && target.isAlive()),
				new NearbyBlocksSensor<HammerpedeEntity>().setRadius(15)
						.setPredicate((block, entity) -> block.is(GigTags.ALIEN_REPELLENTS)),
				new HurtBySensor<>());
	}

	@Override
	public BrainActivityGroup<HammerpedeEntity> getCoreTasks() {
		return BrainActivityGroup.coreTasks(new LookAtTarget<>(), new FleeFireTask<>(1.2F), new AnimalPanic(2.0f),
				new MoveToWalkTarget<>());
	}

	@Override
	public BrainActivityGroup<HammerpedeEntity> getIdleTasks() {
		return BrainActivityGroup.idleTasks(
				new FirstApplicableBehaviour<HammerpedeEntity>(new TargetOrRetaliate<>(),
						new SetPlayerLookTarget<>().stopIf(target -> !target.isAlive()
								|| target instanceof Player && ((Player) target).isCreative()),
						new SetRandomLookTarget<>()),
				new OneRandomBehaviour<>(new SetRandomWalkTarget<>().speedModifier(0.65f),
						new Idle<>().runFor(entity -> entity.getRandom().nextInt(30, 60))));
	}

	@Override
	public BrainActivityGroup<HammerpedeEntity> getFightTasks() {
		return BrainActivityGroup.fightTasks(new InvalidateAttackTarget<>().stopIf(target -> !target.isAlive()),
				new SetWalkTargetToAttackTarget<>().speedMod(1.05F));
	}

	@Override
	protected void registerGoals() {
//		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35, false));
	}

	public static AttributeSupplier.Builder createAttributes() {
		return LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 15.0).add(Attributes.ARMOR, 1.0)
				.add(Attributes.ARMOR_TOUGHNESS, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
				.add(Attributes.ATTACK_KNOCKBACK, 0.0).add(Attributes.ATTACK_DAMAGE, 0.5)
				.add(Attributes.FOLLOW_RANGE, 16.0).add(Attributes.MOVEMENT_SPEED, 0.3300000041723251);
	}

	@Override
	public void onSignalReceive(ServerLevel var1, GameEventListener var2, BlockPos var3, GameEvent var4, Entity var5,
			Entity var6, float var7) {
		super.onSignalReceive(var1, var2, var3, var4, var5, var6, var7);
		this.getNavigation().moveTo(var3.getX(), var3.getY(), var3.getZ(), 0.9F);
	}

}
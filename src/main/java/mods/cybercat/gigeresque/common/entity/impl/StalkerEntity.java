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
import mods.cybercat.gigeresque.common.data.handler.TrackedDataHandlers;
import mods.cybercat.gigeresque.common.entity.AlienEntity;
import mods.cybercat.gigeresque.common.entity.ai.enums.AlienAttackType;
import mods.cybercat.gigeresque.common.entity.ai.pathing.CrawlerNavigation;
import mods.cybercat.gigeresque.common.entity.ai.sensors.NearbyLightsBlocksSensor;
import mods.cybercat.gigeresque.common.entity.ai.tasks.FleeFireTask;
import mods.cybercat.gigeresque.common.entity.ai.tasks.KillLightsTask;
import mods.cybercat.gigeresque.common.entity.helper.GigAnimationsDefault;
import mods.cybercat.gigeresque.common.tags.GigTags;
import mods.cybercat.gigeresque.common.util.EntityUtils;
import mods.cybercat.gigeresque.common.util.GigVibrationListener;
import mods.cybercat.gigeresque.interfacing.Eggmorphable;
import mods.cybercat.gigeresque.interfacing.Host;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.Bat;
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
import net.tslat.smartbrainlib.api.core.behaviour.custom.attack.AnimatableMeleeAttack;
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

public class StalkerEntity extends AlienEntity implements GeoEntity, SmartBrainOwner<StalkerEntity> {

	private final AnimatableInstanceCache cache = AzureLibUtil.createInstanceCache(this);
	private static final EntityDataAccessor<AlienAttackType> CURRENT_ATTACK_TYPE = SynchedEntityData
			.defineId(StalkerEntity.class, TrackedDataHandlers.ALIEN_ATTACK_TYPE);
	private final CrawlerNavigation landNavigation = new CrawlerNavigation(this, level);

	public StalkerEntity(EntityType<? extends Monster> entityType, Level world) {
		super(entityType, world);
		this.dynamicGameEventListener = new DynamicGameEventListener<GigVibrationListener>(
				new GigVibrationListener(new EntityPositionSource(this, this.getEyeHeight()), 48, this));
		navigation = landNavigation;
	}

	@Override
	public void registerControllers(ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "livingController", 5, event -> {
			var velocityLength = this.getDeltaMovement().horizontalDistance();
			var isDead = this.dead || this.getHealth() < 0.01 || this.isDeadOrDying();
			if (velocityLength >= 0.000000001 && !isDead && this.getLastDamageSource() == null
					&& this.entityData.get(STATE) == 0)
				if (animationSpeedOld >= 0.35F)
					return event.setAndContinue(GigAnimationsDefault.RUN);
				else
					return event.setAndContinue(GigAnimationsDefault.WALK);
			else if (isDead)
				return event.setAndContinue(GigAnimationsDefault.DEATH);
			if (this.getLastDamageSource() != null && this.hurtDuration > 0 && !isDead
					&& this.entityData.get(STATE) == 0)
				return event.setAndContinue(RawAnimation.begin().then("hurt", LoopType.PLAY_ONCE));
			else if (this.entityData.get(STATE) >= 1 && !isDead)
				return event.setAndContinue(RawAnimation.begin()
						.then(AlienAttackType.animationMappings.get(getCurrentAttackType()), LoopType.PLAY_ONCE));
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
	public List<ExtendedSensor<StalkerEntity>> getSensors() {
		return ObjectArrayList.of(new NearbyPlayersSensor<>(),
				new NearbyLivingEntitySensor<StalkerEntity>().setPredicate((entity,
						target) -> !((entity instanceof AlienEntity || entity instanceof Warden
								|| entity instanceof ArmorStand || entity instanceof Bat)
								|| !target.hasLineOfSight(entity)
								|| (entity.getVehicle() != null && entity.getVehicle().getSelfAndPassengers()
										.anyMatch(AlienEntity.class::isInstance))
								|| (entity instanceof AlienEggEntity) || ((Host) entity).isBleeding()
								|| ((Eggmorphable) entity).isEggmorphing() || (EntityUtils.isFacehuggerAttached(entity))
								|| (entity.getFeetBlockState().getBlock() == GIgBlocks.NEST_RESIN_WEB_CROSS)
										&& entity.isAlive())),
				new NearbyBlocksSensor<StalkerEntity>().setRadius(15)
						.setPredicate((block, entity) -> block.is(GigTags.ALIEN_REPELLENTS)),
				new NearbyLightsBlocksSensor<StalkerEntity>().setRadius(15)
						.setPredicate((block, entity) -> block.is(GigTags.DESTRUCTIBLE_LIGHT)),
				new HurtBySensor<>());
	}

	@Override
	public BrainActivityGroup<StalkerEntity> getCoreTasks() {
		return BrainActivityGroup.coreTasks(new LookAtTarget<>(), new FleeFireTask<>(1.3F), new KillLightsTask<>(),
				new MoveToWalkTarget<>());
	}

	@Override
	public BrainActivityGroup<StalkerEntity> getIdleTasks() {
		return BrainActivityGroup.idleTasks(
				new FirstApplicableBehaviour<StalkerEntity>(new TargetOrRetaliate<>(),
						new SetPlayerLookTarget<>().stopIf(target -> !target.isAlive()
								|| target instanceof Player && ((Player) target).isCreative()),
						new SetRandomLookTarget<>()),
				new OneRandomBehaviour<>(new SetRandomWalkTarget<>().speedModifier(1.05f),
						new Idle<>().runFor(entity -> entity.getRandom().nextInt(30, 60))));
	}

	@Override
	public BrainActivityGroup<StalkerEntity> getFightTasks() {
		return BrainActivityGroup.fightTasks(new InvalidateAttackTarget<>().stopIf(target -> !target.isAlive()),
				new SetWalkTargetToAttackTarget<>().speedMod(1.7F),
				new AnimatableMeleeAttack(20)
						.whenStarting(entity -> this.setAttackingState(this.getRandom().nextInt(0, 3)))
						.whenStopping(entity -> this.setAttackingState(0)));
	}

	@Override
	protected void registerGoals() {
//		this.goalSelector.addGoal(2, new LongerAlienMeleeAttackGoal(this, 1.35, false));
	}

	public static AttributeSupplier.Builder createAttributes() {
		return LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 15.0).add(Attributes.ARMOR, 2.0)
				.add(Attributes.ARMOR_TOUGHNESS, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
				.add(Attributes.FOLLOW_RANGE, 16.0).add(Attributes.MOVEMENT_SPEED, 0.23000000417232513)
				.add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.ATTACK_KNOCKBACK, 0.3);
	}

	@Override
	public void onSignalReceive(ServerLevel var1, GameEventListener var2, BlockPos var3, GameEvent var4, Entity var5,
			Entity var6, float var7) {
		super.onSignalReceive(var1, var2, var3, var4, var5, var6, var7);
		this.getNavigation().moveTo(var3.getX(), var3.getY(), var3.getZ(), 1.9F);
	}

	@Override
	public void tick() {
		super.tick();
		if (!level.isClientSide && getCurrentAttackType() == AlienAttackType.NONE) {
			setCurrentAttackType(switch (random.nextInt(5)) {
			case 0 -> AlienAttackType.NORMAL;
			case 1 -> AlienAttackType.HEAVY;
			case 2 -> AlienAttackType.NORMAL;
			case 3 -> AlienAttackType.HEAVY;
			default -> AlienAttackType.NORMAL;
			});
		}
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public boolean doHurtTarget(Entity target) {

		float additionalDamage = switch (getCurrentAttackType().genericAttackType) {
		case HEAVY -> 3.0f;
		default -> 0.0f;
		};

		if (target instanceof LivingEntity && !level.isClientSide) {
			switch (getCurrentAttackType().genericAttackType) {
			case NORMAL -> {
				return super.doHurtTarget(target);
			}
			case HEAVY -> {
				target.hurt(DamageSource.mobAttack(this), additionalDamage);
				return super.doHurtTarget(target);
			}
			}
		}
		return super.doHurtTarget(target);

	}

	public AlienAttackType getCurrentAttackType() {
		return entityData.get(CURRENT_ATTACK_TYPE);
	}

	public void setCurrentAttackType(AlienAttackType value) {
		entityData.set(CURRENT_ATTACK_TYPE, value);
	}

	@Override
	public void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(CURRENT_ATTACK_TYPE, AlienAttackType.NONE);
	}

}
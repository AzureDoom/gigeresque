package mods.cybercat.gigeresque.common.entity.impl.mutant;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mod.azure.azurelib.animatable.GeoEntity;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager.ControllerRegistrar;
import mod.azure.azurelib.core.animation.Animation.LoopType;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.util.AzureLibUtil;
import mods.cybercat.gigeresque.client.particle.Particles;
import mods.cybercat.gigeresque.common.Gigeresque;
import mods.cybercat.gigeresque.common.block.AcidBlock;
import mods.cybercat.gigeresque.common.block.GigBlocks;
import mods.cybercat.gigeresque.common.entity.ai.sensors.NearbyLightsBlocksSensor;
import mods.cybercat.gigeresque.common.entity.ai.sensors.NearbyRepellentsSensor;
import mods.cybercat.gigeresque.common.entity.ai.tasks.attack.AlienMeleeAttack;
import mods.cybercat.gigeresque.common.entity.ai.tasks.movement.FleeFireTask;
import mods.cybercat.gigeresque.common.entity.ai.tasks.blocks.KillLightsTask;
import mods.cybercat.gigeresque.common.entity.ai.tasks.movement.LeapAtTargetTask;
import mods.cybercat.gigeresque.common.entity.helper.AzureVibrationUser;
import mods.cybercat.gigeresque.common.entity.helper.CrawlerAlien;
import mods.cybercat.gigeresque.common.entity.helper.GigAnimationsDefault;
import mods.cybercat.gigeresque.common.tags.GigTags;
import mods.cybercat.gigeresque.common.util.DamageSourceUtils;
import mods.cybercat.gigeresque.common.util.GigEntityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
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
import net.tslat.smartbrainlib.api.core.sensor.custom.UnreachableTargetSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.HurtBySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyLivingEntitySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyPlayersSensor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StalkerEntity extends CrawlerAlien implements GeoEntity, SmartBrainOwner<StalkerEntity> {

    private final AnimatableInstanceCache cache = AzureLibUtil.createInstanceCache(this);
    public int breakingCounter = 0;

    public StalkerEntity(EntityType<? extends CrawlerAlien> entityType, Level world) {
        super(entityType, world);
        this.setMaxUpStep(0.1f);
        this.vibrationUser = new AzureVibrationUser(this, 1.9F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH,
                Gigeresque.config.stalkerXenoHealth).add(Attributes.ARMOR, Gigeresque.config.stalkerXenoArmor).add(
                Attributes.ARMOR_TOUGHNESS, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.0).add(Attributes.FOLLOW_RANGE,
                16.0).add(Attributes.MOVEMENT_SPEED, 0.23000000417232513).add(Attributes.ATTACK_DAMAGE,
                Gigeresque.config.stalkerAttackDamage).add(Attributes.ATTACK_KNOCKBACK, 0.3);
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "livingController", 5, event -> {
                    var velocityLength = this.getDeltaMovement().horizontalDistance();
                    var isDead = this.dead || this.getHealth() < 0.01 || this.isDeadOrDying();
                    if (velocityLength >= 0.000000001 && !isDead && this.getLastDamageSource() == null && event.getAnimatable().getAttckingState() == 0)
                        if (walkAnimation.speedOld >= 0.35F && event.getAnimatable().isAggressive())
                            return event.setAndContinue(GigAnimationsDefault.RUNNING);
                        else return event.setAndContinue(GigAnimationsDefault.MOVING);
                    if (this.getLastDamageSource() != null && this.hurtDuration > 0 && !isDead && event.getAnimatable().getAttckingState() == 0)
                        return event.setAndContinue(RawAnimation.begin().then("hurt", LoopType.PLAY_ONCE));
                    return event.setAndContinue(GigAnimationsDefault.IDLE);
                }).triggerableAnim("attack_heavy", GigAnimationsDefault.ATTACK_HEAVY) // attack
                        .triggerableAnim("attack_normal", GigAnimationsDefault.ATTACK_NORMAL) // attack
                        .triggerableAnim("death", GigAnimationsDefault.DEATH) // death
                        .triggerableAnim("idle", GigAnimationsDefault.IDLE) // idle
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected Brain.@NotNull Provider<?> brainProvider() {
        return new SmartBrainProvider<>(this);
    }

    @Override
    protected void customServerAiStep() {
        tickBrain(this);
        super.customServerAiStep();
    }

    @Override
    public List<ExtendedSensor<StalkerEntity>> getSensors() {
        return ObjectArrayList.of(new NearbyPlayersSensor<>(),
                new NearbyLivingEntitySensor<StalkerEntity>().setPredicate(
                        GigEntityUtils::entityTest),
                new NearbyBlocksSensor<StalkerEntity>().setRadius(7),
                new NearbyRepellentsSensor<StalkerEntity>().setRadius(15).setPredicate(
                        (block, entity) -> block.is(GigTags.ALIEN_REPELLENTS) || block.is(Blocks.LAVA)),
                new NearbyLightsBlocksSensor<StalkerEntity>().setRadius(7).setPredicate(
                        (block, entity) -> block.is(GigTags.DESTRUCTIBLE_LIGHT)), new UnreachableTargetSensor<>(),
                new HurtBySensor<>());
    }

    @Override
    public BrainActivityGroup<StalkerEntity> getCoreTasks() {
        return BrainActivityGroup.coreTasks(new LookAtTarget<>(), new FleeFireTask<>(1.3F), new MoveToWalkTarget<>());
    }

    @Override
    public BrainActivityGroup<StalkerEntity> getIdleTasks() {
        return BrainActivityGroup.idleTasks(
                new KillLightsTask<>().stopIf(target -> (this.isAggressive() || this.isVehicle() || this.isFleeing())),
                new FirstApplicableBehaviour<StalkerEntity>(new TargetOrRetaliate<>(),
                        new SetPlayerLookTarget<>().predicate(
                                target -> target.isAlive() && (!target.isCreative() || !target.isSpectator())),
                        new SetRandomLookTarget<>()),
                new OneRandomBehaviour<>(new SetRandomWalkTarget<>().speedModifier(0.9f),
                        new Idle<>().startCondition(entity -> !this.isAggressive()).runFor(
                                entity -> entity.getRandom().nextInt(30, 60))));
    }

    @Override
    public BrainActivityGroup<StalkerEntity> getFightTasks() {
        return BrainActivityGroup.fightTasks(new InvalidateAttackTarget<>().invalidateIf(
                        (entity, target) -> GigEntityUtils.removeTarget(target)), new LeapAtTargetTask<>(0),
                new SetWalkTargetToAttackTarget<>().speedMod((owner, target) -> Gigeresque.config.stalkerAttackSpeed),
                // move to
                new AlienMeleeAttack<>(13));// attack
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.isVehicle() && !this.isDeadOrDying() && !this.isInWater() && this.level().getGameRules().getBoolean(
                GameRules.RULE_MOBGRIEFING) && this.isAggressive()) {
            if (!this.level().isClientSide) breakingCounter++;
            if (breakingCounter > 10)
                for (var testPos : BlockPos.betweenClosed(blockPosition().relative(getDirection()),
                        blockPosition().relative(getDirection()).above(3))) {
                    if (!(level().getBlockState(testPos).is(Blocks.GRASS) || level().getBlockState(testPos).is(
                            Blocks.TALL_GRASS)))
                        if (level().getBlockState(testPos).is(GigTags.WEAK_BLOCKS) && !level().getBlockState(
                                testPos).isAir()) {
                            if (!level().isClientSide) this.level().destroyBlock(testPos, true, null, 512);
                            this.triggerAnim("attackController", "swipe");
                            breakingCounter = -90;
                            if (level().isClientSide()) {
                                for (var i = 2; i < 10; i++)
                                    level().addAlwaysVisibleParticle(Particles.GOO,
                                            this.getX() + ((this.getRandom().nextDouble() / 2.0) - 0.5) * (this.getRandom().nextBoolean() ? -1 : 1),
                                            this.getEyeY() - ((this.getEyeY() - this.blockPosition().getY()) / 2.0),
                                            this.getZ() + ((this.getRandom().nextDouble() / 2.0) - 0.5) * (this.getRandom().nextBoolean() ? -1 : 1),
                                            0.0, -0.15, 0.0);
                                level().playLocalSound(testPos.getX(), testPos.getY(), testPos.getZ(),
                                        SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS,
                                        0.2f + random.nextFloat() * 0.2f, 0.9f + random.nextFloat() * 0.15f, false);
                            }
                        } else if (!level().getBlockState(testPos).is(GigTags.ACID_RESISTANT) && !level().getBlockState(
                                testPos).isAir() && (getHealth() >= (getMaxHealth() * 0.50))) {
                            if (!level().isClientSide)
                                this.level().setBlockAndUpdate(testPos.above(),
                                        GigBlocks.BLACK_FLUID_BLOCK.defaultBlockState().setValue(AcidBlock.THICKNESS,
                                                4));
                            this.hurt(damageSources().generic(), 5);
                            breakingCounter = -90;
                        }
                }
            if (breakingCounter >= 25) breakingCounter = 0;
        }
        this.setNoGravity(
                !this.level().getBlockState(this.blockPosition().above()).isAir() && !this.level().getBlockState(
                        this.blockPosition().above()).is(
                        BlockTags.STAIRS) && !this.verticalCollision && !this.isDeadOrDying() && !this.isAggressive());
        this.setSpeed(this.isNoGravity() ? 0.7F : this.flyDist);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (!this.level().isClientSide) {
            var attacker = source.getEntity();
            if (source.getEntity() != null && attacker instanceof LivingEntity living)
                this.brain.setMemory(MemoryModuleType.ATTACK_TARGET, living);
        }

        if (DamageSourceUtils.isDamageSourceNotPuncturing(source, this.damageSources()))
            return super.hurt(source, amount);

        if (!this.level().isClientSide && source != damageSources().genericKill()) {
            var acidThickness = this.getHealth() < (this.getMaxHealth() / 2) ? 1 : 0;

            if (this.getHealth() < (this.getMaxHealth() / 4)) acidThickness += 1;
            if (amount >= 5) acidThickness += 1;
            if (amount > (this.getMaxHealth() / 10)) acidThickness += 1;
            if (acidThickness == 0) return super.hurt(source, amount);

            var newState = GigBlocks.BLACK_FLUID_BLOCK.defaultBlockState().setValue(AcidBlock.THICKNESS, acidThickness);

            if (this.getFeetBlockState().getBlock() == Blocks.WATER)
                newState = newState.setValue(BlockStateProperties.WATERLOGGED, true);
            if (!this.getFeetBlockState().is(GigTags.ACID_RESISTANT))
                level().setBlockAndUpdate(this.blockPosition(), newState);
        }
        return super.hurt(source, amount);
    }

    @Override
    public void generateAcidPool(int xOffset, int zOffset) {
        var pos = this.blockPosition().offset(xOffset, 0, zOffset);
        var posState = level().getBlockState(pos);
        var newState = GigBlocks.BLACK_FLUID.defaultBlockState();

        if (posState.getBlock() == Blocks.WATER) newState = newState.setValue(BlockStateProperties.WATERLOGGED, true);

        if (!(posState.getBlock() instanceof LiquidBlock)) return;
        level().setBlockAndUpdate(pos, newState);
    }

    @Override
    public double getMeleeAttackRangeSqr(LivingEntity livingEntity) {
        return this.getBbWidth() * 3.0f * (this.getBbWidth() * 3.0f) + livingEntity.getBbWidth();
    }

    @Override
    public boolean isWithinMeleeAttackRange(LivingEntity livingEntity) {
        double d = this.distanceToSqr(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
        return d <= this.getMeleeAttackRangeSqr(livingEntity);
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        if (target instanceof LivingEntity livingEntity && !this.level().isClientSide && this.getRandom().nextInt(0,
                10) > 7) {
            livingEntity.hurt(damageSources().mobAttack(this),
                    this.getRandom().nextInt(4) > 2 ? Gigeresque.config.stalkerTailAttackDamage : 0.0f);
            this.heal(1.0833f);
            return super.doHurtTarget(target);
        }
        if (target instanceof Creeper creeper) creeper.hurt(damageSources().mobAttack(this), creeper.getMaxHealth());
        this.heal(1.0833f);
        return super.doHurtTarget(target);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
    }

    @Override
    public boolean onClimbable() {
        setIsCrawling(this.horizontalCollision && !this.isNoGravity() && !this.level().getBlockState(
                this.blockPosition().above()).is(BlockTags.STAIRS) || this.isAggressive());
        return !this.level().getBlockState(this.blockPosition().above()).is(
                BlockTags.STAIRS) && !this.isAggressive() && this.fallDistance <= 0.1;
    }

    @Override
    public void travel(@NotNull Vec3 movementInput) {
        if (isEffectiveAi() && this.isInWater()) {
            moveRelative(getSpeed(), movementInput);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
            if (getTarget() == null) setDeltaMovement(getDeltaMovement().add(0.0, -0.005, 0.0));
        } else super.travel(movementInput);
    }

    @Override
    public boolean isPathFinding() {
        return false;
    }

}

package mods.cybercat.gigeresque.common.entity.ai.goal;

import java.util.EnumSet;

import org.jetbrains.annotations.Nullable;

import mods.cybercat.gigeresque.common.entity.AlienEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

public class AlienWalkGoal extends Goal {
	public static final int DEFAULT_INTERVAL = 120;
	protected final AlienEntity mob;
	protected double wantedX;
	protected double wantedY;
	protected double wantedZ;
	protected final double speedModifier;
	protected int interval;
	protected boolean forceTrigger;
	private final boolean checkNoActionTime;

	public AlienWalkGoal(AlienEntity pathfinderMob, double d) {
		this(pathfinderMob, d, 120);
	}

	public AlienWalkGoal(AlienEntity pathfinderMob, double d, int i) {
		this(pathfinderMob, d, i, true);
	}

	public AlienWalkGoal(AlienEntity pathfinderMob, double d, int i, boolean bl) {
		this.mob = pathfinderMob;
		this.speedModifier = d;
		this.interval = i;
		this.checkNoActionTime = bl;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		Vec3 vec3;
		if (this.mob.isVehicle())
			return false;
		if (this.mob.getDeltaMovement().horizontalDistance() >= 0.000000001)
			return false;
		if (!this.forceTrigger) {
			if (this.checkNoActionTime && this.mob.getNoActionTime() >= 100) {
				return false;
			}
			if (this.mob.getRandom().nextInt(RandomStrollGoal.reducedTickDelay(this.interval)) != 0) {
				return false;
			}
		}
		if ((vec3 = this.getPosition()) == null) {
			return false;
		}
		this.wantedX = vec3.x;
		this.wantedY = vec3.y;
		this.wantedZ = vec3.z;
		this.forceTrigger = false;
		return true;
	}

	@Nullable
	protected Vec3 getPosition() {
		return DefaultRandomPos.getPos(this.mob, 10, 7);
	}

	@Override
	public boolean canContinueToUse() {
		return !this.mob.getNavigation().isDone() && !this.mob.isVehicle();
	}

	@Override
	public void start() {
		this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
	}

	@Override
	public void stop() {
		this.mob.getNavigation().stop();
		super.stop();
	}

	/**
	 * Makes task to bypass chance
	 */
	public void trigger() {
		this.forceTrigger = true;
	}

	/**
	 * Changes task random possibility for execution
	 */
	public void setInterval(int newchance) {
		this.interval = newchance;
	}
}
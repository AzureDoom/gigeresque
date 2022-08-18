package mods.cybercat.gigeresque.common.entity.ai.goal.classic;

import org.jetbrains.annotations.Nullable;

import mods.cybercat.gigeresque.common.block.GIgBlocks;
import mods.cybercat.gigeresque.common.entity.impl.AdultAlienEntity;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class FindNestGoal extends MoveToTargetPosGoal {
	private final AdultAlienEntity mob;

	public FindNestGoal(AdultAlienEntity strider) {
		super(strider, 2.5D, 8, 2);
		this.mob = strider;
	}

	@Override
	public BlockPos getTargetPos() {
		return this.targetPos;
	}

	@Override
	public boolean shouldContinue() {
		return mob.hasPassengers() && this.isTargetPos(this.mob.world, this.targetPos);
	}

	@Override
	public boolean canStart() {
		if (this.cooldown > 0) {
			--this.cooldown;
			return false;
		}
		this.cooldown = this.getInterval(this.mob);
		return mob.hasPassengers() && this.findTargetPos();
	}

	@Override
	public boolean shouldResetPath() {
		return this.tryingTime % 20 == 0;
	}

	@Override
	public void stop() {
		super.stop();
	}

	@Override
	public void tick() {
		super.tick();
		World world = this.mob.world;
		BlockPos blockPos = this.mob.getBlockPos();
		BlockPos blockPos2 = this.tweakToProperPos(blockPos, world);
		this.mob.setAttacking(true);
		if (this.hasReached() && blockPos2 != null && mob.hasPassengers()) {
			mob.getFirstPassenger().setInvisible(false);
			mob.removeAllPassengers();
			this.cooldown = 0;
		}
	}

	@Nullable
	private BlockPos tweakToProperPos(BlockPos pos, BlockView world) {
		@SuppressWarnings("unused")
		BlockPos[] blockPoss;
		if (world.getBlockState(pos).isOf(GIgBlocks.NEST_RESIN_WEB_CROSS)) {
			return pos;
		}
		for (BlockPos blockPos : blockPoss = new BlockPos[] { pos.down(), pos.west(), pos.east(), pos.north(),
				pos.south(), pos.down().down() }) {
			if (!world.getBlockState(blockPos).isOf(GIgBlocks.NEST_RESIN_WEB_CROSS))
				continue;
			return blockPos;
		}
		return null;
	}

	@Override
	protected boolean isTargetPos(WorldView world, BlockPos pos) {
		return world.getBlockState(pos).isOf(GIgBlocks.NEST_RESIN_WEB_CROSS);
	}
}
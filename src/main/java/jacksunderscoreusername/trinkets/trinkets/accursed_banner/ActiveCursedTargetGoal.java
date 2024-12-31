package jacksunderscoreusername.trinkets.trinkets.accursed_banner;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class ActiveCursedTargetGoal<T extends LivingEntity> extends TrackTargetGoal {
    protected final int reciprocalChance;
    @Nullable
    protected LivingEntity targetEntity;
    protected TargetPredicate targetPredicate;

    public ActiveCursedTargetGoal(MobEntity mob, boolean checkCanNavigate) {
        this(mob, 10, checkCanNavigate, null);
    }

    public ActiveCursedTargetGoal(
            MobEntity mob,
            int reciprocalChance,
            boolean checkCanNavigate,
            @Nullable TargetPredicate.EntityPredicate targetPredicate
    ) {
        super(mob, false, checkCanNavigate);
        this.reciprocalChance = toGoalTicks(reciprocalChance);
        this.setControls(EnumSet.of(Control.TARGET));
        this.targetPredicate = TargetPredicate.createAttackable().setBaseMaxDistance(this.getFollowRange()).setPredicate(targetPredicate);
    }

    @Override
    public boolean canStart() {
        if (this.reciprocalChance > 0 && this.mob.getRandom().nextInt(this.reciprocalChance) != 0) {
            return false;
        } else {
            this.findClosestTarget();
            return this.targetEntity != null;
        }
    }

    protected Box getSearchBox(double distance) {
        return this.mob.getBoundingBox().expand(distance, distance, distance);
    }

    protected void findClosestTarget() {
        List<Entity> validTargets = this.mob.getWorld().getOtherEntities(null, this.getSearchBox(this.getFollowRange()), entity ->
                entity instanceof LivingEntity livingEntity && livingEntity.getStatusEffect(CursedEffect.CURSED) != null);
        double bestDistance = Double.MAX_VALUE;
        LivingEntity bestTarget = null;
        Vec3d here = new Vec3d(this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
        for (var entity : validTargets) {
            if (entity instanceof LivingEntity livingEntity) {
                double distance = entity.squaredDistanceTo(here);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestTarget = livingEntity;
                }
            }
        }
        this.targetEntity = bestTarget;
    }

    @Override
    public void start() {
        this.mob.setTarget(this.targetEntity);
        super.start();
    }
}

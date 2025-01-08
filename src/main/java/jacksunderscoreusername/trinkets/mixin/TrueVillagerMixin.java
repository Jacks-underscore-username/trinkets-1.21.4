package jacksunderscoreusername.trinkets.mixin;

import jacksunderscoreusername.trinkets.Main;
import jacksunderscoreusername.trinkets.minix_io.TrueVillager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(VillagerEntity.class)
public abstract class TrueVillagerMixin implements TrueVillager {
    @Shadow
    public abstract Brain<VillagerEntity> getBrain();

    @Shadow
    public abstract int getReputation(PlayerEntity player);

    @Unique
    public GlobalPos trinkets_1_21_4_v2$firstHome = null;
    @Unique
    public GlobalPos trinkets_1_21_4_v2$firstJob = null;
    @Unique
    public boolean trinkets_1_21_4_v2$wasChildOrZombie = false;
    @Unique
    public Set<UUID> trinkets_1_21_4_v2$alreadyQuestedPlayers = new HashSet<>();

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        Brain<VillagerEntity> brain = this.getBrain();
        if (trinkets_1_21_4_v2$firstHome == null) {
            Optional<GlobalPos> homeMemory = brain.getOptionalMemory(MemoryModuleType.HOME);
            if (homeMemory != null && homeMemory.isPresent()) {
                trinkets_1_21_4_v2$firstHome = homeMemory.get();
            }
        }
        if (trinkets_1_21_4_v2$firstJob == null) {
            Optional<GlobalPos> jobMemory = brain.getOptionalMemory(MemoryModuleType.JOB_SITE);
            if (jobMemory != null && jobMemory.isPresent()) {
                trinkets_1_21_4_v2$firstJob = jobMemory.get();
            }
        }
        if (((VillagerEntity) (Object) this).isBaby() && !trinkets_1_21_4_v2$wasChildOrZombie) {
            trinkets_1_21_4_v2$wasChildOrZombie = true;
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        if (trinkets_1_21_4_v2$firstHome != null)
            GlobalPos.CODEC
                    .encodeStart(NbtOps.INSTANCE, trinkets_1_21_4_v2$firstHome)
                    .resultOrPartial(Main.LOGGER::error)
                    .ifPresent(data -> nbt.put("trinkets_1_21_4_v2$firstHome", data));
        if (trinkets_1_21_4_v2$firstJob != null)
            GlobalPos.CODEC
                    .encodeStart(NbtOps.INSTANCE, trinkets_1_21_4_v2$firstJob)
                    .resultOrPartial(Main.LOGGER::error)
                    .ifPresent(data -> nbt.put("trinkets_1_21_4_v2$firstJob", data));
        nbt.putBoolean("trinkets_1_21_4_v2$wasChildOrZombie", trinkets_1_21_4_v2$wasChildOrZombie);
        Uuids.SET_CODEC
                .encodeStart(NbtOps.INSTANCE, trinkets_1_21_4_v2$alreadyQuestedPlayers)
                .resultOrPartial(Main.LOGGER::error)
                .ifPresent(data -> nbt.put("trinkets_1_21_4_v2$alreadyQuestedPlayers", data));
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("trinkets_1_21_4_v2$firstHome", NbtElement.COMPOUND_TYPE)) {
            GlobalPos.CODEC
                    .parse(NbtOps.INSTANCE, nbt.get("trinkets_1_21_4_v2$firstHome"))
                    .resultOrPartial(Main.LOGGER::error)
                    .ifPresent(data -> trinkets_1_21_4_v2$firstHome = data);
        }
        if (nbt.contains("trinkets_1_21_4_v2$firstJob", NbtElement.COMPOUND_TYPE)) {
            GlobalPos.CODEC
                    .parse(NbtOps.INSTANCE, nbt.get("trinkets_1_21_4_v2$firstJob"))
                    .resultOrPartial(Main.LOGGER::error)
                    .ifPresent(data -> trinkets_1_21_4_v2$firstJob = data);
        }
        if (nbt.contains("trinkets_1_21_4_v2$wasChildOrZombie")) {
            trinkets_1_21_4_v2$wasChildOrZombie = nbt.getBoolean("trinkets_1_21_4_v2$wasChildOrZombie");
        }
        if (nbt.contains("trinkets_1_21_4_v2$alreadyQuestedPlayers", NbtElement.COMPOUND_TYPE)) {
            Uuids.SET_CODEC
                    .parse(NbtOps.INSTANCE, nbt.get("trinkets_1_21_4_v2$alreadyQuestedPlayers"))
                    .resultOrPartial(Main.LOGGER::error)
                    .ifPresent(data -> trinkets_1_21_4_v2$alreadyQuestedPlayers = data);
        }
    }

    @Inject(method = "initialize", at = @At("TAIL"))
    private void initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData, CallbackInfoReturnable<EntityData> cir) {
        if (spawnReason.equals(SpawnReason.CONVERSION))
            trinkets_1_21_4_v2$wasChildOrZombie = true;
    }

    @Unique
    private HashMap<UUID, Integer> trinkets_1_21_4_v2$playerMessageTimes = new HashMap<>();

    @Unique
    public boolean trinkets_1_21_4_v2$canStartQuest(PlayerEntity player) {
        Brain<VillagerEntity> brain = this.getBrain();
        Optional<GlobalPos> homeMemory = brain.getOptionalMemory(MemoryModuleType.HOME);
        Optional<GlobalPos> jobMemory = brain.getOptionalMemory(MemoryModuleType.JOB_SITE);
        Random random = new Random(player.getUuid().hashCode() + ((VillagerEntity) (Object) this).getUuid().hashCode());
        int neededReputation = random.nextInt(10, 30);
        int playerReputation = this.getReputation(player);
        boolean isHero = player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE);
        if (isHero) playerReputation += 10;

        boolean willGiveQuestsToPlayer = random.nextInt(1, 3) <= (isHero ? 1 : 2);
        boolean hasEnoughReputation = playerReputation >= neededReputation;
        boolean hasNotQuested = !trinkets_1_21_4_v2$alreadyQuestedPlayers.contains(player.getUuid());
        boolean wasNeverChildOrZombie = !trinkets_1_21_4_v2$wasChildOrZombie;
        boolean sameHome = trinkets_1_21_4_v2$firstHome != null && homeMemory != null && homeMemory.isPresent() && homeMemory.get().equals(trinkets_1_21_4_v2$firstHome);
        boolean sameJob = trinkets_1_21_4_v2$firstJob != null && jobMemory != null && jobMemory.isPresent() && jobMemory.get().equals(trinkets_1_21_4_v2$firstJob);
        boolean result = willGiveQuestsToPlayer && hasEnoughReputation && hasNotQuested && wasNeverChildOrZombie && sameHome && sameJob;
        if (result) {
            trinkets_1_21_4_v2$alreadyQuestedPlayers.add(player.getUuid());
        } else if (!player.isSneaking() && (!trinkets_1_21_4_v2$playerMessageTimes.containsKey(player.getUuid())) || System.currentTimeMillis() / 1000 - trinkets_1_21_4_v2$playerMessageTimes.get(player.getUuid()) > 2) {
            trinkets_1_21_4_v2$playerMessageTimes.put(player.getUuid(), (int) (System.currentTimeMillis() / 1000));
            Text message = null;
            if (!sameHome || !sameJob || !wasNeverChildOrZombie)
                message = Text.literal("This villager will not give quests");
            else if (!willGiveQuestsToPlayer)
                message = Text.literal("This villager will not give you quests");
            else if (!hasNotQuested)
                message = Text.literal("You have already taken a quest from this villager");
            else if (!hasEnoughReputation)
                message = Text.literal("You do not have enough reputation to start a quest (" + playerReputation + "/" + neededReputation + ")");
            player.sendMessage(message, false);
            Entity villager = ((Entity) (Object) this);
            player.getWorld().playSound(villager, villager.getBlockPos(), SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL, 1.0F, 1.0F);
        }
        return result;
    }
}

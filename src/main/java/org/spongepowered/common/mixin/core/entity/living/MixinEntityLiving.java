/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.core.entity.living;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Agent;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.entity.EntityLeashEvent;
import org.spongepowered.api.event.entity.EntityUnleashEvent;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.Sponge;

import static com.google.common.base.Preconditions.checkState;
import static org.spongepowered.api.data.DataQuery.of;

@NonnullByDefault
@Mixin(EntityLiving.class)
public abstract class MixinEntityLiving extends MixinEntityLivingBase implements Agent {

    @Shadow private boolean canPickUpLoot;
    @Shadow public abstract boolean isAIDisabled();
    @Shadow protected abstract void setNoAI(boolean p_94061_1_);
    @Shadow public abstract net.minecraft.entity.Entity getLeashedToEntity();
    @Shadow public abstract void setLeashedToEntity(net.minecraft.entity.Entity entityIn, boolean sendAttachNotification);
    @Shadow protected EntityAITasks tasks;

    public boolean isAiEnabled() {
        return !isAIDisabled();
    }

    public void setAiEnabled(boolean aiEnabled) {
        setNoAI(!aiEnabled);
    }

    public boolean isLeashed() {
        return getLeashedToEntity() != null;
    }

    public void setLeashed(boolean leashed) {
        throw new UnsupportedOperationException(); // TODO
    }

    public Optional<Entity> getLeashHolder() {
        return Optional.fromNullable((Entity) getLeashedToEntity());
    }

    public void setLeashHolder(@Nullable Entity entity) {
        setLeashedToEntity((net.minecraft.entity.Entity) entity, true);
    }

    public boolean getCanPickupItems() {
        return this.canPickUpLoot;
    }

    public void setCanPickupItems(boolean canPickupItems) {
        this.canPickUpLoot = canPickupItems;
    }

    @Inject(method = "interactFirst", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLiving;setLeashedToEntity(Lnet/minecraft/entity/Entity;Z)V"), locals = LocalCapture.CAPTURE_FAILEXCEPTION, cancellable = true)
    public void callLeashEvent(EntityPlayer playerIn, CallbackInfoReturnable<Boolean> ci, ItemStack itemstack) {
        final EntityLeashEvent event = SpongeEventFactory.createEntityLeash(Sponge.getGame(), this, (Player)playerIn);
        Sponge.getGame().getEventManager().post(event);
        if(event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "clearLeashed", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLiving;isLeashed:Z", opcode = Opcodes.PUTFIELD), cancellable = true)
    public void callUnleashEvent(boolean sendPacket, boolean dropLead, CallbackInfo ci) {
        final EntityUnleashEvent event = SpongeEventFactory.createEntityUnleash(Sponge.getGame(), this, (Entity)getLeashedToEntity());
        Sponge.getGame().getEventManager().post(event);
        if(event.isCancelled()) {
            ci.cancel();
        }
    }

    @Override
    public DataContainer toContainer() {
        DataContainer container = super.toContainer();
        container.set(of("AiEnabled"), !this.isAIDisabled());
        return container;
    }

    private List<EntityAIBase> getTasks() {
        List<EntityAIBase> results = new ArrayList<EntityAIBase>();

        try {
            Field taskEntriesPrivateField = tasks.getClass().getDeclaredField("taskEntries");
            taskEntriesPrivateField.setAccessible(true);
            List taskEntries = (List) taskEntriesPrivateField.get(tasks);
            for (Object entry: taskEntries) {
                Field actionPrivateField = entry.getClass().getDeclaredField("action");
                actionPrivateField.setAccessible(true);
                EntityAIBase taskEntry = (EntityAIBase)actionPrivateField.get(entry);
                results.add(taskEntry);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        return results;
    }

    private Optional<EntityAIBase> findTaskByName(String taskName) {
        checkState(EXTERNAL_NAME_TO_TASK_CLASS.containsKey(taskName),
            String.format("No task class found for external name %s", taskName));

        String taskClassName = EXTERNAL_NAME_TO_TASK_CLASS.get(taskName);

        for (EntityAIBase task: getTasks()) {
            if (task.getClass().getName().equals(taskClassName)) {
                return Optional.of(task);
            }
        }

        return Optional.absent();
    }

    @Override public List<String> getTaskNames() {
        List<String> names = new ArrayList<String>();

        for(EntityAIBase task: getTasks()) {
            names.add(externalNameForTaskEntry(task));
        }

        return names;
    }

    @Override public void startTask(String taskName) {
        lookupTask(taskName).startExecuting();
    }

    @Override public void resetTask(String taskName) {
        lookupTask(taskName).resetTask();
    }

    private EntityAIBase lookupTask(String taskName) {
        Optional<EntityAIBase> maybeTask = findTaskByName(taskName);
        checkState(maybeTask.isPresent(), String.format("No task found for name %s in class %s",
            taskName, this.getClass().getName()));
        return maybeTask.get();
    }

    private String externalNameForTaskEntry(EntityAIBase taskEntry) {
        checkState(TASK_CLASS_TO_EXTERNAL_NAME.containsKey(taskEntry.getClass().getName()),
            String.format("No external name found for class %s", taskEntry.getClass().getName()));
        return TASK_CLASS_TO_EXTERNAL_NAME.get(taskEntry.getClass().getName());
    }

    private static final Map<String, String> EXTERNAL_NAME_TO_TASK_CLASS =
        new LinkedHashMap<String, String>();

    private static final Map<String, String> TASK_CLASS_TO_EXTERNAL_NAME =
        new LinkedHashMap<String, String>();

    static {
        EXTERNAL_NAME_TO_TASK_CLASS.put("attack_with_arrow", "net.minecraft.entity.ai.EntityAIArrowAttack");
        EXTERNAL_NAME_TO_TASK_CLASS.put("attack_on_collide", "net.minecraft.entity.ai.EntityAIAttackOnCollide");
        EXTERNAL_NAME_TO_TASK_CLASS.put("avoid", "net.minecraft.entity.ai.EntityAIAvoidEntity");
        EXTERNAL_NAME_TO_TASK_CLASS.put("beg", "net.minecraft.entity.ai.EntityAIBeg");
        EXTERNAL_NAME_TO_TASK_CLASS.put("break_door", "net.minecraft.entity.ai.EntityAIBreakDoor");
        EXTERNAL_NAME_TO_TASK_CLASS.put("player_control", "net.minecraft.entity.ai.EntityAIControlledByPlayer");
        EXTERNAL_NAME_TO_TASK_CLASS.put("swell_creeper", "net.minecraft.entity.ai.EntityAICreeperSwell");
        EXTERNAL_NAME_TO_TASK_CLASS.put("defend_village", "net.minecraft.entity.ai.EntityAIDefendVillage");
        EXTERNAL_NAME_TO_TASK_CLASS.put("use_door", "net.minecraft.entity.ai.EntityAIDoorInteract");
        EXTERNAL_NAME_TO_TASK_CLASS.put("eat_grass", "net.minecraft.entity.ai.EntityAIEatGrass");
        EXTERNAL_NAME_TO_TASK_CLASS.put("find_nearest_thing", "net.minecraft.entity.ai.EntityAIFindEntityNearest");
        EXTERNAL_NAME_TO_TASK_CLASS.put("find_nearest_thing_to_player", "net.minecraft.entity.ai.EntityAIFindEntityNearestPlayer");
        EXTERNAL_NAME_TO_TASK_CLASS.put("flee_sun", "net.minecraft.entity.ai.EntityAIFleeSun");
        EXTERNAL_NAME_TO_TASK_CLASS.put("follow_golem", "net.minecraft.entity.ai.EntityAIFollowGolem");
        EXTERNAL_NAME_TO_TASK_CLASS.put("follow_owner", "net.minecraft.entity.ai.EntityAIFollowOwner");
        EXTERNAL_NAME_TO_TASK_CLASS.put("follow_parent", "net.minecraft.entity.ai.EntityAIFollowParent");
        EXTERNAL_NAME_TO_TASK_CLASS.put("harvest_farmland", "net.minecraft.entity.ai.EntityAIHarvestFarmland");
        EXTERNAL_NAME_TO_TASK_CLASS.put("hurt_by_target", "net.minecraft.entity.ai.EntityAIHurtByTarget");
        EXTERNAL_NAME_TO_TASK_CLASS.put("leap_at_target", "net.minecraft.entity.ai.EntityAILeapAtTarget");
        EXTERNAL_NAME_TO_TASK_CLASS.put("look_at_trade_player", "net.minecraft.entity.ai.EntityAILookAtTradePlayer");
        EXTERNAL_NAME_TO_TASK_CLASS.put("look_at_villager", "net.minecraft.entity.ai.EntityAILookAtVillager");
        EXTERNAL_NAME_TO_TASK_CLASS.put("look_idle", "net.minecraft.entity.ai.EntityAILookIdle");
        EXTERNAL_NAME_TO_TASK_CLASS.put("mate", "net.minecraft.entity.ai.EntityAIMate");
        EXTERNAL_NAME_TO_TASK_CLASS.put("move_indoor", "net.minecraft.entity.ai.EntityAIMoveIndoors");
        EXTERNAL_NAME_TO_TASK_CLASS.put("move_through_village", "net.minecraft.entity.ai.EntityAIMoveThroughVillage");
        EXTERNAL_NAME_TO_TASK_CLASS.put("move_to_block", "net.minecraft.entity.ai.EntityAIMoveToBlock");
        EXTERNAL_NAME_TO_TASK_CLASS.put("move_towards_restriction", "net.minecraft.entity.ai.EntityAIMoveTowardsRestriction");
        EXTERNAL_NAME_TO_TASK_CLASS.put("move_towards_target", "net.minecraft.entity.ai.EntityAIMoveTowardsTarget");
        EXTERNAL_NAME_TO_TASK_CLASS.put("attack_nearest_target", "net.minecraft.entity.ai.EntityAINearestAttackableTarget");
        EXTERNAL_NAME_TO_TASK_CLASS.put("ocelot_attack", "net.minecraft.entity.ai.EntityAIOcelotAttack");
        EXTERNAL_NAME_TO_TASK_CLASS.put("ocelot_sit", "net.minecraft.entity.ai.EntityAIOcelotSit");
        EXTERNAL_NAME_TO_TASK_CLASS.put("open_door", "net.minecraft.entity.ai.EntityAIOpenDoor");
        EXTERNAL_NAME_TO_TASK_CLASS.put("owner_get_hurt_by_target", "net.minecraft.entity.ai.EntityAIOwnerHurtByTarget");
        EXTERNAL_NAME_TO_TASK_CLASS.put("owner_hurt_target", "net.minecraft.entity.ai.EntityAIOwnerHurtTarget");
        EXTERNAL_NAME_TO_TASK_CLASS.put("panic", "net.minecraft.entity.ai.EntityAIPanic");
        EXTERNAL_NAME_TO_TASK_CLASS.put("play", "net.minecraft.entity.ai.EntityAIPlay");
        EXTERNAL_NAME_TO_TASK_CLASS.put("avoid_doors", "net.minecraft.entity.ai.EntityAIRestrictOpenDoor");
        EXTERNAL_NAME_TO_TASK_CLASS.put("avoid_sun", "net.minecraft.entity.ai.EntityAIRestrictSun");
        EXTERNAL_NAME_TO_TASK_CLASS.put("run_around_like_crazy", "net.minecraft.entity.ai.EntityAIRunAroundLikeCrazy");
        EXTERNAL_NAME_TO_TASK_CLASS.put("sit", "net.minecraft.entity.ai.EntityAISit");
        EXTERNAL_NAME_TO_TASK_CLASS.put("swim", "net.minecraft.entity.ai.EntityAISwimming");
        EXTERNAL_NAME_TO_TASK_CLASS.put("target_untamed", "net.minecraft.entity.ai.EntityAITargetNonTamed");
        EXTERNAL_NAME_TO_TASK_CLASS.put("tempt", "net.minecraft.entity.ai.EntityAITempt");
        EXTERNAL_NAME_TO_TASK_CLASS.put("trade_with_player", "net.minecraft.entity.ai.EntityAITradePlayer");
        EXTERNAL_NAME_TO_TASK_CLASS.put("interact_with_other_villagers", "net.minecraft.entity.ai.EntityAIVillagerInteract");
        EXTERNAL_NAME_TO_TASK_CLASS.put("mate_with_other_villager", "net.minecraft.entity.ai.EntityAIVillagerMate");
        EXTERNAL_NAME_TO_TASK_CLASS.put("wander", "net.minecraft.entity.ai.EntityAIWander");
        EXTERNAL_NAME_TO_TASK_CLASS.put("watch_closest", "net.minecraft.entity.ai.EntityAIWatchClosest");
        EXTERNAL_NAME_TO_TASK_CLASS.put("watch_closest_2", "net.minecraft.entity.ai.EntityAIWatchClosest2");

        EXTERNAL_NAME_TO_TASK_CLASS.put("spider_attack", "net.minecraft.entity.monster.EntitySpider$AISpiderAttack");

        EXTERNAL_NAME_TO_TASK_CLASS.put("slime_float", "net.minecraft.entity.monster.EntitySlime$AISlimeFloat");
        EXTERNAL_NAME_TO_TASK_CLASS.put("slime_attack", "net.minecraft.entity.monster.EntitySlime$AISlimeAttack");
        EXTERNAL_NAME_TO_TASK_CLASS.put("slime_face_random", "net.minecraft.entity.monster.EntitySlime$AISlimeFaceRandom");
        EXTERNAL_NAME_TO_TASK_CLASS.put("slime_hop", "net.minecraft.entity.monster.EntitySlime$AISlimeHop");

        EXTERNAL_NAME_TO_TASK_CLASS.put("ghast_fly", "net.minecraft.entity.monster.EntityGhast$AIRandomFly");
        EXTERNAL_NAME_TO_TASK_CLASS.put("ghast_look_around", "net.minecraft.entity.monster.EntityGhast$AILookAround");
        EXTERNAL_NAME_TO_TASK_CLASS.put("ghast_fireball_attack", "net.minecraft.entity.monster.EntityGhast$AIFireballAttack");

        EXTERNAL_NAME_TO_TASK_CLASS.put("enderman_place_block", "net.minecraft.entity.monster.EntityEnderman$AIPlaceBlock");
        EXTERNAL_NAME_TO_TASK_CLASS.put("enderman_take_block", "net.minecraft.entity.monster.EntityEnderman$AITakeBlock");

        EXTERNAL_NAME_TO_TASK_CLASS.put("summon_silverfish", "net.minecraft.entity.monster.EntitySilverfish$AISummonSilverfish");
        EXTERNAL_NAME_TO_TASK_CLASS.put("silverfish_hide_in_stone", "net.minecraft.entity.monster.EntitySilverfish$AIHideInStone");

        EXTERNAL_NAME_TO_TASK_CLASS.put("blaze_fireball_attack", "net.minecraft.entity.monster.EntityBlaze$AIFireballAttack");

        EXTERNAL_NAME_TO_TASK_CLASS.put("squid_move_random", "net.minecraft.entity.passive.EntitySquid$AIMoveRandom");

        EXTERNAL_NAME_TO_TASK_CLASS.put("guardian_attack", "net.minecraft.entity.monster.EntityGuardian$AIGuardianAttack");

        EXTERNAL_NAME_TO_TASK_CLASS.put("rabbit_panic", "net.minecraft.entity.passive.EntityRabbit$AIPanic");
        EXTERNAL_NAME_TO_TASK_CLASS.put("rabbit_raid_farm", "net.minecraft.entity.passive.EntityRabbit$AIRaidFarm");
        EXTERNAL_NAME_TO_TASK_CLASS.put("rabbit_avoid_wolves", "net.minecraft.entity.passive.EntityRabbit$AIAvoidEntity");

        for (Map.Entry<String,String> entry: EXTERNAL_NAME_TO_TASK_CLASS.entrySet()) {
            TASK_CLASS_TO_EXTERNAL_NAME.put(entry.getValue(), entry.getKey());
        }
    }
}

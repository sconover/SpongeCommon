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
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIFollowOwner;
import net.minecraft.entity.ai.EntityAILeapAtTarget;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAIOcelotAttack;
import net.minecraft.entity.ai.EntityAIOcelotSit;
import net.minecraft.entity.ai.EntityAISit;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
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

        Class taskClass = EXTERNAL_NAME_TO_TASK_CLASS.get(taskName);

        for (EntityAIBase task: getTasks()) {
            if (task.getClass().equals(taskClass)) {
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
        checkState(TASK_CLASS_TO_EXTERNAL_NAME.containsKey(taskEntry.getClass()),
            String.format("No external name found for class %s", taskEntry.getClass().getName()));
        return TASK_CLASS_TO_EXTERNAL_NAME.get(taskEntry.getClass());
    }

    private static final Map<String, Class> EXTERNAL_NAME_TO_TASK_CLASS =
        new LinkedHashMap<String, Class>();

    private static final Map<Class, String> TASK_CLASS_TO_EXTERNAL_NAME =
        new LinkedHashMap<Class, String>();

    static {
        EXTERNAL_NAME_TO_TASK_CLASS.put("swim", EntityAISwimming.class);
        EXTERNAL_NAME_TO_TASK_CLASS.put("sit", EntityAISit.class);
        EXTERNAL_NAME_TO_TASK_CLASS.put("tempt", EntityAITempt.class);
        EXTERNAL_NAME_TO_TASK_CLASS.put("follow_owner", EntityAIFollowOwner.class);
        EXTERNAL_NAME_TO_TASK_CLASS.put("ocelot_sit", EntityAIOcelotSit.class);
        EXTERNAL_NAME_TO_TASK_CLASS.put("leap_at_target", EntityAILeapAtTarget.class);
        EXTERNAL_NAME_TO_TASK_CLASS.put("ocelot_attack", EntityAIOcelotAttack.class);
        EXTERNAL_NAME_TO_TASK_CLASS.put("mate", EntityAIMate.class);
        EXTERNAL_NAME_TO_TASK_CLASS.put("wander", EntityAIWander.class);
        EXTERNAL_NAME_TO_TASK_CLASS.put("watch_closest", EntityAIWatchClosest.class);
        EXTERNAL_NAME_TO_TASK_CLASS.put("avoid", EntityAIAvoidEntity.class);

        for (Map.Entry<String,Class> entry: EXTERNAL_NAME_TO_TASK_CLASS.entrySet()) {
            TASK_CLASS_TO_EXTERNAL_NAME.put(entry.getValue(), entry.getKey());
        }
    }
}

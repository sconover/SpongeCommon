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
package org.spongepowered.common.mixin.core.entity.projectile;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.base.Optional;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.item.EntityExpBottle;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.entity.projectile.EntitySnowball;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.entity.projectile.source.ProjectileSource;
import org.spongepowered.api.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.entity.living.human.EntityHuman;
import org.spongepowered.common.entity.projectile.ProjectileLauncher;

@Mixin({EntityBlaze.class, EntityGhast.class, EntityHuman.class, EntityPlayerMP.class, EntitySkeleton.class, EntityWitch.class, EntityWither.class})
public abstract class MixinEntityProjectileSource extends EntityLivingBase implements ProjectileSource, ProjectileLauncher.ProjectileCreator {

    public MixinEntityProjectileSource() {
        super(null);
    }

    @Override
    public <T extends Projectile> Optional<T> launchProjectile(Class<T> projectileClass) {
        return this.launchProjectile(projectileClass, null);
    }

    @Override
    public <T extends Projectile> Optional<T> launchProjectile(Class<T> projectileClass, Vector3d velocity) {
        Vector3d position = new Vector3d(this.posX, this.getEntityBoundingBox().minY + (double) (this.height / 2.0F), this.posZ);
        return ProjectileLauncher.launchProjectile(this, projectileClass, position, (World) this.worldObj, velocity, this);
    }

    private boolean wasCreated = false;

    @Override
    public Optional<Projectile> createProjectile(EntityType projectileType, Vector3d position) {
        this.wasCreated = true;
        if (projectileType == EntityTypes.ARROW) {
            return Optional.of((Projectile) new EntityArrow(this.worldObj, this, 1));
        } else if (projectileType == EntityTypes.EGG) {
            return Optional.of((Projectile) new EntityEgg(this.worldObj, this));
        } else if (projectileType == EntityTypes.ENDER_PEARL) {
            return Optional.of((Projectile) new EntityEnderPearl(this.worldObj, this));
        } else if (projectileType == EntityTypes.FIREBALL) {
            // TODO
        } else if (projectileType == EntityTypes.WITHER_SKULL) {
            // TODO
        } else if (projectileType == EntityTypes.EYE_OF_ENDER) {
            // TODO
        } else if (projectileType == EntityTypes.FIREWORK) {
            // TODO
        } else if (projectileType == EntityTypes.FISHING_HOOK) {
            // TODO
        } else if (projectileType == EntityTypes.SNOWBALL) {
            return Optional.of((Projectile) new EntitySnowball(this.worldObj, this));
        } else if (projectileType == EntityTypes.THROWN_EXP_BOTTLE) {
            return Optional.of((Projectile) new EntityExpBottle(this.worldObj, this));
        } else if (projectileType == EntityTypes.SPLASH_POTION) {
            // TODO
        }
        this.wasCreated = false;
        return null;
    }

    @Override
    public Vector3d getVelocity(Projectile projectile) {
        if (this.wasCreated) {
            this.wasCreated = false;
            return null; // Already calculated from createProjectile
        }
        // TODO calculate velocity from rotation of entity
        return null;
    }

    @Override
    public Boolean spawnProjectile(Projectile projectile) {
        return null;
    }
}

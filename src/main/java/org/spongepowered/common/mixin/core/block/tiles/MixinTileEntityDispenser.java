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
package org.spongepowered.common.mixin.core.block.tiles;

import static com.google.common.base.Preconditions.checkNotNull;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.base.Optional;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockSourceImpl;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.EnumFacing;
import org.spongepowered.api.block.tileentity.TileEntityType;
import org.spongepowered.api.block.tileentity.TileEntityTypes;
import org.spongepowered.api.block.tileentity.carrier.Dispenser;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.entity.projectile.ProjectileLauncher;
import org.spongepowered.common.util.VecHelper;

@NonnullByDefault
@Mixin(TileEntityDispenser.class)
public abstract class MixinTileEntityDispenser extends MixinTileEntityLockable implements Dispenser, ProjectileLauncher.ProjectileCreator {

    @Override
    public TileEntityType getType() {
        return TileEntityTypes.DISPENSER;
    }

    @Override
    public <T extends Projectile> Optional<T> launchProjectile(Class<T> projectileClass) {
        return launchProjectile(projectileClass, null);
    }

    @Override
    public <T extends Projectile> Optional<T> launchProjectile(Class<T> projectileClass, Vector3d velocity) {
        return ProjectileLauncher.launchProjectile(this, checkNotNull(projectileClass, "projectileClass"), VecHelper.toVector3d(this.getPos()),
                (World) this.worldObj, velocity, this);
    }

    @Override
    public Optional<Projectile> createProjectile(EntityType projectileType, Vector3d position) {
        if (projectileType == EntityTypes.ARROW) {
            return Optional.fromNullable(launchItem(Items.arrow));
        } else if (projectileType == EntityTypes.EGG) {
            return Optional.fromNullable(launchItem(Items.egg));
        } else if (projectileType == EntityTypes.ENDER_PEARL) {
            // TODO
        } else if (projectileType == EntityTypes.FIREBALL) {
            return Optional.fromNullable(launchItem(Items.fire_charge));
        } else if (projectileType == EntityTypes.WITHER_SKULL) {
            // TODO
        } else if (projectileType == EntityTypes.EYE_OF_ENDER) {
            // TODO
        } else if (projectileType == EntityTypes.FIREWORK) {
            return Optional.fromNullable(launchItem(Items.fireworks));
        } else if (projectileType == EntityTypes.FISHING_HOOK) {
            // TODO
        } else if (projectileType == EntityTypes.SNOWBALL) {
            return Optional.fromNullable(launchItem(Items.snowball));
        } else if (projectileType == EntityTypes.THROWN_EXP_BOTTLE) {
            return Optional.fromNullable(launchItem(Items.experience_bottle));
        } else if (projectileType == EntityTypes.SPLASH_POTION) {
            // TODO
        }
        return null;
    }

    private boolean wasDispensed = false;

    private Projectile launchItem(Item item) {
        BehaviorDefaultDispenseItem behavior = (BehaviorDefaultDispenseItem) BlockDispenser.dispenseBehaviorRegistry.getObject(item);
        int numEntities = this.worldObj.loadedEntityList.size();
        behavior.dispense(new BlockSourceImpl(this.worldObj, this.getPos()), new ItemStack(item));
        for (int i = this.worldObj.loadedEntityList.size() - 1; i >= numEntities; i--) {
            if (this.worldObj.loadedEntityList.get(i) instanceof Projectile) {
                this.wasDispensed = true;
                return (Projectile) this.worldObj.loadedEntityList.get(i);
            }
        }
        return null;
    }

    @Override
    public Vector3d getVelocity(Projectile projectile) {
        if (this.wasDispensed) {
            return null;
        }
        IBlockState state = this.worldObj.getBlockState(this.getPos());
        EnumFacing enumfacing = BlockDispenser.getFacing(state.getBlock().getMetaFromState(state));
        return new Vector3d(enumfacing.getFrontOffsetX(), enumfacing.getFrontOffsetY() + 0.1F, enumfacing.getFrontOffsetZ());
    }

    @Override
    public Boolean spawnProjectile(Projectile projectile) {
        if (this.wasDispensed) {
            this.wasDispensed = false;
            return true;
        }
        return null;
    }

}

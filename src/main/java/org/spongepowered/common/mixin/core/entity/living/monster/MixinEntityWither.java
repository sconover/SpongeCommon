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
package org.spongepowered.common.mixin.core.entity.living.monster;

import com.flowpowered.math.vector.Vector3d;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.EntityWither;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.monster.Wither;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.entity.projectile.source.ProjectileSource;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.util.SpongeHooks;

import java.util.ArrayList;
import java.util.List;

@NonnullByDefault
@Mixin(EntityWither.class)
@Implements(@Interface(iface = Wither.class, prefix = "wither$"))
public abstract class MixinEntityWither extends MixinEntityMob {

    @Shadow public abstract int getInvulTime();
    @Shadow public abstract void setInvulTime(int invulnerableTicks);
    @Shadow public abstract int getWatchedTargetId(int targetId);

    public int getInvulnerableTicks() {
        return this.getInvulTime();
    }

    public void setInvulnerableTicks(int invulnerableTicks) {
        this.setInvulTime(invulnerableTicks);
    }

    public List<Living> getTargets() {
        List<Living> watchedTargets = new ArrayList<Living>();
        for (int i = 0; i < 2; ++i) {
            int j = this.getWatchedTargetId(i + 1);
            Entity entity = null;

            if (j > 0) {
                entity = this.worldObj.getEntityByID(j);
            }
            if (entity != null) {
                watchedTargets.add((Living) entity);
            }
        }
        return watchedTargets;
    }

    public <T extends Projectile> T skeleton$launchProjectile(Class<T> projectileClass) {
        return skeleton$launchProjectile(projectileClass, null);
    }

    public <T extends Projectile> T skeleton$launchProjectile(Class<T> projectileClass, Vector3d velocity) {
        double x = this.posX ;
        double y = getEntityBoundingBox().minY + (double)(this.height / 3.0F) - this.posY;
        double z = this.posZ;

        return (T) SpongeHooks.launchProjectile(getEntityWorld(), new Vector3d(x, y, z), ((ProjectileSource) this), projectileClass, velocity);
    }

}

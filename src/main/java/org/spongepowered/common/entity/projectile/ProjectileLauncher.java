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
package org.spongepowered.common.entity.projectile;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.base.Optional;
import org.spongepowered.api.data.manipulator.entity.VelocityData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.entity.projectile.source.ProjectileSource;
import org.spongepowered.common.Sponge;

public class ProjectileLauncher {

    public interface ProjectileCreator {

        // Return null to use default
        Optional<Projectile> createProjectile(EntityType projectileType, Vector3d position);

        // Return null to not set velocity
        Vector3d getVelocity(Projectile projectile);

        // Return null to use default
        Boolean spawnProjectile(Projectile projectile);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Projectile> Optional<T> launchProjectile(ProjectileSource source, Class<T> projectileClass, Vector3d position,
            org.spongepowered.api.world.World world, Vector3d velocity, ProjectileCreator creator) {
        Optional<EntityType> optType = Sponge.getSpongeRegistry().getEntity(projectileClass);
        if (!optType.isPresent()) {
            throw new IllegalArgumentException("Projectile class " + projectileClass + " is not a known projectile entity class");
        }
        Optional<Projectile> optProjectile = creator.createProjectile(optType.get(), position);
        if (optProjectile == null) {
            Optional<Entity> optEntity = world.createEntity(optType.get(), position);
            if (!optEntity.isPresent() || !(optEntity.get() instanceof Projectile)) {
                optProjectile = Optional.absent();
            } else {
                optProjectile = (Optional<Projectile>) (Object) optEntity;
            }
        }
        if (!optProjectile.isPresent()) {
            return Optional.absent();
        }
        Projectile projectile = optProjectile.get();
        projectile.setShooter(source);
        if (velocity == null) {
            velocity = creator.getVelocity(projectile);
        }
        if (velocity != null) {
            VelocityData velocityData = projectile.getOrCreate(VelocityData.class).get();
            velocityData.setVelocity(velocity);
            projectile.offer(velocityData);
        }
        Boolean spawned = creator.spawnProjectile(projectile);
        if (spawned == null) {
            spawned = world.spawnEntity(projectile);
        }
        if (!spawned) {
            Sponge.getLogger().warn("Unable to spawn projectile in world, returning projectile regardless.");
        }
        return Optional.of((T) projectile);
    }

}

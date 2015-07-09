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
package org.spongepowered.common.data.processor.entity;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.base.Optional;
import net.minecraft.entity.Entity;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataPriority;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.entity.VelocityData;
import org.spongepowered.api.service.persistence.InvalidDataException;
import org.spongepowered.common.data.DataTransactionBuilder;
import org.spongepowered.common.data.SpongeDataProcessor;
import org.spongepowered.common.data.manipulator.entity.SpongeVelocityData;

public class SpongeVelocityProcessor implements SpongeDataProcessor<VelocityData> {

    @Override
    public Optional<VelocityData> build(DataView container) throws InvalidDataException {
        Optional<Double> optX = container.getDouble(SpongeVelocityData.X_VELOCITY);
        if (!optX.isPresent()) {
            return Optional.absent();
        }
        Optional<Double> optY = container.getDouble(SpongeVelocityData.Y_VELOCITY);
        if (!optY.isPresent()) {
            return Optional.absent();
        }
        Optional<Double> optZ = container.getDouble(SpongeVelocityData.Z_VELOCITY);
        if (!optY.isPresent()) {
            return Optional.absent();
        }
        return Optional.of(new SpongeVelocityData().setVelocity(new Vector3d(optX.get(), optY.get(), optZ.get())));
    }

    @Override
    public VelocityData create() {
        return new SpongeVelocityData();
    }

    @Override
    public Optional<VelocityData> createFrom(DataHolder dataHolder) {
        Optional<VelocityData> opData = getFrom(dataHolder);
        if (opData.isPresent()) {
            return opData;
        }
        return Optional.of(create());
    }

    @Override
    public Optional<VelocityData> getFrom(DataHolder dataHolder) {
        if (dataHolder instanceof Entity) {
            double mX = ((Entity) dataHolder).motionX;
            double mY = ((Entity) dataHolder).motionY;
            double mZ = ((Entity) dataHolder).motionZ;
            if (mX == 0 && mY == 0 && mZ == 0) {
                return Optional.absent();
            }
            return Optional.of(create().setVelocity(new Vector3d(mX, mY, mZ)));
        }
        return Optional.absent();
    }

    @Override
    public Optional<VelocityData> fillData(DataHolder dataHolder, VelocityData manipulator, DataPriority priority) {
        return Optional.absent(); // TODO
    }

    @Override
    public DataTransactionResult setData(DataHolder dataHolder, VelocityData manipulator, DataPriority priority) {
        Vector3d vel = manipulator.getVelocity();
        if (dataHolder instanceof Entity) {
            ((Entity) dataHolder).motionX = vel.getX();
            ((Entity) dataHolder).motionY = vel.getY();
            ((Entity) dataHolder).motionZ = vel.getZ();
            return DataTransactionBuilder.successReplaceData(manipulator);
        }
        return DataTransactionBuilder.fail(manipulator);
    }

    @Override
    public boolean remove(DataHolder dataHolder) {
        if (dataHolder instanceof Entity) {
            ((Entity) dataHolder).motionX = 0;
            ((Entity) dataHolder).motionY = 0;
            ((Entity) dataHolder).motionZ = 0;
            return true;
        }
        return false;
    }

}

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
package org.spongepowered.common.world.extent;

import com.flowpowered.math.vector.Vector2i;
import org.spongepowered.api.util.PositionOutOfBoundsException;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.extent.BiomeArea;
import org.spongepowered.api.world.extent.ImmutableBiomeArea;
import org.spongepowered.api.world.extent.MutableBiomeArea;
import org.spongepowered.api.world.extent.StorageType;
import org.spongepowered.common.util.VecHelper;

public abstract class AbstractBiomeView<A extends BiomeArea> implements BiomeArea {

    protected final A area;
    protected final Vector2i min;
    protected final Vector2i max;
    protected final Vector2i size;

    public AbstractBiomeView(A area, Vector2i min, Vector2i max) {
        this.area = area;
        this.min = min;
        this.max = max;
        this.size = max.sub(min).add(Vector2i.ONE);
    }

    @Override
    public Vector2i getBiomeMin() {
        return min;
    }

    @Override
    public Vector2i getBiomeMax() {
        return max;
    }

    @Override
    public Vector2i getBiomeSize() {
        return size;
    }

    @Override
    public boolean containsBiome(Vector2i position) {
        return containsBiome(position.getX(), position.getY());
    }

    @Override
    public boolean containsBiome(int x, int z) {
        return VecHelper.inBounds(x, z, min, max);
    }

    protected final void checkRange(int x, int z) {
        if (!VecHelper.inBounds(x, z, this.min, this.max)) {
            throw new PositionOutOfBoundsException(new Vector2i(x, z), this.min, this.max);
        }
    }

    @Override
    public BiomeType getBiome(Vector2i position) {
        return getBiome(position.getX(), position.getY());
    }

    @Override
    public BiomeType getBiome(int x, int z) {
        checkRange(x, z);
        return area.getBiome(x, z);
    }

    @Override
    public MutableBiomeArea getBiomeCopy() {
        return null;
    }

    @Override
    public MutableBiomeArea getBiomeCopy(StorageType type) {
        return null;
    }

    @Override
    public ImmutableBiomeArea getImmutableBiomeCopy() {
        return null;
    }
}

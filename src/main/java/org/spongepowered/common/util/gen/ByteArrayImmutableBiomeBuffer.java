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
package org.spongepowered.common.util.gen;

import com.flowpowered.math.matrix.Matrix3d;
import com.flowpowered.math.vector.Vector2i;
import net.minecraft.world.biome.BiomeGenBase;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.biome.BiomeTypes;
import org.spongepowered.api.world.extent.ImmutableBiomeArea;
import org.spongepowered.api.world.extent.MutableBiomeArea;
import org.spongepowered.api.world.extent.StorageType;
import org.spongepowered.api.world.extent.UnmodifiableBiomeArea;

/**
 * Immutable biome area, backed by a byte array. The array passed to the
 * constructor is copied to ensure that the instance is immutable.
 */
@NonnullByDefault
public final class ByteArrayImmutableBiomeBuffer extends AbstractBiomeBuffer implements ImmutableBiomeArea {

    private final BiomeGenBase[] biomeById = BiomeGenBase.getBiomeGenArray();
    private final byte[] biomes;

    public ByteArrayImmutableBiomeBuffer(byte[] biomes, Vector2i start, Vector2i size) {
        super(start, size);
        this.biomes = biomes.clone();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public BiomeType getBiome(int x, int z) {
        checkRange(x, z);
        BiomeType biomeType = (BiomeType) this.biomeById[this.biomes[(x - this.start.getX()) + (z - this.start.getY()) * this.size.getX()] & 0xff];
        return biomeType == null ? BiomeTypes.OCEAN : biomeType;
    }

    @Override
    public BiomeType getBiome(Vector2i position) {
        return getBiome(position.getX(), position.getY());
    }

    @Override
    public ImmutableBiomeArea getBiomeView(Vector2i newMin, Vector2i newMax) {
        return null;
    }

    @Override
    public ImmutableBiomeArea getBiomeView(Matrix3d transform) {
        return null;
    }

    @Override
    public ImmutableBiomeArea getRelativeBiomeView() {
        return null;
    }

    @Override
    public UnmodifiableBiomeArea getUnmodifiableBiomeView() {
        return this;
    }

    @Override
    public MutableBiomeArea getBiomeCopy(StorageType type) {
        switch (type) {
            case STANDARD:
                return new ByteArrayMutableBiomeBuffer(this.biomes.clone(), this.start, this.size);
            case THREAD_SAFE:
            default:
                throw new UnsupportedOperationException(type.name());
        }
    }

    @Override
    public ImmutableBiomeArea getImmutableBiomeCopy() {
        return this;
    }
}

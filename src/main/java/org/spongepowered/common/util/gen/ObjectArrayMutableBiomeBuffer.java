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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.flowpowered.math.vector.Vector2i;
import net.minecraft.world.biome.BiomeGenBase;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.extent.MutableBiomeArea;

/**
 * Mutable view of a {@link BiomeGenBase} array.
 *
 * <p>Normally, the {@link ByteArrayMutableBiomeBuffer} class uses memory more
 * efficiently, but when the {@link BiomeGenBase} array is already created (for
 * example for a contract specified by Minecraft) this implementation becomes
 * more efficient.</p>
 */
public final class ObjectArrayMutableBiomeBuffer extends AbstractBiomeBuffer implements MutableBiomeArea {

    private final BiomeGenBase[] biomes;

    /**
     * Creates a new instance.
     *
     * @param biomes The biome array. The array is not copied, so changes made
     *        by this object will write through.
     * @param start The start position
     * @param size The size
     */
    public ObjectArrayMutableBiomeBuffer(BiomeGenBase[] biomes, Vector2i start, Vector2i size) {
        super(start, size);

        checkNotNull(biomes);
        checkArgument(biomes.length >= size.getX() * size.getY());
        this.biomes = biomes;
    }

    @Override
    public BiomeType getBiome(Vector2i position) {
        return getBiome(position.getX(), position.getY());
    }

    @Override
    public BiomeType getBiome(int x, int z) {
        checkRange(x, z);
        return (BiomeType) this.biomes[(x - this.start.getX()) | (z - this.start.getY()) << 4];
    }

    @Override
    public void setBiome(Vector2i position, BiomeType biome) {
        setBiome(position.getX(), position.getY(), biome);
    }

    @Override
    public void setBiome(int x, int z, BiomeType biome) {
        checkNotNull(biome, "biome");
        checkRange(x, z);
        this.biomes[(x - this.start.getX()) | (z - this.start.getY()) << 4] = (BiomeGenBase) biome;
    }
}

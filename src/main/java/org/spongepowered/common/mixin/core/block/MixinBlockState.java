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
package org.spongepowered.common.mixin.core.block;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateBase;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataManipulator;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.Sponge;
import org.spongepowered.common.data.SpongeBlockProcessor;
import org.spongepowered.common.data.SpongeManipulatorRegistry;
import org.spongepowered.common.interfaces.block.IMixinBlock;

import static com.google.common.base.Preconditions.checkState;
import static org.spongepowered.api.data.DataQuery.of;

@NonnullByDefault
@Mixin(net.minecraft.block.state.BlockState.StateImplementation.class)
public abstract class MixinBlockState extends BlockStateBase implements BlockState {

    @Shadow
    @SuppressWarnings("rawtypes")
    private ImmutableMap properties;
    @Shadow private Block block;

    private ImmutableList<DataManipulator<?>> manipulators;

    @Override
    public BlockType getType() {
        return (BlockType) getBlock();
    }

    @Override
    public ImmutableCollection<DataManipulator<?>> getManipulators() {
        if (this.manipulators == null) {
            this.manipulators = ((IMixinBlock) this.block).getManipulators(this);
        }
        return this.manipulators;
    }

    @Override
    public <M extends DataManipulator<M>> Optional<M> getManipulator(Class<M> manipulatorClass) {
        for (final DataManipulator<?> manipulator : this.getManipulators()) {
            if (manipulatorClass.isInstance(manipulator)) {
                return SpongeManipulatorRegistry.getInstance().getBlockUtil(manipulatorClass).get().createFrom(this);
            }
        }
        return Optional.absent();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <M extends DataManipulator<M>> Optional<BlockState> withData(M manipulator) {
        SpongeBlockProcessor<M> processor = ((SpongeManipulatorRegistry) Sponge.getSpongeRegistry().getManipulatorRegistry()).getBlockUtil((Class<M>) (Class) manipulator.getClass()).get();
        return processor.withData(this, manipulator);
    }

    @Override
    public <M extends DataManipulator<M>> Optional<BlockState> withoutData(Class<M> manipulator) {
        for (final DataManipulator<?> manipulator1 : this.getManipulators()) {
            if (manipulator.isInstance(manipulator1)) {
                return SpongeManipulatorRegistry.getInstance().getBlockUtil(manipulator).get().removeFrom(this);
            }
        }
        return Optional.absent();
    }

    @Override
    public DataContainer toContainer() {
        return new MemoryDataContainer()
            .set(of("BlockType"), this.getType().getId())
            .set(of("Data"), this.getManipulators());
    }

    @Override
    public BlockState withPropertyEnumOrdinal(String propertyName, int ordinal) {
        PropertyEnum propertyEnum = getPropertyEnumWithName(propertyName);

        Map<Integer, Enum> ordinalToEnum = ordinalToEnum(propertyEnum.getAllowedValues());

        checkState(ordinalToEnum.containsKey(ordinal),
            String.format("Enum ordinal out of range: %d", ordinal));

        return (BlockState)withProperty(propertyEnum, ordinalToEnum.get(ordinal));
    }

    @Override public boolean isEnumOrdinalValid(String propertyName, int ordinal) {
        PropertyEnum propertyEnum = getPropertyEnumWithName(propertyName);
        Map<Integer, Enum> ordinalToEnum = ordinalToEnum(propertyEnum.getAllowedValues());
        return ordinalToEnum.containsKey(ordinal);
    }

    @Override public int getPropertyEnumOrdinalValue(String propertyName) {
        PropertyEnum propertyEnum = getPropertyEnumWithName(propertyName);
        return ((Enum)getProperties().get(propertyEnum)).ordinal();
    }

    @Override public boolean hasPropertyEnum(String propertyName) {
        return maybeGetPropertyWithName(propertyName).isPresent() &&
            getPropertyWithName(propertyName) instanceof PropertyEnum;
    }

    private PropertyEnum getPropertyEnumWithName(String propertyName) {
        checkState(hasPropertyEnum(propertyName),
            String.format("BlockState does not have enum property %s", propertyName));
        return (PropertyEnum) getPropertyWithName(propertyName);
    }

    private IProperty getPropertyWithName(String propertyName) {
        Optional<IProperty> maybeProperty = maybeGetPropertyWithName(propertyName);
        if (maybeProperty.isPresent()) return maybeProperty.get();
        throw new RuntimeException(
            String.format("Unexpectedly did not find property with name %s", propertyName));
    }

    private Optional<IProperty> maybeGetPropertyWithName(String propertyName) {
        for (Object key : getProperties().keySet()) {
            IProperty property = (IProperty)key;
            if (property.getName().equals(propertyName)) {
                return Optional.of(property);
            }
        }
        return Optional.absent();
    }

    private Map<Integer, Enum> ordinalToEnum(Collection enumValues) {
        Map<Integer, Enum> result = new LinkedHashMap<Integer, Enum>();
        for (Object o: enumValues) {
            Enum e = (Enum)o;
            result.put(e.ordinal(), e);
        }
        return result;
    }
}

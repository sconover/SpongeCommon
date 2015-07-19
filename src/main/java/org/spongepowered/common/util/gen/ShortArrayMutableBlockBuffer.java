package org.spongepowered.common.util.gen;

import com.flowpowered.math.matrix.Matrix4d;
import com.flowpowered.math.vector.Vector3i;
import net.minecraft.block.Block;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.extent.ImmutableBlockVolume;
import org.spongepowered.api.world.extent.MutableBlockVolume;
import org.spongepowered.api.world.extent.StorageType;
import org.spongepowered.api.world.extent.UnmodifiableBlockVolume;

@NonnullByDefault
public class ShortArrayMutableBlockBuffer extends AbstractBlockBuffer implements MutableBlockVolume {

    @SuppressWarnings("ConstantConditions")
    private static final BlockState AIR = BlockTypes.AIR.getDefaultState();
    private final short[] blocks;

    public ShortArrayMutableBlockBuffer(Vector3i start, Vector3i size) {
        this(new short[size.getX() * size.getY() * size.getZ()], start, size);
    }

    public ShortArrayMutableBlockBuffer(short[] blocks, Vector3i start, Vector3i size) {
        super(start, size);
        this.blocks = blocks;
    }

    @Override
    public void setBlock(Vector3i position, BlockState block) {
        setBlock(position.getX(), position.getY(), position.getZ(), block);
    }

    @Override
    public void setBlockType(Vector3i position, BlockType type) {
        setBlockType(position.getX(), position.getY(), position.getZ(), type);
    }

    @Override
    public void setBlockType(int x, int y, int z, BlockType type) {
        setBlock(x, y, z, type.getDefaultState());
    }

    @Override
    public void setBlock(int x, int y, int z, BlockState block) {
        checkRange(x, y, z);
        this.blocks[getIndex(x, y, z)] = (short) Block.BLOCK_STATE_IDS.get(block);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        checkRange(x, y, z);
        BlockState block = (BlockState) Block.BLOCK_STATE_IDS.getByValue(this.blocks[getIndex(x, y, z)]);
        return block == null ? AIR : block;
    }

    @Override
    public MutableBlockVolume getBlockView(Vector3i newMin, Vector3i newMax) {
        return null;
    }

    @Override
    public MutableBlockVolume getBlockView(Matrix4d transform) {
        return null;
    }

    @Override
    public MutableBlockVolume getRelativeBlockView() {
        return null;
    }

    @Override
    public UnmodifiableBlockVolume getUnmodifiableBlockView() {
        return null;
    }

    @Override
    public MutableBlockVolume getBlockCopy(StorageType type) {
        switch (type) {
            case STANDARD:
                return new ShortArrayMutableBlockBuffer(this.blocks.clone(), this.start, this.size);
            case THREAD_SAFE:
            default:
                throw new UnsupportedOperationException(type.name());
        }
    }

    @Override
    public ImmutableBlockVolume getImmutableBlockCopy() {
        return new ShortArrayImmutableBlockBuffer(this.blocks, this.start, this.size);
    }
}

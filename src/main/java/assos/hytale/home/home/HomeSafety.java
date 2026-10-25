package assos.hytale.home.home;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.modules.collision.WorldUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;

public final class HomeSafety {

    private HomeSafety() {
    }

    @SuppressWarnings("deprecation")
    public static boolean isSafe(@Nonnull World world, double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            return false;
        }

        int blockY = MathUtil.floor(y);
        if (blockY < 1 || blockY >= ChunkUtil.HEIGHT - 1) {
            return false;
        }

        ChunkStore chunkStore = world.getChunkStore();
        int blockX = MathUtil.floor(x);
        int blockZ = MathUtil.floor(z);
        if (chunkStore.getChunkSectionReferenceAtBlock(blockX, blockY, blockZ) == null) {
            return true;
        }

        long feet = WorldUtil.getPackedMaterialAndFluidAtPosition(chunkStore, x, y, z);
        long head = WorldUtil.getPackedMaterialAndFluidAtPosition(chunkStore, x, y + 1.0, z);
        long ground = WorldUtil.getPackedMaterialAndFluidAtPosition(chunkStore, x, y - 1.0, z);

        return !isSolid(feet)
                && !isSolid(head)
                && fluidId(feet) == 0
                && fluidId(head) == 0
                && isSolid(ground)
                && fluidId(ground) == 0;
    }

    private static boolean isSolid(long packed) {
        BlockMaterial material = BlockMaterial.fromValue(MathUtil.unpackLeft(packed));
        return material == BlockMaterial.Solid;
    }

    private static int fluidId(long packed) {
        return (int) packed;
    }
}

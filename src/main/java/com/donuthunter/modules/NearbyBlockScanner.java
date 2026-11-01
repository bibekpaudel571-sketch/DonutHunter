package com.donuthunter.modules;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Category;

/** A deliberately bounded, manual scan. It does not inspect unloaded chunks. */
public abstract class NearbyBlockScanner extends Module {
    private final Set<Block> targets;
    private final int radius;

    protected NearbyBlockScanner(Category category, String name, String description, int radius, Block... targets) {
        super(category, name, description);
        this.radius = radius;
        this.targets = Set.of(targets);
    }

    @Override
    public void onActivate() {
        if (mc.level == null || mc.player == null) {
            info("Join a world before running this scan.");
            toggle();
            return;
        }

        BlockPos origin = mc.player.blockPosition();
        int found = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (targets.contains(mc.level.getBlockState(pos).getBlock())) found++;
                }
            }
        }
        info("Found %d matching blocks within %d blocks. Only loaded world data is checked.", found, radius);
        toggle();
    }
}

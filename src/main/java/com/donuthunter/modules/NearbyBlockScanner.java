
package com.donuthunter.modules;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Category;

public abstract class NearbyBlockScanner extends Module {
    private static final int SCAN_INTERVAL_TICKS = 100; // 5 seconds

    private final Set<Block> targets;
    private final int radius;

    private int ticks;
    private int lastFound = -1;

    protected NearbyBlockScanner(
        Category category,
        String name,
        String description,
        int radius,
        Block... targets
    ) {
        super(category, name, description);
        this.radius = radius;
        this.targets = Set.of(targets);
    }

    @Override
    public void onActivate() {
        ticks = 0;
        lastFound = -1;
        scan();
    }

    @Override
    public void onTick() {
        ticks++;

        if (ticks >= SCAN_INTERVAL_TICKS) {
            ticks = 0;
            scan();
        }
    }

    private void scan() {
        if (mc.level == null || mc.player == null) return;

        BlockPos origin = mc.player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int found = 0;
        int radiusSquared = radius * radius;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radiusSquared) continue;

                    pos.set(
                        origin.getX() + x,
                        origin.getY() + y,
                        origin.getZ() + z
                    );

                    if (targets.contains(mc.level.getBlockState(pos).getBlock())) {
                        found++;
                    }
                }
            }
        }

        if (found != lastFound) {
            info("Found %d matching blocks within %d blocks.", found, radius);
            lastFound = found;
        }
    }
}

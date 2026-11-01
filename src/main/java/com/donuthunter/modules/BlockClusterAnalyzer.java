package com.donuthunter.modules;

import meteordevelopment.meteorclient.systems.modules.Categories;
import net.minecraft.world.level.block.Blocks;

public class BlockClusterAnalyzer extends NearbyBlockScanner {
    public BlockClusterAnalyzer() {
        super(Categories.World, "block-cluster-analyzer", "Counts storage blocks and spawners in a bounded area around you.", 32,
            Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL, Blocks.SHULKER_BOX, Blocks.SPAWNER);
    }
}

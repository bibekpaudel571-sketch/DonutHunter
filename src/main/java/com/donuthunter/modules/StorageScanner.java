package com.donuthunter.modules;

import meteordevelopment.meteorclient.systems.modules.Categories;
import net.minecraft.world.level.block.Blocks;

public class StorageScanner extends NearbyBlockScanner {
    public StorageScanner() {
        super(Categories.World, "storage-scanner", "Counts nearby chests, barrels and shulker boxes.", 48,
            Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL, Blocks.SHULKER_BOX);
    }
}

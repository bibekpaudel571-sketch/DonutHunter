package com.donuthunter.modules;

import meteordevelopment.meteorclient.systems.modules.Categories;
import net.minecraft.world.level.block.Blocks;

public class SpawnerScanner extends NearbyBlockScanner {
    public SpawnerScanner() {
        super(Categories.World, "spawner-scanner", "Counts nearby spawner blocks in loaded world data.", 64,
            Blocks.SPAWNER);
    }
}

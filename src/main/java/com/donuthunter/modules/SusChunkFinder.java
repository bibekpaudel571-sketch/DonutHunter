
package com.donuthunter.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.chunk.LevelChunk;

public class SusChunkFinder extends Module {
    private static final int BLOCKS_PER_TICK = 8192;

    private static final int BASE_THRESHOLD = 20;
    private static final int FARM_THRESHOLD = 15;

    private final List<ScanTarget> targets = new ArrayList<>();
    private final Map<Long, SuspectChunk> suspiciousChunks = new HashMap<>();

    private int targetIndex;
    private int blockIndex;

    private int minY;
    private int maxY;

    private int lastPlayerChunkX;
    private int lastPlayerChunkZ;

    private ChunkCounts counts;

    private final Color fillColor = new Color(0, 0, 0, 55);
    private final Color lineColor = new Color(0, 0, 0, 130);

    public SusChunkFinder() {
        super(
            Categories.World,
            "sus-chunk-finder",
            "Finds possible bases and farms in loaded chunks."
        );
    }

    @Override
    public void onActivate() {
        suspiciousChunks.clear();
        targets.clear();

        targetIndex = 0;
        blockIndex = 0;

        if (mc.level == null || mc.player == null) return;

        minY = mc.level.getMinY();
        maxY = mc.level.getMaxY();

        rebuildTargets();
    }

    @Override
    public void onDeactivate() {
        targets.clear();
        suspiciousChunks.clear();
        counts = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.level == null || mc.player == null) return;

        int playerChunkX = mc.player.blockPosition().getX() >> 4;
        int playerChunkZ = mc.player.blockPosition().getZ() >> 4;

        if (playerChunkX != lastPlayerChunkX ||
            playerChunkZ != lastPlayerChunkZ) {
            rebuildTargets();
        }

        processScan();
    }

    private void rebuildTargets() {
        if (mc.level == null || mc.player == null) return;

        targets.clear();

        targetIndex = 0;
        blockIndex = 0;
        counts = null;

        lastPlayerChunkX = mc.player.blockPosition().getX() >> 4;
        lastPlayerChunkZ = mc.player.blockPosition().getZ() >> 4;

        int radius = mc.options.renderDistance().get();

        for (int cx = lastPlayerChunkX - radius;
             cx <= lastPlayerChunkX + radius; cx++) {

            for (int cz = lastPlayerChunkZ - radius;
                 cz <= lastPlayerChunkZ + radius; cz++) {

                // Only use chunks already loaded by Minecraft.
                LevelChunk chunk = mc.level.getChunkSource()
                    .getChunkNow(cx, cz);

                if (chunk != null) {
                    targets.add(new ScanTarget(cx, cz, chunk));
                }
            }
        }
    }

    private void processScan() {
        int remaining = BLOCKS_PER_TICK;

        while (remaining > 0 && targetIndex < targets.size()) {
            ScanTarget target = targets.get(targetIndex);

            if (counts == null) {
                counts = new ChunkCounts();
                blockIndex = 0;
            }

            int height = maxY - minY;
            int totalBlocks = 256 * height;

            int amount = Math.min(
                remaining,
                totalBlocks - blockIndex
            );

            for (int i = 0; i < amount; i++) {
                int index = blockIndex + i;

                int y = minY + index / 256;
                int local = index % 256;

                int localX = local & 15;
                int localZ = local >> 4;

                BlockPos pos = new BlockPos(
                    target.x * 16 + localX,
                    y,
                    target.z * 16 + localZ
                );

                Block block = target.chunk.getBlockState(pos).getBlock();

                countBlock(block);
            }

            blockIndex += amount;
            remaining -= amount;

            if (blockIndex >= totalBlocks) {
                finishChunk(target);

                targetIndex++;
                blockIndex = 0;
                counts = null;
            }
        }

        // Start another pass after all currently loaded targets
        // have been scanned.
        if (targetIndex >= targets.size() && !targets.isEmpty()) {
            rebuildTargets();
        }
    }

    private void countBlock(Block block) {
        // Storage: possible base indicators.
        if (block == Blocks.CHEST ||
            block == Blocks.TRAPPED_CHEST ||
            block == Blocks.BARREL ||
            block instanceof ShulkerBoxBlock) {
            counts.storage++;
        }

        // Base building materials.
        if (block == Blocks.BLACKSTONE ||
            block == Blocks.POLISHED_BLACKSTONE ||
            block == Blocks.POLISHED_BLACKSTONE_BRICKS ||
            block == Blocks.GILDED_BLACKSTONE ||
            block == Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS ||
            block == Blocks.CHISELED_POLISHED_BLACKSTONE) {
            counts.blackstone++;
        }

        if (block == Blocks.QUARTZ_BLOCK ||
            block == Blocks.CHISELED_QUARTZ_BLOCK ||
            block == Blocks.QUARTZ_BRICKS ||
            block == Blocks.QUARTZ_PILLAR ||
            block == Blocks.SMOOTH_QUARTZ) {
            counts.quartz++;
        }

        if (block instanceof TrapDoorBlock) {
            counts.trapdoors++;
        }

        // Farm and redstone indicators.
        if (block == Blocks.OBSERVER) {
            counts.observers++;
        }

        if (block == Blocks.PISTON ||
            block == Blocks.STICKY_PISTON) {
            counts.pistons++;
        }

        if (block == Blocks.REDSTONE_WIRE ||
            block == Blocks.REPEATER ||
            block == Blocks.COMPARATOR ||
            block == Blocks.REDSTONE_TORCH ||
            block == Blocks.REDSTONE_WALL_TORCH) {
            counts.redstone++;
        }

        if (block == Blocks.HOPPER ||
            block == Blocks.FURNACE ||
            block == Blocks.BLAST_FURNACE ||
            block == Blocks.SMOKER ||
            block == Blocks.DISPENSER ||
            block == Blocks.DROPPER) {
            counts.utility++;
        }

        if (block == Blocks.SPAWNER) {
            counts.spawners++;
        }
    }

    private void finishChunk(ScanTarget target) {
        int baseScore =
            counts.storage * 5 +
            counts.utility * 2 +
            counts.blackstone / 8 +
            counts.quartz / 8 +
            counts.trapdoors / 3;

        int farmScore =
            counts.observers * 3 +
            counts.pistons * 3 +
            counts.redstone / 4 +
            counts.utility;

        boolean possibleBase = baseScore >= BASE_THRESHOLD;
        boolean possibleFarm = farmScore >= FARM_THRESHOLD;

        long key = chunkKey(target.x, target.z);

        if (possibleBase || possibleFarm) {
            String type;

            if (possibleBase && possibleFarm) {
                type = "Possible Base / Farm";
            } else if (possibleBase) {
                type = "Possible Base";
            } else {
                type = "Possible Farm";
            }

            int score = Math.max(baseScore, farmScore);

            boolean newlyFound = !suspiciousChunks.containsKey(key);

            suspiciousChunks.put(
                key,
                new SuspectChunk(target.x, target.z, score)
            );

            if (newlyFound) {
                info(
                    "SUS CHUNK! %s | X: %d Z: %d | Score: %d | Storage: %d | Blackstone: %d | Quartz: %d | Trapdoors: %d | Observers: %d | Pistons: %d | Redstone: %d",
                    type,
                    target.x * 16,
                    target.z * 16,
                    score,
                    counts.storage,
                    counts.blackstone,
                    counts.quartz,
                    counts.trapdoors,
                    counts.observers,
                    counts.pistons,
                    counts.redstone
                );
            }
        } else {
            suspiciousChunks.remove(key);
        }
    }

    private long chunkKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.level == null) return;

        for (SuspectChunk chunk : suspiciousChunks.values()) {
            double x = chunk.x * 16.0;
            double z = chunk.z * 16.0;

            event.renderer.box(
                x, minY, z,
                x + 16, maxY, z + 16,
                fillColor,
                lineColor,
                ShapeMode.Both,
                0
            );
        }
    }

    private static class ChunkCounts {
        int storage;
        int blackstone;
        int quartz;
        int trapdoors;
        int observers;
        int pistons;
        int redstone;
        int utility;
        int spawners;
    }

    private record ScanTarget(int x, int z, LevelChunk chunk) {}

    private record SuspectChunk(int x, int z, int score) {}
}

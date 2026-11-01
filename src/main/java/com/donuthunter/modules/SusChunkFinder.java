
package com.donuthunter.modules;

import java.util.HashMap;
import java.util.Map;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;

public class SusChunkFinder extends Module {
    private static final int SCAN_INTERVAL = 100;
    private static final int SCAN_RADIUS = 4;
    private static final int MIN_SCORE = 20;

    private final Map<Long, SuspectChunk> suspiciousChunks = new HashMap<>();
    private final Map<Long, Integer> activitySamples = new HashMap<>();

    private final Color fillColor = new Color(0, 0, 0, 55);
    private final Color lineColor = new Color(0, 0, 0, 130);

    private int ticks;

    public SusChunkFinder() {
        super(
            Categories.World,
            "sus-chunk-finder",
            "Finds suspicious loaded chunks using storage and observed player activity."
        );
    }

    @Override
    public void onActivate() {
        ticks = 0;
        suspiciousChunks.clear();
        activitySamples.clear();
        scanChunks();
    }

    @Override
    public void onDeactivate() {
        suspiciousChunks.clear();
        activitySamples.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.level == null || mc.player == null) return;

        ticks++;

        if (ticks >= SCAN_INTERVAL) {
            ticks = 0;
            scanChunks();
        }
    }

    private void scanChunks() {
        if (mc.level == null || mc.player == null) return;

        int playerChunkX = mc.player.blockPosition().getX() >> 4;
        int playerChunkZ = mc.player.blockPosition().getZ() >> 4;

        for (int cx = playerChunkX - SCAN_RADIUS;
             cx <= playerChunkX + SCAN_RADIUS; cx++) {

            for (int cz = playerChunkZ - SCAN_RADIUS;
                 cz <= playerChunkZ + SCAN_RADIUS; cz++) {

                // false means do not force-load missing chunks.
                LevelChunk chunk = mc.level.getChunkSource().getChunk(
                    cx, cz, ChunkStatus.FULL, false
                );

                if (chunk == null) continue;

                int storage = 0;
                int utility = 0;
                int spawners = 0;

                // Block entities exist throughout the chunk's full
                // vertical range, including underground areas.
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    Block block = chunk.getBlockState(
                        entity.getBlockPos()
                    ).getBlock();

                    if (isStorage(block)) {
                        storage++;
                    } else if (isUtility(block)) {
                        utility++;
                    } else if (block == Blocks.SPAWNER) {
                        spawners++;
                    }
                }

                long key = chunkKey(cx, cz);

                // Observe other players while this module is running.
                boolean playerSeen = false;

                for (Player player : mc.level.players()) {
                    if (player == mc.player) continue;

                    BlockPos pos = player.blockPosition();

                    if ((pos.getX() >> 4) == cx &&
                        (pos.getZ() >> 4) == cz) {
                        playerSeen = true;
                        break;
                    }
                }

                if (playerSeen) {
                    activitySamples.merge(key, 1, Integer::sum);
                }

                int samples = activitySamples.getOrDefault(key, 0);

                int score =
                    storage * 5 +
                    utility * 2 +
                    spawners * 3 +
                    Math.min(samples * 2, 10);

                if (score >= MIN_SCORE) {
                    boolean newlyFound = !suspiciousChunks.containsKey(key);

                    suspiciousChunks.put(
                        key,
                        new SuspectChunk(
                            cx, cz, score,
                            storage, utility, spawners, samples
                        )
                    );

                    if (newlyFound) {
                        info(
                            "Suspicious chunk: %d, %d | Score: %d",
                            cx, cz, score
                        );
                    }
                } else {
                    suspiciousChunks.remove(key);
                }
            }
        }
    }

    private boolean isStorage(Block block) {
        return block == Blocks.CHEST
            || block == Blocks.TRAPPED_CHEST
            || block == Blocks.BARREL
            || block instanceof ShulkerBoxBlock;
    }

    private boolean isUtility(Block block) {
        return block == Blocks.HOPPER
            || block == Blocks.FURNACE
            || block == Blocks.BLAST_FURNACE
            || block == Blocks.SMOKER
            || block == Blocks.DISPENSER
            || block == Blocks.DROPPER;
    }

    private long chunkKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.level == null) return;

        int minY = mc.level.getMinY();
        int maxY = mc.level.getMaxY();

        for (SuspectChunk chunk : suspiciousChunks.values()) {
            double x = chunk.x * 16.0;
            double z = chunk.z * 16.0;

            // Highlight the entire chunk vertically.
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

    private record SuspectChunk(
        int x,
        int z,
        int score,
        int storage,
        int utility,
        int spawners,
        int activitySamples
    ) {}
}

package com.donuthunter;

import com.donuthunter.modules.BlockClusterAnalyzer;
import com.donuthunter.modules.StorageScanner;
import com.donuthunter.modules.SpawnerScanner;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class DonutHunter extends MeteorAddon {
    @Override
    public void onInitialize() {
        Modules.get().add(new BlockClusterAnalyzer());
        Modules.get().add(new StorageScanner());
        Modules.get().add(new SpawnerScanner());
    }

    @Override
    public String getPackage() {
        return "com.donuthunter";
    }
}

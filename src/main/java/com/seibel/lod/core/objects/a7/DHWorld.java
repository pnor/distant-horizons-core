package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.handlers.LodDimensionFinder;
import com.seibel.lod.core.objects.a7.io.DHFolderHandler;
import com.seibel.lod.core.objects.a7.io.LevelToFileMatcher;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;

import java.io.File;
import java.util.HashMap;

public class DHWorld {
    private final File saveDir;
    private final HashMap<IWorldWrapper, DHLevel> levels;

    private LevelToFileMatcher levelToFileMatcher = null;

    public DHWorld() {
        saveDir = DHFolderHandler.getCurrentWorldFolder();
        levels = new HashMap<>();
    }

    public DHLevel getLevel(IWorldWrapper wrapper) {
        if (!levels.containsKey(wrapper)) {
            if (levelToFileMatcher == null || levelToFileMatcher.getTargetWorld() != wrapper) {
                levelToFileMatcher = new LevelToFileMatcher(saveDir, wrapper);
            }
            DHLevel level = levelToFileMatcher.tryGetLevel();
            if (level != null) {
                levels.put(wrapper, level);
                levelToFileMatcher = null;
                return level;
            } else {
                return null;
            }
        } else return levels.get(wrapper);
    }
}

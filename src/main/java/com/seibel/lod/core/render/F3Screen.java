package com.seibel.lod.core.render;

import com.seibel.lod.core.ModInfo;

import java.util.Arrays;
import java.util.List;

public class F3Screen {
    public static List<String> f3List = Arrays.asList(
            "",
            ModInfo.READABLE_NAME + " version: " + ModInfo.VERSION
    );
    public static boolean renderCustomF3 = false;
}

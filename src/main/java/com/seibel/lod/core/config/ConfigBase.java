package com.seibel.lod.core.config;

import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.config.file.ConfigFileHandling;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Indexes and sets everything up for the file handling and gui
 *
 * @author coolGi2007
 * @author Ran
 */
public class ConfigBase {
    public static final List<ConfigEntry<?>> entries = new ArrayList<ConfigEntry<?>>();
    public static final List<String> categories = new ArrayList<>();

    public static void init(Class<?> config) {
        categories.add(""); // Add root category to category list
        initNestedClass(config, ""); // Init root category

        // File handling (load from file)
        ConfigFileHandling.loadFromFile();

        // Temporary to see stuff
        System.out.println(entries);
        System.out.println(categories);
    }

    private static void initNestedClass(Class<?> config, String category) {
        // Put all the entries in entries

        for (Field field : config.getFields())
		{
            if (ConfigEntry.class.isAssignableFrom(field.getType())) { // If item is type ConfigEntry
                try {
                    entries.add((ConfigEntry<?>) field.get(field.getType())); // Add to entries
                } catch (IllegalAccessException exception) {
                    exception.printStackTrace();
                }
                entries.get(entries.size() - 1).category = category;
                entries.get(entries.size() - 1).name = field.getName();
            }

			if (field.isAnnotationPresent(ConfigAnnotations.Category.class)) { // If it's a category then init the stuff inside it and put it in the category list
                String NCategory = (category.isEmpty() ? "" : category + ".") + field.getName();
                categories.add(NCategory);
                initNestedClass(field.getType(), NCategory);
            }
		}
    }
}

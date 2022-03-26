package com.seibel.lod.core.config;

import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.config.file.ConfigFileHandling;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Indexes and sets everything up for the file handling and gui
 *
 * @author coolGi2007
 * @author Ran
 */
public class ConfigBase {
    /*
            What the config works with

        Enum
        Integer
        Boolean
        Double
        Float
        Byte
        Map<String, Boolean> or MultiOption
     */
    public static final List<Class<?>> acceptableInputs = new ArrayList<>();
    private static void addAcceptableInputs() {
        acceptableInputs.add(Boolean.class);
        acceptableInputs.add(Byte.class);
        acceptableInputs.add(Short.class);
        acceptableInputs.add(Integer.class);
        acceptableInputs.add(Long.class);
        acceptableInputs.add(Float.class);
        acceptableInputs.add(String.class);
        acceptableInputs.add(HashMap.class);
    }

    public static final List<ConfigEntry<?>> entries = new ArrayList<ConfigEntry<?>>();
    public static final List<String> categories = new ArrayList<>();

    public static void init(Class<?> config) {
        addAcceptableInputs(); // Add all of the acceptable stuff to the acceptableInputs list
//        categories.add(""); // Add root category to category list
        initNestedClass(config, ""); // Init root category

        // File handling (load from file)
        ConfigFileHandling.loadFromFile();
    }

    private static void initNestedClass(Class<?> config, String category) {
        // Put all the entries in entries

        for (Field field : config.getFields()) {
            if (ConfigEntry.class.isAssignableFrom(field.getType())) { // If item is type ConfigEntry
                try {
                    if (isAcceptableType(((ConfigEntry<?>) field.get(field.getType())).get().getClass())) {
                        entries.add((ConfigEntry<?>) field.get(field.getType())); // Add to entries
                        entries.get(entries.size() - 1).category = category;
                        entries.get(entries.size() - 1).name = field.getName();
                    } else {
                        ClientApi.LOGGER.error("Invalid variable type at [" + (category.isEmpty() ? "" : category + ".") + field.getName() + "].");
                        ClientApi.LOGGER.error("Type [" + ((ConfigEntry<?>) field.get(field.getType())).get().getClass() + "] is not one of these types [" + acceptableInputs.toString() + "]");
                    }
                } catch (IllegalAccessException exception) {
                    exception.printStackTrace();
                }
            }

            if (field.isAnnotationPresent(ConfigAnnotations.Category.class)) { // If it's a category then init the stuff inside it and put it in the category list
                String NCategory = (category.isEmpty() ? "" : category + ".") + field.getName();
                categories.add(NCategory);
                initNestedClass(field.getType(), NCategory);
            }
        }
    }

    private static boolean isAcceptableType(Class<?> Class) {
        if (Class.isEnum())
            return true;
        for(Class<?> i: acceptableInputs) {
            if(Class == i)
                return true;
        }
        return false;
    }
}

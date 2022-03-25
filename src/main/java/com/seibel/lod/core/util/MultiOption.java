package com.seibel.lod.core.util;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A way to add a config option that contains multiple items
 * This could be used for multiple dimensions
 *
 * @author coolGi
 */
// TODO: Add support for enum's
public class MultiOption<T> {
//    Map<String, T> items = new HashMap<>(); // TODO: Later on use this rather than using 2 lists
    List<String> itemNames = new ArrayList<>();
    List<T> itemValues = new ArrayList<>();

    public MultiOption(List<String> itemNames, List<T> itemValues) {
        this.itemNames = itemNames;
        this.itemValues = itemValues;
    }

    public MultiOption() {

    }
    
    public String getAsString() {
        JSONObject jsonObject = new JSONObject();

        for (int i=0; i< itemNames.size(); i++) {
            jsonObject.put(itemNames.get(i), itemValues.get(i));
        }

        return jsonObject.toJSONString();
    }

    public MultiOption getFromString(String s) {
        List<String> tmpItemNames = new ArrayList<>();
        List<T> tmpItemValues = new ArrayList<>();

        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) new JSONParser().parse(s);
        } catch (ParseException p) {
            p.printStackTrace();
        }

        for (int i = 0; i < jsonObject.keySet().toArray().length; i++) {
            tmpItemNames.add((String) jsonObject.keySet().toArray()[i]);
            tmpItemValues.add((T) jsonObject.get(jsonObject.keySet().toArray()[i]));
        }

        this.itemNames = tmpItemNames;
        this.itemValues = tmpItemValues;
        return this;
    }

    public T get(String item) {
        return itemValues.get(itemNames.indexOf(item));
    }
    public MultiOption set(String item, T value) {
        if (itemNames.contains(item)) {
            itemValues.set(itemNames.indexOf(item), value);
        } else {
            itemNames.add(item);
            itemValues.add(value);
        }
        return this;
    }
}

package com.seibel.lod.core.jar.wrapperInterfaces.config;

import com.seibel.lod.core.JarMain;
import com.seibel.lod.core.wrapperInterfaces.config.IConfigWrapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ConfigWrapper implements IConfigWrapper {
    public static final ConfigWrapper INSTANCE = new ConfigWrapper();
    private static JSONObject jsonObject = new JSONObject();

    public static void init() {
        try {
            Object obj = new JSONParser().parse(JarMain.convertInputStreamToString(JarMain.accessFile("assets/lod/lang/en_us.json")));
            jsonObject =  (JSONObject) obj;
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean langExists(String str) {
        if (jsonObject.get(str) == null)
            return false;
        else
            return true;
    }

    @Override
    public String getLang(String str) {
        if (jsonObject.get(str) != null)
            return (String) jsonObject.get(str);
        else
            return str;
    }
}

package com.seibel.lod.core.jar.installer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Gets the releases available on gitlab and sends out the link
 *
 * @author coolGi
 */
public class GitlabGetter {
    public static final String GitLabApi = "https://gitlab.com/api/v4/projects/";
    public static final String projectID = "18204078";
    public static JSONArray projectRelease = new JSONArray();

    public static List<String> releaseNames = new ArrayList<>(); // This list contains the release ID's
    public static List<String> readableReleaseNames = new ArrayList<>(); // This list contains the readable names of the ID's
    private static List<String> mcVersionReleases = new ArrayList<>(); // A list of all minecraft releases



    public static void init() {
        try {
            projectRelease = (JSONArray) new JSONParser().parse(WebDownloader.downloadAsString(new URL(GitLabApi+projectID+"/releases")));
            for (int i = 0; i < projectRelease.size(); i++) {
                releaseNames.add(((JSONObject) projectRelease.get(i)).get("tag_name").toString());
                readableReleaseNames.add(((JSONObject) projectRelease.get(i)).get("name").toString());
            }

            // Some tests for getting the release versions
//            System.out.println(getRelease("1.6.3a", "1.18.2"));
//            System.out.println(getRelease("1.16.4-a1.2", null)); // The oldest downloadable version is 1.2 as versions before that didn't include downloads

            // Set the mcVersionReleases
            JSONArray minecraftReleases = (JSONArray) ((JSONObject) new JSONParser().parse(WebDownloader.downloadAsString(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json")))).get("versions");
            for (int i = 0; i < minecraftReleases.size(); i++) {
                JSONObject jsonObject = (JSONObject) minecraftReleases.get(i);
                if (jsonObject.get("type").toString().equals("release"))
                    mcVersionReleases.add(jsonObject.get("id").toString());
            }

            // Some tests to get minecraft versions available in that version of the mod
//            System.out.println(getMcVersionsInRelease("1.6.5a"));
//            System.out.println(getMcVersionsInRelease("1.16.4-a1.2"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    /** Gets the compatible minecraft versions a release of the mod works with */
    public static List<String> getMcVersionsInRelease(String version) {
        List<String> versions = new ArrayList<>();

        JSONArray releaseArray = null;
        try {
            releaseArray = (
                    ((JSONArray)
                            ((JSONObject)
                                    ((JSONObject)
                                            projectRelease.get(releaseNames.indexOf(version))
                                    ).get("assets")
                            ).get("links")));
        } catch (Exception e) {
            System.out.println("ERROR: Release ["+version+"] is not a valid release. Printing stacktrace...");
            e.printStackTrace();
            return null;
        }


        for (int i = 0; i < releaseArray.size(); i++) {
            String name = ((JSONObject) releaseArray.get(i)).get("name").toString();
            for (String mcVersion : mcVersionReleases) {
                if (name.contains(mcVersion)) {
                    versions.add(mcVersion);
                    break;
                }
            }
        }

        return versions;
    }
    /** Gets the url to the download of a release of the mod */
    public static URL getRelease(String version, String mcVersion) {
        // Get the asset links of the releases
        JSONArray releaseArray = null;
        try {
            releaseArray = (
                    ((JSONArray)
                            ((JSONObject)
                                    ((JSONObject)
                                            projectRelease.get(releaseNames.indexOf(version))
                                    ).get("assets")
                            ).get("links")));
        } catch (Exception e) {
            System.out.println("ERROR: Release ["+version+"] is not a valid release. Printing stacktrace...");
            e.printStackTrace();
            return null;
        }


        if (mcVersion != null) {
            for (int i = 0; i < releaseArray.size(); i++) {
                if (((JSONObject) releaseArray.get(i)).get("name").toString().contains(mcVersion)) { // With the way our GitLab releases is set up, the only way to check the mc version is to check if it is in the name
                    try {
                        return new URL(((JSONObject) releaseArray.get(i)).get("direct_asset_url").toString());
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        } else {
            // If version is null it gets the first version available
            try {
                return new URL(((JSONObject) releaseArray.get(0)).get("direct_asset_url").toString());
            } catch (Exception e) { e.printStackTrace(); }
        }

        return null;
    }
}

package jacksunderscoreusername.ancient_trinkets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Config {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    @SerializedName("// Sends a message to the chat whenever a new trinket is created")
    public final String _comment_announce_spawns = "(Default: true)";
    @SerializedName("Announce trinket spawns")
    public boolean announce_spawns = true;

    @SerializedName("// Sends a message to the chat whenever a trinket is destroyed")
    public final String _comment_announce_destroys = "(Default: true)";
    @SerializedName("Announce trinket destroys")
    public boolean announce_destroys = true;

    @SerializedName("// The maximum number of each trinket that can exist at once, set to zero to remove the limit")
    public final String _comment_max_trinket_count = "(Default: 1)";
    @SerializedName("Trinket count limit")
    public int max_trinket_count = 1;

    @SerializedName("// The maximum number of how many times a trinket can be used before it breaks, set to zero to remove the limit")
    public final String _comment_max_uses = "(Default: 0)";
    @SerializedName("Single use")
    public int max_uses = 0;


    @SerializedName("// --- Settings for disallowing trinket hoarding by a single player ---")
    public final String _sect_interference = "";

    @SerializedName("// The maximum number of ancient_trinkets a player can have, if they exceed this limit then all their ancient_trinkets lose function until the player has less then the limit, set to zero to remove the limit")
    public final String _comment_max_player_trinkets = "(Default: 0)";
    @SerializedName("Max ancient_trinkets per player")
    public int max_player_trinkets = 1;

    @SerializedName("// The mode used to determine if a player has too many ancient_trinkets, only matters if 'Max ancient_trinkets per player' is > 0")
    public final String _comment_player_limit_mode = "(Default: 2)";
    @SerializedName("// 1: Only counts the ancient_trinkets when the player uses them")
    public final String _comment_player_limit_mode_0 = "";
    @SerializedName("// 2: All ancient_trinkets are now 'claimed' when picked up until another player picks it up or it is destroyed, counts all ancient_trinkets currently claimed by the player")
    public final String _comment_player_limit_mode_2 = "";
    public int player_limit_mode = 2;

    @SerializedName("// Delays trinket disablement for modes 1 and 2 until the condition has been met for X seconds, this is to prevent a player throwing a trinket at someone else and disabling their ancient_trinkets")
    public final String _comment_trinket_interference_warmup = "(Default: 15)";
    @SerializedName("Trinket interference warmup")
    public int trinket_interference_warmup = 15;

    @SerializedName("// Delays trinket enablement until the player has below the limit of ancient_trinkets for X seconds")
    public final String _comment_trinket_interference_cooldown = "(Default: 300)";
    @SerializedName("Trinket interference cooldown")
    public int trinket_interference_cooldown = 300;

    public static Config loadConfigFile(File file) {
        Config config = null;

        if (file.exists()) {
            try (BufferedReader fileReader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)
            )) {
                config = gson.fromJson(fileReader, Config.class);
            } catch (IOException e) {
                throw new RuntimeException("Problem occurred when trying to load config: ", e);
            }
        }
        if (config == null) {
            config = new Config();
        }

        config.saveConfigFile(file);
        return config;
    }

    public void saveConfigFile(File file) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String toJsonString() {
        return gson.toJson(this);
    }

    public static Config fromJsonString(String jsonString) {
        return gson.fromJson(jsonString, Config.class);
    }
}
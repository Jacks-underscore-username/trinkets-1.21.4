package jacksunderscoreusername.trinkets.dialog;

import com.google.gson.Gson;
import com.mojang.serialization.JsonOps;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class DialogPage {
    private static final Gson gson = new Gson();

    @FunctionalInterface
    public interface DialogCallback<T, I> {
        void apply(T player, I inventory);
    }

    public static class DialogPageItem {
        public DialogPageItem(Text text) {
            this(text, null, null);
        }

        public DialogPageItem(Text text, DialogCallback<ServerPlayerEntity, Inventory> callback, ServerPlayerEntity player) {
            this(text, callback, player, null);
        }

        public DialogPageItem(Text text, DialogCallback<ServerPlayerEntity, Inventory> callback, ServerPlayerEntity player, Text tooltip) {
            this.text = gson.toJson(TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, text).getOrThrow());
            if (callback != null) {
                this.callback = callback;
                this.isButton = true;
                UUID key = player.getUuid();
                if (!DialogHelper.newCallbacks.containsKey(key)) {
                    DialogHelper.newCallbacks.put(key, new ArrayList<>());
                }
                DialogHelper.newCallbacks.get(key).add(callback);
            }
            if (tooltip != null) {
                this.tooltip = gson.toJson(TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, tooltip).getOrThrow());
            }
        }

        transient public DialogCallback<ServerPlayerEntity, Inventory> callback;
        public String text;
        public String tooltip = null;
        public boolean isButton = false;
    }

    public DialogPage(DialogPageItem... items) {
        this.items.addAll(Arrays.asList(items));
    }

    public DialogPage addItems(DialogPageItem... items) {
        this.items.addAll(Arrays.asList(items));
        return this;
    }

    public ArrayList<DialogPageItem> items = new ArrayList<>();

    public String toJsonString() {
        return gson.toJson(this);
    }

    public static DialogPage fromJsonString(String jsonString) {
        return gson.fromJson(jsonString, DialogPage.class);
    }
}
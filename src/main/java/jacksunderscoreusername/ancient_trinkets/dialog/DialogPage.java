package jacksunderscoreusername.ancient_trinkets.dialog;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DialogPage {
    private static final Gson gson = new Gson();

    public enum Alignment {TOP, BOTTOM}

    public enum Type {
        TEXT,
        BUTTON
    }

    public interface Callback {
        void apply(ServerPlayerEntity player, SimpleInventory inventory);
    }

    public static class DialogPageItem {
        public DialogPageItem(Type type) {
            this.type = type.toString();
        }

        private final String type;
        private String text = null;
        private String tooltip = null;
        private String alignment = Alignment.TOP.toString();
        private boolean clickable = false;
        transient public Callback callback;
        private boolean forceLineBreak = true;

        public Type getType() {
            return Type.valueOf(type);
        }

        public Text getText() {
            return TextCodecs.CODEC.decode(JsonOps.INSTANCE, gson.fromJson(text, JsonElement.class)).getOrThrow().getFirst();
        }

        public DialogPageItem setText(Text text) {
            this.text = gson.toJson(TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, text).getOrThrow());
            return this;
        }

        public Text getTooltip() {
            return tooltip == null ? null : TextCodecs.CODEC.decode(JsonOps.INSTANCE, gson.fromJson(tooltip, JsonElement.class)).getOrThrow().getFirst();
        }

        public DialogPageItem setTooltip(Text tooltip) {
            this.tooltip = gson.toJson(TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, tooltip).getOrThrow());
            return this;
        }

        public Alignment getAlignment() {
            return Alignment.valueOf(alignment);
        }

        public DialogPageItem setAlignment(Alignment alignment) {
            this.alignment = alignment.toString();
            return this;
        }

        public boolean isClickable() {
            return clickable;
        }

        public DialogPageItem setClickCallback(Callback callback) {
            this.clickable = true;
            this.callback = callback;
            return this;
        }

        public boolean getForceLineBreak() {
            return this.forceLineBreak;
        }

        public DialogPageItem setForceLineBreak(boolean forceLineBreak) {
            this.forceLineBreak = forceLineBreak;
            return this;
        }

        public String toString() {
            return gson.toJson(this);
        }

        public static DialogPageItem fromString(String string) {
            return gson.fromJson(string, DialogPageItem.class);
        }
    }

    public DialogPage(DialogPageItem... items) {
        this.items.addAll(Arrays.asList(items));
    }

    public DialogPage addItems(DialogPageItem... items) {
        this.items.addAll(Arrays.asList(items));
        return this;
    }

    public DialogPage setOpenCallback(Callback callback) {
        this.callback = callback;
        return this;
    }

    public ArrayList<DialogPageItem> items = new ArrayList<>();

    public Callback callback = null;

    public String build(ServerPlayerEntity player) {
        DialogHelper.buttonCallbacks.put(player.getUuid(), items.stream().map(item -> item.callback).filter(item -> item != null).toList());
        return gson.toJson(items.stream().map(DialogPageItem::toString).toList(), List.class);
    }

    public static List<DialogPageItem> decodeItems(String string) {
        return ((List<String>) gson.fromJson(string, List.class)).stream().map(subString -> DialogPageItem.fromString(subString)).toList();
    }
}
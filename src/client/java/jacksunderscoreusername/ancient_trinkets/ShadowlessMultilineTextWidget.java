package jacksunderscoreusername.ancient_trinkets;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AbstractTextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.CachedMapper;
import net.minecraft.util.Util;

import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
public class ShadowlessMultilineTextWidget extends AbstractTextWidget {
    private OptionalInt maxWidth = OptionalInt.empty();
    private OptionalInt maxRows = OptionalInt.empty();
    private final CachedMapper<ShadowlessMultilineTextWidget.CacheKey, MultilineText> cacheKeyToText;

    public ShadowlessMultilineTextWidget(Text message, TextRenderer textRenderer) {
        this(0, 0, message, textRenderer);
    }

    public ShadowlessMultilineTextWidget(int x, int y, Text message, TextRenderer textRenderer) {
        super(x, y, 0, 0, message, textRenderer);
        this.cacheKeyToText = Util.cachedMapper(
                cacheKey -> cacheKey.maxRows.isPresent()
                        ? MultilineText.create(textRenderer, cacheKey.maxWidth, cacheKey.maxRows.getAsInt(), cacheKey.message)
                        : MultilineText.create(textRenderer, cacheKey.message, cacheKey.maxWidth)
        );
        this.active = false;
    }

    public ShadowlessMultilineTextWidget setTextColor(int i) {
        super.setTextColor(i);
        return this;
    }

    public ShadowlessMultilineTextWidget setMaxWidth(int maxWidth) {
        this.maxWidth = OptionalInt.of(maxWidth);
        return this;
    }

    public ShadowlessMultilineTextWidget setMaxRows(int maxRows) {
        this.maxRows = OptionalInt.of(maxRows);
        return this;
    }

    @Override
    public int getWidth() {
        return this.cacheKeyToText.map(this.getCacheKey()).getMaxWidth();
    }

    @Override
    public int getHeight() {
        return this.cacheKeyToText.map(this.getCacheKey()).count() * 9;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MultilineText multilineText = this.cacheKeyToText.map(this.getCacheKey());
        int i = this.getX();
        int j = this.getY();
        int k = 9;
        int l = this.getTextColor();
        multilineText.draw(context, i, j, k, l);
    }

    private ShadowlessMultilineTextWidget.CacheKey getCacheKey() {
        return new ShadowlessMultilineTextWidget.CacheKey(this.getMessage(), this.maxWidth.orElse(Integer.MAX_VALUE), this.maxRows);
    }

    @Environment(EnvType.CLIENT)
    record CacheKey(Text message, int maxWidth, OptionalInt maxRows) {
    }
}

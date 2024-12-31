package jacksunderscoreusername.trinkets.quest;

import jacksunderscoreusername.trinkets.Main;
import jacksunderscoreusername.trinkets.dialog.DialogHelper;
import jacksunderscoreusername.trinkets.dialog.DialogPage;
import jacksunderscoreusername.trinkets.dialog.DialogScreenHandler;
import jacksunderscoreusername.trinkets.payloads.SendDialogPagePayload;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;

import java.util.List;

public class QuestManager {

    public static final ScreenHandlerType<DialogScreenHandler> DIALOG_SCREEN_HANDLER = Registry.register(Registries.SCREEN_HANDLER, Identifier.of(Main.MOD_ID, "dialog_screen"), new ScreenHandlerType<>(DialogScreenHandler::new, FeatureSet.empty()));

    public static void initialize() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && entity instanceof WanderingTraderEntity trader && trader.getOffers().stream().noneMatch((offer) -> {
                ItemStack stack = offer.getSellItem();
                WrittenBookContentComponent data = stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
                return data != null && data.title().raw().equals("Quest");
            })) {
                Main.state.data.questInitializedTraders.add(trader.getUuid());
                TradeOfferList oldOffers = trader.getOffers();
                ItemStack item = Items.WRITTEN_BOOK.getDefaultStack();
                item.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, new WrittenBookContentComponent(RawFilteredPair.of("Quest"), "Wandering Trader", 3, List.of(new RawFilteredPair[]{RawFilteredPair.of(Text.literal("No data"))}), true));
                oldOffers.add(new TradeOffer(new TradedItem(Items.EMERALD, world.random.nextBetween(16, 32)), item, 1, 0, 1));
            }
            return ActionResult.PASS;
        });

        UseEntityCallback.EVENT.register(((player, world, hand, entity, hitResult) -> {
            if (entity instanceof LivingEntity livingEntity && !player.isSneaking()) {
                if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
                    DialogPage page = new DialogPage();
                    page.addItems(
                            new DialogPage.DialogPageItem(Text.literal("Want some ").formatted(Formatting.ITALIC, Formatting.DARK_GRAY).append(Text.literal("golden").formatted(Formatting.ITALIC, Formatting.GOLD)).append(Text.literal(" apples?").formatted(Formatting.ITALIC, Formatting.DARK_GRAY))),
                            new DialogPage.DialogPageItem(Text.literal("It will cost you one diamond").formatted(Formatting.DARK_GRAY)));
                    page.addItems(new DialogPage.DialogPageItem(Text.literal("Sure!").formatted(Formatting.ITALIC), (subPlayer, inventory) -> {
                        if (inventory.getStack(0).isOf(Items.DIAMOND) && inventory.getStack(1).isEmpty()) {
                            inventory.getStack(0).decrement(1);
                            inventory.setStack(1, Items.GOLDEN_APPLE.getDefaultStack());
                            inventory.markDirty();
                        }
                    }, serverPlayer, Text.literal("Click to buy").formatted(Formatting.ITALIC)));
                    DialogHelper.openScreen(serverPlayer, livingEntity, page);
                }
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        }));

        DialogHelper.initialize();
        Paper.initialize();
    }
}

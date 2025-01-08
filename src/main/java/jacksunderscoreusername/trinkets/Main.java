package jacksunderscoreusername.trinkets;

import jacksunderscoreusername.trinkets.payloads.*;
import jacksunderscoreusername.trinkets.quest.QuestManager;
import jacksunderscoreusername.trinkets.trinkets.Trinkets;
import jacksunderscoreusername.trinkets.trinkets.breeze_core.UseBreezeCorePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Main implements ModInitializer {
    public static final String MOD_ID = "trinkets";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static MinecraftServer server = null;

    public static StateSaverAndLoader state = null;

    public static File configFile = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + "_config.json").toFile();
    public static Config config = Config.loadConfigFile(configFile);

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register((s) -> {
            server = s;
            state = StateSaverAndLoader.getServerState(s);
        });

        Trinkets.initialize();

        PayloadTypeRegistry.playS2C().register(SwingHandPayload.ID, SwingHandPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ConfigPayload.ID, ConfigPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SetDialogEntityPayload.ID, SetDialogEntityPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SendDialogPagePayload.ID, SendDialogPagePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DialogClickedPayload.ID, DialogClickedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CloseDialogPagePayload.ID, CloseDialogPagePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(UseBreezeCorePayload.ID, UseBreezeCorePayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sender.sendPacket(new ConfigPayload(config.toJsonString()));
        });

        Commands.initialize();
        QuestManager.initialize();
    }
}
//package jacksunderscoreusername.trinkets.shulker_swarm;
//
//import jacksunderscoreusername.trinkets.Main;
//import jacksunderscoreusername.trinkets.Trinket;
//import jacksunderscoreusername.trinkets.TrinketCreationHandlers;
//import jacksunderscoreusername.trinkets.TrinketDataComponent;
//import net.minecraft.entity.Entity;
//import net.minecraft.entity.EntityType;
//import net.minecraft.entity.SpawnReason;
//import net.minecraft.entity.player.PlayerEntity;
//import net.minecraft.util.ActionResult;
//import net.minecraft.util.Hand;
//import net.minecraft.util.Rarity;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.world.World;
//
//import static jacksunderscoreusername.trinkets.TrinketDataComponent.TRINKET_DATA;
//
//public class ShulkerSwarm extends Trinket {
//    public static String id = "shulker_swarm";
//
//    public String getId() {
//        return id;
//    }
//
//    public static Settings getSettings() {
//        return new Settings()
//                .maxCount(1)
//                .component(TRINKET_DATA, new TrinketDataComponent.TrinketData(1))
//                .rarity(Rarity.EPIC);
//    }
//
//    public ShulkerSwarm(Settings settings) {
//        super(settings);
//    }
//
//    public void initialize() {
//        TrinketCreationHandlers.OnMobKill(EntityType.SHULKER, 50, this);
//    }
//
//    @Override
//    public ActionResult use(World world, PlayerEntity user, Hand hand) {
//        if (world.isClient) {
//            return ActionResult.SUCCESS;
//        }
//        EntityType.SHULKER_BULLET.spawn(Main.server.getWorld(world.getRegistryKey()), BlockPos.ofFloored(user.raycast(25,0,true).getPos()), SpawnReason.MOB_SUMMONED);
//        return ActionResult.SUCCESS;
//    }
//}
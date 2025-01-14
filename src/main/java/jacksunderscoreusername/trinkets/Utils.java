package jacksunderscoreusername.trinkets;

import com.mojang.datafixers.util.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;

public class Utils {
    public static class BoundingBox {
        public BoundingBox(BlockPos max, BlockPos min) {
            this.max = max;
            this.min = min;
        }

        public BlockPos max;
        public BlockPos min;
    }

    public static BoundingBox getConnectedBlocksBoundingBox(RegistryKey<World> dim, BlockPos startPos, Block block) {
        World world = Main.server.getWorld(dim);
        ArrayList<BlockPos> scannedPoints = new ArrayList<>();
        ArrayList<BlockPos> pointsToScan = new ArrayList<>();
        pointsToScan.add(startPos);
        BlockPos[] offsets = {
                new BlockPos(1, 0, 0),
                new BlockPos(-1, 0, 0),
                new BlockPos(0, 1, 0),
                new BlockPos(0, -1, 0),
                new BlockPos(0, 0, 1),
                new BlockPos(0, 0, -1),
        };
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        while (!pointsToScan.isEmpty()) {
            BlockPos point = pointsToScan.removeFirst();
            scannedPoints.add(point);
            if (point.getX() > maxX) {
                maxX = point.getX();
            }
            if (point.getY() > maxY) {
                maxY = point.getY();
            }
            if (point.getZ() > maxZ) {
                maxZ = point.getZ();
            }
            if (point.getX() < minX) {
                minX = point.getX();
            }
            if (point.getY() < minY) {
                minY = point.getY();
            }
            if (point.getZ() < minZ) {
                minZ = point.getZ();
            }
            for (var offset : offsets) {
                BlockPos newPoint = point.add(offset);
                if (!scannedPoints.contains(newPoint)) {
                    assert world != null;
                    if (world.getBlockState(newPoint).getBlock().equals(block)) {
                        pointsToScan.add(newPoint);
                    }
                }
            }
        }
        return new BoundingBox(new BlockPos(maxX, maxY, maxZ), new BlockPos(minX, minY, minZ));
    }

    public static BoundingBox getConnectedBlocksBoundingBox(RegistryKey<World> dim, BlockPos startPos, Block block, Direction.Axis primaryAxis, Direction.Axis secondaryAxis) {
        World world = Main.server.getWorld(dim);
        BlockPos.Mutable minPos = startPos.mutableCopy();
        BlockPos.Mutable maxPos = startPos.mutableCopy();

        //This turns the Axis into the positive and negative directions. This allows us to search in those two directions.
        // if the axis = X then it would move in both the negative and positive X axis.
        Direction searchDirectionPrimaryNegative = Direction.get(Direction.AxisDirection.NEGATIVE, primaryAxis);
        Direction searchDirectionPrimaryPositive = searchDirectionPrimaryNegative.getOpposite();
        Direction searchDirectionSecondaryNegative = Direction.get(Direction.AxisDirection.NEGATIVE, secondaryAxis);
        Direction searchDirectionSecondaryPositive = searchDirectionSecondaryNegative.getOpposite();

        //TODO: optimization is possible here by moving diagonally and then only doing an end check for the directions to see where the block ends.
        //Move minPos to the minimum main axis direction.
        while (world.getBlockState(minPos).getBlock().equals(block)) {
            minPos.move(searchDirectionPrimaryNegative);
        }
        minPos.move(searchDirectionPrimaryPositive);

        //Move minPos to the minimum secondary axis direction.
        while (world.getBlockState(minPos).getBlock().equals(block)) {
            minPos.move(searchDirectionSecondaryNegative);
        }
        minPos.move(searchDirectionSecondaryPositive);


        //Move maxPos to the maximum main axis direction.
        while (world.getBlockState(maxPos).getBlock().equals(block)) {
            maxPos.move(searchDirectionPrimaryPositive);
        }
        maxPos.move(searchDirectionPrimaryNegative);

        //Move maxPos to the maximum secondary axis direction.
        while (world.getBlockState(maxPos).getBlock().equals(block)) {
            maxPos.move(searchDirectionSecondaryPositive);
        }
        maxPos.move(searchDirectionSecondaryNegative);

        return new BoundingBox(maxPos, minPos);
    }

    public static boolean areBothPointsConnected(BlockPos pos1, RegistryKey<World> dim1, BlockPos pos2, RegistryKey<World> dim2, Block block) {
        return areBothPointsConnected(pos1, dim1, pos2, dim2, block, new BlockPos[]{
                new BlockPos(1, 0, 0),
                new BlockPos(-1, 0, 0),
                new BlockPos(0, 1, 0),
                new BlockPos(0, -1, 0),
                new BlockPos(0, 0, 1),
                new BlockPos(0, 0, -1)
        });
    }

    public static boolean areBothPointsConnected(BlockPos pos1, RegistryKey<World> dim1, BlockPos pos2, RegistryKey<World> dim2, Block block, Direction.Axis excludedAxis) {
        if (excludedAxis.equals(Direction.Axis.X)) {
            return areBothPointsConnected(pos1, dim1, pos2, dim2, block, new BlockPos[]{
                    new BlockPos(0, 1, 0),
                    new BlockPos(0, -1, 0),
                    new BlockPos(0, 0, 1),
                    new BlockPos(0, 0, -1)
            });
        }
        if (excludedAxis.equals(Direction.Axis.Y)) {
            return areBothPointsConnected(pos1, dim1, pos2, dim2, block, new BlockPos[]{
                    new BlockPos(1, 0, 0),
                    new BlockPos(-1, 0, 0),
                    new BlockPos(0, 0, 1),
                    new BlockPos(0, 0, -1)
            });
        }
        if (excludedAxis.equals(Direction.Axis.Z)) {
            return areBothPointsConnected(pos1, dim1, pos2, dim2, block, new BlockPos[]{
                    new BlockPos(1, 0, 0),
                    new BlockPos(-1, 0, 0),
                    new BlockPos(0, 1, 0),
                    new BlockPos(0, -1, 0)
            });
        }
        throw new RuntimeException("Invalid axis");
    }

    public static boolean areBothPointsConnected(BlockPos pos1, RegistryKey<World> dim1, BlockPos pos2, RegistryKey<World> dim2, Block block, BlockPos[] lookOffsets) {
        if (!dim1.equals(dim2)) {
            return false;
        }
        World world = Main.server.getWorld(dim1);
        ArrayList<BlockPos> scannedPoints = new ArrayList<>();
        ArrayList<BlockPos> pointsToScan = new ArrayList<>();
        pointsToScan.add(pos2);
        while (!pointsToScan.isEmpty()) {
            BlockPos point = pointsToScan.removeFirst();
            if (point.equals(pos1)) return true;
            scannedPoints.add(point);
            for (var offset : lookOffsets) {
                BlockPos newPoint = point.add(offset);
                if (!scannedPoints.contains(newPoint)) {
                    assert world != null;
                    if (world.getBlockState(newPoint).getBlock().equals(block)) {
                        pointsToScan.add(newPoint);
                    }
                }
            }
        }
        return false;
    }

    public static void fillArea(World world, Block block, BoundingBox bounds, @Nullable Consumer<BlockEntity> entitySetter) {
        fillArea(world, block.getDefaultState(), bounds, entitySetter);
    }

    public static void fillArea(World world, BlockState state, BoundingBox bounds, @Nullable Consumer<BlockEntity> entitySetter) {
        for (var x = bounds.min.getX(); x <= bounds.max.getX(); x++) {
            for (var y = bounds.min.getY(); y <= bounds.max.getY(); y++) {
                for (var z = bounds.min.getZ(); z <= bounds.max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    world.setBlockState(pos, state);

                    if (entitySetter != null) {
                        entitySetter.accept(world.getBlockEntity(pos));
                    }
                }
            }
        }
    }

    /**
     * @param hue        0-360
     * @param saturation 0-1
     * @param lightness  0-1
     * @return The color in RGB format as an int 0 - 256^3-1
     */
    public static int hslToRgb(double hue, double saturation, double lightness) {
        if (hue < 0 || hue > 360 || saturation < 0 || saturation > 1 || lightness < 0 || lightness > 1) {
            throw new IllegalArgumentException("Hue must be 0-360, saturation and lightness must be 0.0-1.0.");
        }

        double c = (1 - Math.abs(2 * lightness - 1)) * saturation;
        double x = c * (1 - Math.abs((hue / 60) % 2 - 1));
        double m = lightness - c / 2;

        double r = 0, g = 0, b = 0;
        if (hue < 60) {
            r = c;
            g = x;
            b = 0;
        } else if (hue < 120) {
            r = x;
            g = c;
            b = 0;
        } else if (hue < 180) {
            r = 0;
            g = c;
            b = x;
        } else if (hue < 240) {
            r = 0;
            g = x;
            b = c;
        } else if (hue < 300) {
            r = x;
            g = 0;
            b = c;
        } else {
            r = c;
            g = 0;
            b = x;
        }

        int red = (int) Math.round((r + m) * 255);
        int green = (int) Math.round((g + m) * 255);
        int blue = (int) Math.round((b + m) * 255);

        return (red << 16) | (green << 8) | blue;
    }

    public static String prettyTime(int timeInSeconds, boolean exact) {
        int seconds = 0;
        int minutes = 0;
        int hours = 0;
        if (timeInSeconds / 60 / 60 >= 1) {
            hours += timeInSeconds / 60 / 60;
            timeInSeconds = timeInSeconds % (60 * 60);
        }
        if (timeInSeconds / 60 >= 1) {
            minutes += timeInSeconds / 60;
            timeInSeconds = timeInSeconds % 60;
        }
        seconds += timeInSeconds;
        int typeCount = 0;
        if (seconds > 0) {
            typeCount++;
        }
        if (minutes > 0) {
            typeCount++;
        }
        if (hours > 0) {
            typeCount++;
        }
        String out = "";
        if (exact) {
            if (hours > 0) {
                out += hours + " hour" + (hours == 1 ? "" : "s");
            }
            if (minutes > 0) {
                out += (typeCount == 2 ? hours > 0 ? " and " : "" : typeCount == 3 ? ", " : "") + minutes + " minute" + (minutes == 1 ? "" : "s");
            }
            if (seconds > 0) {
                out += (typeCount == 2 ? minutes > 0 ? " and " : "" : typeCount == 3 ? ", and " : "") + seconds + " second" + (seconds == 1 ? "" : "s");
            }
        } else {
            if (hours > 1 || (hours == 1 && minutes == 0)) {
                hours = Math.round(hours + minutes / 60F);
                out += hours + " hour" + (hours == 1 ? "" : "s");
            } else if (hours == 1) {
                out += hours + " hour ";
                out += minutes + " minute" + (minutes == 1 ? "" : "s");
            } else if (minutes > 1 || (minutes == 1 && seconds == 0)) {
                minutes = Math.round(minutes + seconds / 60F);
                out += minutes + " minute" + (minutes == 1 ? "" : "s");
            } else if (minutes == 1) {
                out += minutes + " minute ";
                out += seconds + " second" + (seconds == 1 ? "" : "s");
            } else if (seconds > 0) {
                out += seconds + " second" + (seconds == 1 ? "" : "s");
            }
        }
        return out;
    }

    public static Optional<Pair<BlockPos, RegistryEntry<Structure>>> findStructure(ServerWorld world, BlockPos pos, RegistryKey<Structure> key, int radius, boolean generateNew) {
        RegistryEntryList<Structure> registryEntryList = RegistryEntryList.of(world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE).getOrThrow(key));
        Pair<BlockPos, RegistryEntry<Structure>> pair = world.getChunkManager().getChunkGenerator().locateStructure(world, registryEntryList, pos, radius, generateNew);
        if (generateNew && pair != null && world.getChunk(pair.getFirst().getX() / 16, pair.getFirst().getZ() / 16).getInhabitedTime() != 0) {
            return findStructure(world, pos, key, radius, true);
        }
        return Optional.ofNullable(pair);
    }
}

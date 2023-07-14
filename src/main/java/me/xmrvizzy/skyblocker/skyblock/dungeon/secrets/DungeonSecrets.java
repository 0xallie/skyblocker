package me.xmrvizzy.skyblocker.skyblock.dungeon.secrets;

import com.google.gson.JsonObject;
import me.xmrvizzy.skyblocker.SkyblockerMod;
import me.xmrvizzy.skyblocker.config.SkyblockerConfig;
import me.xmrvizzy.skyblocker.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.InflaterInputStream;

public class DungeonSecrets {
    protected static final Logger LOGGER = LoggerFactory.getLogger(DungeonSecrets.class);
    private static final String DUNGEONS_DATA_DIR = "/assets/skyblocker/dungeons";
    private static final HashMap<String, HashMap<String, HashMap<String, int[]>>> ROOMS = new HashMap<>();
    /**
     * Maps the block identifier string to a custom numeric block id used in dungeon rooms data.
     * @implNote Not using {@link net.minecraft.registry.Registry#getId(Object) Registry#getId(Block)} and {@link net.minecraft.block.Blocks Blocks} since this is also used by {@link me.xmrvizzy.skyblocker.skyblock.dungeon.secrets.DungeonRoomsDFU DungeonRoomsDFU}, which runs outside of Minecraft.
     */
    @SuppressWarnings("JavadocReference")
    protected static final Map<String, Byte> NUMERIC_ID = Map.ofEntries(
            Map.entry("minecraft:stone", (byte) 1),
            Map.entry("minecraft:diorite", (byte) 2),
            Map.entry("minecraft:polished_diorite", (byte) 3),
            Map.entry("minecraft:andesite", (byte) 4),
            Map.entry("minecraft:polished_andesite", (byte) 5),
            Map.entry("minecraft:grass_block", (byte) 6),
            Map.entry("minecraft:dirt", (byte) 7),
            Map.entry("minecraft:coarse_dirt", (byte) 8),
            Map.entry("minecraft:cobblestone", (byte) 9),
            Map.entry("minecraft:bedrock", (byte) 10),
            Map.entry("minecraft:oak_leaves", (byte) 11),
            Map.entry("minecraft:gray_wool", (byte) 12),
            Map.entry("minecraft:double_stone_slab", (byte) 13),
            Map.entry("minecraft:mossy_cobblestone", (byte) 14),
            Map.entry("minecraft:clay", (byte) 15),
            Map.entry("minecraft:stone_bricks", (byte) 16),
            Map.entry("minecraft:mossy_stone_bricks", (byte) 17),
            Map.entry("minecraft:chiseled_stone_bricks", (byte) 18),
            Map.entry("minecraft:gray_terracotta", (byte) 19),
            Map.entry("minecraft:cyan_terracotta", (byte) 20),
            Map.entry("minecraft:black_terracotta", (byte) 21)
    );
    private static JsonObject roomsJson;
    private static JsonObject waypointsJson;
    @Nullable
    private static CompletableFuture<Void> roomsLoaded;
    private static Vector2i mapEntrancePos;
    private static int mapRoomWidth;

    public static boolean isRoomsLoaded() {
        return roomsLoaded != null && roomsLoaded.isDone();
    }

    /**
     * Loads the dungeon secrets asynchronously from {@code /assets/skyblocker/dungeons}.
     * Use {@link #isRoomsLoaded()} to check for completion of loading.
     */
    public static void init() {
        if (SkyblockerConfig.get().locations.dungeons.noLoadSecretWaypoints) {
            return;
        }
        CompletableFuture.runAsync(DungeonSecrets::load);
        SkyblockerMod.getInstance().scheduler.scheduleCyclic(DungeonSecrets::update, 10);
    }

    private static void load() {
        try {
            List<CompletableFuture<Void>> dungeonFutures = new ArrayList<>();
            //noinspection DataFlowIssue
            File dungeons = new File(SkyblockerMod.class.getResource(DUNGEONS_DATA_DIR).getFile());
            int resourcePathIndex = dungeons.getPath().indexOf(DUNGEONS_DATA_DIR);
            //noinspection DataFlowIssue
            for (File dungeon : dungeons.listFiles()) {
                if (!dungeon.isDirectory()) {
                    continue;
                }
                File[] roomShapes = dungeon.listFiles();
                if (roomShapes == null) {
                    LOGGER.error("Failed to load dungeon secrets for dungeon {}", dungeon.getName());
                    continue;
                }
                ROOMS.put(dungeon.getName(), new HashMap<>());
                List<CompletableFuture<Void>> roomShapeFutures = new ArrayList<>();
                for (File roomShape : roomShapes) {
                    roomShapeFutures.add(CompletableFuture.supplyAsync(() -> readRooms(roomShape, resourcePathIndex)).thenAccept(rooms -> ROOMS.get(dungeon.getName()).put(roomShape.getName(), rooms)));
                }
                dungeonFutures.add(CompletableFuture.allOf(roomShapeFutures.toArray(CompletableFuture[]::new)).thenRun(() -> LOGGER.debug("Loaded dungeon secrets for dungeon {} with {} room shapes and {} rooms total", dungeon.getName(), ROOMS.get(dungeon.getName()).size(), ROOMS.get(dungeon.getName()).values().stream().mapToInt(HashMap::size).sum())));
            }
            // Execute with MinecraftClient as executor since we need to wait for MinecraftClient#resourceManager to be set
            dungeonFutures.add(CompletableFuture.runAsync(() -> {
                try (BufferedReader roomsReader = MinecraftClient.getInstance().getResourceManager().openAsReader(new Identifier(SkyblockerMod.NAMESPACE, "dungeons/dungeonrooms.json")); BufferedReader waypointsReader = MinecraftClient.getInstance().getResourceManager().openAsReader(new Identifier(SkyblockerMod.NAMESPACE, "dungeons/secretlocations.json"))) {
                    roomsJson = SkyblockerMod.GSON.fromJson(roomsReader, JsonObject.class);
                    waypointsJson = SkyblockerMod.GSON.fromJson(waypointsReader, JsonObject.class);
                    LOGGER.debug("Loaded dungeon secrets json");
                } catch (Exception e) {
                    LOGGER.error("Failed to load dungeon secrets json", e);
                }
            }, MinecraftClient.getInstance()));
            roomsLoaded = CompletableFuture.allOf(dungeonFutures.toArray(CompletableFuture[]::new)).thenRun(() -> LOGGER.info("Loaded dungeon secrets for {} dungeon(s), {} room shapes, and {} rooms total", ROOMS.size(), ROOMS.values().stream().mapToInt(HashMap::size).sum(), ROOMS.values().stream().map(HashMap::values).flatMap(Collection::stream).mapToInt(HashMap::size).sum()));
        } catch (Exception e) {
            LOGGER.error("Failed to load dungeon secrets", e);
        }
    }

    private static HashMap<String, int[]> readRooms(File roomShape, int resourcePathIndex) {
        HashMap<String, int[]> data = new HashMap<>();
        File[] rooms = roomShape.listFiles();
        if (rooms == null) {
            LOGGER.error("Failed to load dungeon secrets room shape {}", roomShape.getName());
            return data;
        }
        for (File room : rooms) {
            String name = room.getName();
            //noinspection DataFlowIssue
            try (ObjectInputStream in = new ObjectInputStream(new InflaterInputStream(SkyblockerMod.class.getResourceAsStream(room.getPath().substring(resourcePathIndex))))) {
                data.put(name.substring(0, name.length() - 9), (int[]) in.readObject());
                LOGGER.debug("Loaded dungeon secrets room {}", name);
            } catch (NullPointerException | IOException | ClassNotFoundException e) {
                LOGGER.error("Failed to load dungeon secrets room " + name, e);
            }
        }
        LOGGER.debug("Loaded dungeon secrets room shape {} with {} rooms", roomShape.getName(), data.size());
        return data;
    }

    private static void update() {
        if (!SkyblockerConfig.get().locations.dungeons.secretWaypoints || !Utils.isInDungeons()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }
        ItemStack stack = client.player.getInventory().main.get(8);
        if (!stack.isOf(Items.FILLED_MAP)) {
            return;
        }
        MapState map = FilledMapItem.getMapState(FilledMapItem.getMapId(stack), client.world);
        if (map == null) {
            return;
        }
        if (mapEntrancePos == null && (mapEntrancePos = DungeonMapUtils.getEntrancePos(map)) == null) {
            return;
        }
        if (mapRoomWidth == 0 && (mapRoomWidth = DungeonMapUtils.getRoomWidth(map, mapEntrancePos)) == 0) {
            return;
        }
    }
}

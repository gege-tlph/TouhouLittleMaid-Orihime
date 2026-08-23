package com.github.tartaricacid.touhoulittlemaid.datapack.resources;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.datapack.BoardStateData;
import com.github.tartaricacid.touhoulittlemaid.datapack.pojo.BoardStateRecord;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

/**
 * 读取数据包里的预设棋谱（{@code data/<ns>/board_states/*.json}）。
 *
 * <p>逐层读取而不是只取最上层：多个数据包可以各自追加棋谱，合并加载。</p>
 *
 * <p>写法对新基：26.1 的 Fabric API 在 {@code registerReloadListener(Identifier, listener)}
 * 那一步就把 id 传进去了，所以**不需要**行为基准那边的 {@code IdentifiableResourceReloadListener}
 * 与 {@code getFabricId()}——与宿主自己的 {@code KaomojiDataReloadListener} 保持一致。</p>
 */
public class BoardStateDataReloadListener implements ResourceManagerReloadListener {
    private static final Identifier CHESS_PATH = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "board_states/chess.json");
    private static final Identifier XIANGQI_PATH = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "board_states/xiangqi.json");
    private static final Identifier GOMOKU_PATH = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "board_states/gomoku.json");

    private static final Gson GSON = new Gson();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        // 先清除旧数据
        BoardStateData.clear();
        // 再逐层读取新数据，进行合并加载
        resourceManager.listPacks().forEach(packResources -> {
            readData(packResources, CHESS_PATH, BoardStateData::addChessRecords);
            readData(packResources, XIANGQI_PATH, BoardStateData::addXiangqiRecords);
            readData(packResources, GOMOKU_PATH, BoardStateData::addGomokuRecords);
        });
    }

    private static void readData(PackResources packResources, Identifier path, Consumer<List<BoardStateRecord>> adder) {
        IoSupplier<InputStream> resource = packResources.getResource(PackType.SERVER_DATA, path);
        if (resource == null) {
            return;
        }
        try (InputStream inputStream = resource.get();
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            List<BoardStateRecord> records = GSON.fromJson(reader, new TypeToken<List<BoardStateRecord>>() {
            }.getType());
            if (records != null && !records.isEmpty()) {
                adder.accept(records);
            }
        } catch (Exception exception) {
            // 单个数据包坏掉不许拖垮整次重载：其余包照常合并，坏的那个记日志
            TouhouLittleMaid.LOGGER.error("Failed to load board state data from {}", path, exception);
        }
    }
}

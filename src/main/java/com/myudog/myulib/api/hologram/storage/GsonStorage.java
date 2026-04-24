package com.myudog.myulib.api.hologram.storage;

import net.minecraft.server.MinecraftServer;

public class GsonStorage implements Storage {

    /**
     * 🌟 執行存檔：將當前註冊表序列化為 JSON
     */
    @Override
    public static void saveToFile(MinecraftServer server) {
        // 獲取該世界的根目錄路徑
        Path savePath = server.getWorldPath(LevelResource.ROOT).resolve("myulib/holograms.json");
        File file = savePath.toFile();

        // 確保目錄存在
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        try (FileWriter writer = new FileWriter(file)) {
            // 將整個 Map 轉換為 JSON 字串
            Collection<HologramDefinition> data = com.myudog.myulib.api.hologram.HologramManager.INSTANCE.all().values();
            GsonProvider.GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 🌟 執行讀取：從 JSON 還原註冊表
     */
    public static void loadFromFile(MinecraftServer server) {
        Path loadPath = server.getWorldPath(LevelResource.ROOT).resolve("myulib/holograms.json");
        File file = loadPath.toFile();

        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file)) {
            // 定義要還原的資料型別 (List<HologramDefinition>)
            var type = new TypeToken<ArrayList<HologramDefinition>>(){}.getType();
            ArrayList<HologramDefinition> loadedData = GsonProvider.GSON.fromJson(reader, type);

            if (loadedData != null) {
                ;
                for (HologramDefinition def : loadedData) {
                    com.myudog.myulib.api.hologram.HologramManager.INSTANCE.register(def);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

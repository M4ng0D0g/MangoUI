package com.myudog.myulib.api.hologram;

import com.myudog.myulib.api.game.object.impl.BaseGameObject;

public class HologramObject extends BaseGameObject {
    public static final GameObjectProperty<String> TEXT = new GameObjectProperty<>("text", String.class, s -> s);

    public HologramObject(Identifier id) { super(id, GameObjectKind.DECORATIVE); }

    @Override
    public void onInitialize(GameInstance<?, ?, ?> instance) {
        // 註冊到房間的 HologramManager
        // 當有玩家加入這個房間時，Manager 會自動把這個 Hologram 打包成 Packet 射給該玩家
        instance.getData().getHologramManager().addHologram(this.getId(), get(POS), get(TEXT));
    }

    // 提供給 GameState 更新文字的 API (例如：倒數計時 10, 9, 8...)
    public void updateText(GameInstance<?, ?, ?> instance, String newText) {
        instance.getData().getHologramManager().updateText(this.getId(), newText);
        // Manager 內部會負責廣播 UpdateHologramPacket 給房間內的所有玩家
    }
}
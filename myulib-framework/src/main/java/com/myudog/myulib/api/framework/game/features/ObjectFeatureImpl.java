package com.myudog.myulib.api.framework.game.features;

import com.myudog.myulib.api.framework.game.core.GameInstance;
import com.myudog.myulib.api.core.object.IObjectDef;
import com.myudog.myulib.api.core.object.IObjectRt;
import com.myudog.myulib.api.core.object.ObjectManager;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ObjectFeatureImpl implements ObjectFeature {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectFeatureImpl.class.getName());

    // ?? 敹?雿輻 ConcurrentHashMap嚗Ⅱ靽??瑁?蝺??鈭辣??Tick ?湔???冽?
    private final Map<Identifier, IObjectRt> runtimeObjects = new ConcurrentHashMap<>();

    @Override
    public void addRuntimeObject(@NotNull Identifier instanceId, @NotNull IObjectRt obj) {
        this.runtimeObjects.put(instanceId, obj);
    }

    @Override
    public Optional<IObjectRt> getObject(@NotNull Identifier instanceId) {
        return Optional.ofNullable(runtimeObjects.get(instanceId));
    }

    @Override
    public Collection<IObjectRt> getRuntimeObjects() {
        return Collections.unmodifiableCollection(runtimeObjects.values());
    }

    /**
     * ?? ?詨????摩嚗?????撖阡?
     * @param instance ?嗅????脣祕靘?(??銝???銝?)
     * @param defId    ????ID (撠? ObjectManager 銝剔? ObjectDef)
     * @param instanceId ??Runtime ?拐辣?銝霅蝣?(靘? "zombie_spawner_1")
     * @return ????Runtime ?拐辣
     */
    @Override
    public IObjectRt spawnObject(GameInstance<?, ?, ?> instance, Identifier defId, Identifier instanceId) {
        // 1. 敺?恣??脣???
        IObjectDef def = ObjectManager.INSTANCE.getDefinition(defId);
        if (def == null) {
            throw new IllegalArgumentException("?⊥????拐辣嚗銝撠???ObjectDef: " + defId);
        }

        // 2. ?澆???極撱瘜?(銝??閬??instance)
        IObjectRt rtObj = def.spawn();

        // 3. ???蒂??撖虫?
        rtObj.onInitialize();
        rtObj.spawn();

        // 4. ?脣??單?啗蕭頩文
        this.runtimeObjects.put(instanceId, rtObj);

        return rtObj;
    }

    /**
     * ?? ?詨?皜??摩嚗???GameData ?貉????
     * 鞎痊?澆???Runtime ?拐辣??destroy嚗誑蝘駁 Minecraft 撖阡????憛?
     */
    @Override
    public void clean(GameInstance<?, ?, ?> instance) {
        for (Map.Entry<Identifier, IObjectRt> entry : runtimeObjects.entrySet()) {
            try {
                entry.getValue().destroy();
            } catch (Exception e) {
                LOGGER.error("?瑟? Runtime ?拐辣??隤? {}", entry.getKey(), e);
            }
        }
        this.runtimeObjects.clear();
    }
}

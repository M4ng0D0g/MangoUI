

```java

// 定義一個簡單的生命值組件
public class HealthComponent implements IComponent {
    public float currentHealth;
    public float maxHealth;

    public HealthComponent(float current, float max) {
        this.currentHealth = current;
        this.maxHealth = max;
    }
}

// 實作該組件的序列化規則，讓它可以被寫入 NBT 存檔
public class HealthSerializer implements ComponentSerializer<HealthComponent> {
    @Override
    public Tag serialize(HealthComponent component) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("current", component.currentHealth);
        tag.putFloat("max", component.maxHealth);
        return tag;
    }

    @Override
    public HealthComponent deserialize(Tag tag) {
        CompoundTag compound = (CompoundTag) tag;
        return new HealthComponent(compound.getFloat("current"), compound.getFloat("max"));
    }
}

```

```java

public class MyGameEngine {
    
    // 全域的 ECS 容器
    public static final EcsContainer ECS = new EcsContainer();

    public void initialize() {
        // 1. 註冊序列化器 (為了存檔與讀檔)
        ECS.registerSerializer(HealthComponent.class, new HealthSerializer());

        // 2. 掛載持久化儲存機制 (假設你有一個實作了 DataStorage 的 NbtStorageProvider)
        // ECS.install(new NbtStorageProvider());

        // 3. 🌟 註冊 EventBus 監聽器 🌟
        // 當有實體被掛上任何組件時，這個監聽器就會被觸發
        ECS.eventBus.subscribe(ComponentAddedEvent.class, event -> {
            // 檢查被加入的是不是 HealthComponent
            if (event.getComponent() instanceof HealthComponent health) {
                System.out.println("實體 [" + event.getEntityId() + "] 被賦予了生命值組件！");
                System.out.println("當前血量: " + health.currentHealth + "/" + health.maxHealth);
                
                // 這裡可以觸發 UI 更新、播放音效，或為實體加上受傷無敵時間的計時器
            }
        });
    }

    public void spawnZombie() {
        // 4. 創建實體 (獲取一個新的整數 ID)
        int zombieId = ECS.createEntity();

        // 5. 為實體掛上組件
        // 👉 這行執行時，底層會將資料存入 ComponentStorage，
        // 👉 並且觸發 ComponentAddedEvent，進而印出上方監聽器的訊息。
        ECS.addComponent(zombieId, HealthComponent.class, new HealthComponent(20.0f, 20.0f));

        // 6. 日後獲取組件來修改資料 (例如殭屍受到攻擊)
        HealthComponent zombieHealth = ECS.getComponent(zombieId, HealthComponent.class);
        if (zombieHealth != null) {
            zombieHealth.currentHealth -= 5.0f;
        }
    }
}

```
package com.myudog.myulib.client.gui.test;

import com.myudog.myulib.client.gui.MyuUI;
import com.myudog.myulib.client.gui.base.RenderContext;
import com.myudog.myulib.client.gui.base.Widget;
import com.myudog.myulib.client.gui.layout.FlexContainer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * TestUIScreen
 * 
 * 用於測試 MyuUI 系統的螢幕。
 */
public class TestUIScreen extends Screen {
    private Widget root;

    public TestUIScreen() {
        super(Component.literal("MyuUI Test"));
    }

    @Override
    protected void init() {
        FlexContainer column = MyuUI.column();
        column.setX(width / 2 - 50);
        column.setY(height / 2 - 50);
        column.setGap(10);

        column.addChild(MyuUI.label("MyuUI Demo").centered(true));
        column.addChild(MyuUI.button("Click Me!", () -> {
            System.out.println("Button Clicked!");
        }));
        
        column.addChild(MyuUI.button("Close", this::onClose));

        this.root = column;
    }

    // 暫時註解掉，因為 Minecraft 版本或 UI 框架方法簽名不相符
    /*
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractBackground(graphics, mouseX, mouseY, partialTick);
        
        if (root != null) {
            RenderContext ctx = new RenderContext(graphics, this.font, mouseX, mouseY, partialTick);
            root.render(ctx);
        }
        
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (root != null && root.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    */
}

package com.myudog.myulib.api.framework.ui.sidebar;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ?瑕???? (Zero-Flicker)???嗥???犖閮???
 */
public class VirtualSidebar {
    private final ServerPlayer player;
    private final String objectiveName;
    private Component title;

    // ?? 敹怠????閮??嗅??恍銝祕??撟曇?嚗靘?撌桃瘥?????
    private int currentLineCount = 0;
    private boolean isVisible = false;

    public VirtualSidebar(ServerPlayer player, String objectiveName, Component title) {
        this.player = player;
        this.objectiveName = objectiveName;
        this.title = title;
    }

    public void show() {
        if (isVisible) return;

        Objective dummyObjective = new Objective(
                null, this.objectiveName, ObjectiveCriteria.DUMMY, this.title, ObjectiveCriteria.RenderType.INTEGER, false, null
        );
        player.connection.send(new ClientboundSetObjectivePacket(dummyObjective, 0)); // Action 0: Create

        // 1.20.2+ 雿輻 DisplaySlot Enum
        player.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, dummyObjective));

        isVisible = true;
    }

    public void hide() {
        if (!isVisible) return;

        Objective dummyObjective = new Objective(null, this.objectiveName, ObjectiveCriteria.DUMMY, this.title, ObjectiveCriteria.RenderType.INTEGER, false, null);
        player.connection.send(new ClientboundSetObjectivePacket(dummyObjective, 1)); // Action 1: Remove

        isVisible = false;
        currentLineCount = 0; // ?蔭???
    }

    public void updateTitle(Component newTitle) {
        this.title = newTitle;
        if (!isVisible) return;

        Objective dummyObjective = new Objective(null, this.objectiveName, ObjectiveCriteria.DUMMY, this.title, ObjectiveCriteria.RenderType.INTEGER, false, null);
        player.connection.send(new ClientboundSetObjectivePacket(dummyObjective, 2)); // Action 2: Update
    }

    /**
     * ?? ?園????詨??湔瞍?瘜?
     */
    public void updateLines(List<Component> newLines) {
        if (!isVisible) return;

        int newSize = newLines.size();

        // 1. ?湔?憓???
        for (int i = 0; i < newSize; i++) {
            // 雿輻?箏????ID (靘? "line_00", "line_01")
            String slotId = String.format("line_%02d", i);

            // 閮??函?Ｖ????豢?摨?(頞??Ｗ??貉?擃?蝣箔??曹?敺銝?)
            int scoreValue = 15 - i;

            // ?? 1.20.3+ 撠惇撠?嚗?摰?slotId嚗?憿舐內 newLines.get(i) ??摮?
            // ClientboundSetScorePacket(?拙振?迂/ID, 閮??踹?蝔? ?, 憿舐內??, ?詨??澆?)
            player.connection.send(new ClientboundSetScorePacket(
                    slotId,
                    this.objectiveName,
                    scoreValue,
                    Optional.of(newLines.get(i)),
                    null
            ));
        }

        // 2. 皜憭???銵 (Delta Cleanup)
        // ?身???5 銵??活?湔?芸?乩? 3 銵??????? line_03 ??line_04 ?芷??
        if (currentLineCount > newSize) {
            for (int i = newSize; i < currentLineCount; i++) {
                String obsoleteSlotId = String.format("line_%02d", i);
                // ?喲?Reset 撠?靘?日摰? ID
                player.connection.send(new ClientboundResetScorePacket(obsoleteSlotId, this.objectiveName));
            }
        }

        // 3. ?湔敹怠????
        currentLineCount = newSize;
    }
}

package com.myudog.myulib.mixin;

import com.myudog.myulib.api.field.FieldDefinition;
import com.myudog.myulib.api.field.FieldManager;
import com.myudog.myulib.api.permission.PermissionAction;
import com.myudog.myulib.api.permission.PermissionDecision;
import com.myudog.myulib.api.permission.PermissionManager;
import com.myudog.myulib.api.rolegroup.RoleGroupManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Player.class)
public abstract class MixinPlayerEntity {

    // 🎯 攔截 3: 攻擊實體 (包含近戰與玩家射出的弓箭命中時判定)
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(Entity target, CallbackInfo ci) {
        Player attacker = (Player) (Object) this;
        Level level = attacker.level();

        // 核心：使用「目標生物的位置」找 Field！
        // 如果玩家站在區外，朝安全區射箭，這裡的 target.position() 會是安全區內，觸發保護！
        Identifier dimId = level.dimension().identifier();
        Optional<FieldDefinition> targetField = FieldManager.findAt(dimId, target.position());

        // 判斷攻擊目標類型
        PermissionAction action = PermissionAction.ATTACK_HOSTILE_MOB;
        if (target instanceof Player) action = PermissionAction.ATTACK_PLAYER;
        else if (target instanceof Animal || target instanceof Villager) action = PermissionAction.ATTACK_FRIENDLY_MOB;

        PermissionDecision decision = PermissionManager.evaluate(
                attacker.getUUID(),
                RoleGroupManager.getSortedGroupIdsOf(attacker.getUUID()),
                action,
                targetField.map(FieldDefinition::id).orElse(null),
                dimId
        );

        if (decision == PermissionDecision.DENY) {
            ci.cancel(); // 武器揮空，傷害無效化
        }
    }
}

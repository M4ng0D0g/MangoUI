package com.myudog.myulib.api.framework.permission;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PermissionScope
 *
 * 蝟餌絞嚗??恣?頂蝯?(Framework - Permission)
 * 閫嚗誨銵其??摰??典?嚗?嚗?摰雁摨艾摰?堆??抒???????捆?具?
 * 憿?嚗ata Container / Local Resolver
 *
 * `PermissionScope` ?折蝬剛風鈭摰嗅犖??????{@link PermissionTable}??
 * 摰?鞎砍?桐?雿?惜蝝?瑁?閫???摩??
 */
public class PermissionScope {
    /** ?拙振?犖???”?? (Player UUID -> Table)??*/
    private final Map<UUID, PermissionTable> playerTables = new HashMap<>();

    /** 甈?蝯?甈?銵冽?撠?(Group Name -> Table)??*/
    private final Map<String, PermissionTable> groupTables = new HashMap<>();

    /**
     * ?脣??遣蝡?摰摰嗥??犖甈?銵具?
     */
    public PermissionTable forPlayer(UUID playerId) {
        return playerTables.computeIfAbsent(playerId, k -> new PermissionTable());
    }

    /**
     * ?脣??遣蝡?摰??????”??
     */
    public PermissionTable forGroup(String groupName) {
        return groupTables.computeIfAbsent(groupName, k -> new PermissionTable());
    }

    public Map<UUID, PermissionTable> playerTablesSnapshot() {
        return Map.copyOf(playerTables);
    }

    public Map<String, PermissionTable> groupTablesSnapshot() {
        return Map.copyOf(groupTables);
    }

    /**
     * 閫??甇支??典??抒??蝯???摰?
     *
     * 閫???芸???嚗?
     * 1. ?拙振?嫣? (Player Override)嚗?拙振?犖銵其葉?身摰??誑甇斤皞?
     * 2. ?拙振?撅祈澈?? (Groups)嚗??批?亦????風?拙振?撅祉?蝯嚗?蝚砌???閮剖?????
     * 3. ?身蝯?(everyone)嚗隞乩??閮剖?嚗??迨雿???身蝯身摰?
     *
     * @param playerId     ?拙振 UUID
     * @param playerGroups ?拙振?桀???????嚗歇??嚗?
     * @param action       閬炎?亦???
     * @return 鋆?蝯? (ALLOW, DENY, UNSET)
     */
    public PermissionDecision resolve(UUID playerId, Iterable<String> playerGroups, PermissionAction action) {
        // 1. ?亥岷?拙振?嫣?
        if (playerTables.containsKey(playerId)) {
            PermissionDecision decision = playerTables.get(playerId).get(action);
            if (decision != PermissionDecision.UNSET) return decision;
        }

        // 2. ?亥岷?拙振?瑟??澈??甈?
        for (String group : playerGroups) {
            if (groupTables.containsKey(group)) {
                PermissionDecision decision = groupTables.get(group).get(action);
                if (decision != PermissionDecision.UNSET) return decision;
            }
        }

        // 3. ?亥岷甇支??典???"everyone" ?身甈?
        if (groupTables.containsKey("everyone")) {
            return groupTables.get("everyone").get(action);
        }

        // 4. ?交迨撅斤??閮剖?嚗???UNSET嚗?憭 Manager 蝜潛?敺?游誨???典??交
        return PermissionDecision.UNSET;
    }
}

package com.koffee.KoffeeUtils;

import com.koffee.EthanApiPlugin.Collections.NPCs;
import com.koffee.EthanApiPlugin.Collections.query.NPCQuery;
import net.runelite.api.NPC;

import java.util.Optional;

public class NpcUtil {

    public static Optional<NPC> getNpc(String name, boolean caseSensitive) {
        if (caseSensitive) {
            return NPCs.search().withName(name).nearestToPlayer();
        } else {
            return nameContainsNoCase(name).nearestToPlayer();
        }
    }

    public static Optional<NPC> findNearestAttackableNpc(String name) {
        return NPCs.search()
                .filter(npc -> npc.getName() != null && npc.getName().toLowerCase().contains(name.toLowerCase()) && npc.getInteracting() == null && npc.getHealthRatio() != 0)
                .nearestToPlayer();
    }

    public static NPCQuery nameContainsNoCase(String name) {
        return NPCs.search().filter(npcs -> npcs.getName() != null && npcs.getName().toLowerCase().contains(name.toLowerCase()));
    }

}

package com.koffee.RuneDragons.src.main.java.com.koffee.data;

import net.runelite.api.ItemID;
import net.runelite.api.coords.WorldArea;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Constants {
    //Locations
    public static final List<Integer> HOME_REGIONS = Arrays.asList(7513, 7514, 7769, 7770);
    public static final List<Integer> LITH_REGIONS = Arrays.asList(14242, 6223);
    public static final WorldArea EDGEVILLE_TELE = new WorldArea(3083, 3487, 17, 14, 0);
    public static final WorldArea LITH_TELE = new WorldArea(3541, 10390, 17, 14, 0);
    public static final WorldArea LITH_TELE_DOWNSTAIRS = new WorldArea(3541, 10466, 17, 20, 0);
    public static final WorldArea RUNE_DRAGONS_DOOR = new WorldArea(1562, 5057, 12, 23, 0);
    public static final WorldArea RUNE_DRAGONS = new WorldArea(1574, 5061, 26, 27, 0);
    // Potions
    public Set<Integer> EXTENDED_ANTIFIRE_POTS = Set.of(
            ItemID.EXTENDED_ANTIFIRE1,
            ItemID.EXTENDED_ANTIFIRE2,
            ItemID.EXTENDED_ANTIFIRE3,
            ItemID.EXTENDED_ANTIFIRE4
    );
    public Set<Integer> SUPER_EXTENDED_ANTIFIRE_POTS = Set.of(
            ItemID.EXTENDED_SUPER_ANTIFIRE1,
            ItemID.EXTENDED_SUPER_ANTIFIRE2,
            ItemID.EXTENDED_SUPER_ANTIFIRE3,
            ItemID.EXTENDED_SUPER_ANTIFIRE4
    );
    public Set<Integer> PRAYER_POTS = Set.of(
            ItemID.PRAYER_POTION1,
            ItemID.PRAYER_POTION2,
            ItemID.PRAYER_POTION3,
            ItemID.PRAYER_POTION4,
            ItemID.SUPER_RESTORE1,
            ItemID.SUPER_RESTORE2,
            ItemID.SUPER_RESTORE3,
            ItemID.SUPER_RESTORE4
    );
    public Set<Integer> SUPER_COMBAT_POTS = Set.of(
            ItemID.SUPER_COMBAT_POTION1,
            ItemID.SUPER_COMBAT_POTION2,
            ItemID.SUPER_COMBAT_POTION3,
            ItemID.SUPER_COMBAT_POTION4
    );
    public Set<Integer> DIVINE_SUPER_COMBAT_POTS = Set.of(
            ItemID.DIVINE_SUPER_COMBAT_POTION1,
            ItemID.DIVINE_SUPER_COMBAT_POTION2,
            ItemID.DIVINE_SUPER_COMBAT_POTION3,
            ItemID.DIVINE_SUPER_COMBAT_POTION4
    );
    public Set<Integer> DIGSITE_PENDANTS = Set.of(
            ItemID.DIGSITE_PENDANT_1,
            ItemID.DIGSITE_PENDANT_2,
            ItemID.DIGSITE_PENDANT_3,
            ItemID.DIGSITE_PENDANT_4,
            ItemID.DIGSITE_PENDANT_5
    );

}


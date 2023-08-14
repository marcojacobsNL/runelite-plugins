package com.koffee.KoffeeUtils.src.main.java.com.plugins.API;

import com.koffee.PacketUtils.WidgetInfoExtended;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;

public class SpellUtil {
    public static WidgetInfoExtended parseStringForWidgetInfoExtended(String input) {
        for (WidgetInfoExtended value : WidgetInfoExtended.values()) {
            if (value.name().equalsIgnoreCase("SPELL_" + input.replace(" ", "_"))) {
                return value;
            }
        }
        return null;
    }

    public static Widget getSpellWidget(Client client, String input) {
        return client.getWidget(parseStringForWidgetInfoExtended(input).getPackedId());
    }
}

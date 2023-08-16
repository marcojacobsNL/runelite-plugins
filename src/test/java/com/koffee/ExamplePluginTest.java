package com.koffee;

import com.koffee.EthanApiPlugin.EthanApiPlugin;
import com.koffee.KoffeeUtils.KoffeeUtilsPlugin;
import com.koffee.PacketUtils.PacketUtilsPlugin;
import com.koffee.RuneDragons.RuneDragonsPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ExamplePluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(EthanApiPlugin.class, PacketUtilsPlugin.class, KoffeeUtilsPlugin.class, RuneDragonsPlugin.class);
        RuneLite.main(args);
    }
}
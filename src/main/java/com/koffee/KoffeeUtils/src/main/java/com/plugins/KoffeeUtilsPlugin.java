package com.koffee.KoffeeUtils.src.main.java.com.plugins;

import com.koffee.EthanApiPlugin.EthanApiPlugin;
import com.koffee.PacketUtils.PacketUtilsPlugin;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(name = "<html><font color=\"#FF9DF9\">[PP]</font> KoffeeUtils</html>",
                description = "Utility Plugin for KoffeePlugins",
                tags = {"koffee","ethan"})
@PluginDependency(EthanApiPlugin.class)
@PluginDependency(PacketUtilsPlugin.class)
@Slf4j
public class KoffeeUtilsPlugin extends Plugin {
    @Override
    protected void startUp() throws Exception {
        log.info("[KoffeeUtils] Koffee Utils started");
    }
}

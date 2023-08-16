package com.koffee.KoffeeUtils;

import com.koffee.EthanApiPlugin.EthanApiPlugin;
import com.koffee.PacketUtils.PacketUtilsPlugin;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(name = "<html><font color=\"#d42020\">[Koffee]</font> Utils</html>",
        description = "Utility Plugin for KoffeePlugins",
        tags = {"koffee", "ethan"})
@PluginDependency(EthanApiPlugin.class)
@PluginDependency(PacketUtilsPlugin.class)
@Slf4j
public class KoffeeUtilsPlugin extends Plugin {

    public static boolean iterating = false;

    @Inject
    private ChatMessageManager messageManager;

    /**
     * Pauses execution for a random amount of time between two values.
     *
     * @param minSleep The minimum time to sleep.
     * @param maxSleep The maximum time to sleep.
     * @see #sleep(int)
     */

    public static void sleep(int minSleep, int maxSleep) {
        sleep(CalculationUtils.random(minSleep, maxSleep));
    }

    /**
     * Pauses execution for a given number of milliseconds.
     *
     * @param toSleep The time to sleep in milliseconds.
     */
    public static void sleep(int toSleep) {
        try {
            long start = System.currentTimeMillis();
            Thread.sleep(toSleep);

            // Guarantee minimum sleep
            long now;
            while (start + toSleep > (now = System.currentTimeMillis())) {
                Thread.sleep(start + toSleep - now);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void sleep(long toSleep) {
        try {
            long start = System.currentTimeMillis();
            Thread.sleep(toSleep);

            // Guarantee minimum sleep
            long now;
            while (start + toSleep > (now = System.currentTimeMillis())) {
                Thread.sleep(start + toSleep - now);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void startUp() throws Exception {
        log.info("[KoffeeUtils] Koffee Utils started");
    }

    public void sendDebugMessage(String message) {
        String msg = new ChatMessageBuilder().append(Color.RED, message).build();
        messageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(msg)
                .build());
    }

    public void sendGameMessage(String message) {
        String msg = new ChatMessageBuilder().append(Color.BLUE, message).build();
        messageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(msg)
                .build());
    }
}

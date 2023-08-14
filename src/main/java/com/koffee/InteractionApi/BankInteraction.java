package com.koffee.InteractionApi;

import com.koffee.EthanApiPlugin.Collections.Bank;
import com.koffee.EthanApiPlugin.EthanApiPlugin;
import com.koffee.Packets.MousePackets;
import com.koffee.Packets.WidgetPackets;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

import java.util.Optional;
import java.util.function.Predicate;

public class BankInteraction {
    private static final int WITHDRAW_QUANTITY = 3960;

    public static boolean useItem(String name, String... actions) {
        return Bank.search().withName(name).first().flatMap(item ->
        {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(item, actions);
            return Optional.of(true);
        }).orElse(false);
    }

    public static boolean useItem(int id, String... actions) {
        return Bank.search().withId(id).first().flatMap(item ->
        {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(item, actions);
            return Optional.of(true);
        }).orElse(false);
    }

    public static boolean useItem(Predicate<? super Widget> predicate, String... actions) {
        return Bank.search().filter(predicate).first().flatMap(item ->
        {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(item, actions);
            return Optional.of(true);
        }).orElse(false);
    }

    public static void withdrawX(Widget item, int amount) {
        if (EthanApiPlugin.getClient().getVarbitValue(WITHDRAW_QUANTITY) == amount) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetActionPacket(5, item.getId(), item.getItemId(), item.getIndex());
            return;
        }
        BankInteraction.useItem(item, "Withdraw-X");
        EthanApiPlugin.getClient().setVarcStrValue(359, Integer.toString(amount));
        EthanApiPlugin.getClient().setVarcIntValue(5, 7);
        EthanApiPlugin.getClient().runScript(681);
        EthanApiPlugin.getClient().setVarbit(WITHDRAW_QUANTITY, amount);
    }

    public static boolean useItemIndex(int index, String... actions) {
        return Bank.search().indexIs(index).first().flatMap(item ->
        {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(item, actions);
            return Optional.of(true);
        }).orElse(false);
    }

    public static boolean useItem(Widget item, String... actions) {
        if (item == null) {
            return false;
        }
        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetAction(item, actions);
        return true;
    }

    public static void depositInventory() {
        Widget widget = EthanApiPlugin.getClient().getWidget(WidgetInfo.BANK_DEPOSIT_INVENTORY);
        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetAction(widget, "Deposit", "Deposit inventory");
    }
}

package com.koffee.KoffeeUtils;

import com.koffee.EthanApiPlugin.Collections.Bank;
import com.koffee.EthanApiPlugin.Collections.BankInventory;
import com.koffee.EthanApiPlugin.Collections.query.ItemQuery;
import com.koffee.EthanApiPlugin.EthanApiPlugin;
import com.koffee.InteractionApi.BankInventoryInteraction;
import com.koffee.Packets.MousePackets;
import com.koffee.Packets.WidgetPackets;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.koffee.KoffeeUtils.KoffeeUtilsPlugin.iterating;
import static com.koffee.KoffeeUtils.KoffeeUtilsPlugin.sleep;

@Slf4j
@Singleton
public class BankUtil {

    public static ItemQuery nameContainsNoCase(String name) {
        return Bank.search().filter(widget -> widget.getName().toLowerCase().contains(name.toLowerCase()));
    }

    public static int getItemAmount(int itemId) {
        return getItemAmount(itemId, false);
    }

    public static int getItemAmount(int itemId, boolean stacked) {
        return stacked ?
                Bank.search().withId(itemId).first().map(Widget::getItemQuantity).orElse(0) :
                Bank.search().withId(itemId).result().size();
    }

    public static int getItemAmount(String itemName) {
        return nameContainsNoCase(itemName).result().size();
    }


    public static boolean hasItem(int id) {
        return hasItem(id, 1, false);
    }

    public static boolean hasItem(int id, int amount) {
        return getItemAmount(id, false) >= amount;
    }

    public static boolean hasItem(int id, int amount, boolean stacked) {
        return getItemAmount(id, stacked) >= amount;
    }

    public static boolean hasAny(int... ids) {
        for (int id : ids) {
            if (getItemAmount(id) > 0) {
                return true;
            }
        }
        return false;
    }

    public static void depositInventory() {
        Widget widget = EthanApiPlugin.getClient().getWidget(WidgetInfo.BANK_DEPOSIT_INVENTORY);
        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetAction(widget, "Deposit", "Deposit inventory");
    }

    public static boolean containsExcept(Collection<Integer> ids) {
        if (!Bank.isOpen()) {
            return false;
        }
        Collection<Widget> inventoryItems = BankInventory.search().result();

        for (Widget item : inventoryItems) {
            if (!ids.contains(item.getItemId())) {
                return true;
            }
        }
        return false;
    }

    public static void depositAllOfItem(Widget Item) {
        BankInventoryInteraction.useItem(Item, "Deposit-All");
    }

    // Credits to illumine
    public static void depositAllExcept(Collection<Integer> ids) {
        if (!Bank.isOpen()) {
            return;
        }
        Collection<Widget> inventoryItems = BankInventory.search().result();
        List<Integer> depositedItems = new ArrayList<>();
        try {
            iterating = true;
            for (Widget item : inventoryItems) {
                if (!ids.contains(item.getItemId()) && item.getItemId() != 6512 && !depositedItems.contains(item.getItemId())) //6512 is empty widget slot
                {
                    log.info("depositing item: " + item.getItemId());
                    depositAllOfItem(item);
                    sleep(200, 400);
                    depositedItems.add(item.getItemId());
                }
            }
            iterating = false;
            depositedItems.clear();
        } catch (Exception e) {
            iterating = false;
            e.printStackTrace();
        }
    }
}

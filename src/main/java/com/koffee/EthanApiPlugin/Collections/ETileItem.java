package com.koffee.EthanApiPlugin.Collections;

import com.koffee.Packets.MousePackets;
import com.koffee.Packets.TileItemPackets;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;

public class ETileItem {
    public WorldPoint location;
    public TileItem tileItem;

    public ETileItem(WorldPoint worldLocation, TileItem tileItem) {
        this.location = worldLocation;
        this.tileItem = tileItem;
    }

    public WorldPoint getLocation() {
        return location;
    }

    public TileItem getTileItem() {
        return tileItem;
    }

    public void interact(boolean ctrlDown) {
        MousePackets.queueClickPacket();
        TileItemPackets.queueTileItemAction(this, ctrlDown);
    }
}

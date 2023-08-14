package com.koffee.RuneDragons.src.main.java.com.koffee;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

class RuneDragonsOverlay extends OverlayPanel {
    private final RuneDragonsPlugin plugin;
    private final RuneDragonsConfig config;

    @Inject
    private RuneDragonsOverlay(RuneDragonsPlugin runeDragonsPlugin, RuneDragonsConfig runeDragonsConfig) {
        super(runeDragonsPlugin);
        plugin = runeDragonsPlugin;
        config = runeDragonsConfig;

        setPosition(OverlayPosition.BOTTOM_LEFT);
        setPreferredSize(new Dimension(160, 160));
    }

    @Override
    public Dimension render(Graphics2D graphics) {

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Rooftop Agility")
                .color(Color.WHITE)
                .build());

        panelComponent.getChildren().add(TitleComponent.builder()
                .text(plugin.isStarted() ? "Running" : "Paused")
                .color(plugin.isStarted() ? Color.GREEN : Color.RED)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Elapsed Time: ")
                .leftColor(Color.YELLOW)
                .right(plugin.getElapsedTime())
                .rightColor(Color.WHITE)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("State: ")
                .leftColor(Color.YELLOW)
                .right(plugin.getState() != null ? plugin.getState().name() : "null")
                .rightColor(Color.WHITE)
                .build());
        return super.render(graphics);
    }
}
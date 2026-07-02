package com.fubabeo.mod.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

public class CompassHudOverlay implements HudRenderCallback {

    public static boolean isVisible = false;
    public static double targetX = 0;
    public static double targetZ = 0;
    public static String targetName = "Target";

    // Size of the arrow indicator
    private static final int ARROW_SIZE = 12;

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        if (!isVisible) return;

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // ═══ POSITION: Above hotbar, centered ═══
        // Hotbar is at bottom, ~22px high. Place compass above it.
        int centerX = screenWidth / 2;
        int baseY = screenHeight - 59; // Just above the hotbar + XP bar area

        double deltaX = targetX - player.getX();
        double deltaZ = targetZ - player.getZ();
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (distance < 3) {
            // ═══ ARRIVED — Pulsing indicator ═══
            drawArrivedIndicator(drawContext, client, centerX, baseY);
        } else {
            // ═══ NAVIGATING — Rotating arrow ═══
            drawNavigationHud(drawContext, client, player, centerX, baseY, deltaX, deltaZ, distance);
        }
    }

    private void drawArrivedIndicator(DrawContext drawContext, MinecraftClient client, int cx, int baseY) {
        String text = "★ " + targetName + " ★";
        String subText = "Da toi noi!";
        int tw = client.textRenderer.getWidth(text);
        int sw = client.textRenderer.getWidth(subText);
        int maxW = Math.max(tw, sw);

        // Background
        int bgX = cx - maxW / 2 - 8;
        int bgW = maxW + 16;
        drawContext.fill(bgX, baseY - 22, bgX + bgW, baseY + 2, 0xAA004400);
        // Green border top
        drawContext.fill(bgX, baseY - 22, bgX + bgW, baseY - 21, 0xFF00FF00);

        drawContext.drawText(client.textRenderer, Text.literal(text), cx - tw / 2, baseY - 19, 0x55FF55, true);
        drawContext.drawText(client.textRenderer, Text.literal(subText), cx - sw / 2, baseY - 8, 0xAAFFAA, true);
    }

    private void drawNavigationHud(DrawContext drawContext, MinecraftClient client,
                                    PlayerEntity player, int cx, int baseY,
                                    double deltaX, double deltaZ, double distance) {
        // ═══ CALCULATE ANGLE ═══
        // In Minecraft:
        //   Yaw 0   = South (+Z direction)
        //   Yaw 90  = West  (-X direction)
        //   Yaw 180 = North (-Z direction)
        //   Yaw 270 = East  (+X direction)
        //
        // atan2(-deltaX, deltaZ) gives the angle FROM the player TO the target
        // in Minecraft's yaw convention (0 = south, clockwise).
        // The relative angle = targetAngle - playerYaw tells us where the target is
        // relative to where the player is looking.

        double targetAngle = Math.toDegrees(Math.atan2(-deltaX, deltaZ));
        double playerYaw = player.getYaw();
        double relativeAngle = targetAngle - playerYaw;

        // Normalize to [0, 360)
        relativeAngle = ((relativeAngle % 360) + 360) % 360;

        // ═══ DRAW BACKGROUND PANEL ═══
        int panelW = 100;
        int panelH = 36;
        int panelX = cx - panelW / 2;
        int panelY = baseY - panelH;

        // Semi-transparent dark background
        drawContext.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC0A0A1A);
        // Top accent line
        drawContext.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFF6366F1);
        // Side borders
        drawContext.fill(panelX, panelY, panelX + 1, panelY + panelH, 0x40FFFFFF);
        drawContext.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0x40FFFFFF);

        // ═══ DRAW ROTATING ARROW ═══
        // Arrow center position (left side of panel)
        int arrowCX = panelX + 18;
        int arrowCY = panelY + panelH / 2;

        drawRotatedArrow(drawContext, arrowCX, arrowCY, relativeAngle, ARROW_SIZE);

        // ═══ DRAW TARGET INFO (right side) ═══
        // Name (truncate if too long)
        String name = targetName.length() > 10 ? targetName.substring(0, 9) + ".." : targetName;
        int nameColor = 0xFFFFFF;
        drawContext.drawText(client.textRenderer, Text.literal(name), panelX + 34, panelY + 5, nameColor, true);

        // Distance
        String distStr;
        int distColor;
        if (distance < 50) {
            distStr = String.format("%.0fm", distance);
            distColor = 0xFFFF55; // Yellow = close
        } else if (distance < 200) {
            distStr = String.format("%.0fm", distance);
            distColor = 0xFFAA00; // Orange = medium
        } else {
            distStr = String.format("%.0fm", distance);
            distColor = 0xAAAAAA; // Grey = far
        }
        drawContext.drawText(client.textRenderer, Text.literal(distStr), panelX + 34, panelY + 18, distColor, true);

        // ═══ CLOSE INDICATOR ═══
        drawContext.drawText(client.textRenderer, Text.literal("x"), panelX + panelW - 10, panelY + 3, 0xFF5555, true);
    }

    /**
     * Draws a triangle arrow pointing in the direction of relativeAngle.
     * relativeAngle: 0 = straight ahead (up on screen), 90 = to the right, etc.
     *
     * We draw the arrow using 4 small filled quads to approximate a rotated triangle.
     */
    private void drawRotatedArrow(DrawContext drawContext, int cx, int cy, double angleDeg, int size) {
        double angleRad = Math.toRadians(angleDeg);
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);

        // Arrow is a triangle pointing UP (negative Y in screen coords).
        // We rotate it by angleDeg.
        //
        // Tip:    ( 0, -size)
        // Left:   (-size/2, size/2)
        // Right:  ( size/2, size/2)
        //
        // After rotation: x' = x*cos - y*sin, y' = x*sin + y*cos

        // Tip point
        int tipX = cx + (int)(0 * cos - (-size) * sin);
        int tipY = cy + (int)(0 * sin + (-size) * cos);

        // Left point
        double lx = -size * 0.45;
        double ly = size * 0.5;
        int leftX = cx + (int)(lx * cos - ly * sin);
        int leftY = cy + (int)(lx * sin + ly * cos);

        // Right point
        double rx = size * 0.45;
        double ry = size * 0.5;
        int rightX = cx + (int)(rx * cos - ry * sin);
        int rightY = cy + (int)(rx * sin + ry * cos);

        // Center indent (for a pointy arrow shape)
        double ix = 0;
        double iy = size * 0.15;
        int indentX = cx + (int)(ix * cos - iy * sin);
        int indentY = cy + (int)(ix * sin + iy * cos);

        // Draw two triangles to form the arrow head
        // Triangle 1: Tip → Left → Indent
        fillTriangle(drawContext, tipX, tipY, leftX, leftY, indentX, indentY, 0xFFFF8800);
        // Triangle 2: Tip → Right → Indent
        fillTriangle(drawContext, tipX, tipY, rightX, rightY, indentX, indentY, 0xFFFFAA00);

        // Draw a small dot at center for style
        drawContext.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFCC00);
    }

    /**
     * Fills a triangle using horizontal scanlines.
     * Simple rasterization for small triangles in the HUD.
     */
    private void fillTriangle(DrawContext dc, int x1, int y1, int x2, int y2, int x3, int y3, int color) {
        // Sort vertices by Y
        if (y1 > y2) { int t; t=x1; x1=x2; x2=t; t=y1; y1=y2; y2=t; }
        if (y1 > y3) { int t; t=x1; x1=x3; x3=t; t=y1; y1=y3; y3=t; }
        if (y2 > y3) { int t; t=x2; x2=x3; x3=t; t=y2; y2=y3; y3=t; }

        if (y3 == y1) return; // Degenerate

        for (int y = y1; y <= y3; y++) {
            // Interpolate x along edges
            float xa, xb;

            // Edge 1→3 always spans full height
            xa = x1 + (float)(x3 - x1) * (y - y1) / (y3 - y1);

            if (y < y2) {
                // Upper half: edge 1→2
                if (y2 == y1) xb = x1;
                else xb = x1 + (float)(x2 - x1) * (y - y1) / (y2 - y1);
            } else {
                // Lower half: edge 2→3
                if (y3 == y2) xb = x2;
                else xb = x2 + (float)(x3 - x2) * (y - y2) / (y3 - y2);
            }

            int left = (int) Math.min(xa, xb);
            int right = (int) Math.max(xa, xb);
            if (right >= left) {
                dc.fill(left, y, right + 1, y + 1, color);
            }
        }
    }
}

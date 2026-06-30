package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.network.ModNetwork;
import com.bl4ues.scpinventory.network.PickupItemPacket;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;

public final class PickupPromptClient {
    private static final int TEXT_WHITE = 0xFFE8E8E8;
    private static final int TEXT_GRAY = 0xFFB2B3B3;
    private static final int BRACKET = 0xDDE8E8E8;
    private static final int HAND = 0xEEF2F2F2;
    private static final double MAX_PICKUP_REACH = 4.75D;
    private static final double SOFT_AIM_RADIUS_SQR = 0.85D * 0.85D;

    private static ItemEntity target;

    private PickupPromptClient() {
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null) {
            target = null;
            return;
        }

        target = findTarget(mc, player);
        if (target != null && mc.options.keyUse.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new PickupItemPacket(target.getId()));
            target = null;
        }
    }

    public static void render(GuiGraphics g, int screenWidth, int screenHeight, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || mc.options.hideGui || target == null || !target.isAlive()) {
            return;
        }

        ScreenPoint point = projectToScreen(mc, target.getBoundingBox().getCenter().add(0.0D, 0.18D, 0.0D), screenWidth, screenHeight);
        if (point == null) {
            return;
        }

        int screenX = Mth.clamp(point.x(), 24, screenWidth - 24);
        int screenY = Mth.clamp(point.y(), 24, screenHeight - 24);
        drawTargetBrackets(g, screenX, screenY);

        int iconX = Math.max(4, screenX - 54);
        int iconY = Math.max(4, screenY - 32);
        int textX = iconX + 56;
        int textY = iconY + 8;
        if (textX + 190 > screenWidth) {
            textX = Math.max(4, screenX - 205);
            iconX = Math.max(4, textX - 56);
        }

        drawHandIcon(g, iconX, iconY);
        g.drawString(mc.font, "Pickup", textX, textY, TEXT_GRAY, true);
        g.drawString(mc.font, target.getItem().getHoverName().getString(), textX, textY + 16, TEXT_WHITE, true);
    }

    private static ItemEntity findTarget(Minecraft mc, LocalPlayer player) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F).normalize();
        double reach = MAX_PICKUP_REACH;
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            reach = Math.min(reach, eye.distanceTo(mc.hitResult.getLocation()) + 0.75D);
        }

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.35D);
        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, searchBox, item -> item.isAlive() && !item.getItem().isEmpty());
        return items.stream()
                .map(item -> new TargetCandidate(item, scoreItem(item, eye, look, reach)))
                .filter(candidate -> candidate.score() < Double.MAX_VALUE)
                .min(Comparator.comparingDouble(TargetCandidate::score))
                .map(TargetCandidate::item)
                .orElse(null);
    }

    private static double scoreItem(ItemEntity item, Vec3 eye, Vec3 look, double reach) {
        Vec3 center = item.getBoundingBox().getCenter();
        Vec3 toItem = center.subtract(eye);
        double alongRay = toItem.dot(look);
        if (alongRay < 0.0D || alongRay > reach) return Double.MAX_VALUE;

        Vec3 closest = eye.add(look.scale(alongRay));
        double lineDistanceSqr = closest.distanceToSqr(center);
        boolean directBoxHit = item.getBoundingBox().inflate(0.42D).clip(eye, eye.add(look.scale(reach))).isPresent();
        if (!directBoxHit && lineDistanceSqr > SOFT_AIM_RADIUS_SQR) return Double.MAX_VALUE;
        return lineDistanceSqr + (alongRay * 0.015D);
    }

    private static ScreenPoint projectToScreen(Minecraft mc, Vec3 worldPos, int screenWidth, int screenHeight) {
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 relative = worldPos.subtract(camera.getPosition());
        Quaternionf rotation = new Quaternionf(camera.rotation());
        rotation.conjugate();
        Vector3f transformed = new Vector3f((float) relative.x, (float) relative.y, (float) relative.z);
        transformed.rotate(rotation);
        if (transformed.z() >= -0.05F) return null;

        double fov = mc.options.fov().get();
        double scale = screenHeight / (2.0D * Math.tan(Math.toRadians(fov) / 2.0D));
        int x = (int) Math.round((screenWidth / 2.0D) - (transformed.x() * scale / transformed.z()));
        int y = (int) Math.round((screenHeight / 2.0D) + (transformed.y() * scale / transformed.z()));
        return new ScreenPoint(x, y);
    }

    private static void drawTargetBrackets(GuiGraphics g, int centerX, int centerY) {
        int half = 12;
        int len = 7;
        int x1 = centerX - half;
        int x2 = centerX + half;
        int y1 = centerY - half;
        int y2 = centerY + half;
        g.fill(x1, y1, x1 + len, y1 + 1, BRACKET);
        g.fill(x1, y1, x1 + 1, y1 + len, BRACKET);
        g.fill(x2 - len, y1, x2, y1 + 1, BRACKET);
        g.fill(x2 - 1, y1, x2, y1 + len, BRACKET);
        g.fill(x1, y2 - 1, x1 + len, y2, BRACKET);
        g.fill(x1, y2 - len, x1 + 1, y2, BRACKET);
        g.fill(x2 - len, y2 - 1, x2, y2, BRACKET);
        g.fill(x2 - 1, y2 - len, x2, y2, BRACKET);
    }

    private static void drawHandIcon(GuiGraphics g, int x, int y) {
        g.fill(x + 15, y + 3, x + 19, y + 27, HAND);
        g.fill(x + 21, y + 5, x + 25, y + 28, HAND);
        g.fill(x + 27, y + 9, x + 31, y + 29, HAND);
        g.fill(x + 33, y + 15, x + 37, y + 32, HAND);
        g.fill(x + 8, y + 22, x + 16, y + 36, HAND);
        g.fill(x + 12, y + 34, x + 34, y + 40, HAND);
        g.fill(x + 18, y + 40, x + 32, y + 45, HAND);
    }

    private record TargetCandidate(ItemEntity item, double score) {
    }

    private record ScreenPoint(int x, int y) {
    }
}

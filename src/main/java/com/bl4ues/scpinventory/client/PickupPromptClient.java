package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.network.ModNetwork;
import com.bl4ues.scpinventory.network.PickupItemPacket;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

public final class PickupPromptClient {

    private static final String PICKUP_ICON_BASE64 = "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";

    private static final int ICON_SOURCE_SIZE = 128;
    private static final int ICON_SIZE = 42;
    private static final int TEXT_WHITE = 0xFFE8E8E8;
    private static final int TEXT_GRAY = 0xFFB2B3B3;
    private static final int BRACKET = 0xDDE8E8E8;
    private static final double MAX_PICKUP_REACH = 4.75D;
    private static final double SOFT_AIM_RADIUS_SQR = 0.85D * 0.85D;

    private static ResourceLocation pickupIcon;
    private static boolean textureReady = false;
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
    }

    public static boolean tryPickupTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || target == null || !target.isAlive() || target.getItem().isEmpty()) {
            return false;
        }

        ModNetwork.CHANNEL.sendToServer(new PickupItemPacket(target.getId()));
        return true;
    }

    public static void render(GuiGraphics g, int screenWidth, int screenHeight, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || mc.options.hideGui || target == null || !target.isAlive()) {
            return;
        }

        ensureTexture(mc);

        ScreenPoint point = projectToScreen(mc, target.getBoundingBox().getCenter().add(0.0D, 0.18D, 0.0D), screenWidth, screenHeight);
        if (point == null) {
            return;
        }

        int screenX = Mth.clamp(point.x(), 24, screenWidth - 24);
        int screenY = Mth.clamp(point.y(), 24, screenHeight - 24);

        drawTargetBrackets(g, screenX, screenY);

        int iconX = screenX - 52;
        int iconY = screenY - 32;
        int textX = iconX + ICON_SIZE + 12;
        int textY = iconY + 8;

        if (textX + 180 > screenWidth) {
            textX = screenX - 200;
            iconX = textX - ICON_SIZE - 12;
        }
        if (iconX < 4) {
            iconX = 4;
            textX = iconX + ICON_SIZE + 12;
        }
        if (iconY < 4) {
            iconY = 4;
            textY = iconY + 8;
        }

        drawIcon(g, iconX, iconY);
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

        List<ItemEntity> items = player.level().getEntitiesOfClass(
                ItemEntity.class,
                searchBox,
                item -> item.isAlive() && !item.getItem().isEmpty()
        );

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

        if (alongRay < 0.0D || alongRay > reach) {
            return Double.MAX_VALUE;
        }

        Vec3 closest = eye.add(look.scale(alongRay));
        double lineDistanceSqr = closest.distanceToSqr(center);

        AABB forgivingBox = item.getBoundingBox().inflate(0.42D);
        boolean directBoxHit = forgivingBox.clip(eye, eye.add(look.scale(reach))).isPresent();

        if (!directBoxHit && lineDistanceSqr > SOFT_AIM_RADIUS_SQR) {
            return Double.MAX_VALUE;
        }

        return lineDistanceSqr + (alongRay * 0.015D);
    }

    private static ScreenPoint projectToScreen(Minecraft mc, Vec3 worldPos, int screenWidth, int screenHeight) {
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        Vec3 relative = worldPos.subtract(cameraPos);

        Quaternionf rotation = new Quaternionf(camera.rotation());
        rotation.conjugate();

        Vector3f transformed = new Vector3f((float) relative.x, (float) relative.y, (float) relative.z);
        transformed.rotate(rotation);

        if (transformed.z() >= -0.05F) {
            return null;
        }

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

    private static void drawIcon(GuiGraphics g, int x, int y) {
        if (pickupIcon == null) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.95F);
        g.blit(pickupIcon, x, y, ICON_SIZE, ICON_SIZE, 0.0F, 0.0F, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void ensureTexture(Minecraft mc) {
        if (textureReady) return;

        pickupIcon = new ResourceLocation(ScpInventoryMod.MODID, "hud/pickup_hand");
        try {
            byte[] bytes = Base64.getDecoder().decode(PICKUP_ICON_BASE64);
            try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
                NativeImage image = NativeImage.read(input);
                mc.getTextureManager().register(pickupIcon, new DynamicTexture(image));
            }
        } catch (IOException | IllegalArgumentException ignored) {
            pickupIcon = null;
        }

        textureReady = true;
    }

    private record TargetCandidate(ItemEntity item, double score) {
    }

    private record ScreenPoint(int x, int y) {
    }
}

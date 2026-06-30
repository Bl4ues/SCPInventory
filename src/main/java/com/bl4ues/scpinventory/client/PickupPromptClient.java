package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.network.ModNetwork;
import com.bl4ues.scpinventory.network.PickupItemPacket;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
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

    private static final int ICON_SOURCE_SIZE = 64;
    private static final int ICON_SIZE = 58;
    private static final int TEXT_WHITE = 0xFFE8E8E8;
    private static final int TEXT_GRAY = 0xFFB2B3B3;
    private static final double MAX_PICKUP_REACH = 4.75D;
    private static final double SOFT_AIM_RADIUS_SQR = 0.85D * 0.85D;

    private static ResourceLocation pickupIcon;
    private static boolean textureReady = false;
    private static ItemEntity target;
    private static ItemEntity glowingTarget;

    private PickupPromptClient() {
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null) {
            clearTarget();
            return;
        }

        target = findTarget(mc, player);
        updateGlowingTarget(target);
        if (target != null && mc.options.keyUse.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new PickupItemPacket(target.getId()));
            clearTarget();
        }
    }

    public static void render(GuiGraphics g, int screenWidth, int screenHeight, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || mc.options.hideGui || target == null || !target.isAlive()) {
            return;
        }

        ensureTexture(mc);

        ScreenPoint point = projectToScreen(mc, target.getBoundingBox().getCenter().add(0.0D, 0.18D, 0.0D), screenWidth, screenHeight);
        if (point == null) {
            point = new ScreenPoint(screenWidth / 2, screenHeight / 2);
        }

        int screenX = Mth.clamp(point.x(), 28, screenWidth - 28);
        int screenY = Mth.clamp(point.y(), 28, screenHeight - 28);

        int iconX = screenX - 44;
        int iconY = screenY - 32;
        int textX = screenX + 54;
        int textY = screenY - 20;

        if (textX + 180 > screenWidth) {
            iconX = Math.max(6, screenX - ICON_SIZE - 180);
            textX = iconX + ICON_SIZE + 12;
        }
        if (iconX < 6) {
            iconX = 6;
        }
        if (iconY < 6) {
            iconY = 6;
            textY = iconY + 12;
        }
        if (iconY + ICON_SIZE > screenHeight - 6) {
            iconY = screenHeight - ICON_SIZE - 6;
            textY = iconY + 12;
        }

        drawIcon(g, iconX, iconY);
        g.drawString(mc.font, "Pickup", textX, textY, TEXT_GRAY, true);
        g.drawString(mc.font, target.getItem().getHoverName().getString(), textX, textY + 18, TEXT_WHITE, true);
    }

    public static void renderWorldOutline(PoseStack poseStack, Camera camera) {
        // Intentionally empty for now. The direct outline-buffer render produced a solid white item.
        // The selected ItemEntity still receives the vanilla glowing flag as a safer highlight path.
    }

    private static ItemEntity findTarget(Minecraft mc, LocalPlayer player) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F).normalize();
        double reach = MAX_PICKUP_REACH;
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            reach = Math.min(reach, eye.distanceTo(mc.hitResult.getLocation()) + 0.75D);
        }
        final double effectiveReach = reach;

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(effectiveReach)).inflate(1.35D);
        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, searchBox, item -> item.isAlive() && !item.getItem().isEmpty());
        return items.stream()
                .map(item -> new TargetCandidate(item, scoreItem(item, eye, look, effectiveReach)))
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

        double depth = Math.abs(transformed.z());
        if (depth < 0.05D) return null;

        double fov = mc.options.fov().get();
        double scale = screenHeight / (2.0D * Math.tan(Math.toRadians(fov) / 2.0D));

        int x = (int) Math.round((screenWidth / 2.0D) - (transformed.x() * scale / depth));
        int y = (int) Math.round((screenHeight / 2.0D) - (transformed.y() * scale / depth));
        return new ScreenPoint(x, y);
    }

    private static void drawIcon(GuiGraphics g, int x, int y) {
        if (pickupIcon == null) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.96F);
        g.blit(pickupIcon, x, y, ICON_SIZE, ICON_SIZE, 0.0F, 0.0F, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void ensureTexture(Minecraft mc) {
        if (textureReady) return;
        pickupIcon = registerEmbeddedTexture(mc, "hud/pickup_hand_clean", PICKUP_ICON_BASE64);
        textureReady = true;
    }

    private static ResourceLocation registerEmbeddedTexture(Minecraft mc, String name, String base64) {
        ResourceLocation location = new ResourceLocation(ScpInventoryMod.MODID, name);
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
                NativeImage image = NativeImage.read(input);
                mc.getTextureManager().register(location, new DynamicTexture(image));
            }
            return location;
        } catch (IOException | IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void updateGlowingTarget(ItemEntity newTarget) {
        if (glowingTarget != null && glowingTarget != newTarget) {
            glowingTarget.setGlowingTag(false);
            glowingTarget = null;
        }
        if (newTarget != null && newTarget.isAlive()) {
            glowingTarget = newTarget;
            glowingTarget.setGlowingTag(true);
        }
    }

    private static void clearTarget() {
        target = null;
        updateGlowingTarget(null);
    }

    private record TargetCandidate(ItemEntity item, double score) {
    }

    private record ScreenPoint(int x, int y) {
    }
}

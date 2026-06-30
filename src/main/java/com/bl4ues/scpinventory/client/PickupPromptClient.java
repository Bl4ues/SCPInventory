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
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.OutlineBufferSource;
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

        ensureTexture(mc);

        ScreenPoint point = projectToScreen(mc, target.getBoundingBox().getCenter().add(0.0D, 0.18D, 0.0D), screenWidth, screenHeight);
        if (point == null) {
            point = new ScreenPoint(screenWidth / 2, screenHeight / 2);
        }

        int screenX = Mth.clamp(point.x(), 28, screenWidth - 28);
        int screenY = Mth.clamp(point.y(), 28, screenHeight - 28);

        int iconX = screenX + 34;
        int iconY = screenY - 31;
        int textX = iconX + ICON_SIZE + 14;
        int textY = iconY + 10;

        if (textX + 210 > screenWidth) {
            iconX = Math.max(6, screenX - ICON_SIZE - 230);
            textX = iconX + ICON_SIZE + 14;
        }
        if (iconY < 6) {
            iconY = 6;
            textY = iconY + 10;
        }
        if (iconY + ICON_SIZE > screenHeight - 6) {
            iconY = screenHeight - ICON_SIZE - 6;
            textY = iconY + 10;
        }

        drawIcon(g, iconX, iconY);
        g.drawString(mc.font, "Pickup", textX, textY, TEXT_GRAY, true);
        g.drawString(mc.font, target.getItem().getHoverName().getString(), textX, textY + 18, TEXT_WHITE, true);
    }

    public static void renderWorldOutline(PoseStack poseStack, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null || target == null || !target.isAlive()) {
            return;
        }

        Vec3 cameraPosition = camera.getPosition();
        double x = target.getX() - cameraPosition.x;
        double y = target.getY() - cameraPosition.y;
        double z = target.getZ() - cameraPosition.z;

        poseStack.pushPose();
        OutlineBufferSource outline = mc.renderBuffers().outlineBufferSource();
        outline.setColor(255, 255, 255, 190);
        mc.getEntityRenderDispatcher().render(target, x, y, z, target.getYRot(), mc.getFrameTime(), poseStack, outline, LightTexture.FULL_BRIGHT);
        outline.endOutlineBatch();
        poseStack.popPose();
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
        pickupIcon = registerEmbeddedTexture(mc, "hud/pickup_hand", PICKUP_ICON_BASE64);
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

    private record TargetCandidate(ItemEntity item, double score) {
    }

    private record ScreenPoint(int x, int y) {
    }
}

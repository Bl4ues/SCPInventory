package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.network.ModNetwork;
import com.bl4ues.scpinventory.network.PickupItemPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.resources.ResourceLocation;
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
    private static final ResourceLocation PICKUP_ICON = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/pickup.png");

    private static final int ICON_SOURCE_SIZE = 64;
    private static final int ICON_SIZE = 58;
    private static final int TEXT_WHITE = 0xFFE8E8E8;
    private static final int TEXT_GRAY = 0xFFB2B3B3;
    private static final double MAX_PICKUP_REACH = 4.75D;
    private static final double SOFT_AIM_RADIUS_SQR = 0.85D * 0.85D;

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

        ScreenPoint point = projectToScreen(mc, target.getBoundingBox().getCenter().add(0.0D, 0.18D, 0.0D), screenWidth, screenHeight);
        if (point == null) {
            point = new ScreenPoint(screenWidth / 2, screenHeight / 2);
        }

        int screenX = Mth.clamp(point.x(), 28, screenWidth - 28);
        int screenY = Mth.clamp(point.y(), 28, screenHeight - 28);

        int iconX = screenX - 54;
        int iconY = screenY - 34;
        int textX = screenX + 42;
        int textY = screenY - 19;

        if (textX + 180 > screenWidth) {
            textX = Math.max(8, screenX - 164);
            iconX = Math.max(6, textX - ICON_SIZE - 14);
        }
        if (iconX < 6) {
            iconX = 6;
        }
        if (iconY < 6) {
            iconY = 6;
            textY = iconY + 13;
        }
        if (iconY + ICON_SIZE > screenHeight - 6) {
            iconY = screenHeight - ICON_SIZE - 6;
            textY = iconY + 13;
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
        poseStack.translate(x, y, z);
        poseStack.scale(1.075F, 1.075F, 1.075F);

        OutlineBufferSource outline = mc.renderBuffers().outlineBufferSource();
        outline.setColor(255, 255, 255, 150);
        mc.getEntityRenderDispatcher().render(target, 0.0D, 0.0D, 0.0D, target.getYRot(), mc.getFrameTime(), poseStack, outline, LightTexture.FULL_BRIGHT);
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
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.96F);
        g.blit(PICKUP_ICON, x, y, ICON_SIZE, ICON_SIZE, 0.0F, 0.0F, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
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

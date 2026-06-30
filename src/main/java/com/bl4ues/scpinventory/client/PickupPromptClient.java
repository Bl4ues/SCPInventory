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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;

public final class PickupPromptClient {
    private static final ResourceLocation PICKUP_ICON = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/pickup.png");

    private static final int ICON_SOURCE_SIZE = 128;
    private static final int ICON_SIZE = 82;
    private static final int TEXT_WHITE = 0xFFE8E8E8;
    private static final int TEXT_GRAY = 0xFFB2B3B3;
    private static final float PICKUP_TEXT_SCALE = 1.55F;
    private static final float ITEM_TEXT_SCALE = 1.85F;
    private static final double MAX_PICKUP_REACH = 2.25D;
    private static final double SOFT_AIM_RADIUS_SQR = 0.58D * 0.58D;
    private static final float MODEL_OUTLINE_SCALE = 1.12F;
    private static final int PICKUP_CLICK_COOLDOWN_TICKS = 5;

    private static ItemEntity target;
    private static boolean useWasDown = false;
    private static int pickupCooldownTicks = 0;

    private PickupPromptClient() {
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null || player.isCreative() || player.isSpectator()) {
            clearTarget();
            useWasDown = false;
            pickupCooldownTicks = 0;
            return;
        }

        if (pickupCooldownTicks > 0) {
            pickupCooldownTicks--;
        }

        target = findTarget(mc, player);

        boolean useDown = mc.options.keyUse.isDown();
        boolean pressedThisTick = useDown && !useWasDown;
        useWasDown = useDown;

        if (target != null && pressedThisTick && pickupCooldownTicks <= 0) {
            ModNetwork.CHANNEL.sendToServer(new PickupItemPacket(target.getId()));
            pickupCooldownTicks = PICKUP_CLICK_COOLDOWN_TICKS;
            clearTarget();
        }
    }

    public static void render(GuiGraphics g, int screenWidth, int screenHeight, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || mc.options.hideGui || target == null || !target.isAlive()) {
            return;
        }

        ScreenPoint point = projectToScreen(mc, target.getBoundingBox().getCenter().add(0.0D, -0.08D, 0.0D), screenWidth, screenHeight);
        if (point == null) {
            point = new ScreenPoint(screenWidth / 2, screenHeight / 2);
        }

        int screenX = Mth.clamp(point.x(), 28, screenWidth - 28);
        int screenY = Mth.clamp(point.y(), 28, screenHeight - 28);

        int iconX = screenX - (ICON_SIZE / 2) - 7;
        int iconY = screenY - (ICON_SIZE / 2) + 8;
        int textX = iconX + ICON_SIZE + 4;
        int pickupY = iconY + 22;
        int itemY = pickupY + 32;

        int itemWidth = Math.round(mc.font.width(target.getItem().getHoverName().getString()) * ITEM_TEXT_SCALE);
        if (textX + itemWidth > screenWidth - 8) {
            textX = Math.max(8, screenWidth - itemWidth - 8);
            iconX = Math.max(6, textX - ICON_SIZE - 4);
        }
        if (iconX < 6) {
            iconX = 6;
            textX = iconX + ICON_SIZE + 4;
        }
        if (iconY < 6) {
            iconY = 6;
            pickupY = iconY + 22;
            itemY = pickupY + 32;
        }
        if (iconY + ICON_SIZE > screenHeight - 6) {
            iconY = screenHeight - ICON_SIZE - 6;
            pickupY = iconY + 22;
            itemY = pickupY + 32;
        }

        drawIcon(g, iconX, iconY);
        drawScaledString(g, mc, "Pickup", textX, pickupY, PICKUP_TEXT_SCALE, TEXT_GRAY);
        drawScaledString(g, mc, target.getItem().getHoverName().getString(), textX, itemY, ITEM_TEXT_SCALE, TEXT_WHITE);
    }

    public static void renderWorldOutline(PoseStack poseStack, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null || target == null || !target.isAlive()) {
            return;
        }

        Vec3 cameraPosition = camera.getPosition();
        AABB bounds = target.getBoundingBox();
        Vec3 center = bounds.getCenter();

        double x = target.getX() - cameraPosition.x;
        double y = target.getY() - cameraPosition.y;
        double z = target.getZ() - cameraPosition.z;
        double cx = center.x - cameraPosition.x;
        double cy = center.y - cameraPosition.y;
        double cz = center.z - cameraPosition.z;

        OutlineBufferSource outline = mc.renderBuffers().outlineBufferSource();
        outline.setColor(255, 255, 255, 165);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        ItemStack originalStack = target.getItem();
        ItemStack singleVisualStack = originalStack.copy();
        singleVisualStack.setCount(1);

        poseStack.pushPose();
        try {
            target.setItem(singleVisualStack);
            poseStack.translate(cx, cy, cz);
            poseStack.scale(MODEL_OUTLINE_SCALE, MODEL_OUTLINE_SCALE, MODEL_OUTLINE_SCALE);
            poseStack.translate(-cx, -cy, -cz);
            mc.getEntityRenderDispatcher().render(target, x, y, z, target.getYRot(), mc.getFrameTime(), poseStack, outline, LightTexture.FULL_BRIGHT);
        } finally {
            target.setItem(originalStack);
            poseStack.popPose();
        }

        outline.endOutlineBatch();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static ItemEntity findTarget(Minecraft mc, LocalPlayer player) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F).normalize();
        double reach = MAX_PICKUP_REACH;
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            reach = Math.min(reach, eye.distanceTo(mc.hitResult.getLocation()) + 0.35D);
        }
        final double effectiveReach = reach;

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(effectiveReach)).inflate(0.85D);
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
        boolean directBoxHit = item.getBoundingBox().inflate(0.35D).clip(eye, eye.add(look.scale(reach))).isPresent();
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
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.98F);
        g.blit(PICKUP_ICON, x, y, ICON_SIZE, ICON_SIZE, 0.0F, 0.0F, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void drawScaledString(GuiGraphics g, Minecraft mc, String text, int x, int y, float scale, int color) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.scale(scale, scale, 1.0F);
        g.drawString(mc.font, text, 0, 0, color, true);
        pose.popPose();
    }

    private static void clearTarget() {
        target = null;
    }

    private record TargetCandidate(ItemEntity item, double score) {
    }

    private record ScreenPoint(int x, int y) {
    }
}

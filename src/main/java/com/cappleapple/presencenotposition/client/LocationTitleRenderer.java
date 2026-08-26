package com.cappleapple.presencenotposition.client;

import com.cappleapple.presencenotposition.config.ClientConfig;
import com.cappleapple.presencenotposition.presentation.AnimationDefinition;
import com.cappleapple.presencenotposition.presentation.PresentationStackLayout;
import com.cappleapple.presencenotposition.presentation.VisualDefinition;
import com.cappleapple.presencenotposition.resource.ClientResourceIndex;
import com.cappleapple.presencenotposition.resource.TextureSize;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class LocationTitleRenderer {
    private LocationTitleRenderer() {
    }

    public static void render(RenderGuiEvent.Post event) {
        List<ClientPresentationManager.Active> activePresentations = ClientPresentationManager.active();
        if (activePresentations.isEmpty()) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        GuiGraphics graphics = event.getGuiGraphics();
        int titleX = PresentationStackLayout.horizontalAnchor(graphics.guiWidth(), ClientConfig.TITLE_X.get());
        List<PresentationStackLayout.Row> rows = PresentationStackLayout.rows(
            graphics.guiHeight(), activePresentations.size(), ClientConfig.TITLE_Y.get(), ClientConfig.TITLE_SPACING.get()
        );
        for (int index = 0; index < activePresentations.size(); index++) {
            ClientPresentationManager.Active active = activePresentations.get(index);
            PresentationStackLayout.Row row = rows.get(index);
            float alpha = active.alpha(partial);
            float sizeScale = row.scale();
            float subtitleScale = Math.max(0.7F, sizeScale);
            int subtitleHeight = active.subtitle() == null ? 0
                : Math.max(1, Math.round(Minecraft.getInstance().font.lineHeight * subtitleScale));
            int subtitleGap = active.subtitle() == null ? 0 : 1;
            int contentHeight = Math.max(1, row.height() - subtitleHeight - subtitleGap);
            int contentCenterY = row.top() + contentHeight / 2;
            if (alpha > 0.0F) {
                VisualDefinition visual = active.definition().title().visual();
                boolean renderedTexture = switch (visual.type()) {
                    case TEXT -> false;
                    case TEXTURE -> renderSpritesheet(graphics, visual, active.elapsedTicks(), alpha, titleX, contentCenterY, contentHeight, sizeScale);
                    case FRAMES -> renderFrames(graphics, visual, active.elapsedTicks(), alpha, titleX, contentCenterY, contentHeight, sizeScale);
                };
                if (!renderedTexture) renderText(graphics, active, alpha, titleX, contentCenterY, contentHeight, sizeScale);
                if (active.subtitle() != null) {
                    renderSubtitle(graphics, active, alpha, titleX, row.top() + contentHeight + subtitleGap, subtitleScale);
                }
            }
        }
    }

    private static boolean renderSpritesheet(
        GuiGraphics graphics, VisualDefinition visual, int elapsed, float alpha,
        int centerX, int centerY, int rowHeight, float sizeScale
    ) {
        ResourceLocation texture = visual.texture();
        TextureSize size = ClientResourceIndex.snapshot().textureSizes().get(texture);
        if (size == null) return false;
        AnimationDefinition animation = visual.animation();
        int frame = animation.frameAt(elapsed);
        boolean horizontal = animation.layout() == AnimationDefinition.Layout.HORIZONTAL
            || animation.layout() == AnimationDefinition.Layout.AUTO && size.width() % animation.frameCount() == 0
                && size.height() % animation.frameCount() != 0;
        int frameWidth = horizontal ? size.width() / animation.frameCount() : size.width();
        int frameHeight = horizontal ? size.height() : size.height() / animation.frameCount();
        if (frameWidth < 1 || frameHeight < 1) return false;
        int u = horizontal ? frame * frameWidth : 0;
        int v = horizontal ? 0 : frame * frameHeight;
        drawTexture(graphics, texture, size, frameWidth, frameHeight, u, v, alpha, centerX, centerY, rowHeight, sizeScale);
        return true;
    }

    private static boolean renderFrames(
        GuiGraphics graphics, VisualDefinition visual, int elapsed, float alpha,
        int centerX, int centerY, int rowHeight, float sizeScale
    ) {
        int frame = visual.animation().frameAt(elapsed);
        if (frame >= visual.frames().size()) frame = visual.frames().size() - 1;
        ResourceLocation texture = visual.frames().get(frame);
        TextureSize size = ClientResourceIndex.snapshot().textureSizes().get(texture);
        if (size == null) return false;
        drawTexture(graphics, texture, size, size.width(), size.height(), 0, 0, alpha, centerX, centerY, rowHeight, sizeScale);
        return true;
    }

    private static void drawTexture(
        GuiGraphics graphics, ResourceLocation texture, TextureSize fullSize,
        int frameWidth, int frameHeight, int u, int v, float alpha,
        int centerX, int centerY, int rowHeight, float sizeScale
    ) {
        float scale = Math.min((graphics.guiWidth() * 0.65F * sizeScale) / frameWidth, (rowHeight - 4.0F) / frameHeight);
        scale = Math.min(scale, 1.0F);
        int width = Math.max(1, Math.round(frameWidth * scale));
        int height = Math.max(1, Math.round(frameHeight * scale));
        int x = (int) Math.floor(centerX - width / 2.0F);
        int y = centerY - height / 2;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(texture, x, y, width, height, u, v, frameWidth, frameHeight, fullSize.width(), fullSize.height());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderText(
        GuiGraphics graphics, ClientPresentationManager.Active active, float alpha,
        int centerX, int centerY, int contentHeight, float sizeScale
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        int color = ((int) (alpha * 255.0F) << 24) | 0xFFFFFF;
        float textScale = Math.min(1.65F * sizeScale, contentHeight / (float) minecraft.font.lineHeight);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().scale(textScale, textScale, 1.0F);
        graphics.drawString(minecraft.font, active.title(), -minecraft.font.width(active.title()) / 2, -minecraft.font.lineHeight / 2, color, true);
        graphics.pose().popPose();
    }

    private static void renderSubtitle(
        GuiGraphics graphics, ClientPresentationManager.Active active, float alpha, int centerX, int y, float scale
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        int color = ((int) (alpha * 255.0F) << 24) | 0xFFFFFF;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(minecraft.font, active.subtitle(), -minecraft.font.width(active.subtitle()) / 2, 0, color, true);
        graphics.pose().popPose();
    }
}

package io.github.amerebagatelle.mods.nuit.skybox;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.amerebagatelle.mods.nuit.components.Blend;
import io.github.amerebagatelle.mods.nuit.components.Conditions;
import io.github.amerebagatelle.mods.nuit.components.Properties;
import io.github.amerebagatelle.mods.nuit.components.RGBA;
import io.github.amerebagatelle.mods.nuit.mixin.LevelRendererAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public class MonoColorSkybox extends AbstractSkybox {
    public static Codec<MonoColorSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.optionalFieldOf("properties", Properties.of()).forGetter(AbstractSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.of()).forGetter(AbstractSkybox::getConditions),
            RGBA.CODEC.optionalFieldOf("color", RGBA.of()).forGetter(MonoColorSkybox::getColor),
            Blend.CODEC.optionalFieldOf("blend", Blend.normal()).forGetter(MonoColorSkybox::getBlend)
    ).apply(instance, MonoColorSkybox::new));
    public RGBA color;
    public Blend blend;

    public MonoColorSkybox(Properties properties, Conditions conditions, RGBA color, Blend blend) {
        super(properties, conditions);
        this.color = color;
        this.blend = blend;
    }

    @Override
    public void render(LevelRendererAccessor worldRendererAccess, PoseStack matrices, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback) {
        if (this.alpha > 0) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            this.blend.applyBlendFunc(this.alpha);
            BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            for (int i = 0; i < 6; ++i) {
                matrices.pushPose();
                if (i == 1) {
                    matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
                } else if (i == 2) {
                    matrices.mulPose(Axis.XP.rotationDegrees(-90.0F));
                    matrices.mulPose(Axis.YP.rotationDegrees(180.0F));
                } else if (i == 3) {
                    matrices.mulPose(Axis.XP.rotationDegrees(180.0F));
                } else if (i == 4) {
                    matrices.mulPose(Axis.ZP.rotationDegrees(90.0F));
                    matrices.mulPose(Axis.YP.rotationDegrees(-90.0F));
                } else if (i == 5) {
                    matrices.mulPose(Axis.ZP.rotationDegrees(-90.0F));
                    matrices.mulPose(Axis.YP.rotationDegrees(90.0F));
                }

                Matrix4f matrix4f = matrices.last().pose();
                bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, -100.0F).color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.alpha).endVertex();
                bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, 100.0F).color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.alpha).endVertex();
                bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, 100.0F).color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.alpha).endVertex();
                bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, -100.0F).color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.alpha).endVertex();
                matrices.popPose();
            }
            BufferUploader.drawWithShader(bufferBuilder.end());

            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
        }
    }

    public RGBA getColor() {
        return this.color;
    }

    public Blend getBlend() {
        return this.blend;
    }
}

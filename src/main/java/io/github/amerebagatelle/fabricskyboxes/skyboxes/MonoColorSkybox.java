package io.github.amerebagatelle.fabricskyboxes.skyboxes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.amerebagatelle.fabricskyboxes.mixin.skybox.WorldRendererAccess;
import io.github.amerebagatelle.fabricskyboxes.util.JsonObjectWrapper;
import io.github.amerebagatelle.fabricskyboxes.util.Utils;
import io.github.amerebagatelle.fabricskyboxes.util.object.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MonoColorSkybox extends AbstractSkybox {
    public static final Codec<MonoColorSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Fade.CODEC.fieldOf("fade").forGetter(AbstractSkybox::getFade),
            Utils.getClampedFloat(.0F, 1.0F).optionalFieldOf("maxAlpha", 1.0F).forGetter(AbstractSkybox::getMaxAlpha),
            Utils.getClampedFloat(.0F, 1.0F).optionalFieldOf("transitionSpeed", 1.0F).forGetter(AbstractSkybox::getTransitionSpeed),
            Codec.BOOL.optionalFieldOf("changeFog", false).forGetter(AbstractSkybox::isChangeFog),
            RGBA.CODEC.optionalFieldOf("fogColors", RGBA.ZERO).forGetter(AbstractSkybox::getFogColors),
            Codec.BOOL.optionalFieldOf("shouldRotate", false).forGetter(AbstractSkybox::isShouldRotate),
            Weather.CODEC.listOf().optionalFieldOf("weather", Lists.newArrayList(Weather.values())).forGetter((box) -> box.getWeather().stream().map(Weather::fromString).collect(Collectors.toList())),
            Identifier.CODEC.listOf().optionalFieldOf("biomes", ImmutableList.of()).forGetter(AbstractSkybox::getBiomes),
            Identifier.CODEC.listOf().optionalFieldOf("dimensions", ImmutableList.of()).forGetter(AbstractSkybox::getDimensions),
            HeightEntry.CODEC.listOf().optionalFieldOf("heightRanges", ImmutableList.of()).forGetter(AbstractSkybox::getHeightRanges),
            RGBA.CODEC.fieldOf("color").forGetter(MonoColorSkybox::getColor),
            Decorations.CODEC.optionalFieldOf("decorations", Decorations.DEFAULT).forGetter(AbstractSkybox::getDecorations)
    ).apply(instance, MonoColorSkybox::new));
    private RGBA color;

    public MonoColorSkybox() {
    }

    public MonoColorSkybox(Fade fade, float maxAlpha, float transitionSpeed, boolean changeFog, RGBA fogColors, boolean shouldRotate, List<Weather> weather, List<Identifier> biomes, List<Identifier> dimensions, List<HeightEntry> heightRanges, RGBA color, Decorations decorations) {
        super(fade, maxAlpha, transitionSpeed, changeFog, fogColors, shouldRotate, weather.stream().map(Weather::toString).collect(Collectors.toList()), biomes, dimensions, heightRanges, decorations);
        this.color = color;
    }

    @Override
    public void render(WorldRendererAccess worldRendererAccess, MatrixStack matrices, float tickDelta) {
        if (this.alpha > 0) {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientWorld world = Objects.requireNonNull(client.world);
            RenderSystem.disableTexture();
            BackgroundRenderer.setFogBlack();
            BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
            RenderSystem.depthMask(false);
            RenderSystem.enableFog();
            RenderSystem.color3f(this.color.getRed(), this.color.getGreen(), this.color.getBlue());
            worldRendererAccess.getLightSkyBuffer().bind();
            worldRendererAccess.getSkyVertexFormat().startDrawing(0L);
            worldRendererAccess.getLightSkyBuffer().draw(matrices.peek().getModel(), 7);
            VertexBuffer.unbind();
            worldRendererAccess.getSkyVertexFormat().endDrawing();
            RenderSystem.disableFog();
            RenderSystem.disableAlphaTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            float[] skyColor = world.getSkyProperties().getSkyColor(world.getSkyAngle(tickDelta), tickDelta);
            float skySide;
            float skyColorGreen;
            float o;
            float p;
            float q;
            if (skyColor != null) {
                RenderSystem.disableTexture();
                RenderSystem.shadeModel(7425);
                matrices.push();
                matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(90.0F));
                skySide = MathHelper.sin(world.getSkyAngleRadians(tickDelta)) < 0.0F ? 180.0F : 0.0F;
                matrices.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(skySide));
                matrices.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(90.0F));
                float skyColorRed = skyColor[0];
                skyColorGreen = skyColor[1];
                float skyColorBlue = skyColor[2];
                Matrix4f matrix4f = matrices.peek().getModel();
                bufferBuilder.begin(6, VertexFormats.POSITION_COLOR);
                bufferBuilder.vertex(matrix4f, 0.0F, 100.0F, 0.0F).color(skyColorRed, skyColorGreen, skyColorBlue, skyColor[3]).next();

                for (int n = 0; n <= 16; ++n) {
                    o = (float) n * 6.2831855F / 16.0F;
                    p = MathHelper.sin(o);
                    q = MathHelper.cos(o);
                    bufferBuilder.vertex(matrix4f, p * 120.0F, q * 120.0F, -q * 40.0F * skyColor[3]).color(skyColor[0], skyColor[1], skyColor[2], 0.0F).next();
                }

                bufferBuilder.end();
                BufferRenderer.draw(bufferBuilder);
                matrices.pop();
                RenderSystem.shadeModel(7424);
            }

            this.renderDecorations(worldRendererAccess, matrices, tickDelta, bufferBuilder, this.alpha);

            RenderSystem.disableTexture();
            RenderSystem.color3f(0.0F, 0.0F, 0.0F);
            assert client.player != null;
            double d = client.player.getCameraPosVec(tickDelta).y - world.getLevelProperties().getSkyDarknessHeight();
            if (d < 0.0D) {
                matrices.push();
                matrices.translate(0.0D, 12.0D, 0.0D);
                worldRendererAccess.getDarkSkyBuffer().bind();
                worldRendererAccess.getSkyVertexFormat().startDrawing(0L);
                worldRendererAccess.getDarkSkyBuffer().draw(matrices.peek().getModel(), 7);
                VertexBuffer.unbind();
                worldRendererAccess.getSkyVertexFormat().endDrawing();
                matrices.pop();
            }

            if (world.getSkyProperties().isAlternateSkyColor()) {
                RenderSystem.color3f(this.color.getRed() * 0.2F + 0.04F, this.color.getBlue() * 0.2F + 0.04F, this.color.getGreen() * 0.6F + 0.1F);
            } else {
                RenderSystem.color3f(this.color.getRed(), this.color.getBlue(), this.color.getGreen());
            }

            RenderSystem.enableTexture();
            RenderSystem.depthMask(true);
            RenderSystem.disableFog();
        }
    }

    @Override
    public Codec<? extends AbstractSkybox> getCodec(int schemaVersion) {
        if (schemaVersion == 2) {
            return CODEC;
        }
        return null;
    }

    @Override
    public String getType() {
        return "monocolor";
    }

    @Override
    public void parseJson(JsonObjectWrapper jsonObjectWrapper) {
        super.parseJson(jsonObjectWrapper);
        try {
            this.color = new RGBA(jsonObjectWrapper.get("red").getAsFloat(), jsonObjectWrapper.get("blue").getAsFloat(), jsonObjectWrapper.get("green").getAsFloat());
        } catch (NullPointerException e) {
            throw new JsonParseException("Could not get a required field for skybox of type " + this.getType());
        }
    }

    public RGBA getColor() {
        return this.color;
    }
}
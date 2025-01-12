package mods.cybercat.gigeresque.client.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.azure.azurelib.renderer.GeoEntityRenderer;
import mods.cybercat.gigeresque.client.entity.model.AlienEntityModel;
import mods.cybercat.gigeresque.client.entity.render.feature.ClassicAlienFeatureRenderer;
import mods.cybercat.gigeresque.common.entity.impl.classic.ClassicAlienEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class AlienEntityRenderer extends GeoEntityRenderer<ClassicAlienEntity> {

    public AlienEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new AlienEntityModel());
        this.shadowRadius = 0.5f;
        this.addRenderLayer(new ClassicAlienFeatureRenderer(this));
    }

    @Override
    public void render(ClassicAlienEntity entity, float entityYaw, float partialTick, PoseStack stack, @NotNull MultiBufferSource bufferSource, int packedLightIn) {
        var scaleFactor = 0.8f + ((entity.getGrowth() / entity.getMaxGrowth()) / 5f);
        stack.scale(scaleFactor, scaleFactor, scaleFactor);
        if (entity.onGround() && !entity.isVehicle()) {
            stack.translate(0, -0.2, 0);
        }
        super.render(entity, entityYaw, partialTick, stack, bufferSource, packedLightIn);
    }

    @Override
    protected float getDeathMaxRotation(ClassicAlienEntity entityLivingBaseIn) {
        return 0;
    }

    @Override
    public float getMotionAnimThreshold(ClassicAlienEntity animatable) {
        return !animatable.isExecuting() && animatable.isVehicle() ? 0.000f : 0.005f;
    }
}

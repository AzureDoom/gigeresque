package mods.cybercat.gigeresque.client.entity.model;

import mod.azure.azurelib.model.DefaultedEntityGeoModel;
import mods.cybercat.gigeresque.Constants;
import mods.cybercat.gigeresque.common.entity.impl.runner.RunnerAlienEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class RunnerAlienEntityModel extends DefaultedEntityGeoModel<RunnerAlienEntity> {

    public RunnerAlienEntityModel() {
        super(Constants.modResource("runner_alien/runner_alien"), true);
    }

    @Override
    public RenderType getRenderType(RunnerAlienEntity animatable, ResourceLocation texture) {
        return RenderType.entityTranslucent(getTextureResource(animatable));
    }
}

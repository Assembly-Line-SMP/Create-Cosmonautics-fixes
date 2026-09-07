package dev.devce.rocketnautics.client.render.spaceRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.devce.rocketnautics.RocketConfig;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.content.RocketDimensions;
import dev.devce.rocketnautics.content.orbit.DeepSpaceData;
import dev.devce.rocketnautics.content.orbit.universe.CubePlanet;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.orekit.time.AbsoluteDate;

@EventBusSubscriber(modid = RocketNautics.MODID, value = Dist.CLIENT)
public final class SpaceRenderer {
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent e) {
        boolean isModern = dev.devce.rocketnautics.RocketConfig.CLIENT.skyRenderingSystem.get() == dev.devce.rocketnautics.RocketConfig.SkyRenderingSystem.MODERN;
        if (!isModern) return;

        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        if (UniverseHelper.receivedPositionTick == -1 || UniverseHelper.UNIVERSE == null) return;

        Minecraft mc = Minecraft.getInstance();

        PoseStack ps = e.getPoseStack();

        ps.pushPose();
        ps.mulPose(e.getModelViewMatrix()); // AFTER_SKY renders before the model view matrix is normally applied

        if (mc.level == null) return;
        ResourceKey<Level> dimension = mc.level.dimension();

        float partial = e.getPartialTick().getGameTimeDeltaPartialTick(true);
        AbsoluteDate date = UniverseHelper.getRenderDate(partial);

        if (dimension == RocketDimensions.DEEP_SPACE) {
            Vec3 pos = e.getCamera().getPosition();
            VoxelShape box = DeepSpaceData.getBoxForPosition(pos);

            if (box.bounds().contains(pos))
                UniverseRenderer.render(null, ps,
                    e.getPartialTick().getGameTimeDeltaTicks(), partial,
                    UniverseHelper.receivedPosition.getPosition(date),
                    UniverseHelper.receivedPosition.getFrame(),
                    date, e.getCamera());

            ps.pushPose();
            IBRenderer.render(ps, pos);
            ps.popPose();
        } else {
            if (!RocketConfig.CLIENT.enableCustomSky.get()) return;

            CubePlanet planet = UniverseHelper.UNIVERSE.getPlanets().stream()
                    .filter(p -> {
                        if (p.linkedDimension() == null) return false;
                        return p.linkedDimension().key() == dimension;
                    })
                    .findFirst()
                    .orElse(null);

            UniverseRenderer.render(planet, ps,
                    e.getPartialTick().getGameTimeDeltaTicks(), partial,
                    UniverseHelper.receivedPosition.getPosition(date),
                    UniverseHelper.receivedPosition.getFrame(),
                    date, e.getCamera());
        }

        ps.popPose();
    }
}

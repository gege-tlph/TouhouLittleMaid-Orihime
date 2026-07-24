package com.github.tartaricacid.touhoulittlemaid.client.particle;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.context.IContext;
import com.github.tartaricacid.touhoulittlemaid.molang.runtime.ExecutionContext;
import com.github.tartaricacid.touhoulittlemaid.molang.runtime.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector3d;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ParticleSpawner {
    private static final Cache<String, ParticleOptions> PARTICLE_OPTIONS_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(60, TimeUnit.SECONDS).build();

    public static boolean evalSpawnParticle(ExecutionContext<IContext<Entity>> context,
                                            Function.ArgumentCollection arguments,
                                            boolean absPos) throws ExecutionException, CommandSyntaxException {
        String id = arguments.getAsString(context, 0);
        if (StringUtils.isBlank(id)) {
            return false;
        }

        Vector3d pos = new Vector3d();
        Vector3d delta = new Vector3d();
        double particleSpeed = 0;
        int count = 0;
        int particleLifeTime = 20;
        int size = arguments.size();
        if (size > 1) pos.x = arguments.getAsDouble(context, 1);
        if (size > 2) pos.y = arguments.getAsDouble(context, 2);
        if (size > 3) pos.z = arguments.getAsDouble(context, 3);
        if (size > 4) delta.x = arguments.getAsDouble(context, 4);
        if (size > 5) delta.y = arguments.getAsDouble(context, 5);
        if (size > 6) delta.z = arguments.getAsDouble(context, 6);
        if (size > 7) particleSpeed = arguments.getAsDouble(context, 7);
        if (size > 8) count = Math.max(arguments.getAsInt(context, 8), 0);
        if (size > 9) particleLifeTime = Math.max(arguments.getAsInt(context, 9), 1);

        spawnParticle(context.entity().entity(), id, pos, delta, particleSpeed, count,
                particleLifeTime, absPos, context.entity().random());
        return true;
    }

    private static void spawnParticle(Entity entity, String id, Vector3d pos, Vector3d delta,
                                      double particleSpeed, int count, int particleLifeTime,
                                      boolean absPos, RandomSource random)
            throws CommandSyntaxException, ExecutionException {
        ParticleOptions options = PARTICLE_OPTIONS_CACHE.get(id, () ->
                ParticleArgument.readParticle(new StringReader(id), entity.level().registryAccess()));
        ParticleEngine engine = Minecraft.getInstance().particleEngine;
        if (count == 0) {
            Vec3 offset = new Vec3(pos.x(), pos.y(), pos.z());
            if (!absPos) {
                offset = offset.yRot(-(entity instanceof Player player ? player.yBodyRot : entity.getYRot()) * Mth.DEG_TO_RAD);
            }
            double x = entity.getX() + offset.x();
            double y = entity.getY() + offset.y();
            double z = entity.getZ() + offset.z();
            double vx = particleSpeed * delta.x();
            double vy = particleSpeed * delta.y();
            double vz = particleSpeed * delta.z();
            Minecraft.getInstance().execute(() -> setLifetime(engine.createParticle(options, x, y, z, vx, vy, vz), particleLifeTime));
            return;
        }

        for (int i = 0; i < count; i++) {
            createParticle(entity, pos, delta, particleSpeed, particleLifeTime, engine, options, absPos, random);
        }
    }

    private static void createParticle(Entity entity, Vector3d pos, Vector3d delta,
                                       double particleSpeed, int particleLifeTime,
                                       ParticleEngine engine, ParticleOptions options,
                                       boolean absPos, RandomSource random) {
        double ox = random.nextGaussian() * delta.x();
        double oy = random.nextGaussian() * delta.y();
        double oz = random.nextGaussian() * delta.z();
        Vec3 offset = new Vec3(pos.x() + ox, pos.y() + oy, pos.z() + oz);
        if (!absPos) {
            offset = offset.yRot(-entity.getYRot() * Mth.DEG_TO_RAD);
        }
        double x = entity.getX() + offset.x();
        double y = entity.getY() + offset.y();
        double z = entity.getZ() + offset.z();
        double vx = random.nextGaussian() * particleSpeed;
        double vy = random.nextGaussian() * particleSpeed;
        double vz = random.nextGaussian() * particleSpeed;
        Minecraft.getInstance().execute(() -> setLifetime(engine.createParticle(options, x, y, z, vx, vy, vz), particleLifeTime));
    }

    private static void setLifetime(Particle particle, int lifetime) {
        if (particle != null) {
            particle.setLifetime(lifetime);
        }
    }
}

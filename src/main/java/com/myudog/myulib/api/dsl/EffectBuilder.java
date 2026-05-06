package com.myudog.myulib.api.dsl;
import com.myudog.myulib.api.Shapes;
import com.myudog.myulib.api.color.ColorProvider;
import com.myudog.myulib.api.dynamics.IForceField;
import com.myudog.myulib.api.floating.IFloatingObject;
import com.myudog.myulib.api.shape.IShape;
import com.myudog.myulib.internal.monitor.VFXMonitor;
import com.myudog.myulib.internal.scheduler.EffectTicker;
import com.myudog.myulib.internal.state.ParticleState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
public final class EffectBuilder {
    @FunctionalInterface
    public interface ParticleRenderCallback {
        void render(Vec3 pos, Vector3f color, float progress);
    }
    public static final class ForceFieldBinding {
        public final IForceField field;
        public final double strength;
        public ForceFieldBinding(IForceField field, double strength) {
            this.field = field;
            this.strength = strength;
        }
    }
    private int duration = 20;
    private final List<IntConsumer> tickActions = new ArrayList<>();
    private Supplier<Vec3> centerProvider;
    public EffectBuilder(Vec3 center) {
        this.centerProvider = () -> center;
    }
    public void duration(int ticks) {
        this.duration = Math.max(0, ticks);
    }
    public void follow(Entity entity, Vec3 offset) {
        this.centerProvider = () -> new Vec3(entity.getX(), entity.getY(), entity.getZ()).add(offset);
    }
    public void follow(Supplier<Vec3> provider) {
        this.centerProvider = provider;
    }
    private Vec3 getCurrentCenter() {
        return centerProvider.get();
    }
    public void onTick(IntConsumer action) {
        tickActions.add(action);
    }
    public void bindObject(IFloatingObject obj, IntFunction<Vec3> path) {
        onTick(tick -> obj.moveTo(path.apply(tick), 1));
    }
    public void renderShape(
            IShape shape,
            boolean isSolid,
            Vec3 size,
            double density,
            String colorStart,
            String colorEnd,
            int rainbowPeriod,
            List<ForceFieldBinding> forceFields,
            double friction,
            boolean useCollision,
            double bounce,
            int minLife,
            int maxLife,
            ParticleRenderCallback onRender
    ) {
        List<Vec3> points = isSolid ? shape.getSolidPoints(size, density) : shape.getOutlinePoints(size, density);
        List<ParticleState> particles = new ArrayList<>(points.size());
        for (Vec3 point : points) {
            int life = minLife + (int) (Math.random() * Math.max(1, maxLife - minLife + 1));
            particles.add(new ParticleState(point, new Vec3(0.0, 0.0, 0.0), 0, life));
        }
        Vector3f startRGB = ColorProvider.hexToRGB(colorStart);
        Vector3f endRGB = colorEnd == null ? null : ColorProvider.hexToRGB(colorEnd);
        List<ForceFieldBinding> safeForceFields = forceFields == null ? List.of() : forceFields;
        onTick(tick -> {
            if (!VFXMonitor.requestSpawn(particles.size())) {
                return;
            }
            Vec3 currentCenter = getCurrentCenter();
            Iterator<ParticleState> iterator = particles.iterator();
            while (iterator.hasNext()) {
                ParticleState particle = iterator.next();
                particle.age++;
                if (particle.isDead()) {
                    iterator.remove();
                    continue;
                }
                Vec3 totalForce = new Vec3(0.0, 0.0, 0.0);
                for (ForceFieldBinding binding : safeForceFields) {
                    totalForce = totalForce.add(binding.field.calculateForce(particle.pos, currentCenter, binding.strength));
                }
                particle.vel = particle.vel.add(totalForce).scale(friction);
                if (useCollision) {
                    double damp = Math.max(0.0, 1.0 - Math.max(0.0, bounce));
                    particle.vel = particle.vel.scale(damp);
                }
                particle.pos = particle.pos.add(particle.vel);
                Vector3f currentColor;
                if (rainbowPeriod > 0) {
                    currentColor = ColorProvider.getRainbowColor(tick + (int) (particle.randomSeed * 100.0), rainbowPeriod);
                } else if (endRGB != null) {
                    currentColor = ColorProvider.lerp(startRGB, endRGB, particle.getProgress());
                } else {
                    currentColor = startRGB;
                }
                onRender.render(currentCenter.add(particle.pos), currentColor, particle.getProgress());
            }
        });
    }
    public void forCircle(double radius, double density, Consumer<Vec3> action) {
        onTick(tick -> {
            Vec3 centerPos = getCurrentCenter();
            for (Vec3 offset : Shapes.CIRCLE.getOutlinePoints(new Vec3(radius, 0.0, radius), density)) {
                action.accept(centerPos.add(offset));
            }
        });
    }
    public void build() {
        final int[] current = {0};
        EffectTicker.addTask(() -> {
            if (current[0] >= duration) {
                return false;
            }
            for (IntConsumer action : tickActions) {
                action.accept(current[0]);
            }
            current[0]++;
            return true;
        });
    }
    public static void spawnEffect(Vec3 center, Consumer<EffectBuilder> setup) {
        EffectBuilder builder = new EffectBuilder(center);
        setup.accept(builder);
        builder.build();
    }
}
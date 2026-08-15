package cn.sh1rocu.touhoulittlemaid.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 爆炸事件（Forge 形态的 Fabric 壳）。
 *
 * <p>DETONATE 的触发点在 {@code mixin/common/ServerExplosionMixin}：26.1.2 的爆炸重构后
 * {@code Explosion} 是接口、伤害流程在 {@code ServerExplosion.hurtEntities()} 内部，
 * 故触发点从 origin/1.21.1 的 {@code Explosion.explode} 局部捕获改为包住
 * {@code level.getEntities(...)} 的取表调用——监听器从可变列表里移除实体即等价于免疫爆炸。
 * ⚠️ 1.21.11 分支迁移期丢了这个触发点（事件类在、触发点无），该分支上「女仆免疫子弹爆炸」
 * 从不生效；本树按 origin/1.21.1 行为补齐。</p>
 */
public class ExplosionEvents {
    public static Event<Start> START = EventFactory.createArrayBacked(Start.class, (callbacks) -> (world, explosion) -> {
        for (Start event : callbacks) {
            if (event.onExplosionStart(world, explosion)) {
                return true;
            }
        }

        return false;
    });
    public static Event<Detonate> DETONATE = EventFactory.createArrayBacked(Detonate.class, (callbacks) -> (world, explosion, entities, diameter) -> {
        for (Detonate event : callbacks) {
            event.onDetonate(world, explosion, entities, diameter);
        }

    });

    public ExplosionEvents() {
    }

    public interface Detonate {
        void onDetonate(Level world, Explosion explosion, List<Entity> entities, double diameter);
    }

    public interface Start {
        boolean onExplosionStart(Level world, Explosion explosion);
    }
}

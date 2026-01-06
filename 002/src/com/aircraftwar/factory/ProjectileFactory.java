package com.aircraftwar.factory;

import com.aircraftwar.entity.Bullet;
import com.aircraftwar.entity.EnemyBullet;
import com.aircraftwar.entity.IBullet;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 可注册的 ProjectileFactory：按弹种字符串创建弹体。当前注册默认的 player_basic / enemy_basic。
 */
public class ProjectileFactory {
    public interface Maker {
        IBullet make(Map<String, Object> params);
    }

    private static final Map<String, Maker> registry = new HashMap<>();

    static {
        // register defaults
        registry.put("player_basic", params -> new Bullet((int) params.getOrDefault("x", 0), (int) params.getOrDefault("y", 0), (int) params.getOrDefault("w", 40)));
        registry.put("enemy_basic", params -> new EnemyBullet((int) params.getOrDefault("x", 0), (int) params.getOrDefault("y", 0)));
    }

    public static void register(String type, Maker maker) {
        registry.put(type, maker);
    }

    public static IBullet create(String type, Map<String, Object> params) {
        Maker m = registry.get(type);
        if (m != null) return m.make(params);
        // fallback to enemy_basic
        return registry.get("enemy_basic").make(params);
    }

    // Backward-compatible helpers
    public static IBullet createPlayerBullet(int playerX, int playerY, int playerWidth) {
        Map<String, Object> p = new HashMap<>();
        p.put("x", playerX);
        p.put("y", playerY);
        p.put("w", playerWidth);
        return create("player_basic", p);
    }

    public static IBullet createEnemyBullet(int x, int y) {
        Map<String, Object> p = new HashMap<>();
        p.put("x", x);
        p.put("y", y);
        return create("enemy_basic", p);
    }
}

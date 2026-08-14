package com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.built;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

// 不要用枚举
public class GeoLocatorType {
    public static final GeoLocatorType LEFT_HAND = new GeoLocatorType("LeftHandLocator");
    public static final GeoLocatorType RIGHT_HAND = new GeoLocatorType("RightHandLocator");
    public static final GeoLocatorType BACKPACK = new GeoLocatorType("BackpackLocator");
    public static final GeoLocatorType HEAD = new GeoLocatorType("Head");
    // TaCZ 背枪定位点。模型骨骼叫 PistolLocator / RifleLocator，
    // 由 RawGeometryTree 的 getByName 自动归组——这两行注释掉的那段时间里，
    // 模型即使有这两根骨骼也会被判成「不是定位点」，背枪渲染无从谈起。
    public static final GeoLocatorType TAC_PISTOL = new GeoLocatorType("PistolLocator");
    public static final GeoLocatorType TAC_RIFLE = new GeoLocatorType("RifleLocator");
/*
    public static final GeoLocatorType LEFT_WAIST = new GeoLocatorType("LeftWaistLocator");
    public static final GeoLocatorType RIGHT_WAIST = new GeoLocatorType("RightWaistLocator");
*/

    private final String name;
    private final byte seq;

    public GeoLocatorType(String name) {
        if (Inner.FROZEN.getAcquire()) {
            throw new IllegalStateException();
        }
        this.name = name;
        synchronized (Inner.NAME_MAP) {
            this.seq = (byte) Inner.NAME_MAP.size();
            Inner.NAME_MAP.put(name, this);
        }
    }

    public String getName() {
        return name;
    }

    public byte getSeq() {
        return seq;
    }

    public static int size() {
        return Inner.NAME_MAP.size();
    }

    @Nullable
    public static GeoLocatorType getByName(String name) {
        return Inner.NAME_MAP.get(name);
    }

    public static void freeze() {
        Inner.FROZEN.setRelease(true);
    }

    private static class Inner {
        private static final Object2ReferenceOpenHashMap<String, GeoLocatorType> NAME_MAP = new Object2ReferenceOpenHashMap<>(16);
        private static final AtomicBoolean FROZEN = new AtomicBoolean(false);
    }
}

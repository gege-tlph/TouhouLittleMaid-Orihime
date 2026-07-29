package com.github.tartaricacid.touhoulittlemaid.crafting;

import javax.annotation.Nullable;
import net.minecraft.world.item.Item;
import net.minecraft.core.Holder;
import java.util.stream.Stream;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Objects;

public final class FallbackIngredient implements CustomIngredient {
    private static final Codec<JsonElement> JSON_ELEMENT_CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
            json -> new Dynamic<>(JsonOps.INSTANCE, json)
    );

    public static final MapCodec<FallbackIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FallbackEntry.CODEC.listOf().fieldOf("fallbacks").forGetter(FallbackIngredient::fallbacks)
    ).apply(instance, FallbackIngredient::new));

    private final List<FallbackEntry> fallbacks;
    private final @Nullable Ingredient resolvedIngredient;

    public FallbackIngredient(List<FallbackEntry> fallbacks) {
        this.fallbacks = List.copyOf(fallbacks);
        this.resolvedIngredient = resolveIngredient(this.fallbacks);
    }

    public List<FallbackEntry> fallbacks() {
        return this.fallbacks;
    }

    // 1.21.11 / Fabric API 8.2.x：CustomIngredient.getMatchingStacks(): List<ItemStack>
    // 已改为 getMatchingItems(): Stream<Holder<Item>>；Ingredient.getItems() 亦更名为 items()。
    // 另：Ingredient 不再可为空（底层是 NON_AIR_HOLDER_SET_CODEC），Ingredient.EMPTY 已移除，
    // 故 resolvedIngredient 改为 @Nullable —— null 即表示「无可用回退，什么都不匹配」，
    // 与 HEAD 中 Ingredient.EMPTY 的语义等价。
    @Override
    public boolean test(ItemStack stack) {
        return this.resolvedIngredient != null && this.resolvedIngredient.test(stack);
    }

    @Override
    public Stream<Holder<Item>> getMatchingItems() {
        return this.resolvedIngredient == null ? Stream.empty() : this.resolvedIngredient.items();
    }

    @Override
    public boolean requiresTesting() {
        return this.resolvedIngredient != null && this.resolvedIngredient.requiresTesting();
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    /** @return 首个「mod 已加载且 JSON 解析成功」的 Ingredient；均不满足时返回 null（= 什么都不匹配） */
    @Nullable
    private static Ingredient resolveIngredient(List<FallbackEntry> fallbacks) {
        for (FallbackEntry entry : fallbacks) {
            if (!FabricLoader.getInstance().isModLoaded(entry.modid())) {
                continue;
            }
            var parsed = Ingredient.CODEC.parse(JsonOps.INSTANCE, entry.value())
                    .resultOrPartial(message -> TouhouLittleMaid.LOGGER.error("Failed to parse fallback ingredient for mod {}: {}", entry.modid(), message));
            if (parsed.isPresent()) {
                return parsed.get();
            }
        }
        return null;
    }

    public record FallbackEntry(String modid, JsonElement value) {
        public static final Codec<FallbackEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("modid").forGetter(FallbackEntry::modid),
                JSON_ELEMENT_CODEC.fieldOf("value").forGetter(FallbackEntry::value)
        ).apply(instance, FallbackEntry::new));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof FallbackIngredient that)) {
            return false;
        }
        return Objects.equals(this.fallbacks, that.fallbacks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.fallbacks);
    }

    public static final Identifier ID = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "fallback_ingredient");

    public static class Serializer implements CustomIngredientSerializer<FallbackIngredient> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public Identifier getIdentifier() {
            return ID;
        }

        @Override
        public MapCodec<FallbackIngredient> getCodec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FallbackIngredient> getPacketCodec() {
            return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        }
    }
}

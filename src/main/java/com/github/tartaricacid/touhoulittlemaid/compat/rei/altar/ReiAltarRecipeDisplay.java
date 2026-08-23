package com.github.tartaricacid.touhoulittlemaid.compat.rei.altar;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.github.tartaricacid.touhoulittlemaid.compat.rei.MaidREIClientPlugin;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

public class ReiAltarRecipeDisplay implements Display {
    public static final DisplaySerializer<ReiAltarRecipeDisplay> SERIALIZER = DisplaySerializer.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Identifier.CODEC.fieldOf("id").forGetter(ReiAltarRecipeDisplay::getId),
                    EntryIngredient.codec().listOf().fieldOf("inputs").forGetter(Display::getInputEntries),
                    EntryIngredient.codec().listOf().fieldOf("outputs").forGetter(Display::getOutputEntries),
                    com.mojang.serialization.Codec.FLOAT.fieldOf("power_cost").forGetter(ReiAltarRecipeDisplay::getPowerCost),
                    com.mojang.serialization.Codec.STRING.fieldOf("lang_key").forGetter(ReiAltarRecipeDisplay::getLangKey)
            ).apply(instance, ReiAltarRecipeDisplay::new)),
            StreamCodec.composite(
                    Identifier.STREAM_CODEC, ReiAltarRecipeDisplay::getId,
                    EntryIngredient.streamCodec().apply(ByteBufCodecs.list()), Display::getInputEntries,
                    EntryIngredient.streamCodec().apply(ByteBufCodecs.list()), Display::getOutputEntries,
                    ByteBufCodecs.FLOAT, ReiAltarRecipeDisplay::getPowerCost,
                    ByteBufCodecs.STRING_UTF8, ReiAltarRecipeDisplay::getLangKey,
                    ReiAltarRecipeDisplay::new));

    private final Identifier id;
    private final List<EntryIngredient> inputs;
    private final List<EntryIngredient> outputs;
    private final float powerCost;
    private final String langKey;

    public ReiAltarRecipeDisplay(Identifier id, List<EntryIngredient> inputs,
                                 List<EntryIngredient> outputs, float powerCost, String langKey) {
        this.id = id;
        this.inputs = inputs;
        this.outputs = outputs;
        this.powerCost = powerCost;
        this.langKey = langKey;
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        return inputs;
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return outputs;
    }

    public Identifier getId() {
        return id;
    }

    public float getPowerCost() {
        return powerCost;
    }

    public String getLangKey() {
        return langKey;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return MaidREIClientPlugin.ALTAR;
    }

    @Override
    public Optional<Identifier> getDisplayLocation() {
        return Optional.of(id);
    }

    @Override
    public DisplaySerializer<? extends Display> getSerializer() {
        return SERIALIZER;
    }
}

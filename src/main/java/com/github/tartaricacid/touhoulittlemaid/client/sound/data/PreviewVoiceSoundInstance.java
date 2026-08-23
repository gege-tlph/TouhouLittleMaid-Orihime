package com.github.tartaricacid.touhoulittlemaid.client.sound.data;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.sound.OggReader;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Util;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;

/**
 * 音色试听的播放载体：与 {@link MaidAISoundInstance} 同一套解码（mp3/opus/vorbis 自动识别），
 * 但**非定位**——它不属于世界里的任何实体，跟着界面走，不随镜头衰减。
 */
@Environment(EnvType.CLIENT)
public class PreviewVoiceSoundInstance extends AbstractSoundInstance implements FabricSoundInstance {
    private final byte[] data;

    public PreviewVoiceSoundInstance(byte[] data) {
        super(InitSounds.MAID_AI_CHAT, SoundSource.MASTER, SoundInstance.createUnseededRandom());
        this.data = data;
        this.volume = 1.0f;
        this.pitch = 1.0f;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
    }

    @Override
    public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary library, Identifier sound, boolean looping) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                try {
                    return new Mp3AudioStream(this.data);
                } catch (UnsupportedAudioFileException e) {
                    OggReader.Type oggType = OggReader.getOggType(this.data);
                    if (oggType.equals(OggReader.Type.OPUS)) {
                        return new OpusAudioStream(this.data);
                    }
                    if (oggType.equals(OggReader.Type.VORBIS)) {
                        return new JOrbisAudioStream(new ByteArrayInputStream(this.data));
                    }
                }
            } catch (Exception e) {
                TouhouLittleMaid.LOGGER.error(e);
            }
            return null;
        }, Util.backgroundExecutor());
    }
}

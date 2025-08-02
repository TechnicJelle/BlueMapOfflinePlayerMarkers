package com.technicjelle.bluemapofflineplayermarkers.impl.fabric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record BukkitCodec(Long lastPlayed) {
    public static final Codec<BukkitCodec> CODEC = RecordCodecBuilder.create(
            (instance) -> instance.group(
                    Codec.LONG
                            .fieldOf("lastPlayed")
                            .orElse(0L)
                            .forGetter((codec) -> codec.lastPlayed)
            ).apply(instance, BukkitCodec::new));
}

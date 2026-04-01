package com.technicjelle.bluemapofflineplayermarkers.impl.fabric.mixin;

import com.technicjelle.bluemapofflineplayermarkers.impl.fabric.BukkitCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @Unique
    Long lastPlayed = null;

    @Inject(method = "disconnect", at = @At("TAIL"))
    private void getDisconnectTime(CallbackInfo ci) {
        lastPlayed = System.currentTimeMillis();
    }

    @Inject(method = "readAdditionalSaveData", at = @At(value = "TAIL"))
    private void readBukkitNbt(ValueInput input, CallbackInfo ci) {
        var bukkit = input.read("bukkit", BukkitCodec.CODEC);
        if (bukkit.isEmpty()) return;

        lastPlayed = bukkit.get().lastPlayed();
    }

    @Inject(method = "addAdditionalSaveData", at = @At(value = "TAIL"))
    private void writeBukkitNbt(ValueOutput output, CallbackInfo ci) {
        if (lastPlayed != null) {
            output.store("bukkit", BukkitCodec.CODEC, new BukkitCodec(lastPlayed));
        }
    }
}

package com.technicjelle.bluemapofflineplayermarkers.impl.fabric.mixin;

import com.technicjelle.bluemapofflineplayermarkers.impl.fabric.BukkitCodec;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    @Unique
    Long lastPlayed = null;

    @Inject(method = "onDisconnect", at = @At("TAIL"))
    private void getDisconnectTime(CallbackInfo ci) {
        lastPlayed = System.currentTimeMillis();
    }

    @Inject(method = "readCustomData", at = @At(value = "TAIL"))
    private void readBukkitNbt(ReadView view, CallbackInfo ci) {
        var bukkit = view.read("bukkit", BukkitCodec.CODEC);
        if (bukkit.isEmpty()) return;

        lastPlayed = bukkit.get().lastPlayed();
    }

    @Inject(method = "writeCustomData", at = @At(value = "TAIL"))
    private void writeBukkitNbt(WriteView view, CallbackInfo ci) {
        if (lastPlayed != null) {
            view.put("bukkit", BukkitCodec.CODEC, new BukkitCodec(lastPlayed));
        }
    }
}

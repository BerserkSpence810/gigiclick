package com.gigiclick.test.mixin;

import com.gigiclick.test.ServerCounters;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
	@Inject(method = "handleAnimate", at = @At("RETURN"))
	private void gigiclickTest$countSwing(ServerboundSwingPacket packet, CallbackInfo ci) {
		ServerCounters.SWINGS.incrementAndGet();
	}

	@Inject(method = "handleAttack", at = @At("RETURN"))
	private void gigiclickTest$countAttack(ServerboundAttackPacket packet, CallbackInfo ci) {
		ServerCounters.ATTACKS.incrementAndGet();
	}
}

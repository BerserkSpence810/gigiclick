package com.gigiclick.mixin;

import com.gigiclick.AutoClicker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "runTick", at = @At("HEAD"))
	private void gigiclick$onFrame(boolean renderLevel, CallbackInfo ci) {
		AutoClicker.onFrame((Minecraft) (Object) this);
	}

	@Inject(method = "startAttack", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V"))
	private void gigiclick$onSwing(CallbackInfoReturnable<Boolean> cir) {
		AutoClicker.recordSwing();
	}
}

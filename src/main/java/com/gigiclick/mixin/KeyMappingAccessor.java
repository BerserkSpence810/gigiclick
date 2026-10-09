package com.gigiclick.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
	@Accessor("clickCount")
	int gigiclick$getClickCount();

	@Accessor("clickCount")
	void gigiclick$setClickCount(int clickCount);
}

package com.gigiclick;

import com.gigiclick.mixin.KeyMappingAccessor;
import net.minecraft.client.Minecraft;

import java.util.concurrent.ThreadLocalRandom;

public final class AutoClicker {
	private static final long SECOND_NANOS = 1_000_000_000L;

	private static boolean active;
	private static long nextClickNanos;

	private static final long[] recentSwings = new long[64];
	private static int recentSwingsHead;
	private static long totalSwings;

	private AutoClicker() {
	}

	public static void onFrame(Minecraft mc) {
		if (!shouldClick(mc)) {
			active = false;
			return;
		}

		long now = System.nanoTime();
		if (!active) {
			active = true;
			nextClickNanos = now + randomIntervalNanos();
			return;
		}

		if (now >= nextClickNanos) {
			KeyMappingAccessor attack = (KeyMappingAccessor) mc.options.keyAttack;
			attack.gigiclick$setClickCount(attack.gigiclick$getClickCount() + 1);
			nextClickNanos += randomIntervalNanos();
			if (nextClickNanos < now) {
				nextClickNanos = now + randomIntervalNanos();
			}
		}
	}

	public static boolean isActive() {
		return active;
	}

	public static int currentCps() {
		long cutoff = System.nanoTime() - SECOND_NANOS;
		int count = 0;
		for (long time : recentSwings) {
			if (time != 0 && time > cutoff) {
				count++;
			}
		}
		return count;
	}

	public static long totalSwings() {
		return totalSwings;
	}

	public static void recordSwing() {
		totalSwings++;
		recentSwings[recentSwingsHead] = System.nanoTime();
		recentSwingsHead = (recentSwingsHead + 1) % recentSwings.length;
	}

	private static boolean shouldClick(Minecraft mc) {
		return GigiClick.config.enabled
				&& mc.player != null
				&& mc.gameMode != null
				&& mc.gui.screen() == null
				&& mc.gui.overlay() == null
				&& mc.options.keyAttack.isDown()
				&& !mc.player.isUsingItem();
	}

	public static long randomIntervalNanos() {
		Config config = GigiClick.config;
		double cps = config.minCps == config.maxCps
				? config.minCps
				: ThreadLocalRandom.current().nextDouble(config.minCps, config.maxCps);
		return (long) (SECOND_NANOS / cps);
	}
}

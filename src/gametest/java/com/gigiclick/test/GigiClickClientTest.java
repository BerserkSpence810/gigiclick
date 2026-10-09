package com.gigiclick.test;

import com.gigiclick.GigiClick;
import com.gigiclick.ui.GigiClickScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicInteger;

public class GigiClickClientTest implements FabricClientGameTest {
	private static final BlockPos TARGET_BLOCK = new BlockPos(0, 101, 2);

	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1920, 1080);

		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("fill -3 99 -3 3 99 6 stone");
			world.getServer().runCommand("tp @a 0 100 0 0 0");
			world.getServer().runCommand("gamemode creative @a");
			context.waitTicks(10);

			context.getInput().lookAt(0f, -90f);
			setRange(context, 10, 10);
			double rate = measure(context, ServerCounters.SWINGS, 3.0);
			log("air, creative, fixed 10 CPS -> server got %.2f swings/s", rate);
			check(rate > 8.5 && rate < 11.0, "expected ~10 swings/s, got " + rate);

			setRange(context, 14, 18);
			rate = measure(context, ServerCounters.SWINGS, 3.0);
			log("air, creative, 14-18 CPS -> server got %.2f swings/s", rate);
			check(rate > 13.0 && rate < 18.5, "expected 14-18 swings/s, got " + rate);

			context.runOnClient(mc -> GigiClick.config.enabled = false);
			rate = measure(context, ServerCounters.SWINGS, 2.0);
			log("disabled -> server got %.2f swings/s", rate);
			check(rate < 0.6, "nothing should click when disabled, got " + rate);
			context.runOnClient(mc -> GigiClick.config.enabled = true);

			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("summon iron_golem 0 100 2.5 {NoAI:1b,Invulnerable:1b,Silent:1b,Rotation:[180f,0f]}");
			context.waitTicks(10);
			context.getInput().lookAt(0f, 0f);
			setRange(context, 10, 10);
			rate = measure(context, ServerCounters.ATTACKS, 3.0);
			log("mob, survival, fixed 10 CPS -> server got %.2f hits/s", rate);
			check(rate > 8.5 && rate < 11.0, "expected ~10 hits/s, got " + rate);

			setRange(context, 8, 12);
			context.getInput().holdMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
			waitSeconds(context, 1.2);
			context.takeScreenshot("gigiclick-hud-active");
			context.getInput().releaseMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
			world.getServer().runCommand("kill @e[type=iron_golem]");

			context.getInput().lookAt(0f, -90f);
			rate = measure(context, ServerCounters.SWINGS, 2.0);
			log("air, survival, 8-12 CPS -> server got %.2f swings/s (vanilla miss cooldown)", rate);

			world.getServer().runCommand("setblock 0 101 2 dirt");
			context.waitTicks(5);
			context.getInput().lookAt(0f, 0f);
			context.getInput().holdMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
			context.waitFor(mc -> mc.level.getBlockState(TARGET_BLOCK).isAir(), 100);
			context.getInput().releaseMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
			log("mining: dirt broke while holding left click");

			context.setScreen(() -> new GigiClickScreen(null));
			waitSeconds(context, 1.5);
			context.takeScreenshot("gigiclick-screen");

			double[] handle = context.computeOnClient(mc -> {
				double scale = mc.getWindow().getGuiScale();
				int screenW = mc.getWindow().getGuiScaledWidth();
				int screenH = mc.getWindow().getGuiScaledHeight();
				int panelX = (screenW - 320) / 2;
				int panelY = (screenH - 248) / 2;
				double handleX = panelX + 26 + (12 - 1) / 39.0 * (320 - 24 - 28);
				double handleY = panelY + 48 + 42;
				return new double[] {handleX * scale, handleY * scale};
			});
			context.getInput().setCursorPos(handle[0], handle[1]);
			waitSeconds(context, 0.6);
			context.takeScreenshot("gigiclick-screen-hover");
			context.setScreen(() -> null);
		}

		log("all checks passed");
	}

	private static void setRange(ClientGameTestContext context, int min, int max) {
		context.runOnClient(mc -> GigiClick.config.setRange(min, max));
	}

	private static double measure(ClientGameTestContext context, AtomicInteger counter, double seconds) {
		context.getInput().holdMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		waitSeconds(context, 0.3);
		int before = counter.get();
		long start = System.nanoTime();
		waitSeconds(context, seconds);
		int after = counter.get();
		double elapsed = (System.nanoTime() - start) / 1e9;
		context.getInput().releaseMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		context.waitTicks(5);
		return (after - before) / elapsed;
	}

	private static void waitSeconds(ClientGameTestContext context, double seconds) {
		long end = System.nanoTime() + (long) (seconds * 1e9);
		while (System.nanoTime() < end) {
			context.waitTick();
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private static void log(String format, Object... args) {
		GigiClick.LOGGER.info("[test] " + String.format(format, args));
	}
}

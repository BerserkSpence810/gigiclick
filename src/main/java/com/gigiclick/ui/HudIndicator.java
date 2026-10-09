package com.gigiclick.ui;

import com.gigiclick.AutoClicker;
import com.gigiclick.Config;
import com.gigiclick.GigiClick;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class HudIndicator {
	private static final int X = 4;
	private static final int Y = 4;

	private static final Anim visibility = new Anim(0f, 10f);
	private static final Anim activity = new Anim(0f, 14f);
	private static long lastFrameNanos;

	private HudIndicator() {
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		Config config = GigiClick.config;

		long now = System.nanoTime();
		float dt = lastFrameNanos == 0 ? 0f : Math.min(0.1f, (now - lastFrameNanos) / 1e9f);
		lastFrameNanos = now;

		boolean shouldShow = config.showHud && config.enabled && mc.player != null;
		float alpha = visibility.update(shouldShow ? 1f : 0f, dt);
		float active = activity.update(AutoClicker.isActive() ? 1f : 0f, dt);
		if (alpha <= 0.01f) {
			return;
		}

		Font font = mc.font;
		Component name = Component.translatable("gigiclick.name");
		Component detail;
		int dotColor;
		if (AutoClicker.isActive()) {
			detail = Component.translatable("gigiclick.hud.cps", AutoClicker.currentCps());
			dotColor = Theme.SUCCESS;
		} else {
			detail = Component.translatable("gigiclick.hud.range", config.minCps, config.maxCps);
			dotColor = Theme.ACCENT;
		}

		int nameW = font.width(name);
		int width = 6 + 5 + 5 + nameW + 6 + font.width(detail) + 7;
		int height = 15;

		Draw.setOpacity(alpha);
		try {
			Draw.roundRect(g, X, Y, width, height, 4, 0xC80D0D12);
			Draw.rect(g, X, Y + 3, 1, height - 6, Draw.mix(Theme.ACCENT_DEEP, Theme.SUCCESS, active));

			int dotX = X + 8;
			int dotY = Y + height / 2;
			if (active > 0.01f) {
				float pulse = 0.5f + 0.5f * (float) Math.sin(now / 1.2e8);
				Draw.circle(g, dotX, dotY, 4, Draw.withAlpha(dotColor, 0.35f * pulse * active));
			}
			Draw.circle(g, dotX, dotY, 2, dotColor);

			int textX = X + 16;
			int textY = Y + 4;
			Draw.text(g, font, name, textX, textY, Theme.TEXT);
			Draw.text(g, font, detail, textX + nameW + 6, textY, AutoClicker.isActive() ? Theme.TEXT : Theme.TEXT_SECONDARY);
		} finally {
			Draw.setOpacity(1f);
		}
	}
}

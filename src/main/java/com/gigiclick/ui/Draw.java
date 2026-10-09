package com.gigiclick.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class Draw {
	private static float opacity = 1f;

	private Draw() {
	}

	public static void setOpacity(float value) {
		opacity = Math.clamp(value, 0f, 1f);
	}

	public static int fade(int argb) {
		if (opacity >= 1f) {
			return argb;
		}
		int alpha = Math.round((argb >>> 24) * opacity);
		return (alpha << 24) | (argb & 0xFFFFFF);
	}

	public static int withAlpha(int argb, float alpha) {
		int a = Math.round((argb >>> 24) * Math.clamp(alpha, 0f, 1f));
		return (a << 24) | (argb & 0xFFFFFF);
	}

	public static int mix(int from, int to, float t) {
		t = Math.clamp(t, 0f, 1f);
		int a = lerp(from >>> 24, to >>> 24, t);
		int r = lerp((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
		int g = lerp((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
		int b = lerp(from & 0xFF, to & 0xFF, t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static int lerp(int a, int b, float t) {
		return Math.round(a + (b - a) * t);
	}

	public static void rect(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
		if (w > 0 && h > 0) {
			g.fill(x, y, x + w, y + h, fade(color));
		}
	}

	public static void roundRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
		radius = Math.min(radius, Math.min(w, h) / 2);
		if (radius <= 0) {
			rect(g, x, y, w, h, color);
			return;
		}
		int c = fade(color);
		for (int i = 0; i < radius; i++) {
			int inset = cornerInset(radius, i);
			g.fill(x + inset, y + i, x + w - inset, y + i + 1, c);
			g.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, c);
		}
		g.fill(x, y + radius, x + w, y + h - radius, c);
	}

	public static void roundPanel(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int fill, int border) {
		roundRect(g, x, y, w, h, radius, border);
		roundRect(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), fill);
	}

	public static void circle(GuiGraphicsExtractor g, int cx, int cy, int radius, int color) {
		roundRect(g, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
	}

	private static int cornerInset(int radius, int row) {
		double dy = radius - row - 0.5;
		return radius - (int) Math.round(Math.sqrt(radius * radius - dy * dy));
	}

	public static void text(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color) {
		g.text(font, text, x, y, fade(color), false);
	}

	public static void text(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int color) {
		g.text(font, text, x, y, fade(color), false);
	}

	public static void centeredText(GuiGraphicsExtractor g, Font font, Component text, int cx, int y, int color) {
		g.text(font, text, cx - font.width(text) / 2, y, fade(color), false);
	}

	public static void rightText(GuiGraphicsExtractor g, Font font, Component text, int right, int y, int color) {
		g.text(font, text, right - font.width(text), y, fade(color), false);
	}

	public static void scaledText(GuiGraphicsExtractor g, Font font, String text, float x, float y, float scale, int color) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.text(font, text, 0, 0, fade(color), false);
		g.pose().popMatrix();
	}

	public static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}
}

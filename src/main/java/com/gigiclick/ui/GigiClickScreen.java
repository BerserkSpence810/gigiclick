package com.gigiclick.ui;

import com.gigiclick.AutoClicker;
import com.gigiclick.Config;
import com.gigiclick.GigiClick;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GigiClickScreen extends Screen {
	private static final int PANEL_W = 320;
	private static final int PANEL_H = 248;
	private static final int PAD = 12;

	private static final Preset[] PRESETS = {
			new Preset("relaxed", 5, 8),
			new Preset("steady", 8, 12),
			new Preset("fast", 12, 16),
			new Preset("turbo", 16, 22),
	};

	private static final String POWER = "power";
	private static final String HUD = "hud";
	private static final String KEY = "key";
	private static final String RESET = "reset";
	private static final String DONE = "done";
	private static final String HANDLE_MIN = "handle_min";
	private static final String HANDLE_MAX = "handle_max";
	private static final String CHIP = "chip";

	private final Screen parent;
	private final Config config = GigiClick.config;

	private int px;
	private int py;
	private float fitScale = 1f;

	private long lastFrameNanos;
	private final Anim openAnim = new Anim(0f, 12f);
	private final Anim enabledAnim;
	private final Anim hudAnim;
	private final Anim minAnim;
	private final Anim maxAnim;
	private final Map<String, Anim> hoverAnims = new HashMap<>();

	private String dragging;

	private final float[] samples = new float[96];
	private int sampleCount;
	private long nextSampleNanos;

	public GigiClickScreen(Screen parent) {
		super(Component.translatable("gigiclick.screen.title"));
		this.parent = parent;
		this.enabledAnim = new Anim(config.enabled ? 1f : 0f, 16f);
		this.hudAnim = new Anim(config.showHud ? 1f : 0f, 16f);
		this.minAnim = new Anim(config.minCps, 22f);
		this.maxAnim = new Anim(config.maxCps, 22f);
		for (int i = 0; i < samples.length; i++) {
			pushSample(1e9f / AutoClicker.randomIntervalNanos());
		}
	}

	@Override
	protected void init() {
		fitScale = Math.min(1f, Math.min((height - 8f) / PANEL_H, (width - 8f) / PANEL_W));
		px = (width - PANEL_W) / 2;
		py = (height - PANEL_H) / 2;
	}

	@Override
	public void onClose() {
		config.save();
		minecraft.gui.setScreen(parent);
	}

	private int cardX() {
		return px + PAD;
	}

	private int cardW() {
		return PANEL_W - PAD * 2;
	}

	private int speedCardY() {
		return py + 48;
	}

	private int previewCardY() {
		return py + 140;
	}

	private int optionsCardY() {
		return py + 186;
	}

	private int footerY() {
		return py + 218;
	}

	private int trackX() {
		return cardX() + 14;
	}

	private int trackW() {
		return cardW() - 28;
	}

	private int trackCenterY() {
		return speedCardY() + 42;
	}

	private int powerSwitchX() {
		return px + PANEL_W - PAD - 24;
	}

	private int powerSwitchY() {
		return py + 15;
	}

	private int hudSwitchX() {
		return cardX() + 10 + font.width(Component.translatable("gigiclick.option.hud")) + 8;
	}

	private int chipW() {
		return (trackW() - 3 * 6) / 4;
	}

	private int chipX(int i) {
		return trackX() + i * (chipW() + 6);
	}

	private int chipY() {
		return speedCardY() + 64;
	}

	private Component keyLabel() {
		return GigiClick.toggleKey().getTranslatedKeyMessage();
	}

	private int keycapW() {
		return Math.max(20, font.width(keyLabel()) + 12);
	}

	private int keycapX() {
		return cardX() + cardW() - 10 - keycapW();
	}

	private int valueToX(float value) {
		float t = (value - Config.MIN_ALLOWED) / (Config.MAX_ALLOWED - Config.MIN_ALLOWED);
		return Math.round(trackX() + t * trackW());
	}

	private int xToValue(double x) {
		double t = (x - trackX()) / trackW();
		int value = (int) Math.round(Config.MIN_ALLOWED + t * (Config.MAX_ALLOWED - Config.MIN_ALLOWED));
		return Math.clamp(value, Config.MIN_ALLOWED, Config.MAX_ALLOWED);
	}

	private double toPanelX(double mouseX) {
		return width / 2.0 + (mouseX - width / 2.0) / fitScale;
	}

	private double toPanelY(double mouseY) {
		return height / 2.0 + (mouseY - height / 2.0) / fitScale;
	}

	private String elementAt(double mx, double my) {
		if (Draw.inside(mx, my, powerSwitchX() - 44, powerSwitchY() - 3, 68, 18)) {
			return POWER;
		}
		int optionsY = optionsCardY();
		if (Draw.inside(mx, my, cardX() + 6, optionsY + 3, hudSwitchX() + 24 - cardX() - 2, 18)) {
			return HUD;
		}
		if (Draw.inside(mx, my, keycapX(), optionsY + 4, keycapW(), 16)) {
			return KEY;
		}
		if (Draw.inside(mx, my, cardX(), footerY(), 56, 18)) {
			return RESET;
		}
		if (Draw.inside(mx, my, px + PANEL_W - PAD - 72, footerY(), 72, 18)) {
			return DONE;
		}
		for (int i = 0; i < PRESETS.length; i++) {
			if (Draw.inside(mx, my, chipX(i), chipY(), chipW(), 16)) {
				return CHIP + i;
			}
		}
		if (Draw.inside(mx, my, trackX() - 8, trackCenterY() - 8, trackW() + 16, 16)) {
			return nearestHandle(mx);
		}
		return null;
	}

	private String nearestHandle(double mx) {
		int value = xToValue(mx);
		if (value < config.minCps) {
			return HANDLE_MIN;
		}
		if (value > config.maxCps) {
			return HANDLE_MAX;
		}
		double toMin = Math.abs(mx - valueToX(config.minCps));
		double toMax = Math.abs(mx - valueToX(config.maxCps));
		return toMin <= toMax ? HANDLE_MIN : HANDLE_MAX;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(g, mouseX, mouseY, partialTick);
		g.fill(0, 0, width, height, Draw.withAlpha(Theme.BACKDROP, openAnim.get()));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		long now = System.nanoTime();
		float dt = lastFrameNanos == 0 ? 0f : Math.min(0.1f, (now - lastFrameNanos) / 1e9f);
		lastFrameNanos = now;

		float open = openAnim.update(1f, dt);
		enabledAnim.update(config.enabled ? 1f : 0f, dt);
		hudAnim.update(config.showHud ? 1f : 0f, dt);
		if (HANDLE_MIN.equals(dragging) || HANDLE_MAX.equals(dragging)) {
			minAnim.set(config.minCps);
			maxAnim.set(config.maxCps);
		} else {
			minAnim.update(config.minCps, dt);
			maxAnim.update(config.maxCps, dt);
		}
		stepPreview(now);

		String hovered = dragging != null ? dragging : elementAt(toPanelX(mouseX), toPanelY(mouseY));
		for (String id : new String[] {POWER, HUD, KEY, RESET, DONE, HANDLE_MIN, HANDLE_MAX, CHIP + 0, CHIP + 1, CHIP + 2, CHIP + 3}) {
			hover(id).update(id.equals(hovered) ? 1f : 0f, dt);
		}
		if (hovered != null) {
			g.requestCursor(hovered.startsWith("handle") ? CursorTypes.RESIZE_EW : CursorTypes.POINTING_HAND);
		}

		float eased = 1f - (1f - open) * (1f - open) * (1f - open);
		float scale = (0.94f + 0.06f * eased) * fitScale;
		g.pose().pushMatrix();
		g.pose().translate(width / 2f, height / 2f);
		g.pose().scale(scale, scale);
		g.pose().translate(-width / 2f, -height / 2f);
		Draw.setOpacity(open);
		try {
			drawPanel(g);
			drawHeader(g);
			drawSpeedCard(g);
			drawPreviewCard(g);
			drawOptionsCard(g);
			drawFooter(g);
		} finally {
			Draw.setOpacity(1f);
			g.pose().popMatrix();
		}

		if (hovered != null && hovered.startsWith(CHIP)) {
			Preset preset = PRESETS[hovered.charAt(CHIP.length()) - '0'];
			g.setTooltipForNextFrame(font, Component.translatable("gigiclick.preset.tooltip", preset.min, preset.max), mouseX, mouseY);
		} else if (KEY.equals(hovered)) {
			g.setTooltipForNextFrame(font, Component.translatable("gigiclick.option.key.tooltip"), mouseX, mouseY);
		}
	}

	private void drawPanel(GuiGraphicsExtractor g) {
		for (int i = 4; i >= 1; i--) {
			Draw.roundRect(g, px - i, py - i + 3, PANEL_W + i * 2, PANEL_H + i * 2, 6 + i, 0x14000000);
		}
		Draw.roundPanel(g, px, py, PANEL_W, PANEL_H, 7, Theme.PANEL, Theme.PANEL_BORDER);
		g.fillGradient(px + 3, py + 1, px + PANEL_W - 3, py + 44, Draw.fade(0x1F8B5CF6), Draw.fade(0x008B5CF6));
	}

	private void drawHeader(GuiGraphicsExtractor g) {
		int badgeX = px + PAD;
		int badgeY = py + 11;
		Draw.roundRect(g, badgeX, badgeY, 20, 20, 5, Theme.ACCENT_DEEP);
		Draw.roundRect(g, badgeX, badgeY, 20, 19, 5, Theme.ACCENT);
		Component mark = Component.literal("G").withStyle(ChatFormatting.BOLD);
		Draw.text(g, font, mark, badgeX + 10 - font.width(mark) / 2 + 1, badgeY + 6, Theme.TEXT);

		int titleX = badgeX + 28;
		Draw.text(g, font, Component.translatable("gigiclick.name").withStyle(ChatFormatting.BOLD), titleX, py + 12, Theme.TEXT);
		Draw.text(g, font, Component.translatable("gigiclick.screen.subtitle"), titleX, py + 23, Theme.TEXT_MUTED);

		int sx = powerSwitchX();
		int sy = powerSwitchY();
		Component status = Component.translatable(config.enabled ? "gigiclick.status.on" : "gigiclick.status.off");
		Draw.rightText(g, font, status, sx - 7, sy + 2, config.enabled ? Theme.SUCCESS : Theme.TEXT_MUTED);
		drawSwitch(g, sx, sy, enabledAnim.get(), hover(POWER).get());

		Draw.rect(g, px + PAD, py + 40, PANEL_W - PAD * 2, 1, Theme.DIVIDER);
	}

	private void drawSpeedCard(GuiGraphicsExtractor g) {
		int x = cardX();
		int y = speedCardY();
		int w = cardW();
		Draw.roundPanel(g, x, y, w, 86, 5, Theme.CARD, Theme.CARD_BORDER);

		Draw.text(g, font, Component.translatable("gigiclick.speed.title").withStyle(ChatFormatting.BOLD), x + 10, y + 9, Theme.TEXT);
		Draw.text(g, font, Component.translatable("gigiclick.speed.hint"), x + 10, y + 20, Theme.TEXT_MUTED);

		int shownMin = Math.round(minAnim.get());
		int shownMax = Math.round(maxAnim.get());
		String range = shownMin == shownMax ? String.valueOf(shownMin) : shownMin + "–" + shownMax;
		Component unit = Component.translatable("gigiclick.unit.cps");
		int right = x + w - 10;
		int unitX = right - font.width(unit);
		Draw.text(g, font, unit, unitX, y + 16, Theme.TEXT_MUTED);
		Draw.scaledText(g, font, range, unitX - 4 - font.width(range) * 2, y + 8, 2f, Theme.TEXT);

		int tx = trackX();
		int tw = trackW();
		int cy = trackCenterY();
		Draw.roundRect(g, tx, cy - 2, tw, 4, 2, Theme.TRACK);
		int minX = Math.round(trackX() + (minAnim.get() - Config.MIN_ALLOWED) / (Config.MAX_ALLOWED - Config.MIN_ALLOWED) * tw);
		int maxX = Math.round(trackX() + (maxAnim.get() - Config.MIN_ALLOWED) / (Config.MAX_ALLOWED - Config.MIN_ALLOWED) * tw);
		Draw.roundRect(g, minX, cy - 2, Math.max(4, maxX - minX), 4, 2, Theme.ACCENT);

		for (int tick : new int[] {1, 10, 20, 30, 40}) {
			Component label = Component.literal(String.valueOf(tick));
			Draw.centeredText(g, font, label, valueToX(tick), cy + 8, Theme.TEXT_MUTED);
		}

		drawHandle(g, minX, cy, hover(HANDLE_MIN).get(), config.minCps, HANDLE_MIN.equals(dragging));
		drawHandle(g, maxX, cy, hover(HANDLE_MAX).get(), config.maxCps, HANDLE_MAX.equals(dragging));

		for (int i = 0; i < PRESETS.length; i++) {
			Preset preset = PRESETS[i];
			boolean active = preset.min == config.minCps && preset.max == config.maxCps;
			float h = hover(CHIP + i).get();
			int cx = chipX(i);
			int fill = active ? Draw.mix(Theme.CARD, Theme.ACCENT, 0.2f) : Draw.mix(Theme.CARD, Theme.CARD_HOVER, h);
			int border = active ? Theme.ACCENT : Draw.mix(Theme.CARD_BORDER, Theme.TEXT_MUTED, h * 0.7f);
			Draw.roundPanel(g, cx, chipY(), chipW(), 16, 4, fill, border);
			int textColor = active ? Theme.TEXT : Draw.mix(Theme.TEXT_SECONDARY, Theme.TEXT, h);
			Draw.centeredText(g, font, preset.label(), cx + chipW() / 2, chipY() + 4, textColor);
		}
	}

	private void drawHandle(GuiGraphicsExtractor g, int x, int cy, float hover, int value, boolean dragging) {
		float emphasis = Math.max(hover, dragging ? 1f : 0f);
		if (emphasis > 0.01f) {
			Draw.circle(g, x, cy, 6 + Math.round(3 * emphasis), Draw.withAlpha(0x408B5CF6, emphasis));
		}
		Draw.circle(g, x, cy, 6, Theme.ACCENT);
		Draw.circle(g, x, cy, 4, Theme.TEXT);

		if (emphasis > 0.05f) {
			String text = String.valueOf(value);
			int bw = font.width(text) + 8;
			int bx = x - bw / 2;
			int by = cy - 22 + Math.round(3 * (1f - emphasis));
			int color = Draw.withAlpha(Theme.ACCENT, emphasis);
			Draw.roundRect(g, bx, by, bw, 12, 3, color);
			Draw.rect(g, x - 1, by + 12, 3, 1, color);
			Draw.rect(g, x, by + 13, 1, 1, color);
			g.text(font, text, bx + 4, by + 2, Draw.fade(Draw.withAlpha(Theme.TEXT, emphasis)), false);
		}
	}

	private void drawPreviewCard(GuiGraphicsExtractor g) {
		int x = cardX();
		int y = previewCardY();
		int w = cardW();
		Draw.roundPanel(g, x, y, w, 40, 5, Theme.CARD, Theme.CARD_BORDER);

		Draw.text(g, font, Component.translatable("gigiclick.preview.title").withStyle(ChatFormatting.BOLD), x + 10, y + 7, Theme.TEXT);
		float average = 0f;
		for (int i = 0; i < sampleCount; i++) {
			average += samples[i];
		}
		average = sampleCount == 0 ? 0f : average / sampleCount;
		Component info = config.enabled
				? Component.translatable("gigiclick.preview.average", String.format(Locale.ROOT, "%.1f", average))
				: Component.translatable("gigiclick.preview.paused");
		Draw.rightText(g, font, info, x + w - 10, y + 7, Theme.TEXT_SECONDARY);

		int gx = x + 10;
		int gw = w - 20;
		int baseY = y + 33;
		int graphH = 13;
		int barW = 3;
		int gap = 2;
		int bars = Math.min(sampleCount, (gw + gap) / (barW + gap));
		float low = config.minCps;
		float span = Math.max(0.0001f, config.maxCps - config.minCps);
		for (int i = 0; i < bars; i++) {
			float sample = samples[sampleCount - 1 - i];
			int barH = config.maxCps == config.minCps
					? graphH - 3
					: 4 + Math.round(Math.clamp((sample - low) / span, 0f, 1f) * (graphH - 4));
			int bx = gx + gw - barW - i * (barW + gap);
			float age = (float) i / Math.max(1, bars - 1);
			int color = config.enabled
					? Draw.withAlpha(i == 0 ? Theme.ACCENT_HOVER : Theme.ACCENT, 1f - age * 0.75f)
					: Theme.TRACK;
			Draw.roundRect(g, bx, baseY - barH, barW, barH, 1, color);
		}
	}

	private void drawOptionsCard(GuiGraphicsExtractor g) {
		int x = cardX();
		int y = optionsCardY();
		int w = cardW();
		Draw.roundPanel(g, x, y, w, 24, 5, Theme.CARD, Theme.CARD_BORDER);

		float hudHover = hover(HUD).get();
		Draw.text(g, font, Component.translatable("gigiclick.option.hud"), x + 10, y + 8, Draw.mix(Theme.TEXT_SECONDARY, Theme.TEXT, hudHover));
		drawSwitch(g, hudSwitchX(), y + 6, hudAnim.get(), hudHover);

		float keyHover = hover(KEY).get();
		int kx = keycapX();
		int kw = keycapW();
		Draw.rightText(g, font, Component.translatable("gigiclick.option.key"), kx - 7, y + 8, Theme.TEXT_SECONDARY);
		Draw.roundRect(g, kx, y + 5, kw, 15, 3, Theme.PANEL_BORDER);
		Draw.roundPanel(g, kx, y + 4, kw, 15, 3, Draw.mix(Theme.PANEL, Theme.CARD_HOVER, keyHover),
				Draw.mix(Theme.PANEL_BORDER, Theme.ACCENT, keyHover));
		Draw.centeredText(g, font, keyLabel(), kx + kw / 2, y + 8, Draw.mix(Theme.TEXT_SECONDARY, Theme.TEXT, keyHover));
	}

	private void drawFooter(GuiGraphicsExtractor g) {
		int y = footerY();
		float resetHover = hover(RESET).get();
		Draw.roundPanel(g, cardX(), y, 56, 18, 4, Draw.mix(Theme.CARD, Theme.CARD_HOVER, resetHover),
				Draw.mix(Theme.CARD_BORDER, Theme.TEXT_MUTED, resetHover * 0.7f));
		Draw.centeredText(g, font, Component.translatable("gigiclick.button.reset"), cardX() + 28, y + 5,
				Draw.mix(Theme.TEXT_SECONDARY, Theme.TEXT, resetHover));

		Draw.centeredText(g, font, Component.translatable("gigiclick.screen.disclaimer"), px + PANEL_W / 2, y + 5, Theme.WARNING);

		float doneHover = hover(DONE).get();
		int doneX = px + PANEL_W - PAD - 72;
		Draw.roundRect(g, doneX, y + 1, 72, 18, 4, Theme.ACCENT_DEEP);
		Draw.roundRect(g, doneX, y, 72, 18, 4, Draw.mix(Theme.ACCENT, Theme.ACCENT_HOVER, doneHover));
		Draw.centeredText(g, font, Component.translatable("gigiclick.button.done").withStyle(ChatFormatting.BOLD), doneX + 36, y + 5, Theme.TEXT);
	}

	private void drawSwitch(GuiGraphicsExtractor g, int x, int y, float on, float hover) {
		int track = Draw.mix(Theme.OFF, Theme.ACCENT, on);
		track = Draw.mix(track, 0xFFFFFFFF, hover * 0.1f);
		Draw.roundRect(g, x, y, 24, 12, 6, track);
		int knobX = x + 6 + Math.round(on * 12);
		Draw.circle(g, knobX, y + 6, 4, Theme.TEXT);
	}

	private Anim hover(String id) {
		return hoverAnims.computeIfAbsent(id, k -> new Anim(0f, 18f));
	}

	private void stepPreview(long now) {
		if (nextSampleNanos == 0 || now - nextSampleNanos > 1_000_000_000L) {
			nextSampleNanos = now;
		}
		while (now >= nextSampleNanos) {
			long interval = AutoClicker.randomIntervalNanos();
			pushSample(1e9f / interval);
			nextSampleNanos += interval;
		}
	}

	private void pushSample(float cps) {
		if (sampleCount == samples.length) {
			System.arraycopy(samples, 1, samples, 0, samples.length - 1);
			sampleCount--;
		}
		samples[sampleCount++] = cps;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0) {
			return super.mouseClicked(event, doubleClick);
		}
		String target = elementAt(toPanelX(event.x()), toPanelY(event.y()));
		if (target == null) {
			return super.mouseClicked(event, doubleClick);
		}

		switch (target) {
			case POWER -> {
				config.enabled = !config.enabled;
				config.save();
			}
			case HUD -> {
				config.showHud = !config.showHud;
				config.save();
			}
			case KEY -> minecraft.gui.setScreen(new KeyBindsScreen(this, minecraft.options));
			case RESET -> {
				config.resetToDefaults();
				config.save();
			}
			case DONE -> onClose();
			case HANDLE_MIN, HANDLE_MAX -> {
				dragging = target;
				dragTo(toPanelX(event.x()));
				return true;
			}
			default -> {
				Preset preset = PRESETS[target.charAt(CHIP.length()) - '0'];
				config.setRange(preset.min, preset.max);
				config.save();
			}
		}
		playClickSound();
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging != null) {
			dragTo(toPanelX(event.x()));
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			dragging = null;
			config.save();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		double mx = toPanelX(mouseX);
		double my = toPanelY(mouseY);
		if (scrollY != 0 && Draw.inside(mx, my, cardX(), speedCardY(), cardW(), 60)) {
			int step = scrollY > 0 ? 1 : -1;
			if (HANDLE_MIN.equals(nearestHandle(mx))) {
				config.setRange(Math.min(config.minCps + step, config.maxCps), config.maxCps);
			} else {
				config.setRange(config.minCps, Math.max(config.maxCps + step, config.minCps));
			}
			config.save();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private void dragTo(double mouseX) {
		int value = xToValue(mouseX);
		if (HANDLE_MIN.equals(dragging)) {
			config.setRange(Math.min(value, config.maxCps), config.maxCps);
		} else {
			config.setRange(config.minCps, Math.max(value, config.minCps));
		}
	}

	private void playClickSound() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
	}

	private record Preset(String id, int min, int max) {
		Component label() {
			return Component.translatable("gigiclick.preset." + id);
		}
	}
}

package com.gigiclick;

import com.gigiclick.ui.GigiClickScreen;
import com.gigiclick.ui.HudIndicator;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class GigiClick implements ClientModInitializer {
	public static final String MOD_ID = "gigiclick";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Config config;

	private static KeyMapping toggleKey;
	private static KeyMapping menuKey;

	@Override
	public void onInitializeClient() {
		config = Config.load();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		toggleKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.gigiclick.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, category));
		menuKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.gigiclick.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				config.enabled = !config.enabled;
				config.save();
				if (client.player != null) {
					client.player.sendOverlayMessage(statusText());
				}
			}
			while (menuKey.consumeClick()) {
				client.gui.setScreen(new GigiClickScreen(null));
			}
		});

		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
				Identifier.fromNamespaceAndPath(MOD_ID, "indicator"), HudIndicator::extract);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(
				literal("gigiclick")
						.executes(ctx -> {
							Minecraft client = ctx.getSource().getClient();
							client.schedule(() -> client.gui.setScreen(new GigiClickScreen(null)));
							return 1;
						})
						.then(literal("status").executes(GigiClick::showStatus))
						.then(literal("toggle").executes(ctx -> {
							config.enabled = !config.enabled;
							config.save();
							return showStatus(ctx);
						}))
						.then(literal("cps")
								.then(argument("min", cpsArg())
										.then(argument("max", cpsArg()).executes(ctx -> {
											config.setRange(IntegerArgumentType.getInteger(ctx, "min"),
													IntegerArgumentType.getInteger(ctx, "max"));
											config.save();
											return showStatus(ctx);
										}))))
						.then(literal("min").then(argument("value", cpsArg()).executes(ctx -> {
							config.setRange(IntegerArgumentType.getInteger(ctx, "value"), config.maxCps);
							config.save();
							return showStatus(ctx);
						})))
						.then(literal("max").then(argument("value", cpsArg()).executes(ctx -> {
							config.setRange(config.minCps, IntegerArgumentType.getInteger(ctx, "value"));
							config.save();
							return showStatus(ctx);
						})))));

		LOGGER.info("GigiClick loaded ({}-{} CPS, {})", config.minCps, config.maxCps, config.enabled ? "on" : "off");
	}

	public static KeyMapping toggleKey() {
		return toggleKey;
	}

	public static String version() {
		return FabricLoader.getInstance().getModContainer(MOD_ID)
				.map(mod -> mod.getMetadata().getVersion().getFriendlyString())
				.orElse("dev");
	}

	private static IntegerArgumentType cpsArg() {
		return IntegerArgumentType.integer(Config.MIN_ALLOWED, Config.MAX_ALLOWED);
	}

	private static int showStatus(CommandContext<FabricClientCommandSource> ctx) {
		ctx.getSource().sendFeedback(statusText());
		return 1;
	}

	private static Component statusText() {
		Component state = config.enabled
				? Component.translatable("gigiclick.status.on").withStyle(ChatFormatting.GREEN)
				: Component.translatable("gigiclick.status.off").withStyle(ChatFormatting.RED);
		return Component.translatable("gigiclick.name").append(" ").append(state)
				.append(Component.translatable("gigiclick.message.range", config.minCps, config.maxCps)
						.withStyle(ChatFormatting.GRAY));
	}
}

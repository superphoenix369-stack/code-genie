package dev.codegenie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class CodeGenie implements ModInitializer {
	public static final String MOD_ID = "codegenie";
	public static final Logger LOGGER = LoggerFactory.getLogger("Code Genie");

	private static GenieConfig config = new GenieConfig();

	public static GenieConfig config() {
		return config;
	}

	@Override
	public void onInitialize() {
		config = GenieConfig.load();

		PayloadTypeRegistry.clientboundPlay().register(DkModePayload.TYPE, DkModePayload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> CheatEngine.onJoin(handler.player));

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> GenieCommand.register(dispatcher));
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(CheatEngine::allowDamage);
		ServerLivingEntityEvents.ALLOW_DEATH.register(CheatEngine::allowDeath);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(CheatEngine::afterDamage);
		ServerLivingEntityEvents.AFTER_DEATH.register(CheatEngine::afterDeath);
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> CheatEngine.copyFrom(oldPlayer, newPlayer));
		ServerTickEvents.END_SERVER_TICK.register(CheatEngine::tick);

		LOGGER.info("Code Genie plugged in. Try /genie SXIOPO");
	}
}

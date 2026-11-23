package dev.codegenie;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: switch DK Mode (big heads) on or off for this player's view. */
public record DkModePayload(boolean on) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<DkModePayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(CodeGenie.MOD_ID, "dk_mode"));
	public static final StreamCodec<ByteBuf, DkModePayload> CODEC = ByteBufCodecs.BOOL.map(DkModePayload::new, DkModePayload::on);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

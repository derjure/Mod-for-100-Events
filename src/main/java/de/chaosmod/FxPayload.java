package de.chaosmod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FxPayload(int fx, int durationMs, int param) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FxPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ChaosMod.ID, "fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FxPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FxPayload::fx,
            ByteBufCodecs.VAR_INT, FxPayload::durationMs,
            ByteBufCodecs.VAR_INT, FxPayload::param,
            FxPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}

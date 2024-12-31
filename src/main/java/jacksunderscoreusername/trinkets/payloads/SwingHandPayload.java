package jacksunderscoreusername.trinkets.payloads;

import jacksunderscoreusername.trinkets.Main;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SwingHandPayload(boolean isMainHand) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(Main.MOD_ID,"swing_hand");
    public static final CustomPayload.Id<SwingHandPayload> ID = new CustomPayload.Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, SwingHandPayload> CODEC = PacketCodec.tuple(PacketCodecs.BOOLEAN, SwingHandPayload::isMainHand, SwingHandPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
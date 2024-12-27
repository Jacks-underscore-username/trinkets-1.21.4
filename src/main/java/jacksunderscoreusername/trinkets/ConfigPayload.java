package jacksunderscoreusername.trinkets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ConfigPayload(String configJson) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(Main.MOD_ID,"config");
    public static final Id<ConfigPayload> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, ConfigPayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING, ConfigPayload::configJson, ConfigPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
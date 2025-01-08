package jacksunderscoreusername.trinkets.trinkets.breeze_core;

import jacksunderscoreusername.trinkets.Main;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

public record UseBreezeCorePayload(Vector3f vec) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(Main.MOD_ID,"use_breeze_core");
    public static final CustomPayload.Id<UseBreezeCorePayload> ID = new CustomPayload.Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, UseBreezeCorePayload> CODEC = PacketCodec.tuple(PacketCodecs.VECTOR_3F, UseBreezeCorePayload::vec, UseBreezeCorePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
package jacksunderscoreusername.ancient_trinkets.payloads;

import jacksunderscoreusername.ancient_trinkets.Main;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DialogClickedPayload(int index) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(Main.MOD_ID,"dialog_clicked");
    public static final Id<DialogClickedPayload> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, DialogClickedPayload> CODEC = PacketCodec.tuple(PacketCodecs.INTEGER, DialogClickedPayload::index, DialogClickedPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
package jacksunderscoreusername.trinkets.payloads;

import jacksunderscoreusername.trinkets.Main;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SendDialogPagePayload(String pageJson) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(Main.MOD_ID,"dialog_page");
    public static final Id<SendDialogPagePayload> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, SendDialogPagePayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING, SendDialogPagePayload::pageJson, SendDialogPagePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
package jacksunderscoreusername.trinkets.payloads;

import jacksunderscoreusername.trinkets.Main;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CloseDialogPagePayload(boolean bool) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(Main.MOD_ID,"close_dialog_page");
    public static final Id<CloseDialogPagePayload> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, CloseDialogPagePayload> CODEC = PacketCodec.tuple(PacketCodecs.BOOLEAN, CloseDialogPagePayload::bool, CloseDialogPagePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
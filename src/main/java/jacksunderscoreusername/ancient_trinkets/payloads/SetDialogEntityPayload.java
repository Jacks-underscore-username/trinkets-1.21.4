package jacksunderscoreusername.ancient_trinkets.payloads;

import jacksunderscoreusername.ancient_trinkets.Main;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SetDialogEntityPayload(int id) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(Main.MOD_ID,"set_dialog_entity");
    public static final Id<SetDialogEntityPayload> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, SetDialogEntityPayload> CODEC = PacketCodec.tuple(PacketCodecs.INTEGER, SetDialogEntityPayload::id, SetDialogEntityPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
package jacksunderscoreusername.trinkets.trinkets.soul_lamp;

import jacksunderscoreusername.trinkets.Main;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RenderGhostsPayload(NbtElement tag) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(Main.MOD_ID,"render_ghosts");
    public static final Id<RenderGhostsPayload> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, RenderGhostsPayload> CODEC = PacketCodec.tuple(PacketCodecs.NBT_ELEMENT, RenderGhostsPayload::tag, RenderGhostsPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
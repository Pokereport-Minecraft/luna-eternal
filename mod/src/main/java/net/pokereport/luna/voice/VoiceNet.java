package net.pokereport.luna.voice;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.UUID;

public final class VoiceNet {
    public enum Action { REFRESH, INVITE, ACCEPT, REJECT, CANCEL, REVOKE, BLOCK, UNBLOCK, REQUESTS, KNOWN_AUDIO }
    public static final UUID NONE=new UUID(0,0);
    public record Request(Action action,UUID target,UUID invitation,boolean value,int page) implements CustomPayload {
        public static final Id<Request> ID=new Id<>(Identifier.of("lunaeternal","pokevoice_request_v1"));
        public static final PacketCodec<RegistryByteBuf,Request> CODEC=new PacketCodec<>() {
            public Request decode(RegistryByteBuf b){
                int a=b.readUnsignedByte();if(a>=Action.values().length)throw new IllegalArgumentException("Voice action");
                UUID target=b.readUuid(),nonce=b.readUuid();boolean value=b.readBoolean();int page=b.readUnsignedShort();
                if(page>1000||b.readableBytes()!=0)throw new IllegalArgumentException("Voice packet size/page");
                return new Request(Action.values()[a],target,nonce,value,page);
            }
            public void encode(RegistryByteBuf b,Request p){b.writeByte(p.action.ordinal());b.writeUuid(p.target);b.writeUuid(p.invitation);b.writeBoolean(p.value);b.writeShort(p.page);}
        };
        public Id<? extends CustomPayload> getId(){return ID;}
    }
    public record Snapshot(String json) implements CustomPayload {
        public static final Id<Snapshot> ID=new Id<>(Identifier.of("lunaeternal","pokevoice_snapshot_v1"));
        public static final PacketCodec<RegistryByteBuf,Snapshot> CODEC=new PacketCodec<>() {
            public Snapshot decode(RegistryByteBuf b){return new Snapshot(b.readString(32768));}
            public void encode(RegistryByteBuf b,Snapshot p){b.writeString(p.json,32768);}
        };
        public Id<? extends CustomPayload> getId(){return ID;}
    }
    public static void register(){PayloadTypeRegistry.playC2S().register(Request.ID,Request.CODEC);PayloadTypeRegistry.playS2C().register(Snapshot.ID,Snapshot.CODEC);}
    public record Peer(UUID id,String name,double distance,String state,UUID invitation,long remainingMillis,boolean ownBlock) {}
    public record View(boolean enabled,boolean healthy,boolean receiveRequests,boolean allowKnown,boolean hud,int page,boolean more,String message,java.util.List<Peer> peers) {}
}

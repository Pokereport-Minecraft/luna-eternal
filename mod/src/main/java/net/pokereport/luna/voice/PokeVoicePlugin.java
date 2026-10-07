package net.pokereport.luna.voice;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.*;

/** Per-recipient server gate; group/plugin/spectator channels retain native semantics. */
public final class PokeVoicePlugin implements VoicechatPlugin {
    public static volatile de.maxhenkel.voicechat.api.VoicechatClientApi clientApi;
    public String getPluginId(){return "pokereport_pokevoice";}
    public void registerEvents(EventRegistration r){
        r.registerEvent(ClientVoicechatInitializationEvent.class,e->clientApi=e.getVoicechat());
        r.registerEvent(MicrophonePacketEvent.class,e->{
            var sender=e.getSenderConnection();var model=VoiceRuntime.relations();
            if(sender==null||!sender.isConnected()||model==null||!model.authenticatedOnline(sender.getPlayer().getUuid()))e.cancel();
        },100);
        r.registerEvent(EntitySoundPacketEvent.class,this::filter,100);
        r.registerEvent(LocationalSoundPacketEvent.class,this::filter,100);
        r.registerEvent(StaticSoundPacketEvent.class,this::filter,100);
    }
    private void filter(SoundPacketEvent<?> e){
        if(!SoundPacketEvent.SOURCE_PROXIMITY.equals(e.getSource()))return;
        var a=e.getSenderConnection();var b=e.getReceiverConnection();var model=VoiceRuntime.relations();
        if(a==null||b==null||!a.isConnected()||!b.isConnected()||model==null
            ||a.getPlayer().getServerLevel().getServerLevel()!=b.getPlayer().getServerLevel().getServerLevel()
            ||!model.allowsProximity(a.getPlayer().getUuid(),b.getPlayer().getUuid()))e.cancel();
    }
}

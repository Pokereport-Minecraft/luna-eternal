package net.pokereport.luna.client.voice;

import com.google.gson.Gson;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.pokereport.luna.voice.*;
import java.util.*;

public final class VoiceClient implements ClientModInitializer {
    private static final Gson GSON=new Gson();
    public static VoiceNet.View view;
    public static long receivedAt;
    public static boolean hud=true;
    private static UUID displayedInvitation=VoiceNet.NONE;
    private static int ticks;
    public void onInitializeClient(){
        try{var p=new java.util.Properties();var f=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("pokevoice-client.properties");if(java.nio.file.Files.exists(f))try(var in=java.nio.file.Files.newInputStream(f)){p.load(in);hud=Boolean.parseBoolean(p.getProperty("hud","true"));}}catch(java.io.IOException e){hud=true;}
        ClientPlayNetworking.registerGlobalReceiver(VoiceNet.Snapshot.ID,(p,ctx)->ctx.client().execute(()->{
            var next=GSON.fromJson(p.json(),VoiceNet.View.class);
            if(next==null||next.peers()==null||next.peers().size()>50)return;
            view=next;receivedAt=System.nanoTime();
            if(ctx.client().currentScreen instanceof PokeVoiceScreen screen)screen.update();
            for(var peer:next.peers())if(peer.state().equals("PENDING_INCOMING")&&!peer.invitation().equals(displayedInvitation)){
                // A battle/PokePad screen is never replaced by an incoming dialog.
                if(ctx.client().currentScreen==null&&com.cobblemon.mod.common.client.CobblemonClient.INSTANCE.getBattle()==null){displayedInvitation=peer.invitation();ctx.client().setScreen(new VoiceInvitationScreen(peer));}
                break;
            }
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{view=null;displayedInvitation=VoiceNet.NONE;ticks=0;});
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(client.player!=null&&++ticks%40==0&&ClientPlayNetworking.canSend(VoiceNet.Request.ID))action(VoiceNet.Action.REFRESH,VoiceNet.NONE,VoiceNet.NONE,false,view==null?0:view.page());
        });
        HudRenderCallback.EVENT.register((ctx,counter)->{
            var c=MinecraftClient.getInstance();if(!hud||c.currentScreen!=null||com.cobblemon.mod.common.client.CobblemonClient.INSTANCE.getBattle()!=null||view==null||!view.hud()||c.options.hudHidden||System.nanoTime()-receivedAt>5_000_000_000L)return;
            var p=view.peers().stream().filter(e->e.distance()>=0&&e.distance()<=12).findFirst().orElse(null);if(p==null)return;
            int x=c.getWindow().getScaledWidth()/2+100,y=c.getWindow().getScaledHeight()-55;
            String label=(p.state().equals("KNOWN")?"Voz · ":"Voz bloqueada · ")+p.name();
            if(p.state().equals("PENDING_INCOMING"))label="Solicitud de voz · abre PokeVoice";
            String shortLabel=c.textRenderer.trimToWidth(label,Math.max(40,c.getWindow().getScaledWidth()-x-8));
            ctx.fill(x-3,y-3,x+c.textRenderer.getWidth(shortLabel)+3,y+12,0xAD111827);ctx.drawText(c.textRenderer,shortLabel,x,y,0xFFFFCA68,false);
        });
    }
    public static void toggleHud(){
        hud=!hud;
        try {var p=new java.util.Properties();p.setProperty("hud",Boolean.toString(hud));var f=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("pokevoice-client.properties");try(var out=java.nio.file.Files.newOutputStream(f)){p.store(out,"PokeVoice HUD");}}catch(java.io.IOException e){org.slf4j.LoggerFactory.getLogger("PokeVoice").warn("HUD preference could not be saved");}
    }
    public static void action(VoiceNet.Action action,UUID target,UUID nonce,boolean value,int page){
        if(ClientPlayNetworking.canSend(VoiceNet.Request.ID))ClientPlayNetworking.send(new VoiceNet.Request(action,target,nonce,value,page));
    }
    /** Invoke the registered native key action, honoring remapping; no private SVC classes/reflection. */
    public static void nativeMenu(String translation){
        var c=MinecraftClient.getInstance();
        for(KeyBinding key:c.options.allKeys)if(key.getTranslationKey().equals(translation)){
            if(key.isUnbound()){if(c.player!=null)c.player.sendMessage(Text.literal("Asigna una tecla a "+Text.translatable(key.getTranslationKey()).getString()+" en Controles."),false);return;}
            c.setScreen(null);KeyBinding.onKeyPressed(InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey()));return;
        }
        if(c.player!=null)c.player.sendMessage(Text.literal("Simple Voice Chat no esta disponible."),false);
    }
}

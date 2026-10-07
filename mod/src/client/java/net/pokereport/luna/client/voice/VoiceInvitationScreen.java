package net.pokereport.luna.client.voice;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.text.Text;
import net.pokereport.luna.voice.*;

public final class VoiceInvitationScreen extends Screen {
    private final VoiceNet.Peer peer;
    private final long expires;
    public VoiceInvitationScreen(VoiceNet.Peer peer){super(Text.literal("Solicitud de voz"));this.peer=peer;expires=System.nanoTime()+peer.remainingMillis()*1_000_000L;}
    public boolean shouldPause(){return false;}
    @Override public void renderBackground(DrawContext c,int mx,int my,float delta) {}
    protected void init(){int x=width/2,y=height/2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Aceptar"),b->respond(VoiceNet.Action.ACCEPT)).dimensions(x-105,y+43,100,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Rechazar"),b->respond(VoiceNet.Action.REJECT)).dimensions(x+5,y+43,100,20).build());
    }
    private void respond(VoiceNet.Action action){VoiceClient.action(action,peer.id(),peer.invitation(),false,0);close();}
    public void tick(){if(System.nanoTime()>expires||VoiceClient.view==null||VoiceClient.view.peers().stream().noneMatch(p->p.invitation().equals(peer.invitation())))close();}
    public void render(DrawContext c,int mx,int my,float delta){int x=width/2,y=height/2;
        c.fill(x-150,y-85,x+150,y+78,0xED111827);c.fill(x-150,y-85,x+150,y-82,0xFFFFCA68);
        c.drawCenteredTextWithShadow(textRenderer,"SOLICITUD DE VOZ",x,y-68,0xFFFFCA68);
        var e=client.getNetworkHandler().getPlayerListEntry(peer.id());if(e!=null)PlayerSkinDrawer.draw(c,e.getSkinTextures(),x-16,y-49,32);
        c.drawCenteredTextWithShadow(textRenderer,textRenderer.trimToWidth(peer.name(),275),x,y-7,0xFFFFFFFF);
        c.drawCenteredTextWithShadow(textRenderer,"quiere hablar contigo cuando esten cerca.",x,y+10,0xFFC5CCDC);
        c.drawCenteredTextWithShadow(textRenderer,"Caduca en "+Math.max(0,(expires-System.nanoTime())/1_000_000_000L)+" s",x,y+26,0xFFFFCA68);
        super.render(c,mx,my,delta);
    }
}

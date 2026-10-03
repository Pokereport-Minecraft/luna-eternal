package net.pokereport.luna.client.voice;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pokereport.luna.voice.*;
import java.util.*;

/** Existing PokePad chassis, typography and palette; no second root device/menu. */
public final class PokeVoiceScreen extends Screen {
    private static final Identifier CHASSIS=Identifier.of("lunaeternal","textures/gui/pokepad/pokepad_cosmeticos.png");
    private final Screen parent;
    private int tab,page;
    private float scale;private int left,top;
    private final List<VoiceNet.Peer> shown=new ArrayList<>();
    public PokeVoiceScreen(Screen parent){super(Text.literal("PokeVoice"));this.parent=parent;}
    protected void init(){scale=Math.min(width/1380f,height/828f);left=(int)((width-1380*scale)/2);top=(int)((height-828*scale)/2);update();VoiceClient.action(VoiceNet.Action.REFRESH,VoiceNet.NONE,VoiceNet.NONE,false,0);}
    public boolean shouldPause(){return false;}
    public void close(){client.setScreen(parent);}
    private int x(int n){return left+Math.round(n*scale);}private int y(int n){return top+Math.round(n*scale);}private int size(int n){return Math.max(1,Math.round(n*scale));}
    // Prevent Screen.render from blurring the device drawn earlier in this frame.
    @Override public void renderBackground(DrawContext c,int mx,int my,float delta) {}
    private final class VoiceButton extends ButtonWidget {
        private final boolean selected,danger;
        VoiceButton(String label,int nx,int ny,int w,Runnable action,boolean selected){
            super(x(nx),y(ny),size(w),size(40),Text.literal(label),b->action.run(),DEFAULT_NARRATION_SUPPLIER);
            this.selected=selected;this.danger=label.equals("Bloquear")||label.equals("Revocar")||label.equals("Rechazar");
        }
        @Override protected void renderWidget(DrawContext c,int mx,int my,float delta){
            boolean hover=isHovered()||isFocused();int px=getX(),py=getY(),w=getWidth(),h=getHeight();
            int border=selected?0xFFFFCF78:hover?0xFFFFE2AA:danger?0xFFAB6470:0xFF59657A;
            int bg=selected?0xFF725127:hover?0xFF303D50:0xFF1B2636;
            c.fill(px,py,px+w,py+h,border);c.fill(px+1,py+1,px+w-1,py+h-1,bg);
            float f=Math.min(Math.max(0.45f,(h-4)/12f),(w-6f)/Math.max(1,textRenderer.getWidth(getMessage())));
            int textWidth=textRenderer.getWidth(getMessage());c.getMatrices().push();
            c.getMatrices().translate(px+(w-textWidth*f)/2,py+(h-8*f)/2,0);c.getMatrices().scale(f,f,1);
            c.drawText(textRenderer,getMessage(),0,0,selected?0xFFFFE3AB:0xFFF1F4FA,false);c.getMatrices().pop();
        }
    }
    private void button(String label,int nx,int ny,int w,Runnable action){button(label,nx,ny,w,action,false);}
    private void button(String label,int nx,int ny,int w,Runnable action,boolean selected){addDrawableChild(new VoiceButton(label,nx,ny,w,action,selected));}
    public void update(){
        if(client==null)return;clearChildren();shown.clear();
        button("Inicio",80,90,120,this::close);
        String[] tabs={"Cercanos","Conocidos","Grupos","Ajustes","Privacidad"};
        for(int i=0;i<tabs.length;i++){final int value=i;button(tabs[i],474+i*154,210,146,()->{tab=value;page=0;update();},tab==value);}
        var view=VoiceClient.view;
        if(tab==2){button("Abrir grupos",640,425,400,()->VoiceClient.nativeMenu("key.voice_chat_group"));return;}
        if(tab==3){button("Microfono y audio",640,395,400,()->VoiceClient.nativeMenu("key.voice_chat_settings"));button("Volumen individual",640,460,400,()->VoiceClient.nativeMenu("key.voice_chat_adjust_volumes"));button(VoiceClient.hud?"HUD: activado":"HUD: desactivado",640,525,400,()->{VoiceClient.toggleHud();update();});return;}
        if(view==null)return;
        if(tab==4){
            button(view.receiveRequests()?"Solicitudes: si":"Solicitudes: no",620,330,450,()->VoiceClient.action(VoiceNet.Action.REQUESTS,VoiceNet.NONE,VoiceNet.NONE,!view.receiveRequests(),view.page()));
            button(view.allowKnown()?"Audio de conocidos: si":"Audio de conocidos: no",620,400,450,()->VoiceClient.action(VoiceNet.Action.KNOWN_AUDIO,VoiceNet.NONE,VoiceNet.NONE,!view.allowKnown(),view.page()));
        }
        int rows=tab==4?3:5;
        var filtered=view.peers().stream().filter(p->tab==0?p.distance()>=0&&p.distance()<=12:tab==1?p.state().equals("KNOWN"):p.ownBlock()).toList();
        page=Math.max(0,Math.min(page,Math.max(0,(filtered.size()-1)/rows)));
        shown.addAll(filtered.subList(Math.min(filtered.size(),page*rows),Math.min(filtered.size(),page*rows+rows)));
        for(int i=0;i<shown.size();i++){
            var p=shown.get(i);int row=(tab==4?480:275)+i*75;
            switch(p.state()){
                case "STRANGER" -> {button("Solicitar",985,row+10,135,()->act(VoiceNet.Action.INVITE,p));button("Bloquear",1125,row+10,95,()->act(VoiceNet.Action.BLOCK,p));}
                case "PENDING_OUTGOING" -> button("Cancelar",1000,row+10,210,()->act(VoiceNet.Action.CANCEL,p));
                case "PENDING_INCOMING" -> {button("Aceptar",985,row+10,115,()->act(VoiceNet.Action.ACCEPT,p));button("Rechazar",1105,row+10,115,()->act(VoiceNet.Action.REJECT,p));}
                case "KNOWN" -> {button("Revocar",985,row+10,115,()->act(VoiceNet.Action.REVOKE,p));button("Bloquear",1105,row+10,115,()->act(VoiceNet.Action.BLOCK,p));}
                case "BLOCKED" -> button("Desbloquear",990,row+10,230,()->act(VoiceNet.Action.UNBLOCK,p));
            }
        }
        button("Anterior",485,704,200,()->{if(page>0){page--;update();}else if(view.page()>0)VoiceClient.action(VoiceNet.Action.REFRESH,VoiceNet.NONE,VoiceNet.NONE,false,view.page()-1);});
        button("Siguiente",1040,704,200,()->{if((page+1)*rows<filtered.size()){page++;update();}else if(view.more())VoiceClient.action(VoiceNet.Action.REFRESH,VoiceNet.NONE,VoiceNet.NONE,false,view.page()+1);});
    }
    private void act(VoiceNet.Action action,VoiceNet.Peer p){VoiceClient.action(action,p.id(),p.invitation(),false,VoiceClient.view.page());}
    private void text(DrawContext c,String label,int nx,int ny,int height,int color,int maxWidth){
        float f=height*scale/9f;c.getMatrices().push();c.getMatrices().translate(x(nx),y(ny),0);c.getMatrices().scale(f,f,1);c.drawText(textRenderer,textRenderer.trimToWidth(label,(int)(maxWidth*scale/f)),0,0,color,false);c.getMatrices().pop();
    }
    public void render(DrawContext c,int mouseX,int mouseY,float delta){
        c.fill(0,0,width,height,0xAA05080D);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        c.drawTexture(CHASSIS,left,top,size(1380),size(828),0,0,1380,828,1380,828);RenderSystem.disableBlend();
        text(c,"POKEVOICE",80,175,35,0xFFFFCC6F,290);text(c,"Comunicacion",80,235,25,0xFFFFFFFF,290);text(c,"de Entrenadores",80,272,25,0xFFFFFFFF,290);
        var api=net.pokereport.luna.voice.PokeVoicePlugin.clientApi;
        text(c,api==null||api.isDisconnected()?"Estado: sin UDP":"Estado: conectado",80,350,22,0xFFFFFFFF,290);
        text(c,api==null||api.isMuted()?"Microfono: mute":"Microfono: listo",80,395,22,0xFFFFFFFF,290);
        text(c,api==null||api.isDisabled()?"Audio: apagado":"Audio: activo",80,440,22,0xFFFFFFFF,290);
        text(c,"Solo conocidos",80,535,22,0xFFFFCC6F,290);text(c,"por proximidad.",80,572,22,0xFFFFFFFF,290);
        text(c,"Los grupos son",80,635,22,0xFFFFFFFF,290);text(c,"independientes.",80,672,22,0xFFFFFFFF,290);
        if(VoiceClient.view==null)text(c,"Conectando con PokeVoice...",490,320,25,0xFF16203A,700);
        else if(!VoiceClient.view.enabled()||!VoiceClient.view.healthy())text(c,"PokeVoice no disponible. Proximidad protegida.",490,275,22,0xFF972C33,735);
        if((tab==0||tab==1)&&shown.isEmpty()&&VoiceClient.view!=null)text(c,tab==0?"Acercate a otro entrenador para solicitar voz.":"Todavia no tienes conexiones aceptadas.",490,320,23,0xFF46516A,720);
        if(tab==2){text(c,"Grupos oficiales de Simple Voice Chat",490,300,25,0xFF16203A,735);text(c,"Crear, unirse y salir sin cambiar tus relaciones.",490,355,22,0xFF16203A,735);}
        else if(tab==3)text(c,"Audio y microfono conservan sus ajustes nativos.",490,310,22,0xFF16203A,735);
        else for(int i=0;i<shown.size();i++){
            var p=shown.get(i);int row=(tab==4?480:275)+i*75;
            c.fill(x(476),y(row),x(1230),y(row+66),i%2==0?0x24233752:0x15233752);
            c.fill(x(476),y(row),x(480),y(row+66),p.state().equals("KNOWN")?0xFF2D9077:0xFFB28A46);
            var entry=client.getNetworkHandler()==null?null:client.getNetworkHandler().getPlayerListEntry(p.id());
            if(entry!=null){RenderSystem.enableBlend();PlayerSkinDrawer.draw(c,entry.getSkinTextures(),x(486),y(row+5),size(48));RenderSystem.disableBlend();}
            text(c,p.name(),548,row+5,24,0xFF16203A,410);
            String status=switch(p.state()){case "KNOWN"->(api==null||api.isDisconnected(p.id())?"Conocido · sin voz":"Conocido · voz disponible");case "PENDING_INCOMING"->"Solicitud recibida";case "PENDING_OUTGOING"->"Solicitud enviada";case "BLOCKED"->"Bloqueado";default->"Voz bloqueada";};
            if(api!=null&&p.state().equals("KNOWN")&&api.isTalking(p.id()))status="Hablando "+"|".repeat(1+(int)(System.currentTimeMillis()/180%3));
            text(c,status+(p.distance()<0?" Â· fuera de alcance":" Â· "+(int)p.distance()+" bloques"),548,row+40,19,0xFF46516A,420);
        }
        if(VoiceClient.view!=null)text(c,VoiceClient.view.message(),475,765,18,0xFF16203A,780);
        super.render(c,mouseX,mouseY,delta);
    }
}

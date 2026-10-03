package net.pokereport.luna.voice;

import com.google.gson.Gson;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.pokereport.luna.LunaEternal;
import java.util.*;
import java.util.concurrent.*;
import java.lang.reflect.Method;

public final class VoiceRuntime implements DedicatedServerModInitializer {
    private static final Gson GSON=new Gson();
    private static volatile VoiceRelations relations;
    private static VoiceConfig config=VoiceConfig.defaults();
    private static ThreadPoolExecutor worker;
    private static final Map<UUID,Long> rate=new HashMap<>();
    private static final Map<UUID,Long> refreshRate=new HashMap<>();
    private static final Map<UUID,ServerPlayerEntity> sessions=new ConcurrentHashMap<>();
    private static final Map<UUID,String> names=new ConcurrentHashMap<>();
    private static volatile Map<UUID,VoiceRelations.Presence> presence=Map.of();
    private static Method auth;
    public static VoiceRelations relations(){return relations;}
    private static boolean authenticated(ServerPlayerEntity p){
        if(!FabricLoader.getInstance().isModLoaded("easyauth"))return p.getServer()!=null&&p.getServer().isOnlineMode();
        try {
            if(auth==null)auth=Class.forName("xyz.nikitacartes.easyauth.interfaces.PlayerAuth").getMethod("easyAuth$isAuthenticated");
            return Boolean.TRUE.equals(auth.invoke(p));
        }catch(ReflectiveOperationException e){return false;}
    }
    public void onInitializeServer(){
        ServerPlayNetworking.registerGlobalReceiver(VoiceNet.Request.ID,(p,ctx)->request(ctx.player(),p));
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            try {config=VoiceConfig.load(FabricLoader.getInstance().getConfigDir().resolve("pokevoice.properties"));}
            catch(Exception e){LunaEternal.LOG.error("PokeVoice configuration invalid; proximity stays denied",e);return;}
            worker=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(128),r->{Thread t=new Thread(r,"pokevoice-control");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
            relations=new VoiceRelations(new VoiceRepository(LunaEternal.database()),config,System::currentTimeMillis);
            worker.execute(()->{try{relations.load();LunaEternal.LOG.info("PokeVoice cache ready; API 2.6.20; relations loaded");}catch(Exception e){LunaEternal.LOG.error("PokeVoice persistence unavailable; proximity stays denied",e);}});
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(relations==null)return;
            var live=new HashMap<UUID,VoiceRelations.Presence>();
            for(var p:server.getPlayerManager().getPlayerList()) {
                live.put(p.getUuid(),new VoiceRelations.Presence(p.getWorld().getRegistryKey().getValue().toString(),p.getX(),p.getY(),p.getZ(),authenticated(p),p.isAlive()));
                names.put(p.getUuid(),p.getName().getString());sessions.put(p.getUuid(),p);
            }
            presence=Map.copyOf(live);relations.presence(live);
            // Never acquire the control lock from the tick/audio thread while JDBC is running.
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{rate.remove(handler.player.getUuid());refreshRate.remove(handler.player.getUuid());names.remove(handler.player.getUuid());sessions.remove(handler.player.getUuid());
            if(worker!=null)try{worker.execute(()->relations.disconnect(handler.player.getUuid()));}catch(RejectedExecutionException e){}
            var live=new HashMap<>(presence);live.remove(handler.player.getUuid());presence=Map.copyOf(live);if(relations!=null)relations.presence(live);});
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{
            if(worker!=null){worker.shutdown();try{if(!worker.awaitTermination(10,TimeUnit.SECONDS))worker.shutdownNow();}catch(InterruptedException e){Thread.currentThread().interrupt();}}
            relations=null;presence=Map.of();rate.clear();refreshRate.clear();names.clear();sessions.clear();
        });
    }
    private static void request(ServerPlayerEntity actor,VoiceNet.Request request){
        if(relations==null||worker==null||!authenticated(actor)||!actor.isAlive())return;
        var limiter=request.action()==VoiceNet.Action.REFRESH?refreshRate:rate;
        long now=System.nanoTime();long previous=limiter.getOrDefault(actor.getUuid(),0L);
        if(now-previous<250_000_000L)return;limiter.put(actor.getUuid(),now);
        UUID actorId=actor.getUuid();MinecraftServer server=actor.getServer();
        try {worker.execute(()->{
            String message="";boolean changed=false;
            try {
                if(sessions.get(actorId)!=actor||System.nanoTime()-now>500_000_000L)throw new VoiceRelations.Denied();
                var live=presence.get(actorId);if(live==null||!live.authenticated())throw new VoiceRelations.Denied();
                switch(request.action()) {
                    case REFRESH -> {}
                    case INVITE -> {relations.invite(actorId,request.target());message="Solicitud de voz enviada.";changed=true;}
                    case ACCEPT -> {relations.accept(actorId,request.target(),request.invitation());message="Conexion de voz establecida.";changed=true;}
                    case REJECT -> {relations.reject(actorId,request.target(),request.invitation());message="Solicitud rechazada.";changed=true;}
                    case CANCEL -> {relations.cancel(actorId,request.target(),request.invitation());message="Solicitud cancelada.";changed=true;}
                    case REVOKE -> {relations.revoke(actorId,request.target());message="Comunicacion revocada.";changed=true;}
                    case BLOCK -> {relations.block(actorId,request.target());message="Entrenador bloqueado.";changed=true;}
                    case UNBLOCK -> {relations.unblock(actorId,request.target());message="Bloqueo retirado; la comunicacion requiere una nueva solicitud.";changed=true;}
                    case REQUESTS -> {var p=relations.preferences(actorId);relations.preferences(actorId,new VoiceRelations.Preferences(request.value(),p.allowKnown()));changed=true;}
                    case KNOWN_AUDIO -> {var p=relations.preferences(actorId);relations.preferences(actorId,new VoiceRelations.Preferences(p.receiveRequests(),request.value()));changed=true;}
                }
            }catch(VoiceRelations.Denied|IllegalArgumentException e){message="Accion de voz no disponible.";}
             catch(Exception e){message="PokeVoice no disponible; tu privacidad se mantiene.";LunaEternal.LOG.error("PokeVoice control failed",e);}
            VoiceNet.Snapshot result=snapshot(actorId,request.page(),message);
            VoiceNet.Snapshot targetResult=changed&&presence.containsKey(request.target())?snapshot(request.target(),0,(request.action()==VoiceNet.Action.BLOCK||request.action()==VoiceNet.Action.UNBLOCK)?"":message):null;
            server.execute(()->{
                // The session object must still be the same: a response cannot cross reconnects.
                if(server.getPlayerManager().getPlayer(actorId)==actor&&authenticated(actor))send(actor,result);
                var target=server.getPlayerManager().getPlayer(request.target());
                if(targetResult!=null&&target!=null&&authenticated(target))send(target,targetResult);
            });
        });}catch(RejectedExecutionException e){/* bounded queue: drop requests, never weaken audio consent */}
    }
    private static void send(ServerPlayerEntity p,VoiceNet.Snapshot snapshot){if(ServerPlayNetworking.canSend(p,VoiceNet.Snapshot.ID))ServerPlayNetworking.send(p,snapshot);}
    private static VoiceNet.Snapshot snapshot(UUID actor,int page,String message){
        var model=relations;var pref=model.preferences(actor);Map<UUID,VoiceRelations.Presence> live=presence;
        LinkedHashSet<UUID> ids=new LinkedHashSet<>();var me=live.get(actor);
        if(me!=null)live.forEach((id,p)->{if(!id.equals(actor)&&me.closeTo(p,config.inviteDistance()))ids.add(id);});
        model.edges().forEach((pair,e)->{if(pair.low().equals(actor)||pair.high().equals(actor)){
            boolean own=pair.low().equals(actor)?e.blockLow():e.blockHigh();
            if(e.known()||own)ids.add(pair.low().equals(actor)?pair.high():pair.low());
        }});
        var requests=model.invitations(actor);
        var ordered=new ArrayList<>(ids);ordered.sort(Comparator.<UUID>comparingInt(id->requests.stream().anyMatch(i->i.from().equals(id)||i.to().equals(id))?0:1).thenComparing(UUID::toString));List<VoiceNet.Peer> peers=new ArrayList<>();
        int start=Math.min(ordered.size(),page*50),end=Math.min(ordered.size(),start+50);
        for(UUID id:ordered.subList(start,end)) {
            var p=live.get(id);double distance=me!=null&&p!=null&&me.dimension().equals(p.dimension())?Math.sqrt(Math.pow(me.x()-p.x(),2)+Math.pow(me.y()-p.y(),2)+Math.pow(me.z()-p.z(),2)):-1;
            var edge=model.edges().get(VoiceRelations.Pair.of(actor,id));boolean own=edge!=null&&(VoiceRelations.Pair.of(actor,id).low().equals(actor)?edge.blockLow():edge.blockHigh());
            var state=model.state(actor,id);if(state==VoiceRelations.State.BLOCKED&&!own)state=VoiceRelations.State.STRANGER;
            var invitation=requests.stream().filter(i->i.from().equals(id)||i.to().equals(id)).findFirst().orElse(null);
            peers.add(new VoiceNet.Peer(id,names.getOrDefault(id,id.toString().substring(0,8)),distance,state.name(),invitation==null?VoiceNet.NONE:invitation.id(),invitation==null?0:Math.max(0,invitation.expiresAt()-System.currentTimeMillis()),own));
        }
        return new VoiceNet.Snapshot(GSON.toJson(new VoiceNet.View(config.enabled(),model.healthy(),pref.receiveRequests(),pref.allowKnown(),config.nearbyHud(),page,end<ordered.size(),message,peers)));
    }
}

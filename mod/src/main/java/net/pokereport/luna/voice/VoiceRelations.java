package net.pokereport.luna.voice;

import java.util.*;
import java.util.function.LongSupplier;

/** Serialized control operations; the audio path only reads the published immutable cache. */
public final class VoiceRelations {
    public enum State { STRANGER, PENDING_OUTGOING, PENDING_INCOMING, KNOWN, BLOCKED }
    public record Pair(UUID low, UUID high) {
        public static Pair of(UUID a,UUID b) {
            if(a.equals(b)) throw new IllegalArgumentException("self relationship");
            return a.compareTo(b)<0 ? new Pair(a,b):new Pair(b,a);
        }
    }
    public record Edge(boolean known,boolean blockLow,boolean blockHigh) {
        public boolean blocked() { return blockLow||blockHigh; }
    }
    public record Preferences(boolean receiveRequests,boolean allowKnown) {}
    public record Presence(String dimension,double x,double y,double z,boolean authenticated,boolean alive) {
        public boolean closeTo(Presence p,double distance) {
            double dx=x-p.x,dy=y-p.y,dz=z-p.z;
            return authenticated&&p.authenticated&&alive&&p.alive&&dimension.equals(p.dimension)
                &&Double.isFinite(dx+dy+dz)&&dx*dx+dy*dy+dz*dz<=distance*distance;
        }
    }
    public record Invitation(UUID id,UUID from,UUID to,long expiresAt) {}
    public record Saved(Map<Pair,Edge> edges,Map<UUID,Preferences> preferences) {}
    public interface Repository {
        Saved load() throws Exception;
        void edge(Pair pair,Edge value) throws Exception;
        void preferences(UUID user,Preferences value) throws Exception;
    }
    public static final class Denied extends RuntimeException { public Denied() {super("Accion de voz no disponible.");} }
    private static final Preferences DEFAULT=new Preferences(true,true);
    private final Repository repository;
    private final VoiceConfig config;
    private final LongSupplier clock;
    private final Map<Pair,Invitation> pending=new HashMap<>();
    private final Map<Pair,Long> cooldown=new HashMap<>();
    private final Map<UUID,Long> actorCooldown=new HashMap<>();
    private volatile Saved saved=new Saved(Map.of(),Map.of());
    private volatile Map<UUID,Presence> online=Map.of();
    private volatile boolean healthy;
    public VoiceRelations(Repository repository,VoiceConfig config,LongSupplier clock) {this.repository=repository;this.config=config;this.clock=clock;}
    public synchronized void load() throws Exception {
        Saved s=repository.load();
        for(var e:s.edges.entrySet()) {
            if(e.getKey().low.compareTo(e.getKey().high)>=0 || (e.getValue().known&&e.getValue().blocked()))
                throw new IllegalArgumentException("Invalid relationship record");
        }
        saved=new Saved(Map.copyOf(s.edges),Map.copyOf(s.preferences)); healthy=true;
    }
    public boolean healthy(){return healthy;}
    public void presence(Map<UUID,Presence> value){online=Map.copyOf(value);}
    /** Transport may negotiate before EasyAuth login; microphone audio must not. */
    public boolean authenticatedOnline(UUID actor){
        if(actor==null||!healthy||!config.enabled())return false;
        Presence p=online.get(actor);return p!=null&&p.authenticated()&&p.alive();
    }
    public Preferences preferences(UUID id){return saved.preferences.getOrDefault(id,DEFAULT);}
    public Map<Pair,Edge> edges(){return saved.edges;}
    public synchronized List<Invitation> invitations(UUID actor){expire();return pending.values().stream().filter(i->i.from.equals(actor)||i.to.equals(actor)).toList();}
    public synchronized State state(UUID actor,UUID target){
        if(actor.equals(target))return State.STRANGER;
        Edge e=saved.edges.get(Pair.of(actor,target));
        if(e!=null&&e.blocked())return State.BLOCKED;
        if(e!=null&&e.known)return State.KNOWN;
        expire();Invitation i=pending.get(Pair.of(actor,target));
        return i==null?State.STRANGER:i.from.equals(actor)?State.PENDING_OUTGOING:State.PENDING_INCOMING;
    }
    /** SVC remains responsible for radius, attenuation, whisper and channel transport. */
    public boolean allowsProximity(UUID sender,UUID receiver){
        if(!healthy || !config.enabled() || sender==null || receiver==null || sender.equals(receiver))return false;
        Presence a=online.get(sender),b=online.get(receiver);
        if(a==null||b==null||!a.authenticated||!b.authenticated||!a.alive||!b.alive||!a.dimension.equals(b.dimension))return false;
        Saved s=saved; Edge e=s.edges.get(Pair.of(sender,receiver));
        if(e!=null&&e.blocked())return false;
        if(!s.preferences.getOrDefault(sender,DEFAULT).allowKnown||!s.preferences.getOrDefault(receiver,DEFAULT).allowKnown)return false;
        return !config.requireRelationship() || e!=null&&e.known;
    }
    private void requireActor(UUID actor){Presence p=online.get(actor);if(!healthy||!config.enabled()||p==null||!p.authenticated||!p.alive)throw new Denied();}
    private void encounter(UUID a,UUID b){
        requireActor(a); Presence x=online.get(a),y=online.get(b);
        if(a.equals(b)||y==null||!x.closeTo(y,config.inviteDistance()))throw new Denied();
    }
    public synchronized Invitation invite(UUID a,UUID b){
        encounter(a,b);expire();Pair pair=Pair.of(a,b);long now=clock.getAsLong();
        if(state(a,b)!=State.STRANGER||!preferences(b).receiveRequests||!preferences(a).receiveRequests
            ||cooldown.getOrDefault(pair,0L)>now||actorCooldown.getOrDefault(a,0L)>now
            ||count(a)>=config.maxPending()||count(b)>=config.maxPending())throw new Denied();
        Invitation i=new Invitation(UUID.randomUUID(),a,b,now+config.expirationSeconds()*1000L);
        pending.put(pair,i);actorCooldown.put(a,now+2000);return i;
    }
    private long count(UUID actor){return pending.values().stream().filter(i->i.from.equals(actor)||i.to.equals(actor)).count();}
    private Invitation invitation(UUID actor,UUID target,UUID nonce,boolean recipient){
        requireActor(actor);expire();Invitation i=pending.get(Pair.of(actor,target));
        if(i==null||!i.id.equals(nonce)||!(recipient?i.to:i.from).equals(actor))throw new Denied();return i;
    }
    public synchronized void accept(UUID actor,UUID target,UUID nonce) throws Exception {
        Invitation i=invitation(actor,target,nonce,true);encounter(actor,target);
        if(!preferences(actor).receiveRequests||!preferences(target).receiveRequests)throw new Denied();
        Pair p=Pair.of(actor,target);Edge e=saved.edges.get(p);
        if(e!=null&&e.blocked())throw new Denied();
        write(p,new Edge(true,false,false));pending.remove(p);
    }
    public synchronized void reject(UUID actor,UUID target,UUID nonce){
        Invitation i=invitation(actor,target,nonce,true);Pair p=Pair.of(i.from,i.to);
        pending.remove(p);cooldown.put(p,clock.getAsLong()+config.rejectCooldownSeconds()*1000L);
    }
    public synchronized void cancel(UUID actor,UUID target,UUID nonce){
        Invitation i=invitation(actor,target,nonce,false);pending.remove(Pair.of(i.from,i.to));
    }
    public synchronized void revoke(UUID actor,UUID target) throws Exception {
        requireActor(actor);Pair p=Pair.of(actor,target);Edge e=saved.edges.get(p);
        if(e==null||!e.known)throw new Denied();write(p,new Edge(false,e.blockLow,e.blockHigh));pending.remove(p);
    }
    public synchronized void block(UUID actor,UUID target) throws Exception {
        requireActor(actor);Pair p=Pair.of(actor,target);Edge e=saved.edges.getOrDefault(p,new Edge(false,false,false));
        write(p,new Edge(false,p.low.equals(actor)||e.blockLow,p.high.equals(actor)||e.blockHigh));pending.remove(p);
    }
    public synchronized void unblock(UUID actor,UUID target) throws Exception {
        requireActor(actor);Pair p=Pair.of(actor,target);Edge e=saved.edges.get(p);
        if(e==null)throw new Denied();write(p,new Edge(false,p.low.equals(actor)?false:e.blockLow,p.high.equals(actor)?false:e.blockHigh));
    }
    private void write(Pair p,Edge value) throws Exception {
        // Denial is visible immediately; newly granted consent only after durable commit.
        Map<Pair,Edge> m=new HashMap<>(saved.edges);m.put(p,value);
        Saved next=new Saved(Map.copyOf(m),saved.preferences);
        if(!value.known)saved=next;
        try {repository.edge(p,value);saved=next;}catch(Exception e){healthy=false;throw e;}
    }
    public synchronized void preferences(UUID actor,Preferences value) throws Exception {
        requireActor(actor);Map<UUID,Preferences> m=new HashMap<>(saved.preferences);m.put(actor,value);
        Saved next=new Saved(saved.edges,Map.copyOf(m));
        if(!value.allowKnown||!value.receiveRequests)saved=next;
        try{repository.preferences(actor,value);saved=next;}catch(Exception e){healthy=false;throw e;}
        if(!value.receiveRequests)pending.values().removeIf(i->i.from.equals(actor)||i.to.equals(actor));
    }
    public synchronized void disconnect(UUID actor){pending.values().removeIf(i->i.from.equals(actor)||i.to.equals(actor));actorCooldown.remove(actor);}
    public synchronized void expire(){
        long now=clock.getAsLong();Map<UUID,Presence> live=online;
        pending.values().removeIf(i->i.expiresAt<=now||live.get(i.from)==null||live.get(i.to)==null
            ||!live.get(i.from).closeTo(live.get(i.to),config.inviteDistance()));
        cooldown.values().removeIf(t->t<=now);actorCooldown.values().removeIf(t->t<=now);
    }
}

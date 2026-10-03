package net.pokereport.luna.voice;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

class VoiceRelationsTest {
    static class Memory implements VoiceRelations.Repository {
        Map<VoiceRelations.Pair,VoiceRelations.Edge> edges=new HashMap<>();Map<UUID,VoiceRelations.Preferences> prefs=new HashMap<>();int calls;boolean fail;
        public VoiceRelations.Saved load(){return new VoiceRelations.Saved(edges,prefs);}
        public void edge(VoiceRelations.Pair p,VoiceRelations.Edge e) throws Exception {calls++;if(fail)throw new Exception("storage unavailable");edges.put(p,e);}
        public void preferences(UUID u,VoiceRelations.Preferences p) throws Exception{calls++;if(fail)throw new Exception("storage unavailable");prefs.put(u,p);}
    }
    final UUID a=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID();
    AtomicLong now=new AtomicLong(1000);Memory repo;VoiceRelations model;
    Map<UUID,VoiceRelations.Presence> live;
    VoiceRelations.Presence pos(String dimension,double x){return new VoiceRelations.Presence(dimension,x,98,0,true,true);}
    @BeforeEach void setup() throws Exception {repo=new Memory();model=new VoiceRelations(repo,VoiceConfig.defaults(),now::get);model.load();live=new HashMap<>(Map.of(a,pos("pokereport:ciudadela",0),b,pos("pokereport:ciudadela",4),c,pos("pokereport:ciudadela",5)));model.presence(live);}
    void known() throws Exception{var i=model.invite(a,b);model.accept(b,a,i.id());}
    @Test void transportHandshakeDoesNotAuthorizeUnauthenticatedMicrophone(){
        live.put(a,new VoiceRelations.Presence("pokereport:ciudadela",0,98,0,false,true));model.presence(live);
        assertFalse(model.authenticatedOnline(a));assertTrue(model.authenticatedOnline(b));assertFalse(model.authenticatedOnline(null));
    }
    @Test void authenticatedMicrophoneDoesNotRequireKnownRelation(){assertTrue(model.authenticatedOnline(a));assertFalse(model.allowsProximity(a,b));}
    @Test void disconnectedOrDeadMicrophoneDenied(){live.remove(a);live.put(b,new VoiceRelations.Presence("pokereport:ciudadela",0,98,0,true,false));model.presence(live);assertFalse(model.authenticatedOnline(a));assertFalse(model.authenticatedOnline(b));}
    @Test void strangersDenied(){assertFalse(model.allowsProximity(a,b));assertFalse(model.allowsProximity(b,a));}
    @Test void knownBidirectional() throws Exception{known();assertTrue(model.allowsProximity(a,b));assertTrue(model.allowsProximity(b,a));}
    @Test void pendingDirectional(){model.invite(a,b);assertEquals(VoiceRelations.State.PENDING_OUTGOING,model.state(a,b));assertEquals(VoiceRelations.State.PENDING_INCOMING,model.state(b,a));assertFalse(model.allowsProximity(a,b));}
    @Test void crossInvitesDenied(){model.invite(a,b);assertThrows(VoiceRelations.Denied.class,()->model.invite(b,a));}
    @Test void selfInviteDenied(){assertThrows(VoiceRelations.Denied.class,()->model.invite(a,a));}
    @Test void distanceBoundary(){live.put(b,pos("pokereport:ciudadela",12));model.presence(live);assertNotNull(model.invite(a,b));}
    @Test void outsideDistanceDenied(){live.put(b,pos("pokereport:ciudadela",12.01));model.presence(live);assertThrows(VoiceRelations.Denied.class,()->model.invite(a,b));}
    @Test void differentDimensionDenied(){live.put(b,pos("pokereport:johto",4));model.presence(live);assertThrows(VoiceRelations.Denied.class,()->model.invite(a,b));}
    @Test void expiresAtBoundary(){var i=model.invite(a,b);now.set(i.expiresAt());assertThrows(VoiceRelations.Denied.class,()->model.accept(b,a,i.id()));assertEquals(VoiceRelations.State.STRANGER,model.state(a,b));}
    @Test void nonceReplayDenied() throws Exception{var i=model.invite(a,b);model.accept(b,a,i.id());assertThrows(VoiceRelations.Denied.class,()->model.accept(b,a,i.id()));}
    @Test void forgedNonceDenied(){model.invite(a,b);assertThrows(VoiceRelations.Denied.class,()->model.accept(b,a,UUID.randomUUID()));}
    @Test void senderCannotAcceptOwnInvite(){var i=model.invite(a,b);assertThrows(VoiceRelations.Denied.class,()->model.accept(a,b,i.id()));}
    @Test void recipientCannotCancel(){var i=model.invite(a,b);assertThrows(VoiceRelations.Denied.class,()->model.cancel(b,a,i.id()));}
    @Test void cancelDoesNotGrant(){var i=model.invite(a,b);model.cancel(a,b,i.id());assertFalse(model.allowsProximity(a,b));assertThrows(VoiceRelations.Denied.class,()->model.accept(b,a,i.id()));}
    @Test void rejectCooldown(){var i=model.invite(a,b);model.reject(b,a,i.id());now.addAndGet(59_999);assertThrows(VoiceRelations.Denied.class,()->model.invite(a,b));now.addAndGet(1);assertNotNull(model.invite(a,b));}
    @Test void actorCooldown(){model.invite(a,b);assertThrows(VoiceRelations.Denied.class,()->model.invite(a,c));now.addAndGet(2000);assertNotNull(model.invite(a,c));}
    @Test void requestPreference() throws Exception{model.preferences(b,new VoiceRelations.Preferences(false,true));assertThrows(VoiceRelations.Denied.class,()->model.invite(a,b));}
    @Test void togglingRequestsClearsPending() throws Exception{model.invite(a,b);model.preferences(b,new VoiceRelations.Preferences(false,true));assertTrue(model.invitations(a).isEmpty());}
    @Test void blockPrecedence() throws Exception{known();model.block(b,a);assertFalse(model.allowsProximity(a,b));assertFalse(model.allowsProximity(b,a));assertThrows(VoiceRelations.Denied.class,()->model.invite(a,b));}
    @Test void unblockDoesNotRestoreConsent() throws Exception{known();model.block(a,b);model.unblock(a,b);assertFalse(model.allowsProximity(a,b));}
    @Test void mutualBlockCannotRemoveOtherBlock() throws Exception{model.block(a,b);model.block(b,a);model.unblock(a,b);assertEquals(VoiceRelations.State.BLOCKED,model.state(a,b));}
    @Test void revokePersists() throws Exception{known();model.revoke(a,b);VoiceRelations restored=new VoiceRelations(repo,VoiceConfig.defaults(),now::get);restored.load();restored.presence(live);assertFalse(restored.allowsProximity(a,b));}
    @Test void restartKnownNotPending() throws Exception{known();model.invite(b,c);VoiceRelations restored=new VoiceRelations(repo,VoiceConfig.defaults(),now::get);restored.load();restored.presence(live);assertTrue(restored.allowsProximity(a,b));assertEquals(VoiceRelations.State.STRANGER,restored.state(b,c));}
    @Test void offlineAudioDenied() throws Exception{known();live.remove(b);model.presence(live);assertFalse(model.allowsProximity(a,b));}
    @Test void disconnectedInvitationGone(){var i=model.invite(a,b);model.disconnect(b);assertThrows(VoiceRelations.Denied.class,()->model.accept(b,a,i.id()));}
    @Test void dimensionTransitionExpiresInvitation(){var i=model.invite(a,b);live.put(b,pos("pokereport:johto",4));model.presence(live);assertThrows(VoiceRelations.Denied.class,()->model.accept(b,a,i.id()));}
    @Test void knownDoesNotCrossDimensions() throws Exception{known();live.put(b,pos("pokereport:johto",4));model.presence(live);assertFalse(model.allowsProximity(a,b));live.put(a,pos("pokereport:johto",0));model.presence(live);assertTrue(model.allowsProximity(a,b));}
    @Test void unauthenticatedDenied(){live.put(b,new VoiceRelations.Presence("pokereport:ciudadela",4,98,0,false,true));model.presence(live);assertThrows(VoiceRelations.Denied.class,()->model.invite(a,b));}
    @Test void deadParticipantDenied(){live.put(b,new VoiceRelations.Presence("pokereport:ciudadela",4,98,0,true,false));model.presence(live);assertThrows(VoiceRelations.Denied.class,()->model.invite(a,b));}
    @Test void allowKnownPreference() throws Exception{known();model.preferences(a,new VoiceRelations.Preferences(true,false));assertFalse(model.allowsProximity(a,b));assertFalse(model.allowsProximity(b,a));}
    @Test void noStorageCallsInAudio() throws Exception{known();int before=repo.calls;for(int n=0;n<10000;n++)assertTrue(model.allowsProximity(a,b));assertEquals(before,repo.calls);}
    @Test void persistenceFailureDoesNotGrant() throws Exception{var i=model.invite(a,b);repo.fail=true;assertThrows(Exception.class,()->model.accept(b,a,i.id()));assertFalse(model.healthy());assertFalse(model.allowsProximity(a,b));}
    @Test void unknownOrNullIdentityDenied(){assertFalse(model.allowsProximity(null,b));assertFalse(model.allowsProximity(a,UUID.randomUUID()));}
    @Test void acceptRejectRace() throws Exception{var i=model.invite(a,b);var executor=Executors.newFixedThreadPool(2);try{var calls=executor.invokeAll(List.<Callable<Boolean>>of(()->{try{model.accept(b,a,i.id());return true;}catch(VoiceRelations.Denied e){return false;}},()->{try{model.reject(b,a,i.id());return true;}catch(VoiceRelations.Denied e){return false;}}));assertNotEquals(calls.get(0).get(),calls.get(1).get());assertTrue(model.invitations(a).isEmpty());}finally{executor.shutdownNow();}}
    @Test void corruptRecordFailsClosed(){repo.edges.put(new VoiceRelations.Pair(a,a),new VoiceRelations.Edge(true,false,false));var bad=new VoiceRelations(repo,VoiceConfig.defaults(),now::get);assertThrows(Exception.class,bad::load);assertFalse(bad.allowsProximity(a,b));}
    @Test void disabledRemainsClosed() throws Exception{known();var disabled=new VoiceRelations(repo,new VoiceConfig(false,true,12,30,60,3,true),now::get);disabled.load();disabled.presence(live);assertFalse(disabled.allowsProximity(a,b));}
    @Test void maxPending() throws Exception{for(int n=0;n<3;n++){UUID id=UUID.randomUUID();live.put(id,pos("pokereport:ciudadela",1));model.presence(live);model.invite(a,id);now.addAndGet(2000);}assertThrows(VoiceRelations.Denied.class,()->model.invite(a,b));}
    @Test void maliciousCoordinates(){live.put(b,pos("pokereport:ciudadela",Double.NaN));model.presence(live);assertThrows(VoiceRelations.Denied.class,()->model.invite(a,b));}
}

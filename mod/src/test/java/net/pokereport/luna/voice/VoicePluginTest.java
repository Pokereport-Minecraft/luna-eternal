package net.pokereport.luna.voice;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

class VoicePluginTest {
    @SuppressWarnings("unchecked") static <T> T proxy(Class<T> type,java.util.function.Function<Method,Object> fn){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->fn.apply(m));}
    @Test void sourceBypassAllThreePacketTypes(){
        Map<Class<?>,Consumer<?>> callbacks=new HashMap<>();
        var plugin=new PokeVoicePlugin();plugin.registerEvents(new EventRegistration(){public <T extends Event>void registerEvent(Class<T> c,Consumer<T> f,int priority){callbacks.put(c,f);}});
        for(var type:List.of(EntitySoundPacketEvent.class,LocationalSoundPacketEvent.class,StaticSoundPacketEvent.class))for(String source:List.of("group","plugin","spectator")){
            AtomicBoolean cancelled=new AtomicBoolean();Object e=proxy(type,m->switch(m.getName()){case "getSource"->source;case "cancel"->{cancelled.set(true);yield true;}default->throw new AssertionError("Bypass touched "+m.getName());});
            ((Consumer<Object>)callbacks.get(type)).accept(e);assertFalse(cancelled.get());
        }
    }
    @Test void microphoneWithoutAuthenticatedRuntimeDenied(){
        Map<Class<?>,Consumer<?>> callbacks=new HashMap<>();new PokeVoicePlugin().registerEvents(new EventRegistration(){public <T extends Event>void registerEvent(Class<T> c,Consumer<T> f,int priority){callbacks.put(c,f);}});
        AtomicBoolean cancelled=new AtomicBoolean();var e=proxy(MicrophonePacketEvent.class,m->switch(m.getName()){case "getSenderConnection"->null;case "cancel"->{cancelled.set(true);yield true;}default->throw new AssertionError(m.getName());});
        ((Consumer<MicrophonePacketEvent>)callbacks.get(MicrophonePacketEvent.class)).accept(e);assertTrue(cancelled.get());
    }
    @Test void missingSenderDeniedForProximity(){
        Map<Class<?>,Consumer<?>> callbacks=new HashMap<>();new PokeVoicePlugin().registerEvents(new EventRegistration(){public <T extends Event>void registerEvent(Class<T> c,Consumer<T> f,int priority){callbacks.put(c,f);}});
        AtomicBoolean cancelled=new AtomicBoolean();var e=proxy(EntitySoundPacketEvent.class,m->switch(m.getName()){case "getSource"->"proximity";case "getSenderConnection","getReceiverConnection"->null;case "cancel"->{cancelled.set(true);yield true;}default->throw new AssertionError(m.getName());});
        ((Consumer<EntitySoundPacketEvent>)callbacks.get(EntitySoundPacketEvent.class)).accept(e);assertTrue(cancelled.get());
    }
}

package net.pokereport.luna.voice;

import java.util.*;
import java.lang.management.*;

/** Standalone synthetic policy benchmark. This does NOT measure Minecraft TPS or actual UDP audio. */
public final class VoiceFilterBenchmark {
    public static void main(String[] args) throws Exception {
        System.out.println("players,decisions,allowed,wall_ms,thread_cpu_ms,ns_per_decision,heap_delta_bytes,gc_count_delta,gc_ms_delta,repository_calls_during_audio");
        for(int n:new int[]{2,10,25,50})run(n);
    }
    private static void run(int n) throws Exception {
        class Memory implements VoiceRelations.Repository {
            int calls;public VoiceRelations.Saved load(){return new VoiceRelations.Saved(Map.of(),Map.of());}
            public void edge(VoiceRelations.Pair p,VoiceRelations.Edge e){calls++;}public void preferences(UUID u,VoiceRelations.Preferences p){calls++;}
        }
        Memory repository=new Memory();long[] clock={1000};var model=new VoiceRelations(repository,VoiceConfig.defaults(),()->clock[0]);model.load();
        UUID[] users=new UUID[n];var live=new HashMap<UUID,VoiceRelations.Presence>();
        for(int i=0;i<n;i++){users[i]=UUID.randomUUID();live.put(users[i],new VoiceRelations.Presence("pokereport:ciudadela",i%8,98,0,true,true));}model.presence(live);
        for(int i=0;i<n;i++)for(int j=i+1;j<n;j++){clock[0]+=2000;var invitation=model.invite(users[i],users[j]);model.accept(users[j],users[i],invitation.id());}
        int iterations=2_000_000;long allowed=0;
        for(int i=0;i<200_000;i++)model.allowsProximity(users[i%n],users[(i+1)%n]);
        var cpu=ManagementFactory.getThreadMXBean();long threadBefore=cpu.getCurrentThreadCpuTime();long heapBefore=used(),gcBefore=gcCount(),gcTimeBefore=gcTime();int calls=repository.calls;long before=System.nanoTime();
        for(int i=0;i<iterations;i++)if(model.allowsProximity(users[i%n],users[(i+1)%n]))allowed++;
        long elapsed=System.nanoTime()-before;
        System.out.printf(Locale.ROOT,"%d,%d,%d,%.3f,%.3f,%.1f,%d,%d,%d,%d%n",n,iterations,allowed,elapsed/1e6,(cpu.getCurrentThreadCpuTime()-threadBefore)/1e6,elapsed/(double)iterations,used()-heapBefore,gcCount()-gcBefore,gcTime()-gcTimeBefore,repository.calls-calls);
        if(allowed!=iterations||calls!=repository.calls)throw new AssertionError("Policy benchmark invariant");
    }
    static long used(){return Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();}
    static long gcCount(){return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(x->Math.max(0,x.getCollectionCount())).sum();}
    static long gcTime(){return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(x->Math.max(0,x.getCollectionTime())).sum();}
}

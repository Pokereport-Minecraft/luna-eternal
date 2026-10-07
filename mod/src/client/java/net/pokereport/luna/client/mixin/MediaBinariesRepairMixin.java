package net.pokereport.luna.client.mixin;

import net.pokereport.luna.media.NativeMediaRepair;
import org.watermedia.WaterMedia;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pinned WaterMedia Binaries 3.0.0.6 trusts version.cfg even after DLL loss. */
@Pseudo
@Mixin(targets="org.watermedia.binaries.WaterMediaBinaries",remap=false)
public abstract class MediaBinariesRepairMixin {
    @Inject(method="start",at=@At("HEAD"),remap=false)
    private void pokereport$repairMedia(WaterMedia context,CallbackInfoReturnable<Boolean> cir) {
        if(!System.getProperty("os.name","").toLowerCase(java.util.Locale.ROOT).startsWith("windows"))return;
        try {
            int repaired=NativeMediaRepair.repair(WaterMedia.tmp().resolve("ffmpeg"),
                    WaterMedia.class.getClassLoader().getResourceAsStream("libs/ffmpeg-windows.zip"));
            org.slf4j.LoggerFactory.getLogger("PokeReport-Media").info("Official FFmpeg native files verified; repaired={}",repaired);
        } catch(java.io.IOException e){org.slf4j.LoggerFactory.getLogger("PokeReport-Media").error("FFmpeg native repair failed",e);}
    }
}

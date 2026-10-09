package net.pokereport.luna.cards;

import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.pokereport.luna.net.Red;
import net.pokereport.luna.ui.Toque;
import net.pokereport.luna.world.LunaDimensions;

/**
 * NPC Coleccionista de Cartas en Ciudadela.
 * Coordenadas exactas: X = 29.690, Y = 72, Z = 69.037, yaw = 180.0f (mirando al NORTE).
 * Al interactuar (click derecho), abre la sección de Cartas en el PokéPad vía Red.enviarAbrirComercio(sp, "cartas").
 */
public final class CardsNpc {

    private CardsNpc() {}

    public static final String TAG_CARDS_NPC = "luna_npc_cartas";
    public static final Vec3d POSICION = new Vec3d(29.690, 72.0, 69.037);
    public static final float YAW = 180.0f; // Mirando al Norte (180 grados en Minecraft)
    public static final String NOMBRE = "Coleccionista de Cartas";

    public static void registrar() {
        UseEntityCallback.EVENT.register((jugador, mundo, mano, entidad, golpe) -> {
            if (!es(entidad)) return ActionResult.PASS;
            if (mano != Hand.MAIN_HAND) return ActionResult.SUCCESS;
            if (!mundo.isClient() && jugador instanceof ServerPlayerEntity sp
                    && !Toque.repetido(sp.getUuid(), "npc:cartas")) {
                Red.enviarAbrirComercio(sp, "cartas");
            }
            return ActionResult.SUCCESS;
        });
    }

    public static boolean es(Entity e) {
        return e != null && e.getCommandTags().contains(TAG_CARDS_NPC);
    }

    public static boolean colocar(MinecraftServer servidor) {
        ServerWorld mundo = servidor.getWorld(LunaDimensions.CIUDADELA);
        if (mundo == null) return false;

        mundo.getChunk(BlockPos.ofFloored(POSICION));
        for (Entity e : mundo.getOtherEntities(null, Box.of(POSICION, 4, 4, 4),
                CardsNpc::es)) {
            e.discard();
        }

        TrainerMob mob = TrainerMob.getEntityType().create(mundo);
        if (mob == null) return false;

        mob.refreshPositionAndAngles(POSICION.x, POSICION.y, POSICION.z, YAW, 0f);
        mob.setHeadYaw(YAW);
        mob.setBodyYaw(YAW);
        mob.setCustomName(Text.literal(NOMBRE));
        mob.setCustomNameVisible(true);
        mob.setAiDisabled(true);
        mob.setInvulnerable(true);
        mob.setSilent(true);
        mob.setPersistent(true);
        mob.addCommandTag("luna_npc");
        mob.addCommandTag(TAG_CARDS_NPC);

        return mundo.spawnEntity(mob);
    }
}

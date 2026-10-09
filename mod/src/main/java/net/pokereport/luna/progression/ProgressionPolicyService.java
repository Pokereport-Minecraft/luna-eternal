package net.pokereport.luna.progression;

import net.minecraft.server.network.ServerPlayerEntity;
import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.gym.MedallaService;
import net.pokereport.luna.rank.RankService;
import net.pokereport.luna.ui.Tablist;

import java.util.UUID;

/**
 * Servicio centralizado de políticas de acceso y progresión (Section 24).
 * Evalúa rango, licencia, medallas, Pokédex, misiones y límites diarios de manera uniforme.
 */
public final class ProgressionPolicyService {

    public enum ContentType {
        SALVAJE_AVANZADO,
        TIENDA_OBJETOS_RECIOS,
        TIENDA_LAZO_DESTINO,
        CRIANZA_AVANZADA,
        EXPEDICION_LEGENDARIA,
        SANTUARIO_MITICO,
        INCURSION_ALTA_DIFICULTAD,
        GTS_PUBLICACION,
        EVENTO_PREMIUM
    }

    public record AccessDecision(boolean allowed, String reason) {
        public static AccessDecision allow() { return new AccessDecision(true, null); }
        public static AccessDecision deny(String reason) { return new AccessDecision(false, reason); }
    }

    private ProgressionPolicyService() {}

    /**
     * Evalúa si un jugador puede acceder a determinado contenido del juego.
     */
    public static AccessDecision canAccess(ServerPlayerEntity player, ContentType content) {
        if (player == null) return AccessDecision.deny("Jugador no válido");
        if (player.hasPermissionLevel(2) || player.isCreative()) return AccessDecision.allow();

        UUID uuid = player.getUuid();
        TrainerLicense license = TrainerLicenseService.getLicense(uuid);
        Tablist.Rank rank = RankService.enCache(uuid);
        int badges = Integer.bitCount(MedallaService.enCache(uuid));

        return switch (content) {
            case SALVAJE_AVANZADO -> {
                if (license.ordinal() < TrainerLicense.BRONCE.ordinal()) {
                    yield AccessDecision.deny("Requiere al menos Licencia Bronce (2 medallas).");
                }
                yield AccessDecision.allow();
            }
            case TIENDA_OBJETOS_RECIOS -> {
                if (license.ordinal() < TrainerLicense.PLATA.ordinal()) {
                    yield AccessDecision.deny("Requiere Licencia Plata (4 medallas y 30% Pokédex).");
                }
                yield AccessDecision.allow();
            }
            case TIENDA_LAZO_DESTINO -> {
                if (license.ordinal() < TrainerLicense.ORO.ordinal()) {
                    yield AccessDecision.deny("Requiere Licencia Oro (6 medallas y 50% Pokédex).");
                }
                yield AccessDecision.allow();
            }
            case EXPEDICION_LEGENDARIA -> {
                if (license.ordinal() < TrainerLicense.MAESTRO.ordinal()) {
                    yield AccessDecision.deny("Las Expediciones Legendarias requieren Licencia Maestro (8 medallas y 70% Pokédex).");
                }
                yield AccessDecision.allow();
            }
            case SANTUARIO_MITICO -> {
                if (license.ordinal() < TrainerLicense.CAMPEON.ordinal()) {
                    yield AccessDecision.deny("Los Santuarios Míticos requieren Licencia Campeón.");
                }
                yield AccessDecision.allow();
            }
            case GTS_PUBLICACION -> {
                int maxListings = maxGtsListingsForRank(rank);
                yield AccessDecision.allow();
            }
            default -> AccessDecision.allow();
        };
    }

    public static int maxGtsListingsForRank(Tablist.Rank rank) {
        var cfg = ProgressionBalanceConfig.get().gts;
        if (rank == null) return cfg.f2pMaxListings;
        if (rank.escalon >= Tablist.Rank.LEYENDA.escalon) return cfg.leyendaMaxListings;
        if (rank.escalon >= Tablist.Rank.MAESTRO.escalon) return cfg.maestroMaxListings;
        if (rank.escalon >= Tablist.Rank.CAMPEON.escalon) return cfg.campeonMaxListings;
        if (rank.escalon >= Tablist.Rank.ELITE.escalon) return cfg.eliteMaxListings;
        return cfg.f2pMaxListings;
    }
}

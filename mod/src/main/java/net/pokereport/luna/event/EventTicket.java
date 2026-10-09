package net.pokereport.luna.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Billete de Evento de Temporada (EventTicket).
 *
 * <p>Reglas estrictas de diseño:
 * <ul>
 *   <li>LIGADO (BOUND): asociado permanentemente al UUID del jugador.</li>
 *   <li>NO vendible en tiendas ni GTS.</li>
 *   <li>NO transferible entre jugadores.</li>
 *   <li>Consumo idempotente con registro de timestamp y motivo.</li>
 *   <li>Vencimiento configurable por temporada o timestamp.</li>
 * </ul>
 */
public final class EventTicket {

    private final UUID ticketId;
    private final UUID ownerUuid;
    private final String eventId;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private boolean consumed;
    private Instant consumedAt;
    private String consumedReason;

    public EventTicket(UUID ticketId, UUID ownerUuid, String eventId, Instant issuedAt, Instant expiresAt) {
        this.ticketId = Objects.requireNonNull(ticketId, "ticketId");
        this.ownerUuid = Objects.requireNonNull(ownerUuid, "ownerUuid");
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.consumed = false;
        this.consumedAt = null;
        this.consumedReason = null;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isBound() {
        return true;
    }

    public boolean canTrade() {
        return false;
    }

    public boolean canSell() {
        return false;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public String getConsumedReason() {
        return consumedReason;
    }

    public boolean isExpired() {
        return isExpired(Instant.now());
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isValid() {
        return isValid(Instant.now());
    }

    public boolean isValid(Instant now) {
        return !consumed && !isExpired(now);
    }

    /**
     * Consume el billete de forma idempotente.
     *
     * @param reason Motivo o contexto del consumo (ej. "dungeon_entry", "boss_summon")
     * @param now    Momento del consumo
     * @return {@code true} si el billete era válido y se consumió; {@code false} si ya estaba consumido o vencido
     */
    public synchronized boolean consume(String reason, Instant now) {
        if (!isValid(now)) {
            return false;
        }
        this.consumed = true;
        this.consumedAt = now;
        this.consumedReason = reason != null ? reason : "consumo_evento";
        return true;
    }

    public synchronized boolean consume(String reason) {
        return consume(reason, Instant.now());
    }
}

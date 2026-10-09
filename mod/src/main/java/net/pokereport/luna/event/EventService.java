package net.pokereport.luna.event;

import net.pokereport.luna.LunaEternal;
import net.pokereport.luna.economy.Currency;
import net.pokereport.luna.progression.ProgressionBalanceConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servicio de Gestión de Eventos y Billetes de Temporada.
 *
 * <p>Controla la emisión y validación de {@link EventTicket}, el cobro en divisas
 * (PokéDólares o LunaCoins) y el consumo idempotente.
 */
public final class EventService {

    private final Map<UUID, List<EventTicket>> playerTickets = new ConcurrentHashMap<>();

    public record PurchaseResult(boolean success, String message, EventTicket ticket) {}
    public record ConsumeResult(boolean success, String message, EventTicket ticket) {}

    public EventService() {}

    /**
     * Intenta adquirir un billete de evento para el jugador especificado.
     *
     * @param playerUuid  UUID del jugador
     * @param playerId    ID de base de datos del jugador
     * @param currency    Moneda a utilizar (POKEDOLLARS o LUNACOINS)
     * @param duration    Duración de validez del billete (ej. 7 días)
     * @return Resultado de la compra
     */
    public PurchaseResult purchaseTicket(UUID playerUuid, long playerId, Currency currency, Duration duration) {
        var cfg = ProgressionBalanceConfig.get().events;
        if (!cfg.active) {
            return new PurchaseResult(false, "No hay ningún evento activo en este momento.", null);
        }

        String eventId = cfg.activeEventId;
        List<EventTicket> tickets = playerTickets.computeIfAbsent(playerUuid, k -> new CopyOnWriteArrayList<>());

        long validCount = tickets.stream()
                .filter(t -> t.getEventId().equalsIgnoreCase(eventId) && t.isValid())
                .count();

        if (validCount >= cfg.maxTicketsPerPlayer) {
            return new PurchaseResult(false, "Ya has alcanzado el límite máximo de billetes activos para este evento.", null);
        }

        long cost = (currency == Currency.REPORTCOIN) ? cfg.ticketCostLunaCoins : cfg.ticketCostPokedollars;

        try {
            if (LunaEternal.economy() != null) {
                String idempotencyKey = "event_ticket_" + eventId + "_" + playerUuid + "_" + System.currentTimeMillis();
                LunaEternal.economy().debit(playerId, currency, cost, "event_ticket_purchase", idempotencyKey);
            }
        } catch (Exception e) {
            return new PurchaseResult(false, "Saldo insuficiente o error al procesar el pago (" + e.getMessage() + ").", null);
        }

        Instant now = Instant.now();
        Instant expires = (duration != null && !duration.isZero()) ? now.plus(duration) : now.plus(Duration.ofDays(7));
        EventTicket ticket = new EventTicket(UUID.randomUUID(), playerUuid, eventId, now, expires);
        tickets.add(ticket);

        return new PurchaseResult(true, "Billete adquirido con éxito para el evento: " + eventId, ticket);
    }

    /**
     * Consume un billete válido de forma idempotente para participar en una actividad de evento.
     *
     * @param playerUuid UUID del jugador
     * @param eventId    ID del evento requerido
     * @param reason     Contexto del consumo (ej. "raid_boss_luna", "dungeon_entry")
     * @return Resultado del consumo
     */
    public ConsumeResult consumeTicket(UUID playerUuid, String eventId, String reason) {
        List<EventTicket> tickets = playerTickets.get(playerUuid);
        if (tickets == null || tickets.isEmpty()) {
            return new ConsumeResult(false, "No posees ningún billete de evento.", null);
        }

        Instant now = Instant.now();
        synchronized (tickets) {
            for (EventTicket ticket : tickets) {
                if (ticket.getEventId().equalsIgnoreCase(eventId) && ticket.isValid(now)) {
                    boolean ok = ticket.consume(reason, now);
                    if (ok) {
                        return new ConsumeResult(true, "Billete consumido correctamente para: " + reason, ticket);
                    }
                }
            }
        }

        return new ConsumeResult(false, "No se encontró ningún billete activo y sin usar para este evento.", null);
    }

    /**
     * Verifica si el jugador posee al menos un billete válido y vigente para el evento.
     */
    public boolean hasValidTicket(UUID playerUuid, String eventId) {
        List<EventTicket> tickets = playerTickets.get(playerUuid);
        if (tickets == null || tickets.isEmpty()) return false;
        Instant now = Instant.now();
        return tickets.stream().anyMatch(t -> t.getEventId().equalsIgnoreCase(eventId) && t.isValid(now));
    }

    /**
     * Retorna la lista de todos los billetes registrados para el jugador.
     */
    public List<EventTicket> getTickets(UUID playerUuid) {
        List<EventTicket> tickets = playerTickets.get(playerUuid);
        return tickets != null ? Collections.unmodifiableList(tickets) : Collections.emptyList();
    }

    /**
     * Registra manualmente un billete (utilizado para pruebas unitarias o asignaciones administrativas).
     */
    public void registerTicket(EventTicket ticket) {
        if (ticket != null) {
            playerTickets.computeIfAbsent(ticket.getOwnerUuid(), k -> new CopyOnWriteArrayList<>()).add(ticket);
        }
    }

    public void clear() {
        playerTickets.clear();
    }
}

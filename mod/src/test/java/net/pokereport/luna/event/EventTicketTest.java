package net.pokereport.luna.event;

import net.pokereport.luna.economy.Currency;
import net.pokereport.luna.progression.ProgressionBalanceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EventTicketTest {

    private UUID playerUuid;

    @BeforeEach
    void setUp() {
        playerUuid = UUID.randomUUID();
        ProgressionBalanceConfig.Data data = new ProgressionBalanceConfig.Data();
        data.events.active = true;
        data.events.activeEventId = "halloween_2026";
        data.events.ticketCostPokedollars = 25_000L;
        data.events.ticketCostLunaCoins = 500L;
        data.events.maxTicketsPerPlayer = 1;
        ProgressionBalanceConfig.set(data);
    }

    @Test
    @DisplayName("Propiedades de billete ligado (BOUND) y no transferible")
    void testTicketBoundProperties() {
        Instant now = Instant.now();
        EventTicket ticket = new EventTicket(UUID.randomUUID(), playerUuid, "halloween_2026", now, now.plus(Duration.ofDays(7)));

        assertTrue(ticket.isBound(), "El billete debe estar permanentemente ligado al jugador");
        assertFalse(ticket.canTrade(), "El billete NO puede ser transferido");
        assertFalse(ticket.canSell(), "El billete NO puede ser vendido");
        assertFalse(ticket.isConsumed());
        assertTrue(ticket.isValid());
        assertEquals("halloween_2026", ticket.getEventId());
        assertEquals(playerUuid, ticket.getOwnerUuid());
    }

    @Test
    @DisplayName("Expiración temporal del billete de evento")
    void testTicketExpiration() {
        Instant now = Instant.now();
        Instant past = now.minus(Duration.ofHours(1));
        EventTicket expiredTicket = new EventTicket(UUID.randomUUID(), playerUuid, "halloween_2026", past.minus(Duration.ofDays(1)), past);

        assertTrue(expiredTicket.isExpired(now));
        assertFalse(expiredTicket.isValid(now));
        assertFalse(expiredTicket.consume("dungeon_entry", now), "No debe permitir consumir un billete vencido");
    }

    @Test
    @DisplayName("Consumo idempotente de billete")
    void testIdempotentTicketConsumption() {
        Instant now = Instant.now();
        EventTicket ticket = new EventTicket(UUID.randomUUID(), playerUuid, "halloween_2026", now, now.plus(Duration.ofDays(1)));

        // Primer consumo exitoso
        boolean firstConsume = ticket.consume("boss_entry", now);
        assertTrue(firstConsume);
        assertTrue(ticket.isConsumed());
        assertEquals("boss_entry", ticket.getConsumedReason());
        assertEquals(now, ticket.getConsumedAt());
        assertFalse(ticket.isValid(now));

        // Segundo consumo debe fallar (idempotencia)
        boolean secondConsume = ticket.consume("boss_entry", now);
        assertFalse(secondConsume, "El billete no puede consumirse dos veces");
    }

    @Test
    @DisplayName("Flujo de compra y consumo en EventService")
    void testEventServiceFlow() {
        EventService service = new EventService();

        // 1. Compra exitosa
        var purchaseRes = service.purchaseTicket(playerUuid, 1001L, Currency.POKEDOLLAR, Duration.ofDays(3));
        assertTrue(purchaseRes.success());
        assertNotNull(purchaseRes.ticket());
        assertTrue(service.hasValidTicket(playerUuid, "halloween_2026"));

        // 2. Intento de compra excediendo límite de 1 billete
        var secondPurchase = service.purchaseTicket(playerUuid, 1001L, Currency.POKEDOLLAR, Duration.ofDays(3));
        assertFalse(secondPurchase.success(), "Debe bloquear la compra si se supera maxTicketsPerPlayer");

        // 3. Consumo exitoso
        var consumeRes = service.consumeTicket(playerUuid, "halloween_2026", "raid_challenge");
        assertTrue(consumeRes.success());
        assertFalse(service.hasValidTicket(playerUuid, "halloween_2026"), "Ya no debe tener billete válido tras consumirse");

        // 4. Intento de re-consumo falla
        var reConsumeRes = service.consumeTicket(playerUuid, "halloween_2026", "raid_challenge");
        assertFalse(reConsumeRes.success(), "No debe permitir consumir si no hay billetes válidos");
    }
}

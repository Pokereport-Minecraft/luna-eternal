# PokeReport Network — auditoría forense de economía

Fecha: 2026-10-01  
Rama: `codex/economy-launch-readiness`, basada en `remediacion/luna-eternal`  
Estado de los datos: **PREAPERTURA / PRUEBAS**

## Regla de interpretación

El servidor todavía no abrió al público. Los saldos, ventas, premios y órdenes
actuales fueron generados durante construcción y QA. No son una muestra de
jugadores reales y no se usan para fijar precios ni activar ajustes automáticos.
Solo sirven para comprobar invariantes, descubrir exploits y validar telemetría.
La calibración real empezará después del lanzamiento, con observación mínima y
cambios pequeños, reversibles y versionados.

## Arquitectura actual

- `POKEDOLLAR` es la moneda normal visible como **Plata** y transferible.
- `REPORTCOIN` son **LunaCoins**, premium y no transferibles.
- `MARK` fue retirada por V049 y solo debe existir en el histórico.
- No existe conversión Plata/LunaCoins; debe seguir siendo imposible.
- `ledger_entry` es append-only y `player_economy` es la caché de saldo.
- Los movimientos normales usan transacción, idempotencia y `FOR UPDATE`.

Datos de prueba observados: 24 cuentas, 637 asientos, 243.093 Plata y 115.070
LunaCoins en carteras, con discrepancia cero contra el ledger. En 30 días se
registraron +265.629 y -36.157 Plata, pero +100.000 fue `admin_grant` y la mayor
vía, `shop_sell` (+115.200 en 335 operaciones), provino de ventas repetidas de
una cuenta operadora. Esto prueba contaminación de QA, no inflación pública.

## Inventario de grifos de Plata

| Sistema | Magnitud/cadencia del código | Riesgo |
|---|---:|---|
| Recompra NPC | 448 entradas al 10%, sin límite | **Crítico**: bayas/muebles repetibles |
| Misiones únicas | 28.990 total/jugador | Finito |
| Misiones diarias | hasta 2.706/día | Alto combinado |
| Cazas | hasta 10.868/día más objetos | Alto; falta tasa real |
| Torre | 1.000 cada 5 rondas, incluso tras 100 | Alto si es repetible |
| Lunita | 1.000 cada 12 h/jugador | Fuerte; vigilar multicuentas |
| ¿Quién es este Pokémon? | 500 cada 2 h global | 6.000/día global |
| Pokédex nueva especie | 5/especie | Finito y bajo |
| Trabajos | 4.100 por oficio pagado | Finito; progreso explotable |
| Cajas | premios 500/2.000, llave por hora | Falta expectativa ponderada |
| Administración | sin límite | Excluir de KPI y exigir actor/motivo |

## Sumideros de Plata

| Sistema | Regla |
|---|---:|
| Tienda NPC | 50–150.000 según artículo |
| Fundación de clan | 5.000 |
| Buhonero | 10.000/24 h |
| Santuario | 5.000/día |
| Cartas | 900/24 h |
| GTS | 1% publicación + impuesto 5/8/12/18% |
| Mercado de objetos | impuesto progresivo de venta |

Homes y pwarps son gratuitos. Eso puede ser correcto: una función social básica
no necesita coste solo para destruir moneda.

## Contabilidad correcta

No son faucets ni sinks: transferencias, compra/venta entre jugadores, reserva y
devolución de órdenes, aportes/retiros de clan y reembolsos. Solo tasas e
impuestos destruyen moneda. La masa completa es:

`carteras + reservas de compra + tesoros de clan + otros escrows`

El informe anterior solo medía carteras y clasificaba por signo, creando falsos
grifos/sumideros. Debe usarse clasificación explícita y snapshots por componente.

## Mercado, GTS y tienda NPC

Solo hay cuatro ventas GTS de prueba por 5.312 Plata y ningún cruce real de
órdenes. No alcanza para precio histórico, liquidez ni outliers. Custodia,
prioridad precio-tiempo, anti-autocompra, impuestos y entrega idempotente están
bien diseñados. Falta proteger multiplicaciones, separar volumen de emisión,
exigir muestra mínima para precio sugerido y detectar extremos sin castigo
automático.

El catálogo tiene 641 entradas. No hay arbitraje directo compra→venta, pero 71
bayas recompradas a 20 y 367 muebles recomprados a 12 convierten producción o
recetas repetibles en Plata ilimitada. No existen cuota, presión de oferta ni
telemetría por artículo/cantidad. Los precios deben conservarse mientras se mide;
después solo los farmables podrán ajustarse diariamente dentro de bandas. Ante
telemetría rota o volumen anómalo, el circuit breaker congela precios.

## Trabajos y defectos encontrados

Hay ocho trabajos. Minero, Pescador y Granjero pagan bonos finitos de 4.100 cada
uno; los demás progresan y desbloquean. Minero no registra bloques colocados: un
mineral con Silk Touch puede recolocarse para ganar XP.

1. `current + delta` no usa suma exacta y puede desbordar.
2. Varias rutas calculan `precio * cantidad` sin multiplicación exacta.
3. El ledger no guarda clase de flujo, artículo, cantidad, contraparte ni actor.
4. El “ingreso diario” incluye transferencias, reembolsos, escrow y admins.
5. La distribución mezcla testers y solo expone P50/P90/P99.
6. `play_ticks` está en cero; no se puede medir Plata/hora todavía.
7. Queda configuración inerte de CobbleDollars aunque el mod no está activo.
8. Comentarios de misiones aún describen Marcas pese a V049.

## Guardrails y salida

- discrepancia entre libro y saldos siempre cero;
- movimientos sin clasificación cero;
- KPIs sin testers/admins y sin transferencias/escrows;
- P10/P25/P50/P75/P90/P95/P99 y Plata/hora por sistema/perfil;
- ningún faucet recurrente domina la creación orgánica;
- controlador congelado hasta cumplir muestra mínima;
- ajustes como máximo diarios, pequeños, versionados y reversibles;
- snapshots horarios y alertas por descuadre, concentración y volumen NPC;
- simulaciones para 10/30/50/100/500/1.000 jugadores a 30/180 días.

La economía no puede certificarse como balanceada sin jugadores públicos. Sí
puede certificarse como **lista para medir** cuando pasan build y migraciones,
cuadra la masa completa, todo movimiento está clasificado, testers quedan fuera
de KPIs, la simulación es reproducible y el controlador sigue en observación.

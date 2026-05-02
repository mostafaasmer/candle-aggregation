# Candle Aggregation Service

A Spring Boot service that aggregates real-time bid/ask tick data from Kafka into OHLCV candles and exposes them through a TradingView-compatible `/history` endpoint.

## Project Overview

```
MarketDataSimulator
       │  publishes BidAskEvent (JSON) every 30 s
       ▼
  Kafka topic: bid-ask-events (3 partitions, keyed by symbol)
       │
       ▼
BidAskEventConsumer   (3 concurrent listener threads)
       │  mid = (bid + ask) / 2
       ▼
CandleAggregator      (ConcurrentHashMap per symbol:interval key)
       │  on window rollover → finalizes candle
       ▼
CandleStorageService  (ConcurrentSkipListMap per key, sorted by time)
       │
       ▼
GET /history?symbol=BTC-USD&interval=1m&from=<unix>&to=<unix>
```

### Simulated symbols

| Symbol   | Starting mid-price |
|----------|--------------------|
| BTC-USD  | $65,000            |
| ETH-USD  | $3,500             |
| SOL-USD  | $180               |

Ticks are generated every 30 seconds with a Gaussian random-walk (σ = 0.05 %) and a 0.01 % spread.

### Supported intervals

`1s` · `5s` · `1m` · `5m` · `15m` · `1h`

### API

```
GET /history
  ?symbol=BTC-USD
  &interval=1m
  &from=1620000000    (inclusive Unix seconds)
  &to=1620003600      (inclusive Unix seconds)
```

**Success response**

```json
{
  "s": "ok",
  "t": [1620000000, 1620000060],
  "o": [29500.0, 29505.0],
  "h": [29510.0, 29515.0],
  "l": [29490.0, 29495.0],
  "c": [29505.0, 29510.0],
  "v": [10, 5]
}
```

The currently open (not yet finalized) candle is appended to the arrays when its window start falls within `[from, to]`.

**Error response** (unknown interval, e.g. `interval=3m`)

```json
{ "s": "error", "errmsg": "Unknown interval: '3m'. Valid values: 1s, 5s, 1m, 5m, 15m, 1h" }
```

---

## Assumptions and Trade-offs

| Area | Decision | Reason |
|---|---|---|
| **Storage** | Pure in-memory (`ConcurrentSkipListMap`) | Simplicity; data is lost on restart. A production system would use a time-series DB (e.g. InfluxDB, TimescaleDB). |
| **Candle price** | Mid-price `(bid + ask) / 2` | Standard approach when only bid/ask is available rather than last-trade price. |
| **Volume** | Tick count (number of bid/ask events per window) | No notional size is provided by the simulator. A real feed would supply trade size. |
| **Concurrency** | `ConcurrentHashMap.compute()` for per-key atomic updates | Gives lock-free, per-symbol isolation without a global lock. `LiveCandle` is only mutated inside `compute()`. |
| **Kafka partitioning** | Symbol used as message key | Guarantees that all ticks for the same symbol land on the same partition, preserving order and enabling stateful consumers without cross-partition coordination. |
| **Live candle** | Appended to `/history` response | Gives callers the partial candle for the current window without waiting for the next tick to finalize it. |
| **Kafka startup** | `spring-boot-docker-compose` + healthcheck | Docker Compose is started automatically before the Spring context; healthcheck (`start_period: 30s`, 6 retries) ensures Kafka is ready before the listener registers. |
| **No persistence across restarts** | Candles are lost when the JVM exits | Acceptable for a demo; production would replay Kafka offsets or use a persistent store. |
| **No authentication** | `/history` is open | Out of scope for this exercise. |

---

## Prerequisites

- Java 17+
- Maven 3.9+ (or use the included `./mvnw` wrapper)
- Docker Desktop (running) — started automatically by `spring-boot-docker-compose`

---

## Running the Application

```bash
# Start the app (Docker Compose launches Kafka automatically)
./mvnw spring-boot:run
```

Kafka UI is available at **http://localhost:8090** once the broker is healthy.

Sample request after a few ticks have been produced:

```bash
curl "http://localhost:8080/history?symbol=BTC-USD&interval=1m&from=0&to=9999999999"
```

---

## Running Tests

```bash
# Run all tests
./mvnw test
```

The test suite uses **embedded Kafka** (`@EmbeddedKafka`) — no Docker or external broker is required.

### Test coverage

| Test class | What it covers |
|---|---|
| `CandleAggregationApplicationTests` | Spring context loads with embedded Kafka |
| `CandleAggregatorTest` | OHLCV correctness, window rollover, per-symbol isolation, interval alignment |
| `CandleStorageServiceTest` | Store/retrieve, range queries, symbol/interval isolation, ascending sort |
| `HistoryControllerTest` | HTTP 200/400 responses, live candle inclusion/exclusion, empty data sets |

```bash
# Run a single test class
./mvnw test -Dtest=CandleAggregatorTest

# Run with verbose output
./mvnw test -Dsurefire.useFile=false
```

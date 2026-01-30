# lot-o-odds

A Clojure project for analyzing lottery odds and comparing different lottery games.

## Supported Games

- **Lucky for Life** - $2 tickets, 42.56% house edge
- **Mega Millions** - $5 tickets (new format), 61.90% house edge

## Lucky for Life - Prize Tiers and Odds

| Match | Prize | Odds |
|-------|-------|------|
| 5 + LB | $1,000 a day for life* | 1 in 30,821,472 |
| 5 of 5 | $25,000 a year for life* | 1 in 1,813,028 |
| 4 + LB | $5,000* | 1 in 143,356 |
| 4 of 5 | $200 | 1 in 8,433 |
| 3 + LB | $150 | 1 in 3,413 |
| 3 of 5 | $20 | 1 in 201 |
| 2 + LB | $25 | 1 in 250 |
| 2 | $3 | 1 in 15 |
| 1 + LB | $6 | 1 in 50 |
| LB | $4 | 1 in 32 |

*Note: Some prizes may have cash value options

## Usage

Run the REPL:
```bash
clojure -M:repl
```

Run tests:
```bash
clojure -M:test
```

Run simulations:
```bash
# Lucky for Life (default: $20 initial, 100 simulations)
clojure -M:simulate
clojure -M:simulate 50 500

# Mega Millions (default: $25 initial, 100 simulations, $50M jackpot)
clojure -M:mm-simulate
clojure -M:mm-simulate 100 500 100000000
```

Compare to casino games:
```bash
clojure -M:compare-games
```

## Structure

- `src/jmshelby/lot_o_odds/` - Source code
- `test/jmshelby/lot_o_odds/` - Tests

## License

Copyright © 2026

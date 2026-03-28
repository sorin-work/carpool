# carpool

Carpool coordination for an office or a school: shared trips, a fair driver rotation and fuel cost sharing.

## Quick start

You need Docker with the compose plugin.

```bash
docker compose up -d --build
```

Open <http://localhost:4419> and sign in with the demo account:

| Email                | Password        |
| -------------------- | --------------- |
| `demo@carpool.local` | `demo-password` |

The demo account belongs to two groups ("Elm St school run" and "Northside office pool") with a
few months of rides behind them. Set `SEED_DEMO=false` to start with an empty database.

## Local development

The API needs Java 21; the web app needs Node 22.

```bash
docker compose up -d db          # PostgreSQL on localhost:5442

cd api
./mvnw spring-boot:run           # http://localhost:8080

cd web
npm install
npm run dev                      # http://localhost:4419, proxies /api to :8080
```

## Tests

```bash
cd api && ./mvnw test            # JUnit 5, against an in-memory H2 database
cd web && npm test               # Vitest
```

## License

[MIT](LICENSE)

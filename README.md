# carpool

Carpool coordination for an office or a school: shared trips, a fair driver rotation and fuel cost sharing.

A group (the parents of one school run, a team that commutes to the same office) sets up its
recurring trips once, for example "Mon–Fri 07:45 from Elm St to Riverside Primary". Every day the
trip runs becomes a ride. Members with a car offer to drive or let the rotation pick a driver,
riders ask for a seat, and at the end of the month the app works out who owes whom for fuel.

## Features

- **Groups** with an owner, admins and members. People join with an eight-letter invite code or
  link; admins can rotate the code, promote members or remove them.
- **Recurring trips** with origin, destination, departure time, weekdays, distance and an
  optional first and last day. Trips can be paused, edited (planned rides move with them),
  cancelled for a range of days such as a school holiday, or copied as the return journey.
- **Weekly schedule** in the group's time zone, with the driver, free seats, waitlist and who is
  away that week. Rides can be added to a calendar app as an `.ics` file.
- **Seats** are handed out first come, first served. Without a driver a request waits; when the
  car is full the next riders join a waitlist that moves up when someone cancels.
- **Fair driver rotation** that fills rides without a driver: whoever has driven least over the
  last eight weeks goes first, ties go to whoever drove longest ago, people who are away are
  skipped and nobody gets two rides on one day if someone else is free. Admins preview it before
  assigning.
- **Swaps**: a driver asks another member to take a ride, optionally taking one of theirs in
  return.
- **Cancellations** notify everyone on the ride, with the reason. Drivers get a reminder the day
  before and can leave a note for their riders.
- **Driving stats** per member: drives, kilometres, rides taken, passengers carried, the share of
  their trips they drove and what they are lined up for next. Ride history exports to CSV.
- **Fuel cost sharing**: each group sets a cost per kilometre; every ride's cost is split between
  everyone in the car and the riders owe their share to the driver. Monthly balances come with a
  short list of payments to settle up, and payments can be recorded.
- **In-app notifications** with an unread badge.

## Stack

| Part     | Tech                                                                          |
| -------- | ----------------------------------------------------------------------------- |
| API      | Java 21, Spring Boot 3.5 (Web, Validation, Data JPA, Security), JWT (jjwt)    |
| Database | PostgreSQL 17, Flyway migrations                                              |
| Web      | Vue 3, TypeScript, Vite, Vue Router, Pinia                                    |
| Tests    | JUnit 5, AssertJ, MockMvc against H2 in PostgreSQL mode; Vitest, Vue Test Utils |
| Ops      | Docker Compose, multi-stage Dockerfiles, nginx, GitHub Actions                |

```
browser ──> web (nginx :80) ──/api──> api (Spring Boot :8080) ──> db (Postgres :5432)
             static Vue app            JWT auth, Flyway on start
```

The browser only talks to the web container: nginx serves the built app and forwards `/api` to
the API. In development the Vite dev server does the same.

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
few months of rides behind them, a swap request waiting for an answer and an upcoming absence.
The other demo members (`mihai@carpool.local`, `elena@carpool.local`, ...) use the same password.
Set `SEED_DEMO=false` to start with an empty database.

```bash
docker compose down        # stop
docker compose down -v     # stop and delete the database
```

## Configuration

Compose reads these from the environment or a `.env` file (see [.env.example](.env.example)).

| Variable            | Default                                          | Used by | Meaning |
| ------------------- | ------------------------------------------------ | ------- | ------- |
| `WEB_PORT`          | `4419`                                           | web     | Port the app is published on |
| `DB_PORT`           | `5442`                                           | db      | Port PostgreSQL is published on |
| `POSTGRES_USER`     | `carpool`                                        | db, api | Database user |
| `POSTGRES_PASSWORD` | `carpool`                                        | db, api | Database password |
| `POSTGRES_DB`       | `carpool`                                        | db, api | Database name |
| `JWT_SECRET`        | `local-development-secret-change-me-0123456789`  | api     | Signs login tokens; at least 32 characters. Change it for anything but local use |
| `JWT_TTL`           | `7d`                                             | api     | How long a login lasts |
| `SEED_DEMO`         | `true` in compose, `false` otherwise             | api     | Create the demo account on first start |

Running the API outside Docker it also reads `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`
and `PORT` (default `8080`).

## Local development

The API needs Java 21; the web app needs Node 22.

```bash
docker compose up -d db          # PostgreSQL on localhost:5442

cd api
./mvnw spring-boot:run           # http://localhost:8080
SEED_DEMO=true ./mvnw spring-boot:run   # ... with the demo data

cd web
npm install
npm run dev                      # http://localhost:4419, proxies /api to :8080
```

`API_URL` points the dev server's proxy somewhere else.

## Tests

```bash
cd api
./mvnw verify                    # unit tests and API tests (MockMvc, H2 in PostgreSQL mode)

cd web
npm run lint
npm run format:check
npm run typecheck
npm test                         # Vitest with jsdom
npm run build
```

API tests run every request through the real application with Flyway migrations applied to H2,
each test in a transaction that is rolled back, with the clock pinned to Monday 2 March 2026. CI
runs all of the above plus a Docker build on every push.

## Project layout

```
api/                      Spring Boot service
  src/main/java/com/carpool/
    user/  security/      accounts, login, JWT
    group/                groups, members, roles, invite codes
    trip/                 recurring trips
    ride/                 rides, schedule, cancellation, reminders
    booking/              seat requests, seat allocation and waitlist
    rotation/             fair rotation and absences
    swap/                 swap requests
    notification/         in-app notifications
    stats/  cost/         driving stats, fuel cost split and settlements
    calendar/  export/    .ics and CSV downloads
    seed/                 demo data
  src/main/resources/db/migration/   Flyway migrations
web/                      Vue 3 app
  src/api/                typed API client
  src/stores/             auth, current group, notification badge
  src/views/              pages; views/group/ holds the group tabs
  src/components/         shared components
  src/utils/              dates, money, initials, notification markup
docs/api.md               API notes
```

## License

[MIT](LICENSE)

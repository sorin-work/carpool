# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [1.0.0] - 2026-11-01

First release.

### Groups and members

- Accounts with email and password, JWT sessions, password change and a lock-out after five
  wrong passwords.
- Carpool groups with an owner, admins and members; invite codes and invite links with a preview
  of the group; promote, remove, hand over ownership, leave or delete a group.
- Each member says whether they drive, how many seats their car has and where to be picked up.

### Trips and rides

- Recurring trips with weekdays, departure time, distance and first and last day, in the group's
  time zone. Pause, edit (planned rides move with the trip), copy as the return journey, or
  cancel a range of days for a holiday.
- Weekly schedule with drivers, free seats, waitlists, notes from the driver and who is away.
- Seat requests handed out in request order with a waitlist that moves up on cancellation.
- Drivers take rides, step down, cancel with a reason or ask someone to swap, optionally taking
  one of their rides in return. Admins can assign drivers and reinstate cancelled rides.
- A fair rotation that fills rides without a driver based on the last eight weeks of driving,
  skipping absences and avoiding two rides a day for one person.
- My rides across all groups, an `.ics` calendar file and a CSV export of the ride history.

### Costs and stats

- A cost per kilometre per group, split between everyone in the car; monthly balances, a short
  settle-up list and recorded payments.
- Driving stats per member with each member's ride history and upcoming drives.

### Notifications

- In-app notifications for cancellations, seat confirmations, swaps, driver changes and a
  reminder for drivers the day before, with an unread badge. Old notifications are removed after
  90 days.

### Operations

- Docker Compose setup with PostgreSQL, the API and the web app behind nginx, health checks and a
  demo account with sample data.
- GitHub Actions running the API tests, web lint, type check, tests and build, and a Docker build.

[1.0.0]: https://github.com/sorin-work/carpool/releases/tag/v1.0.0

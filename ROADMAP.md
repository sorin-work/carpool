# Roadmap

Things I might add to carpool. The app covers the whole loop today (groups, trips, seats, the
rotation, swaps, fuel costs and settling up), so nothing here is promised or scheduled. These
are ideas, written down with enough detail that I (or anyone else) can pick one up later without
starting from zero. Items may change, move between groups or be dropped.

What the app does today is in the [README](README.md), the [API notes](docs/api.md) and
[how carpool works](docs/architecture.md).

- **Next**: small or well understood. I would start with these.
- **Later**: worth doing, but bigger or waiting on a decision.
- **Maybe**: not sure yet that they are worth the extra complexity.

Sizes are rough: **S** is an evening or two, **M** a few evenings up to a couple of weekends,
**L** several weeks of spare time.

Each item is meant to fit in one pull request and to be covered by the tests. The API has two
levels: plain unit tests for the pure rule classes (`SeatAllocator`, `FairRotation`,
`CostSplitter`, `SettlementPlanner`, `RideGenerator`, `DaysOfWeek`) and MockMvc tests that send
real requests through the application against H2 in PostgreSQL mode, with Flyway applied and the
clock pinned to Monday 2 March 2026 (`support/IntegrationTest`, `support/MutableClock`). So a
rule worth testing on its own (who drives, who gets a seat, what someone owes) belongs in a pure
class next to those, not inside a service. The web app has Vitest specs with Vue Test Utils in
`web/src/__tests__`; logic there belongs in `web/src/utils`. Most items add columns, and each
brings its own Flyway migration (the next free number is `V16`). Java file names below are
relative to `api/src/main/java/com/carpool/`, others start with `api/`, `web/` or `docs/`.

When an item ships, its row in the overview says `Shipped`, its section moves under a
`## Shipped` heading at the end with one paragraph on what was built and what was left out, and
the same pull request updates the README, the notes under `docs/` and an entry under
`## [Unreleased]` in the [CHANGELOG](CHANGELOG.md) (the heading is added by the first item that
needs it).

"Depends on" lists real prerequisites only. Items that merely touch the same file are not listed
there; the bigger overlaps are mentioned in the items.

## Overview

| #   | Item                                             | Group | Area          | Size | Depends on |
| --- | ------------------------------------------------ | ----- | ------------- | ---- | ---------- |
| 1   | Standing seat requests                           | Next  | seats         | M    | -          |
| 2   | Absences free up drives and seats                | Next  | rotation      | S    | -          |
| 3   | Rides that follow group and car changes          | Next  | rides         | S    | -          |
| 4   | Costs fixed when a ride takes place              | Next  | costs         | S    | -          |
| 5   | History that survives leaving and deleting       | Next  | groups        | M    | -          |
| 6   | Two people changing one ride at once             | Later | rides         | S    | -          |
| 7   | Correcting rides after they left                 | Later | rides         | M    | -          |
| 8   | Running balances and payment corrections         | Later | costs         | M    | 5          |
| 9   | Children and other riders without an account     | Later | seats         | L    | -          |
| 10  | Pickup stops and their order                     | Later | trips         | M    | -          |
| 11  | Picked up and dropped off                        | Later | rides         | M    | -          |
| 12  | Email notifications                              | Later | notifications | M    | -          |
| 13  | Password reset                                   | Later | accounts      | S    | 12         |
| 14  | Sessions that can be ended                       | Later | accounts      | S    | -          |
| 15  | Calendar subscription                            | Later | calendar      | S    | -          |
| 16  | Reminders for riders and for rides nobody drives | Later | notifications | S    | -          |
| 17  | Open swaps and swap housekeeping                 | Later | swaps         | M    | -          |
| 18  | Weekdays a driver can do                         | Later | rotation      | S    | -          |
| 19  | Fairness that counts riding                      | Later | rotation      | M    | -          |
| 20  | A rotation that runs ahead                       | Later | rotation      | M    | -          |
| 21  | Join approval and expiring invites               | Later | groups        | M    | -          |
| 22  | Group activity log                               | Later | ops           | M    | -          |
| 23  | Deleting an account and taking the data          | Later | accounts      | M    | 5          |
| 24  | Hardening for use outside a laptop               | Later | security      | S    | -          |
| 25  | Backups, restore and upgrades                    | Later | ops           | S    | -          |
| 26  | Tests against PostgreSQL                         | Later | tests         | M    | -          |
| 27  | Accessibility pass                               | Later | ui            | M    | -          |
| 28  | Metrics and logs                                 | Later | ops           | S    | -          |
| 29  | Messages on a ride                               | Later | rides         | M    | -          |
| 30  | More than one car                                | Later | members       | M    | -          |
| 31  | Translations                                     | Maybe | ui            | M    | -          |
| 32  | Installable app with the week offline            | Maybe | ui            | M    | -          |
| 33  | Push notifications                               | Maybe | notifications | M    | 32         |
| 34  | Distances from a self-hosted router              | Maybe | trips         | M    | -          |
| 35  | Holidays from a calendar file                    | Maybe | trips         | S    | -          |
| 36  | More than one API instance                       | Maybe | ops           | M    | -          |
| 37  | Faster schedule reads in big groups              | Maybe | performance   | M    | -          |

## Next

### 1. Standing seat requests

A rider asks for a seat one ride at a time (`POST /rides/{id}/seat`, `BookingService.request`).
A child who goes to school every weekday needs ten clicks every fortnight, and a missed click
means a parent at the gate with nobody to drive the child. Every carpool app I looked at has some
form of "I ride this every Monday and Wednesday"; it is the most obvious gap here.

- Data: a new `standing_requests` table with `member_id`, `trip_id`, `days_of_week` (the same
  bitmask as `trips.days_of_week`, read through `DaysOfWeek`), `starts_on`, an optional `ends_on`
  and `created_at`. One row per member and trip.
- Where the bookings come from: `RideService.materialize` already creates every ride the first
  time a date range is read. After it stores a new ride it also creates a `SeatBooking` for every
  standing request that covers that weekday and date, and then runs `SeatPlanner.rebalance`.
  Creating a standing request also books the rides already stored for the coming weeks.
- A pure `StandingSeats.bookingsFor(ride, requests, absences)` decides who gets a booking, so the
  date rules are unit tested without the database.
- Tricky: one ride cancelled by the rider must stay cancelled. If the rider drops one Monday,
  the booking row is `CANCELLED` and the standing request must not bring it back the next time
  someone opens the schedule. Only rides without any booking row for that member get one.
- Tricky: queue order. `SeatAllocator` hands out seats in booking order, and the order is
  `created_at`. A standing booking created when someone happens to read the schedule would land
  behind one-off requests made earlier, or ahead of later ones, by accident. I lean towards giving
  standing bookings the request's own `created_at`, so a regular rider is ahead of a one-off
  request, and saying so in `docs/architecture.md`.
- Tricky: notifications. `SeatPlanner.rebalance` sends "You have a seat on ..." for every booking
  that gains a seat. For standing bookings that is one notification per ride and becomes noise.
  Skip it for standing bookings unless the rider was on the waitlist first.
- Editing the trip (`RideService.replan`) can drop weekdays; the standing request then simply
  matches fewer rides. Pausing the trip leaves the request in place.
- Absences (item 2) should skip those days.
- UI: on the trips tab (`web/src/views/group/TripsPanel.vue`) a "Ride this regularly" form that
  reuses `DayPicker.vue`, and the rider's standing requests listed on their profile in the group.
- Tests: unit tests for `StandingSeats`; integration tests in a new
  `booking/StandingRequestTest.java`: bookings appear when the schedule is read, a cancelled ride
  stays cancelled, order against one-off requests, no notification flood, a request for someone
  else's trip is refused.

### 2. Absences free up drives and seats

`AbsenceService.add` only stores the dates. The rotation skips the member on those days, but
rides they already drive stay theirs, and seats they already hold stay taken. A parent who adds
"skiing, 10 to 14 March" is still down to drive on the 11th, and nobody is told until the driver
reminder goes to someone who is on a mountain.

- After saving the absence, `AbsenceService` materializes the range (`RideService.materialize`,
  the absence is at most 120 days) and, for every ride in it that has not left yet:
  - if the member drives it: clear the driver and rebalance, as `GroupService.handOverRides`
    already does when someone leaves, and tell the riders (`DRIVER_LEFT`);
  - if the member has an active booking: cancel it and rebalance, so the waitlist moves up.
- The response says what was released ("2 drives and 3 seats"), and the form in
  `components/AbsenceList.vue` shows a preview first. A `GET /groups/{id}/absences/preview` with
  the same dates avoids surprises.
- Tricky: deleting an absence does not give the rides back. Someone else may have taken them in
  the meantime. Say so in the UI rather than trying to restore.
- Tricky: an absence that starts today. Rides that already left must not change (`RideRules`
  already refuses those), but a ride later the same day should.
- Tricky: an admin adding an absence for someone else is not possible today (`add` always uses
  the caller). Leave it that way; the release logic should not care who added it.
- Tests: in `rotation/RotationControllerTest.java` or a new `AbsenceTest.java`: a drive released
  and riders told, a seat released and the waitlist moved up, departed rides untouched, the
  preview matches what the save does.

### 3. Rides that follow group and car changes

Two settings change without touching the rides they affect.

- The group's time zone (`GroupService.update`, `setZone`). Every stored ride keeps its old
  `departure_at` instant, so after a change from `Europe/Bucharest` to `Europe/London` the 07:45
  school run shows as 05:45. `RideService.replan(trip)` already moves planned rides to a trip's
  local time; calling it for every trip of the group after a zone change fixes the stored rides.
- A member's car (`GroupService.updateMyMembership`). Lowering `carSeats` from 4 to 2 leaves
  `seats_offered` at 4 on every ride they already drive, so four riders are confirmed in a
  two-seat car. Turning `canDrive` off leaves them as the driver of rides they can no longer do.
  - Fewer seats: lower `seats_offered` on their upcoming rides to the new number and rebalance,
    which moves the extra riders to the waitlist and keeps the earliest requests in.
  - No car: release their upcoming drives the way `handOverRides` does.
  - The profile form (`components/MembershipForm.vue`) warns before saving: "You drive 6 rides
    in the next weeks. They will need a new driver."
- Tricky: a time zone change across a date line could move a ride to another calendar date.
  `ride_date` is the local date and stays; only the instant moves. That is what replan does today
  and it is the right meaning: "the Monday ride" stays the Monday ride.
- Tricky: a past ride must not move, or the history and the costs shift. `replan` only reads
  `findUpcomingForTrip`, which already covers that.
- Tests: integration tests in `group/GroupControllerTest.java` and `MemberManagementTest.java`:
  the departure instant moves with the zone, past rides stay, extra riders go to the waitlist,
  drives are released and riders told.

### 4. Costs fixed when a ride takes place

`CostService.report` works out every ride's cost when the report is read, from the trip's
current `distance_km` and the group's current `cost_per_km` and `currency`. Changing the cost per
kilometre in July rewrites what everyone owed in March, including months that were settled with
recorded payments. Correcting a trip's distance does the same.

- Data: `distance_km` on `rides`, copied from the trip when the ride is created
  (`RideService.materialize`) and updated for upcoming rides by `replan`. A migration fills it
  from the trip for every existing ride.
- The rate: a small `cost_rates` table (`group_id`, `effective_from`, `cost_per_km`) instead of
  one column. Saving a new cost per kilometre in the settings adds a row from today (or from a
  date the admin picks). `CostService` looks up the rate in force on each ride's date. The
  migration adds one row per group with the current value from the start of time.
- Currency: changing it once costs or payments exist mixes amounts. Refuse the change in that
  case, with a message that says why.
- `CostSplitter` does not change; it already takes the distance and the rate as arguments.
- Tricky: rides that already left in the current month when the rate changes. With
  `effective_from` defaulting to today, those keep the old rate. The settings form should show
  the date so nobody is surprised.
- Undecided: copy the rate onto each ride at departure instead of a rate table. Simpler to read,
  but there is no job that runs at departure, and a table keeps the history visible in settings.
- Tests: `CostControllerTest`: a rate change keeps last month's balances, a distance edit only
  changes upcoming rides, a currency change is refused once there are costs.

### 5. History that survives leaving and deleting

Removing a member deletes their `group_members` row, and the foreign keys do the rest: their seat
bookings, absences, swaps and payments are deleted (`on delete cascade`), and rides they drove
lose their driver (`on delete set null`). `RideRepository.findCompleted` joins the driver, so
those rides drop out of the costs. The effect: when a parent leaves at the end of the school
year, the money others owed them, the money they owed and every payment they made disappear from
past months. Deleting a trip (`DELETE /trips/{id}`) deletes all its rides, past ones included,
with the same effect on costs, stats and the CSV export.

- Members: a `left_at` column on `group_members` instead of deleting the row.
  `GroupService.leave` and `removeMember` set it. `GroupAccess.requireMember` treats a member who
  left as a stranger (404 as today), and every member list (`findAllInGroup`, the rotation
  candidates, swap targets, payment pickers) filters them out. Past rides, bookings and payments
  keep pointing at the row, so history and balances stay as they were.
- Joining again reuses the same row and clears `left_at`, which the unique `(group_id, user_id)`
  constraint already expects.
- Names: someone who left shows as "Elena (left)" in history, stats and costs.
- Trips: delete only when none of its rides has left; otherwise "Archive", which sets
  `archived_at`, stops new rides and hides the trip from the trips tab. Deleting the whole group
  stays a real delete; that is the owner's explicit choice.
- Tricky: there are many queries that list members. Missing one means a person who left can be
  picked by the rotation. A repository method that already filters (`findActiveInGroup`) and a
  test per list is the safest way.
- Tricky: a member who left still owes money. The costs page should keep them in the balances
  and the settle-up list until they are even, and drop them after that.
- Tests: `MemberManagementTest`: costs and stats are the same before and after a member leaves,
  someone who left cannot see the group, the rotation does not pick them, rejoining works.
  `TripControllerTest`: archive instead of delete once rides left.

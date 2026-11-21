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

## Later

### 6. Two people changing one ride at once

Nothing stops two requests from changing the same ride at the same moment. `Ride` has no
`@Version` and nothing locks the row.

- Two members press "I'll drive" within a second. Both transactions see no driver, both assign
  themselves, the second commit wins, and the first person got a 200 and believes they drive.
- Two riders ask for the last seat. Each transaction counts the bookings it can see, and under
  PostgreSQL's default read committed both can end up `CONFIRMED`: five riders in a four-seat car.
- Two people open a new week at the same time. Both `materialize` calls insert the same
  `(trip_id, ride_date)`; the unique constraint stops the second one with an error that reaches
  the user as a 500.

- Rides: lock the ride row at the start of every method that changes it (`drive`,
  `assignDriver`, `stopDriving`, `cancel`, `BookingService.request` and `cancel`, swap accept,
  rotation apply) with a `findWithDetailsForUpdate` using `PESSIMISTIC_WRITE`. Contention is one
  ride at a time, so a lock is simpler than `@Version` and retry. The second "I'll drive" then
  waits, sees a driver and gets the existing 409.
- Materialize: `insert ... on conflict (trip_id, ride_date) do nothing` instead of `save`, as a
  native query. H2's PostgreSQL mode understands it, but check that before relying on it.
- Tricky: rotation apply locks many rides. Lock them in departure order to avoid deadlocks with a
  single-ride change.
- Tests: H2 and MockMvc in one transaction cannot show a race. A test with two threads and their
  own transactions can, and it is more convincing on PostgreSQL (item 26).

### 7. Correcting rides after they left

Once a ride's departure time has passed it cannot be changed: `RideRules.requireOpen` refuses
with "This ride has already left". But plans change on the morning itself. The driver was ill
and nobody went, a rider did not turn up, or someone else drove. `findCompleted` counts every
scheduled ride that has passed with its confirmed riders, so the costs and the driving stats
record a trip that did not happen and charge a rider who was not in the car.

- A short correction window, say seven days after departure, in which the driver or an admin can:
  - mark the ride as not having happened (a new `RideStatus.DID_NOT_RUN`, so it stays apart from
    a ride cancelled in advance);
  - remove a rider who did not come (`BookingStatus.NO_SHOW`);
  - record who really drove.
- `findCompleted`, `StatsService` and the CSV export already filter on status, so the new values
  only need to be excluded there.
- Everyone on the ride gets a notification about the correction, with who made it.
- Tricky: a correction can change a month that was already settled. Show the new balance, do not
  touch payments. The activity log (item 22) is where people can see why the number moved.
- Undecided: whether a no-show still pays. Some groups charge a rider who did not cancel; I
  would keep it simple (no charge) and leave a setting for later.
- Tests: `RideControllerTest`: corrections inside and outside the window, who may make them,
  costs and stats after each kind.

### 8. Running balances and payment corrections

Balances are per calendar month (`CostService.report` with `from` and `to`), and a payment only
counts in the month it was made. If Mihai owed 40 lei in February and pays in March, February
still shows the debt and March shows him 40 lei ahead. A debt nobody paid in February is not
carried into March at all. And a payment recorded with the wrong amount stays forever: there is
no endpoint to change or remove one.

- An "all time" view next to the monthly one: the balance since the group started, and the
  settle-up list (`SettlementPlanner.settle`) worked out from that. The monthly view stays for
  "what did we drive in March".
- Payments: `DELETE /payments/{id}` for whoever recorded it or an admin, plus a correction note.
  The other party is told. A payment is never edited in place.
- Undecided: confirmation by the person who received the money. It stops someone recording a
  payment they did not make, but it adds a state that has to be explained. I lean towards a
  notification to the receiver with an "I did not get this" button that flags it for admins.
- Depends on item 5, because a running balance is only stable when leaving members do not take
  their rows with them.
- Tests: `CostControllerTest` for the running balance across months and for removing a payment;
  `SettlementPlannerTest` for larger groups if the planner is used on bigger totals.

### 9. Children and other riders without an account

The README says the app is for school runs, but a seat belongs to a group member: one booking per
member per ride (`uq_seat_bookings_ride_member`). A parent with two children cannot book two
seats, and a child is not a member with a login. Today the workaround is a fake account per
child, which also puts the child in the rotation and the costs.

- Data: a `riders` table for people a member books for (`member_id` of the parent, `name`, an
  optional note such as "needs a booster seat"). `seat_bookings` gets an optional `rider_id`, and
  the unique constraint becomes one booking per ride, member and rider (an index over
  `coalesce(rider_id, 0)`, because a plain unique constraint treats nulls as different).
- `SeatAllocator` does not change: it counts bookings, and each child is one booking.
- Costs: a child's share is charged to the parent. `CostService` groups by member already;
  bookings with a rider still belong to the parent's member row.
- Notifications go to the parent. The driver sees the children's names with the parent next to
  them, on the ride page (`web/src/views/RideView.vue`) and in the calendar file.
- Standing requests (item 1) get an optional rider too.
- Tricky: privacy. Children's names and notes become visible to the whole group. Keep the note
  short and say who sees it.
- Tricky: a parent who also rides. They can hold their own booking and their children's on the
  same ride; the unique index has to allow exactly that.
- Undecided: contact phone numbers. A driver waiting at the gate wants the parent's number. An
  optional phone per member, shown only to people on the same ride, fits; I would do it here.
- Tests: `BookingControllerTest`: two children on one ride, the parent and a child together, the
  car fills by child, costs land on the parent, the unique index refuses a double booking.

### 10. Pickup stops and their order

A trip has an origin and a destination and nothing in between. Where each rider gets in is the
free text `pickup_note` on their membership, and the driver works out the order. For a morning
school run with three stops the times also differ: the first child is picked up at 07:30, the
last at 07:45.

- Data: `trip_stops` (`trip_id`, `position`, `name`, `minutes_after_departure`). A booking (and a
  standing request) can pick a stop. A member's usual stop replaces most uses of `pickup_note`,
  which stays as a free text hint.
- The ride page lists riders grouped by stop in order, with the time at each stop. The calendar
  file uses the rider's stop time and place instead of the trip's.
- The return trip copy (`TripService.addReturn`) reverses the stops and their offsets.
- Tricky: editing stops once bookings use them. Removing a stop moves its riders to "no stop"
  and tells them; reordering only changes times.
- Undecided: costs by stop. Someone picked up at the last stop rides fewer kilometres. Equal
  shares are simple and what most groups expect; I would keep them.
- Tests: `TripControllerTest` for stops and the return copy, `CalendarControllerTest` for the
  rider's time, a unit test for the offsets around midnight and daylight saving changes (the
  departure is local time, stop times follow it).

### 11. Picked up and dropped off

For a school run the question parents ask most is "is my child in the car" and later "are they at
school". The app knows who has a seat but nothing about the trip itself.

- The driver marks each rider picked up and dropped off on the ride page. `picked_up_at` and
  `dropped_off_at` on `seat_bookings`, settable from shortly before departure until a few hours
  after.
- The member who booked (the parent, with item 9) gets a notification for each.
- These marks also answer item 7's question of who really rode: a rider never picked up could be
  offered as a no-show in the correction.
- Tricky: a driver with a phone in one hand is driving. One large button per rider, no forms,
  and nothing that needs typing.
- Tricky: forgetting to mark is the normal case. Missing marks must not mean anything bad, and
  the app should not nag.
- Tests: `RideControllerTest` for who may mark and when; a Vitest spec for the buttons on the ride
  page.

### 12. Email notifications

Everything goes to the in-app list (`NotificationService.notify`), and the badge updates once a
minute while the app is open (`AppHeader.vue`). A cancellation at 06:50 for a 07:45 ride only
reaches people who happen to open the app.

- Optional SMTP through `spring-boot-starter-mail`, configured with `MAIL_HOST`, `MAIL_PORT`,
  `MAIL_USER`, `MAIL_PASSWORD` and `MAIL_FROM`. Without `MAIL_HOST` nothing changes.
- An outbox: `notify` also writes an `email_outbox` row in the same transaction, and a scheduled
  job sends and retries. A slow mail server then never holds up a request, and a rolled back
  change sends nothing.
- Preferences per member and notification type: right away, in a daily summary, or not at all.
  Sensible defaults: cancellations and driver changes right away, seat confirmations in the
  summary.
- Messages use `**name**` markup (`RideMessages.bold`). The email needs the same escaping rule the
  web app follows (`web/src/utils/message.ts`), in Java.
- Every email has a sign-in-free unsubscribe link with a signed token for that type.
- Development: a mailpit service in `docker-compose.yml` behind a compose profile, so the default
  stack stays three containers.
- Tests: unit tests for the markup to text and HTML; integration tests with a fake mail sender:
  outbox rows written, preferences respected, nothing sent for a rolled back request.

### 13. Password reset

There is no way back into an account after a forgotten password. There is no site admin either,
so nobody can help. With email (item 12) a reset link is the usual answer.

- `POST /auth/forgot` with an email: always answers the same, whether the account exists or not.
  A token is stored hashed (`password_resets`: `user_id`, `token_hash`, `expires_at`, `used_at`),
  valid for an hour and once.
- `POST /auth/reset` with the token and a new password. It ends every session (item 14 when it
  exists) and signs the user in.
- Throttle by email and by address, like `LoginThrottle`.
- Tricky: without SMTP configured the link has nowhere to go. Hide "Forgot your password?" in
  that case, and document a command for whoever runs the server.
- Tests: `AuthControllerTest`: same answer for unknown emails, expired and reused tokens refused,
  the old password stops working.

### 14. Sessions that can be ended

A login is a JWT valid for seven days, checked only by its signature (`JwtService.verify`). After
a password change (`PUT /me/password`) every old token keeps working until it expires. "Sign out"
only forgets the token in the browser's `localStorage` (`web/src/api/client.ts`). A lost phone
stays signed in for a week, and nothing can be done about it.

- A `token_version` on `users`, written into the token as a claim. `JwtAuthenticationFilter`
  checks it against the user row and refuses older tokens. That is one primary key lookup per
  request; cache it for a few seconds if it ever shows.
- A password change, a reset (item 13) and a new "Sign out everywhere" button on the profile page
  raise the version. The password change answers with a fresh token so the current tab stays in.
- The same check refuses tokens of deleted accounts (item 23).
- Undecided: moving the token from `localStorage` to an `HttpOnly` cookie, which keeps it out of
  reach of any script. It means turning CSRF protection back on in `SecurityConfig`. Worth doing,
  but separately.
- Tests: `AuthControllerTest`: an old token is refused after a password change and after "sign
  out everywhere", the new one works.

### 15. Calendar subscription

The `.ics` file is a download of the next four weeks (`CalendarController`, `DAYS_AHEAD = 28`).
A calendar app imports it once and never updates it: a cancelled ride stays in the calendar, and a
new one never appears. The endpoint needs the bearer token, so a calendar app cannot fetch it on
its own.

- A secret feed address per member and group: `calendar_feeds` with a random token stored
  hashed, `GET /api/calendar/{token}.ics` open to anyone with the token, and "Copy calendar link"
  and "Reset link" on the profile page.
- Cancelled rides stay in the feed for a while with `STATUS:CANCELLED` and a higher `SEQUENCE`
  (`IcsWriter` writes neither today), so calendar apps remove them instead of keeping them.
- Tricky: the link is a password. It should show only what the member needs (their own rides),
  be easy to reset, and stop working when they leave the group.
- Tricky: calendar apps poll every few minutes to every day. Answer with `ETag` and support
  `If-None-Match`, so most polls are cheap.
- Tests: `IcsWriterTest` for the cancelled event; `CalendarControllerTest` for the feed with a good
  and a reset token and for a member who left.

### 16. Reminders for riders and for rides nobody drives

`DriverReminders` sends the driver one reminder 24 hours ahead. Riders get nothing, and nobody is
told that tomorrow's ride still has no driver; the riders on it stay `PENDING` and find out at
the door.

- A "no driver yet" alert: the same hourly job finds rides in the next 24 hours without a driver
  and with at least one booking, and tells the group's drivers and admins once
  (`no_driver_alert_sent_at` on `rides`).
- Rider reminders, off by default, per member: "Tomorrow 07:45 with Mihai". It needs a
  `reminded_at` per booking, because `reminder_sent_at` is per ride.
- How far ahead: a group setting instead of the fixed `AHEAD = 24h` (the evening before suits a
  school run better than exactly 24 hours).
- Tricky: a driver assigned less than 24 hours before departure never gets a reminder today
  because the window is checked once per hour. That is fine, but an assignment inside the window
  should count as the reminder.
- Tests: `DriverRemindersTest`: alerts once, not for rides without riders, rider reminders only
  for those who asked.

### 17. Open swaps and swap housekeeping

A driver who cannot make it has to ask one named member (`CreateSwapRequest.targetMemberId`), wait
for an answer, then ask the next. And a few loose ends:

- pending swaps for a ride that has left stay pending forever (only `cancel` withdraws them via
  `withdrawSwaps`, and `accept` then fails with 409);
- when a swap is accepted only the requester is told (`SwapService.accept`); the riders learn
  about their new driver only if they were waiting for a seat;
- a member who is away that day can be asked and can accept.

- "Ask the group": a swap with no target. Every member with a car who is not away gets the
  request, and the first to accept takes the ride. The others' requests close.
- Expiry: swaps for rides that left become `EXPIRED`, checked when swaps are listed or by the
  cleanup job.
- Accepting tells the riders who drives now.
- Absences (item 2) are checked on create and on accept.
- Tests: `SwapControllerTest` for an open swap with two members racing to accept (item 6 helps),
  expiry, riders told, an away member refused.

### 18. Weekdays a driver can do

The rotation knows only absences. A parent who works late on Mondays and Wednesdays has to add an
absence for every Monday and Wednesday, or step down every time the rotation picks them.

- `drive_days` on `group_members` (the `DaysOfWeek` bitmask), all days by default, set in the
  membership form next to the seats.
- `FairRotation.Candidate` gets the days, and `RotationService.plan` treats other days like
  absences.
- Offering to drive by hand (`RideService.drive`) ignores it; it limits the rotation only.
- Undecided: per trip instead of per weekday (the morning run but not the afternoon one). Per
  weekday covers most cases; per trip can come later if someone asks.
- Tests: `FairRotationTest` cases for limited days, `RotationControllerTest` for the setting.

### 19. Fairness that counts riding

`FairRotation` balances the number of drives. A member who rides once a week and one who rides
every day are asked to drive equally often. The stats page already shows the share of each
member's trips they drove (`StatsService`, drives over drives plus rides), which is the number
people compare in practice.

- A group setting: "count drives" (today) or "balance by use". With balance by use, the
  candidate with the lowest share of their own trips driven goes first, with today's rules
  (seats, two rides a day, absences) unchanged around it.
- The fuel cost split already pays drivers for the extra driving, which is why this is a setting
  and not the new default.
- Tricky: members who drive but never ride have a share of 0 out of 0. They should not be picked
  first forever; treat them as having driven their share.
- Tricky: the eight-week window. A member who just joined has little history either way. The
  tie-breakers in `docs/architecture.md` need a line for the new mode.
- Tests: `FairRotationTest`: a frequent rider drives more, a member who only drives is not
  overloaded, today's mode gives the same results as before.

### 20. A rotation that runs ahead

The rotation only fills rides when an admin opens the rotation tab and presses apply. And apply
(`POST /rotation/apply`) plans again from scratch, so if someone took a ride between the preview
and the click, the admin gets a different result from the one they looked at.

- A group setting "fill rides N days ahead" (off by default). A daily job runs the rotation for
  the day that comes into range and tells each driver in one notification ("You drive Tuesday and
  Thursday next week") instead of one per ride.
- Apply takes the proposal the admin saw (pairs of ride and member) and checks each pair is
  still possible: the ride still has no driver, the member still has a car and is not away. Pairs
  that changed are left out and listed in the answer.
- Tricky: the job and an admin pressing apply at the same time. Locking the rides (item 6) makes
  that safe.
- Tests: `RotationControllerTest` for applying a stale proposal; a test that runs the job by hand
  with the clock moved (scheduling is off in tests, `app.scheduling.enabled=false`).

### 21. Join approval and expiring invites

Anyone with the invite code is a member at once (`GroupService.join`). They then see every
member's name, pickup note (often a home address) and every ride. For a school group that is a
lot to give away with one forwarded link. The code never expires unless an admin rotates it, and
rotating it breaks the link for everyone.

- A group setting "admins approve new members". A join then creates a membership with
  `status = PENDING`; admins see the requests on the members tab and approve or decline, and the
  person is told. `GroupAccess` treats a pending member as a stranger.
- Invite links with an expiry and a number of uses, in a `group_invites` table next to the group
  code: "valid for 7 days, 3 people" for a new family.
- Tricky: the invite preview (`GET /groups/invite/{code}`) should say whether approval is needed.
- Tests: `GroupControllerTest` for a pending member who cannot see the group, approval, decline,
  an expired invite and one that ran out of uses.

### 22. Group activity log

Who cancelled Thursday's ride, who moved Elena off the waitlist, who changed the cost per
kilometre, who recorded that payment: this is only in the notifications sent to some of the
people affected, and those are deleted after 90 days (`NotificationCleanup`). When money is
involved people want to be able to look it up.

- A `group_events` table (`group_id`, `actor_member_id`, `type`, `subject` such as a ride or
  payment id, `detail` as JSON, `created_at`), written by the services where they already notify.
- An "Activity" tab for admins, and the ride page shows that ride's events to everyone in the
  group.
- Most worth recording: settings and rate changes, payments and their removal, cancellations and
  reinstatements, driver changes, members joining, leaving and being removed, role changes.
- Retention: longer than notifications, say two years, with the cleanup job.
- Tests: an event per action in the existing controller tests, and visibility (members see a
  ride's events, only admins see the full log).

### 23. Deleting an account and taking the data

There is no way to delete an account (`MeController` has `GET`, `PUT` and the password change)
and no way to download what the app keeps about you. Both are expected of an app that stores
names, addresses in pickup notes and travel times.

- `DELETE /me` with the password. The member leaves every group the way item 5 leaves them, and
  the user row is anonymised: the name becomes "Former member", the email and the password hash
  are cleared. Rides and payments others depend on stay.
- An owner of a group with other members must hand it over first, as `leave` already requires.
- `GET /me/export`: one JSON file with the profile, memberships, rides driven and taken,
  bookings, absences, payments and notifications.
- Depends on item 5: deleting the user today cascades into the same history loss.
- Tests: `AuthControllerTest` or a new `AccountTest.java`: the account cannot sign in, others'
  costs are unchanged, the export contains the expected parts and nothing of other people's
  beyond names.

### 24. Hardening for use outside a laptop

A few defaults that are fine on a laptop and not on a server:

- `JWT_SECRET` defaults to `local-development-secret-change-me-0123456789` in compose and in
  `application.yml`. Anyone who reads the README can sign their own tokens for a server that
  kept it. The API should refuse to start with that value unless it is explicitly in demo mode.
- nginx (`web/nginx.conf`) sends no security headers. A `Content-Security-Policy` that allows
  only the app's own scripts, `X-Content-Type-Options`, `Referrer-Policy` and
  `frame-ancestors 'none'` are cheap to add.
- `LoginThrottle` counts failures per email only, so one address can try one password against
  many accounts. Registration and the invite preview have no limit at all. Add per address limits
  for login, registration and invite lookups. The address comes from `X-Forwarded-For`, which is
  only trustworthy when set by the nginx container.
- Undecided: what demo mode means for the secret. I lean towards allowing the default secret only
  together with `SEED_DEMO=true` and printing a warning.
- Tests: a startup test for the secret check; `AuthControllerTest` for the new limits; a check of
  the headers can go in the Docker job of `.github/workflows/ci.yml`.

### 25. Backups, restore and upgrades

The data lives in the `db-data` volume and nothing copies it anywhere. `docker compose down -v`
in the wrong folder, or a broken disk, ends the group's history.

- A `backup` service in `docker-compose.yml` (behind a profile) that runs `pg_dump` every night
  into `./backups` and keeps the last 14.
- `docs/operations.md`: how to restore a dump into a fresh volume, tried for real, and how to
  upgrade: back up, pull, `docker compose up -d --build`, Flyway migrates on start. A PostgreSQL
  major version upgrade (17 to 18) needs a dump and restore, which the doc should spell out.
- The version in the page footer (from the build) so people know what they run.
- Tests: a script under `api/` or `scripts/` that restores a dump into a throwaway database and
  runs a query, run by hand; it is too heavy for every CI run.

### 26. Tests against PostgreSQL

Every API test runs on H2 in PostgreSQL mode (`api/src/test/resources/application.properties`).
It is fast, but the migrations never run on PostgreSQL in CI (the Docker job only builds images),
and the differences that matter here are exactly the ones H2 hides: row locks and concurrent
transactions (item 6), `on conflict`, `timestamp with time zone` handling, case-insensitive email
lookups.

- A Maven profile that points the tests at PostgreSQL, through Testcontainers or a service
  container in the `api` job of `.github/workflows/ci.yml`. H2 stays the default locally.
- Run the full suite on PostgreSQL in CI, and the concurrency tests only there.
- Tricky: `IntegrationTest` rolls back each test's transaction. Tests with several threads need
  their own transactions and cleanup.
- Tests: the item is the tests. The first one worth adding is the double "I'll drive" from item 6.

### 27. Accessibility pass

The Vue views have barely any ARIA: about a dozen `aria-` or `role` attributes in the whole app.
Some things that stand out:

- the group tabs (`GroupView.vue`, `nav.tabs`) are links styled as tabs, without
  `aria-current` on the active one;
- booking states (confirmed, waitlisted, pending) are told apart mostly by colour in `RideCard.vue`;
- `DayPicker.vue` toggles weekdays with buttons that do not say whether they are on
  (`aria-pressed`);
- form errors are not tied to their fields, and the page title changes on navigation but focus
  does not move, so a screen reader user does not hear that the page changed;
- the notification badge in `AppHeader.vue` is not announced.

- Fix those, then add an automated check: `axe-core` run against the rendered components in the
  existing Vitest specs (jsdom), for the main views.
- Tests: the axe checks, and a few specs for keyboard use of the day picker and the tabs.

### 28. Metrics and logs

Only `/actuator/health` is exposed. The scheduled jobs (`DriverReminders`, `NotificationCleanup`)
can fail every hour and the only trace is a stack trace in `docker compose logs`.

- Micrometer with the Prometheus endpoint, on the management port only (not forwarded by nginx),
  off by default. Counters for reminders sent, notifications cleaned up, logins failed and
  throttled, and the timing of the schedule endpoint.
- A request id: taken from `X-Request-Id` or generated, written in every log line and returned in
  error responses (`ApiError`), so a user's screenshot can be matched to a log line.
- Optional JSON logs (`LOG_FORMAT=json`) for people who collect them.
- Tests: an integration test that the request id comes back in an error, and that the metrics
  endpoint is not reachable through the normal API path.

### 29. Messages on a ride

The driver can leave one note of up to 300 characters (`PUT /rides/{id}/note`). Riders cannot say
anything. When a rider drops their seat (`BookingService.cancel`) the driver is not told at all;
only riders who move up from the waitlist hear about it. So the driver waits outside a house for a
child who stayed home sick.

- Cancelling a seat tells the driver, with an optional reason ("Ana is ill today"). That part is
  small and could ship on its own.
- A short message thread per ride (`ride_messages`) for the driver and everyone with a booking,
  with a notification to the others. Quick messages for the driver ("Running 5 minutes late",
  "Outside").
- Tricky: messages on a ride that left are history; keep them read-only.
- Tricky: this is not a chat app. No editing, no reactions, a length limit, and the thread is
  deleted with the ride's other data after the retention period.
- Tests: `BookingControllerTest` for the driver being told; a new `RideMessageTest.java` for who
  can post and read.

### 30. More than one car

A member has one car per group: `can_drive` and `car_seats` on `group_members`. A family with a
small car on weekdays and a seven-seater on Fridays has to change the number every week, and the
number of child seats, which matters more on a school run than the number of seats, is not
recorded at all.

- A `vehicles` table per user (`name`, `seats`, `child_seats`), and a default vehicle per
  membership. Offering to drive picks the vehicle (the `seats` field of `DriveRequest` stays for
  "only two seats today").
- The rotation uses the default vehicle. With item 9, a ride whose riders need child seats
  prefers a car that has them.
- A migration turns every member with `can_drive` into one vehicle with their seats.
- Tests: migration on existing data, driving with a second car, the rotation preferring child
  seats when the riders need them.

## Maybe

### 31. Translations

The UI is English only, with dates and money formatted as `en-GB` (`web/src/utils/dates.ts`,
`money.ts`), while the demo groups and the default time zone are in Bucharest. A Romanian school
group would want Romanian.

- `vue-i18n` in the web app, English and Romanian to start, a language choice on the profile and
  the browser's language as the default. Formats follow the chosen language.
- The hard part is on the server: notifications are stored as finished English sentences
  (`RideMessages`). Translating them later needs the type and its parameters stored instead, and
  the text built when it is shown.
- That rewrite of the notifications is why this is a Maybe: it touches every service that
  notifies.
- Tests: a spec that every key exists in both languages; tests for building notification text
  from its parameters.

### 32. Installable app with the week offline

The app is mostly used on a phone in the morning, often in a car park with a weak signal.

- A web app manifest and a service worker (`vite-plugin-pwa`) so it can be installed, and the
  schedule and "my rides" kept for reading offline, with "as of 07:12" shown when the data comes
  from the cache.
- Nothing is changed offline; buttons are disabled with a short message.
- Tricky: stale data is worse than none when a ride was cancelled. The "as of" line has to be
  impossible to miss.
- Tests: a spec for the offline banner; the service worker itself is checked by hand.

### 33. Push notifications

With item 32 in place, Web Push can deliver a cancellation to a phone without email. The server
signs pushes with its own VAPID keys and needs no account anywhere, but the messages do travel
through the browser vendor's push service, which is worth saying in the docs.

- `push_subscriptions` per user and device, a subscribe toggle on the profile page, sending from
  the same outbox as email (item 12) if that exists.
- Tricky: iOS only delivers pushes to installed web apps, which is why this depends on item 32.
- Tests: the subscription endpoints and the payload builder; delivery is checked by hand.

### 34. Distances from a self-hosted router

The distance of a trip is typed by hand, and it drives every cost. People guess, or copy it from a
map app.

- An optional `ROUTING_URL` pointing at an OSRM server the group runs itself. With it, the trip
  form has "Work out the distance" for the origin and destination.
- The catch: OSRM needs coordinates, and turning an address into coordinates needs a geocoder
  (Nominatim), which is another heavy service to run. Choosing points on a map instead avoids it
  but adds a map library and tiles.
- Without `ROUTING_URL` nothing changes, so the app keeps working without any service.
- Tests: the client against a stub server.

### 35. Holidays from a calendar file

Cancelling school holidays is a manual "cancel days" per trip, at most 62 days at a time
(`TripService.cancelRange`). Schools and councils publish their holidays as calendar files.

- Upload an `.ics` file on the trips tab, see the date ranges it contains, tick the ones to apply
  and to which trips, and the existing `cancelRange` does the rest with the event's name as the
  reason.
- Only all-day events, read with a small parser for `VEVENT`, `DTSTART` and `DTEND`; recurring
  events are left out of the first version.
- Tests: parser unit tests on a real-looking school calendar; an integration test for applying a
  range to two trips.

### 36. More than one API instance

The compose stack runs one API container, and some parts assume it: `LoginThrottle` keeps its
counts in memory, and the scheduled jobs run in every instance, so two instances would send
every reminder twice (the `reminder_sent_at` check and update can race).

- ShedLock with its JDBC provider so each job runs once, and the throttle counts in a table.
- This only matters to someone who runs several instances behind a load balancer, which is more
  than a carpool group needs. Hence Maybe.
- Tests: the throttle against the table; a job test that a held lock skips the run.

### 37. Faster schedule reads in big groups

Every read of a date range first runs `RideService.materialize` for every active trip, and
`upcomingFor` (my rides across groups) does that for each group in turn, then builds views ride by
ride. For a family in three groups with a few trips each that is fine. For an office pool with
40 trips, or a member in many groups, every page load does work that only matters once per ride.

- Measure first: the test setup already counts SQL statements
  (`hibernate.generate_statistics`), so a test can pin the number of queries for the schedule, my
  rides and the calendar.
- Then: remember per trip how far rides exist (`materialized_until`) so reads skip generating,
  and let a nightly job extend every trip a few weeks ahead.
- Tricky: standing requests (item 1) create bookings when rides are created; moving creation into
  a job moves that too, which is fine as long as it happens before anyone can see the ride.
- Tests: statement count tests for the three endpoints before and after.

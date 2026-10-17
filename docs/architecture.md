# How carpool works

Notes on the rules that are easy to get wrong. The code that implements each one is named so
you can find the tests next to it.

## Trips and rides

A **trip** is a weekly pattern: a departure time, a set of weekdays (stored as a bitmask, Monday
is bit 0), a distance and a first and optional last day. A **ride** is one date of a trip.

Rides are not created up front. The first time anyone reads a date range (the schedule, the
rotation, the calendar file) `RideService.materialize` stores the missing rides for every active
trip of the group. `RideGenerator` works out the dates:

- the range is `from` inclusive to `to` exclusive, the trip's `endsOn` is its last day and is
  included;
- the departure instant is the trip's local time on that date in the group's time zone, looked up
  for every date so daylight saving time changes do not shift rides.

Editing a trip moves its planned rides to the new time and cancels dates it no longer runs on
(`RideService.replan`). Pausing a trip hides its future rides; past rides stay in the history.

## Seats

`SeatAllocator` decides every booking's status from scratch whenever something changes (a request,
a cancellation, a new driver, fewer seats):

- no driver: everyone is `PENDING`;
- with a driver: the earliest requests are `CONFIRMED` up to the seats offered, the rest
  `WAITLISTED` in order.

Riders who gain a seat get a notification. A rider who cancels and asks again goes to the back of
the queue. One booking row per rider and ride is enforced by a unique constraint, so a double
click cannot take two seats.

## The rotation

`FairRotation` fills rides that have no driver, in departure order. For each ride it looks at
members with a car who are not away that day and picks, in this order of preference:

1. a car with room for everyone who asked for a seat, and a driver without another ride that day;
2. room for everyone, even if it is the driver's second ride that day;
3. a smaller car, preferring a driver without another ride that day;
4. any car.

Within a preference level the member with the fewest drives wins. Drives count over the last
eight weeks plus rides already assigned in the future, and every ride assigned in the same run
counts straight away. Ties go to whoever drove longest ago (never driving beats everything),
then to the member who joined first. Rides nobody can take are left open and shown as such.

## Fuel costs

Each group has a cost per kilometre. A ride that took place costs `distanceKm × costPerKm`,
split equally between the driver and the confirmed riders; each rider owes their share, rounded
half up to the cent, to the driver. The driver's own share is not charged to anyone.

Balances are per period (a calendar month in the app). Recorded payments count towards the
period they were made in. `SettlementPlanner` suggests payments by repeatedly letting the
member who owes most pay the member who is owed most.

## Time

Everything that depends on "now" takes a `java.time.Clock`. Tests replace it with a clock pinned
to Monday 2 March 2026 06:00 UTC (08:00 in Bucharest) and move it where they need to.

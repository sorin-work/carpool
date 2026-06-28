# API notes

All endpoints live under `/api` and speak JSON. Except for registration and login they need a
bearer token:

```
Authorization: Bearer <token from /api/auth/login>
```

Tokens are JWTs signed with `JWT_SECRET` and expire after `JWT_TTL` (7 days by default). There is
no refresh endpoint; the web app sends you back to the sign-in page when a token is rejected.

## Errors

Every error has the same shape:

```json
{
  "timestamp": "2026-03-02T06:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/groups/3/trips",
  "fields": { "distanceKm": "must be greater than 0" }
}
```

`fields` only appears for validation errors. Groups, rides and trips you are not a member of
answer `404`, not `403`, so ids cannot be probed. Actions you are not allowed to take inside your
own group answer `403`; conflicts with the current state of a ride (already left, cancelled,
someone else is driving) answer `409`.

## Dates and times

- Calendar dates are `YYYY-MM-DD` and belong to the group's time zone.
- Instants (`departureAt`, `createdAt`) are ISO-8601 in UTC.
- Date ranges are `from` (inclusive) to `to` (exclusive).
- A trip's `endsOn` is its last day and is included.

## Auth and profile

| Method | Path                 | Notes                                        |
| ------ | -------------------- | -------------------------------------------- |
| POST   | `/auth/register`     | `email`, `displayName`, `password` (8+ chars) |
| POST   | `/auth/login`        | returns `token`, `expiresAt`, `user`         |
| GET    | `/me`                |                                              |
| PUT    | `/me`                | `displayName`                                |
| PUT    | `/me/password`       | `currentPassword`, `newPassword`             |
| GET    | `/me/rides?days=7`   | my upcoming rides across all groups          |

## Groups and members

| Method | Path                                       | Who                |
| ------ | ------------------------------------------ | ------------------ |
| GET    | `/groups`                                  | me                 |
| POST   | `/groups`                                  | anyone, becomes owner |
| POST   | `/groups/join`                             | `inviteCode`, case and dashes ignored |
| GET    | `/groups/{id}`                             | members            |
| PATCH  | `/groups/{id}`                             | admins; `name`, `description`, `timezone`, `costPerKm`, `currency` |
| POST   | `/groups/{id}/invite-code`                 | admins; new code, old one stops working |
| POST   | `/groups/{id}/leave`                       | members            |
| PUT    | `/groups/{id}/members/me`                  | `canDrive`, `carSeats`, `pickupNote` |
| PUT    | `/groups/{id}/members/{memberId}/role`     | admins; `ADMIN` or `MEMBER` |
| DELETE | `/groups/{id}/members/{memberId}`          | admins             |
| POST   | `/groups/{id}/members/{memberId}/ownership`| owner              |

## Trips and rides

A trip is the recurring pattern; a ride is one date of it. Rides are created the first time a
date range is read, so there is no endpoint to create them.

| Method | Path                                   | Who / notes |
| ------ | -------------------------------------- | ----------- |
| GET    | `/groups/{id}/trips`                   | members |
| POST   | `/groups/{id}/trips`                   | admins |
| PUT    | `/trips/{id}`                          | admins |
| POST   | `/trips/{id}/pause`, `/resume`         | admins; paused trips drop out of the upcoming schedule |
| DELETE | `/trips/{id}`                          | admins; deletes the trip's rides too |
| GET    | `/groups/{id}/rides?from&to`           | members; at most 62 days, defaults to two weeks from today |
| GET    | `/rides/{id}`                          | members |
| POST   | `/rides/{id}/driver`                   | members with a car; optional `seats` |
| PUT    | `/rides/{id}/driver/{memberId}`        | admins |
| DELETE | `/rides/{id}/driver`                   | the driver or an admin |
| PUT    | `/rides/{id}/note`                     | the driver or an admin |
| POST   | `/rides/{id}/cancel`                   | the driver or an admin; optional `reason`, notifies everyone on the ride |
| POST   | `/rides/{id}/reinstate`                | admins |
| GET    | `/rides/{id}/passengers`               | members |
| POST   | `/rides/{id}/seat`                     | members; `PENDING` without a driver, then `CONFIRMED` or `WAITLISTED` in request order |
| DELETE | `/rides/{id}/seat`                     | the rider; the waitlist moves up |
| GET    | `/groups/{id}/calendar.ics`            | my rides for the next four weeks |
| GET    | `/groups/{id}/rides.csv?from&to`       | rides that took place, up to a year |

## Rotation and absences

| Method | Path                                   | Notes |
| ------ | -------------------------------------- | ----- |
| GET    | `/groups/{id}/rotation?from&to`        | proposal only, nothing is saved; up to 31 days |
| POST   | `/groups/{id}/rotation/apply?from&to`  | admins; assigns and notifies the drivers |
| GET    | `/groups/{id}/absences`                | upcoming absences of everyone |
| POST   | `/groups/{id}/absences`                | `startsOn`, `endsOn` (both inclusive), `note` |
| DELETE | `/absences/{id}`                       | the member or an admin |

The rotation only fills rides without a driver. For each ride, in departure order, it picks the
member with a car who has driven least in the last eight weeks (counting rides already assigned
in the future), skipping anyone who is away. Ties go to whoever drove longest ago, then to the
member who joined first. A car with room for every rider beats a smaller one, and nobody gets two
rides on the same day if someone else is free.

## Swaps

| Method | Path                       | Notes |
| ------ | -------------------------- | ----- |
| POST   | `/rides/{id}/swaps`        | the driver; `targetMemberId`, optional `offeredRideId` (a ride the target drives), `message` |
| GET    | `/groups/{id}/swaps`       | swaps I asked for or was asked |
| POST   | `/swaps/{id}/accept`       | the target |
| POST   | `/swaps/{id}/decline`      | the target |
| POST   | `/swaps/{id}/cancel`       | the requester |

## Notifications

| Method | Path                              | Notes |
| ------ | --------------------------------- | ----- |
| GET    | `/notifications?page=0&size=20`   | newest first; `page` is zero-based, `size` up to 100 |
| GET    | `/notifications/unread-count`     | |
| POST   | `/notifications/{id}/read`        | |
| POST   | `/notifications/read-all`         | |

Messages are plain text with names wrapped in `**double asterisks**`. Clients must escape the text
before turning those into bold. Notifications older than 90 days are deleted every night.

## Stats and costs

`GET /groups/{id}/stats?from&to` counts, per member, drives, kilometres driven, rides taken,
passengers carried and the share of their trips they drove. Only rides that actually left and
were not cancelled count. Defaults to the last 90 days.

`GET /groups/{id}/costs?from&to` defaults to the current month. Each ride costs
`distanceKm × costPerKm`, split equally between everyone in the car; riders owe their share to
the driver. Amounts are in cents. `settlements` is a short list of payments that evens everyone
out.

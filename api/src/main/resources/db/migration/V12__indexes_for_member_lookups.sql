-- "My rides" and the stats look bookings and swaps up by member.
create index ix_seat_bookings_member on seat_bookings (member_id, status);
create index ix_swap_requests_requester on swap_requests (requester_id, status);
create index ix_absences_dates on absences (ends_on, starts_on);

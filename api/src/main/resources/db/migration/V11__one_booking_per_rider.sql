-- Two quick clicks on "Ask for a seat" could both pass the duplicate check and
-- insert two rows. Keep the first row per rider and ride, then let the database
-- enforce it.
delete from seat_bookings b
where exists (
    select 1 from seat_bookings o
    where o.ride_id = b.ride_id and o.member_id = b.member_id and o.id < b.id
);

alter table seat_bookings add constraint uq_seat_bookings_ride_member unique (ride_id, member_id);

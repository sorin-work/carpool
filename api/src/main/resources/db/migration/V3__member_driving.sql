alter table group_members add column can_drive boolean not null default false;
alter table group_members add column car_seats integer not null default 0;
alter table group_members add column pickup_note varchar(200);

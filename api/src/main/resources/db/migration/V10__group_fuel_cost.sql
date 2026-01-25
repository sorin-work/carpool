-- What a kilometre costs the driver (fuel plus wear), in the group's currency.
alter table carpool_groups add column cost_per_km numeric(6, 3) not null default 0;
alter table carpool_groups add column currency varchar(3) not null default 'EUR';

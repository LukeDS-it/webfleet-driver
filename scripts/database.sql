create table aggregate (
  name varchar(255) primary key,
  description varchar(255),
  text text,
  parent varchar(255)
);

alter table aggregate
  add constraint aggregate_parent
foreign key (parent) references aggregate (name);
create table if not exists aggregate (
  name varchar(255) not null primary key,
  description varchar(255),
  text text not null,
  parent varchar(255)
);

alter table aggregate
  add constraint aggregate_parent
foreign key (parent) references aggregate (name);
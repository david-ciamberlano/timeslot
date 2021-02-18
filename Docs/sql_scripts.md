# SQL

## Create Tables

```SQL

create table users(
    username varchar_ignorecase(50) not null primary key,
    password varchar_ignorecase(80) not null,
    enabled boolean not null
);

create table authorities (
    username varchar_ignorecase(50) not null,
    authority varchar_ignorecase(50) not null,
    constraint fk_authorities_users foreign key(username) references users(username)
);

create unique index ix_auth_username on authorities (username,authority);

create table account(
    username varchar_ignorecase(50) not null primary key,
    address    VARCHAR(80) NOT NULL,
    passphrase VARCHAR(250) NOT NULL,
    UNIQUE (address)
);

```

## Init Database

```SQL


INSERT INTO users (username, password, enabled) VALUES
('init','$2a$10$Y1OQ3JCE05olIuVER0i7peeJSccHr4P2e2EIslecXat6tqmxyGd46', TRUE);

INSERT INTO authorities (username, authority)
values ('init', 'ROLE_ADMIN');




```


## Init users via Rest Api

```
POST /admin/v1/user

{
  "username": "admin",
  "password": "admin",
  "algoAddress": "2626GBPAKSW7AGQZ35BJDR3KFRR6HDZNY57TETUJWZHFNCBRBDUATAM44E",
  "algoPassphrase": "ramp tiny spell since buffalo person meadow another fatal salt chalk into uncover pink sing escape maple slight infant nation critic crop air above tourist",
  "administrator": true
}


curl -X POST "http://localhost:9696/admin/v1/user" -H  "accept: */*" -H  "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin\",\"algoAddress\":\"2626GBPAKSW7AGQZ35BJDR3KFRR6HDZNY57TETUJWZHFNCBRBDUATAM44E\",\"algoPassphrase\":\"ramp tiny spell since buffalo person meadow another fatal salt chalk into uncover pink sing escape maple slight infant nation critic crop air above tourist\",\"administrator\":true}"

```

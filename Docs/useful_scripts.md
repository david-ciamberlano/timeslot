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

```text

curl -X POST "http://localhost:9696/admin/v1/user" -H  "accept: */*" -H  "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin\",\"algoAddress\":\"<your-address>\",\"algoPassphrase\":\"<your-passphrase>\",\"administrator\":true}"

POST /admin/v1/user

{
  "username": "admin",
  "password": "admin",
  "algoAddress": "<your-address>",
  "algoPassphrase": "<your-passphrase>",
  "administrator": true
}




```



## How to encode the Password with BCrypt
```jshelllanguage
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String rawPassword = "admin";
String encodedPassword = encoder.encode(rawPassword);

System.out.println(encodedPassword);
```


## Base64 encode

```jshelllanguage
base64.b64encode(
    hashlib.sha256(
        str('This string!').encode('utf-8')
    ).digest()
).decode('utf-8')
```


```json
{
  "username": "archive",
  "password": "admin",
  "algoAddress": "HYX5BTKBQQKHBYLZXDT672PY5NBGK7NBI4NPTOUS472KEBC3ZXYYQ2MKGE",
  "algoPassphrase": "hazard camp vapor chapter rather impose lawsuit promote bid confirm pig radar witness rich lake ginger surface suggest luggage drill day adult pole about cat",
  "administrator": true
}
```

# timeslot Project



## How to encode the Password woth BCrypt
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


## SQL 

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
    passphrase VARCHAR(250) NOT NULLACCOUNT
);


INSERT INTO users (username, password, enabled) VALUES
('admin','$2a$10$Y1OQ3JCE05olIuVER0i7peeJSccHr4P2e2EIslecXat6tqmxyGd46', TRUE);

INSERT INTO account (username, address, passphrase) VALUES
('admin', '2626GBPAKSW7AGQZ35BJDR3KFRR6HDZNY57TETUJWZHFNCBRBDUATAM44E',
 'ramp tiny spell since buffalo person meadow another fatal salt chalk into uncover pink sing escape maple slight infant nation critic crop air above tourist');

INSERT INTO authorities (username, authority)
values ('admin', 'ROLE_ADMIN');

INSERT INTO users (username, password, enabled) VALUES
('user1','$2a$10$QL6bJw8E9/uWCIOjMuhWVezuVhD1g3PapS3Z.BZfoGNuZWtQzTo4a	', TRUE);

INSERT INTO account (username, address, passphrase) VALUES
('user1', '4JXP3IICEHDLYCIJ3IBKAEKQVIFYLN2DFGOKCXYVXKM77BN6K6JFTMTIY4',
 'cover infant item glare grass category chronic urban happy swear feed hazard trap allow spray liberty glue top thought fan weapon aunt thumb able process');


INSERT INTO authorities (username, authority)
values ('user1', 'ROLE_USER');

```




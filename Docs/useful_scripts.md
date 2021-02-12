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


```json
{
  "username": "archive",
  "password": "admin",
  "algoAddress": "HYX5BTKBQQKHBYLZXDT672PY5NBGK7NBI4NPTOUS472KEBC3ZXYYQ2MKGE",
  "algoPassphrase": "hazard camp vapor chapter rather impose lawsuit promote bid confirm pig radar witness rich lake ginger surface suggest luggage drill day adult pole about cat",
  "administrator": true
}

```

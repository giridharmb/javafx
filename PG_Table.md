## Docker & PostgreSQL Setup

#### Run PG Docker

```bash
docker run -d \
  --name my-postgres \
  -e POSTGRES_PASSWORD=please_ignore_this \
  -p 5432:5432 \
  -v postgres_data:/var/lib/postgresql/data \
  postgres:16
```

#### Create PG Table

```sql
CREATE TABLE t_random(
   random_num INT NOT NULL,
   random_float DOUBLE PRECISION NOT NULL,
   md5 TEXT NOT NULL
);
```

### PG Creds File

File `~/config/db.conf`

```ini
db.host=127.0.0.1
db.username=postgres
db.password=please_ignore_this
db.name=postgres
```

File `~/config/db_v2.conf`

```ini
db.host=127.0.0.1
db.username=postgres
db.password=please_ignore_this
db.name=postgres
```

## Insert Some Rows Into PG Table (Which JavaFX Application Can Render)

#### Create: t_random_v1 + t_random_v2

```sql
CREATE TABLE t_random_v1 (
   random_num INT NOT NULL,
   random_float DOUBLE PRECISION NOT NULL,
   md5 TEXT NOT NULL
);
```

```sql
CREATE TABLE t_random_v2 (
   random_num INT NOT NULL,
   random_float DOUBLE PRECISION NOT NULL,
   md5 TEXT NOT NULL
);
```

#### Insert Data: t_random_v1 + t_random_v2

```sql
INSERT INTO t_random_v1 (random_num, random_float, md5) 
    SELECT 
    floor(random()* (999-100 + 1) + 100), 
    random(), 
    md5(random()::text) 
 from generate_series(1,100);
```

```sql
INSERT INTO t_random_v2 (random_num, random_float, md5) 
    SELECT 
    floor(random()* (999-100 + 1) + 100), 
    random(), 
    md5(random()::text) 
 from generate_series(1,100);
```

#### Create: t_random_v3 + t_random_v4

```sql
CREATE TABLE t_random_v3 (
   random_num INT NOT NULL,
   random_float DOUBLE PRECISION NOT NULL,
   md5_1 TEXT NOT NULL,
   md5_2 TEXT NOT NULL,
);
```

```sql
CREATE TABLE t_random_v4 (
   random_num INT NOT NULL,
   random_float DOUBLE PRECISION NOT NULL,
   md5_1 TEXT NOT NULL,
   md5_2 TEXT NOT NULL,
);
```

#### Insert Data: t_random_v3 + t_random_v4

```sql
INSERT INTO t_random_v3 (random_num, random_float, md5_1, md5_2) 
    SELECT 
    floor(random()* (999-100 + 1) + 100), 
    random(), 
    md5(random()::text), 
    md5(random()::text) 
 from generate_series(1,100);
```

```sql
INSERT INTO t_random_v4 (random_num, random_float, md5_1, md5_2) 
    SELECT 
    floor(random()* (999-100 + 1) + 100), 
    random(), 
    md5(random()::text), 
    md5(random()::text) 
 from generate_series(1,100);
```




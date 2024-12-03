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

#### Insert Some Rows Into PG Table (Which JavaFX Application Can Render)

```sql
INSERT INTO t_random (random_num, random_float, md5) 
    SELECT 
    floor(random()* (999-100 + 1) + 100), 
    random(), 
    md5(random()::text) 
 from generate_series(1,100);
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
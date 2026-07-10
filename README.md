# Skupabase

Addon de Skript para ejecutar queries SQL contra Supabase/Postgres desde Minecraft.

## Instalación

1. Copia `skupabase-0.1.0.jar` a `plugins/`.
2. Instala `Skript`.
3. Inicia el servidor una vez.
4. Configura `plugins/Skupabase/config.yml`.
5. Reinicia el servidor.

## Supabase: paso a paso

### Paso 1

Pulsa el botón verde `Connect`.

![Paso 1 - Connect](docs/images/step-1-connect.png)

### Paso 2

Selecciona:

- `Direct: Connection String`
- `Connection Method`: `Session pooler`
- `Type`: `JDBC`

![Paso 2 - Connection method](docs/images/step-2-connection-method.png)

### Paso 3

Baja hasta `Connection string` y cópiala.

- Evita copiar una URL que ya lleve la contraseña.
- Si no sabes la contraseña, usa `Reset password`.

![Paso 3 - Connection string](docs/images/step-3-connection-string.png)

### Paso 4

Rellena `plugins/Skupabase/config.yml`.

![Paso 4 - config.yml](docs/images/step-4-config-yml.png)

Ejemplo:

```yml
jdbc-url: "jdbc:postgresql://aws-1-sa-east-1.pooler.supabase.com:5432/postgres?user=postgres.jftqytycpvjjcjacojyd"
username: "postgres.jftqytycpvjjcjacojyd"
password: "tu_password"
connect-timeout-seconds: 10
query-timeout-seconds: 30
max-result-rows: 500
```

## Uso

### Ejecutar query

```skript
set {_id} to supabase query "select * from public.players"
```

### Ver estado

```skript
send "%supabase query status {_id}%"
```

### Ver resultado

```skript
send "%supabase query result {_id}%"
```

### Ejecutar sin guardar ID

```skript
run supabase query "update public.players set coins = coins + 10 where uuid = '...'"
```

## Ejemplo rápido

```skript
command /sbtest:
    trigger:
        set {_id} to supabase query "select now() as server_time"
        wait 20 ticks
        send "%supabase query status {_id}%"
        send "%supabase query result {_id}%"
```

## Build

El jar compilado queda en:

```text
dist/skupabase-0.1.0.jar
```

## Imágenes

Guarda las capturas en:

```text
docs/images/
```


# Skupabase

Addon de Skript para ejecutar consultas SQL contra una base de datos de Supabase/Postgres desde scripts de Minecraft.

## Qué hace

- Ejecuta SQL libre desde Skript.
- Devuelve un `query id` para consultar el estado más tarde.
- Permite leer el resultado como JSON.
- Ejecuta la consulta en segundo plano para no bloquear el hilo principal del servidor.

## Requisitos

- Paper 1.20.x
- Skript 2.15.4 o compatible
- Una base de datos de Supabase con conexión PostgreSQL habilitada

## Instalación

1. Copia `skupabase-0.1.0.jar` a la carpeta `plugins/` de tu servidor.
2. Asegúrate de tener `Skript` instalado.
3. Inicia el servidor una vez para que se cree la carpeta del plugin.
4. Configura `plugins/Skupabase/config.yml`.
5. Reinicia el servidor.

## Configuración

Archivo: `plugins/Skupabase/config.yml`

```yml
jdbc-url: "jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres?sslmode=require"
username: "postgres"
password: "tu_password"
connect-timeout-seconds: 10
query-timeout-seconds: 30
max-result-rows: 500
```

### Nota sobre la conexión

Usa una URL JDBC de Postgres de Supabase. En la mayoría de casos debes incluir `sslmode=require`.

## Sintaxis disponible

### Ejecutar una query

```skript
set {_id} to supabase query "select * from public.players"
```

Esto devuelve un ID de consulta. La ejecución es asíncrona.

### Consultar estado

```skript
send "%supabase query status {_id}%"
```

Estados posibles:

- `pending`
- `running`
- `success`
- `error`
- `missing`

### Consultar resultado

```skript
send "%supabase query result {_id}%"
```

El resultado se devuelve como texto JSON.

### Ejecutar sin guardar ID

```skript
run supabase query "update public.players set coins = coins + 10 where uuid = '...'"
```

También puedes usar:

```skript
execute supabase query "delete from public.players where uuid = '...'"
```

## Ejemplo básico

```skript
command /sbtest:
    trigger:
        set {_id} to supabase query "select now() as server_time"
        send "&7Query enviada. ID: %{_id}%"

        wait 20 ticks

        send "&7Estado: %supabase query status {_id}%"
        send "&7Resultado: %supabase query result {_id}%"
```

## Ejemplo CRUD libre

```skript
command /sbcrud:
    trigger:
        set {_uuid} to "%uuid of player%"
        set {_name} to "%player%"

        set {_insertId} to supabase query "insert into public.skupabase_test (player_uuid, player_name) values ('%{_uuid}%', '%{_name}%') returning id"
        wait 20 ticks
        send "&aInsert: %supabase query result {_insertId}%"

        set {_selectId} to supabase query "select * from public.skupabase_test where player_uuid = '%{_uuid}%' order by id desc limit 1"
        wait 20 ticks
        send "&bSelect: %supabase query result {_selectId}%"

        set {_updateId} to supabase query "update public.skupabase_test set player_name = '%{_name}%_edited' where player_uuid = '%{_uuid}%'"
        wait 20 ticks
        send "&eUpdate: %supabase query result {_updateId}%"

        set {_deleteId} to supabase query "delete from public.skupabase_test where player_uuid = '%{_uuid}%'"
        wait 20 ticks
        send "&cDelete: %supabase query result {_deleteId}%"
```

## Recomendaciones

- Usa consultas simples y cortas.
- Si una consulta tarda, consulta primero `status` antes de leer `result`.
- Evita construir SQL con datos no confiables.
- Para uso serio, limita quién puede editar scripts que llamen a este addon.

## Limitaciones actuales

- La sintaxis es SQL libre, no una DSL de CRUD.
- El resultado se expone como JSON en texto.
- No hay parámetros enlazados todavía, así que debes construir la query en el script.

## Build

El jar compilado queda en:

```text
dist/skupabase-0.1.0.jar
```


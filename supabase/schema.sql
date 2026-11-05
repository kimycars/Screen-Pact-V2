-- ScrollBan: servidor de grupos en Supabase.
--
-- Qué guarda: por persona (identidad anónima, sin correo), los minutos de cada día por app con
-- límite, su límite, las llaves usadas y su media de antes; y sus retos con fecha. Lo ve solo
-- la gente con la que comparte grupo. Cada pacto es un grupo de 2; además hay grupos de 3 o más.
-- Qué NO guarda en claro: nombres, llaves ni nada que permita desbloquear. En los grupos de 3 o
-- más, cada miembro sube su nombre cifrado con el secreto del grupo (profile) y las invitaciones
-- llevan el secreto cifrado con la clave del pacto entre quien invita y quien recibe (box). El
-- servidor no tiene ninguna de esas claves.
--
-- Acceso: ninguna tabla se puede leer ni escribir directamente desde la app (RLS activo y sin
-- políticas). Todo pasa por las funciones de abajo, que comprueban auth.uid().
--
-- Se puede volver a ejecutar entero: no borra datos.

-- Tablas --------------------------------------------------------------------------------------

create table if not exists public.groups (
  id uuid primary key,
  -- sha256 del secreto del grupo. Cada móvil del pacto lo calcula con sus llaves; el servidor
  -- solo guarda el hash, así que ni viéndolo se puede entrar en el grupo.
  secret_hash text not null,
  created_at timestamptz not null default now()
);

create table if not exists public.group_members (
  group_id uuid not null references public.groups(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  joined_at timestamptz not null default now(),
  primary key (group_id, user_id)
);
create index if not exists group_members_user_idx on public.group_members(user_id);
-- Nombre del miembro cifrado para el resto del grupo (solo en grupos de 3 o más).
alter table public.group_members add column if not exists profile text check (char_length(profile) <= 600);

-- Invitación a un grupo de 3 o más: el secreto del grupo cifrado para quien la recibe.
create table if not exists public.group_invites (
  group_id uuid not null references public.groups(id) on delete cascade,
  to_user uuid not null references auth.users(id) on delete cascade,
  from_user uuid not null references auth.users(id) on delete cascade,
  box text not null check (char_length(box) between 1 and 1000),
  created_at timestamptz not null default now(),
  primary key (group_id, to_user)
);
create index if not exists group_invites_to_idx on public.group_invites(to_user);

create table if not exists public.day_usage (
  user_id uuid not null references auth.users(id) on delete cascade,
  day date not null,
  package text not null check (char_length(package) between 1 and 200),
  app_name text not null check (char_length(app_name) between 1 and 80),
  used_ms bigint not null check (used_ms between 0 and 86400000),
  limit_min int not null default 0 check (limit_min between 0 and 1440),
  unlocks int not null default 0 check (unlocks between 0 and 1000),
  baseline_ms bigint check (baseline_ms between 0 and 86400000),
  updated_at timestamptz not null default now(),
  primary key (user_id, day, package)
);

create table if not exists public.challenges (
  user_id uuid not null references auth.users(id) on delete cascade,
  id uuid not null,
  package text not null check (char_length(package) between 1 and 200),
  app_name text not null check (char_length(app_name) between 1 and 80),
  daily_minutes int not null check (daily_minutes between 0 and 1440),
  start_day date not null,
  days int not null check (days between 1 and 60),
  created_at timestamptz not null default now(),
  primary key (user_id, id)
);

alter table public.groups enable row level security;
alter table public.group_members enable row level security;
alter table public.day_usage enable row level security;
alter table public.challenges enable row level security;
alter table public.group_invites enable row level security;

revoke all on public.groups, public.group_members, public.day_usage, public.challenges, public.group_invites from anon, authenticated;

-- Funciones -----------------------------------------------------------------------------------

-- Entra en el grupo de un pacto. Si aún no existe, lo crea con ese secreto; si existe, el
-- secreto tiene que coincidir. Como mucho 8 personas por grupo y 50 grupos por persona.
create or replace function public.join_group(p_group uuid, p_secret text)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_hash text := encode(sha256(convert_to(p_secret, 'UTF8')), 'hex');
  v_stored text;
begin
  if v_uid is null then raise exception 'not signed in'; end if;
  if p_secret is null or char_length(p_secret) < 32 then raise exception 'bad secret'; end if;

  insert into groups (id, secret_hash) values (p_group, v_hash) on conflict (id) do nothing;
  select secret_hash into v_stored from groups where id = p_group;
  if v_stored is distinct from v_hash then raise exception 'bad secret'; end if;

  if exists (select 1 from group_members where group_id = p_group and user_id = v_uid) then return; end if;
  if (select count(*) from group_members where group_id = p_group) >= 8 then raise exception 'group full'; end if;
  if (select count(*) from group_members where user_id = v_uid) >= 50 then raise exception 'too many groups'; end if;

  insert into group_members (group_id, user_id) values (p_group, v_uid);
  delete from group_invites where group_id = p_group and to_user = v_uid;
end;
$$;

-- Guarda el nombre cifrado del que llama en un grupo suyo.
create or replace function public.set_profile(p_group uuid, p_profile text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  update group_members set profile = p_profile where group_id = p_group and user_id = auth.uid();
end;
$$;

-- Invita a alguien a un grupo de 3 o más. Solo se puede invitar a alguien con quien ya se
-- comparte otro grupo (en la práctica, un amigo con pacto). Como mucho 8 personas contando las
-- invitaciones pendientes.
create or replace function public.invite_to_group(p_group uuid, p_to uuid, p_box text)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
begin
  if v_uid is null then raise exception 'not signed in'; end if;
  if not exists (select 1 from group_members where group_id = p_group and user_id = v_uid) then raise exception 'not a member'; end if;
  if not exists (
    select 1 from group_members a join group_members b using (group_id)
    where a.user_id = v_uid and b.user_id = p_to and a.group_id <> p_group
  ) then raise exception 'not connected'; end if;
  if exists (select 1 from group_members where group_id = p_group and user_id = p_to) then return; end if;
  if (select count(*) from group_members where group_id = p_group)
     + (select count(*) from group_invites where group_id = p_group and to_user <> p_to) >= 8 then raise exception 'group full'; end if;
  insert into group_invites (group_id, to_user, from_user, box) values (p_group, p_to, v_uid, p_box)
  on conflict (group_id, to_user) do update set from_user = excluded.from_user, box = excluded.box, created_at = now();
end;
$$;

-- Rechaza una invitación.
create or replace function public.decline_invite(p_group uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  delete from group_invites where group_id = p_group and to_user = auth.uid();
end;
$$;

-- Sale de un grupo (al romper el pacto). Si se queda vacío, se borra.
create or replace function public.leave_group(p_group uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  delete from group_members where group_id = p_group and user_id = auth.uid();
  delete from groups g where g.id = p_group and not exists (select 1 from group_members m where m.group_id = g.id);
end;
$$;

-- Sube los minutos de los últimos días y el reto activo. Sustituye los datos propios de esos
-- días y borra los de hace más de 60 días.
-- p_usage: [{day, package, app_name, used_ms, limit_min, unlocks, baseline_ms}]
-- p_challenges: [{id, package, app_name, daily_minutes, start_day, days}]
create or replace function public.push_usage(p_usage jsonb, p_challenges jsonb)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
begin
  if v_uid is null then raise exception 'not signed in'; end if;
  if jsonb_array_length(p_usage) > 400 or jsonb_array_length(p_challenges) > 10 then raise exception 'too much'; end if;

  insert into day_usage (user_id, day, package, app_name, used_ms, limit_min, unlocks, baseline_ms, updated_at)
  select v_uid, r.day, r.package, left(r.app_name, 80), r.used_ms, r.limit_min, r.unlocks, r.baseline_ms, now()
  from jsonb_to_recordset(p_usage) as r(day date, package text, app_name text, used_ms bigint, limit_min int, unlocks int, baseline_ms bigint)
  where r.day between current_date - 60 and current_date + 1
  on conflict (user_id, day, package) do update set
    app_name = excluded.app_name,
    used_ms = excluded.used_ms,
    limit_min = excluded.limit_min,
    unlocks = excluded.unlocks,
    baseline_ms = excluded.baseline_ms,
    updated_at = now();

  delete from day_usage where user_id = v_uid and day < current_date - 60;

  delete from challenges where user_id = v_uid;
  insert into challenges (user_id, id, package, app_name, daily_minutes, start_day, days)
  select v_uid, c.id, c.package, left(c.app_name, 80), c.daily_minutes, c.start_day, c.days
  from jsonb_to_recordset(p_challenges) as c(id uuid, package text, app_name text, daily_minutes int, start_day date, days int);
end;
$$;

-- Todo lo que la app enseña en Juntos, en una sola llamada: mis grupos con sus miembros, y los
-- minutos y retos de los últimos p_days días de cada persona con la que comparto grupo (y míos).
create or replace function public.my_board(p_days int default 31)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_since date := current_date - least(greatest(p_days, 1), 60);
begin
  if v_uid is null then raise exception 'not signed in'; end if;
  return (
    with mine as (
      select group_id from group_members where user_id = v_uid
    ), members as (
      select m.group_id, m.user_id from group_members m join mine using (group_id)
    ), people as (
      select distinct user_id from members
    )
    select jsonb_build_object(
      'me', v_uid,
      'members', coalesce((select jsonb_agg(jsonb_build_object('g', m.group_id, 'u', m.user_id, 'p', gm.profile))
        from members m join group_members gm on gm.group_id = m.group_id and gm.user_id = m.user_id), '[]'::jsonb),
      'invites', coalesce((select jsonb_agg(jsonb_build_object('g', i.group_id, 'from', i.from_user, 'box', i.box))
        from group_invites i where i.to_user = v_uid), '[]'::jsonb),
      'usage', coalesce((
        select jsonb_agg(jsonb_build_object(
          'u', d.user_id, 'day', d.day, 'package', d.package, 'app_name', d.app_name,
          'used_ms', d.used_ms, 'limit_min', d.limit_min, 'unlocks', d.unlocks, 'baseline_ms', d.baseline_ms))
        from day_usage d where d.user_id in (select user_id from people) and d.day >= v_since), '[]'::jsonb),
      'challenges', coalesce((
        select jsonb_agg(jsonb_build_object(
          'u', c.user_id, 'id', c.id, 'package', c.package, 'app_name', c.app_name,
          'daily_minutes', c.daily_minutes, 'start_day', c.start_day, 'days', c.days))
        from challenges c where c.user_id in (select user_id from people)), '[]'::jsonb)
    )
  );
end;
$$;

-- Borra a la persona y todo lo suyo (al apagar el ranking en Ajustes). Los grupos que se
-- quedan vacíos se borran también.
create or replace function public.delete_my_data()
returns void
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_uid uuid := auth.uid();
begin
  if v_uid is null then raise exception 'not signed in'; end if;
  delete from auth.users where id = v_uid;
  delete from public.groups g where not exists (select 1 from public.group_members m where m.group_id = g.id);
end;
$$;

revoke all on function public.join_group(uuid, text), public.leave_group(uuid), public.push_usage(jsonb, jsonb),
  public.my_board(int), public.delete_my_data(), public.set_profile(uuid, text), public.invite_to_group(uuid, uuid, text),
  public.decline_invite(uuid) from public, anon;
grant execute on function public.join_group(uuid, text), public.leave_group(uuid), public.push_usage(jsonb, jsonb),
  public.my_board(int), public.delete_my_data(), public.set_profile(uuid, text), public.invite_to_group(uuid, uuid, text),
  public.decline_invite(uuid) to authenticated;

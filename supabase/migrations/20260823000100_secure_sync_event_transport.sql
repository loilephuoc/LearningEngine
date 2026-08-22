create table public.sync_user_revisions (
  user_id uuid primary key references auth.users(id) on delete cascade,
  last_revision bigint not null default 0 check (last_revision >= 0)
);

create table public.sync_events (
  user_id uuid not null references auth.users(id) on delete cascade,
  revision bigint not null check (revision > 0),
  event_id text not null check (event_id <> ''),
  idempotency_key text not null check (idempotency_key <> ''),
  origin_device_id text not null check (origin_device_id <> ''),
  namespace text not null check (namespace in ('CONTENT', 'MEDIA', 'LEARNING')),
  kind text not null check (kind in ('CONTENT_FIELD', 'MEDIA', 'REVIEW_EVENT')),
  entity_id text not null check (entity_id <> ''),
  payload_version integer not null check (payload_version > 0),
  payload jsonb not null check (jsonb_typeof(payload) = 'object'),
  created_at timestamptz not null default statement_timestamp(),
  primary key (user_id, revision),
  unique (user_id, event_id),
  unique (user_id, idempotency_key)
);

create index sync_events_user_revision_idx on public.sync_events(user_id, revision);

create table public.sync_device_cursors (
  user_id uuid not null references auth.users(id) on delete cascade,
  device_id text not null check (device_id <> ''),
  acknowledged_revision bigint not null default 0 check (acknowledged_revision >= 0),
  updated_at timestamptz not null default statement_timestamp(),
  primary key (user_id, device_id)
);

alter table public.sync_user_revisions enable row level security;
alter table public.sync_events enable row level security;
alter table public.sync_device_cursors enable row level security;

revoke all on public.sync_user_revisions, public.sync_events, public.sync_device_cursors from anon, authenticated;
grant select on public.sync_events, public.sync_device_cursors to authenticated;

create policy sync_events_select_own on public.sync_events for select to authenticated
  using ((select auth.uid()) = user_id);
create policy sync_cursors_select_own on public.sync_device_cursors for select to authenticated
  using ((select auth.uid()) = user_id);

create or replace function public.push_sync_events(p_events jsonb)
returns table(event_id text, revision bigint, duplicate boolean)
language plpgsql security definer set search_path = pg_catalog, public
as $$
declare
  owner_id uuid := auth.uid();
  item jsonb;
  existing public.sync_events%rowtype;
  allocated bigint;
begin
  if owner_id is null then raise exception 'authentication required' using errcode = '42501'; end if;
  if jsonb_typeof(p_events) <> 'array' or jsonb_array_length(p_events) > 100 then
    raise exception 'invalid event batch' using errcode = '22023';
  end if;
  for item in select value from jsonb_array_elements(p_events) loop
    if item ? 'user_id' then raise exception 'user identity is server-derived' using errcode = '22023'; end if;
    select * into existing from public.sync_events e
      where e.user_id = owner_id and (e.event_id = item->>'event_id' or e.idempotency_key = item->>'idempotency_key');
    if found then
      if existing.event_id <> item->>'event_id' or existing.idempotency_key <> item->>'idempotency_key'
         or existing.origin_device_id <> item->>'origin_device_id' or existing.namespace <> item->>'namespace'
         or existing.kind <> item->>'kind' or existing.entity_id <> item->>'entity_id'
         or existing.payload_version <> (item->>'payload_version')::integer or existing.payload <> item->'payload' then
        raise exception 'idempotency identity conflict' using errcode = '23505';
      end if;
      event_id := existing.event_id; revision := existing.revision; duplicate := true; return next;
      continue;
    end if;
    insert into public.sync_user_revisions(user_id, last_revision) values (owner_id, 1)
      on conflict (user_id) do update set last_revision = public.sync_user_revisions.last_revision + 1
      returning last_revision into allocated;
    insert into public.sync_events(user_id, revision, event_id, idempotency_key, origin_device_id, namespace, kind, entity_id, payload_version, payload)
      values (owner_id, allocated, item->>'event_id', item->>'idempotency_key', item->>'origin_device_id', item->>'namespace', item->>'kind', item->>'entity_id', (item->>'payload_version')::integer, item->'payload');
    event_id := item->>'event_id'; revision := allocated; duplicate := false; return next;
  end loop;
end;
$$;

create or replace function public.ack_sync_cursor(p_device_id text, p_acknowledged_revision bigint)
returns bigint language plpgsql security definer set search_path = pg_catalog, public
as $$
declare owner_id uuid := auth.uid(); result bigint; maximum bigint;
begin
  if owner_id is null then raise exception 'authentication required' using errcode = '42501'; end if;
  if p_device_id is null or p_device_id = '' or p_acknowledged_revision < 0 then raise exception 'invalid acknowledgement' using errcode = '22023'; end if;
  select coalesce(last_revision, 0) into maximum from public.sync_user_revisions where user_id = owner_id;
  if p_acknowledged_revision > coalesce(maximum, 0) then raise exception 'acknowledgement exceeds allocated revision' using errcode = '22023'; end if;
  insert into public.sync_device_cursors(user_id, device_id, acknowledged_revision)
    values(owner_id, p_device_id, p_acknowledged_revision)
    on conflict(user_id, device_id) do update set
      acknowledged_revision = greatest(public.sync_device_cursors.acknowledged_revision, excluded.acknowledged_revision),
      updated_at = case when excluded.acknowledged_revision > public.sync_device_cursors.acknowledged_revision then statement_timestamp() else public.sync_device_cursors.updated_at end
    returning acknowledged_revision into result;
  return result;
end;
$$;

revoke all on function public.push_sync_events(jsonb) from public, anon;
revoke all on function public.ack_sync_cursor(text, bigint) from public, anon;
grant execute on function public.push_sync_events(jsonb) to authenticated;
grant execute on function public.ack_sync_cursor(text, bigint) to authenticated;

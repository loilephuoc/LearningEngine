-- FAMILY / "Ngày đáng nhớ" v7. Apply with the Supabase SQL editor or CLI.
-- The Android client must use only a publishable/anon key. Never embed service_role.

create or replace function public.family_sync_touch_row()
returns trigger language plpgsql security invoker set search_path = public as $$
begin
  -- A stale active client is never allowed to resurrect an existing tombstone.
  if tg_op = 'UPDATE' and old.deleted_at_epoch_millis is not null and new.deleted_at_epoch_millis is null then
    new.deleted_at_epoch_millis := old.deleted_at_epoch_millis;
  end if;
  new.server_updated_at := clock_timestamp();
  new.updated_at := clock_timestamp();
  new.deleted_at := case when new.deleted_at_epoch_millis is null then null
    else to_timestamp(new.deleted_at_epoch_millis / 1000.0) end;
  return new;
end $$;

create table if not exists public.family_persons (
  owner_user_id uuid not null references auth.users(id), id text not null,
  full_name text not null, nickname text, group_type text not null, relationship_label text,
  birth_date_solar date, phone text, address text, note text, avatar_ref text,
  created_at_epoch_millis bigint not null, updated_at_epoch_millis bigint not null, deleted_at_epoch_millis bigint,
  created_at timestamptz not null default now(), updated_at timestamptz not null default now(), deleted_at timestamptz,
  server_updated_at timestamptz not null default clock_timestamp(), primary key (owner_user_id, id)
);
create table if not exists public.family_event_categories (
  owner_user_id uuid not null references auth.users(id), id text not null,
  name text not null, built_in_key text, built_in boolean not null default false, icon_key text, sort_order integer not null default 0,
  created_at_epoch_millis bigint not null, updated_at_epoch_millis bigint not null, deleted_at_epoch_millis bigint,
  created_at timestamptz not null default now(), updated_at timestamptz not null default now(), deleted_at timestamptz,
  server_updated_at timestamptz not null default clock_timestamp(), primary key (owner_user_id, id)
);
create table if not exists public.family_events (
  owner_user_id uuid not null references auth.users(id), id text not null,
  category_id text not null, title text not null, related_person_id text, related_person_name text,
  calendar_type text not null, solar_date date, lunar_day integer, lunar_month integer,
  lunar_leap_month boolean not null default false, source_year integer, recurrence text not null, note text,
  created_at_epoch_millis bigint not null, updated_at_epoch_millis bigint not null, deleted_at_epoch_millis bigint,
  created_at timestamptz not null default now(), updated_at timestamptz not null default now(), deleted_at timestamptz,
  server_updated_at timestamptz not null default clock_timestamp(), primary key (owner_user_id, id),
  check (calendar_type in ('SOLAR','LUNAR')), check (recurrence in ('NONE','MONTHLY','YEARLY'))
);
create table if not exists public.family_reminder_rules (
  owner_user_id uuid not null references auth.users(id), id text not null,
  target_type text not null, target_id text not null, amount integer not null check (amount >= 0), offset_unit text not null,
  remind_hour integer not null check (remind_hour between 0 and 23), remind_minute integer not null check (remind_minute between 0 and 59),
  enabled boolean not null, repeat_mode text not null, occurrence_date date,
  created_at_epoch_millis bigint not null, updated_at_epoch_millis bigint not null, deleted_at_epoch_millis bigint,
  created_at timestamptz not null default now(), updated_at timestamptz not null default now(), deleted_at timestamptz,
  server_updated_at timestamptz not null default clock_timestamp(), primary key (owner_user_id, id),
  check (target_type in ('PERSON_BIRTHDAY','EVENT','TASK')), check (offset_unit in ('DAY','WEEK','MONTH')),
  check (repeat_mode in ('FOLLOW_TARGET','ONCE')), check (repeat_mode <> 'ONCE' or occurrence_date is not null)
);
create table if not exists public.family_tasks (
  owner_user_id uuid not null references auth.users(id), id text not null,
  title text not null, description text, start_at timestamp, due_at timestamp, recurrence text not null,
  status text not null, priority text not null, related_person_id text, related_event_id text,
  completed_at timestamp, note text,
  created_at_epoch_millis bigint not null, updated_at_epoch_millis bigint not null, deleted_at_epoch_millis bigint,
  created_at timestamptz not null default now(), updated_at timestamptz not null default now(), deleted_at timestamptz,
  server_updated_at timestamptz not null default clock_timestamp(), primary key (owner_user_id, id),
  check (recurrence in ('NONE','MONTHLY','YEARLY')), check (status in ('TODO','IN_PROGRESS','DONE','CANCELLED')),
  check (priority in ('LOW','NORMAL','HIGH','URGENT'))
);
create table if not exists public.family_checklist_items (
  owner_user_id uuid not null references auth.users(id), id text not null, task_id text not null,
  item_text text not null, completed boolean not null, sort_order integer not null,
  created_at_epoch_millis bigint not null, updated_at_epoch_millis bigint not null, deleted_at_epoch_millis bigint,
  created_at timestamptz not null default now(), updated_at timestamptz not null default now(), deleted_at timestamptz,
  server_updated_at timestamptz not null default clock_timestamp(), primary key (owner_user_id, id)
);
create table if not exists public.family_task_occurrence_completions (
  owner_user_id uuid not null references auth.users(id), id text not null, task_id text not null,
  occurrence_datetime timestamp not null, completed_at timestamp not null,
  created_at_epoch_millis bigint not null, updated_at_epoch_millis bigint not null, deleted_at_epoch_millis bigint,
  created_at timestamptz not null default now(), updated_at timestamptz not null default now(), deleted_at timestamptz,
  server_updated_at timestamptz not null default clock_timestamp(), primary key (owner_user_id, id)
);

do $$ declare table_name text;
begin
  foreach table_name in array array['family_persons','family_event_categories','family_events','family_reminder_rules','family_tasks','family_checklist_items','family_task_occurrence_completions'] loop
    execute format('alter table public.%I enable row level security', table_name);
    execute format('drop policy if exists family_owner_select on public.%I', table_name);
    execute format('drop policy if exists family_owner_insert on public.%I', table_name);
    execute format('drop policy if exists family_owner_update on public.%I', table_name);
    execute format('drop policy if exists family_owner_delete on public.%I', table_name);
    execute format('create policy family_owner_select on public.%I for select to authenticated using (owner_user_id = (select auth.uid()))', table_name);
    execute format('create policy family_owner_insert on public.%I for insert to authenticated with check (owner_user_id = (select auth.uid()))', table_name);
    execute format('create policy family_owner_update on public.%I for update to authenticated using (owner_user_id = (select auth.uid())) with check (owner_user_id = (select auth.uid()))', table_name);
    execute format('create policy family_owner_delete on public.%I for delete to authenticated using (owner_user_id = (select auth.uid()))', table_name);
    execute format('create index if not exists %I on public.%I (owner_user_id, server_updated_at)', table_name || '_owner_server_idx', table_name);
    execute format('create index if not exists %I on public.%I (owner_user_id, deleted_at)', table_name || '_owner_deleted_idx', table_name);
    execute format('drop trigger if exists family_sync_touch on public.%I', table_name);
    execute format('create trigger family_sync_touch before update on public.%I for each row execute function public.family_sync_touch_row()', table_name);
  end loop;
end $$;

create index if not exists family_reminder_rules_target_idx on public.family_reminder_rules (owner_user_id, target_type, target_id);
create index if not exists family_checklist_items_task_idx on public.family_checklist_items (owner_user_id, task_id);
create index if not exists family_task_completions_task_idx on public.family_task_occurrence_completions (owner_user_id, task_id);
create index if not exists family_events_person_idx on public.family_events (owner_user_id, related_person_id);
create index if not exists family_tasks_person_idx on public.family_tasks (owner_user_id, related_person_id);
create index if not exists family_tasks_event_idx on public.family_tasks (owner_user_id, related_event_id);

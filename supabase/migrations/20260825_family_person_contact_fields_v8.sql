-- FAMILY / "Ngày đáng nhớ" v8 — flexible Person contact fields.
-- Apply after 20260825_family_sync_v7.sql. Android continues to use only publishable/anon key.

create table if not exists public.family_person_contact_fields (
  owner_user_id uuid not null references auth.users(id),
  id text not null,
  person_id text not null,
  field_type text not null,
  label text,
  field_value text not null,
  is_primary boolean not null default false,
  sort_order integer not null default 0,
  created_at_epoch_millis bigint not null,
  updated_at_epoch_millis bigint not null,
  deleted_at_epoch_millis bigint,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  server_updated_at timestamptz not null default clock_timestamp(),
  primary key (owner_user_id, id),
  check (field_type in ('PHONE','EMAIL','ADDRESS','WEBSITE','COMPANY','JOB_TITLE','CUSTOM')),
  check (length(trim(field_value)) > 0),
  check (field_type <> 'CUSTOM' or (label is not null and length(trim(label)) > 0))
);

alter table public.family_person_contact_fields enable row level security;

drop policy if exists family_owner_select on public.family_person_contact_fields;
drop policy if exists family_owner_insert on public.family_person_contact_fields;
drop policy if exists family_owner_update on public.family_person_contact_fields;
drop policy if exists family_owner_delete on public.family_person_contact_fields;

create policy family_owner_select on public.family_person_contact_fields
  for select to authenticated using (owner_user_id = (select auth.uid()));
create policy family_owner_insert on public.family_person_contact_fields
  for insert to authenticated with check (owner_user_id = (select auth.uid()));
create policy family_owner_update on public.family_person_contact_fields
  for update to authenticated
  using (owner_user_id = (select auth.uid()))
  with check (owner_user_id = (select auth.uid()));
create policy family_owner_delete on public.family_person_contact_fields
  for delete to authenticated using (owner_user_id = (select auth.uid()));

create index if not exists family_person_contact_fields_owner_server_idx
  on public.family_person_contact_fields (owner_user_id, server_updated_at);
create index if not exists family_person_contact_fields_owner_deleted_idx
  on public.family_person_contact_fields (owner_user_id, deleted_at);
create index if not exists family_person_contact_fields_person_idx
  on public.family_person_contact_fields (owner_user_id, person_id, sort_order);

drop trigger if exists family_sync_touch on public.family_person_contact_fields;
create trigger family_sync_touch
  before update on public.family_person_contact_fields
  for each row execute function public.family_sync_touch_row();

insert into storage.buckets(id, name, public, file_size_limit, allowed_mime_types)
values (
  'sync-media',
  'sync-media',
  false,
  52428800,
  array['audio/mpeg', 'audio/wav', 'image/jpeg', 'image/png', 'image/webp']
)
on conflict (id) do update set
  public = false,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

create policy sync_media_select_own on storage.objects for select to authenticated
  using (
    bucket_id = 'sync-media'
    and (storage.foldername(name))[1] = (select auth.uid())::text
    and array_length(storage.foldername(name), 1) = 1
  );

create policy sync_media_insert_own on storage.objects for insert to authenticated
  with check (
    bucket_id = 'sync-media'
    and (storage.foldername(name))[1] = (select auth.uid())::text
    and array_length(storage.foldername(name), 1) = 1
    and storage.filename(name) ~ '^[0-9a-f]{64}$'
  );

-- Deliberately no UPDATE or DELETE policy: upload is content-addressed and remote GC is deferred.

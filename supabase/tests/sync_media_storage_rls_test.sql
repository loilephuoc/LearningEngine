begin;
select plan(7);

select is((select public from storage.buckets where id = 'sync-media'), false, 'bucket is private');
select policies_are('storage', 'objects', array['sync_media_insert_own', 'sync_media_select_own']);

insert into auth.users(id) values
  ('00000000-0000-0000-0000-000000000001'),
  ('00000000-0000-0000-0000-000000000002');

set local role authenticated;
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000001', true);
select lives_ok($$ insert into storage.objects(bucket_id, name, owner_id) values ('sync-media', '00000000-0000-0000-0000-000000000001/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', '00000000-0000-0000-0000-000000000001') $$, 'owner inserts content-addressed object');
select throws_ok($$ insert into storage.objects(bucket_id, name, owner_id) values ('sync-media', '00000000-0000-0000-0000-000000000002/bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', '00000000-0000-0000-0000-000000000001') $$, '42501', null, 'owner cannot insert another prefix');
select is((select count(*) from storage.objects), 1::bigint, 'owner lists own object');

select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000002', true);
select is((select count(*) from storage.objects), 0::bigint, 'other user cannot list object');
select throws_ok($$ delete from storage.objects where bucket_id = 'sync-media' $$, '42501', null, 'authenticated user cannot delete remote blobs');

set local role anon;
select is((select count(*) from storage.objects), 0::bigint, 'anon cannot list objects');

select * from finish();
rollback;

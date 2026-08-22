begin;
select plan(12);

select has_table('public', 'sync_events');
select has_table('public', 'sync_device_cursors');
select has_function('public', 'push_sync_events', array['jsonb']);
select has_function('public', 'ack_sync_cursor', array['text', 'bigint']);
select policies_are('public', 'sync_events', array['sync_events_select_own']);
select policies_are('public', 'sync_device_cursors', array['sync_cursors_select_own']);

insert into auth.users(id) values
  ('00000000-0000-0000-0000-000000000001'),
  ('00000000-0000-0000-0000-000000000002');

set local role authenticated;
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000001', true);
select lives_ok($$ select * from public.push_sync_events('[{"event_id":"e1","idempotency_key":"k1","origin_device_id":"d1","namespace":"CONTENT","kind":"CONTENT_FIELD","entity_id":"c1","payload_version":1,"payload":{"field":"QUESTION","operation":"SET","value":"q"}}]'::jsonb) $$);
select is((select count(*) from public.sync_events), 1::bigint, 'owner reads own event');
select is((select revision from public.push_sync_events('[{"event_id":"e1","idempotency_key":"k1","origin_device_id":"d1","namespace":"CONTENT","kind":"CONTENT_FIELD","entity_id":"c1","payload_version":1,"payload":{"field":"QUESTION","operation":"SET","value":"q"}}]'::jsonb)), 1::bigint, 'retry keeps revision');
select is((select count(*) from public.sync_events), 1::bigint, 'retry does not duplicate');
select is(public.ack_sync_cursor('d1', 1), 1::bigint, 'owner ACK accepted');

select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000002', true);
select is((select count(*) from public.sync_events), 0::bigint, 'other user cannot read event');
select is((select count(*) from public.sync_device_cursors), 0::bigint, 'other user cannot read cursor');

select * from finish();
rollback;

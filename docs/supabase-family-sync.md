# Supabase FAMILY sync v7 setup

FAMILY remains local-first. Supabase is an authenticated backup/multi-device transport; Calendar and Android reminders never query it directly.

1. Create a Supabase project and enable Email + Password authentication.
2. Apply `supabase/migrations/20260825_family_sync_v7.sql` using the Supabase CLI or SQL editor.
3. In local `gradle.properties`, or as environment variables, provide:

   ```properties
   LEARNING_ENGINE_SUPABASE_URL=https://YOUR_PROJECT.supabase.co
   LEARNING_ENGINE_SUPABASE_PUBLISHABLE_KEY=YOUR_PUBLIC_PUBLISHABLE_OR_ANON_KEY
   ```

4. Never use `service_role`, a database password, or an admin token in Android configuration.
5. Build/install Android, open **Ngày đáng nhớ → cloud icon**, and sign in with an Email + Password test account.
6. Use **Đồng bộ ngay** for the first merge. Existing local rows are uploaded when the remote account is empty; local data is never cleared merely because remote is empty.
7. Validate two-device convergence, offline outbox replay, tombstone propagation, and recurring-task completions.
8. Validate RLS with two accounts: A must not select/update/delete B rows or insert a row with B's `owner_user_id`.

The migration enables RLS and owner policies on all seven tables. It also assigns server-observed `server_updated_at` values and prevents an older active copy from resurrecting an existing tombstone.

Session tokens are encrypted with an Android Keystore AES-GCM key. `family-sync-v1.json` stores only owner/outbox/sync metadata and never stores tokens or duplicate entity payloads.

Known v7 limitation: `avatarRef` is preserved as text. Device-local avatar files are not uploaded to Supabase Storage.

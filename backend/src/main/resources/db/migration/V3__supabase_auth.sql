-- Supabase Auth now owns authentication (passwords, sessions, email verification, password
-- reset). The local users table becomes a lightweight profile keyed 1:1 to auth.users.id —
-- this requires a real Supabase project (the auth schema doesn't exist on plain Postgres).

ALTER TABLE users RENAME TO profiles;
ALTER TABLE profiles ALTER COLUMN id DROP DEFAULT;
ALTER TABLE profiles DROP COLUMN password_hash;
ALTER TABLE profiles DROP COLUMN email_verified;

DROP TABLE auth_tokens;

-- Creates a profile row automatically whenever Supabase Auth creates a user, so the two are
-- never out of sync. Self-registration always defaults to CITIZEN — admin accounts have their
-- role promoted separately (seeder or an admin action), matching the app's existing rule.
CREATE OR REPLACE FUNCTION public.handle_new_auth_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER SET search_path = public
AS $$
BEGIN
  INSERT INTO public.profiles (id, full_name, email, phone, role, is_active)
  VALUES (
    NEW.id,
    COALESCE(NEW.raw_user_meta_data->>'full_name', 'New User'),
    NEW.email,
    NEW.raw_user_meta_data->>'phone',
    'CITIZEN',
    TRUE
  );
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_auth_user();

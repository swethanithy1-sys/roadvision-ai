-- V3 created profiles as a 1:1 mirror of auth.users but never enforced the link, so deleting a
-- Supabase Auth user left its profile row (and that user's reports) orphaned behind it. Tie the
-- two together so a profile's lifecycle always follows the auth user it belongs to.

DELETE FROM profiles
WHERE id NOT IN (SELECT id FROM auth.users);

ALTER TABLE profiles
    ADD CONSTRAINT profiles_auth_user_fk
    FOREIGN KEY (id) REFERENCES auth.users(id) ON DELETE CASCADE;

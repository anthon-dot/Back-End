-- ==============================================================================
-- SUPABASE STORAGE BUCKET & POLICIES SETUP
-- ==============================================================================

-- 1. Create the 'uploads' bucket if it doesn't already exist
INSERT INTO storage.buckets (id, name, public)
VALUES ('uploads', 'uploads', true)
ON CONFLICT (id) DO UPDATE SET public = true;

-- 2. Allow authenticated users to upload files to 'uploads'
DROP POLICY IF EXISTS "Allow authenticated users to upload files" ON storage.objects;
CREATE POLICY "Allow authenticated users to upload files"
ON storage.objects
FOR INSERT
TO authenticated
WITH CHECK (bucket_id = 'uploads');

-- 3. Allow public or authenticated read access to files in 'uploads'
DROP POLICY IF EXISTS "Allow public read access to uploaded files" ON storage.objects;
CREATE POLICY "Allow public read access to uploaded files"
ON storage.objects
FOR SELECT
TO public
USING (bucket_id = 'uploads');

-- 4. Allow authenticated users to update or delete their own uploads
DROP POLICY IF EXISTS "Allow users to delete or update files" ON storage.objects;
CREATE POLICY "Allow users to delete or update files"
ON storage.objects
FOR DELETE
TO authenticated
USING (bucket_id = 'uploads');

# Rental Management Backend

Spring Boot backend for the rental management system.

> [!NOTE]
> **Architecture Note:** Production deployment for this application has migrated to a **Supabase-Only Architecture** (Supabase Auth, PostgreSQL with Row Level Security, Supabase Storage, and Deno/Hono Edge Functions). See the root [`README.md`](../README.md) and [`SUPABASE_INTEGRATION_GUIDE.md`](../SUPABASE_INTEGRATION_GUIDE.md) for full serverless details.

## Requirements

- Java 21
- Maven wrapper included in this repository
- Supabase PostgreSQL connection details

## Local Development (Java Mode)

Set the required environment variables before starting the app:

```bash
export DATABASE_URL="jdbc:postgresql://your-supabase-host:5432/postgres"
export DATABASE_USERNAME="your-supabase-username"
export DATABASE_PASSWORD="your-supabase-password"
export JWT_SECRET="replace-with-at-least-32-characters"
export CORS_ALLOWED_ORIGINS="http://localhost:5173,http://localhost:3000"
export ADMIN_USERNAME="admin"
export ADMIN_PASSWORD="change-this-local-password"
```

On Windows PowerShell, use `$env:DATABASE_URL="..."` for each variable.

Then run:

```bash
./mvnw spring-boot:run
```

The app runs locally on `http://localhost:8083` unless `PORT` is set.

## Supabase-Only Deployment

Production is deployed directly on Supabase without requiring a middleman Docker/Render web server:

1. **Database & Migrations**:
   ```bash
   npm run supabase:db:push
   ```
2. **Edge Functions (Hono + TypeScript)**:
   ```bash
   npm run supabase:functions:deploy
   ```
3. **CI/CD**:
   Automated via [`.github/workflows/deploy-supabase.yml`](../.github/workflows/deploy-supabase.yml) on push to `main`.

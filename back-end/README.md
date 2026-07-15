# Rental Management Backend

Spring Boot backend for the rental management system.

## Requirements

- Java 21
- Maven wrapper included in this repository
- Supabase PostgreSQL connection details

## Local Development

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

## Render Deployment

This repository includes a root-level `render.yaml` for a Render web service. Render's current Blueprint native runtimes do not include Java, so this project deploys the Spring Boot app as a Docker web service using Java 21.

The Docker build uses the Maven wrapper:

```bash
./mvnw clean package -DskipTests
```

The container starts the packaged Spring Boot jar:

```bash
java -jar app.jar
```

Configure these Render environment variables:

```text
DATABASE_URL=jdbc:postgresql://your-supabase-host:5432/postgres
DATABASE_USERNAME=your-supabase-username
DATABASE_PASSWORD=your-supabase-password
JWT_SECRET=replace-with-at-least-32-characters
CORS_ALLOWED_ORIGINS=https://your-frontend.onrender.com
ADMIN_USERNAME=admin
ADMIN_PASSWORD=change-this-production-password
```

`DATABASE_URL` must be a JDBC URL for Spring Boot. If Supabase gives a URL beginning with `postgresql://`, convert it to `jdbc:postgresql://` and keep the username and password in `DATABASE_USERNAME` and `DATABASE_PASSWORD`.

Render provides `PORT` automatically. The application uses `server.port=${PORT:8083}`, so it runs on Render's assigned port in production and `8083` locally.

## Health Check

Use this endpoint for Render health checks:

```text
GET /api/health
```

Expected response:

```json
{"status":"UP"}
```

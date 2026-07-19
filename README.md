# SE2030 Web-Based Dental Management System

## Repository Structure
```
/ (Repository Root)
├── .gitignore
├── .env.example   # Safe local environment template variables
├── README.md
├── /backend      # Spring Boot Java 21 REST backend
└── /frontend     # React / Vite / Bootstrap frontend
```

## Prerequisite Tools
- Java Development Kit (JDK) 21 (or compatible build-host JDK, e.g. JDK 24)
- Apache Maven 3.9.x
- Node.js 20.x, 22.x, or 24.x
- npm 10.x or 11.x
- MySQL 8.0 Database Server

## Database Configuration
The application requires a MySQL database. In local development:
1. Create a MySQL schema named `dental_management_dev`.
2. Create a local database user with standard privileges.
3. Define the connection parameters using environment variables:
   - `DB_URL`: JDBC database URL (e.g. `jdbc:mysql://localhost:3306/dental_management_dev`)
   - `DB_USERNAME`: Database login username
   - `DB_PASSWORD`: Database login password

### Setting Environment Variables
Before running the backend, set these variables in your shell:
- **PowerShell (Windows)**:
  ```powershell
  $env:DB_URL="jdbc:mysql://localhost:3306/dental_management_dev"
  $env:DB_USERNAME="dental_user"
  $env:DB_PASSWORD="dental_password"
  ```
- **Command Prompt (Windows)**:
  ```cmd
  set DB_URL=jdbc:mysql://localhost:3306/dental_management_dev
  set DB_USERNAME=dental_user
  set DB_PASSWORD=dental_password
  ```
- **Bash (Linux/macOS)**:
  ```bash
  export DB_URL="jdbc:mysql://localhost:3306/dental_management_dev"
  export DB_USERNAME="dental_user"
  export DB_PASSWORD="dental_password"
  ```

*WARNING: Real credentials and passwords must never be committed to the repository. Always use local environment variables or git-ignored `.env` configurations.*

## Database Migrations
Database schemas are managed exclusively via **Flyway**.
- SQL migration scripts are located at `/backend/src/main/resources/db/migration`.
- Flyway migrations are run automatically during backend startup when the datasource is resolved.
- *Do not execute manual SQL modifications on the schema outside of Flyway.*

## Startup Order
To run the full local stack:
1. Ensure your MySQL database is active and schema created.
2. Export/set the connection environment variables in your terminal.
3. Start the backend:
   ```bash
   cd backend
   .\mvnw spring-boot:run
   ```
4. Start the frontend:
   ```bash
   cd frontend
   npm run dev
   ```
5. Open your browser and navigate to the local frontend address (Vite default is `http://localhost:5173` or `5174`).

## Vite Proxy Configuration
The frontend dev server uses a proxy configured in `vite.config.js` to route all browser requests starting with `/api` relative paths to the backend server at `http://localhost:8080`. This removes the need for hardcoded origins or CORS setup during local development.

## Staff User Administration
The system provides restricted administrative capabilities to manage staff accounts (`ADMIN`, `RECEPTIONIST`, `DENTIST`, `DENTAL_ASSISTANT`). Patient (`PATIENT`) role management remains out of scope for this module.

### API Routes (ADMIN only)
- `POST /api/admin/staff-users` — Create a staff user. Requires valid CSRF.
- `GET /api/admin/staff-users` — List staff users. Paginated and deterministically sorted by username.
- `GET /api/admin/staff-users/{id}` — Inspect details of a specific staff user.
- `PATCH /api/admin/staff-users/{id}/status` — Activate or deactivate a staff user account. Requires valid CSRF.
- `PATCH /api/admin/staff-users/{id}/role` — Change the role of a staff user. Requires valid CSRF.

### Policy Rules
- **Non-Destructive Deactivation**: Accounts are deactivated (`active=false`) instead of deleted. Deactivated accounts are blocked from login authentication.
- **Self-Modification Protection**: Administrators cannot demote or deactivate their own currently active session account.
- **Final Active ADMIN Protection**: The final active `ADMIN` account cannot be deactivated or demoted, preventing lockouts.
- **CSRF Requirements**: Valid CSRF tokens (obtained via `/api/auth/csrf`) are required for all state-changing endpoints.
- **Auditing**: Authenticated administrators are recorded automatically in creation and modification fields.

---
*Note: Patient Registry, Appointment Scheduling, Treatments, Prescriptions, and core Dental Business Modules are not implemented yet.*

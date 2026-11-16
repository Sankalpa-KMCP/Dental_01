# Dental Management System

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![React](https://img.shields.io/badge/React-18%2F19-61DAFB?style=flat-square&logo=react&logoColor=white)](https://react.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?style=flat-square&logo=flyway&logoColor=white)](https://flywaydb.org/)

A full-stack clinic administration platform providing role-based staff management, automated Flyway schema migrations, CSRF protection, and audit logging.

Built with **Java 21**, **Spring Boot 3**, **Spring Security**, **Flyway**, **MySQL 8**, and **React**.

---

## Repository Structure

```
/ (Repository Root)
+-- .gitignore
+-- .env.example   # Safe local environment template variables
+-- README.md
+-- /backend      # Spring Boot Java 21 REST backend
+-- /frontend     # React / Vite / Bootstrap frontend
```

---

## Prerequisite Tools

- **Java Development Kit (JDK)**: 21 or higher
- **Apache Maven**: 3.9.x
- **Node.js**: 20.x, 22.x, or 24.x
- **npm**: 10.x or 11.x
- **MySQL Database Server**: 8.0 or higher

---

## Database Configuration

The application requires a MySQL database. In local development:

1. Create a MySQL database schema named `dental_management_dev`.
2. Create a local database user with standard privileges.
3. Define connection parameters using environment variables:
   - `DB_URL`: JDBC database URL (e.g., `jdbc:mysql://localhost:3306/dental_management_dev`)
   - `DB_USERNAME`: Database username
   - `DB_PASSWORD`: Database password

### Setting Environment Variables

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

---

## Database Migrations

Database schemas are version-controlled via **Flyway**:
- SQL migration scripts are located at `/backend/src/main/resources/db/migration`.
- Migrations execute automatically during backend startup when the datasource resolves.
- Baseline tables and identity/role definitions are structured in `V1__baseline.sql` and `V2__create_identity_and_role_tables.sql`.

---

## Local Setup & Startup Order

To run the full stack locally:

1. Ensure the MySQL database is running with the schema created.
2. Export the connection environment variables in your terminal.
3. **Start the backend server:**
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
4. **Start the frontend client:**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
5. Open your browser and navigate to `http://localhost:5173`.

### Vite Proxy Routing
The frontend development server configures a reverse proxy in `vite.config.js` that forwards browser requests under `/api` directly to the Spring Boot backend at `http://localhost:8080`, preventing CORS complications in local workflows.

---

## Staff Administration & Access Control

The system provides restricted administrative capabilities to manage staff accounts (`ADMIN`, `RECEPTIONIST`, `DENTIST`, `DENTAL_ASSISTANT`).

### REST API Endpoints

| Method | Endpoint | Access Level | Description |
|---|---|---|---|
| `GET` | `/api/auth/csrf` | Public | Fetch active CSRF token for state-changing calls |
| `POST` | `/api/auth/login` | Public | Authenticate user credentials and establish session |
| `POST` | `/api/admin/staff-users` | `ADMIN` | Provision a new staff user account (Requires CSRF) |
| `GET` | `/api/admin/staff-users` | `ADMIN` | Paginated and sorted staff directory list |
| `GET` | `/api/admin/staff-users/{id}` | `ADMIN` | Inspect detailed profile of a specific staff user |
| `PATCH` | `/api/admin/staff-users/{id}/status` | `ADMIN` | Activate or deactivate staff account (Requires CSRF) |
| `PATCH` | `/api/admin/staff-users/{id}/role` | `ADMIN` | Update staff account role assignment (Requires CSRF) |

### Security & Governance Policies

- **Non-Destructive Deactivation**: Accounts are soft-deactivated (`active=false`) instead of dropped. Inactive accounts are immediately rejected by authentication handlers.
- **Self-Modification Guard**: Administrators cannot demote or deactivate their own active account session.
- **Final Active Administrator Guard**: The system prevents deactivation or demotion of the final active `ADMIN` user, eliminating administrative lockout scenarios.
- **Mandatory CSRF Validation**: All modifying operations (`POST`, `PUT`, `PATCH`, `DELETE`) require valid CSRF tokens.
- **Auditing**: JPA auditing automatically tracks and stores user identity and timestamp on entity creation and modification.

---

## License

This project is licensed under the [MIT License](LICENSE).

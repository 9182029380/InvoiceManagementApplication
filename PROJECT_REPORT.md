# Invoice Management — Project Report

Date: 2025-12-17

## 1. Purpose & Executive Summary

This document describes the Invoice Management application (Spring Boot) found in this repository. It provides a concise but complete reference for maintainers and stakeholders: high-level architecture, database schema and SQL DDL, full REST API specification (endpoints, request/response shapes and examples), JSON schemas/DTOs, wireframes for the web UI, technology stack and reasoning, deployment and runtime configuration guidance (Docker & Kubernetes manifests are present in the repo), security guidance, testing plan and recommended next steps.

Assumptions made when preparing this report:
- The application uses MySQL in production (a `k8s/mysql-deployment.yaml` exists in the repo). DDL below targets MySQL; small changes required for Postgres are noted.
- Money fields use DECIMAL(15,2) for accuracy. Java entities used `Double` — we recommend DECIMAL at DB level.
- `invoiceNumber` and `companyId` are strings of small fixed lengths (6 characters) per entity annotations; sizes given in DDL reflect that.
- No authentication or authorization was found in the code; the API is currently unauthenticated. Recommended options are listed in Security.


## 2. High-level Architecture

Components (from codebase):
- Spring Boot application (`com.example.invoice_management`) — controllers, services, repositories, entities, templates.
- Persistence: JPA/Hibernate with MySQL (k8s manifests show MySQL).
- EmailService: sends invoices via SMTP (implementation present in `service` package).
- PDFService: generates invoice PDFs and saves them to disk (service in repo).
- Web UI: Thymeleaf templates in `src/main/resources/templates/` provide server-side rendered pages.
- Dockerfile + docker-compose.yml + k8s manifests present for containerized deployment.

ASCII component diagram:

  [User Browser]
      |
      |  (Thymeleaf HTML)        [API Clients]
      |-----------------\          /
      |                  \        /
  [Spring Boot App]  <--- REST API ---> [3rd party: SMTP]
      |  (Services)
      |-- PDFService
      |-- EmailService
      |-- InvoiceService / PurchaseOrderService / CompanyService
      |
  [JPA/Hibernate]
      |
  [MySQL Database]
      |
  [Persistent storage for PDFs (container volume / host path)]

Component responsibilities:
- Controllers: HTTP layer for web and REST APIs (CompanyController, PurchaseOrderController, InvoiceController, WebController).
- Services: business logic — invoice generation, PO creation, email sending, PDF generation.
- Repositories: JPA repositories for CRUD.
- Templates: server-side UI pages for admin flows.


## 3. Database Schema (derived from entities)

Notes on mapping:
- Java `String` ⇒ VARCHAR
- Java `Long` ⇒ BIGINT (auto-increment)
- Java `LocalDate` ⇒ DATE
- Java `Double` ⇒ DECIMAL(15,2) in DB for monetary precision
- Enum fields stored as VARCHAR using EnumType.STRING

Tables: `our_company`, `client_company`, `purchase_order`, `invoice`.

DDL (MySQL)

-- Our Company
CREATE TABLE our_company (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id VARCHAR(6) NOT NULL UNIQUE,
  company_name VARCHAR(255) NOT NULL,
  address TEXT NOT NULL,
  pan_number VARCHAR(10) NOT NULL UNIQUE,
  gst_number VARCHAR(15) NOT NULL UNIQUE,
  bank_name VARCHAR(255) NOT NULL,
  account_number VARCHAR(64) NOT NULL,
  ifsc_code VARCHAR(32) NOT NULL,
  email VARCHAR(255) NOT NULL,
  phone VARCHAR(50) NOT NULL,
  created_date DATE,
  UNIQUE KEY uq_our_company_company_id (company_id),
  UNIQUE KEY uq_our_company_pan (pan_number),
  UNIQUE KEY uq_our_company_gst (gst_number)
);

-- Client Company
CREATE TABLE client_company (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_name VARCHAR(255) NOT NULL,
  address TEXT NOT NULL,
  pan_number VARCHAR(10),
  gst_number VARCHAR(15),
  email VARCHAR(255) NOT NULL,
  phone VARCHAR(50),
  created_date DATE
);

-- Purchase Order
CREATE TABLE purchase_order (
  po_number VARCHAR(64) PRIMARY KEY,
  client_company_id BIGINT NOT NULL,
  training_details TEXT NOT NULL,
  training_amount DECIMAL(15,2) NOT NULL,
  gst_percentage DECIMAL(5,2) NOT NULL,
  gst_amount DECIMAL(15,2) NOT NULL,
  total_amount DECIMAL(15,2) NOT NULL,
  po_date DATE,
  client_pan_number VARCHAR(10) NOT NULL,
  client_gst_number VARCHAR(15) NOT NULL,
  status VARCHAR(32),
  created_date DATE,
  CONSTRAINT fk_po_client FOREIGN KEY (client_company_id) REFERENCES client_company(id)
);

-- Invoice
CREATE TABLE invoice (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  invoice_number VARCHAR(6) NOT NULL UNIQUE,
  our_company_id BIGINT NOT NULL,
  purchase_order_id VARCHAR(64) NOT NULL,
  invoice_date DATE NOT NULL,
  subtotal DECIMAL(15,2) NOT NULL,
  gst_amount DECIMAL(15,2) NOT NULL,
  total_amount DECIMAL(15,2) NOT NULL,
  pdf_path VARCHAR(1024),
  status VARCHAR(32),
  created_date DATE,
  CONSTRAINT fk_invoice_our_company FOREIGN KEY (our_company_id) REFERENCES our_company(id),
  CONSTRAINT fk_invoice_po FOREIGN KEY (purchase_order_id) REFERENCES purchase_order(po_number),
  UNIQUE KEY uq_invoice_invoice_number (invoice_number)
);

Indexes:
- PKs provide base indexing.
- Unique indexes on `invoice_number`, `company_id`, `pan_number`, `gst_number` where defined.
- Consider indexes on `invoice.created_date`, `purchase_order.client_company_id`, `purchase_order.status` for reporting queries.

MySQL vs PostgreSQL differences:
- Use SERIAL or IDENTITY for auto-increment in Postgres, and TEXT lengths slightly differ. DECIMAL types are identical.


## 4. REST API Specification

Base: default server path (no global prefix in code). Controllers map to `/api/company`, `/api/po`, `/api/invoice`. All responses return JSON.

A. Company API (/api/company)

1) POST /api/company/our
- Description: Create our company (single owner company record).
- Request body (application/json): OurCompany entity fields.
- Example request JSON:
{
  "companyId": "ABC123",
  "companyName": "My Company Pvt Ltd",
  "address": "123 Main St, City",
  "panNumber": "ABCDE1234F",
  "gstNumber": "27ABCDE1234F1Z5",
  "bankName": "Bank Name",
  "accountNumber": "1234567890",
  "ifscCode": "IFSC0001",
  "email": "billing@example.com",
  "phone": "+91-9876543210"
}
- Success: 200 OK with created `OurCompany` JSON.
- Errors: 400 Bad Request if validation fails; 409 Conflict for unique constraint violation.

2) GET /api/company/our
- Description: Get the stored `OurCompany` (application expects a single record)
- Success: 200 OK with `OurCompany` JSON.
- Errors: 404 Not Found if not configured.

3) POST /api/company/client
- Description: Create a client company.
- Request body: `ClientCompany` JSON.
- Example:
{
  "companyName": "Client Co",
  "address": "Client address",
  "panNumber": "XXXPANNUM",
  "gstNumber": "27XXXXX",
  "email": "client@example.com",
  "phone": "0123456789"
}
- Success: 200 OK with created `ClientCompany` JSON.
- Errors: 400 Bad Request when required fields missing.

4) GET /api/company/client
- Description: List all client companies.
- Success: 200 OK with array of `ClientCompany` objects.

Curl example (create client):

curl -X POST "http://localhost:8080/api/company/client" \
  -H "Content-Type: application/json" \
  -d '{"companyName":"Client Co","address":"Addr","email":"client@example.com"}'


B. Purchase Order API (/api/po)

1) POST /api/po
- Description: Create Purchase Order. Request body is PurchaseOrder JSON. Note: `poNumber` is the PK and must be supplied.
- Request JSON example:
{
  "poNumber":"PO-2025-0001",
  "clientCompany": { "id": 1 },
  "trainingDetails":"Advanced Java Training",
  "trainingAmount":100000.00,
  "gstPercentage":18.0,
  "gstAmount":18000.00,
  "totalAmount":118000.00,
  "poDate":"2025-12-15",
  "clientPanNumber":"ABCDE1234F",
  "clientGstNumber":"27ABCDE1234F1Z5"
}
- Success: 200 OK with created PurchaseOrder JSON.
- Errors: 400 Bad Request (missing fields), 404 if client id unknown.

2) GET /api/po/{poNumber}
- Description: Get purchase order by PO number
- Success: 200 OK with PurchaseOrder JSON.
- Errors: 404 Not Found if PO not found.

3) GET /api/po
- Description: List all POs
- Success: 200 OK with array of PurchaseOrder objects.

Curl example (create PO):

curl -X POST "http://localhost:8080/api/po" \
  -H "Content-Type: application/json" \
  -d '{"poNumber":"PO-2025-0002","clientCompany":{"id":1},"trainingDetails":"...","trainingAmount":50000.0,"gstPercentage":18.0,"gstAmount":9000.0,"totalAmount":59000.0}'


C. Invoice API (/api/invoice)

1) POST /api/invoice/generate?companyId={companyId}&poNumber={poNumber}
- Description: Generate an Invoice for the PO using the `companyId` for our company.
- Request: no body; uses query params `companyId` and `poNumber`.
- Success: 200 OK with JSON map containing `invoice`, `pdfPath`, and `message`.
- Example curl:

curl -X POST "http://localhost:8080/api/invoice/generate?companyId=ABC123&poNumber=PO-2025-0001"

- Typical success response shape:
{
  "invoice": { /* Invoice object */ },
  "pdfPath": "/invoices/ABC123_INV001.pdf",
  "message": "Invoice generated successfully"
}

- On errors: 400 or 404 when PO or company not found or PO already invoiced.

2) POST /api/invoice/send/{invoiceId}
- Description: Ensures a PDF exists, sends email to client with invoice; sets invoice.status = SENT.
- Path param: invoiceId (Long)
- Success: 200 OK {"message": "Invoice sent successfully to client@example.com"}
- Errors: 404 if invoice not found, 500 on email failure.

3) GET /api/invoice/{id}
- Description: Get invoice by id
- Success: 200 OK with Invoice JSON

4) GET /api/invoice
- Description: List all invoices
- Success: 200 OK with array of Invoice JSON


D. Web routes (Thymeleaf) — summary (controllers mapped under `/web`)

Important templates located in `src/main/resources/templates/`:
- client-form.html
- company-setup.html
- dashboard.html
- invoice-generate.html
- invoice-list.html
- invoice-view.html
- po-form.html
- po-list.html

Key web paths (from `WebController`):
- GET /web/ — dashboard
- GET /web/company/setup — form to create our company
- POST /web/company/setup — save company
- GET /web/client/new — form to add client
- POST /web/client/save — save client
- GET /web/po/new — PO form
- POST /web/po/save — save PO
- GET /web/po/list — list POs
- GET /web/invoice/generate — generate invoice form
- POST /web/invoice/generate — generate invoice and redirect to view
- GET /web/invoice/view/{id} — view invoice
- POST /web/invoice/send/{id} — send invoice
- GET /web/invoice/list — invoice list


## 5. Data Models (JSON Schemas)

Notes: schema focuses on request/response validation for API consumers.

A) OurCompany (request/response)
- Required: companyId (6 chars), companyName, address, panNumber (10), gstNumber (15), bankName, accountNumber, ifscCode, email, phone

JSON Schema (excerpt):
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "OurCompany",
  "type": "object",
  "properties": {
    "companyId": {"type":"string","minLength":6,"maxLength":6},
    "companyName": {"type":"string"},
    "address": {"type":"string"},
    "panNumber": {"type":"string","maxLength":10},
    "gstNumber": {"type":"string","maxLength":15},
    "bankName": {"type":"string"},
    "accountNumber": {"type":"string"},
    "ifscCode": {"type":"string"},
    "email": {"type":"string","format":"email"},
    "phone": {"type":"string"}
  },
  "required": ["companyId","companyName","address","panNumber","gstNumber","bankName","accountNumber","ifscCode","email","phone"]
}

B) ClientCompany
- Required: companyName, address, email
- panNumber/gstNumber optional but validated for length

C) PurchaseOrder (request)
- Required: poNumber, clientCompany.id, trainingDetails, trainingAmount, gstPercentage, gstAmount, totalAmount, clientPanNumber, clientGstNumber
- Dates in ISO format: YYYY-MM-DD

D) Invoice generate
- Query params: companyId (string), poNumber (string)

E) Invoice (response)
- Fields: id, invoiceNumber, ourCompany (nested OurCompany), purchaseOrder (nested PurchaseOrder), invoiceDate (YYYY-MM-DD), subtotal, gstAmount, totalAmount, pdfPath, status (GENERATED|SENT|PAID), createdDate


## 6. Wireframes and User Flows

I'll provide ASCII wireframes and element lists for the main pages.

1) Dashboard (GET /web/)

[Header] Invoice Management
[Left nav] Dashboard | Company Setup | Clients | POs | Invoices
[Main]
- Our Company status (if not setup -> Setup button)
- Quick actions: New Client | New PO | Generate Invoice
- Summary cards: Total POs, Pending POs, Total Invoices, Sent, Paid
- Lists: Recent POs (5) | Recent Invoices (5)

User flow: Admin lands on dashboard → if company not setup → click "Company Setup" → fill form → redirected to dashboard → add clients → create PO → generate invoice

2) Company Setup (/web/company/setup)
[Form]
- Company ID (auto or manual) [text]
- Company Name [text]
- Address [textarea]
- PAN Number [text]
- GST Number [text]
- Bank Name, Account Number, IFSC
- Email, Phone
[Save button]

3) Client Form (/web/client/new)
[Form]
- Company Name, Address, PAN, GST, Email, Phone
[Save]

4) PO Form (/web/po/new)
[Form]
- PO Number [text]
- Client select [dropdown of clients]
- Training Details [textarea]
- Training Amount [number]
- GST% [number]
- GST Amount [calculated or input]
- Total Amount [calculated]
- PO Date [date]
- Client PAN, Client GST
[Save]

5) Invoice Generate (/web/invoice/generate)
[Form]
- PO select (only PENDING POs)
- Generate button
- On success redirect to /web/invoice/view/{id}

6) Invoice View (/web/invoice/view/{id})
[Header] Invoice Number / Date
[Sections]
- Our Company block (name, address, bank details)
- Bill To: Client details
- PO details / training details
- Line items / subtotal / GST / Total
- Buttons: Download PDF | Send Invoice (email)

7) Invoice List (/web/invoice/list)
[Table]
- Columns: Invoice#, PO#, Client, Date, Amount, Status, Actions(View, Send, Download)


## 7. Technology Stack & Justification

- Java 17+ and Spring Boot (app is Spring Boot) — rapid development, Spring Data JPA, MVC, Thymeleaf.
- Maven (`pom.xml`) — dependency management.
- Jakarta Persistence (JPA) + Hibernate — ORM used in code.
- Database: MySQL (k8s/mysql-deployment.yaml) — widely used, fits this app.
- Email: JavaMail or Spring Email (EmailService in code) — SMTP-based email sending.
- PDF generation: PDFService in repo — likely using iText or Apache PDFBox; check `pom.xml` for exact lib. (If not present, prefer OpenPDF / iText 2.x / Apache PDFBox.)
- Frontend: Thymeleaf server-side templates — simple admin UI and form handling.
- Containerization: Dockerfile and docker-compose.yml included. Kubernetes manifests exist under `k8s/` for production orchestration.

Why these choices:
- Spring Boot provides productivity and integrates with Thymeleaf and JPA easily.
- MySQL is consistent with provided k8s manifests and commonly available managed DBs.
- Containerization and k8s manifests make the app ready for cloud deployment.


## 8. Deployment & Runtime Configuration

Relevant files in repo:
- `Dockerfile` — container image build
- `docker-compose.yml` — local orchestration
- `k8s/*.yaml` — Kubernetes manifests for deployment, service, ingress, mysql
- `src/main/resources/application.yaml` — app configuration (properties)

Runtime environment variables (recommended):
- SPRING_DATASOURCE_URL (jdbc:mysql://host:3306/invoicedb)
- SPRING_DATASOURCE_USERNAME
- SPRING_DATASOURCE_PASSWORD
- SPRING_MAIL_HOST
- SPRING_MAIL_PORT
- SPRING_MAIL_USERNAME
- SPRING_MAIL_PASSWORD
- STORAGE_PATH (where PDFs are written) — map to container volume

Sample application.yaml hints (spring placeholders):

spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  mail:
    host: ${SPRING_MAIL_HOST}
    port: ${SPRING_MAIL_PORT}
    username: ${SPRING_MAIL_USERNAME}
    password: ${SPRING_MAIL_PASSWORD}

Logging and profiles:
- Use `spring.profiles.active` to separate `dev` and `prod`.

Docker & Kubernetes steps (high level):
1) Build jar: `./mvnw clean package -DskipTests` (or `mvnw.cmd` on Windows)
2) Build docker image: `docker build -t invoice-management:latest .`
3) Run locally: `docker-compose up` (ensure env vars set in docker-compose.yml)
4) For Kubernetes: apply manifests in `k8s/` with `kubectl apply -f k8s/namespace.yaml && kubectl apply -f k8s/secret.yaml && kubectl apply -f k8s/mysql-deployment.yaml && kubectl apply -f k8s/deployment.yaml && kubectl apply -f k8s/service.yaml && kubectl apply -f k8s/ingress.yaml` (adjust for your cluster)

Secrets: `k8s/secret.yaml` exists — ensure it stores DB and email credentials. Don't commit plaintext secrets to Git.

Storage for PDFs:
- Map a PersistentVolume to the path the application writes PDFs (check `PDFService` for path). In docker-compose use a volume; in k8s use PersistentVolumeClaim.


## 9. Security Considerations & Recommendations

Current state: no authentication/authorization present in controllers (public endpoints). Email credentials and DB passwords must be stored securely.

Recommendations:
- Authentication/Authorization: add Spring Security with one of:
  - Simple admin login (form-based) for web UI; protect `/web/**`.
  - Token-based API auth: JWT for clients; require Authorization header for `/api/**` endpoints.
  - OAuth2 if integrating with external identity providers.
- Input validation: add server-side validation (@Valid + constraint annotations like @NotNull, @Email, @Size) for request DTOs to prevent invalid data.
- Use HTTPS (TLS) for all incoming traffic (configure ingress/controller TLS certs via k8s ingress).
- CORS: configure allowed origins for API if it is to be used from browser-based SPAs.
- Secrets: use Kubernetes Secrets or a secret manager (Vault, AWS Secrets Manager) — do not store secrets in git.
- Database access: restrict DB network access to only app and administrative networks.
- Prevent file traversal: validate `pdfPath` and storage locations to prevent path injection.
- Email injection: sanitize email subject/body if user-provided inputs are included.


## 10. Testing Plan

Goals: ensure business logic correctness and prevent regressions.

Unit tests (service layer):
- Test InvoiceService.generateInvoice: given a valid PO and company, invoice is created with expected amounts and status.
- Test PurchaseOrderService.createPurchaseOrder: calculates GST and totals correctly if code performs calculation.

Integration tests (spring boot slice):
- Start application context with in-memory DB (H2) and test controller endpoints with MockMvc.
- Test create + read cycle for Company, Client, PO, Invoice.

End-to-end (E2E):
- Build image and run with docker-compose; create client, create PO, generate invoice, verify PDF exists, send invoice (mock SMTP or use a test SMTP server like MailHog).

Sample Maven commands:
- Run unit & integration tests: `./mvnw test`
- Run a specific test: `./mvnw -Dtest=InvoiceServiceTest test`

Example test cases (happy path & edge cases):
1) Generate invoice for PO that is already INVOICED → should error.
2) Create PO with missing client id → 404/400.
3) Send invoice where PDF generation fails → email not sent → appropriate exception handling.

Test data suggestions:
- Use a known OurCompany fixture and a ClientCompany fixture with known PAN/GST.
- Use H2 profile for CI to run fast tests: add `application-test.yaml` with H2 datasource.


## 11. Example SQL Samples & cURL

Create tables (see DDL above). Example insertion for OurCompany:

INSERT INTO our_company (company_id, company_name, address, pan_number, gst_number, bank_name, account_number, ifsc_code, email, phone, created_date)
VALUES ('ABC123','EDFORCE SOLUTIONS PRIVATE LIMITED','Address line','ABCDE1234F','27ABCDE1234F1Z5','SBI','123456789012','SBIN0001234','billing@edforce.com','9876543210', '2025-12-15');

cURL examples:
- Create client:
curl -X POST "http://localhost:8080/api/company/client" -H "Content-Type: application/json" -d '{"companyName":"Client Co","address":"Addr","email":"client@example.com"}'

- Create PO:
curl -X POST "http://localhost:8080/api/po" -H "Content-Type: application/json" -d '{"poNumber":"PO-2025-001","clientCompany":{"id":1},"trainingDetails":"Training","trainingAmount":10000.0,"gstPercentage":18.0,"gstAmount":1800.0,"totalAmount":11800.0}'

- Generate Invoice:
curl -X POST "http://localhost:8080/api/invoice/generate?companyId=ABC123&poNumber=PO-2025-001"

- Send Invoice:
curl -X POST "http://localhost:8080/api/invoice/send/1"


## 12. Backlog & Next Steps

Short term (high priority):
- Add input validation annotations and map controllers to DTOs with @Valid.
- Add authentication for API and admin UI (Spring Security) — at minimum basic auth for /web/* and API token for /api/*.
- Implement server-side money types: change entity Double → BigDecimal to avoid FP rounding issues.
- Add unit tests for services and basic integration tests for controllers.

Medium term:
- Add role-based access control (admin / billing / viewer).
- Improve PDF design and templating; externalize templates for invoicing line items.
- Add audit logs for invoice generation and sending.
- Add pagination for listing endpoints (PO list, invoice list).

Long term / Nice to have:
- Multi-company support (multiple our_company rows) and per-tenant isolation.
- Payment link integration and automated payment reconciliation.
- Reporting & export (CSV, Excel) and scheduled invoicing.


## 13. Appendix — Files & Locations

Key code locations (relative to repo root):
- Entities: `src/main/java/com/example/invoice_management/entity/`
- Controllers: `src/main/java/com/example/invoice_management/controller/`
- Services: `src/main/java/com/example/invoice_management/service/`
- Templates: `src/main/resources/templates/` (see list below)
- Dockerfile, docker-compose.yml, k8s/*.yaml at repo root / `k8s/`

Templates found:
- client-form.html
- company-setup.html
- dashboard.html
- invoice-generate.html
- invoice-list.html
- invoice-view.html
- po-form.html
- po-list.html


---

If you want, I can:
- Add stricter JSON Schemas for each DTO and create corresponding Java DTO classes with validation annotations (and wire controller methods to use them).
- Convert money fields from Double to BigDecimal across entities and update DB schema and tests.
- Generate Postman collection or OpenAPI (Swagger) spec from the controllers.
- Create a README snippet with step-by-step local run instructions (mvn + docker-compose).

Which follow-up should I implement next? (I can open a PR with code changes or create the OpenAPI spec automatically.)


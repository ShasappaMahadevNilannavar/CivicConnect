# CivicConnect — Complete REST API & Controller Endpoints Documentation

## 1. Authentication Endpoints

| Method | Endpoint | Access | Description | Request Body / Params |
|---|---|---|---|---|
| `GET` | `/` | Public | Landing / Home page | - |
| `GET` | `/login` | Public | Sign-in page | - |
| `POST` | `/login` | Public | Process user login | Form parameters: `username`, `password`, `_csrf` |
| `POST` | `/logout` | Authenticated | Sign out session | Form parameter: `_csrf` |
| `GET` | `/register` | Public | Citizen registration form | - |
| `POST` | `/register` | Public | Process citizen account registration | Form object: `RegisterRequest` (`name`, `email`, `password`, `confirmPassword`, `phone`, `address`) |

---

## 2. Citizen Endpoints (`/citizen/**` — Role: `ROLE_CITIZEN`)

| Method | Endpoint | Description | Request / Parameters |
|---|---|---|---|
| `GET` | `/citizen/dashboard` | Citizen home dashboard with metric counters and last 5 recent complaints | - |
| `GET` | `/citizen/complaints/new` | Problem reporting form with category dropdown, Leaflet location pin, and photo uploads | - |
| `POST` | `/citizen/complaints` | Submit a new community complaint with photo attachments | Multipart form: `title`, `description`, `categoryId`, `latitude`, `longitude`, `locationDescription`, `images` (file array) |
| `GET` | `/citizen/complaints` | Search, filter, and paginate citizen's filed reports | Query params: `search`, `status`, `page` (default 0), `size` (default 10) |
| `GET` | `/citizen/complaints/{referenceId}` | Detailed complaint page with photos, Leaflet map, status history timeline, and verification card | Path variable: `referenceId` (e.g. `CC-20261101-A101`) |
| `POST` | `/citizen/complaints/{referenceId}/feedback` | Citizen verification feedback (confirm resolution or dispute outcome) | Form object: `FeedbackRequest` (`resolved` [boolean], `comments` [string]) |
| `GET` | `/citizen/profile` | View citizen profile | - |
| `POST` | `/citizen/profile` | Update profile contact details | Form object: `ProfileUpdateRequest` (`name`, `phone`, `address`) |
| `GET` | `/citizen/notifications` | View unread notification list | - |
| `POST` | `/citizen/notifications/{id}/read` | Mark individual notification as read | Path variable: `id` |
| `POST` | `/citizen/notifications/read-all` | Mark all unread notifications as read | - |

---

## 3. Administrator Endpoints (`/admin/**` — Role: `ROLE_ADMIN`)

| Method | Endpoint | Description | Request / Parameters |
|---|---|---|---|
| `GET` | `/admin/dashboard` | Metrics overview (Total, Submitted, Under Review, In Progress, Resolved, Rejected), Chart.js Status doughnut chart, Category bar chart, Priority breakdown | - |
| `GET` | `/admin/complaints` | Comprehensive reports queue with search and multi-criteria filters | Query params: `search`, `status`, `category`, `priority`, `page`, `size` |
| `GET` | `/admin/complaints/{referenceId}` | Management console for a report: smart duplicate suggestions, status workflow engine, department dispatch, evidence photos, audit trail | Path variable: `referenceId` |
| `POST` | `/admin/complaints/{referenceId}/status` | Execute workflow status transition with mandatory rationale and resolution details | Form object: `StatusUpdateRequest` (`status`, `notes`, `resolutionDescription`) |
| `POST` | `/admin/complaints/{referenceId}/priority` | Set or update priority (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) | Form param: `priority` |
| `POST` | `/admin/complaints/{referenceId}/assign` | Assign report to municipal department and responsible field staff | Form object: `AssignmentRequest` (`departmentId`, `assignedTo`, `notes`) |
| `POST` | `/admin/complaints/{referenceId}/evidence` | Upload post-repair photographic evidence required before resolution | Multipart param: `evidenceImages` (file array) |
| `GET` | `/admin/duplicates` | Queue of potential duplicate reports detected across all active issues | - |

---

## 4. Public & API Endpoints (`/api/**`, `/map`)

| Method | Endpoint | Access | Description | Response Format |
|---|---|---|---|---|
| `GET` | `/map` | Public | Full-screen interactive OpenStreetMap view with category and status filters | HTML |
| `GET` | `/api/complaints/map` | Public | Live JSON dataset of all geotagged complaints with approximate coordinates | JSON array: `[{ id, referenceId, title, category, categoryIcon, status, priority, latitude, longitude, locationDescription, createdAt }]` |
| `GET` | `/api/complaints/{id}/timeline` | Authenticated | JSON audit status history for a complaint | JSON array: `[{ oldStatus, newStatus, notes, changedBy, createdAt }]` |
| `GET` | `/api/notifications/count` | Authenticated | Count of unread notifications for current user | JSON: `{ "count": 2 }` |
| `POST` | `/api/notifications/{id}/read` | Authenticated | Mark notification read via AJAX | JSON: `{ "status": "success" }` |
| `POST` | `/api/notifications/read-all` | Authenticated | Mark all notifications read via AJAX | JSON: `{ "status": "success" }` |

---

## 5. Status Workflow Transition Rules

```
                      ┌───────────────┐
                      │   SUBMITTED   │
                      └───────┬───────┘
                              │
               ┌──────────────┴──────────────┐
               ▼                             ▼
       ┌───────────────┐             ┌───────────────┐
       │ UNDER_REVIEW  │             │   REJECTED    │
       └───────┬───────┘             └───────────────┘
               │                             ▲
               ├─────────────────────────────┘ (if invalid)
               ▼
       ┌───────────────┐
       │   APPROVED    │
       └───────┬───────┘
               ▼
       ┌───────────────┐
       │  IN_PROGRESS  │ ◄───────────────────────────┐
       └───────┬───────┘                             │
               ▼ (admin uploads repair evidence)     │
┌─────────────────────────────────┐                  │ (citizen disputes)
│  RESOLVED_PENDING_VERIFICATION  │──────────────────┘
└────────────────┬────────────────┘
                 ▼ (citizen confirms fix)
     ┌───────────────────────┐
     │   VERIFIED_RESOLVED   │
     └───────────────────────┘
```

# Stage 0.1 — Requirement Analysis

First, let's understand what the company is actually asking.

## Core Business Problem

Users conduct meetings.

The system should:

1. Store meeting information.
2. Store transcripts.
3. Analyze transcripts using AI.
4. Extract action items.
5. Track action items.
6. Detect overdue tasks.
7. Send reminders automatically.
8. Provide citations for every AI-generated insight.

---

# Functional Requirements Breakdown

## Module 1: Authentication

Users must login before accessing APIs.

Endpoints:

```http
POST /api/auth/register
POST /api/auth/login
```

Protected APIs:

```http
POST /api/meetings
GET /api/meetings
POST /api/meetings/{id}/analyze
```

---

## Module 2: Meeting Management

User can:

### Create Meeting

```http
POST /api/meetings
```

Store:

* Title
* Participants
* Meeting Date
* Transcript

---

### Get Meeting

```http
GET /api/meetings/{id}
```

---

### List Meetings

```http
GET /api/meetings
```

Supports:

```http
?page=0
&size=10
```

---

## Module 3: AI Analysis

User requests analysis.

```http
POST /api/meetings/{id}/analyze
```

Generate:

* Summary
* Decisions
* Action Items
* Follow-up Suggestions

---

## Module 4: Action Item Management

Create:

```http
POST /api/action-items
```

Update:

```http
PATCH /api/action-items/{id}/status
```

List:

```http
GET /api/action-items
```

Filters:

```http
?status=PENDING

?assignee=Alice

?meetingId=1
```

---

## Module 5: Overdue Detection

Find tasks:

```text
status != COMPLETED
AND
dueDate < currentDate
```

Endpoint:

```http
GET /api/action-items/overdue
```

---

## Module 6: Reminder System

Automatically:

```text
Find overdue items
↓
Send reminder
↓
Save history
```

---

## Module 7: External Integration

Example:

* Telegram Bot
* Slack
* Discord

We'll use Telegram.

---

# Non Functional Requirements

---

## Unified Response

Every API returns:

```json
{
  "traceId": "abc123",
  "success": true,
  "data": {}
}
```

or

```json
{
  "traceId": "abc123",
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Title required"
  }
}
```

---

## Traceability

Every request gets:

```text
traceId
```

Example:

```text
f84b12f3-111a-44de
```

Used in:

* Logs
* Responses

---

## Logging

Log:

```text
Timestamp
Method
Path
Status
TraceId
```

Example:

```text
2026-06-05
POST
/api/meetings
200
trace123
```

---

## Validation

Examples:

### Invalid Email

```json
{
  "email":"abc"
}
```

Reject.

---

### Invalid Status

```json
{
  "status":"DONE"
}
```

Reject.

---

## Error Handling

Never crash.

Always return proper JSON.

---

# Stage 0.2 Database Design

Let's design carefully.

---

# User Entity

Purpose:

Authentication.

```java
User
```

Fields:

```text
id
name
email
passwordHash
createdAt
updatedAt
```

Table:

```sql
users
```

---

# Meeting Entity

Stores meeting metadata.

```text
id
title
meetingDate
createdBy
createdAt
updatedAt
```

Relationship:

```text
One User
     ↓
Many Meetings
```

---

# Participant Entity

Meeting participants.

```text
id
meetingId
email
```

Why separate table?

Because participants count varies.

---

# TranscriptSegment Entity

Stores transcript line-by-line.

Example:

```json
{
  "timestamp":"00:10",
  "speaker":"John",
  "text":"We should launch next Friday"
}
```

Fields:

```text
id
meetingId
timestamp
speaker
text
```

Relationship:

```text
Meeting
  ↓
Many Transcript Segments
```

---

# MeetingAnalysis Entity

Stores AI results.

Fields:

```text
id
meetingId

summaryJson

decisionsJson

followUpsJson

createdAt
```

Example:

```json
{
  "text":"Launch planned next Friday",
  "citations":[
    {
      "timestamp":"00:10"
    }
  ]
}
```

---

# ActionItem Entity

Most important table.

```text
id
meetingId
task
assignee
dueDate
status
createdAt
updatedAt
```

Status:

```java
PENDING
IN_PROGRESS
COMPLETED
```

---

# ReminderHistory Entity

Tracks reminders sent.

```text
id
actionItemId
message
channel
sentAt
```

---

# Final ER Diagram

```text
User
 │
 └───────< Meeting
                │
                ├──────< Participant
                │
                ├──────< TranscriptSegment
                │
                ├─────── MeetingAnalysis
                │
                └──────< ActionItem
                               │
                               └──────< ReminderHistory
```

---

# Stage 0.3 API Design

Design before coding.

---

# Auth APIs

## Register

```http
POST /api/auth/register
```

Request

```json
{
  "name":"Tanmay",
  "email":"tanmay@gmail.com",
  "password":"password123"
}
```

---

## Login

```http
POST /api/auth/login
```

Response

```json
{
  "token":"jwt-token"
}
```

---

# Meeting APIs

## Create

```http
POST /api/meetings
```

---

## Get

```http
GET /api/meetings/{id}
```

---

## List

```http
GET /api/meetings
```

Supports:

```http
?page=0
&size=10
```

---

## Analyze

```http
POST /api/meetings/{id}/analyze
```

---

# Action Item APIs

## Create

```http
POST /api/action-items
```

---

## Update Status

```http
PATCH /api/action-items/{id}/status
```

---

## List

```http
GET /api/action-items
```

---

## Overdue

```http
GET /api/action-items/overdue
```

---

## Reminder History

Nice bonus API:

```http
GET /api/reminders
```

---

# Stage 0.4 System Architecture

Production architecture:

```text
                Client
                   │
                   ▼
         Spring Boot Controllers
                   │
                   ▼
              Services
                   │
        ┌──────────┼──────────┐
        ▼          ▼          ▼

Repositories   Gemini API   Telegram API

        │
        ▼

PostgreSQL Database
```

---

# Layered Architecture

## Controller Layer

Handles:

```text
HTTP Requests
HTTP Responses
```

Example:

```java
MeetingController
```

---

## Service Layer

Business Logic.

Example:

```java
MeetingService
AIAnalysisService
ReminderService
```

---

## Repository Layer

Database Operations.

Example:

```java
MeetingRepository
```

---

## External Integrations

### Gemini

Used for:

```text
Meeting Analysis
```

---

### Telegram

Used for:

```text
Reminder Notifications
```

---

# Stage 0.5 AI Design (Most Important)

---

## Problem

LLMs hallucinate.

Example transcript:

```text
John:
Let's launch Friday.
```

Bad AI:

```json
{
  "decision":"Launch approved by CEO"
}
```

CEO never mentioned.

Fail.

---

## Solution

Prompt:

```text
STRICT RULES:

Use ONLY transcript content.

Never infer information.

Never invent attendees.

Never invent decisions.

Every item must include citations.

If evidence does not exist,
return empty array.
```

---

## Citation Verification

Before saving AI output:

Check:

```text
Does timestamp exist
inside transcript?
```

Example:

AI returns:

```json
{
  "timestamp":"05:30"
}
```

Transcript contains:

```text
00:10
00:20
00:40
```

Reject.

---
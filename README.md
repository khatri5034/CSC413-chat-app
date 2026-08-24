# Chat App — Full-Stack Real-Time Messaging Platform

A full-stack chat application built from scratch for CSC 413 (Software Engineering) at SFSU. The project pairs a **hand-rolled Java HTTP server** (no Spring/Express — raw sockets, custom request parsing, and routing) with a **Next.js/React/TypeScript** front end and a **MongoDB** persistence layer. It supports user accounts, direct messaging, friends, blocking, read receipts, and message deletion.

> Built by a 4-person team as the course final project. See [My Contribution](#my-contribution--kiran-khatri) below for the parts I personally designed and implemented, and [Team Contributions](#team-contributions) for the full breakdown.

## Table of Contents
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Features](#features)
- [My Contribution — Kiran Khatri](#my-contribution--kiran-khatri)
- [API Reference](#api-reference)
- [Data Model](#data-model)
- [Getting Started](#getting-started)
- [Testing](#testing)
- [Team Contributions](#team-contributions)
- [Demo Videos](#demo-videos)

## Architecture

```
┌─────────────────────┐        HTTP (rewrites /api/* )        ┌──────────────────────────┐        ┌───────────┐
│   Next.js Front End  │ ─────────────────────────────────▶  │  Custom Java HTTP Server  │ ──────▶│  MongoDB  │
│  (React + TypeScript)│ ◀─────────────────────────────────  │   (raw ServerSocket, no    │ ◀──────│           │
│   localhost:3000     │            JSON responses            │   framework, port 1299)   │        │           │
└─────────────────────┘                                       └──────────────────────────┘        └───────────┘
```

- The front end never talks to the backend directly in the browser — `next.config.ts` rewrites every `/api/*` request to `http://localhost:1299/*`, so the Java server just looks like same-origin `/api` routes to the client.
- The backend has **no external web framework**. `Server.java` opens a raw `ServerSocket`, reads the raw HTTP bytes off the wire, and hands them to a hand-written parser/router pipeline:
  - `request.CustomParser` — parses the raw HTTP request text into a `ParsedRequest` (method, path, query params, headers, cookies, body).
  - `handler.HandlerFactory` — routes each path (e.g. `/sendMessage`, `/login`) to its `BaseHandler` implementation.
  - Each `*Handler` — implements the endpoint's business logic and returns a `ResponseBuilder`.
  - `response.CustomHttpResponse` — serializes the response back into a raw HTTP response string.
- Auth is cookie-based: `LoginHandler` issues a signed session hash stored in an `auth` cookie; `auth.AuthFilter` validates that cookie against a Mongo-backed `AuthDao` on every protected request.
- Persistence uses the MongoDB Java driver directly (no ORM) via a generic `BaseDao`/`MongoConnection` pattern, with one DAO per collection (`UserDao`, `MessageDao`, `ConversationDao`, `AuthDao`).

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Next.js 16 (App Router), React 19, TypeScript, Tailwind CSS |
| Backend | Java 22, raw `java.net.ServerSocket` (custom HTTP server, no framework) |
| Database | MongoDB (official Java driver 3.12) |
| Serialization | Gson |
| Auth | SHA-256 password hashing (Apache Commons Codec) + cookie-based session tokens |
| Build/Test | Maven, TestNG, Mockito, Checkstyle |

## Features

- **Accounts & Auth** — sign up, log in, and cookie-based session auth enforced on every protected endpoint.
- **1:1 Messaging** — send messages, fetch full conversation history, and list all of a user's conversations. Conversations are keyed by a deterministic, sorted `fromId_toId` id so either participant resolves to the same thread.
- **Friends List** — add/remove friends and view your friends list.
- **Block List** — block/unblock users; blocked users cannot be added as friends.
- **Read Receipts** — messages track `sent → delivered → read` status with timestamps; recipients get an unread-count badge, and messages auto-mark as read via an `IntersectionObserver` when scrolled into view.
- **Delete Messages** — sender-only message deletion that keeps conversation message counts and per-user `messagesSent`/`messagesReceived` stats in sync.
- **Live Dashboard** — per-user stats (total conversations, messages sent/received, unread count) that refresh automatically after every action.

## My Contribution — Kiran Khatri

I designed and implemented the **message deletion feature** end-to-end, across the full stack:

- **`DeleteMessageHandler.java`** — new authenticated `DELETE /deleteMessage` endpoint that validates the request, deletes the target message, and keeps derived data consistent: it decrements the sender's `messagesSent` and recipient's `messagesReceived` counters and decrements the parent conversation's `messageCount`, all in one atomic-per-request flow with defensive `Math.max(0, …)` guards against negative counts.
- **`BaseDao.queryByMultiple(Document criteria)`** — generalized the DAO layer to support querying (and deleting) by multiple criteria at once (e.g. `conversationId` + `timestamp`), rather than the single-field lookup that existed before, so a specific message could be targeted precisely.
- **Frontend (`ChatBar.tsx`)** — added a per-message delete control (sender-only, with a debounced "in-flight" state to prevent double-deletes) that calls the new endpoint and reloads the conversation.
- **Live stat sync (`home/page.tsx`)** — wired the delete flow to automatically refetch and re-render the user's `messagesSent`/`messagesReceived` dashboard stats immediately after a delete, with no page reload.
- **Tests** — `DeleteMessageHandlerTest.java` covering the handler's auth, validation, and side-effect behavior.

Demo video: https://drive.google.com/file/d/1iJCIjG39moGHNUW9T8g_SGmT3rPYKPhc/view?usp=sharing

## API Reference

All endpoints are proxied through the front end at `/api/*` → backend `http://localhost:1299/*`. Responses are JSON in the shape `{ status: boolean, data: [...] | null, message: string | null }` (see `RestApiAppResponse.java`). Endpoints marked 🔒 require a valid `auth` session cookie (set by `/login`).

| Method | Path | Description |
|---|---|---|
| POST | `/createUser` | Register a new user |
| POST | `/login` | Authenticate and receive a session cookie |
| GET | `/getUser` 🔒 | Fetch the logged-in user's profile & stats |
| POST | `/sendMessage` 🔒 | Send a message to another user |
| GET | `/getConversations` 🔒 | List all conversations for the logged-in user |
| GET | `/getConversation` 🔒 | Get full message history for one conversation |
| DELETE | `/deleteMessage` 🔒 | Delete a message and sync counters *(my feature)* |
| POST | `/markMessageRead` 🔒 | Mark a message as read |
| GET | `/getMessageReceipt` 🔒 | Get delivery/read status for a message |
| GET | `/getUnreadCount` 🔒 | Get the logged-in user's unread message count |
| POST | `/addFriend` 🔒 | Add a friend |
| POST | `/removeFriend` 🔒 | Remove a friend |
| GET | `/getFriends` 🔒 | List friends |
| POST | `/addBlockedUser` 🔒 | Block a user |
| POST | `/removeBlockedUser` 🔒 | Unblock a user |
| GET | `/getBlockedUsers` 🔒 | List blocked users |

## Data Model

MongoDB collections, modeled via `BaseDto` subclasses:

- **User** — `userName`, `password` (SHA-256 hash), `messagesSent`, `messagesReceived`, `friends[]`, `blockedUsers[]`
- **Message** — `fromId`, `toId`, `message`, `timestamp`, `conversationId`, `status` (`sent`/`delivered`/`read`), `deliveredAt`, `readAt`
- **Conversation** — `conversationId` (deterministic sorted `fromId_toId`), `fromId`, `toId`, `messageCount`
- **Auth** — session `hash`, `userName`, `expireTime`

## Getting Started

### Prerequisites
- Java 22+ and Maven
- Node.js 18+
- MongoDB running locally on the default port (`localhost:27017`, database `Homework2`)

### 1. Start MongoDB
```bash
mongod
```

### 2. Run the backend (IntelliJ recommended)
```bash
cd back-end
mvn clean package
java -cp target/final-project-1.3-SNAPSHOT-jar-with-dependencies.jar server.Server
```
The server starts a raw socket listener on port `1299`.

### 3. Run the frontend (VS Code recommended)
```bash
cd front-end
npm install
npm run dev
```
Visit `http://localhost:3000`. API calls under `/api/*` are automatically rewritten to the backend.

## Testing

Backend unit/integration tests live in `back-end/src/test/java` and use TestNG + Mockito to mock the DAO layer:

```bash
cd back-end
mvn test
```

Coverage includes handler behavior (auth, validation, side effects) for create user, login, send/get message(s)/conversation(s), delete message, friends, blocked users, plus routing, cookie parsing, and DTO ↔ `Document` conversion round-trips.

## Team Contributions

This was a 4-person group project. Each member owned a full-stack feature (backend handler + frontend UI + tests):

**Ian Kligman — Friends List**
- Added a `friends` field to `UserDto`
- Implemented `AddFriendHandler`, `RemoveFriendHandler`, `GetFriendsHandler` + routes
- Added `FriendHandlerTests` and the `FriendsList.tsx` UI, integrated into `page.tsx`
- Lets users add/remove other registered users from their personal friends list

**Philip Chen — Read Receipts**
- Extended `MessageDto` with `status`, `deliveredAt`, `readAt`
- Implemented `MarkMessageReadHandler`, `GetMessageReceiptHandler`, `GetUnreadCountHandler` + routes
- Built `ReadReceipts.tsx` (sent/delivered/read checkmark states) and wired auto-mark-as-read + unread badges into `ChatBar.tsx`/`page.tsx`
- Enforces that only the recipient can mark a message read and only the sender can view its receipt

**Joshua Gonzalez — Block List**
- Added a `blockedUsers` field to `UserDto`
- Implemented `AddBlockedUserHandler`, `RemoveBlockedUserHandler`, `GetBlockedUsersHandler` + routes
- Added `BlockedUserHandlerTests` and `BlockedList.tsx`, integrated into `page.tsx`
- Prevents blocked users from being added as friends

**Kiran Khatri — Delete Chat** *(see [My Contribution](#my-contribution--kiran-khatri) above for full detail)*
- `DeleteMessageHandler`, `BaseDao.queryByMultiple`, delete UI in `ChatBar.tsx`, live stat sync in `page.tsx`

## Demo Videos

| Feature | Author | Link |
|---|---|---|
| Friends List | Ian Kligman | https://www.youtube.com/watch?v=h85DVJskR74 |
| Delete Chat | Kiran Khatri | https://drive.google.com/file/d/1iJCIjG39moGHNUW9T8g_SGmT3rPYKPhc/view?usp=sharing |
| Block List | Joshua Gonzalez | https://youtu.be/EqCGC8rMCls |
| Read Receipts | Philip Chen | https://drive.google.com/file/d/16YewHQHLn83D9a-W1ZwgnnuRo_z9eoFB/view?usp=sharing |

# ADR 3: Server-Sent Events for live updates

**Context.** The staff dashboard fetched every order ever placed, every 5 seconds.

**Decision.** Use SSE. Updates only flow from server to browser, and normal HTTP requests cover the other direction, so WebSockets' two-way channel isn't needed. SSE also passes through proxies as ordinary HTTP.

- Events carry only an id and status. The client then refetches, so there is one source of truth and a missed event can't leave stale data on screen.
- `@TransactionalEventListener(AFTER_COMMIT)` means a rolled-back change is never announced, and the refetch is guaranteed to see the new data.
- The client uses `fetch`, not `EventSource`, because `EventSource` can't send an `Authorization` header and a JWT in the URL would end up in logs.
- After reconnecting, the client refetches everything. A slow timer remains as a safety net.

**Trade-offs.** Connections live in one instance's memory, so a second instance would need Redis pub/sub between them.

**Lessons from testing.** Browser tests against the real stack found two bugs that unit tests couldn't:
1. nginx buffered the stream, because a regex `location` outranks a plain prefix. It needed `^~` and `proxy_buffering off`.
2. Calling `complete()` on a dead connection threw, which aborted the send loop, so staff silently missed updates. A failed send now just forgets that connection.

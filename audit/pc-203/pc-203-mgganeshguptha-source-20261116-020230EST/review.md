VERDICT: PASS
[NOTE]: trimName uses String.strip(), which also removes Unicode whitespace (e.g. tabs, non-breaking spaces). The story says "spaces", so trim() would be the strictly ASCII option. Either satisfies AC1 to AC3 and AC5.
[NOTE]: A whitespace-only name (e.g. "   ") passes the existing @Size(min=1) check and is saved as "" after trimming. The story adds no validation, so this is left as is. The plan already records it as an open risk for the story owner.
[NOTE]: Null handling is correct. trimName returns null for a null name, so create still fails validation as before and update still sets a null name as it does today.
[NOTE]: The helper is private static and reused by both methods. No signature, status code, @PreAuthorize, or OpenAPI contract changed.

VERDICT: PASS
[NOTE]: Whitespace-only names such as "   " pass bean validation, are trimmed to "" and are saved as an empty string. The story forbids new validation, so this is left as-is. The plan's Pre-flight item 8 asks the developer to confirm this behaviour or raise a follow-up story.
[NOTE]: String.trim() also removes control characters at or below U+0020, not only spaces. This matches the plan's stated trim definition, so it is acceptable.
[NOTE]: addPetType mutates the incoming request DTO before mapping. This is correct, and the mapper sets the name only once, as the plan requires.

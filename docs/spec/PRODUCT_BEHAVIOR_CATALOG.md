# Platform-Independent Product Behavior Catalog

This catalog converts verified Android reference evidence and current Learning Engine/Desktop
behavior into target learner contracts. Android evidence IDs resolve through
[`../ANDROID_PRODUCT_BEHAVIOR.md`](../ANDROID_PRODUCT_BEHAVIOR.md).

| ID | Behavior | Purpose / learner value | Evidence | Current Desktop | Target state | Decision | Future owner |
|---|---|---|---|---|---|---|---|
| PB-001 | Hierarchical scope | find the right learning unit without scanning a flat list | TOPIC-001/002 | flat lesson browser | Library→Package→Topic→Section→Lesson with collapsed missing levels | Adapt | application query + platform |
| PB-002 | Multi-lesson plan | combine intentional scopes | TOPIC-003/004 | missing | reviewed immutable selection request and deterministic engine plan | Redesign | application + platform |
| PB-003 | Resume-first entry | continue without duplicate work | RECOVERY-001; engine recovery | implemented | prominent same-session Resume everywhere | Preserve | engine + platform projection |
| PB-004 | Standard session setup | understand scope/budget before starting | QUEUE-001 | partially implicit | concise defaults and optional advanced choices | Adapt | application + platform |
| PB-005 | Prompt/reveal/rate rhythm | protect retrieval practice | STUDY-001/002 | implemented | stable cross-platform lifecycle | Preserve | engine + platform |
| PB-006 | Progressive hints | aid retrieval without immediately revealing | typing XML | missing | authored optional help before reveal | Adapt | content/application + platform |
| PB-007 | Four semantic ratings | capture recall quality consistently | STUDY-002 | implemented | stable names/order/meaning; engine schedules | Preserve | engine |
| PB-008 | One-step Undo | recover the latest rating mistake | current engine | implemented | atomic, restart-safe, no arbitrary rerating | Preserve | engine/application |
| PB-009 | Rich prompt/answer | support text, Markdown, image, examples | MEDIA-001/002 | implemented | role-aware accessible content blocks | Preserve/Adapt | content projection + platform |
| PB-010 | Semantic audio | choose correct language/purpose/voice | AUDIO-001/002/003 | generic audio | Prompt/Answer/Example/Translation roles | Redesign | content/application + media adapter |
| PB-011 | Manual replay | repeat pronunciation on demand | AUDIO-001/004 | implemented basic | role-labeled replay/stop with one active asset | Preserve | platform media |
| PB-012 | Listening preset | reduce setup for listening practice | MODE-001 | missing | presentation/media preset over same engine session | Adapt | platform preference/projection |
| PB-013 | Sequential listening | create hands-light practice | AUTO-002/003 | missing | visible interruptible playback plan, no auto-rating | Redesign | media coordinator |
| PB-014 | Auto flow | maintain rhythm without dark patterns | AUTO-001 | missing | opt-in, explainable, stoppable; rating manual | Redesign | platform orchestration |
| PB-015 | Typed Recall | practice production, not recognition | TYPING-001/003 | missing | optional explicit exercise/evaluator | Adapt | domain evaluation + application + platform |
| PB-016 | Forced typing gate | force typing after poor ratings | TYPING-002 | missing | not part of default product; requires science decision | Retire pending evidence | product/domain |
| PB-017 | Calm progress | orient without distracting | PROGRESS-001 | implemented partial | compact authoritative position/budget | Adapt | application query + platform |
| PB-018 | Completion summary | close loop and support next intent | current completion | implemented partial | neutral counts/outlook/history actions | Adapt | application query + platform |
| PB-019 | Daily goal | create learner-chosen intention | STATS-002 | missing | optional informational goal from history | Redesign | application preference/query |
| PB-020 | Detailed analytics | support reflection outside active recall | current Dashboard | implemented | summary/dedicated screens, not card chrome | Preserve | application query + platform |
| PB-021 | Quick search | recover a known item/scope quickly | STUDY-005 | Library search only | reuse query ports; overlay optional | Adapt | application query + platform |
| PB-022 | Favorite | curate meaningful content | STUDY-004 | missing | learner relation, never package mutation | Redesign | domain/application |
| PB-023 | Pinned scopes | return to important lessons | product need | missing | learner-ordered stable shortcuts | Redesign | application + platform |
| PB-024 | Recents | continue recent intent | recovery/history | partial | derived bounded scope list | Adapt | application query |
| PB-025 | Previous card | inspect recent content safely | STUDY-003 | undo only | read-only history; Undo sole backward mutation | Redesign | application query |
| PB-026 | Keyboard-first flow | reduce interaction cost/accessibility barrier | INPUT-001 | implemented core | same actions and guards across platforms | Preserve | platform input |
| PB-027 | Pointer/touch gestures | offer optional fast input | GESTURE-001/002 | missing | discoverable configurable accelerators with parity | Redesign | platform input |
| PB-028 | Floating note | externalize short thinking | MEDIA-003 | missing | transient scratchpad if approved; durable note separate | Redesign | platform/product |
| PB-029 | Display preferences | adapt content visibility/readability | RECOVERY-002 | partial typed config | progressive typed preferences, not session truth | Adapt | platform config |
| PB-030 | Safe media fallback | continue learning when assets fail | current renderer | implemented | per-asset unavailable state and retry | Preserve | application projection + adapter |
| PB-031 | Interruption recovery | prevent duplicate review/progress | engine recovery | implemented | restore learning truth; media/view state stopped/recomputed | Preserve | engine + platform |
| PB-032 | Content authoring in Study | edit imported content during recall | EDIT-001 | absent | keep outside focused session | Retire | future authoring product |
| PB-033 | Android SRS/queue rules | schedule within Activity | SRS/QUEUE reference | not used | never port; Learning Engine authority | Retire | existing engine |
| PB-034 | Lock-screen/device admin | force Android surface | LOCK-001 | irrelevant | no cross-platform product behavior | Retire | — |

## Mandatory blockers before implementation sequence

- Platform-independent session and workspace behavior must remain compatible with current
  recovery/undo contracts.
- Topic hierarchy must be derivable without inventing or rewriting package data.
- Semantic audio roles need a backward-compatible content/package representation before
  Listening automation.
- Timed media requires a deterministic coordinator and stale-callback boundary.
- Typed Recall requires approved evaluation semantics before UI implementation.

## Optional improvements

Gestures, scratch notes, favorites, pinned scopes, goals, visual delight, and TTS improve the
product but do not block hierarchical scope, Standard Session, or manual semantic media.

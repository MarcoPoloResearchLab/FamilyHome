# ISSUES

Entries record newly discovered requests or changes.

Read `AGENTS.md`, `.mprlab/POLICY.md`, `.mprlab/issues-md-format.md`, and relevant stack guides before implementing changes.

Format: `- [ ] [B042] (P1) {I007} Title`

## BugFixes

- [x] [B001] (P1) Restore piano sound.
  Goal:
  Music shows each note but produces no sound.
  Requirements:
  - Write note samples before the audio initialization check.
  - Show audio errors.
  Validation:
  - Verify audio output through keyboard input.
  - Verify repeated notes and both octaves.
  - Run `make ci`.
  The Android integration test failed before the correction.
  The test passed for all 24 keys and repeated notes after the correction.
  `make ci` passed.
  The signed update is installed on the Portal.
  The installed APK hash matches the signed update.
  Android reports active piano audio through the speaker output.
  The media volume changed from zero to 3 of 18.
  The user confirmed audible piano sound on the Portal.

## Improvements

- [x] [I002] Combine navigation and application controls in one toolbar.
  Goal:
  Back, Home, and application controls share one row.
  Requirements:
  - Keep Back and Home visible beside the application controls.
  - Apply one top row to every app and game launched from FamilyHome.
  - Include game menus, active games, and child screens in the audit.
  - Keep each control within the visible toolbar.
  - Preserve navigation and saved drawings.
  Implementation:
  Home keeps its profile, clock, and Settings row.
  Games, Settings, Ask, Drawing, Music, Piano, and Guitar use one toolbar.
  Guitar chord controls share that row.
  Match combines navigation, its game menu, and game status.
  Blocks combines navigation, score, level, lines, and Settings.
  Tiles combines navigation and menu tabs. Its active game menu uses the same row.
  Freedoom and Kart already use fullscreen controls without stacked headers.
  The audit covers their menus and active games, plus the Kart pause menu.
  Validation:
  Integration tests failed before the toolbar changes.
  Seven native screens passed toolbar, touch-target, Back, and Home checks.
  Piano and Guitar returned to Music through Back and to Home through Home.
  The upgrade test preserved profiles, drawings, timer settings, and private files.
  `make ci` passed for the final source.
  Blocks and Tiles passed navigation, menu, settings, and gameplay toolbar checks.
  Signed game artifacts and matching source archives are ready.
  See `docs/combined-toolbar-audit.md` for scope and validation details.
  The user approved installation and replacement of Match, Blocks, and Tiles.
  All four signed updates are installed on the physical Portal.
  Each installed APK hash matches its final artifact.
  Physical review found a 60 px top inset in Blocks after the system bar disappeared.
  The correction removes that inset from the Flutter layout.
  The corrected Blocks toolbar starts at the top of the screen.
  Native screens, game menus, gameplay, Back, and Home passed physical review.
  The FamilyHome child profile and home settings remain present.
  Match, Blocks, and Tiles data was reset as approved.
  The Portal is on Home.


- [ ] [I001] (P1) Standardize HTTP health at `/healthz`.
  Goal:
  Make `/healthz` the canonical health endpoint for the FamilyHome HTTP
  origin. Use the endpoint for readiness without application requests.

  Requirements:
  - Keep unauthenticated `GET /healthz` as the only HTTP health operation.
  - Return `200` only when the service can serve its current application contract.
  - Return a non-success status when a required runtime dependency prevents service.
  - Send `Cache-Control: no-store` on every health response.
  - Keep the response free from credentials and internal state.
  - Do not call a paid provider or mutate application state during a probe.
  - Do not record a probe as application usage or an audit event.
  - Do not emit routine information-level request events for successful probes.
  - Keep failed probe evidence in container and deployment diagnostics.
  - Use `/healthz` for runtime capability health and public health checks.
  - Keep the selected manifest contract unchanged.

  Deliverables:
  - Update the endpoint, orchestration, manifest, documentation, and black-box tests as necessary.

  Validation:
  - Verify unauthenticated `GET /healthz` returns `200` and `Cache-Control: no-store`.
  - Verify a required dependency failure returns a non-success status without a provider call.
  - Verify successful probes create no routine request events.
  - Verify failed probes retain diagnostic evidence.
  - Run `make ci`.

## Maintenance

- [ ] [M400R] (P2) Backlog hygiene and archive
  Goal:
  Keep the issue tracker reliable, readable, and focused on active work while preserving resolved history in the appropriate archive.

  Requirements:
  - Cadence: run weekly during active development and before each release cut.
  - Validate section names, identifier prefixes, recurrence suffixes, priority markers, dependencies, and duplicate IDs against the current `issues-md-format.md`.
  - Reconcile stale statuses, duplicate issues, broken references, obsolete instructions, and entries filed in the incorrect section.
  - Move completed non-recurring history to the repository issue archive or durable documentation when the active tracker becomes noisy.
  - Keep active, blocked, planning, and recurring entries visible in `ISSUES.md`.

  Deliverables:
  - Normalized `ISSUES.md` structure and statuses.
  - Updated issue archive or docs when completed entries are removed from the active tracker.
  - A short `Last run:` note summarizing the cleanup and any follow-up issues filed.

  Validation:
  - Read `ISSUES.md` after edits and confirm that each issue is in the correct section.
  - Confirm that each issue has a unique section-aware ID.
  - Confirm recurring entries remain open and keep the `R` suffix.
  - Confirm no active, blocked, recurring, or planning work was archived.

- [ ] [M401R] (P2) Polish open issues
  Goal:
  Keep unresolved work executable by making each open issue concrete, ordered, and testable.

  Requirements:
  - Cadence: run weekly during active development and before handing a repo to automated execution.
  - Review every unresolved non-recurring issue for missing context, dependencies, repro steps, acceptance criteria, and validation expectations.
  - Make priorities concrete and make sure that each open issue has actionable deliverables.
  - Merge duplicate open issues or add explicit dependency links when separate entries must remain.
  - Do not close or implement issues as part of this polish pass unless that work is separately requested.

  Deliverables:
  - Open issues with enough detail for a person or agent to execute without rediscovery.
  - New or updated dependency markers where ordering matters.
  - A short `Last run:` note listing the number of issues polished and any blockers found.

  Validation:
  - Sample the open entries after the pass and confirm each has clear next actions and validation expectations.
  - Confirm that no recurring runbook has a closed status.
  - Confirm duplicates were merged or explicitly cross-referenced.

- [ ] [M402R] (P2) Architecture and policy review
  Goal:
  Catch architecture, policy, and workflow drift before it becomes hidden maintenance debt.

  Requirements:
  - Cadence: run monthly, before large refactors, and after major framework or runtime changes.
  - Review the codebase, docs, and workflow against `AGENTS.md`, `POLICY.md`, stack guides, and the current architecture notes.
  - Look for drift from forward-only contracts, edge-validation boundaries, smart-constructor usage, testing policy, and module ownership.
  - Record findings as new Maintenance issues with concrete scope, priority, and validation.
  - Close the pass with a no-action note only when the review finds no actionable drift.

  Deliverables:
  - New Maintenance issues for each actionable architecture or policy drift finding.
  - Updated notes on areas reviewed and areas intentionally left unchanged.
  - A short `Last run:` note with the review scope and outcome.

  Validation:
  - Confirm every finding is represented as an issue with owner-readable context and validation criteria.
  - Confirm no implementation changes were mixed into the review runbook unless separately requested.
  - Confirm all recurring runbooks remain open.

- [ ] [M403R] (P1) Dependency and security audit
  Goal:
  Keep third-party dependencies, runtime versions, and security-sensitive configuration within the current supported contract.

  Requirements:
  - Cadence: run weekly for active apps and before each release cut.
  - Inspect package managers, lockfiles, language toolchains, container bases, and generated clients for known vulnerabilities or stale direct dependencies.
  - Review auth, secret, CORS, CSP, SQL, network, and permission-sensitive configuration for drift from the current contract.
  - Prefer current supported dependencies.
  - Do not add compatibility shims for obsolete dependency behavior.
  - File separate Maintenance or BugFix issues for each actionable vulnerability, unsupported runtime, or security-contract gap.

  Deliverables:
  - Documented audit commands or data sources used for the pass.
  - Updated issues for each actionable dependency or security finding.
  - A short `Last run:` note with clean result or follow-up issue IDs.

  Validation:
  - Rerun the repository-native audit, lint, or dependency checks used for the pass.
  - Confirm every finding is either filed, fixed under a separate issue, or explicitly marked not applicable with evidence.
  - Confirm no secrets or private payloads were written into the tracker.

- [ ] [M404R] (P1) CI, release, and artifact health
  Goal:
  Keep the repository's validation, release, publication, and generated artifact surfaces trustworthy.

  Requirements:
  - Cadence: run before every release, publish, or deploy, and weekly for critical services.
  - Verify repository-native CI, lint, format, coverage, release, publish, Docker image, Pages, and artifact workflows still match the documented contract.
  - Do a check of generated artifacts, release tags, published images, and Pages outputs for source-to-public drift.
  - File concrete follow-up issues for failing gates, stale artifacts, missing release prerequisites, or undocumented workflow changes.
  - Do a production deployment only when the operator explicitly requests it.

  Deliverables:
  - Recorded gate status and artifact surfaces inspected.
  - Follow-up issues for each reproducible CI, release, publish, or artifact drift problem.
  - A short `Last run:` note with commands run and any skipped surfaces.

  Validation:
  - Use repository-native `make` targets or documented release helpers for checks.
  - Confirm release and deployment ownership boundaries remain separate.
  - Confirm public or published artifacts match the intended source revision when that surface is inspected.

- [ ] [M405R] (P1) Code contract and static hygiene
  Goal:
  Keep source contracts explicit, current, and statically guarded against policy drift.

  Requirements:
  - Cadence: run monthly and before large refactors.
  - Scan for dead code, unused exports, duplicated literals, silent fallbacks, legacy aliases, compatibility reads, and zero-but-invalid domain states.
  - Do a check of static analysis, coverage, schema, and contract guards that prevent drift.
  - File focused Maintenance issues for each concrete violation instead of broad cleanup placeholders.
  - Keep only the current canonical contract.
  - Preserve obsolete behavior only when a current product requirement explicitly specifies it.

  Deliverables:
  - Issue entries for each actionable static hygiene or contract violation.
  - Notes on static tools, searches, and contract guards used during the pass.
  - A short `Last run:` note with clean result or follow-up issue IDs.

  Validation:
  - Rerun the relevant static checks, contract tests, or repository searches used to identify drift.
  - Confirm every finding has a narrow follow-up issue and does not duplicate existing backlog work.
  - Confirm no implementation changes were mixed into the audit unless separately requested.

- [ ] [M406R] (P1) Production drift and health
  Goal:
  Detect drift between runtime state and the intended repository contract.

  Requirements:
  - Cadence: run weekly for deployed services and after each publish or deploy.
  - Compare current source, runtime configuration, published images, public routes, scheduled jobs, and health checks for drift.
  - Inspect real operator-facing surfaces rather than assuming merged source is deployed.
  - File follow-up issues for stale images, stale Pages output, missing routes, failed monitors, invalid production config, or undocumented runtime differences.
  - Stop before production deploy or destructive operator actions unless the operator explicitly requests them.

  Deliverables:
  - Recorded source revision, public artifact, route, image, or health surfaces inspected.
  - Follow-up issues for each source-to-runtime drift finding.
  - A short `Last run:` note with evidence links or commands used.

  Validation:
  - Verify inspected production or public surfaces directly where access is available.
  - Confirm any deploy-required finding is filed with the exact publish/deploy boundary and owner.
  - Confirm no production state was changed by the audit unless explicitly requested.

- [ ] [M407R] (P2) Documentation and runbook hygiene
  Goal:
  Keep durable documentation and runbooks aligned with the current behavior users and operators actually rely on.

  Requirements:
  - Cadence: run before release cuts and after merge bursts that change user-facing or operator-facing behavior.
  - Review README, ARCHITECTURE, PRD, CHANGELOG, docs, runbooks, setup guides, and local workflow notes for stale behavior or missing new contracts.
  - Review changed English technical prose against `.mprlab/AGENTS.DOCS.md` and the official ASD-STE100 standard.
  - Add approved repository terms to `.mprlab/TERMINOLOGY.md`.
  - Update docs when closed issues changed durable behavior, public APIs, operator workflows, release semantics, or deployment expectations.
  - Remove or rewrite stale instructions instead of preserving obsolete alternatives.
  - File separate issues for documentation gaps that require product or implementation decisions.

  Deliverables:
  - Updated documentation or filed follow-up issues for each gap.
  - A short `Last run:` note listing docs inspected and changes made.
  - Cross-references from archived issue history to durable docs when useful.

  Validation:
  - Run the skill `prepare-ste-reference` script and use its verified official PDF.
  - Run the skill `check-ste` script on each English technical document that changed.
  - Review the changed text against Part 1 writing rules and the Part 2 dictionary.
  - Confirm that the producing agent completed the review without end-user work.
  - Do a check of links, command names, paths, and public contract descriptions changed by the pass.
  - Confirm docs describe the current canonical path only.
  - Confirm issue archive and active tracker references remain consistent.

## Features

- [-] [F001] (P1) Add a guitar to Music.
  Goal:
  Children can play an offline guitar from Music.
  Requirements:
  - Keep Piano and Guitar together under Music.
  - Show an acoustic guitar illustration on the Guitar selection card.
  - Show six strings and five fret positions.
  - Play a note when the child touches a string at a fret.
  - Show the vibrating string section and the selected note.
  - Provide C, G, Am, and F chord controls with finger positions.
  - Play selected strings when the child moves a finger across the strumming area.
  - Support simultaneous touches and repeated notes.
  - Stop audio when the child leaves Guitar.
  Validation:
  - Verify navigation, notes, chords, strumming, and audio through the Android interface.
  - Verify the layout at the Portal screen size.
  - Run `make ci`.

  Current result:
  `make test-android-guitar` first failed with `Missing control: Music. Choose an instrument`.
  The fast-swipe test then failed with `Expected 6 guitar strings, got 1`.
  Both failures passed after the corrections.
  The guitar test verifies chord positions, active string counts, simultaneous touches, repeated notes, and audio cleanup.
  `make ci`, `make test-android-toolbar`, and `make test-android-upgrade` passed.
  The emulator layout check passed at 1280 × 800.
  The signed APK is ready at `android/build/guitar-feature/Children-Portal-v0.13.0.apk`.
  The signed update is installed on the Portal.
  The installed APK hash matches the signed update.
  Music opens both Piano and Guitar on the device.
  A C chord strum produces five active guitar strings through the speaker output.
  Back releases all guitar audio players.
  Audible sound acceptance remains pending.
  The guitar illustration now has a curved body, fretboard, headstock, tuning pegs, sound hole, and bridge.
  The two instrument cards retain their dimensions and controls.
  The screen test and `make ci` passed after the illustration change.
  The revised signed APK is installed from `android/build/guitar-icon/Children-Portal-v0.13.0.apk`.
  The installed APK hash matches that build. The Music screen passed visual review on the Portal.
  The Piano card now shows a blue upright piano with black-and-white keys and brass pedals.
  The screen test and `make ci` passed after this change.
  The signed update from `android/build/music-icons/Children-Portal-v0.13.0.apk` is installed on the Portal.
  Its APK hash matches the build. The Music screen passed visual review on the device.

## Planning

- [ ] [P003] Plan image generation through MediaOps.
  Goal:
  FamilyHome has two separate creative activities.
  In one activity, the child draws with fingers.
  In the other activity, the child describes an image and an image model creates it.
  The selected first model is GPT Image 2 with low quality.
  This issue authorizes planning and documentation only.

  Requirements:
  - Keep finger drawing and image generation as separate activities.
  - Keep the existing drawing activity available.
  - Give children an easy route to each activity.
  - Create images from child descriptions in the image generation activity.
  - Use MediaOps for image generation.
  - Use OpenAI model `gpt-image-2` for the first implementation.
  - Set the initial image quality to `low`.
  - Include the price of image generation in the design decisions.
  - Keep the implementation within the current canonical contracts.

  Current evidence:
  The source review date is 2026-09-04.
  FamilyHome has a drawing canvas and an Ask integration that returns text.
  The MediaOps source declares `mediaops.image.generate` and `mediaops.image.edit` through MCP.
  Their declared inputs include `provider`, `model`, `prompt`, `size`, and `quality`.
  The MediaOps source catalog includes OpenAI model `gpt-image-2` with quality `low`.
  These source facts do not establish deployed capability or account access.

  This session has no connected MediaOps tool.
  The documented local MCP endpoint `http://127.0.0.1:8766/mcp` refused the connection.
  Live descriptor verification and account access remain pending.
  This review made no paid generation request.

  Integration dependency:
  The selected path is Portal to FamilyHome backend to MediaOps to OpenAI.
  Provider execution belongs to MediaOps.
  FamilyHome owns its child interface, family access, image storage, and family limits.
  The application connection to MediaOps requires a defined transport and operation contract.
  Agent verification uses the live MediaOps MCP descriptors and structured results.
  This plan requires no image generation extension to LLM Proxy.
  P002 defines the proposed family ownership and parent authentication contracts.
  The hosted family release requires the applicable P002 implementation.

  Proposed activity navigation:
  The proposed Home controls are `Draw` and `Imagine`.
  `Draw` opens the existing finger drawing activity.
  `Imagine` opens the activity for image generation from descriptions.
  Each activity returns to Home through the common toolbar.
  These are two activities within FamilyHome. Separate Android applications are not part of this proposal.
  The new activity name and exact navigation controls remain open decisions.

  Proposed image flow:
  These steps are proposals for the first implementation.
  1. Enter a prompt or select a character idea.
  2. Select a visual style.
  3. Create one image job through the FamilyHome backend and MediaOps.
  4. Show the job state and resulting image.
  5. Save the selected image under the active child profile.
  Drawing input and image revisions remain scope decisions.

  Cost basis:
  OpenAI currently estimates $0.006 for one 1024 by 1024 image at low quality.
  This estimate covers image output only.
  Prompt input, reference images, and later revisions add charges.
  The selected image dimensions and total request budget remain open decisions.

  Open Decisions:
  - Select the image dimensions and image format.
  - Select typed descriptions, spoken descriptions, or both for the first implementation.
  - Select drawing input and revision scope for the image generation activity.
  - Select the new activity name and navigation controls.
  - Select the style choices and save behavior.
  - Define family spending limits and simultaneous job limits.
  - Define duplicate prevention and accounting for unknown provider outcomes.
  - Define image ownership, deletion, storage duration, and parent controls through the P002 contract.
  - Define content moderation and provider data controls for children.
  - Select the MediaOps application transport, deployment connection, and required release.
  - Define the mapping from MediaOps operation records and image artifacts to FamilyHome image jobs.
  - Verify live MediaOps support for `provider=openai`, `model=gpt-image-2`, and `quality=low`.
  - Verify account access through the selected MediaOps contract.

  Deliverables:
  - Record the approved image flow and cost limits.
  - Record the MediaOps integration contract and its acceptance evidence.
  - Record FamilyHome resources, image storage, job states, and family authorization checks.
  - Define an integration test for an image request with `quality=low` through MediaOps.
  - Define separate live-provider and Portal acceptance steps.
  - Create implementation issues after resolution of the applicable design decisions.

  Validation:
  - Verify that the plan defines two separate activities with an easy route to each.
  - Verify that finger drawing remains available beside image generation from descriptions.
  - Verify the selected provider, model, and quality against the confirmed requirements.
  - Verify the integration statements against FamilyHome source and the MediaOps public contract.
  - Keep account access separate from public model availability and successful image generation.
  - Keep proposed interface behavior and unresolved limits in the planning scope.
  - Review the added prose against the official ASD-STE100 rules and dictionary.
  - Run the language checker, Governor check, issue-format checks, and `git diff --check`.

  References:
  - `service/main.go`: current FamilyHome service.
  - `../MediaOps/docs/mediaops-mcp-spec.md`: MediaOps protocol contract.
  - `../MediaOps/internal/media/mcp/registry.go`: declared image tools and inputs.
  - `../MediaOps/internal/media/image/capabilities/catalog.go`: image model and quality definitions.
  - [OpenAI image generation and cost estimates](https://developers.openai.com/api/docs/guides/image-generation#calculating-costs).
  - [OpenAI guidance for children](https://developers.openai.com/api/docs/guides/safety-checks/under-18-api-guidance).

- [ ] [P002] (P1) Plan authentication, device pairing, and data ownership for multiple families.
  Goal:
  One MPR-hosted service supports invited families with separate identities, data, devices, and usage limits.
  This issue authorizes technical planning and documentation only.
  Application implementation requires a separate request after resolution of the open decisions.

  Confirmed requirements:
  - Use an MPR-hosted pilot for invited households.
  - Keep the initial architecture simple.
  - Define authentication and family isolation before public announcements.
  - Prepare the README, screenshots, and local launch drafts while public announcements remain on hold.
  - Preserve existing child profiles and drawings during the eventual migration.
  - Keep P001 as the Google Calendar planning issue.
  - Retain the selected MIT license as a separate distribution decision.

  Current source evidence:
  The source review date is 2026-09-04.
  `service/main.go` reads one `FAMILYHOME_DEVICE_TOKEN` in `loadConfig`.
  Its `authenticate` middleware compares every bearer credential with that shared value.
  `android/build.sh` writes the credential into `RuntimeConfig` during each APK build.
  `PortalConfig.authorize` attaches that value to Android service requests.

  `ProfileStore` keeps profiles in local `SharedPreferences`, with no backend family registry.
  `completeAsk` accepts caller-supplied `profile_id` and `name` without an ownership lookup.
  `nextCalendarEvent` fetches a caller-supplied HTTP or HTTPS URL.

  `saveDrawing` stores images in one directory, with the caller-supplied profile ID in each filename.
  `getDrawing` permits recipient access through random public URLs.
  The service has no database, parent membership, device registry, or individual credential revocation.

  The deployment manifest declares one API service, one retained volume, and one shared LLM Proxy credential.
  Existing service tests cover the shared bearer gate and public drawing links, but not two-family isolation.

  Planning boundaries:
  The following design is a proposal, not a claim about implemented behavior.
  P002 owns the detailed technical proposal. `docs/MULTI-FAMILY.md` provides its overview.
  P001 uses the family, device, and child identities defined here for later Google Calendar access.
  The issues require coordinated decisions, without a circular implementation dependency.
  Family authentication can precede Google Calendar event creation.

  Proposed minimum architecture:
  - Use one FamilyHome API process and one persistent database for the pilot.
  - Use one TAuth application tenant for FamilyHome parent accounts.
  - Store family membership in FamilyHome rather than creating one TAuth tenant per family.
  - Use the existing Go service and retained volume.
  - Evaluate SQLite with transactions and foreign keys for the current single-instance deployment.
  - Keep the parent website as a small static application on GitHub Pages.
  - Keep the API on its separate hostname.
  - Keep drawing documents and running timers local in the initial scope.
  - Synchronize family profile metadata through the backend.
  - Defer service replication, separate services, per-family databases, billing UI, and document synchronization.
  - Use ordinary database queries for pilot limits and usage records before introducing additional infrastructure.

  Proposed identity and role model:
  A parent identity references `(issuer, tauth_tenant_id, account_id)` from a validated TAuth session.
  The account ID remains stable when a parent links sign-in providers.
  Names and email addresses are not primary identity keys.
  One parent can belong to multiple families through separate membership records.
  One family can contain multiple parents, children, and Portals.

  One Portal belongs to exactly one family at a time.
  A child profile belongs to one family. The child selector does not authenticate the physical child.

  The proposed `owner` role controls membership and family deletion.
  The proposed `parent` role manages children, devices, calendar connections, and share links.
  Device credentials permit family activity operations, not parent administration.
  Removal of the last active owner requires ownership transfer or family deletion in the same transaction.
  Guardian relationships, custody rules, and separate child accounts are outside this pilot proposal.

  Proposed persistent records:
  All identifiers are opaque, and timestamps use UTC.
  Lifecycle columns use closed states with database constraints.
  The field names below describe a proposed schema. SQL migration files remain outside this planning issue.

  | Record | Principal fields | Required relationship in the proposal |
  | --- | --- | --- |
  | `parents` | `id`, `issuer`, `tauth_tenant_id`, `account_id`, `created_at` | Unique external identity tuple |
  | `families` | `id`, `name`, `state`, `weather_location`, `created_at`, `revision` | Root of the household data boundary |
  | `memberships` | `family_id`, `parent_id`, `role`, `state`, `revision` | Unique parent membership within a family |
  | `invitations` | `id`, `purpose`, `family_id`, `recipient_identity`, `secret_digest`, `expires_at`, `accepted_by`, `redeemed_family_id`, `state` | Pilot admission or membership in a specified family |
  | `devices` | `id`, `family_id`, `label`, `credential_digest`, `state`, `credential_expires_at`, `created_at`, `last_seen_at` | Unique credential digest and one owning family |
  | `pairing_requests` | `id`, `candidate_digest`, `code_digest`, `expires_at`, `state`, `approved_by`, `family_id`, `device_id`, `attempts` | One candidate credential and at most one resulting device |
  | `children` | `id`, `family_id`, `display_name`, `state`, `revision` | Unique `(family_id, id)` |
  | `calendar_connections` | `id`, `family_id`, `approved_by`, `state`, `encrypted_credentials`, `revision` | Family-owned connection, with provider details defined by P001 |
  | `calendar_assignments` | `family_id`, `child_id`, `connection_id` | Composite references remain within one family |
  | `drawing_exports` | `id`, `family_id`, `child_id`, `device_id`, `storage_key`, `bytes`, `state`, `created_at` | All references resolve within the same family |
  | `share_links` | `id`, `family_id`, `drawing_id`, `secret_digest`, `expires_at`, `revoked_at` | One drawing export and an explicit recipient grant |
  | `usage_records` | `id`, `family_id`, `device_id`, `operation`, `request_id`, `state`, `reserved_units`, `actual_units`, `created_at` | One reservation per admitted operation |
  | `import_receipts` | `family_id`, `device_id`, `import_id`, `payload_digest`, `profile_mapping`, `state` | One result for each migration input |
  | `idempotency_records` | `principal_id`, `operation`, `key_digest`, `request_digest`, `result_reference`, `expires_at` | Retries remain bound to one caller and payload |

  Composite foreign keys enforce family ownership across devices, children, calendar assignments, and drawing exports.
  Every connection enables database foreign-key enforcement.
  Indexes start with `family_id` for family collections and resource lookups.
  Device credential digests and active pairing-code digests have unique lookup constraints.
  Membership updates, pairing approval, device activation, quota reservation, and import receipts use transactions.

  A schema revision table controls forward-only migrations.
  Database backups and drawing storage require one consistent restore procedure.
  SQLite selection must include a Go driver compatible with the current `CGO_ENABLED=0` build, or an explicit build-contract change.

  Proposed parent authentication contract:
  - Use TAuth and `mpr-ui` for browser sign-in, account identity, session restoration, refresh, and logout.
  - Use `/config-ui.yaml` as the application browser-authentication input.
  - Use the official TAuth session validator in the Go backend.
  - Configure the exact issuer, application tenant, session cookie, and signing-key input.
  - Verify the expected tenant claim explicitly if the released validator does not enforce it.
  - Enable the TAuth account model that provides stable opaque account IDs.
  - Resolve current family membership from the database on each protected parent request.
  - Keep app-owned passwords, session issuance, refresh handling, and login forms outside FamilyHome.
  - Require successful invitation acceptance before a signed-in parent can create or join a pilot family.
  - Bind invitation acceptance to the intended verified identity and one permitted use.
  - Reject a session from another application tenant even when the session is otherwise valid.
  - Define exact CORS origins and credentialed browser requests for the selected website and API hosts.
  - Protect browser mutations with a reviewed CSRF mechanism and permitted-origin checks.
  - Keep Google Calendar consent separate from parent sign-in.
  The invitation purpose distinguishes pilot admission from access to an existing family.
  The family-creation transaction consumes accepted pilot admission and creates its first owner together.
  It records `redeemed_family_id` once to prevent repeated family creation from the same invitation.
  Existing-family invitations grant a specified role after the current inviter authority is verified again.

  Proposed pairing protocol:
  This proposal uses application-owned device enrollment while TAuth continues to authenticate the parent.
  The inspected TAuth source has no device-code grant implementation.
  An OAuth device grant would require a separate TAuth extension.
  The proposed protocol uses one locally generated candidate credential to remove raw credential delivery from the server.

  1. Generate a 256-bit random candidate credential with Android `SecureRandom`.
  2. Encrypt the credential with an Android Keystore key before the first network request.
  3. Create a pairing request with the candidate SHA-256 digest and a client idempotency key.
  4. Return an opaque request ID, a display code, an expiration time, and a polling interval.
  5. Display the request URL as a QR code and show the code separately.
  6. Authenticate the parent through the shared browser flow.
  7. Approve the request with the displayed code and a family selected through current membership.
  8. Poll the request from the Portal with the candidate credential in the authorization header.
  9. Show the approved family and device label on the Portal.
  10. Confirm the family on the Portal through an authenticated confirmation resource.
  11. Atomically create the active device with the same credential digest.
  12. Retrieve family profiles through the active device session.


  Before activation, the candidate credential grants access only to its own pending pairing resource.
  After activation, the same credential identifies the resulting device.
  The server returns activation status and device identity, not a recoverable raw credential.
  After a lost activation response, the Portal retries confirmation with the same credential and receives the same device identity.
  The code and QR URL contain no device credential or parent session material.
  The browser never receives the device credential.

  Active pairing-receipt access also verifies the resulting device state, including revocation.

  Initial response-loss recovery must recreate the same display response or start a new request after explicit cancellation.
  Select that recovery behavior before implementation. Any temporary recovery material requires bounded encrypted storage and expiration.

  | State | Permitted transition | Transaction condition |
  | --- | --- | --- |
  | `pending` | `approved`, `cancelled`, `expired` | Code, expiry, attempt budget, parent identity, and membership are valid |
  | `approved` | `active`, `cancelled`, `expired` | Candidate credential matches and the approving membership remains valid |
  | `active` | Terminal pairing state | Repeated confirmation returns the same device identity |
  | `cancelled` | Terminal | A new request is required |
  | `expired` | Terminal | A new request is required |


  Concurrent approvals for different families produce one winner and an explicit conflict for the other parent.
  Polling uses the candidate credential, not the short display code.
  Pending requests have no family-resource access.
  Creation, approval attempts, and polling have separate limits.
  Expiration, poll intervals, code alphabet, and attempt limits remain configurable decisions.

  Proposed device lifecycle:
  - Store the raw credential only in encrypted Android storage protected by Android Keystore.
  - Store only its digest and lifecycle metadata in the service database.
  - Verify the credential and current device state on each protected request.
  - Resolve `device_id` and `family_id` from that verified record.
  - Reject unknown, expired, or revoked credentials with `401` before resource access or provider execution.
  - Keep each device record bound to its original family.
  - Use removal and a new enrollment for transfer to another family.
  - Preserve the current device state when parent browser logout occurs.
  - Make device revocation an explicit parent operation.
  - Define expiry and renewal before selecting any access-token or refresh-token mechanism.
  - Require new pairing when local credential recovery is impossible.
  Revocation prevents admission of new work after its database commit.
  Previously admitted external work can still complete. The service rechecks device state before returning protected results or exposing new shares.
  The pilot policy must define accounting and storage treatment for that in-flight work.
  Family suspension applies to all device and parent operations for that family.

  Proposed HTTP resource surface:
  This table is a design input for OpenAPI. These routes do not exist yet.
  `fid`, `cid`, `did`, and `pid` below denote family, child, device, and pairing identifiers.

  | Method and resource | Caller | Representation or result |
  | --- | --- | --- |
  | `GET /v1/me` | Parent | Validated identity and accessible family summaries |
  | `POST /v1/invitations/{id}/acceptances` | Intended parent | Consume the invitation under the verified identity |
  | `POST /v1/families` | Admitted parent | Create the family and initial owner atomically |
  | `GET /v1/families/{fid}` | Family parent | Family settings and revision |
  | `PATCH /v1/families/{fid}` | Family parent | Explicit settings patch with `If-Match` |
  | `DELETE /v1/families/{fid}` | Owner | Durable family deletion request |
  | `GET /v1/families/{fid}/memberships` | Family parent | Family membership collection |
  | `POST /v1/families/{fid}/invitations` | Owner | Invitation to a specified parent and role |
  | `PATCH /v1/families/{fid}/memberships/{id}` | Owner | Membership change with the last-owner invariant |
  | `GET, POST /v1/families/{fid}/children` | Family parent | Profile collection or new child profile |
  | `PATCH /v1/families/{fid}/children/{cid}` | Family parent | Versioned child-profile patch |
  | `GET /v1/families/{fid}/devices` | Family parent | Device labels, state, and last contact |
  | `DELETE /v1/families/{fid}/devices/{did}` | Family parent | Revoke the device while retaining the internal receipt |
  | `POST /v1/pairing-requests` | Unpaired Portal | Candidate digest and request metadata |
  | `GET /v1/pairing-requests/{pid}` | Matching candidate | Pending status or resulting device identity |
  | `PUT /v1/pairing-requests/{pid}/approval` | Parent | Display code, selected family, and proposed device label |
  | `PUT /v1/pairing-requests/{pid}/confirmation` | Matching candidate | Confirm the approved family and activate once |
  | `DELETE /v1/pairing-requests/{pid}` | Matching candidate or approving parent | Cancel a pending request |
  | `GET /v1/device` | Active device | Its device identity, family settings, and permitted feature state |
  | `GET /v1/device/children` | Active device | Profiles for its stored family |
  | `GET /v1/children/{cid}/agenda` | Active family device | Events from the child's owned calendar assignments |
  | `POST /v1/children/{cid}/questions` | Active family device | Typed question or audio upload under one question contract |
  | `POST /v1/children/{cid}/drawing-exports` | Active family device | PNG upload and owned export metadata |
  | `GET /v1/device/weather` | Active device | Forecast for its stored household location |
  | `POST /v1/drawing-exports/{id}/share-links` | Family parent | Explicit recipient grant for one owned image |
  | `DELETE /v1/share-links/{id}` | Family parent | Revoke an owned share grant |
  | `GET /shares/{capability}` | Recipient | One image while the capability remains active |
  | `GET /v1/families/{fid}/usage` | Family parent | Aggregated usage and remaining limits |

  Calendar-connection resources and child event creation require the final contract from P001.
  The proposed sharing policy gives parents control over public links. Child-initiated sharing requires a separate explicit decision.
  Family deletion returns `202` with a status resource when removal continues after the request.

  Collection creation returns `201` and `Location`. Reads and representation updates return `200`.
  Completed deletion returns `204`. Invalid authentication returns `401` with the applicable challenge.
  An authenticated caller without the required role receives `403` for a resource visible to that caller.
  A resource outside the caller's family returns `404`, as does an absent resource.

  Malformed JSON returns `400`, semantic validation returns `422`, and unsupported content types return `415`.
  State conflicts return `409`. Stale `If-Match` values return `412`.
  Request limits return `429` and `Retry-After`. Provider failures use an explicit `502` or `503` error code.

  The error envelope is `{"error":{"code":"...","message":"...","request_id":"..."}}`.
  Protected and pairing responses use `Cache-Control: no-store`.
  Family collections have bounded page sizes and cursor pagination.

  Retry-sensitive creation uses `Idempotency-Key` with caller, operation, and request-digest binding.
  A reused key with a different request produces `409`.
  The endpoint contract chooses one caller class for each route. It does not try alternative credential classes after authentication failure.

  Proposed resource-isolation invariants:
  - Construct typed `ParentPrincipal`, `DevicePrincipal`, and `PairingPrincipal` values only after boundary validation.
  - Pass a verified family context to storage methods.
  - Include `family_id` in each owned-resource lookup and mutation.
  - Validate parent membership before accepting a family selected in a URL or payload.
  - Derive device family identity only from the stored device record.
  - Verify child ownership before reading calendar data, saving exports, or calling LLM Proxy.
  - Use stored child names when constructing Ask requests.
  - Enforce ownership for share creation, revocation, and deletion.
  - Preserve intentional recipient access as an explicit share capability.
  - Use opaque storage keys instead of caller-supplied names or paths.
  - Apply family ownership to cached responses, idempotency records, import receipts, and usage records.
  - Prevent suspended-family requests from reaching providers or storage mutations.
  - Verify membership again inside transactions that grant devices or change membership.

  Proposed calendar, drawing, and Ask changes:
  Calendar requests refer to parent-managed connection records rather than client-supplied fetch URLs.
  If ICS remains during the first stage, its stored URL requires destination validation before each connection and redirect.
  That validation excludes loopback, private, link-local, and other internal destinations after DNS resolution, including IPv6 addresses.
  The connection must use the validated destination to prevent DNS changes from bypassing validation.
  Private calendar URLs and OAuth credentials require encrypted backend storage and exclusion from request logs.
  P001 replaces the ICS provider contract in one coordinated change after its separate decisions.

  Drawing upload validates the decoded image, byte limit, and pixel dimensions before storage.
  The proposed export lifecycle is `staging -> ready -> deleting -> deleted`.
  Temporary files and database records require deterministic cleanup after a crash.
  Only `ready` exports can have recipient links.
  Share links use random capabilities whose digests and lifecycle state reside in the database.
  The link contains no child name or profile identifier.

  Family deletion immediately disables recipient access and schedules removal of image bytes.

  Ask uses the existing official LLM Proxy client and server-owned provider configuration.
  Admission atomically reserves family capacity before a provider request.

  The usage lifecycle is `reserved -> completed | failed | unknown`.
  A timeout after dispatch has an unknown provider outcome and requires explicit reconciliation.
  An identical request retry must not cause an automatic second paid request.
  The plan must define the response-retention window and the policy for unknown outcomes.

  Rate limits, concurrent work, storage bytes, and provider budgets remain separate limits.
  FamilyHome owns family accounting. LLM Proxy continues to own provider execution.
  Access logs exclude bearer credentials, share capabilities, private calendar URLs, audio bodies, and question text.

  Audit records retain operation, family, device or parent identity, request ID, and outcome under a defined retention policy.

  Proposed Android and offline behavior:
  - Remove the build-time device credential after the migration.
  - Keep one public service address in the APK configuration.
  - Add explicit unpaired, pairing, paired, expired, revoked, and disconnected states.
  - Keep drawing, music, and timers available without backend connectivity.
  - Cache profile metadata by family and device identity with a server revision.
  - Keep parent sessions and Google credentials off the Portal.
  - Manage paired-family administration through the authenticated parent website.
  - Define local profile creation while unpaired and its later import as explicit product behavior.
  - Keep cloud operations visibly unavailable when the device lacks valid service access.
  - Prevent automatic replay of audio uploads or paid Ask requests after reconnection.
  - Preserve saved local work when the service returns an error.
  - Require an explicit local data-removal procedure before transfer to another family.
  Remote revocation stops service access but cannot erase a disconnected device.
  The child selector provides profile organization, not secrecy between children who share physical access.
  Offline deletion, parent controls on the device, and locally retained work require a documented parent policy.

  Proposed implementation ownership:
  `service/main.go` remains the composition and HTTP startup entry point.
  Proposed internal packages separate validated identities, family storage, pairing, and existing product operations.
  Suggested boundaries are `internal/identity`, `internal/families`, `internal/pairing`, `internal/storage`, and `internal/httpapi`.
  Create only packages required by the actual implementation. Keep narrow typed interfaces at storage and provider boundaries.

  A new Android credential store owns Keystore operations and device lifecycle state.
  `PortalConfig` consumes that store for requests and removes its generated bearer-token input.
  `ProfileStore` owns local data and the bounded import mapping.
  `MainActivity` and `SettingsActivity` render the device state and parent connection flow.
  `AskActivity` and `DrawingActivity` use the current owned-resource contracts.

  The parent website uses declarative `mpr-ui` authentication through `/config-ui.yaml`.
  OpenAPI, client calls, server routes, and public-entrypoint tests change together.
  `Dockerfile` must include nested Go packages if the service source layout changes.

  Proposed deployment inputs:
  - Declare the parent website with `github_pages` and publication branch `gh-pages`.
  - Declare the TAuth tenant through the existing selected-manifest resource contract.
  - Record the exact frontend origin, API origin, auth origin, cookie scope, issuer, and tenant ID.
  - Record provider callbacks separately from the parent website return URL.
  - Keep provider interpretation in TAuth and generic orchestration in the gateway.
  - Add the database path, credential-encryption input, pairing policy, and quota policy to the canonical backend configuration.
  - Remove `FAMILYHOME_DEVICE_TOKEN` from the manifest and private input contract after migration.
  - Preserve the existing LLM Proxy secret boundary and retained application data.
  - Use `make release && make publish && make deploy` for the separately authorized backend lifecycle.
  - Qualify APK publication and real-device installation separately from backend deployment.
  No production hostname or authentication cookie name is selected by this issue.

  Proposed forward-only migration:
  1. Inventory the current Portal profiles, local drawings, server exports, and signing identity.
  2. Create a tested backup of the existing data before the maintenance window.
  3. Create the first family and owner through an explicit operator-approved ownership assignment.
  4. Pair the existing Portal under that family.
  5. Import profile metadata with an import ID and content digest.
  6. Preserve profile IDs when possible, or persist a complete old-to-new mapping before local replacement.
  7. Apply that mapping to local drawing associations and calendar assignments.
  8. Assign existing server exports through an explicit inventory rather than trusting filename prefixes.
  9. Verify counts, image hashes, profile associations, and repeat-import behavior.
  10. Install the signed Android update with `adb install -r`.
  11. Cut over the API, client, and manifest to the single device-authentication contract.
  12. Verify rejection of the former shared credential.
  13. Remove the temporary importer after verified completion.

  The importer requires current parent and device authentication or an explicit offline operator operation.
  Possession of the old shared credential does not prove ownership of any imported family data.
  Existing capability links require an explicit preserve-or-revoke decision before the migration.
  Any retained link becomes an owned grant in the new share model, not a parallel legacy file-serving path.
  Repeated imports with the same digest return the stored receipt. Changed content under the same import ID returns `409`.
  Import and cutover failures use verified backups or a forward correction, without a persistent dual-authentication mode.
  The maintenance sequence must specify the point after which the old app loses connected access.

  Proposed implementation stages after approval:
  | Stage | Scope | Exit evidence |
  | --- | --- | --- |
  | 1 | Persistent family records, TAuth parent integration, admission, and membership | Two parent accounts resolve to separate families through real browser sign-in |
  | 2 | Pairing and individual device lifecycle | Two devices pair to different families, and revocation affects one device only |
  | 3 | Owned profiles, calendars, drawings, shares, and Ask accounting | Swapped identifiers fail before reads, writes, or provider calls |
  | 4 | Local-data import and shared-credential removal | Existing profiles and drawings survive, and the former token fails |
  | 5 | Hosted acceptance, backup restore, signed APK, and pilot readiness | A new invited household completes installation and pairing without developer assistance |
  Each stage becomes a separately scoped implementation issue after its contract is approved.
  Public announcements follow stage 5 and explicit publication authorization.

  Proposed acceptance scenarios for later implementation:
  - Complete parent sign-in through the real shared TAuth and `mpr-ui` flow.
  - Reject missing, expired, wrongly signed, and wrong-tenant parent sessions.
  - Verify that an authenticated but uninvited parent receives no family access.
  - Accept one invitation once, for its intended identity and role.
  - Reject expired invitations and invitations from an owner who lost the necessary authority.
  - Verify one parent with two family memberships and two parents in one family.
  - Prevent removal of the last active owner under simultaneous membership changes.
  - Pair two devices to two different families through separate parent sessions.
  - Reject wrong codes, expired requests, excess attempts, and candidate credentials for another pairing request.
  - Verify one winning approval when two parents select different families simultaneously.
  - Repeat activation after a lost response and obtain the same device identity.
  - Reject pending candidate credentials on all family-data routes.
  - Revoke one device and verify that another device in the same family still works.
  - Verify that browser logout does not silently revoke family devices.
  - Swap family, child, calendar, drawing, share-management, and usage identifiers between families.
  - Verify equal external behavior for absent resources and resources in another family.
  - Verify that rejected requests make no provider call and create no export or usage charge.
  - Verify that public share access is limited to the referenced active image.
  - Verify share revocation, expiration, and family deletion through the recipient URL.
  - Reject internal calendar destinations, redirects to internal addresses, and DNS rebinding attempts.
  - Verify quota reservation under simultaneous requests from multiple devices in one family.
  - Verify that an idempotent retry does not issue a second provider request.
  - Verify explicit handling of unknown provider outcomes after a timeout or process crash.
  - Verify consistent database and image recovery after restart and backup restore.
  - Verify offline music, drawing, and timers before and after successful pairing.
  - Verify preservation of local work after expiration, revocation, and network failure.
  - Verify family transfer only after the documented local-data operation.
  - Repeat the migration and verify stable profile counts and drawing associations.
  - Verify that the distributed APK contains no shared deployment credential.
  - Verify that the old bearer credential fails after the coordinated cutover.

  Open Decisions:
  - Confirm the proposed family cardinalities and owner/parent role permissions.
  - Select the first parent sign-in provider and verify its released shared authentication contract.
  - Select the parent website hostname, TAuth profile, cookie names, and CSRF mechanism.
  - Select SQLite and a compatible Go driver, or document the concrete need for another database.
  - Confirm candidate-credential pairing versus a separately owned TAuth device-grant extension.
  - Define response-loss recovery for initial pairing creation.
  - Select pairing-code entropy, lifetime, poll interval, and attempt limits.
  - Define device credential expiration, renewal, and account-disable behavior.
  - Define pilot invitation issuance and ownership of the initial family-creation operation.
  - Select family request, concurrency, storage, and provider-budget limits.
  - Define idempotency retention, unknown provider outcomes, and usage reconciliation.
  - Define audit, question-result, drawing, share-link, and backup retention.
  - Confirm whether children can create public share links or require parent approval.
  - Define local profile creation, parent controls, revocation behavior, and device-transfer data removal.
  - Select the initial ICS ownership scope and its transition to the P001 Google Calendar contract.
  - Assign ownership for existing profiles and server exports and select the treatment of existing share links.
  - Define the maintenance window, backup restore procedure, and migration completion evidence.

  Deliverables:
  - Resolve each open decision with its owner, selected value, rationale, and acceptance consequence.
  - Record the approved schema, state machines, role matrix, and OpenAPI contract.
  - Record the Android credential lifecycle and offline-data contract.
  - Record the canonical deployment inputs and shared integration dependencies.
  - Record a bounded migration procedure and a two-family acceptance matrix.
  - Create separately scoped implementation issues only after approval of the design.
  - Keep the issue open until the technical decisions are resolved.

  Validation:
  This planning issue completes through document and source review, not application implementation.
  - Verify the current-state statements against the named source functions and deployment contract.
  - Verify the schema and route proposal against the identity, role, and family-isolation invariants.
  - Trace initial response loss, repeated activation, revocation, and migration retry through the proposed state machines.
  - Verify that every unresolved policy or parameter remains in Open Decisions.
  - Verify that P001 and P002 use the same parent, family, device, and child ownership model.
  - Review changed prose against the official ASD-STE100 rules and dictionary.
  - Run the document language checker, Governor check, issue-format checks, and `git diff --check`.
  Later implementation uses failing integration tests through the real HTTP listener and database before production-code changes.
  Hosted browser sign-in, backend deployment, Android installation, and real-device acceptance remain separate qualification results.

  Current planning validation:
  The document language checker, Governor check, issue-format checks, source-reference checks, and `git diff --check` passed.
  The producing-agent language review covered P002, its P001 cross-reference, the overview changes, and the added technical terms.
  The issue remains open for design decisions. Application acceptance remains future work.

  References:
  - `docs/MULTI-FAMILY.md`: design overview.
  - `service/main.go`, `service/main_test.go`: existing backend behavior and tests.
  - `android/build.sh`, `android/app/src/main/java/com/mprlab/portal/PortalConfig.java`: current APK credential contract.
  - `android/app/src/main/java/com/mprlab/portal/ProfileStore.java`: local profile persistence.
  - `android/tests/upgrade-persistence.sh`: existing data-preservation test entry point.
  - `.mprlab/deploy/resources.yml`, `Dockerfile`, `Makefile`: deployment, build, and lifecycle contracts.
  - `github.com/tyemirov/tauth/pkg/sessionvalidator`: parent session validation boundary.
  - [OAuth device-flow considerations](https://www.rfc-editor.org/rfc/rfc8628.html).
  - [Android Keystore](https://developer.android.com/privacy-and-security/keystore).

- [ ] [P001] Plan Google Calendar access for parents and children.
  Goal:
  This issue defines the calendar plan for FamilyHome.
  P002 defines the proposed shared parent, family, device, and child identity model.
  The calendar design requires the same ownership model before implementation.
  Each child sees a combined agenda from the family calendar and that child's personal calendar.
  The parent controls the calendars through Google Calendar.
  Children can add events to their personal calendars from the Portal.
  This issue authorizes documentation only. Implementation requires a separate request.

  Current state:
  The Home control `Add or change calendar` opens Settings.
  Settings accepts a private iCalendar link for each child profile.
  The backend operation `GET /v1/calendar/next` reads one upcoming event.
  The application has no Google OAuth connection, pairing code, or calendar event creation operation.
  Child profiles remain on the Portal. The backend accepts a bearer credential supplied with the Android build.

  Requirements:
  - Keep Google Calendar as the authoritative source for calendar events.
  - Give the parent ownership of one family calendar and one personal calendar for each child profile.
  - Show family events to every child profile in that family.
  - Show personal events only in the corresponding child profile.
  - Combine the two event lists in time order for each child's agenda.
  - Keep each event in its original calendar.
  - Save each child's new event to that child's personal calendar.
  - Show the child's new event in the parent's Google Calendar.
  - Show parent changes from Google Calendar in the corresponding Portal agendas.
  - Complete calendar authorization on the parent's phone or computer.
  - Connect that authorization to the specified Portal through a QR code or pairing code.
  - Keep calendar connection controls in parent settings.
  - Keep the Home widget within its current dimensions.
  - Keep automatic email interpretation outside the first implementation.
  - Permit the parent to enter email-derived events through Google Calendar.

  Proposed design:
  - Create `FamilyHome — Family` and `FamilyHome — <child name>` in the parent's Google account after parent confirmation.
  - Use the Google scope `calendar.app.created` for those calendars.
  - Reuse the existing FamilyHome backend for Google authorization and calendar API access.
  - Keep Google credentials and refresh tokens in protected backend storage.
  - Record the relationships between the Google account, Portal identity, child profile IDs, and calendar IDs.
  - Give each Portal its own revocable credential.
  - Use a pairing code with an expiration time and a single permitted use.
  - Show the Portal's existing child profiles on the parent connection page before calendar creation.
  - Keep the Google OAuth callback on the API hostname.
  - Host the parent browser frontend on GitHub Pages under the current Governor contract.
  - Show the next event and a short agenda in the widget under `My day`.
  - Use `See my week` to open a larger agenda.
  - Use `Add a plan` to open a title, date, and time editor.
  - Permit children to change their own additions through backend authorization.
  - Restrict changes to parent-created events to the parent.
  - Refresh the agenda when it opens and every minute while visible.
  - Refresh the agenda after a successful event creation.
  - Show saved events with their last update time when the connection is unavailable.
  - Keep an unsaved draft until Google confirms the save.
  - Define correct behavior for repeated events, all-day events, time zones, and cancellations.
  - Prevent duplicate events after repeated submissions.
  - Define conflict handling for simultaneous parent and child changes.
  - Replace the iCalendar link contract in one coordinated client and backend change.
  - Preserve existing child profile IDs and unrelated child data during that change.

  Open Decisions:
  - Confirm calendar creation and names for the first implementation.
  - Decide whether connection to existing calendars requires later work and different Google permissions.
  - Select the parent verification method for settings and reconnection.
  - Select the website hostname and publication repository for the parent connection page.
  - Record the separation between the website and API hostnames in the final deployment plan.
  - Select the persistent storage system and credential protection method.
  - Define device enrollment, credential replacement, revocation, and parent session expiration.
  - Confirm child edit and deletion permissions, including the result of a later parent edit.
  - Define disconnect behavior and the treatment of calendar data after reconnection.
  - Confirm the refresh interval, agenda date range, and draft retention behavior.
  - Record the required Google project configuration, OAuth client, consent status, and callback URI.
  - Verify available private configuration before declaring a credential blocker.

  Deliverables:
  - Record an approved product contract after resolution of the open decisions.
  - Define the REST resources for device connections, child calendar assignments, agendas, and event creation.
  - Define the backend authorization checks for each resource.
  - Define the persistent data schema and the removal of obsolete calendar fields.
  - Specify the three implementation stages below.
  - Keep source validation, backend deployment, Android installation, and real-device acceptance as separate gates.

  Implementation stages after approval:
  1. Connect: Complete parent authorization, Portal pairing, calendar creation, and persistent calendar assignments.
  2. Read: Show the combined agenda with changes from Google Calendar and explicit connection status.
  3. Write: Create child events with backend authorization, duplicate prevention, and confirmation from Google Calendar.

  Acceptance scenarios for later implementation:
  - Connect the physical Portal from a separate phone or computer.
  - Verify that the calendar assignments survive a backend restart and an application restart.
  - Verify that Alice sees events from the family calendar and Alice's personal calendar.
  - Verify that Peter sees events from the family calendar and Peter's personal calendar.
  - Verify that a personal event remains outside the other child's agenda.
  - Change an event through the parent's Google Calendar.
  - Verify the corresponding change on the physical Portal.
  - Add an event through Alice's Portal profile.
  - Verify that the parent's Google Calendar shows that event once in Alice's personal calendar.
  - Verify that the event remains outside the family calendar and Peter's personal calendar.
  - Verify that the widget fits the physical Portal screen without clipped text or controls.
  - Extend the acceptance scenarios after approval of the remaining design decisions.

  Validation:
  - Review the plan against the confirmed requirements.
  - Verify that each proposed behavior remains separate from confirmed requirements until approval.
  - Verify that each implementation stage has an observable acceptance result.
  - Verify that this issue changes no application code, Google account, calendar, or runtime.
  - Run the Governor check and `git diff --check` for this documentation change.

  References:
  - [Google Calendar permission scopes](https://developers.google.com/workspace/calendar/api/auth).
  - [Google web-server authorization](https://developers.google.com/identity/protocols/oauth2/web-server).
  - [Google Calendar event retrieval](https://developers.google.com/workspace/calendar/api/v3/reference/events/list).

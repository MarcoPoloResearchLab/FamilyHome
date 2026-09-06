# FamilyHome for iOS

## Status and purpose

F009 records the iOS client with equivalent family activities to FamilyHome on Android.
The user requested this assessment and an actionable issue on 2026-09-06.
This document defines proposed implementation work. The current request covers documentation and issue creation.
Application implementation, store submission, and production operations remain separate work.

The confirmed product is a normal iOS application with functional parity.
The documented device scope includes iPhone and iPad.
FamilyHome keeps its Android Home role on Android. On Apple devices, Home means the screen inside the FamilyHome application.
The existing Go service remains the backend for connected activities.
Framework, device limits, game delivery, and distribution choices remain proposals until F009 records the selected values.

## Difficulty and current evidence

The engineering effort for the core iOS application is moderate, with additional layout qualification across iPhone and iPad.
A new Apple implementation is necessary for most client code because the current interface and device adapters use Android APIs.
The existing product behavior, artwork, sound formulas, and backend contracts provide reusable inputs.
The separate games create the largest uncertainty for full product parity.

The source review date is 2026-09-06. The primary checkout contains active Android changes.
The initial estimate used approximately 5,300 lines of application Java, excluding tests and separate games.
This count describes the reviewed source size. It is not a measured development rate.
No Apple build or device qualification occurred during this assessment.

| Evidence | Current contract | Consequence for iOS |
| --- | --- | --- |
| [Android guide](../android/README.md) and [manifest](../android/app/src/main/AndroidManifest.xml) | Native Java activities and Android Home registration | New Apple screens and navigation |
| [ProfileStore](../android/app/src/main/java/com/mprlab/portal/ProfileStore.java) | Local child profiles and settings | Native storage with stable child associations |
| [DrawingActivity](../android/app/src/main/java/com/mprlab/portal/DrawingActivity.java) | Custom drawing controls and saved documents | New touch renderer and document adapter |
| [PianoActivity](../android/app/src/main/java/com/mprlab/portal/PianoActivity.java) and [GuitarPlayer](../android/app/src/main/java/com/mprlab/portal/GuitarPlayer.java) | Local audio generation through Android audio APIs | Reusable formulas with a new audio adapter |
| [PhotoBoothActivity](../android/app/src/main/java/com/mprlab/portal/PhotoBoothActivity.java) and [PortalCamera](../android/app/src/main/java/com/mprlab/portal/PortalCamera.java) | Camera2 capture and profile albums | Apple camera capture with the accepted product behavior |
| [PortalActivity](../android/app/src/main/java/com/mprlab/portal/PortalActivity.java) | Shared navigation and screensaver lifecycle | One application lifecycle with explicit resource release |
| [GameLauncher](../android/app/src/main/java/com/mprlab/portal/GameLauncher.java) and [game guide](../android/GAMES.md) | Five separately installed Android games | A separate delivery decision for each game |
| [Go service](../service/main.go) and [service guide](../service/README.md) | HTTP operations for Ask, calendar, weather, and drawing links | Reuse backend ownership and define an Apple backend client |

## Functional parity scope

The target is the same set of family activities, adapted to Apple devices and normal application navigation.
Phone and tablet layouts are part of the same product scope.
The parity matrix below defines the work. It gives the behavior and acceptance criteria for platform differences.

| Capability | Required iOS behavior | Work size |
| --- | --- | --- |
| Home and profiles | Select a child, open activities, and keep separate local work | Medium |
| Settings and timers | Retain preferences, show turn deadlines, and report notification availability | Medium |
| Drawing | Draw with fingers, select tools, save documents, and reopen the correct child's work | Medium |
| Piano and guitar | Play simultaneous notes and chords without a service connection | Medium to high |
| Photo Booth | Capture single pictures and four-picture strips, select frames, and manage child albums | Medium to high |
| Screensaver | Provide Disabled, Clock, and Black screen choices within the application | Medium |
| Weather | Show the backend forecast, location, attribution, and explicit connection state | Low to medium |
| Calendar | Show the approved backend calendar result for the selected child | Medium, with backend dependency |
| Ask | Submit text or recorded voice and show an explicit result or failure | Medium, with release-policy dependency |
| Drawing links | Create a share through the approved family and parent contract | Medium, with backend dependency |
| Parent setup | Enroll the device and obtain family profiles through the shared family contract | High, until P002 implementation exists |
| Games | Provide the selected iOS game experience with a recorded disposition for each Android game | High and unestimated until source qualification |

The first version keeps local drawing, music, timers, and Photo Booth without a service connection.
Profile selection organizes local data. It does not authenticate the child or provide secrecy between children with device access.
Photo Booth uses the accepted single-picture, strip, frame, storage, and deletion requirements from P004 and F003 through F007.
F008 defines additional filters. Stage 0 must reconcile its accepted Android behavior with the iOS parity baseline.

Separate scope decisions are necessary before cloud document synchronization or transfer of existing Portal files enters this work.
Purchases, advertisements, AI photo transformations, and automatic installation of other applications are outside this proposal.
The estimate gives core activities and game-specific work separately. A recorded decision for each game remains necessary for feature parity.

### Platform differences

| Area | Android contract | iOS contract |
| --- | --- | --- |
| System Home role | FamilyHome can be selected as the Android Home application | The user opens FamilyHome as a normal application |
| FamilyHome Home control | Returns to the FamilyHome activity screen | Returns to the FamilyHome screen within the application |
| Device restriction | Depends on the selected Android installation and device setup | Guided Access is an optional parent setup choice |
| Activities | Native activities and separately installed game APKs | Native application screens and a selected Apple delivery model for games |
| Inactive timers | Android alarm behavior | Stored deadlines and system local notifications under the selected alert contract |
| Screen geometry | Current Portal layout at 1280 by 800 | Adaptive layouts for supported iPhones, iPads, and application window sizes |

Replacement of the Apple system Home screen is outside the requested capability.
Every core activity must work during normal application use with Guided Access disabled.
Optional Guided Access must not become a prerequisite for setup, navigation, or local activities.

## Proposed architecture

### Application and platform adapters

Swift with SwiftUI is the recommended starting point for screens and navigation.
UIKit can supply custom drawing and instrument controls where direct touch handling is necessary.
The first device experiment must verify these choices before the implementation fixes the framework contract.
The proposal keeps the existing Android application and Go service in this repository.

| Proposed owner | Responsibility | Boundary validation |
| --- | --- | --- |
| Screens and navigation | Render typed state and send explicit user commands | User input and navigation parameters |
| Local repositories | Save profiles, drawing documents, photo albums, and timer state | Files, metadata, capacity, and schema |
| Backend client | Construct requests and convert responses into domain types | HTTP status, payload, and authorization state |
| Credential adapter | Store the individual device credential in Keychain | Secure storage results and device lifecycle |
| Camera adapter | Own capture, orientation, reflection, and camera release | Authorization, camera output, and cancellation |
| Audio adapter | Generate instrument audio and manage interruptions | Audio session, output route, and resource state |
| Timer adapter | Store deadlines and schedule local notifications | Notification authorization and scheduling results |
| Application lifecycle | Coordinate screensaver, foreground changes, and unsaved work | System lifecycle events |

The proposed source root is `ios/`, with one native application target and its integration tests.
The implementation must select the bundle identifier, minimum iOS and iPadOS versions, and Xcode version in stage 0.
Repository Make targets own reproducible build and validation entry points.
Proposed target names appear in the validation section. They do not exist yet.

AVAudioEngine is the proposed audio adapter for simultaneous instrument notes.
Apple documents its audio processing model in [AVAudioEngine](https://developer.apple.com/documentation/avfaudio/avaudioengine).
Formula translation and device measurements are necessary before claims about equivalent sound or touch latency.

Keychain owns device credentials under the [Apple secure storage API](https://developer.apple.com/documentation/security/keychain-services).
The Go backend keeps provider secrets, Google Calendar credentials, family authorization, and provider operations.
The Apple client uses one current backend contract, shared with Android.
Any coordinated API change updates the backend, both clients, and public integration coverage together.

### Local data and resource lifecycle

- Define one current persisted schema for each local resource.
- Keep stable child associations across profile name changes, process restarts, and application updates.
- Save local work before navigation reports completion.
- Complete each photo Save once, including repeated input and interrupted responses from local storage.
- Apply the accepted Photo Booth capacity and image bounds through the Apple storage and renderer adapters.
- Measure actual image memory and temporary storage on the selected iPhones and iPads.
- Show explicit storage errors and keep saved work.
- Release camera and instrument resources on activity exit, background entry, and screensaver entry.
- Cancel incomplete capture sequences and reject late camera callbacks after cancellation.
- Keep completed image review through screensaver entry and wake.
- Consume the first wake touch before activity controls receive input.
- Keep photo albums in application storage under the selected backup and deletion policy.

Portal-specific camera formats and Android activity flags are implementation details of the existing client.
Apple adapters implement the same product results through native platform contracts.
Automatic photo upload and cross-device document transfer remain outside the first version.

### Timers and optional device restriction

The proposed application uses stored deadlines to reconstruct timer state after a process restart or foreground return.
Local notifications supply timer alerts while the application is inactive, subject to system authorization and presentation settings.
Apple documents the scheduling mechanism in [local notifications](https://developer.apple.com/documentation/usernotifications/scheduling-a-notification-locally-from-your-app).

- Cancel or replace the corresponding notification when a timer changes.
- Show an explicit status when notification authorization is denied.
- Verify deadlines after screen lock, process termination, time-zone changes, and manual clock changes.
- Define alert behavior under Focus and silent settings during stage 0.
- Verify in-application timer completion independently from system notification presentation.

FamilyHome uses normal application navigation on iPhone and iPad.
Parents can optionally use Guided Access for a restricted activity session.
Apple describes Guided Access as a temporary restriction to one application in its [iPad guide](https://support.apple.com/guide/ipad/lock-ipad-to-one-app-ipada16d1374/ipados).
The optional setup procedure must explain entry, parent exit, and the controls necessary for FamilyHome activities.
The application screensaver is an overlay within FamilyHome. System screen lock remains a separate device behavior.
For browser setup, the parent must exit the restricted session or use another device.

## Backend dependencies and ownership

| Related issue | Existing responsibility | F009 dependency |
| --- | --- | --- |
| P002 | Parent identity, family ownership, device enrollment, revocation, usage, and local-data policy | Required implementation and acceptance before hosted family qualification |
| P001 | Google Calendar consent, assignments, agenda, and event creation | Required when the selected calendar milestone includes those operations |
| P004 and F003 through F007 | Accepted base Photo Booth behavior and Portal qualification | Product reference for Apple acceptance, with separate Apple device evidence |
| F008 | Additional Photo Booth filters | Reconcile accepted filters with the shared feature baseline before Apple implementation |
| P003 | Proposed image generation | Separate product work outside the first version |

P001 and P002 define proposed backend work. Their closure alone does not prove that the necessary backend operations exist.
F009 must link the resulting implementation issues and their acceptance evidence before hosted qualification.
Offline work and controlled local HTTP integration can proceed before those hosted dependencies finish.

The service currently exposes `/v1/ask`, `/v1/ask/audio`, `/v1/calendar/next`, `/v1/weather`, and `/v1/drawings`.
It currently uses one shared installation credential.
These are source facts, not a proposed Apple release contract.
The hosted Apple client must use individual device enrollment and secure storage under the approved P002 contract.

The prototype can use weather through a local test service with synthetic data and a test credential in Keychain.
The hosted client then implements the selected current family API as one coordinated change.
Each stage must use one credential and resource contract and remove obsolete client paths after the change.

The baseline calendar feature is the next-event display through the current approved backend resource.
Full Google Calendar setup and event creation remain P001 work unless F009 explicitly selects that expanded scope.
The implementation owner must select the calendar contract at stage 0 and verify it again before connected integration.

## Game parity assessment

The existing Android game library launches independent APKs by package and activity name.
Source, integration, distribution, and license decisions are necessary before a reliable estimate for each Apple game.
Each game must have a disposition: an Apple port, an equivalent activity, or an explicitly accepted release exception.
The core engineering estimate does not include game-specific implementation until this assessment completes.
An unfinished game port remains visible in the parity matrix and release scope.

| Game | Repository evidence | Action before its implementation issue |
| --- | --- | --- |
| Blocks | [Flutter adaptation](../games/blocks-portal/README.md) | Verify an Apple build and define navigation inside the selected delivery model |
| Tiles | [Web and Android adaptation](../games/tiles-portal/README.md) | Verify the web build, touch behavior, storage, and permitted Apple packaging |
| Match | [Android adaptation](../games/match-portal/README.md) | Select a new native implementation or a separately qualified source port |
| Kart | [External game catalog](../android/GAMES.md) | Verify an available Apple source port, device performance, and distribution rights |
| Freedoom | [Android engine adaptation](../games/freedoom-portal/README.md) | Select an Apple engine and verify content rights, controls, and age suitability |

An Android license notice or successful Flutter build does not establish Apple distribution acceptance.
Game selection must account for the intended age group and normal iOS navigation.
Any separate-application delivery must record its return flow and limitations during optional Guided Access.
Each selected game receives its own implementation issue, estimate, and device acceptance criteria.

## Implementation milestones and estimates

These estimates assume one experienced iOS engineer at full-time capacity, a working Mac toolchain, and physical iPhones and iPads.
They include implementation and relevant device tests for the proposed core activities.
The ranges are inputs to the implementation plan. They are not delivery commitments or measured results.

| Stage | Deliverable | Exit evidence |
| --- | --- | --- |
| 0: Scope and platform experiment | Parity matrix, devices, OS, framework, layouts, game assessment, service contract, and release route | Real iPhone and iPad drawing input, simultaneous audio, camera capture, and timer experiment |
| 1: Prototype | Home, two profiles, persistent drawing, and one controlled backend feature | Repeatable installation and one complete user flow on iPhone and iPad |
| 2: Core activities | Settings, timers, drawing, instruments, Photo Booth, and screensaver | Offline public integration tests and physical lifecycle evidence |
| 3: Connected family flow | Enrollment, family profiles, weather, selected calendar scope, Ask, and drawing links | Two-family isolation, revocation, errors, and actual provider qualification |
| 4: Release candidate | Accessible phone and tablet layouts, selected games, update preservation, setup guide, and signed artifact | Parity and acceptance matrices, artifact identity, privacy review, and store preparation |

| Cumulative milestone | Estimated engineering time | Conditions |
| --- | --- | --- |
| Working prototype | 2–3 weeks | Stages 0 and 1, with one iPhone, one iPad, and controlled backend data |
| Core application | 6–10 weeks total | Built-in activities, phone and tablet layouts, and connected integration with available backend contracts |
| Core release preparation | 10–16 weeks total | Parent onboarding, accessible interface, real-device tests, and store preparation |
| Final parity release candidate | Core estimate plus selected game work | Game disposition and implementation estimates from stage 0 |

The ranges are cumulative. They must not be added together.
These estimates include phone and tablet layouts and physical acceptance on both device families.
Selected game ports, P001/P002 backend implementation, and Apple review time are additional work or elapsed time.
The release estimate assumes that family authentication and the selected calendar contract are available when stage 3 starts.
If login policy, hardware availability, or feature scope changes, revise the estimate after stage 0.

## Acceptance criteria and test ownership

Each behavior stage starts with a failing integration test through the real application or backend client entry point.
Controlled clocks, storage conditions, camera errors, and HTTP failures enter through their actual adapters.
Routine provider tests use a local protocol implementation. Live-provider qualification remains a separate result.

| Area | Required observable result |
| --- | --- |
| Navigation and profiles | Two child profiles open their own saved work after Home, Back, process restart, and application update |
| Drawing | Finger input, tools, Save, reopen, and confirmed deletion keep the correct document association |
| Instruments | Simultaneous touches produce the selected notes and chords without stuck audio after interruption or exit |
| Photo Booth | Single pictures, four distinct strip images, frames, review, Save, Retake, Discard, albums, and capacity errors pass |
| Camera lifecycle | Denial, orientation, text direction, background entry, screensaver, and repeated capture release pass on physical iPhone and iPad |
| Timers | Deadline reconstruction, cancellation, notification denial, lock, and process termination match the selected alert contract |
| Screensaver | Each mode obeys its timeout and keeps completed work. Wake input activates no underlying control |
| Offline operation | Local drawing, music, timers, and Photo Booth work without network access |
| Connected states | Loading, empty, offline, invalid response, unauthorized, revoked, and provider failure states remain explicit |
| Family access | Two devices in separate families cannot retrieve or modify each other's resources through exchanged identifiers |
| Credential lifecycle | Enrollment, restart, expiration, revocation, and device transfer obey the approved family and Keychain contract |
| Ask and shares | Parent policy, explicit submission, duplicate prevention, cancellation, and recipient access match the approved backend contract |
| Layout and access | Controls remain accessible across permitted window sizes, safe areas, keyboard states, orientations, text sizes, and reduced motion |
| Phone and tablet parity | Every selected activity remains usable on iPhone and iPad, with controls adapted to the available space |
| Normal application use | Launch, internal Home, activity navigation, background return, and local activities work with Guided Access disabled |
| Game disposition | Each Android game has a qualified Apple implementation, equivalent activity, or explicitly accepted release exception |
| Device endurance | Repeated camera and music sessions meet the selected memory, touch-latency, and thermal limits |
| Installation | A signed update keeps local profiles, drawings, albums, settings, and timer state on the selected iPhones and iPads |

Proposed public Make targets are `build-ios`, `test-ios-contract`, `test-ios-ui`, and `test-ios-upgrade`.
The implementation must document their toolchain, device selection, input data, and outputs before use.
Target registration must keep the existing Android and service checks.
After the final implementation change, run the repository CI lane and the applicable Apple targets once.
Document, issue-format, source-reference, Governor, and whitespace checks are necessary for the present documentation task.

## Release preparation

The first distribution decision must distinguish a developer installation, invited TestFlight pilot, and public App Store release.
Build and sign the Apple artifact on an operator-controlled Mac through the native toolchain.
Record its source revision, bundle identifier, version, build number, and SHA-256 before publication.
Publication must consume that fixed artifact through a repository-owned command.

Apple's [App Review Guidelines](https://developer.apple.com/app-store/review/guidelines/) define the store requirements cited below.
The source review date is 2026-09-06. Another review of the current rules is necessary during store preparation.

- Select the age group and category before the release design is final.
- For the Kids Category, define parental gates and permitted data flows under sections 1.3 and 5.1.4.
- Review Ask text, recordings, and provider data access under the selected child privacy contract.
- Define third-party AI disclosure and consent under section 5.1.2 before hosted Ask acceptance.
- Review Google-only parent sign-in against section 4.8, including the exact browser and device-enrollment flow.
- Record any applicable login exception or necessary identity change in P002 before implementation.
- Keep Google Calendar consent separate from parent identity under P001.
- Define account deletion and family-data deletion under the selected account contract and section 5.1.1.
- Document camera, microphone, notification, and any selected photo-library access through their actual user flows.
- Prepare privacy disclosures, support information, reviewer access, screenshots, and third-party notices for the selected distribution route.

Moving parent sign-in to a browser does not establish an exception to Apple's login requirements.
An identity-policy change must use a coordinated P002 decision and one shared authentication contract.
Guided Access and parental gates serve different purposes. Acceptance evidence for each is necessary.

Source checks, physical acceptance, signed artifact creation, TestFlight upload, store approval, and public availability are separate results.
F009 can close when the selected parity release candidate meets its acceptance criteria and the release package is reviewable.
Each deferred or changed feature must have a recorded disposition in that release scope before closure.
Store submission and production publication each have their own execution request and result.

## Open Decisions

The product owner selects the product and distribution values.
The implementation owner proposes device limits and framework choices with measurements.
The backend owner selects shared identity and calendar contracts through P002 and P001.

| Decision | Proposed starting point | Decision deadline |
| --- | --- | --- |
| Feature baseline | Shared Android activities on iPhone and iPad, with an explicit disposition for each platform difference | Stage 0 exit |
| Framework | Swift, SwiftUI, and narrow UIKit adapters | Stage 0 exit |
| Device and OS baseline | Explicit minimum and current iOS/iPadOS versions, with physical phone and tablet coverage | Stage 0 exit |
| Layout contract | Adaptive phone and tablet layouts, with orientation behavior selected for each activity | Stage 0 exit |
| Performance limits | Measured audio latency, camera memory, storage, and session duration on selected hardware | Stage 0 exit |
| Family setup | Shared P002 enrollment with the device credential in Keychain | Connected implementation |
| Local-data policy | Local work with explicit parent deletion, backup treatment, and device-transfer behavior | Storage implementation |
| Calendar scope | Next event first, with P001 agenda and write operations separately selected | Connected implementation |
| Parent login | Select the shared identity contract after Apple login-policy review | Hosted authentication design |
| Ask release policy | Explicit parent control, data disclosures, consent, and provider qualification | Hosted Ask implementation |
| Photo Booth filters | Carry accepted F008 behavior into the parity matrix and record any proposed release exception | Stage 0 exit |
| Game delivery | Select a port, equivalent activity, or proposed release exception for each Android game | Stage 0 exit |
| Distribution and category | Invited pilot before a separately approved public release | Stage 0 exit |

## First execution step

1. Assign the F009 implementation owner.
2. Record the stage 0 decisions with the product and backend owners.
3. Select the physical iPhones, iPads, and native toolchain.
4. Record the selected core feature baseline and measurable device limits.
5. Add a failing public integration test for Home, profile selection, drawing Save, and reopen.
6. Build the minimum iPhone and iPad flow and record its device evidence.
7. Update the estimate and milestone state from that evidence.

F009 remains open after this documentation task. Its acceptance depends on the future Apple implementation and qualification described above.

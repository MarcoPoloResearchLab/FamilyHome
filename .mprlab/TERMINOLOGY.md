# Repository Terminology

This file contains the approved technical nouns and technical verbs for repository documentation.

Use this file with `.mprlab/AGENTS.DOCS.md` and ASD-STE100 Simplified Technical English, Issue 9.

Do not add a general dictionary word to this file. Use the ASD-STE100 dictionary for general words.

Give each term one meaning. Use the same term for the same concept in all documents.

## MPR Lab Technical Nouns

- `acceptance criteria`: Conditions that show that a change has the necessary behavior.
- `active issue tracker`: The canonical file that contains current work.
- `ADR`: An architecture decision record.
- `adapter`: A code unit that connects a core module to an external system.
- `agent guide`: A file that gives binding instructions to an agent.
- `API`: A repository-owned application programming interface.
- `API contract`: The canonical schema and behavior of an API.
- `ASD-STE100`: The Simplified Technical English standard for technical documentation.
- `architecture`: The structure, boundaries, and ownership of a software system.
- `App Store Connect`: The Apple service that receives and manages iOS store artifacts.
- `artifact`: A file or image that a build, release, or generator creates.
- `backlog`: The set of unresolved issues in the active issue tracker.
- `backend client`: A code unit that sends requests to a backend.
- `browser frontend`: A user interface that operates in a web browser.
- `build`: A process or output that converts source code into an artifact.
- `changelog`: A file that records completed changes for releases.
- `CI`: The repository continuous-integration system.
- `CLI`: A command-line interface.
- `code path`: A sequence of operations in source code.
- `config`: Source-controlled configuration data.
- `container`: An isolated runtime package with an application and its dependencies.
- `contract`: A binding definition of behavior, data, or ownership.
- `credential`: A private value that an external service uses to authenticate an identity.
- `documentation`: Technical information in repository documents.
- `coverage`: Evidence that tests exercise specified behavior.
- `dependency`: An external or internal component that a system requires.
- `deployment`: An operation that changes a runtime environment.
- `domain type`: A type that represents validated domain data.
- `EAS`: Expo Application Services for hosted build, submission, and update operations.
- `endpoint`: One HTTP API address and its operation.
- `end user`: The person who requests or receives the agent work.
- `environment file`: A private file that contains environment variable assignments.
- `environment variable`: A named process input.
- `Expo`: A framework and source config system for React Native mobile clients.
- `Expo CLI`: The Expo command-line tool for local development and native project generation.
- `Google Play`: The Google service that receives and manages Android store artifacts.
- `issue`: One tracked unit of work.
- `issue tracker`: A file or system that contains issues.
- `language checker`: A tool that finds specified language errors.
- `language review`: An agent-owned examination of text against language rules and terminology.
- `manifest`: A source-controlled file that declares resources or configuration.
- `mobile client`: An application for a mobile platform.
- `mobile store artifact`: A signed `.ipa` or `.aab` file for store publication.
- `native toolchain`: The platform tools that build and sign a mobile store artifact.
- `payload`: Structured data that crosses a system boundary.
- `PDF`: A file that uses the Portable Document Format.
- `PRD`: A product requirement document.
- `private input channel`: A documented process environment, anonymous pipe, or private file input.
- `producing agent`: The agent that creates or changes technical prose.
- `pull request`: A proposed Git change for review and merge.
- `repository`: A source-controlled project and its files.
- `reference cache`: A private local directory that stores a verified official reference.
- `route`: An API or user-interface address and its handler.
- `runbook`: A technical procedure for an operator or agent.
- `runtime`: An operating instance of a service or application.
- `schema`: A machine-readable definition of structured data.
- `SHA-256`: A cryptographic digest that identifies the verified official reference.
- `source code`: Human-readable instructions that define software behavior.
- `source blocker`: A failure that prevents access to a necessary official source.
- `stack guide`: An agent guide for one language, framework, or runtime.
- `STE reference`: The verified official ASD-STE100 PDF that controls a language review.
- `store publisher`: A repository-owned tool that submits a mobile store artifact directly to its store.
- `technical document`: A repository document that contains technical information or instructions.
- `technical noun`: A subject-field noun that the repository approves.
- `technical prose`: English technical text outside code and source-controlled literals.
- `technical verb`: A subject-field verb that the repository approves.
- `validation`: Evidence that a change obeys its current contract.
- `worktree`: A Git checkout that has its own working directory.

## Repository Technical Nouns

Add repository-specific technical nouns below this line.

- `image generation`: Creation of a new image through an AI model.
- `image model`: An AI model that creates or changes images.
- `image job`: A stored request and result for one image generation operation.
- `prompt`: The text input that tells an AI model what to create.
- `reference image`: An image input that guides an image model.
- `output token`: A provider accounting unit for model output.
- `content moderation`: Examination of model inputs and outputs against the permitted content policy.

- `ICS feed`: An iCalendar document retrieved from a configured URL.
- `screenshot`: An image captured from an application display.
- `screen recording`: A video captured from an application display.
- `signing key`: A private cryptographic key used to sign an APK.
- `source snapshot`: A fixed copy of the source files used for one build.

- `fret`: A guitar position that determines the vibrating string length.
- `fretboard`: The guitar surface that contains strings and frets.
- `chord`: A set of musical notes played together.
- `strumming area`: The guitar control where a finger crosses strings to play them.

- `authentication`: Verification of the identity that presents a credential.
- `candidate credential`: A random Portal credential limited to its pairing request until device activation.
- `capability link`: A URL that grants its holder access to one specified resource.
- `credential digest`: A cryptographic hash used to verify a presented random credential.
- `child selector`: The application control that selects the active child profile.
- `CSRF`: Cross-site request forgery against a browser session.
- `device credential`: A secret that authenticates one registered Portal.
- `device enrollment`: The operation that registers a Portal and assigns its family.
- `DNS rebinding`: A change in address resolution that causes a request to reach an unapproved network destination.
- `drawing export`: An image that a Portal uploads for recipient access.
- `family membership`: A stored relationship between a parent identity and a family.
- `family isolation`: Enforcement of separate resource access for different families.
- `family record`: The stored identity and settings for one household.
- `foreign key`: A database constraint that requires a matching referenced record.
- `identity reference`: The issuer, application tenant, and account ID that identify a parent.
- `idempotency key`: A caller-supplied value that identifies retries of one operation.
- `import receipt`: A persistent record of a completed or pending data import and its identity mapping.
- `membership role`: A closed set of parent permissions within a family.
- `pairing request`: A temporary request to connect one Portal to a family.
- `pilot invitation`: An invitation that permits a specified parent to enter the hosted pilot.
- `principal`: A typed, authenticated caller identity used by an API operation.
- `quota reservation`: An atomic allocation of service capacity before an operation starts.
- `request context`: Validated caller identity and resource access data for one API request.
- `share link`: A revocable capability link for one drawing export.
- `SQLite`: The embedded relational database proposed for the single-instance pilot.
- `TAuth tenant`: The application authentication policy within TAuth.
- `usage record`: A stored record of service consumption by a family and device.
- `agenda`: A list of calendar events in time order.
- `calendar`: A Google Calendar collection that contains scheduled events.
- `calendar ID`: The Google Calendar identifier for one calendar.
- `calendar event`: A scheduled activity in a calendar.
- `child profile`: The FamilyHome identity that selects one child's application data.
- `draft`: Calendar event data that the user has not saved to Google Calendar.
- `family calendar`: The parent-owned calendar that supplies events to all child profiles in one family.
- `Google account`: The Google identity that owns the FamilyHome calendars.
- `OAuth`: The protocol through which a parent authorizes FamilyHome to access Google Calendar.
- `pairing code`: A temporary code that connects a parent session to a specified Portal.
- `parent`: The adult who owns the Google account and controls calendar access for a family.
- `parent session`: An authenticated browser session with authority to control the family's calendar connection.
- `personal calendar`: The parent-owned calendar assigned to one child profile.
- `QR code`: A machine-readable image that opens the Portal connection page on another device.
- `refresh token`: A credential that permits the backend to renew Google API access.
- `widget`: The Home screen area that shows one FamilyHome function.

```text
- `term`: Definition with one meaning.
```

## MPR Lab Technical Verbs

- `archive`: Move completed history from the active issue tracker to durable storage.
- `authenticate`: Confirm the identity of a client or user.
- `authorize`: Confirm that an identity can do an operation on a resource.
- `build`: Convert source code into an executable or generated artifact.
- `cache`: Store a verified reference outside a target repository for repeated use.
- `commit`: Record a Git change in repository history.
- `configure`: Set source-controlled values that control system behavior.
- `deploy`: Change a runtime environment to use a specified artifact and configuration.
- `file`: Add an issue to the active issue tracker.
- `generate`: Create an artifact from its canonical source.
- `lint`: Use static rules to find source or document errors.
- `merge`: Add the changes from a pull request to its target branch.
- `normalize`: Change a file to obey one canonical format or contract.
- `parse`: Convert input data into a typed internal value.
- `publish`: Make an artifact available outside the source repository.
- `refactor`: Change code structure without a change to public behavior.
- `regenerate`: Create a generated artifact again from its canonical source.
- `redistribute`: Provide a third-party reference outside its approved distribution method.
- `render`: Convert source data into a visible or machine-readable output.
- `retrieve`: Get an official reference from its approved source.
- `review`: Examine an artifact against its requirements and record the result.
- `scan`: Use an automated process to find specified source patterns.
- `serialize`: Convert a typed value into a transport or storage representation.
- `validate`: Confirm that an input or artifact obeys its contract.
- `verify`: Confirm a result at its public or runtime boundary.

Use the simple present, simple past, simple future, imperative, or infinitive form of these verbs.

## Toolbar Technical Nouns

- `adaptation`: The source changes that modify an upstream game for the Portal.
- `APK`: An Android application installation artifact.
- `emulator`: A local runtime that represents an Android device.
- `signing certificate`: The public identity that Android uses to verify an application signer.
- `source archive`: A compressed file that contains application source code.
- `toolbar`: One horizontal row of application controls.

## Repository Technical Verbs

- `combine`: Put navigation and application controls in one toolbar.
- `save`: Write application data to persistent storage.

Add repository-specific technical verbs below this line.

```text
- `term`: Definition with one meaning and the approved verb forms.
```

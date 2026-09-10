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

- `characterization test`: An integration test that records current public behavior before a refactor.
- `file permission mode`: A number or symbol that gives filesystem access bits.
- `GitHub Pages`: The GitHub service that hosts a static website from a repository branch.
- `integration test`: A test of real product logic and component interactions through a public entry point, with controlled dependencies when necessary.
- `inverted test pyramid`: The MPR Lab test strategy with integration tests as the primary layer and focused unit tests where useful.
- `production code`: Source code that implements repository behavior outside the test suite.
- `public entry point`: An interface through which a user or caller uses repository behavior.
- `static website`: A browser frontend that uses generated files without a website server runtime.
- `test-driven development`: A coding sequence that uses a failing integration test before a production code change.
- `unit test`: A test that isolates one code unit from its collaborators.
- `website hostname`: The hostname that identifies a public static website.

- `dependency injection`: A design that supplies a component's dependencies from outside that component.

## Repository Technical Nouns

Add repository-specific technical nouns below this line.

- `Ask`: The FamilyHome activity through which a child submits a question and receives an answer from a configured language model.
- `LLM Proxy`: The shared service that authenticates tenant requests and operates the selected language model through its provider.
- `request work budget`: The maximum processing time that a request permits through LLM Proxy.
- `text-to-speech`: Conversion of an answer into audible speech through the Android speech service.
- `question draft`: Question text that the child has not submitted or that remains available after a failed request.
- `YAML`: The configuration format proposed for the canonical FamilyHome backend settings.

- `image generation`: Creation of a new image through an AI model.
- `Imagine`: The FamilyHome activity for image creation from child descriptions and idea choices.
- `image gallery`: The saved generated pictures associated with one child profile.
- `SDK`: A software development kit that an application imports to call a public service.
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
- `onboarding`: The initial parent setup flow after authentication.
- `calendar setup`: The parent flow that connects or creates calendars and records their family or child assignments.
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

## Display Technical Nouns

- `dialog`: An application window that requests input or shows information above the current screen.

- `screensaver`: The display that replaces the current FamilyHome screen after a specified time without user input.
- `timeout`: The selected time without user input before the screensaver starts.

## Photo Booth Technical Nouns

- `Photo Booth`: The FamilyHome activity for camera pictures.
- `photo filter`: A local image effect applied to camera preview and saved pictures.
- `face accessory`: A photo filter that places an illustrated object at a detected face.
- `YuNet`: The bundled neural network model that detects faces and eye landmarks.
- `OpenCV`: The native library that runs the YuNet model in Photo Booth.
- `face landmark`: A detected eye, nose, or mouth position within a face.
- `face detection`: Calculation of face positions within an image without identification of a person.
- `image crop`: The rectangular source area used for a camera preview or saved picture.
- `person tracking`: Repeated face and motion measurements used to keep a selected person within an image crop.
- `optical flow`: Measurement of image-point movement between camera frames.
- `shutter event`: The command that requests a camera picture.
- `digital zoom`: Enlargement of a selected image area through a smaller source region.
- `Spotlight`: The Meta Portal camera function that follows a selected person.
- `camera preview`: The live camera image shown before a picture.
- `countdown`: The visible count before the camera takes a picture.
- `photo strip`: One image that combines a sequence of camera pictures.
- `photo album`: The saved camera pictures associated with one child profile.
- `decorative frame`: An illustrated border around a camera picture or photo strip.
- `Camera2`: The Android platform API for camera access and image output.
- `JPEG`: The image format for saved camera pictures.
- `YUV`: The camera image format with separate brightness and color components.

## Apple Platform Technical Nouns

- `iOS client`: The proposed FamilyHome application for iPhone and iPad with the shared family activities.
- `feature parity`: Equivalent family activities across platforms, with recorded platform differences and accepted release exceptions.
- `parity matrix`: The table that compares selected activity behavior and its acceptance across platforms.
- `iPadOS`: The Apple operating system for iPad.
- `SwiftUI`: The Apple framework proposed for FamilyHome screens and navigation.
- `UIKit`: The Apple framework proposed for custom touch controls and native interface adapters.
- `AVFoundation`: The Apple framework for camera capture and audiovisual media.
- `AVAudioEngine`: The Apple audio engine proposed for the local instruments.
- `Keychain`: The Apple service for secure credential storage.
- `local notification`: A system notification that an application schedules on the device.
- `Guided Access`: The Apple feature that temporarily restricts a device to one application.
- `parental gate`: An interface control that permits access to a protected operation after an adult action.
- `release candidate`: A fixed application build prepared for final acceptance before publication.
- `TestFlight`: The Apple service for distribution of test applications.
- `touch latency`: The time between a touch event and the corresponding visible or audible result.
- `safe area`: The part of an application window without system interface obstructions.

## Voice Activation Technical Nouns

- `wake phrase`: The spoken phrase that starts one Ask interaction after local detection.
- `keyword detection`: Local audio analysis that identifies the configured wake phrase.
- `voice activity detection`: Local audio analysis that identifies speech and silence within an activated question.
- `PCM`: Uncompressed digital audio samples used by local speech processing.
- `microphone owner`: The single application component that controls microphone capture and supplies audio to its consumers.
- `false activation`: A wake event produced without an intentional wake phrase from a user.
- `foreground service`: An Android service with a persistent notification that supports permitted work outside a visible activity.

## Repository Technical Verbs

- `combine`: Put navigation and application controls in one toolbar.
- `save`: Write application data to persistent storage.

Add repository-specific technical verbs below this line.

```text
- `term`: Definition with one meaning and the approved verb forms.
```

## Appearance Technical Nouns

- `character illustration`: An application image that gives a face and limbs to an activity symbol.
- `control surface`: The visible background and outline of an application control.
- `dp`: The Android unit that keeps interface dimensions consistent across display densities.
- `font scale`: The Android setting that changes text size.
- `typeface`: The design of the letters and symbols in a font.

- `sp`: The Android text unit that supports the selected font scale.
- `text role`: A named combination of typeface, weight, and text dimensions.

- `game engine`: The native program that operates game rules, graphics, and input.
- `resource pack`: An archive that supplies game interface assets.
- `skin`: A named set of game interface colors and images.

# AGENTS.md

## Forward-Only Contract Discipline

This repository uses forward-only confident programming. This agent contract is mandatory.

Use only the current canonical contract. Do not add fallbacks, backward compatibility, legacy support, or compatibility shims.

Delete obsolete code paths, schemas, config, and persisted data shapes. Do not keep dual reads, dual writes, aliases, or recovery paths.

A one-time data migration can move persisted data into the current schema. Remove the migration bridge after the operation.

<!-- BEGIN MPRLAB-GOVERNANCE -->
## MPR Lab Governance

Most workflow context files live under `.mprlab/`. The root `AGENTS.md` remains the repository entrypoint for agents.

Read these files before editing:

- `.mprlab/POLICY.md`: binding validation and confident-programming rules.
- `.mprlab/PLANNING.md`: durable planning contract.
- `.mprlab/AGENTS.DOCS.md`: ASD-STE100 documentation rules.
- `.mprlab/TERMINOLOGY.md`: approved repository technical terms.
- `.mprlab/issues-md-format.md`: issue tracker format and recurring identifier rules.
- `.mprlab/ISSUES.md`: active issue tracker.
- `.mprlab/AGENTS.GIT.md`: Git and pull request workflow.
- `.mprlab/AGENTS.API.md`: RESTful HTTP and gRPC API guidance.
- `.mprlab/AGENTS.GO.md`: Go guidance.
- `.mprlab/AGENTS.PY.md`: Python guidance.
- `.mprlab/AGENTS.MOBILE.md`: mobile client guidance.
- `.mprlab/AGENTS.DOCKER.md`: Docker and container guidance.

File permission modes are outside agent scope.
Never examine, validate, compare, require, change, or record a file permission mode.
Never use a file permission mode in acceptance, security, credential, execution, publication, deployment, or failure analysis.
The values `0600` and `7777` have no governance meaning.
This rule does not change service authorization or operation authority.

Always reference each issue by its ID, for example `B001` or `I027`.
Never use an `ISSUES.md` file path, line number, or `path:line` syntax as an issue reference.

Do not create `.mprlab/AGENTS.md`. Scoped guidance belongs in `.mprlab/AGENTS.*.md` files.
If guidance conflicts, follow `.mprlab/POLICY.md` first, then root `AGENTS.md`, then the relevant scoped guide.
<!-- END MPRLAB-GOVERNANCE -->

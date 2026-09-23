# CodeQL analysis

The repository uses the advanced workflow in `.github/workflows/codeql.yml`.
It runs on pushes and pull requests targeting `main` or `dev`, weekly on the
default branch, and on manual dispatch.

## Analysis layout

Each of the five Gradle projects has its own Java analysis job and result category:

- `examples/kafka-basic`
- `examples/kafka-streams`
- `examples/binance-websocket`
- `variants/mvc-jpa/template`
- `variants/webflux-r2dbc/template`

Each job scopes its source root to that project, initializes CodeQL in manual
build mode, then uses Java 21 and the root Gradle wrapper to compile it with `-p`.
A clean build with build and
configuration caches disabled ensures that CodeQL observes the compiler.
Dependencies may still be cached. Tests are compiled for analysis; the existing
validation workflows run them.

The raw templates deliberately keep `__PACKAGE_NAME__` and `__PACKAGE_PATH__`.
Those are valid Java identifiers and can be compiled without running the service.
Separate CodeQL databases prevent the variants' identical class names from
colliding, and keep alerts attached to the checked-in template files.

An additional job retains GitHub Actions analysis. The CodeQL action is pinned
to a commit from its v4 release line; repository Dependabot tracks action updates.

## Switching from default setup

Keep the existing default setup active while reviewing this workflow. When ready
to push the reviewed change:

1. Open the repository's **Settings > Advanced Security**, under the **Security
   and quality** sidebar section.
2. In the **CodeQL analysis** row, open the menu and choose **Switch to advanced**.
   Confirm **Disable CodeQL** in the dialog. This stops the generated default
   workflow so the checked-in advanced workflow can upload its results.
3. Use this repository's `codeql.yml`; do not commit a second, auto-generated
   workflow from GitHub's editor.
4. Commit and push the reviewed files to `dev`. The push starts the new
   **CodeQL** workflow. Promote through the normal pull request to `main`;
   the weekly schedule takes effect once the workflow is on the default branch.
5. In **Actions > CodeQL**, verify that all five Java jobs and the GitHub Actions
   job succeed. In **Security > Code scanning**, inspect CodeQL's tool status and
   confirm that the new per-project analyses have no dependency-discovery or
   duplicate-class diagnostics. Coverage is split across five databases, so each
   individual Java job is not expected to scan every Java file in the repository.

Default setup blocks uploads from an advanced CodeQL workflow. If the workflow
was pushed first and reports that its upload was rejected because default setup
is enabled, perform the switch above and rerun the workflow.

See GitHub's [advanced setup instructions](https://docs.github.com/en/code-security/how-tos/find-and-fix-code-vulnerabilities/configure-code-scanning/configuring-advanced-setup-for-code-scanning),
[manual Java build guidance](https://docs.github.com/en/code-security/reference/code-scanning/codeql/build-options-for-compiled-languages),
and [default-setup upload restriction](https://docs.github.com/en/code-security/reference/code-scanning/sarif-files/troubleshoot-sarif-uploads/default-setup-enabled).

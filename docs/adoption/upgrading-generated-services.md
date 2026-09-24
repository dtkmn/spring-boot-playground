# Upgrading a generated service

A generated service is an independent application. Its owner maintains both its
dependencies and the copied starter files. Dependabot can propose dependency,
action, and image updates; it does not synchronize application code, build logic,
workflows, scripts, or Helm templates. Review the starter's release notes and port
the changes relevant to your service.

## Establish the baseline

Keep the generated `STARTER.md` in the application's repository. It records the
upstream repository, source commit, version, working-tree state, variant, and generation
command. This is historical origin information, not the application's current
version or a claim that every later starter change has been applied.

Use the recorded full commit as the old baseline. A version is recorded only when
generation used a clean checkout of an exact release tag. `unreleased` may mean
the checkout has no matching release tag; a shallow clone without tags can also
cause this. Fetch the relevant tags before generation if you need the release
label. A `modified` working tree is not reproducible from its commit alone: retain
its changes or establish another baseline. An archive without Git metadata records
`unknown`; preserve its original release reference and generation options yourself.

Older services without `STARTER.md` need a baseline established from their own
creation records, commits, or retained scaffold. Create an origin record from that
evidence before adding upgrade notes. Do not guess a release from the current
dependency versions. Without a reliable baseline, review proposed changes
individually against the application rather than treating a generated diff as its
complete upgrade history.

## Compare two disposable scaffolds

Start with a clean application working tree and create an upgrade branch, retaining
a committed checkpoint of your application changes. Choose an available target
release or full commit after reviewing its release notes; a commit from `dev` is
unreleased work. The placeholders below are references you must replace, not
example releases.

For commits available only in a fork or local repository, use that source instead
of the upstream URL below. In Bash, prepare separate checkouts outside the application:

```bash
old_ref='<full source commit from STARTER.md>'
target_ref='<chosen release tag or full commit>'
upgrade_dir=$(mktemp -d "${TMPDIR:-/tmp}/starter-upgrade.XXXXXX")
git clone https://github.com/dtkmn/spring-boot-playground.git "$upgrade_dir/starter"
git -C "$upgrade_dir/starter" worktree add --detach "$upgrade_dir/old-source" "$old_ref"
git -C "$upgrade_dir/starter" worktree add --detach "$upgrade_dir/new-source" "$target_ref"
git -C "$upgrade_dir/new-source" rev-parse HEAD
```

Record the printed target commit for the application upgrade. Use **identical
original generation options** for both scaffolds, including the variant, names,
package, and both ports. Copy these values from the command in `STARTER.md`; the
following values are only an example:

```bash
generation_options=(
  --variant mvc-jpa
  --service-name customer-profile
  --group-id tech.company.platform
  --artifact-id customer-profile
  --package-name tech.company.platform.customerprofile
  --app-port 8080
  --management-port 8081
)
(cd "$upgrade_dir/old-source" && ./scripts/init-service.sh "${generation_options[@]}" --output-dir "$upgrade_dir/old-service")
(cd "$upgrade_dir/new-source" && ./scripts/init-service.sh "${generation_options[@]}" --output-dir "$upgrade_dir/new-service")
git diff --no-index -- "$upgrade_dir/old-service" "$upgrade_dir/new-service"
```

Read each source checkout's prerequisites before generation. The historical
`v1.0.0` generator uses Perl; newer generator changes do not alter that old tag.
The two output directories must be new disposable paths, never the application
directory. `git diff --no-index` exits with `1` when it finds differences; that is
expected. Compare individual files or subdirectories the same way to narrow the
review. `STARTER.md` differences describe the two origins and are not application
changes to apply.

## Port and verify selected changes

Review the old-to-new scaffold diff alongside your application's current files.
Port selected changes on the upgrade branch and reconcile them with your own
code and configuration. Do not replace the application directory wholesale.
For a wrapper update, keep `gradlew`, `gradlew.bat`, and `gradle/wrapper/` aligned.
Keep service-specific Helm values, credentials, and deployment settings intact.
Never replace or replay already-applied Flyway migrations; express necessary
database changes in new migrations appropriate to your application's schema.

Run `./gradlew check bootJar` from the application, with Docker running for its
integration tests. Exercise affected application behavior and add coverage where
needed. If Compose, container, or Helm assets changed, also validate that workflow
with disposable data and the application's own configuration. The starter's smoke
scripts run Compose `down -v`; use a disposable copy, not a development database.

Append an upgrade note to the application's `STARTER.md` with the target commit or
release, the areas adopted, any deferred changes, and the validation performed.
Preserve the original origin and generation command. A partial upgrade must not
claim the whole application now matches the target starter. Keep these notes with
the reviewed application commit so the next upgrade can avoid reapplying changes.

Remove the temporary comparison directory once the review is complete. Generated
comparison scaffolds and local trial logs do not belong in the application's
committed source.

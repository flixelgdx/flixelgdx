---
name: release-notes
description: Gather every pull request merged into a git tag/release and summarize them into Added, Changed, Fixed, and Removed sections, one line per change with contributor mentions. Use when asked for release notes, a changelog, or a summary of the PRs in a tag or release.
argument-hint: "[tag] [--no-create] (tag defaults to the latest tag; a missing tag is created locally unless --no-create is given)"
allowed-tools: Bash(git fetch:*) Bash(git tag:*) Bash(git log:*) Bash(git describe:*) Bash(git rev-parse:*) Bash(gh pr view:*) Bash(gh pr list:*) Bash(gh release view:*) Bash(gh release list:*) AskUserQuestion
---

# Release notes from a tag

Summarize every pull request that went into a tag into release notes.

## Steps

1. Pull the latest tags:

   ```bash
   git fetch --tags --force origin
   ```

2. Read the arguments (`$ARGUMENTS`):
   - The first argument that does not start with `--` is the target tag.
   - `--no-create` turns off tag creation.
   - With no tag given, the target is the newest existing tag: `git tag --sort=-creatordate | head -1`. Skip step 3.

3. Create the target tag if it does not exist. Check with:

   ```bash
   git rev-parse -q --verify "refs/tags/<target>"
   ```

   - If it exists, use it as-is. Never move or recreate an existing tag.
   - If it is missing and `--no-create` was NOT given, fetch `master` and create an annotated tag at its tip:

     ```bash
     git fetch origin master
     git tag -a <target> origin/master -m "<target>"
     ```

     Create it locally only. Do not push it here: pushing a tag starts a release. Step 10 asks the user about pushing.
   - If it is missing and `--no-create` WAS given, summarize from the previous tag up to `origin/master` instead, without creating anything.

4. Pick the range.
   - Previous tag: the tag before the target: `git describe --tags --abbrev=0 <target>^`.
   - If the target tag has no previous tag, use the whole history up to the target.
   - In the `--no-create` case with a missing tag, the range is `$(git describe --tags --abbrev=0 origin/master)..origin/master`. Use `origin/master` wherever later steps say `<target>`.

5. List the PRs in the range. PRs land as merge commits titled `Merge pull request #N from ...`:

   ```bash
   git log --merges --format='%s' <previous>..<target> | grep -oE '#[0-9]+' | tr -d '#' | sort -un
   ```

   Also catch squash merges, whose subjects end in `(#N)`:

   ```bash
   git log --no-merges --format='%s' <previous>..<target> | grep -oE '\(#[0-9]+\)$' | tr -dc '0-9\n' | sort -un
   ```

   Combine both lists and drop duplicates.

6. Read each PR. Run these in parallel batches:

   ```bash
   gh pr view <N> --json number,title,body,labels,author,commits -q '{n: .number, title: .title, author: .author.login, labels: [.labels[].name], body: .body, coauthors: [.commits[].authors[].login] | unique}'
   ```

   Read the title AND body so the summary describes what the PR actually did, not just its title.

7. Filter. Drop any PR that is not a user-facing framework change:
   - documentation only (Javadoc, Markdown, README, website docs)
   - chores: dependency bumps, CI/workflow changes, build tweaks, formatting, Spotless/Checkstyle fixes, repo housekeeping, version bumps, merges of `master` into a branch
   - reverted PRs, and PRs reverted later in the same range
   - anything else with no effect on someone using the framework

   When a PR mixes relevant changes with docs or chores, keep only the relevant part.

8. Sort each remaining change into exactly one section, as a code snippet the user can copy and paste:
   - **Added**: new features, APIs, platforms, or options
   - **Changed**: reworks, refactors with visible effects, behavior or API changes, performance improvements
   - **Fixed**: bug fixes
   - **Removed**: removed features, APIs, or support

   A single PR may produce lines in more than one section (for example, it adds one API and removes another).

9. Write each change as ONE line:
   - describe the change plainly and concisely, in past tense (for example, "Added gamepad rumble support")
   - end the line with the `@` of every contributor to that change (PR author plus commit co-authors, skipping bots such as `dependabot` or `github-actions`)
   - when several PRs make up one change, merge them into one line and list every contributor once

10. Ask about pushing the tag. Only do this if step 3 created a new tag in this run. After writing the notes, ask with the `AskUserQuestion` tool:
    - Question: "Push tag `<target>` to GitHub? This starts the release."
    - Options: "Push it" and "Keep it local".

    Only if the user picks "Push it", run:

    ```bash
    git push origin refs/tags/<target>
    ```

    Any other answer (including "Keep it local", "Other", or a dismissed question) means do NOT push. Leave the tag local and say nothing more.

## Output format

Your entire response must be ONLY the notes below, as a code snippet. No greeting, no intro such as "Here's your summary", no closing remarks, no PR numbers list, no explanation of what was filtered out. Omit any section that has no entries. The push question from step 10 is asked through the `AskUserQuestion` tool, not written into the response.

```markdown
## Added

- Short description of the change @contributor

## Changed

- Short description of the change @contributor @othercontributor

## Fixed

- Short description of the change @contributor

## Removed

- Short description of the change @contributor
```

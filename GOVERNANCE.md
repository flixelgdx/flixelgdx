# Governance

FlixelGDX is maintained by a small core team under the
[FlixelGDX Foundation](https://github.com/flixelgdx). This document explains how the project is run, who is responsible for what, and how 
decisions get made, so contributors always know what to expect.

---

# Roles

## Project Leader

The Project Leader has absolute final say over every aspect of the project. This includes the technical
direction, releases, and all decisions that affect the framework and its community.

**Responsibilities:**
- Full administrative access to the repository and the FlixelGDX Foundation organization
- Creating and managing releases
- Appointing and removing Maintainers
- Resolving any conflict or situation that Maintainers cannot or should not handle on their own
- Final say on any decision, technical or otherwise

## Current Project Leaders

<div align="left">
  <!-- Header and PFP on the same line -->
  <h3 style="display: flex; align-items: center; gap: 15px; margin-bottom: 10px;">
    <img src="https://github.com/stringdotjar.png" width="50" style="border-radius: 12px;" alt="String GitHub PFP">
    String (Framework Founder)
  </h3>

  <!-- Contact Info -->
  <p>
    <strong>GitHub</strong>: <a href="https://github.com/stringdotjar">@stringdotjar</a><br>
    <strong>Email</strong>: <a href="mailto:stringfromjava@gmail.com">stringfromjava@gmail.com</a><br>
    <strong>Discord</strong>: <a href="https://discord.com/users/1303562708758695937">publicstaticstring</a>
  </p>

  <!-- Discord Presence Widget -->
  <a href="https://discord.com/users/1303562708758695937">
    <img src="https://lanyard.cnrad.dev/api/1303562708758695937?showDisplayName=true&borderRadius=0" alt="Discord Presence">
  </a>
</div>

---

## Maintainer

Maintainers are the day-to-day managers of the framework. They keep the issue tracker and pull request
queue healthy, review contributions, and are the first point of contact for general questions from
contributors and users.

**Responsibilities:**
- Reviewing and managing issues and pull requests
- Direct push access to the main source branches
- Answering general questions from contributors and the community
- Escalating anything serious to the Project Leader

Maintainers do not have access to repository settings, organization administration, or release management.
If a situation requires any of those, they contact the Project Leader.

## Current Maintainers

(None yet.)

---

## Contributor

Contributors are anyone who participates in the project by opening issues or submitting pull requests.
There are no special privileges at this level - contributors do not have write access to the repository.

Consistent, high-quality contributions are how someone gets noticed for a Maintainer invitation.

---

# Becoming a Maintainer

Maintainership is invite-only. The Project Leader is the sole person who decides who becomes a Maintainer
and when. There is no formal application process. Contributing well over time is what gets someone noticed.

---

# Maintainer Removal

If a Maintainer becomes inactive, receives sustained complaints, or violates the
[Code of Conduct](CODE_OF_CONDUCT.md), the decision on what to do rests entirely with the Project Leader.
This includes temporary suspension or permanent removal of Maintainer status.

---

# How Decisions Get Made

## Small changes

A small change is anything that addresses a specific, well-scoped issue: a bug fix, a documentation
improvement, a minor addition, or a minor refactor. For these, any Contributor, Maintainer, or Project
Leader can open a pull request directly without prior discussion, ideally referencing the issue it
addresses.

## Large changes

A large change is anything that significantly alters the framework's architecture, removes or breaks
existing behavior, or affects multiple systems at once. For these, a GitHub Discussion must be opened
first. Work should not begin until the discussion has reached a clear agreement on the approach. There
is no minimum time limit - the discussion stays open until that agreement is clear.

If you are unsure whether your change counts as large, open a discussion anyway. It is always better to
align early than to submit a large pull request that needs to be substantially reworked.

---

# Versioning

FlixelGDX is still in active development and is currently versioned at `0.x.x`. While we aim to be
thoughtful about compatibility, **breaking changes can and will happen at any time during this phase**.
Do not depend on any API being stable until the project reaches `1.0.0`.

Once the project reaches `1.0.0`, we will follow [Semantic Versioning](https://semver.org):

- **Major** (`x.0.0`): Breaking changes
- **Minor** (`0.x.0`): Backward-compatible new features
- **Patch** (`0.0.x`): Backward-compatible bug fixes

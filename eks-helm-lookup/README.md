# eks-helm-lookup

A sample Jenkins pipeline that automates **semantic versioning of an EKS Terraform module** based on upstream Helm chart releases.

## Overview

Many EKS Terraform modules pin a specific Helm chart version as a default variable (e.g. `example_chart_version = "1.2.3"`).  Keeping that pin current is tedious to do manually.  This pipeline automates the process:

1. **Checks the current chart version** pinned in the Terraform module's `variables.tf`.
2. **Queries the Helm repository** for the latest published chart version.
3. **Compares the versions** and determines whether a major, minor, or patch bump is warranted.
4. **Bumps the module version** in `CHANGELOG.md` and `README.md` to match the semver significance of the chart change.
5. **Commits and pushes** the file changes to a new branch.
6. **Opens a draft PR** against the module repository with a detailed description of the change.
7. **Notifies Slack** with a summary and a direct link to the PR.

The pipeline is designed to run on a monthly cron schedule, but can also be triggered manually.

## Prerequisites

| Requirement | Notes |
|---|---|
| Jenkins shared pipeline library | Must include `src/org/example/utilities/GitHub.groovy` (see `src/` in this repo). Register the library in Jenkins under the name used in `@Library(...)`. |
| Jenkins agent | Needs `helm`, `jq`, and `git` available on `PATH`. |
| GitHub App credential (read/write) | Used by the `GitHub.groovy` utility to call the GitHub REST API (open PRs, add comments, query repo state) and by the `git push` step to write to the target module repository. A [GitHub App](https://docs.github.com/en/apps/creating-github-apps/about-creating-github-apps/about-creating-github-apps) is recommended over a PAT for private module repositories — it provides scoped permissions, automatic token rotation, and an audit trail tied to the app rather than a personal account. Store the generated installation token in Jenkins as a **Username with password** credential. |
| GitHub App credential (read-only) | A separate, read-only credential used for the initial `git checkout` of the module repository. Keeping read and write credentials separate limits blast radius if the read credential is ever exposed. |
| Slack plugin | The [Jenkins Slack plugin](https://plugins.jenkins.io/slack/) must be installed and configured. |
| HTTP Request plugin | The [HTTP Request plugin](https://plugins.jenkins.io/http_request/) is used by the GitHub utility class for API calls. |

## Setup

### 1. Configure the shared library

Register this repository as a Jenkins Global Pipeline Library:

- **Jenkins → Manage Jenkins → Configure System → Global Pipeline Libraries**
- Set the name to match the `@Library('your-pipeline-library')` annotation in the Jenkinsfile.
- Point it at the root of this repository.

### 2. Create GitHub App credentials in Jenkins

This pipeline uses the `GitHub.groovy` shared library class to interact with the GitHub REST API (open draft PRs, post PR comments, query repository metadata) and standard `git push` over HTTPS for committing changes.  Both operations authenticate using a **GitHub App installation token** stored as a Jenkins credential.

Recommended setup:

1. **Create a GitHub App** in your organization ([docs](https://docs.github.com/en/apps/creating-github-apps/about-creating-github-apps/about-creating-github-apps)) with the following repository permissions on the target Terraform module repo:
   - **Contents**: Read & write (for `git push`)
   - **Pull requests**: Read & write (for opening PRs and adding comments)
   - **Metadata**: Read-only (required by default)
2. **Install the app** on the target repository.
3. **Generate an installation access token** and store it in Jenkins as a **Username with password** credential:
   - Username: the GitHub App's slug or any non-empty string
   - Password: the installation access token
4. **Create a second read-only credential** (same process, Contents: Read-only) for the initial checkout step. This limits exposure if the read credential is ever leaked.
5. Use the Jenkins credential IDs for these two credentials as the values for `your-github-rw-credential-id` and `your-github-ro-credential-id` in the Jenkinsfile.

> **Note:** The `GitHub.groovy` utility passes credentials to the GitHub API via the HTTP Request plugin's `customHeaders` (a `Basic` auth header).  Tokens are never logged — they are only held in memory during the pipeline run and passed directly to the HTTP client.

### 3. Update the Jenkinsfile

Edit the following placeholders in the Jenkinsfile before loading it into Jenkins:

| Placeholder | Description |
|---|---|
| `your-pipeline-library` | The library name registered in Jenkins (step 1 above). |
| `your-github-rw-credential-id` | Jenkins credential ID for the **read/write** GitHub App token (step 2 above). Used by the `GitHub.groovy` utility for REST API calls and by `git push` when committing file changes. |
| `your-github-ro-credential-id` | Jenkins credential ID for the **read-only** GitHub App token (step 2 above). Used only for the initial `git checkout` of the module repository. |
| `your-agent-label` | Jenkins agent label that has `helm`, `jq`, and `git`. |
| `your-github-org` | GitHub organization that owns the target Terraform module repo. |
| `your-tfmod-example-kubernetes-integration` | The Terraform module repository name. |
| `example-charts` | The Helm repository alias to add locally. |
| `https://example.github.io/charts` | The Helm repository URL. |
| `example-kubernetes-integration` | The chart name as it appears in `helm search repo`. |
| `example_chart_version` | The Terraform variable name that holds the chart version default. |
| `#your-slack-channel` | Default Slack channel for notifications. |

### 4. Target module structure

The Terraform module repository that this pipeline manages is expected to have:

```
variables.tf      # contains a variable block with the chart version default
README.md         # contains version references that will be updated in-place
CHANGELOG.md      # follows Keep a Changelog format; new entries are prepended
```

Example `variables.tf` snippet:

```hcl
variable "example_chart_version" {
  description = "Version of the example Helm chart to deploy."
  type        = string
  default     = "1.2.3"
}
```

## How semver bumping works

The pipeline performs a component-by-component comparison of the current and latest chart versions and maps the Helm chart change onto the module's own version:

| Chart change | Module bump |
|---|---|
| Major (X in X.y.z changes) | Major |
| Minor (y in x.Y.z changes) | Minor |
| Patch (z in x.y.Z changes) | Patch |

The new module version is computed from the last entry in `CHANGELOG.md`.

## Pipeline stages

```
Pipeline Configuration  →  Initialize GitHub Utility  →  Checkout Target Module
  →  Resolve Chart Versions  →  (if update needed)
       Update Files, Commit, Push  →  Open Draft PR  →  Notify Slack
```

If no chart update is detected, the pipeline exits cleanly after the **Resolve Chart Versions** stage without making any changes.

## Related files

- [`Jenkinsfile`](Jenkinsfile) — the pipeline definition
- [`src/org/example/utilities/GitHub.groovy`](../../src/org/example/utilities/GitHub.groovy) — shared utility class for GitHub API interactions

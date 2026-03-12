# GitHub.groovy — Jenkins Shared Library Utility

A Groovy singleton utility class for interacting with the **GitHub REST API** from within a Jenkins pipeline.  It wraps common operations — opening pull requests, posting PR comments, updating commit statuses, and querying repository metadata — behind simple method calls and handles authentication via either a **Jenkins credential store** entry or a **HashiCorp Vault** secret.

## Location

```
src/org/example/utilities/GitHub.groovy
```

The companion helper used internally for building the `Authorization` header:

```
src/org/example/utilities/HttpRequestHelper.groovy
```

## Requirements

| Requirement | Notes |
|---|---|
| [HTTP Request Plugin](https://plugins.jenkins.io/http_request/) | All GitHub API calls are made through `httpRequest`. |
| [HashiCorp Vault Plugin](https://plugins.jenkins.io/hashicorp-vault-plugin/) | Required only when using `setVaultCred()`. Provides the `withVault` pipeline step that retrieves secrets from a HashiCorp Vault KV store and injects them as environment variables. |
| GitHub credential | A token with appropriate scopes for the operations you call (see [Authentication](#authentication) below). |

---

## Authentication

The utility supports two credential lookup strategies.  Set **one** per pipeline run before making any API calls.

### Option A — Jenkins credential store (recommended for most setups)

Store a GitHub token as a **Username with password** Jenkins credential and pass the credential ID:

```groovy
GitHub.instance.setApiCredId('your-jenkins-credential-id')
```

The credential's password field must contain a valid token (GitHub PAT or GitHub App installation token).  The username field can be any non-empty string.

### Option B — HashiCorp Vault

Uses the [Jenkins HashiCorp Vault Plugin](https://plugins.jenkins.io/hashicorp-vault-plugin/) (`withVault` step) to retrieve the token from a Vault KV secret at pipeline runtime. The plugin injects the secret value into a temporary environment variable, which the utility reads to build the auth header.

Pass a map matching the plugin's `withVault` step format:

```groovy
Map vaultCred = [
    path: 'kv/teams/your-team/prod/github',
    secretValues: [
        [envVar: 'GITHUB_TOKEN', vaultKey: 'token']
    ]
]

GitHub.instance.setVaultCred(vaultCred)
```

**`vaultCred` map fields:**

| Field | Type | Description |
|---|---|---|
| `path` | `String` | Full Vault KV path to the secret (e.g. `kv/teams/my-team/env/github`). |
| `secretValues` | `List<Map>` | Exactly **one** entry. The map must contain `envVar` (the temporary env variable name Jenkins will inject) and `vaultKey` (the key within the Vault secret that holds the token value). |

> **Note:** Only one entry in `secretValues` is supported.  The utility reads the credential from the first entry's `envVar`.

When `vaultCred` is set it takes precedence over `apiCredId`.

---

## Setup

### 1. Register the library in Jenkins

**Jenkins → Manage Jenkins → Configure System → Global Pipeline Libraries**

- **Name:** `your-pipeline-library` (use this name in `@Library`)
- **Default version:** `main` (or your default branch)
- **Source:** point at the root of this repository

### 2. Load in your Jenkinsfile

```groovy
@Library('your-pipeline-library') _

import org.example.utilities.GitHub
```

---

## Usage

### Initialise the singleton

Call the setters once, early in your pipeline (e.g. in a dedicated "Initialize" stage):

```groovy
// TODO: Replace with your GitHub org name
GitHub.instance.setOrg('your-github-org')

// TODO: Replace with the target repository name
GitHub.instance.setRepo('your-repo-name')

// Choose ONE authentication method:

// Option A — Jenkins credential store
GitHub.instance.setApiCredId('your-jenkins-credential-id')

// Option B — Vault
// GitHub.instance.setVaultCred([
//     path: 'kv/teams/your-team/prod/github',
//     secretValues: [ [envVar: 'GITHUB_TOKEN', vaultKey: 'token'] ]
// ])
```

---

## API Reference

### Setters

| Method | Parameter | Description |
|---|---|---|
| `setOrg(String org)` | GitHub org or user name | Sets the organization used in all API URL paths. |
| `setRepo(String repo)` | Repository name | Sets the repository used in all API URL paths. |
| `setApiCredId(String id)` | Jenkins credential ID | Token retrieved from the Jenkins credential store. |
| `setVaultCred(Map cred)` | Vault credential map (see above) | Token retrieved from HashiCorp Vault. Takes precedence over `apiCredId`. |
| `setPullRequestId(String id)` | PR number as a string | Manually sets the PR id (useful when the PR already exists). |
| `setPullRequestCommit(String commit)` | Full commit SHA | Required before calling `updatePullRequestStatus`. |
| `setPullRequestStatus(String status)` | GitHub status string | Manually overrides the cached PR status. |

---

### `openPullRequest`

Opens a pull request and stores the PR id on the singleton.

```groovy
String prId = GitHub.instance.openPullRequest(
    this,               // Jenkins steps reference
    'feature/my-branch', // source branch
    'main',             // target branch
    'chore: my PR title',
    true                // isDraft (optional, default: false)
)
```

**Returns:** PR number as a `String`.

---

### `addPullRequestComment`

Posts a comment on the stored pull request.

```groovy
GitHub.instance.addPullRequestComment(
    this,
    'My Comment Title',
    'Body text of the comment.',
    false   // useCodeFormat (optional, default: true — wraps body in a fenced code block)
)
```

---

### `addPullRequestCommentFromFile`

Reads a file and posts its contents as a code-formatted PR comment.

```groovy
GitHub.instance.addPullRequestCommentFromFile(
    this,
    'Terraform Plan Output',
    'plan.txt'
)
```

---

### `getPullRequestStatus`

Fetches and returns the current state of the stored PR (`open`, `closed`, `merged`).  Caches the result on the singleton.

```groovy
String status = GitHub.instance.getPullRequestStatus(this)
```

---

### `updatePullRequestStatus`

Posts a commit status to GitHub (the coloured check that appears on a PR).  Requires `setPullRequestCommit` to have been called first.

Valid `status` values: `pending`, `success`, `error`, `failure`
([GitHub docs](https://docs.github.com/en/rest/commits/statuses?apiVersion=2022-11-28#create-a-commit-status))

```groovy
GitHub.instance.setPullRequestCommit(env.GIT_COMMIT)

GitHub.instance.updatePullRequestStatus(
    this,
    'success',
    'Terraform plan completed successfully.'
)
```

---

### `getDefaultBranch`

Returns the default branch name of the stored repository.

```groovy
String branch = GitHub.instance.getDefaultBranch(this)
```

---

## Full pipeline example

```groovy
@Library('your-pipeline-library') _

import org.example.utilities.GitHub

pipeline {
    agent { label 'your-agent-label' }

    environment {
        TARGET_ORG  = 'your-github-org'
        TARGET_REPO = 'your-repo-name'
    }

    stages {
        stage('Initialize GitHub Utility') {
            steps {
                script {
                    GitHub.instance.setOrg(env.TARGET_ORG)
                    GitHub.instance.setRepo(env.TARGET_REPO)

                    // Jenkins credential store:
                    GitHub.instance.setApiCredId('your-jenkins-credential-id')

                    // Or Vault:
                    // GitHub.instance.setVaultCred([
                    //     path: 'kv/teams/your-team/prod/github',
                    //     secretValues: [ [envVar: 'GITHUB_TOKEN', vaultKey: 'token'] ]
                    // ])
                }
            }
        }

        stage('Open Draft PR') {
            steps {
                script {
                    String prId = GitHub.instance.openPullRequest(
                        this,
                        env.BRANCH_NAME,
                        'main',
                        "chore: automated update from build ${env.BUILD_NUMBER}",
                        true
                    )

                    GitHub.instance.addPullRequestComment(
                        this,
                        'Build Info',
                        "Created by build ${env.BUILD_URL}",
                        false
                    )

                    echo "PR opened: https://github.com/${env.TARGET_ORG}/${env.TARGET_REPO}/pull/${prId}"
                }
            }
        }
    }
}
```

---

## Rate limiting note

GitHub's REST API enforces secondary rate limits on mutative requests (POST/PATCH).  All write operations (`openPullRequest`, `addPullRequestComment`, `updatePullRequestStatus`) include a mandatory 1-second sleep before executing.  Do not remove these pauses — violating the secondary rate limit can result in the token being placed in a temporary (or longer-term) suspension.

Reference: [GitHub best practices — pause between mutative requests](https://docs.github.com/en/rest/using-the-rest-api/best-practices-for-using-the-rest-api?apiVersion=2022-11-28#pause-between-mutative-requests)

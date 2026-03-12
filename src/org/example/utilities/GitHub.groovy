// TODO: Update the package name to match your organization's namespace.
package org.example.utilities

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

/**
 * Utility class for interaction with GitHub.
 * Implemented as a Singleton
 *
 * Usage:
 *   GitHub.instance.setOrg('your-github-org')  // TODO: set to your GitHub org
 *   GitHub.instance.setRepo('your-repo-name')
 *   GitHub.instance.setApiCredId('your-credential-id')
 */
@Singleton
class GitHub {

    // TODO: Replace 'example-org' with your GitHub organization name,
    //       or call setOrg() at the start of your pipeline.
    private String org = 'example-org'
    private String repo
    private String pullRequestId
    private String pullRequestCommit
    private String pullRequestStatus
    private String defaultBranch
    private Map vaultCred
    private String apiCredId

    /**
     * Stores GitHub organization name
     *
     * @param org GitHub organization or user name
     * @return None
     */
    void setOrg(String org) {
        this.org = org
    }

    /**
     * Stores GitHub credential id
     *
     * @param id Credential id containing a GitHub PAT
     * @return None
     */
    void setApiCredId(String id) {
        this.apiCredId = id
    }

    /**
     * Stores GitHub repository name
     *
     * @param repo Github repository name
     * @return None
     */
    void setRepo(String repo) {
        this.repo = repo
    }

    /**
     * Stores GitHub Pull Request commit id
     *
     * @param commit PR commit id
     * @return None
     */
    void setPullRequestCommit(String commit) {
        this.pullRequestCommit = commit
    }

    /**
     * Stores GitHub Pull Request id
     *
     * @param id GitHub PR id
     * @return None
     */
    void setPullRequestId(String id) {
        this.pullRequestId = id
    }

    /**
     * Stores GitHub Pull Request status
     *
     * @param status GitHub PR status
     * @return None
     */
    void setPullRequestStatus(String status) {
        this.pullRequestStatus = status
    }

    /**
     * Stores a HashiCorp Vault credential map used to retrieve the GitHub token at runtime.
     *
     * Requires the Jenkins HashiCorp Vault Plugin:
     * https://plugins.jenkins.io/hashicorp-vault-plugin/
     *
     * The map must follow the format expected by the plugin's withVault step:
     *
     *   Map vaultCred = [
     *       path: 'kv/teams/your-team/env/github',
     *       secretValues: [ [envVar: 'GITHUB_TOKEN', vaultKey: 'token'] ]
     *   ]
     *
     * When set, HashiCorp Vault takes precedence over any value set via setApiCredId().
     *
     * @param cred Map containing the HashiCorp Vault KV path and secret key mapping
     * @return None
     */
    void setVaultCred(Map cred) {
        this.vaultCred = cred
    }

    /**
     * Retrieves the current status of the stored GitHub PR
     *
     * Side effect: Stores retrieved status on the singleton
     *
     * @param steps Reference to the existing pipeline execution
     * @return Pull request status
     */
    String getPullRequestStatus(Object steps) {
        if (this.pullRequestStatus == null && this.pullRequestId != null) {
            jenkins.plugins.http_request.ResponseContentSupplier response = steps.httpRequest(
                url: "https://api.github.com/repos/${this.org}/${this.repo}/pulls/${this.pullRequestId}",
                httpMode: 'GET',
                customHeaders: [makeAuthHeader(steps)],
                acceptType: 'APPLICATION_JSON'
            )
            this.pullRequestStatus = new JsonSlurper().parseText(response.getContent()).state
        } else {
            steps.echo('Skipping fetching pull request status because pullRequestId is null')
        }

        return this.pullRequestStatus
    }

    /**
     * Updates the status of the stored GitHub PR
     * Valid statuses can be found here (https://docs.github.com/en/rest/commits/statuses?apiVersion=2022-11-28#create-a-commit-status)
     *
     * @param steps Reference to the existing pipeline execution
     * @param status GitHub PR status
     * @param description Free-form description to associate with the status update
     * @return None
     */
    void updatePullRequestStatus(Object steps, String status, String description) {
        if (this.pullRequestCommit != null) {
            // See the treatise on sleep at the top of this file.
            steps.sleep(time: 1, unit: 'SECONDS')

            steps.httpRequest(
                url: "https://api.github.com/repos/${this.org}/${this.repo}/statuses/${this.pullRequestCommit}",
                httpMode: 'POST',
                customHeaders: [makeAuthHeader(steps)],
                requestBody: JsonOutput.toJson([
                    state: status,
                    target_url: steps.env.BUILD_URL,
                    context: 'tf_plan/jenkins',
                    description: description,
                ]),
            )
        } else {
            steps.echo('Skipping setting pull request status because pullRequestCommit is null')
        }
    }

    /**
     * Opens a Pull Request against the stored repository
     *
     * Side effect: Sets singleton's pull request id on success
     *
     * @param steps Reference to the existing pipeline execution
     * @param source Source branch for the PR (e.g. the feature branch that contains new work)
     * @param target Branch into which code is to be merged
     * @param title Title for the pull request
     * @param isDraft (Optional) Controls if the PR is opened as a draft (default: false)
     * @return Pull Request id of the newly-opened PR
     */
    String openPullRequest(Object steps, String source, String target, String title, Boolean isDraft = false) {
        // See the treatise on sleep at the top of this file.
        steps.sleep(time: 1, unit: 'SECONDS')

        jenkins.plugins.http_request.ResponseContentSupplier response = steps.httpRequest(
            url: "https://api.github.com/repos/${this.org}/${this.repo}/pulls",
            httpMode: 'POST',
            customHeaders: [makeAuthHeader(steps)],
            acceptType: 'APPLICATION_JSON',
            requestBody: JsonOutput.toJson([
                title: title,
                head: source,
                base: target,
                draft: isDraft,
            ]),
            validResponseCodes: '100:599', // This is purposeful
        )

        if (response.getStatus() > 399) {
            steps.echo("ERROR: Creation of PR failed with status code <${response.getStatus()}>")
            steps.echo(response.getContent())
            throw new Exception("Create PR call failed with status code <${response.getStatus()}>")
        }

        this.pullRequestId = new JsonSlurper().parseText(response.getContent()).number
        return this.pullRequestId
    }

    /**
     * Adds a pull request comment from the contents of a file
     *
     * @param steps Reference to the existing pipeline execution
     * @param title Title of the comment
     * @param file File from which to read comments
     * @return None
     */
    void addPullRequestCommentFromFile(Object steps, String title, String file) {
        // Assume we always want file content formatted
        this.addPullRequestComment(steps, title, steps.readFile(file), true)
    }

    /**
     * Adds a pull request comment based on a provided string message
     *
     * @param steps Reference to the existing pipeline execution
     * @param title Title of the comment
     * @param comment Body of the comment
     * @param useCodeFormat (Optional) Determines if the comment is code-formatted (default: true)
     * @return None
     */
    void addPullRequestComment(Object steps, String title, String comment, Boolean useCodeFormat = true) {
        if (this.pullRequestId != null) {
            String commentText = "**${title}**\n\n"
            commentText += useCodeFormat ? '```\n\n' : ''
            commentText += comment
            commentText += useCodeFormat ? '\n\n```' : ''

            if (commentText.size() > 65000) {
                commentText = "**${title}**\n\nThis comment is too long.  Please see console.log for full output.\n${steps.env.BUILD_URL}"
            }

            // See the treatise on sleep at the top of this file.
            steps.sleep(time: 1, unit: 'SECONDS')

            steps.httpRequest(
                url: "https://api.github.com/repos/${this.org}/${this.repo}/issues/${this.pullRequestId}/comments",
                httpMode: 'POST',
                customHeaders: [makeAuthHeader(steps)],
                requestBody: JsonOutput.toJson([ body: commentText ]),
            )
        } else {
            steps.echo('Skipping adding PR comment because pullRequestId is null')
        }
    }

    /**
     * Retrieves the current default branch of the stored GitHub repository
     *
     * Side effect: Stores retrieved default branch on the singleton
     *
     * @param steps Reference to the existing pipeline execution
     * @return default branch name
     */
    String getDefaultBranch(Object steps) {
        jenkins.plugins.http_request.ResponseContentSupplier response = steps.httpRequest(
            url: "https://api.github.com/repos/${this.org}/${this.repo}",
            httpMode: 'GET',
            customHeaders: [makeAuthHeader(steps)],
            acceptType: 'APPLICATION_JSON'
        )
        this.defaultBranch = new JsonSlurper().parseText(response.getContent()).default_branch

        return this.defaultBranch
    }

    /**
     * Builds the Authorization header, choosing between credential sources.
     * HashiCorp Vault (via setVaultCred) takes precedence over the Jenkins
     * credential store (via setApiCredId) if both are set.
     *
     * @param steps Reference to the existing pipeline execution
     * @return Map suitable for use in httpRequest customHeaders
     */
    // Requires: Jenkins HashiCorp Vault Plugin (https://plugins.jenkins.io/hashicorp-vault-plugin/)
    //           when using Vault-based credentials.
    private Map makeAuthHeader(Object steps) {
        return this.vaultCred != null
            ? makeAuthHeaderFromVault(steps)
            : makeAuthHeaderFromCredId(steps)
    }

    /**
     * Builds a Basic Authorization header by fetching the GitHub token from
     * HashiCorp Vault using the Jenkins HashiCorp Vault Plugin's withVault step.
     *
     * Plugin: https://plugins.jenkins.io/hashicorp-vault-plugin/
     *
     * The vaultCred map must follow the plugin's withVault step format:
     *
     *   Map vaultCred = [
     *       path: 'kv/teams/your-team/env/github',   // KV path in Vault
     *       secretValues: [
     *           [envVar: 'GITHUB_TOKEN', vaultKey: 'token']  // Vault key → env var
     *       ]
     *   ]
     *
     * The plugin injects the secret value into the env var named by 'envVar'.
     * Only the first entry in secretValues is used.
     *
     * @param steps Reference to the existing pipeline execution
     * @return Map suitable for use in httpRequest customHeaders
     */
    private Map makeAuthHeaderFromVault(Object steps) {
        steps.withVault([this.vaultCred]) {
            return buildBasicAuthHeader(steps.env[this.vaultCred.secretValues[0].envVar])
        }
    }

    /**
     * Builds a Basic Authorization header using a Jenkins credential store entry.
     * The credential must be a 'Username with password' type; the password field
     * must contain the GitHub token.
     *
     * @param steps Reference to the existing pipeline execution
     * @return Map suitable for use in httpRequest customHeaders
     */
    private Map makeAuthHeaderFromCredId(Object steps) {
        steps.withCredentials([steps.usernameColonPassword(credentialsId: this.apiCredId, variable: 'GITHUB_CRED')]) {
            return buildBasicAuthHeader(steps.env.GITHUB_CRED)
        }
    }

    /**
     * Encodes a credential string as a masked Basic Authorization header map.
     *
     * @param cred Credential string in 'username:token' or bare 'token' format
     * @return Map with name, value, and maskValue fields
     */
    private static Map buildBasicAuthHeader(String cred) {
        return [
            name      : 'Authorization',
            value     : 'Basic ' + cred.bytes.encodeBase64(),
            maskValue : true,
        ]
    }

}

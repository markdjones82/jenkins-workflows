# jenkins-workflows

Example Jenkins Workflows for all kinds of scenarios.

## Directory

### Libraries

- **[GitHub Utility](src/org/example/utilities/README.md)** — Reusable Groovy library for GitHub REST API interactions. Supports both Jenkins credentials and HashiCorp Vault authentication for flexible secret management across different deployment scenarios.

### Pipelines

- **[EKS Helm Lookup](eks-helm-lookup/README.md)** — Automated pipeline for detecting new Helm chart releases and updating Terraform modules with semantic versioning. Includes a production-ready ArgoCD example module with HA, Ingress, TLS, and notification support.

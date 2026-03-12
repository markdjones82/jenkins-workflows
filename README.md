# jenkins-workflows

Example Jenkins Workflows for all kinds of scenarios.

## Directory

### Libraries

- **[GitHub Utility](src/org/example/utilities/README.md)** — Reusable Groovy library for GitHub REST API interactions. Supports both Jenkins credentials and HashiCorp Vault authentication for flexible secret management across different deployment scenarios.

### Pipelines

- **[EKS Helm Lookup](eks-helm-lookup/README.md)** — Automated pipeline for detecting new Helm chart releases and updating Terraform modules with semantic versioning that follows the dependant Helm versioning. Includes a production-ready ArgoCD example module with HA, Ingress, TLS, and notification support.

## Contributing

We welcome contributions! Whether you're fixing bugs, adding features, or improving documentation, please feel free to fork this repository and submit a pull request.

For detailed contribution guidelines, see [CONTRIBUTING.md](CONTRIBUTING.md).

**Quick Start:**
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes with descriptive messages
4. Push to your fork
5. Open a Pull Request

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

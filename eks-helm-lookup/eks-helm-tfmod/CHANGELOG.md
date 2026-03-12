# Changelog

All notable changes to this module will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-03-12 Mark Jones

### Changed
- **BREAKING:** Complete rewrite for generic example use
- Replaced Wiz-specific module with ArgoCD (GitOps deployment tool)
- Changed from AWS Secrets Manager credential fetching to standard Kubernetes/Helm patterns
- Refactored for public consumption: removed private org references and internal dependencies
- ArgoCD is a widely-used, open-source alternative demonstrating the pipeline's flexibility

### Added
- ArgoCD Helm chart deployment with automatic version pinning
- High availability (HA) configuration support
- Ingress configuration with cert-manager TLS integration
- GitHub webhook integration options
- Notifications controller support (Slack, email, webhook)
- ApplicationSet controller option for multi-app management
- Simplified variable interface focused on deployment options vs. credentials

### Removed
- AWS-specific credential management (was overspecialized to Wiz)
- Internal registry proxy configuration
- Component-by-component enablement (Wiz-specific)

### Notes
- This module now serves as a generic example for the EKS Helm Lookup pipeline
- The previous Wiz-specific version is archived as `main.tf.wiz`, `README.md.wiz`
- Original CHANGELOG entries preserved below for reference

---

## [1.0.0] - 2026-02-20 Mark Jones (Original Wiz Module)

### Added
- Initial release of Wiz Kubernetes Integration module
- Support for deploying Wiz Connector, Sensor, and Admission Controller components
- AWS Secrets Manager integration for secure credential management
- Configurable component enablement (connector, sensor, admission controller)
- Admission controller sub-features:
  - Kubernetes audit logs webhook
  - OPA (Open Policy Agent) webhook for policy enforcement
  - Image integrity verification with trust policies
- Comprehensive README documentation including:
  - Prerequisites and IAM/RBAC permissions
  - Provider configuration examples for EKS
  - Complete deployment example with role assumption
  - AWS Secrets Manager secret format specifications
  - Version management documentation explaining chart version pinning strategy
- Module outputs for namespace, Helm release status, and registry server
- Support for custom Wiz client endpoint (for gov cloud)
- Kubernetes v3.x provider compatibility with `_v1` resource types
- Support for Nexus proxy registry configuration
- Configurable image registry paths for environments without direct Azure/public registry access
- Pinned Helm chart version (0.3.15) for production stability with override capability

### Features
- Automated namespace and secret creation
- Docker registry authentication for Wiz sensor images
- Helm-based deployment with configurable values
- Auto-creation of Kubernetes connector with cluster name
- Flexible component configuration via boolean flags
- Version-pinned Helm chart with user override option

[1.0.0]: https://github.com/rgare-io/csktlo-wiz-migration-utilities/releases/tag/v1.0.0

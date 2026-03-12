# ArgoCD on EKS — Terraform Module

A production-ready Terraform module that deploys [ArgoCD](https://argo-cd.readthedocs.io/) (GitOps continuous deployment tool) onto an Amazon EKS cluster using the official [ArgoCD Helm chart](https://github.com/argoproj/argo-helm/tree/main/charts/argo-cd).

## Overview

ArgoCD is a declarative continuous deployment tool that automates application deployment from Git repositories to Kubernetes clusters. This module handles:

- **Helm deployment** of ArgoCD with automatic version pinning
- **HA configuration** for multi-replica deployments in production
- **Ingress setup** with optional TLS termination via cert-manager
- **Admin credentials** management (with guidance for production RBAC)
- **Webhook and notification integrations** (GitHub, Slack, etc.)

**Note:** The chart version is automatically updated by the Jenkins EKS Helm Lookup pipeline when new ArgoCD releases are published.

## Usage

### Basic deployment

```hcl
module "argocd" {
  source = "./eks-helm-tfmod"

  cluster_name = aws_eks_cluster.main.name
  argocd_namespace = "argocd"
}
```

### Production HA with Ingress and TLS

```hcl
module "argocd" {
  source = "./eks-helm-tfmod"

  cluster_name           = aws_eks_cluster.main.name
  argocd_chart_version   = "7.6.8" # Pin a specific version
  environment_name       = "prod"
  
  enable_ha              = true
  enable_ingress         = true
  ingress_hostname       = "argocd.example.com"
  ingress_tls_enabled    = true
  ingress_tls_issuer     = "letsencrypt-prod"
  
  enable_github_webhook  = true
  enable_notifications   = true
  notification_slack_channel = "#deployments"
}
```

## Inputs

| Variable | Type | Default | Description |
|---|---|---|---|
| `cluster_name` | string | — | **Required.** Name of the EKS cluster. |
| `argocd_chart_version` | string | `7.6.8` | Version of the ArgoCD Helm chart. Updated automatically by the EKS Helm Lookup pipeline. |
| `argocd_namespace` | string | `argocd` | Kubernetes namespace for ArgoCD deployment. |
| `environment_name` | string | `production` | Environment identifier for tagging and identification. |
| `enable_ha` | bool | `false` | Enable high availability mode (multiple replicas, distributed deployment). |
| `enable_ingress` | bool | `true` | Enable Kubernetes Ingress for ArgoCD UI and API access. |
| `ingress_hostname` | string | `""` | Hostname for ArgoCD Ingress (e.g., `argocd.example.com`). |
| `ingress_tls_enabled` | bool | `true` | Enable TLS for ArgoCD Ingress. |
| `ingress_tls_issuer` | string | `letsencrypt-prod` | Cert-Manager issuer name for TLS certificates. |
| `admin_password_secret_name` | string | `argocd-admin-password` | Kubernetes secret name for admin password. |
| `enable_github_webhook` | bool | `false` | Enable GitHub webhook integration for automatic syncs. |
| `enable_notifications` | bool | `false` | Enable ArgoCD Notifications controller (Slack, email, webhook). |
| `notification_slack_channel` | string | `""` | Slack channel for deployment notifications. |
| `enable_applicationset` | bool | `false` | Enable ApplicationSet controller for multi-app management. |
| `additional_helm_values` | map(any) | `{}` | Additional ArgoCD Helm values for advanced customization. |

## Outputs

| Output | Description |
|---|---|
| `argocd_namespace` | The Kubernetes namespace where ArgoCD is deployed. |
| `argocd_release_name` | The Helm release name (`argocd`). |
| `argocd_chart_version` | The deployed ArgoCD Helm chart version. |
| `argocd_ui_url` | URL for accessing the ArgoCD web UI (with port-forward fallback). |
| `helm_repository_url` | The ArgoCD Helm repository URL. |

## Requirements

- Terraform >= 1.0
- AWS provider >= 5.0
- Kubernetes provider (configured to target the EKS cluster)
- Helm provider (configured to use the EKS cluster)
- cert-manager (if using TLS Ingress)
- NGINX Ingress Controller or similar (if using Ingress)

## Version Pinning and Automatic Updates

This module pins the ArgoCD Helm chart to a tested, validated version (`7.6.8` by default). This ensures:

- **Predictable deployments** — No surprise breaking changes
- **Tested configurations** — The module is validated against specific chart versions
- **Production stability** — Changes only happen when you explicitly update

The **Jenkins EKS Helm Lookup pipeline** automatically detects new ArgoCD releases, compares versions, and opens draft PRs with updated chart versions and module version bumps. This keeps your deployments current without manual effort.

To use a different chart version:

```hcl
module "argocd" {
  source = "./eks-helm-tfmod"
  argocd_chart_version = "7.7.0"  # Override default
  # ... other configuration
}
```

**Tip:** Test chart version overrides thoroughly in non-production environments first.

## Example: Full EKS + ArgoCD stack

See the Jenkins pipeline ([`example/eks-helm-lookup/Jenkinsfile`](../../example/eks-helm-lookup/Jenkinsfile)) for how this module is version-controlled and automatically updated when new ArgoCD chart releases are published.

## Troubleshooting

### Access the ArgoCD UI

**Via Ingress (if configured):**
```bash
https://argocd.example.com
```

**Via port-forward (if Ingress disabled):**
```bash
kubectl port-forward -n argocd svc/argocd-server 8080:443
# Then visit https://localhost:8080
```

### Get the initial admin password

```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 --decode
```

### Check Helm release status

```bash
helm list -n argocd
helm get values argocd -n argocd
helm get all argocd -n argocd
```

### Verify ArgoCD pods are running

```bash
kubectl get pods -n argocd
```

### Check application sync status

```bash
kubectl get applications -A
kubectl get application <app-name> -n argocd -o yaml
```

## References

- [ArgoCD Documentation](https://argo-cd.readthedocs.io/)
- [ArgoCD Helm Chart](https://github.com/argoproj/argo-helm/tree/main/charts/argo-cd)
- [ArgoCD Application CRD](https://argo-cd.readthedocs.io/en/stable/operator-manual/declarative-setup/)
- [Declarative Setup Guide](https://argo-cd.readthedocs.io/en/stable/operator-manual/declarative-setup/)
- [GitOps Best Practices](https://argo-cd.readthedocs.io/en/stable/user-guide/best-practices/)

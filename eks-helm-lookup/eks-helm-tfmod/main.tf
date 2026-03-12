locals {
  argocd_version = var.argocd_chart_version
}

# Create ArgoCD namespace
resource "kubernetes_namespace_v1" "argocd" {
  metadata {
    name = var.argocd_namespace
    labels = {
      "app.kubernetes.io/name"       = "argocd"
      "app.kubernetes.io/part-of"    = "argocd"
      "app.kubernetes.io/instance"   = var.cluster_name
      "environment"                  = var.environment_name
    }
  }
}

# Add the ArgoCD Helm repository
resource "helm_repository" "argocd" {
  name   = "argo"
  url    = "https://argoproj.github.io/argo-helm"
  update = true
}

# Deploy ArgoCD using Helm
resource "helm_release" "argocd" {
  name             = "argocd"
  repository       = helm_repository.argocd.name
  chart            = "argo-cd"
  version          = local.argocd_version
  namespace        = kubernetes_namespace_v1.argocd.metadata[0].name
  create_namespace = false # Already created above

  values = [
    yamlencode({
      global = {
        domain = var.ingress_hostname != "" ? var.ingress_hostname : "argocd.local"
      }

      # High availability configuration
      ha = {
        enabled = var.enable_ha
      }

      # Ingress configuration for UI and API access
      ingress = {
        main = {
          enabled = var.enable_ingress

          ingressClassName = "nginx" # Adjust to your ingress class
          annotations = var.ingress_tls_enabled ? {
            "cert-manager.io/cluster-issuer" = var.ingress_tls_issuer
          } : {}

          hosts = [
            {
              host = var.ingress_hostname != "" ? var.ingress_hostname : "argocd.local"
              paths = [
                {
                  path     = "/"
                  pathType = "Prefix"
                }
              ]
            }
          ]

          tls = var.ingress_tls_enabled && var.ingress_hostname != "" ? [
            {
              secretName = "argocd-tls"
              hosts = [
                var.ingress_hostname
              ]
            }
          ] : []
        }
      }

      # Server (UI and API server) configuration
      server = {
        autoscaling = {
          enabled = var.enable_ha
        }
      }

      # Application controller configuration
      controller = {
        replicas = var.enable_ha ? 2 : 1
      }

      # Repository server configuration (handles Git operations)
      repoServer = {
        autoscaling = {
          enabled = var.enable_ha
        }
      }

      # ApplicationSet controller (optional, for multi-app management)
      applicationSet = {
        enabled = var.enable_applicationset
      }

      # Notifications controller (optional, for Slack/email/webhook alerts)
      notifications = {
        enabled = var.enable_notifications
      }
    })
  ]

  dynamic "values" {
    for_each = var.additional_helm_values != {} ? [1] : []
    content {
      values = [yamlencode(var.additional_helm_values)]
    }
  }

  depends_on = [
    kubernetes_namespace_v1.argocd,
    helm_repository.argocd
  ]
}

# Optional: Create initial admin secret if provided
# In production, use ArgoCD's built-in RBAC and external auth providers
resource "kubernetes_secret_v1" "argocd_admin_password" {
  count = var.admin_password_secret_name != "" ? 1 : 0

  metadata {
    name      = var.admin_password_secret_name
    namespace = kubernetes_namespace_v1.argocd.metadata[0].name
  }

  type = "Opaque"

  data = {
    # In production, generate this via Terraform random provider or pass from external secret
    # Example: random_password.argocd_admin.result
    password = base64encode("change-me-in-production")
  }

  depends_on = [helm_release.argocd]
}

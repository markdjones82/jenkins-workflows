variable "cluster_name" {
  description = "Name of the Kubernetes cluster"
  type        = string
}

variable "argocd_chart_version" {
  description = "Version of the ArgoCD Helm chart. Pinned to a tested version for stability. Override if you need a different version. Updates are tracked automatically by the pipeline."
  type        = string
  default     = "7.6.8" # Tested and validated version
}

variable "argocd_namespace" {
  description = "Kubernetes namespace where ArgoCD will be deployed"
  type        = string
  default     = "argocd"
}

variable "enable_ha" {
  description = "Enable high availability mode for ArgoCD (multiple replicas, distributed deployment)"
  type        = bool
  default     = false
}

variable "enable_ingress" {
  description = "Enable Kubernetes Ingress for ArgoCD UI and API access"
  type        = bool
  default     = true
}

variable "ingress_hostname" {
  description = "Hostname for the ArgoCD Ingress (e.g., argocd.example.com)"
  type        = string
  default     = ""
}

variable "ingress_tls_enabled" {
  description = "Enable TLS for ArgoCD Ingress"
  type        = bool
  default     = true
}

variable "ingress_tls_issuer" {
  description = "Cert-Manager issuer name for TLS certificate (e.g., letsencrypt-prod)"
  type        = string
  default     = "letsencrypt-prod"
}

variable "admin_password_secret_name" {
  description = "Name of the Kubernetes secret containing the ArgoCD admin password (in namespace specified by argocd_namespace)"
  type        = string
  default     = "argocd-admin-password"
}

variable "enable_github_webhook" {
  description = "Enable webhook integration for GitHub repositories to trigger ArgoCD syncs"
  type        = bool
  default     = false
}

variable "enable_notifications" {
  description = "Enable ArgoCD Notifications controller for Slack/email/webhook alerts"
  type        = bool
  default     = false
}

variable "notification_slack_channel" {
  description = "Slack channel for ArgoCD deployment notifications (requires notifications enabled)"
  type        = string
  default     = ""
}

variable "enable_applicationset" {
  description = "Enable experimental ApplicationSet controller for managing multiple applications as a set"
  type        = bool
  default     = false
}

variable "environment_name" {
  description = "Environment identifier (e.g., dev, staging, prod) - used for tagging and identification"
  type        = string
  default     = "production"
}

variable "additional_helm_values" {
  description = "Additional ArgoCD Helm chart values as a map for advanced customization"
  type        = map(any)
  default     = {}
}

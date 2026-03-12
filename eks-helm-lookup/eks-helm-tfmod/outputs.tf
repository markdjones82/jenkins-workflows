output "argocd_namespace" {
  description = "The Kubernetes namespace where ArgoCD is deployed"
  value       = kubernetes_namespace_v1.argocd.metadata[0].name
}

output "argocd_release_name" {
  description = "The Helm release name for ArgoCD"
  value       = helm_release.argocd.name
}

output "argocd_chart_version" {
  description = "The deployed version of the ArgoCD Helm chart"
  value       = helm_release.argocd.version
}

output "argocd_ui_url" {
  description = "URL for accessing the ArgoCD web UI"
  value       = var.enable_ingress && var.ingress_hostname != "" ? "https://${var.ingress_hostname}" : "kubectl port-forward -n ${kubernetes_namespace_v1.argocd.metadata[0].name} svc/argocd-server 8080:443"
}

output "helm_repository_url" {
  description = "The ArgoCD Helm repository URL used for deployments"
  value       = helm_repository.argocd.url
}

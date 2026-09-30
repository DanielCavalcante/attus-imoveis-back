# Onde as imagens dos dois repositorios moram.
#
# Um registro so para o dominio inteiro, e nao um por repositorio: eles sao o
# mesmo produto, sobem juntos e sao promovidos juntos.

resource "google_artifact_registry_repository" "imagens" {
  location      = var.regiao
  repository_id = "attus-${var.ambiente}"
  format        = "DOCKER"
  description   = "Imagens da API e do site do Encontrei"

  depends_on = [google_project_service.necessarios]
}

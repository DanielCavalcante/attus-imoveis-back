terraform {
  required_version = ">= 1.9"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
  }
}

provider "google" {
  project = var.projeto
  region  = var.regiao
}

# Os servicos do GCP que este estado usa. Declarados aqui para `plan` a partir
# de um projeto novo nao falhar com "API not enabled" um recurso por vez.
resource "google_project_service" "necessarios" {
  for_each = toset([
    "run.googleapis.com",
    "sqladmin.googleapis.com",
    "secretmanager.googleapis.com",
    "artifactregistry.googleapis.com",
    "servicenetworking.googleapis.com",
    "compute.googleapis.com",
    "iam.googleapis.com",
  ])

  service = each.value

  # Desligar API porque o estado foi destruido derrubaria outros servicos do
  # mesmo projeto que nada tem a ver com este.
  disable_on_destroy = false
}

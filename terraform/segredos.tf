# Os segredos, fora do repositorio.
#
# O Terraform declara o CONTEINER do segredo, nunca o valor: `terraform.tfstate`
# guarda em claro tudo que passa por ele, e o estado vive num bucket. Quem
# grava a versao e uma pessoa, uma vez, pelo `gcloud` -- veja o README.

resource "google_secret_manager_secret" "jwt" {
  secret_id = "attus-${var.ambiente}-jwt"

  replication {
    auto {}
  }

  depends_on = [google_project_service.necessarios]
}

resource "google_secret_manager_secret" "senha_do_banco" {
  secret_id = "attus-${var.ambiente}-senha-do-banco"

  replication {
    auto {}
  }

  depends_on = [google_project_service.necessarios]
}

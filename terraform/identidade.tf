# A conta de servico da API.
#
# Propria, e nao a padrao do Compute: a padrao nasce com papel de Editor no
# projeto inteiro, o que e o oposto do C6 -- acesso ao bucket e ao banco, e a
# nada alem disso.

resource "google_service_account" "api" {
  account_id   = "attus-api-${var.ambiente}"
  display_name = "API do Encontrei (${var.ambiente})"

  depends_on = [google_project_service.necessarios]
}

# Falar com o Cloud SQL.
resource "google_project_iam_member" "api_no_banco" {
  project = var.projeto
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.api.email}"
}

# Ler os segredos que ela precisa, e so esses -- a permissao e por segredo, e
# nao por projeto.
resource "google_secret_manager_secret_iam_member" "api_nos_segredos" {
  for_each = {
    jwt   = google_secret_manager_secret.jwt.id
    banco = google_secret_manager_secret.senha_do_banco.id
  }

  secret_id = each.value
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.api.email}"
}

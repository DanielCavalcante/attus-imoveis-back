# O bucket das imagens dos anuncios.
#
# Privado (C5): a API serve as imagens, e e ela quem poe `Content-Type` e
# `nosniff` na resposta. Bucket publico entregaria os mesmos bytes sem nenhum
# dos dois -- e sem passar pelo unico ponto de entrada do sistema.

resource "google_storage_bucket" "imagens" {
  name     = "${var.projeto}-attus-imagens-${var.ambiente}"
  location = var.regiao

  uniform_bucket_level_access = true
  public_access_prevention    = "enforced"

  force_destroy = false

  depends_on = [google_project_service.necessarios]
}

# So a API le e escreve. Nenhum `allUsers` em lugar nenhum deste arquivo.
resource "google_storage_bucket_iam_member" "api" {
  bucket = google_storage_bucket.imagens.name
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${google_service_account.api.email}"
}

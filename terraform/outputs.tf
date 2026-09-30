# O que o repositorio do site le por `terraform_remote_state`.
#
# E isto que faz as duas terraformagens conversarem sem ninguem copiar valor
# de um terminal para um arquivo -- e sem descobrir meses depois que o
# endereco mudou.

output "endereco_da_api" {
  description = "Endereco interno da API. Sem entrada da internet."
  value       = google_cloud_run_v2_service.api.uri
}

output "conta_da_api" {
  description = "Conta de servico com que a API roda."
  value       = google_service_account.api.email
}

output "registro_de_imagens" {
  description = "Onde as imagens dos dois repositorios moram."
  value       = "${var.regiao}-docker.pkg.dev/${var.projeto}/${google_artifact_registry_repository.imagens.repository_id}"
}

output "rede" {
  description = "A VPC, para o site sair por ela e alcancar a API."
  value = {
    id      = google_compute_network.privada.id
    subrede = google_compute_subnetwork.privada.id
    nome    = google_compute_network.privada.name
    subnome = google_compute_subnetwork.privada.name
  }
}

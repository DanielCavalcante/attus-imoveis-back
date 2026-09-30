# A rede privada onde o banco vive.
#
# O Cloud SQL sem IP publico (spec 43, C4) so e alcancavel de dentro de uma
# VPC, e quem alcanca e a API -- por saida direta de VPC, sem conector, que e
# o caminho do Cloud Run v2.

resource "google_compute_network" "privada" {
  name                    = "attus-${var.ambiente}"
  auto_create_subnetworks = false

  depends_on = [google_project_service.necessarios]
}

resource "google_compute_subnetwork" "privada" {
  name          = "attus-${var.ambiente}"
  region        = var.regiao
  network       = google_compute_network.privada.id
  ip_cidr_range = "10.10.0.0/24"

  # Exigido pela saida direta de VPC do Cloud Run.
  private_ip_google_access = true
}

# A faixa que o Google reserva para os servicos gerenciados dele -- e onde o
# IP privado do banco vai nascer.
resource "google_compute_global_address" "faixa_do_banco" {
  name          = "attus-${var.ambiente}-sql"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 16
  network       = google_compute_network.privada.id
}

resource "google_service_networking_connection" "banco" {
  network                 = google_compute_network.privada.id
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.faixa_do_banco.name]
}

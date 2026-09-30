# O estado mora em bucket, e nao no disco de quem aplica.
#
# Dois motivos, e o segundo e o que importa para este projeto: estado local
# nao e compartilhavel, e o repositorio do site PRECISA ler as saidas daqui
# (spec 44, C2) para saber onde a API esta e qual conta pode invoca-la.
#
# O bucket e o prefixo vem de `-backend-config`, porque o backend nao aceita
# variavel: veja o README.
terraform {
  backend "gcs" {}
}

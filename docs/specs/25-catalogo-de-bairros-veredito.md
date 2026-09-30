# 25-catalogo-de-bairros — veredito

Medido contra `49de714..HEAD`, escopo `.`, com `CatalogoDeBairrosSpecTest`
(10/10) e a suíte completa do backend (238 testes, 237 passam, 1 pulado).

- **C1** — atendido. A tabela `neighborhoods` existe e tem linhas depois da migração.
- **C2** — atendido **com ressalva sobre a fonte, descrita abaixo.** A carga traz 64 bairros de João Pessoa e o teste confere nominalmente Manaíra, Bessa, Tambaú, Centro e Mussuré.
- **C3** — atendido. Inserir "manaira" — caixa e acento diferentes de "Manaíra" — é recusado com violação de integridade. A unicidade é sobre a forma normalizada, não sobre o nome cru.
- **C4** — atendido. Com um único anúncio em Manaíra, a rota de João Pessoa devolve também "Mussuré", que não tem anúncio nenhum.
- **C5** — atendido. A rota responde 200 em `/api/v1/announcements/bairros?city=`, com array de strings. Nenhum consumidor precisou mudar.
- **C6** — atendido. A lista sai ordenada e "Água Fria" está na **posição 1**, logo depois de "Aeroclube".
- **C7** — atendido. A lista não tem duplicata.
- **C8** — atendido. Recife não tem catálogo: a rota devolve os 2 bairros que têm anúncio, e o catálogo daquela cidade está vazio.
- **C9** — atendido. `neighborhood=Manaíra` devolve o anúncio de Manaíra e não o do Bessa.
- **C10** — atendido. `neighborhood=Mussuré` devolve total zero e conteúdo vazio — opção do catálogo sem imóvel é lista vazia, não erro.

## A ressalva do C2, e ela é sua para decidir

A spec exigia fonte oficial e disse, com todas as letras, que **não fecha** se a lista não vier de fonte verificável. O que aconteceu:

- **Wikipédia (pt), "Lista de bairros de João Pessoa": 64 bairros.** É a lista que foi semeada, e está citada com data dentro da própria migração.
- **Uma busca indicou 65.** Divergência de um.
- **A página de Regiões de Participação Popular da prefeitura não serve**: ela mistura bairros com comunidades e loteamentos — Mangabeira I a VII, Quadramares, Rangel — e a própria página diz que agrupa "bairros e comunidades".
- **O Atlas Municipal em PDF excede o limite de download** da ferramenta.

Decidi semear os 64 citáveis e **não inventar um 65º nome**: faltar um bairro conferível é corrigível com uma migração de uma linha; inventar um que não existe faria o filtro mentir sobre a geografia da cidade, que é o que a spec proibiu. A migração carrega a fonte, a data e esta divergência por escrito.

**Se você tiver a lista oficial, ela entra numa V8** — migração aplicada não se altera.

## O que a suíte completa expôs

- **Três testes da spec 19 reprovaram**, e os três eram a substituição que esta spec faz: `C10`, `C11` e `C12` de lá mediam a rota de bairros **de João Pessoa**, que agora tem catálogo e devolve 64 em vez de 1 ou 2.
  - `C10` e `C12` continuam verdadeiros como escritos: passaram a usar **Recife**, cidade sem catálogo, onde o `distinct` segue valendo.
  - `C11` — "a lista traz apenas bairros com ao menos um anúncio" — **deixou de ser verdade sem qualificação**. Foi marcado no arquivo da spec 19 como qualificado por esta, com data: vale onde não há catálogo.
- Nenhum outro teste se mexeu. As specs 18, 22 e 24 seguem verdes.
- `NeighborhoodRepository.nomesDe` ordena por `name_search` e não por `name`: `order by` sobre o nome cru usa a colação do banco, e sob colação C o acento joga "Água Fria" para depois de "Varjão" — ordem que nenhum leitor de português reconhece. Foi o C6 que pegou isso, na primeira execução.

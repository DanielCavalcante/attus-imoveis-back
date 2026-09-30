package com.example.demo.integracoes;

import com.example.demo.dtos.EnderecoDTO;

/**
 * Descobre o endereco a partir do CEP.
 *
 * Existe como interface para que o teste nao dependa de rede e para que trocar
 * o servico depois nao espalhe pelo dominio -- e, desde a spec 37, para que
 * haja MAIS DE UM: o ViaCEP era ponto unico de falha, e foi ele cair que fez
 * o problema aparecer.
 *
 * Contrato: nunca lanca. O que separa "este CEP nao existe" de "nao consegui
 * perguntar" e o `Status`, e essa diferenca importa -- a primeira e resposta,
 * a segunda e ausencia de resposta, e quem chama decide o que fazer com cada
 * uma.
 */
public interface ConsultaDeCep {

    enum Status {
        /** O provedor respondeu, e o CEP existe. */
        ENCONTRADO,
        /** O provedor respondeu, e o CEP nao existe. */
        NAO_ENCONTRADO,
        /** O provedor nao respondeu: fora do ar, tempo limite, desligado. */
        INDISPONIVEL
    }

    record Resultado(Status status, EnderecoDTO endereco) {

        public static Resultado encontrado(EnderecoDTO endereco) {
            return new Resultado(Status.ENCONTRADO, endereco);
        }

        public static Resultado naoEncontrado() {
            return new Resultado(Status.NAO_ENCONTRADO, null);
        }

        public static Resultado indisponivel() {
            return new Resultado(Status.INDISPONIVEL, null);
        }
    }

    /** O CEP chega so com digitos, ja conferido por quem chama. */
    Resultado consultar(String cep);
}

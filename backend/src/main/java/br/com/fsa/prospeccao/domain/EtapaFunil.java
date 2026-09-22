package br.com.fsa.prospeccao.domain;

/** Etapas do funil de prospecção, na ordem em que um lead normalmente avança. */
public enum EtapaFunil {
    NOVO,
    CONTATADO,
    QUALIFICADO,
    PROPOSTA,
    NEGOCIACAO,
    GANHO,
    PERDIDO;

    public boolean isFinalizada() {
        return this == GANHO || this == PERDIDO;
    }
}

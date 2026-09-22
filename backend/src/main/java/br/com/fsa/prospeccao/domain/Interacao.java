package br.com.fsa.prospeccao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/** Registro de um contato feito com a empresa (ligação, e-mail, reunião...), com follow-up opcional. */
@Entity
@Table(name = "interacoes")
public class Interacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contato_id")
    private Contato contato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoInteracao tipo;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora;

    @Column(name = "proximo_passo")
    private String proximoPasso;

    @Column(name = "data_follow_up")
    private Instant dataFollowUp;

    @Column(name = "follow_up_concluido", nullable = false)
    private boolean followUpConcluido;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    void prePersist() {
        criadoEm = Instant.now();
        if (dataHora == null) {
            dataHora = criadoEm;
        }
    }

    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public Contato getContato() { return contato; }
    public void setContato(Contato contato) { this.contato = contato; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public TipoInteracao getTipo() { return tipo; }
    public void setTipo(TipoInteracao tipo) { this.tipo = tipo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Instant getDataHora() { return dataHora; }
    public void setDataHora(Instant dataHora) { this.dataHora = dataHora; }
    public String getProximoPasso() { return proximoPasso; }
    public void setProximoPasso(String proximoPasso) { this.proximoPasso = proximoPasso; }
    public Instant getDataFollowUp() { return dataFollowUp; }
    public void setDataFollowUp(Instant dataFollowUp) { this.dataFollowUp = dataFollowUp; }
    public boolean isFollowUpConcluido() { return followUpConcluido; }
    public void setFollowUpConcluido(boolean followUpConcluido) { this.followUpConcluido = followUpConcluido; }
    public Instant getCriadoEm() { return criadoEm; }
}

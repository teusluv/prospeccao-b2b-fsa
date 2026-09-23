import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/cliente'
import type { Contato, ContatoForm, Empresa, EtapaFunil, Interacao, InteracaoForm } from '../api/tipos'
import { useAuth } from '../auth'
import EmpresaFormModal from '../components/EmpresaForm'
import { useToast } from '../components/Toast'
import { Carregando, corEtapa, Erro, EtapaBadge, Modal, Vazio } from '../components/ui'
import {
  deInputDataHora, ETAPAS, formatarCnpj, formatarData, formatarDataHora, linkGoogle, linkGoogleMaps, linkWhatsapp,
  NOME_ETAPA, NOME_PORTE,
  NOME_TIPO, nomeEmpresa, reais, TIPOS_INTERACAO,
} from '../formato'

const ICONE_TIPO: Record<string, string> = { LIGACAO: '📞', EMAIL: '✉️', WHATSAPP: '💬', REUNIAO: '🤝', VISITA: '📍', OUTRO: '📝' }

export default function EmpresaDetalhe() {
  const id = Number(useParams().id)
  const navegar = useNavigate()
  const toast = useToast()
  const { usuario } = useAuth()
  const [empresa, setEmpresa] = useState<Empresa | null>(null)
  const [contatos, setContatos] = useState<Contato[]>([])
  const [interacoes, setInteracoes] = useState<Interacao[]>([])
  const [erro, setErro] = useState<unknown>(null)
  const [editando, setEditando] = useState(false)
  const [contatoEditado, setContatoEditado] = useState<Contato | 'novo' | null>(null)
  const [perdendo, setPerdendo] = useState(false)

  const carregar = useCallback(() => {
    Promise.all([api.empresa(id), api.contatos(id), api.interacoes(id)])
      .then(([e, c, i]) => { setEmpresa(e); setContatos(c); setInteracoes(i.conteudo) })
      .catch(setErro)
  }, [id])
  useEffect(carregar, [carregar])

  async function mudarEtapa(etapa: EtapaFunil, motivo?: string) {
    if (etapa === 'PERDIDO' && !motivo) return setPerdendo(true)
    try {
      setEmpresa(await api.mudarEtapa(id, etapa, motivo))
      setPerdendo(false)
      toast(`Movida para ${NOME_ETAPA[etapa]}`)
    } catch (err) {
      toast(err instanceof Error ? err.message : 'Erro')
    }
  }

  async function excluir() {
    if (!empresa || !confirm(`Excluir ${nomeEmpresa(empresa)} e todo o histórico?`)) return
    try {
      await api.excluirEmpresa(id)
      toast('Empresa excluída')
      navegar('/empresas')
    } catch (err) {
      toast(err instanceof Error ? err.message : 'Erro')
    }
  }

  if (erro) return <Erro erro={erro} />
  if (!empresa) return <Carregando />
  const whats = linkWhatsapp(empresa.telefone)

  return (
    <>
      <div className="topo">
        <div>
          <p className="pequeno"><Link to="/empresas">← Empresas</Link></p>
          <h1 style={{ marginTop: 6 }}>{nomeEmpresa(empresa)}</h1>
          <p>{empresa.segmento ?? 'Sem segmento'} · {empresa.cidade ?? 'Cidade não informada'}{empresa.uf ? `/${empresa.uf}` : ''}</p>
        </div>
        <div className="acoes">
          <a className="btn" href={linkGoogle(nomeEmpresa(empresa), empresa.cidade, empresa.uf)} target="_blank" rel="noreferrer"
            title="Pesquisar no Google para achar telefone, Instagram, endereço">Pesquisar no Google</a>
          <a className="btn" href={linkGoogleMaps(nomeEmpresa(empresa), empresa.cidade, empresa.uf)} target="_blank" rel="noreferrer">Google Maps</a>
          {whats && <a className="btn" href={whats} target="_blank" rel="noreferrer">WhatsApp</a>}
          {empresa.telefone && <a className="btn" href={`tel:${empresa.telefone}`}>Ligar</a>}
          <button className="btn" onClick={() => setEditando(true)}>Editar</button>
          {usuario?.perfil === 'ADMIN' && <button className="btn btn-perigo" onClick={excluir}>Excluir</button>}
        </div>
      </div>

      <div className="card card-pad" style={{ marginBottom: 18 }}>
        <div className="card-titulo"><h2>Etapa no funil</h2><EtapaBadge etapa={empresa.etapa} /></div>
        <div className="etapas-seletor">
          {ETAPAS.map((et) => (
            <button key={et} className={et === empresa.etapa ? 'atual' : ''}
              style={et === empresa.etapa ? { background: corEtapa(et) } : undefined}
              onClick={() => et !== empresa.etapa && mudarEtapa(et)}>
              {NOME_ETAPA[et]}
            </button>
          ))}
        </div>
        {empresa.etapa === 'PERDIDO' && empresa.motivoPerda && <p className="muted pequeno" style={{ marginTop: 10 }}>Motivo da perda: {empresa.motivoPerda}</p>}
      </div>

      <div className="detalhe">
        <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
          <NovaInteracao empresaId={id} contatos={contatos} aoSalvar={() => { carregar(); toast('Interação registrada') }} />
          <div className="card card-pad">
            <div className="card-titulo"><h2>Histórico</h2><span className="muted pequeno">{interacoes.length} registro(s)</span></div>
            {interacoes.length === 0 ? <Vazio titulo="Nenhum contato registrado ainda" /> : (
              <div className="timeline">
                {interacoes.map((i) => (
                  <div className="evento" key={i.id}>
                    <div className="evento-icone">{ICONE_TIPO[i.tipo]}</div>
                    <div>
                      <div className="evento-topo">
                        <strong>{NOME_TIPO[i.tipo]}{i.contatoNome ? ` com ${i.contatoNome}` : ''}</strong>
                        <span className="muted pequeno">{formatarDataHora(i.dataHora)} · {i.usuario.nome}</span>
                      </div>
                      <p>{i.descricao}</p>
                      {(i.proximoPasso || i.dataFollowUp) && (
                        <p className="pequeno" style={{ marginTop: 6 }}>
                          <span className={`badge ${i.followUpConcluido ? 'badge-ok' : new Date(i.dataFollowUp ?? 0) < new Date() ? 'badge-perigo' : 'badge-accent'}`}>
                            {i.followUpConcluido ? 'Feito' : i.dataFollowUp ? `Retorno ${formatarDataHora(i.dataFollowUp)}` : 'Próximo passo'}
                          </span>{' '}
                          {i.proximoPasso}
                          {i.dataFollowUp && !i.followUpConcluido && (
                            <button className="btn btn-sm btn-fantasma" onClick={async () => { await api.concluirFollowUp(i.id); carregar() }}>Marcar como feito</button>
                          )}
                        </p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
          <div className="card card-pad">
            <div className="card-titulo"><h2>Dados</h2></div>
            <dl className="dados" style={{ margin: 0 }}>
              <div><dt>Razão social</dt><dd>{empresa.razaoSocial}</dd></div>
              <div><dt>CNPJ</dt><dd>{formatarCnpj(empresa.cnpj)}</dd></div>
              <div><dt>Telefone</dt><dd>{empresa.telefone ?? (
                <a href={linkGoogle(nomeEmpresa(empresa), empresa.cidade, empresa.uf)} target="_blank" rel="noreferrer">Procurar no Google</a>
              )}</dd></div>
              <div><dt>E-mail</dt><dd>{empresa.email ? <a href={`mailto:${empresa.email}`}>{empresa.email}</a> : '—'}</dd></div>
              <div><dt>Site</dt><dd>{empresa.site ? <a href={empresa.site.startsWith('http') ? empresa.site : `https://${empresa.site}`} target="_blank" rel="noreferrer">{empresa.site}</a> : <span className="badge badge-warn">Sem site</span>}</dd></div>
              <div><dt>Porte</dt><dd>{empresa.porte ? NOME_PORTE[empresa.porte] : '—'}</dd></div>
              <div><dt>Valor estimado</dt><dd>{reais(empresa.valorEstimado)}</dd></div>
              <div><dt>Origem</dt><dd>{empresa.origem ?? '—'}</dd></div>
              <div><dt>Responsável</dt><dd>{empresa.responsavel?.nome ?? '—'}</dd></div>
              <div><dt>Cadastrada em</dt><dd>{formatarData(empresa.criadoEm)}</dd></div>
            </dl>
            {empresa.observacoes && <p className="pequeno" style={{ marginTop: 14, whiteSpace: 'pre-wrap' }}>{linkificar(empresa.observacoes)}</p>}
          </div>

          <div className="card card-pad">
            <div className="card-titulo"><h2>Contatos</h2><button className="btn btn-sm" onClick={() => setContatoEditado('novo')}>+ Adicionar</button></div>
            {contatos.length === 0 ? <Vazio titulo="Nenhum contato">Adicione quem decide na empresa.</Vazio> : contatos.map((c) => (
              <div className="contato" key={c.id}>
                <div className="contato-topo">
                  <strong>{c.nome} {c.principal && <span className="badge badge-accent">Principal</span>}</strong>
                  <button className="btn btn-sm btn-fantasma" onClick={() => setContatoEditado(c)}>Editar</button>
                </div>
                <p className="muted pequeno">{[c.cargo, c.telefone, c.email].filter(Boolean).join(' · ') || 'Sem detalhes'}</p>
                {c.linkedin && <a className="pequeno" href={c.linkedin} target="_blank" rel="noreferrer">LinkedIn</a>}
              </div>
            ))}
          </div>
        </div>
      </div>

      {editando && <EmpresaFormModal empresa={empresa} aoFechar={() => setEditando(false)} aoSalvar={(e) => { setEmpresa(e); setEditando(false); toast('Dados salvos') }} />}
      {contatoEditado && <ContatoModal empresaId={id} contato={contatoEditado === 'novo' ? undefined : contatoEditado}
        aoFechar={() => setContatoEditado(null)} aoSalvar={() => { setContatoEditado(null); carregar() }} />}
      {perdendo && <MotivoPerdaModal aoFechar={() => setPerdendo(false)} aoConfirmar={(m) => mudarEtapa('PERDIDO', m)} />}
    </>
  )
}

function linkificar(texto: string) {
  return texto.split(/(https?:\/\/\S+)/g).map((parte, i) =>
    parte.startsWith('http') ? <a key={i} href={parte} target="_blank" rel="noreferrer">{parte}</a> : parte)
}

function NovaInteracao({ empresaId, contatos, aoSalvar }: { empresaId: number; contatos: Contato[]; aoSalvar: () => void }) {
  const inicial: InteracaoForm = { tipo: 'WHATSAPP', descricao: '', contatoId: null, proximoPasso: '', dataFollowUp: null }
  const [form, setForm] = useState(inicial)
  const [followUp, setFollowUp] = useState('')
  const [erro, setErro] = useState<unknown>(null)
  const [salvando, setSalvando] = useState(false)

  async function salvar(e: FormEvent) {
    e.preventDefault()
    setSalvando(true)
    setErro(null)
    try {
      await api.registrarInteracao(empresaId, { ...form, dataFollowUp: deInputDataHora(followUp), proximoPasso: form.proximoPasso || null })
      setForm(inicial)
      setFollowUp('')
      aoSalvar()
    } catch (err) {
      setErro(err)
    } finally {
      setSalvando(false)
    }
  }

  return (
    <form className="card card-pad" onSubmit={salvar}>
      <div className="card-titulo"><h2>Registrar contato</h2></div>
      <div className="grade g2">
        <div>
          <label>Tipo</label>
          <select value={form.tipo} onChange={(e) => setForm({ ...form, tipo: e.target.value as InteracaoForm['tipo'] })}>
            {TIPOS_INTERACAO.map((t) => <option key={t} value={t}>{NOME_TIPO[t]}</option>)}
          </select>
        </div>
        <div>
          <label>Com quem</label>
          <select value={form.contatoId ?? ''} onChange={(e) => setForm({ ...form, contatoId: e.target.value ? Number(e.target.value) : null })}>
            <option value="">—</option>
            {contatos.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
          </select>
        </div>
        <div className="col2">
          <label>O que aconteceu *</label>
          <textarea required value={form.descricao} onChange={(e) => setForm({ ...form, descricao: e.target.value })}
            placeholder="Ex.: Falei com o dono, gostou da ideia de ter um site. Pediu orçamento." maxLength={4000} />
        </div>
        <div>
          <label>Próximo passo</label>
          <input value={form.proximoPasso ?? ''} onChange={(e) => setForm({ ...form, proximoPasso: e.target.value })} placeholder="Enviar proposta" maxLength={500} />
        </div>
        <div>
          <label>Lembrar em</label>
          <input type="datetime-local" value={followUp} onChange={(e) => setFollowUp(e.target.value)} />
        </div>
      </div>
      <Erro erro={erro} />
      <div className="acoes" style={{ marginTop: 14, justifyContent: 'flex-end' }}>
        <button className="btn btn-primario" disabled={salvando}>{salvando ? 'Salvando…' : 'Registrar'}</button>
      </div>
    </form>
  )
}

function ContatoModal({ empresaId, contato, aoFechar, aoSalvar }: {
  empresaId: number; contato?: Contato; aoFechar: () => void; aoSalvar: () => void
}) {
  const [form, setForm] = useState<ContatoForm>(contato ?? { nome: '', cargo: '', email: '', telefone: '', linkedin: '', principal: false })
  const [erro, setErro] = useState<unknown>(null)
  const campo = (k: keyof Omit<ContatoForm, 'principal'>) => ({
    value: form[k] ?? '', onChange: (e: { target: { value: string } }) => setForm({ ...form, [k]: e.target.value }),
  })

  async function salvar(e: FormEvent) {
    e.preventDefault()
    const dados = { ...form, email: form.email || null, cargo: form.cargo || null, telefone: form.telefone || null, linkedin: form.linkedin || null }
    try {
      if (contato) await api.atualizarContato(contato.id, dados)
      else await api.criarContato(empresaId, dados)
      aoSalvar()
    } catch (err) {
      setErro(err)
    }
  }

  async function excluir() {
    if (!contato || !confirm(`Excluir o contato ${contato.nome}?`)) return
    try {
      await api.excluirContato(contato.id)
      aoSalvar()
    } catch (err) {
      setErro(err)
    }
  }

  return (
    <Modal titulo={contato ? 'Editar contato' : 'Novo contato'} aoFechar={aoFechar} rodape={<>
      {contato && <button className="btn btn-perigo" onClick={excluir} style={{ marginRight: 'auto' }}>Excluir</button>}
      <button className="btn" onClick={aoFechar}>Cancelar</button>
      <button className="btn btn-primario" form="form-contato">Salvar</button>
    </>}>
      <form id="form-contato" className="grade g2" onSubmit={salvar}>
        <div className="col2"><label>Nome *</label><input required maxLength={120} {...campo('nome')} /></div>
        <div><label>Cargo</label><input maxLength={100} {...campo('cargo')} placeholder="Dono, gerente…" /></div>
        <div><label>Telefone</label><input maxLength={30} {...campo('telefone')} /></div>
        <div><label>E-mail</label><input type="email" maxLength={160} {...campo('email')} /></div>
        <div><label>LinkedIn</label><input maxLength={200} {...campo('linkedin')} /></div>
        <label className="check col2"><input type="checkbox" checked={form.principal} onChange={(e) => setForm({ ...form, principal: e.target.checked })} /> Contato principal</label>
        <div className="col2"><Erro erro={erro} /></div>
      </form>
    </Modal>
  )
}

export function MotivoPerdaModal({ aoFechar, aoConfirmar }: { aoFechar: () => void; aoConfirmar: (motivo: string) => void }) {
  const [motivo, setMotivo] = useState('')
  return (
    <Modal titulo="Por que perdemos?" aoFechar={aoFechar} rodape={<>
      <button className="btn" onClick={aoFechar}>Cancelar</button>
      <button className="btn btn-primario" disabled={!motivo.trim()} onClick={() => aoConfirmar(motivo.trim())}>Marcar como perdido</button>
    </>}>
      <label htmlFor="motivo">Motivo</label>
      <textarea id="motivo" autoFocus value={motivo} onChange={(e) => setMotivo(e.target.value)} maxLength={500}
        placeholder="Ex.: Achou caro, já tem fornecedor, não respondeu mais…" />
    </Modal>
  )
}

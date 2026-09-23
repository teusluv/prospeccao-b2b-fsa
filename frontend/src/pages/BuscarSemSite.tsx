import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/cliente'
import type { BuscaGoogleMapsResponse } from '../api/tipos'
import { Carregando, Erro, Vazio } from '../components/ui'
import { linkWhatsapp, NOME_SITUACAO, UFS } from '../formato'

const SUGESTOES = ['Restaurantes', 'Padarias', 'Salões de beleza', 'Barbearias', 'Oficinas mecânicas', 'Clínicas odontológicas',
  'Academias', 'Pet shops', 'Lojas de roupas', 'Materiais de construção']

const CLASSE_SITUACAO = { IMPORTADA: 'badge-ok', JA_CADASTRADA: 'badge-accent', TEM_SITE: '', FECHADA: 'badge-perigo' } as const

export default function BuscarSemSite() {
  const [termo, setTermo] = useState('')
  const [cidade, setCidade] = useState('Feira de Santana')
  const [uf, setUf] = useState('BA')
  const [redeSocial, setRedeSocial] = useState(true)
  const [max, setMax] = useState(60)
  const [resultado, setResultado] = useState<BuscaGoogleMapsResponse | null>(null)
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState<unknown>(null)
  const [mostrarTodos, setMostrarTodos] = useState(false)

  async function buscar(simular: boolean, e?: FormEvent) {
    e?.preventDefault()
    setCarregando(true)
    setErro(null)
    try {
      setResultado(await api.buscarGoogleMaps({ termo, cidade, uf, maxResultados: max, redeSocialContaComoSemSite: redeSocial, simular }))
    } catch (err) {
      setErro(err)
    } finally {
      setCarregando(false)
    }
  }

  const lugares = resultado?.lugares.filter((l) => mostrarTodos || l.situacao === 'IMPORTADA' || l.situacao === 'JA_CADASTRADA') ?? []

  return (
    <>
      <div className="topo">
        <div>
          <h1>Buscar empresas sem site</h1>
          <p>Pesquisa no Google Maps e cadastra como lead só quem ainda não tem site.</p>
        </div>
      </div>

      <form className="card card-pad" onSubmit={(e) => buscar(true, e)}>
        <div className="grade g4">
          <div className="col2">
            <label htmlFor="termo">O que procurar</label>
            <input id="termo" required value={termo} onChange={(e) => setTermo(e.target.value)} placeholder="Ex.: restaurantes" list="sugestoes" maxLength={100} />
            <datalist id="sugestoes">{SUGESTOES.map((s) => <option key={s} value={s} />)}</datalist>
          </div>
          <div><label htmlFor="cidade">Cidade</label><input id="cidade" required value={cidade} onChange={(e) => setCidade(e.target.value)} maxLength={100} /></div>
          <div>
            <label htmlFor="uf">UF</label>
            <select id="uf" value={uf} onChange={(e) => setUf(e.target.value)}>{UFS.map((u) => <option key={u}>{u}</option>)}</select>
          </div>
          <div>
            <label htmlFor="max">Máx. resultados</label>
            <select id="max" value={max} onChange={(e) => setMax(Number(e.target.value))}>
              {[20, 40, 60].map((n) => <option key={n} value={n}>{n}</option>)}
            </select>
          </div>
          <div className="col2" style={{ display: 'flex', alignItems: 'flex-end' }}>
            <label className="check">
              <input type="checkbox" checked={redeSocial} onChange={(e) => setRedeSocial(e.target.checked)} />
              Quem só tem Instagram/Facebook/WhatsApp conta como sem site
            </label>
          </div>
        </div>
        <div className="acoes" style={{ marginTop: 16 }}>
          <button className="btn" disabled={carregando || !termo}>Pesquisar (só ver)</button>
          <button type="button" className="btn btn-primario" disabled={carregando || !termo} onClick={() => buscar(false)}>
            Pesquisar e cadastrar sem site
          </button>
          <span className="muted pequeno">O Google traz no máximo 60 por busca. Faça uma busca por ramo para cobrir a cidade.</span>
        </div>
      </form>

      <div style={{ marginTop: 18 }}>
        <Erro erro={erro} />
        {carregando && <Carregando />}
        {resultado && !carregando && (
          <>
            <div className="resumo-busca">
              <span className="badge">{resultado.encontradas} encontradas</span>
              <span className="badge badge-warn">{resultado.semSite} sem site</span>
              <span className="badge badge-ok">{resultado.importadas} {resultado.simulacao ? 'novas (não gravadas)' : 'cadastradas agora'}</span>
              <span className="badge badge-accent">{resultado.jaCadastradas} já estavam cadastradas</span>
              <label className="check" style={{ marginLeft: 'auto' }}>
                <input type="checkbox" checked={mostrarTodos} onChange={(e) => setMostrarTodos(e.target.checked)} /> Mostrar também quem tem site
              </label>
            </div>
            {resultado.simulacao && resultado.importadas > 0 && (
              <div className="alerta alerta-info" style={{ marginBottom: 14 }}>
                Isto foi só uma prévia. Clique em <strong>“Pesquisar e cadastrar sem site”</strong> para salvar as {resultado.importadas} empresas novas.
              </div>
            )}
            <div className="card">
              {lugares.length === 0 ? <Vazio titulo="Nenhuma empresa sem site nesta busca">Tente outro ramo ou marque “Mostrar também quem tem site”.</Vazio> : (
                <div className="tabela-wrap">
                  <table>
                    <thead><tr><th>Empresa</th><th>Telefone</th><th>Situação</th><th></th></tr></thead>
                    <tbody>
                      {lugares.map((l) => {
                        const whats = linkWhatsapp(l.telefone)
                        return (
                          <tr key={l.googlePlaceId}>
                            <td className="nome-cel"><strong>{l.nome}</strong><span>{[l.categoria, l.endereco].filter(Boolean).join(' · ')}</span></td>
                            <td className="nowrap">{l.telefone ?? '—'}</td>
                            <td>
                              <span className={`badge ${CLASSE_SITUACAO[l.situacao]}`}>
                                {l.situacao === 'IMPORTADA' && resultado.simulacao ? 'Sem site (nova)' : NOME_SITUACAO[l.situacao]}
                              </span>
                              {l.site && <div className="pequeno muted" style={{ marginTop: 4 }}>{l.site}</div>}
                            </td>
                            <td>
                              <div className="lugar-acoes">
                                {l.empresaId && <Link className="btn btn-sm" to={`/empresas/${l.empresaId}`}>Abrir</Link>}
                                {whats && <a className="btn btn-sm" href={whats} target="_blank" rel="noreferrer">WhatsApp</a>}
                                {l.googleMapsUrl && <a className="btn btn-sm btn-fantasma" href={l.googleMapsUrl} target="_blank" rel="noreferrer">Maps</a>}
                              </div>
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </>
  )
}

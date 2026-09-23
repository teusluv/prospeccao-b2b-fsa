import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/cliente'
import type { BuscaResponse, Fonte, Fontes } from '../api/tipos'
import { Carregando, Erro, Vazio } from '../components/ui'
import { linkWhatsapp, NOME_SITUACAO, UFS } from '../formato'

const OUTRO = '__OUTRO__'
const SUGESTOES_GOOGLE = ['Restaurantes', 'Padarias', 'Salões de beleza', 'Barbearias', 'Oficinas mecânicas',
  'Clínicas odontológicas', 'Academias', 'Pet shops', 'Lojas de roupas', 'Materiais de construção']
const CLASSE_SITUACAO = { IMPORTADA: 'badge-ok', JA_CADASTRADA: 'badge-accent', TEM_SITE: '', FECHADA: 'badge-perigo' } as const
const MAXIMOS: Record<Fonte, number[]> = { OPENSTREETMAP: [50, 100, 200, 500], GOOGLE_MAPS: [20, 40, 60] }
const NOME_FONTE: Record<Fonte, string> = { OPENSTREETMAP: 'OpenStreetMap', GOOGLE_MAPS: 'Google Maps' }

export default function BuscarSemSite() {
  const [fontes, setFontes] = useState<Fontes | null>(null)
  const [fonte, setFonte] = useState<Fonte>('OPENSTREETMAP')
  const [categoria, setCategoria] = useState('RESTAURANTES')
  const [termo, setTermo] = useState('')
  const [cidade, setCidade] = useState('Feira de Santana')
  const [uf, setUf] = useState('BA')
  const [redeSocial, setRedeSocial] = useState(true)
  const [max, setMax] = useState(200)
  const [resultado, setResultado] = useState<BuscaResponse | null>(null)
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState<unknown>(null)
  const [mostrarTodos, setMostrarTodos] = useState(false)

  useEffect(() => { api.fontes().then(setFontes).catch(setErro) }, [])

  function trocarFonte(f: Fonte) {
    setFonte(f)
    setMax(f === 'GOOGLE_MAPS' ? 60 : 200)
    setResultado(null)
  }

  const usaTextoLivre = fonte === 'GOOGLE_MAPS' || categoria === OUTRO
  const termoEnviado = usaTextoLivre ? termo.trim() : categoria

  async function buscar(simular: boolean, e?: FormEvent) {
    e?.preventDefault()
    if (!termoEnviado) return
    setCarregando(true)
    setErro(null)
    try {
      setResultado(await api.buscarSemSite({ fonte, termo: termoEnviado, cidade, uf, maxResultados: max, redeSocialContaComoSemSite: redeSocial, simular }))
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
          <p>Encontra empresas da cidade e cadastra como lead só quem ainda não tem site.</p>
        </div>
      </div>

      <form className="card card-pad" onSubmit={(e) => buscar(true, e)}>
        <div className="grade g4">
          <div className="col2">
            <label htmlFor="fonte">Fonte</label>
            <select id="fonte" value={fonte} onChange={(e) => trocarFonte(e.target.value as Fonte)}>
              <option value="OPENSTREETMAP">OpenStreetMap (gratuito)</option>
              <option value="GOOGLE_MAPS" disabled={fontes ? !fontes.googleMapsDisponivel : false}>
                Google Maps{fontes && !fontes.googleMapsDisponivel ? ' (precisa de chave)' : ''}
              </option>
            </select>
          </div>
          {fonte === 'OPENSTREETMAP' ? (
            <div className="col2">
              <label htmlFor="categoria">Ramo</label>
              <select id="categoria" value={categoria} onChange={(e) => setCategoria(e.target.value)}>
                {fontes?.categoriasOpenStreetMap.map((c) => <option key={c.chave} value={c.chave}>{c.nome}</option>)}
                <option value={OUTRO}>Outro (buscar pelo nome)…</option>
              </select>
            </div>
          ) : <div className="col2" />}
          {usaTextoLivre && (
            <div className="col2">
              <label htmlFor="termo">{fonte === 'GOOGLE_MAPS' ? 'O que procurar' : 'Parte do nome da empresa'}</label>
              <input id="termo" required value={termo} onChange={(e) => setTermo(e.target.value)} maxLength={100}
                placeholder={fonte === 'GOOGLE_MAPS' ? 'Ex.: restaurantes' : 'Ex.: açaí, pizzaria, auto peças'}
                list={fonte === 'GOOGLE_MAPS' ? 'sugestoes' : undefined} />
              <datalist id="sugestoes">{SUGESTOES_GOOGLE.map((s) => <option key={s} value={s} />)}</datalist>
            </div>
          )}
          <div><label htmlFor="cidade">Cidade</label><input id="cidade" required value={cidade} onChange={(e) => setCidade(e.target.value)} maxLength={100} /></div>
          <div>
            <label htmlFor="uf">UF</label>
            <select id="uf" value={uf} onChange={(e) => setUf(e.target.value)}>{UFS.map((u) => <option key={u}>{u}</option>)}</select>
          </div>
          <div>
            <label htmlFor="max">Máx. resultados</label>
            <select id="max" value={max} onChange={(e) => setMax(Number(e.target.value))}>
              {MAXIMOS[fonte].map((n) => <option key={n} value={n}>{n}</option>)}
            </select>
          </div>
          <div className={usaTextoLivre ? '' : 'col2'} style={{ display: 'flex', alignItems: 'flex-end' }}>
            <label className="check">
              <input type="checkbox" checked={redeSocial} onChange={(e) => setRedeSocial(e.target.checked)} />
              Só Instagram/Facebook/WhatsApp conta como sem site
            </label>
          </div>
        </div>
        <div className="acoes" style={{ marginTop: 16 }}>
          <button className="btn" disabled={carregando || !termoEnviado}>Pesquisar (só ver)</button>
          <button type="button" className="btn btn-primario" disabled={carregando || !termoEnviado} onClick={() => buscar(false)}>
            Pesquisar e cadastrar sem site
          </button>
          <span className="muted pequeno">
            {fonte === 'OPENSTREETMAP'
              ? 'Escreva o nome da cidade como no mapa (com acentos). A busca pode levar até 1 minuto.'
              : 'O Google traz no máximo 60 por busca. Faça uma busca por ramo.'}
          </span>
        </div>
      </form>

      <div style={{ marginTop: 18 }}>
        <Erro erro={erro} />
        {carregando && <Carregando />}
        {resultado && !carregando && (
          <>
            <div className="resumo-busca">
              <span className="badge">{resultado.encontradas} encontradas no {NOME_FONTE[resultado.fonte]}</span>
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
              {lugares.length === 0 ? (
                <Vazio titulo={resultado.encontradas === 0 ? 'Nada encontrado' : 'Nenhuma empresa sem site nesta busca'}>
                  {resultado.encontradas === 0
                    ? 'Confira o nome da cidade e a UF, ou tente outro ramo.'
                    : 'Tente outro ramo ou marque “Mostrar também quem tem site”.'}
                </Vazio>
              ) : (
                <div className="tabela-wrap">
                  <table>
                    <thead><tr><th>Empresa</th><th>Telefone</th><th>Situação</th><th></th></tr></thead>
                    <tbody>
                      {lugares.map((l) => {
                        const whats = linkWhatsapp(l.telefone)
                        return (
                          <tr key={l.idExterno}>
                            <td className="nome-cel"><strong>{l.nome}</strong><span>{[l.categoria, l.endereco].filter(Boolean).join(' · ')}</span></td>
                            <td className="nowrap">{l.telefone ?? <span className="muted">sem telefone</span>}</td>
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
                                {l.mapaUrl && <a className="btn btn-sm btn-fantasma" href={l.mapaUrl} target="_blank" rel="noreferrer">Mapa</a>}
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

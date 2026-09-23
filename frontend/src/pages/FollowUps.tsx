import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/cliente'
import type { Interacao } from '../api/tipos'
import { useToast } from '../components/Toast'
import { Carregando, Erro, Vazio } from '../components/ui'
import { formatarDataHora, NOME_TIPO } from '../formato'

const PERIODOS = [
  { valor: 'hoje', nome: 'Até agora (atrasados)' },
  { valor: 'semana', nome: 'Próximos 7 dias' },
  { valor: 'mes', nome: 'Próximos 30 dias' },
]

function limite(periodo: string) {
  const d = new Date()
  if (periodo === 'semana') d.setDate(d.getDate() + 7)
  if (periodo === 'mes') d.setDate(d.getDate() + 30)
  return d.toISOString()
}

export default function FollowUps() {
  const toast = useToast()
  const [periodo, setPeriodo] = useState('semana')
  const [meus, setMeus] = useState(true)
  const [lista, setLista] = useState<Interacao[] | null>(null)
  const [erro, setErro] = useState<unknown>(null)

  useEffect(() => {
    setLista(null)
    api.followUps(limite(periodo), meus).then(setLista).catch(setErro)
  }, [periodo, meus])

  async function concluir(i: Interacao) {
    try {
      await api.concluirFollowUp(i.id)
      setLista((l) => l!.filter((x) => x.id !== i.id))
      toast('Follow-up concluído')
    } catch (err) {
      toast(err instanceof Error ? err.message : 'Erro')
    }
  }

  const agora = new Date()

  return (
    <>
      <div className="topo">
        <div>
          <h1>Follow-ups</h1>
          <p>Retornos combinados com os leads. Registre o contato na empresa e marque como feito.</p>
        </div>
        <div className="acoes">
          <select value={periodo} onChange={(e) => setPeriodo(e.target.value)} style={{ width: 'auto' }}>
            {PERIODOS.map((p) => <option key={p.valor} value={p.valor}>{p.nome}</option>)}
          </select>
          <label className="check"><input type="checkbox" checked={meus} onChange={(e) => setMeus(e.target.checked)} /> Só os meus</label>
        </div>
      </div>
      <Erro erro={erro} />
      <div className="card">
        {!lista ? <Carregando /> : lista.length === 0 ? <Vazio titulo="Nenhum retorno pendente">Quando registrar um contato com “Lembrar em”, ele aparece aqui.</Vazio> : (
          <div className="tabela-wrap">
            <table>
              <thead><tr><th>Quando</th><th>Empresa</th><th>Próximo passo</th><th>Último contato</th><th></th></tr></thead>
              <tbody>
                {lista.map((i) => {
                  const atrasado = new Date(i.dataFollowUp!) < agora
                  return (
                    <tr key={i.id}>
                      <td><span className={`badge ${atrasado ? 'badge-perigo' : 'badge-accent'}`}>{formatarDataHora(i.dataFollowUp)}</span></td>
                      <td className="nome-cel"><Link to={`/empresas/${i.empresaId}`}><strong>{i.empresaNome}</strong></Link><span>{i.contatoNome ?? i.usuario.nome}</span></td>
                      <td>{i.proximoPasso ?? '—'}</td>
                      <td className="muted pequeno">{NOME_TIPO[i.tipo]}: {i.descricao.length > 80 ? i.descricao.slice(0, 80) + '…' : i.descricao}</td>
                      <td>
                        <div className="lugar-acoes">
                          <Link className="btn btn-sm" to={`/empresas/${i.empresaId}`}>Abrir</Link>
                          <button className="btn btn-sm btn-primario" onClick={() => concluir(i)}>Feito</button>
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
  )
}

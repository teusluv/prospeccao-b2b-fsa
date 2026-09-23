import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../api/cliente'
import type { Perfil, Usuario } from '../api/tipos'
import { useToast } from '../components/Toast'
import { Carregando, Erro, Modal } from '../components/ui'
import { formatarData } from '../formato'

export default function Usuarios() {
  const toast = useToast()
  const [lista, setLista] = useState<Usuario[] | null>(null)
  const [erro, setErro] = useState<unknown>(null)
  const [editado, setEditado] = useState<Usuario | 'novo' | null>(null)

  const carregar = () => { api.usuarios().then(setLista).catch(setErro) }
  useEffect(carregar, [])

  return (
    <>
      <div className="topo">
        <div><h1>Usuários</h1><p>Quem pode entrar no ProspectRadar.</p></div>
        <button className="btn btn-primario" onClick={() => setEditado('novo')}>+ Novo usuário</button>
      </div>
      <Erro erro={erro} />
      <div className="card">
        {!lista ? <Carregando /> : (
          <div className="tabela-wrap">
            <table>
              <thead><tr><th>Nome</th><th>E-mail</th><th>Perfil</th><th>Situação</th><th>Desde</th></tr></thead>
              <tbody>
                {lista.map((u) => (
                  <tr key={u.id} className="clicavel" onClick={() => setEditado(u)}>
                    <td><strong>{u.nome}</strong></td>
                    <td>{u.email}</td>
                    <td><span className={`badge ${u.perfil === 'ADMIN' ? 'badge-accent' : ''}`}>{u.perfil === 'ADMIN' ? 'Administrador' : 'Vendedor'}</span></td>
                    <td>{u.ativo ? <span className="badge badge-ok">Ativo</span> : <span className="badge badge-perigo">Inativo</span>}</td>
                    <td className="muted">{formatarData(u.criadoEm)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
      {editado && <UsuarioModal usuario={editado === 'novo' ? undefined : editado} aoFechar={() => setEditado(null)}
        aoSalvar={() => { setEditado(null); carregar(); toast('Usuário salvo') }} />}
    </>
  )
}

function UsuarioModal({ usuario, aoFechar, aoSalvar }: { usuario?: Usuario; aoFechar: () => void; aoSalvar: () => void }) {
  const [nome, setNome] = useState(usuario?.nome ?? '')
  const [email, setEmail] = useState(usuario?.email ?? '')
  const [perfil, setPerfil] = useState<Perfil>(usuario?.perfil ?? 'VENDEDOR')
  const [ativo, setAtivo] = useState(usuario?.ativo ?? true)
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState<unknown>(null)

  async function salvar(e: FormEvent) {
    e.preventDefault()
    try {
      if (usuario) await api.atualizarUsuario(usuario.id, { nome, perfil, ativo, novaSenha: senha || undefined })
      else await api.criarUsuario({ nome, email, senha, perfil })
      aoSalvar()
    } catch (err) {
      setErro(err)
    }
  }

  return (
    <Modal titulo={usuario ? 'Editar usuário' : 'Novo usuário'} aoFechar={aoFechar} rodape={<>
      <button className="btn" onClick={aoFechar}>Cancelar</button>
      <button className="btn btn-primario" form="form-usuario">Salvar</button>
    </>}>
      <form id="form-usuario" className="grade g2" onSubmit={salvar}>
        <div className="col2"><label>Nome *</label><input required maxLength={120} value={nome} onChange={(e) => setNome(e.target.value)} /></div>
        <div className="col2"><label>E-mail *</label><input type="email" required disabled={!!usuario} maxLength={160} value={email} onChange={(e) => setEmail(e.target.value)} /></div>
        <div>
          <label>Perfil</label>
          <select value={perfil} onChange={(e) => setPerfil(e.target.value as Perfil)}>
            <option value="VENDEDOR">Vendedor</option>
            <option value="ADMIN">Administrador</option>
          </select>
        </div>
        <div>
          <label>{usuario ? 'Nova senha (opcional)' : 'Senha *'}</label>
          <input type="password" autoComplete="new-password" required={!usuario} minLength={6} maxLength={72} value={senha} onChange={(e) => setSenha(e.target.value)} />
        </div>
        {usuario && <label className="check col2"><input type="checkbox" checked={ativo} onChange={(e) => setAtivo(e.target.checked)} /> Pode entrar no sistema</label>}
        <div className="col2"><Erro erro={erro} /></div>
      </form>
    </Modal>
  )
}

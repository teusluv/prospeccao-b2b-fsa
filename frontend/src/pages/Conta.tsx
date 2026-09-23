import { useState, type FormEvent } from 'react'
import { api } from '../api/cliente'
import { useAuth } from '../auth'
import { Erro } from '../components/ui'

export default function Conta() {
  const { usuario } = useAuth()
  const [atual, setAtual] = useState('')
  const [nova, setNova] = useState('')
  const [confirma, setConfirma] = useState('')
  const [erro, setErro] = useState<unknown>(null)
  const [ok, setOk] = useState(false)

  async function salvar(e: FormEvent) {
    e.preventDefault()
    setOk(false)
    setErro(null)
    if (nova !== confirma) return setErro(new Error('A confirmação não bate com a nova senha'))
    try {
      await api.trocarSenha(atual, nova)
      setOk(true)
      setAtual(''); setNova(''); setConfirma('')
    } catch (err) {
      setErro(err)
    }
  }

  return (
    <>
      <div className="topo"><div><h1>Minha conta</h1><p>{usuario?.nome} · {usuario?.email}</p></div></div>
      <form className="card card-pad" style={{ maxWidth: 460 }} onSubmit={salvar}>
        <div className="card-titulo"><h2>Trocar senha</h2></div>
        <div className="grade">
          <div><label>Senha atual</label><input type="password" autoComplete="current-password" required value={atual} onChange={(e) => setAtual(e.target.value)} /></div>
          <div><label>Nova senha</label><input type="password" autoComplete="new-password" required minLength={6} maxLength={72} value={nova} onChange={(e) => setNova(e.target.value)} /></div>
          <div><label>Confirmar nova senha</label><input type="password" autoComplete="new-password" required value={confirma} onChange={(e) => setConfirma(e.target.value)} /></div>
          <Erro erro={erro} />
          {ok && <div className="alerta alerta-ok">Senha alterada.</div>}
          <div><button className="btn btn-primario">Salvar nova senha</button></div>
        </div>
      </form>
    </>
  )
}

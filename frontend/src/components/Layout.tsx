import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { api } from '../api/cliente'
import { useAuth } from '../auth'
import { Icone } from './ui'

export default function Layout() {
  const { usuario, sair } = useAuth()
  const [aberta, setAberta] = useState(false)
  const [atrasados, setAtrasados] = useState(0)
  const local = useLocation()

  useEffect(() => setAberta(false), [local.pathname])
  useEffect(() => {
    api.followUps(null, true).then((l) => setAtrasados(l.length)).catch(() => {})
  }, [local.pathname])

  const itens = [
    { para: '/', nome: 'Painel', icone: 'painel' },
    { para: '/empresas', nome: 'Empresas', icone: 'empresas' },
    { para: '/funil', nome: 'Funil', icone: 'funil' },
    { para: '/buscar', nome: 'Buscar sem site', icone: 'radar' },
    { para: '/follow-ups', nome: 'Follow-ups', icone: 'sino', contador: atrasados },
    ...(usuario?.perfil === 'ADMIN' ? [{ para: '/usuarios', nome: 'Usuários', icone: 'usuarios' }] : []),
    { para: '/conta', nome: 'Minha conta', icone: 'conta' },
  ]

  return (
    <div className="app">
      <aside className={`sidebar${aberta ? ' aberta' : ''}`}>
        <div className="marca"><img src="/radar.svg" alt="" />ProspectRadar</div>
        <button className="btn btn-sm menu-mobile" onClick={() => setAberta(!aberta)} aria-label="Menu">
          <Icone nome="menu" />
        </button>
        <nav className="nav">
          {itens.map((i) => (
            <NavLink key={i.para} to={i.para} end={i.para === '/'}>
              <Icone nome={i.icone} />
              {i.nome}
              {!!i.contador && <span className="contador">{i.contador}</span>}
            </NavLink>
          ))}
        </nav>
        <div className="usuario-box">
          <strong>{usuario?.nome}</strong>
          <span>{usuario?.email}</span>
          <div><button className="btn btn-sm" onClick={sair}>Sair</button></div>
        </div>
      </aside>
      <main className="conteudo">
        <Outlet />
      </main>
    </div>
  )
}

import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth'
import Layout from './components/Layout'
import { Carregando } from './components/ui'
import BuscarSemSite from './pages/BuscarSemSite'
import Conta from './pages/Conta'
import EmpresaDetalhe from './pages/EmpresaDetalhe'
import Empresas from './pages/Empresas'
import FollowUps from './pages/FollowUps'
import Funil from './pages/Funil'
import Login from './pages/Login'
import Painel from './pages/Painel'
import Usuarios from './pages/Usuarios'

export default function App() {
  const { usuario, carregando } = useAuth()
  if (carregando) return <Carregando />
  if (!usuario) return <Login />
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Painel />} />
        <Route path="empresas" element={<Empresas />} />
        <Route path="empresas/:id" element={<EmpresaDetalhe />} />
        <Route path="funil" element={<Funil />} />
        <Route path="buscar" element={<BuscarSemSite />} />
        <Route path="follow-ups" element={<FollowUps />} />
        <Route path="usuarios" element={usuario.perfil === 'ADMIN' ? <Usuarios /> : <Navigate to="/" />} />
        <Route path="conta" element={<Conta />} />
        <Route path="*" element={<Navigate to="/" />} />
      </Route>
    </Routes>
  )
}

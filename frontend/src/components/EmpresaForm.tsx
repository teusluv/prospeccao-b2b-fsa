import { useEffect, useState, type FormEvent } from 'react'
import { api, ErroApi } from '../api/cliente'
import type { Empresa, EmpresaForm, EtapaFunil, PorteEmpresa, Usuario } from '../api/tipos'
import { useAuth } from '../auth'
import { ETAPAS, formatarCnpj, NOME_ETAPA, NOME_PORTE, PORTES, UFS } from '../formato'
import { Erro, Modal } from './ui'

const vazio: EmpresaForm = { razaoSocial: '', etapa: 'NOVO' }

export default function EmpresaFormModal({ empresa, aoFechar, aoSalvar }: {
  empresa?: Empresa; aoFechar: () => void; aoSalvar: (e: Empresa) => void
}) {
  const { usuario } = useAuth()
  const [form, setForm] = useState<EmpresaForm>(() => empresa ? {
    ...empresa, cnpj: empresa.cnpj ? formatarCnpj(empresa.cnpj) : '', responsavelId: empresa.responsavel?.id ?? null,
  } : vazio)
  const [usuarios, setUsuarios] = useState<Usuario[]>([])
  const [erro, setErro] = useState<unknown>(null)
  const [campos, setCampos] = useState<Record<string, string>>({})
  const [salvando, setSalvando] = useState(false)

  useEffect(() => {
    if (usuario?.perfil === 'ADMIN') api.usuarios().then(setUsuarios).catch(() => {})
  }, [usuario])

  const set = <K extends keyof EmpresaForm>(k: K, v: EmpresaForm[K]) => setForm((f) => ({ ...f, [k]: v }))
  const texto = (k: keyof EmpresaForm) => ({
    value: (form[k] as string | null | undefined) ?? '',
    onChange: (e: { target: { value: string } }) => set(k, e.target.value as never),
  })

  async function salvar(e: FormEvent) {
    e.preventDefault()
    setSalvando(true)
    setErro(null)
    setCampos({})
    try {
      const dados = { ...form }
      const salvo = empresa ? await api.atualizarEmpresa(empresa.id, dados) : await api.criarEmpresa(dados)
      aoSalvar(salvo)
    } catch (err) {
      setErro(err)
      if (err instanceof ErroApi && err.campos) setCampos(err.campos)
    } finally {
      setSalvando(false)
    }
  }

  const erroCampo = (k: string) => campos[k] && <div className="erro-campo">{campos[k]}</div>

  return (
    <Modal
      titulo={empresa ? 'Editar empresa' : 'Nova empresa'}
      aoFechar={aoFechar}
      rodape={<>
        <button className="btn" onClick={aoFechar}>Cancelar</button>
        <button className="btn btn-primario" form="form-empresa" disabled={salvando}>{salvando ? 'Salvando…' : 'Salvar'}</button>
      </>}
    >
      <form id="form-empresa" onSubmit={salvar} className="grade g2">
        <div className="campo col2">
          <label>Razão social *</label>
          <input {...texto('razaoSocial')} required maxLength={200} />
          {erroCampo('razaoSocial')}
        </div>
        <div className="campo"><label>Nome fantasia</label><input {...texto('nomeFantasia')} maxLength={200} /></div>
        <div className="campo"><label>CNPJ</label><input {...texto('cnpj')} placeholder="00.000.000/0000-00" maxLength={18} />{erroCampo('cnpj')}</div>
        <div className="campo"><label>Segmento</label><input {...texto('segmento')} placeholder="Ex.: Restaurante" maxLength={100} /></div>
        <div className="campo">
          <label>Porte</label>
          <select value={form.porte ?? ''} onChange={(e) => set('porte', (e.target.value || null) as PorteEmpresa | null)}>
            <option value="">—</option>
            {PORTES.map((p) => <option key={p} value={p}>{NOME_PORTE[p]}</option>)}
          </select>
        </div>
        <div className="campo"><label>Cidade</label><input {...texto('cidade')} maxLength={100} /></div>
        <div className="campo">
          <label>UF</label>
          <select value={form.uf ?? ''} onChange={(e) => set('uf', e.target.value || null)}>
            <option value="">—</option>
            {UFS.map((u) => <option key={u}>{u}</option>)}
          </select>
        </div>
        <div className="campo"><label>Telefone</label><input {...texto('telefone')} maxLength={30} /></div>
        <div className="campo"><label>E-mail</label><input type="email" {...texto('email')} maxLength={160} />{erroCampo('email')}</div>
        <div className="campo"><label>Site</label><input {...texto('site')} placeholder="Deixe vazio se não tiver" maxLength={200} /></div>
        <div className="campo"><label>Origem</label><input {...texto('origem')} placeholder="Indicação, Google Maps…" maxLength={100} /></div>
        <div className="campo">
          <label>Valor estimado (R$)</label>
          <input type="number" min={0} step="0.01" value={form.valorEstimado ?? ''}
            onChange={(e) => set('valorEstimado', e.target.value === '' ? null : Number(e.target.value))} />
        </div>
        <div className="campo">
          <label>Etapa</label>
          <select value={form.etapa ?? 'NOVO'} onChange={(e) => set('etapa', e.target.value as EtapaFunil)}>
            {ETAPAS.filter((et) => et !== 'PERDIDO' || empresa?.etapa === 'PERDIDO').map((et) => <option key={et} value={et}>{NOME_ETAPA[et]}</option>)}
          </select>
        </div>
        {usuarios.length > 0 && (
          <div className="campo col2">
            <label>Responsável</label>
            <select value={form.responsavelId ?? ''} onChange={(e) => set('responsavelId', e.target.value ? Number(e.target.value) : null)}>
              <option value="">{empresa ? '—' : 'Eu mesmo'}</option>
              {usuarios.filter((u) => u.ativo).map((u) => <option key={u.id} value={u.id}>{u.nome}</option>)}
            </select>
          </div>
        )}
        <div className="campo col2"><label>Observações</label><textarea {...texto('observacoes')} maxLength={4000} /></div>
        <div className="col2"><Erro erro={erro} /></div>
      </form>
    </Modal>
  )
}

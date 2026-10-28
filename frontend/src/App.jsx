import { Navigate, Route, Routes } from 'react-router-dom';
import { sessao } from './api.js';
import Loja from './site/Loja.jsx';
import Layout from './admin/Layout.jsx';
import Entrar from './admin/Entrar.jsx';
import Painel from './admin/Painel.jsx';
import Orcamentos from './admin/Orcamentos.jsx';
import Encomendas from './admin/Encomendas.jsx';
import Clientes from './admin/Clientes.jsx';
import Financeiro from './admin/Financeiro.jsx';
import Materiais from './admin/Materiais.jsx';
import Portfolio from './admin/Portfolio.jsx';
import Configuracoes from './admin/Configuracoes.jsx';
import Produtos from './admin/Produtos.jsx';
import Vendas from './admin/Vendas.jsx';
import Agenda from './admin/Agenda.jsx';
import Relatorios from './admin/Relatorios.jsx';
import Lembretes from './admin/Lembretes.jsx';
import Compras from './admin/Compras.jsx';
import ContasFixas from './admin/ContasFixas.jsx';
import Depoimentos from './admin/Depoimentos.jsx';
import AprovarArte from './site/AprovarArte.jsx';
import FichaProducao from './admin/FichaProducao.jsx';
import Historico from './admin/Historico.jsx';
import { TrocarSenha } from './admin/Configuracoes.jsx';
import { Topo } from './admin/Layout.jsx';

function Protegido({ children }) {
  return sessao.token() ? children : <Navigate to="/admin/entrar" replace />;
}

/** Telas só da administradora: a ajudante volta para o painel. */
function SoAdmin({ children }) {
  return sessao.ajudante() ? <Navigate to="/admin" replace /> : children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Loja />} />
      <Route path="/catalogo" element={<Loja modo="catalogo" />} />
      <Route path="/arte/:codigo" element={<AprovarArte />} />
      <Route path="/pedido/:codigo" element={<AprovarArte />} />
      <Route path="/admin/entrar" element={<Entrar />} />
      <Route path="/admin/encomendas/:id/ficha" element={<Protegido><FichaProducao /></Protegido>} />
      <Route path="/admin" element={<Protegido><Layout /></Protegido>}>
        <Route index element={<Painel />} />
        <Route path="orcamentos" element={<SoAdmin><Orcamentos /></SoAdmin>} />
        <Route path="encomendas" element={<Encomendas />} />
        <Route path="produtos" element={<Produtos />} />
        <Route path="vendas" element={<Vendas />} />
        <Route path="agenda" element={<Agenda />} />
        <Route path="relatorios" element={<SoAdmin><Relatorios /></SoAdmin>} />
        <Route path="historico" element={<SoAdmin><Historico /></SoAdmin>} />
        <Route path="lembretes" element={<SoAdmin><Lembretes /></SoAdmin>} />
        <Route path="clientes" element={<SoAdmin><Clientes /></SoAdmin>} />
        <Route path="financeiro" element={<SoAdmin><Financeiro /></SoAdmin>} />
        <Route path="financeiro/contas" element={<SoAdmin><ContasFixas /></SoAdmin>} />
        <Route path="materiais/compras" element={<SoAdmin><Compras /></SoAdmin>} />
        <Route path="materiais" element={<Materiais />} />
        <Route path="portfolio" element={<SoAdmin><Portfolio /></SoAdmin>} />
        <Route path="portfolio/depoimentos" element={<SoAdmin><Depoimentos /></SoAdmin>} />
        <Route path="configuracoes" element={<SoAdmin><Configuracoes /></SoAdmin>} />
        <Route path="senha" element={<><Topo titulo="Minha senha" /><TrocarSenha /></>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

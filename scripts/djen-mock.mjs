#!/usr/bin/env node
/**
 * Simulador da API pública do DJEN para desenvolvimento local (a API real exige saída de rede para
 * comunicaapi.pje.jus.br). Reproduz o formato assumido pelo adapter `djen` (ver docs/ARQUITETURA.md, risco R1).
 *
 *   node scripts/djen-mock.mjs [porta]      (padrão 8089)
 *
 * Comportamento:
 *  - GET /api/v1/comunicacao?numeroProcesso=...  → 0 a 3 comunicações determinísticas por processo
 *  - processos do TJPE (TR 17) respondem 503, para exercitar falhas
 *  - GET /api/v1/comunicacao/tribunal            → lista de tribunais estaduais
 */
import { createServer } from 'node:http'

const port = Number(process.argv[2] ?? 8089)

const templates = [
  { tipoDocumento: 'Sentença', texto: 'Ante o exposto, JULGO PARCIALMENTE PROCEDENTE o pedido para condenar a ré ao pagamento de indenização por danos materiais.' },
  { tipoDocumento: 'Despacho', texto: 'Intimem-se as partes para que especifiquem as provas que pretendem produzir, no prazo de 15 dias.' },
  { tipoDocumento: 'Decisão', texto: 'Defiro a tutela de urgência requerida, determinando a suspensão dos descontos até ulterior deliberação.' },
  { tipoDocumento: 'Intimação', texto: 'Fica a parte autora intimada a se manifestar sobre o laudo pericial juntado aos autos, no prazo de 15 dias.' },
]

const isoDaysAgo = (days) => {
  const d = new Date(Date.now() - days * 86400000 - 3 * 3600000)
  return d.toISOString().slice(0, 10)
}

function communications(number) {
  const seed = [...number].reduce((sum, c) => sum + Number(c), 0)
  const count = seed % 4
  return Array.from({ length: count }, (_, i) => {
    const t = templates[(seed + i) % templates.length]
    return {
      id: seed * 10 + i,
      hash: `mock-${number}-${i}`,
      siglaTribunal: 'TJ',
      data_disponibilizacao: isoDaysAgo(i * 3 + (seed % 5)),
      tipoComunicacao: 'Intimação',
      tipoDocumento: t.tipoDocumento,
      nomeOrgao: `${(seed % 9) + 1}ª Vara Cível`,
      nomeClasse: 'Procedimento Comum Cível',
      texto: t.texto,
      numero_processo: number,
      link: 'https://comunica.pje.jus.br/',
    }
  })
}

createServer((req, res) => {
  const url = new URL(req.url, `http://localhost:${port}`)
  const send = (status, body) => {
    res.writeHead(status, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify(body))
  }
  if (url.pathname === '/api/v1/comunicacao') {
    const number = (url.searchParams.get('numeroProcesso') ?? '').replace(/\D/g, '')
    if (number.slice(14, 16) === '17') return send(503, { message: 'Serviço indisponível (simulado)' })
    const items = communications(number)
    return send(200, { status: 'success', message: 'ok', count: items.length, items })
  }
  if (url.pathname === '/api/v1/comunicacao/tribunal') {
    return send(200, [
      { uf: 'SP', nomeEstado: 'São Paulo', instituicoes: [{ sigla: 'TJSP', nome: 'Tribunal de Justiça de São Paulo' }] },
      { uf: 'RJ', nomeEstado: 'Rio de Janeiro', instituicoes: [{ sigla: 'TJRJ', nome: 'Tribunal de Justiça do Rio de Janeiro' }] },
    ])
  }
  send(404, { message: 'Não encontrado' })
}).listen(port, () => console.log(`Simulador DJEN em http://localhost:${port}`))

const DATE = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'America/Sao_Paulo' })
const DATE_TIME = new Intl.DateTimeFormat('pt-BR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  timeZone: 'America/Sao_Paulo',
})

/** Datas ISO (yyyy-MM-dd) do backend são datas civis: formatadas sem conversão de fuso. */
export function formatDate(value?: string | null): string {
  if (!value) return '—'
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    const [y, m, d] = value.split('-')
    return `${d}/${m}/${y}`
  }
  return DATE.format(new Date(value))
}

export function formatDateTime(value?: string | null): string {
  if (!value) return '—'
  return DATE_TIME.format(new Date(value)).replace(',', ' às')
}

/** Tempo relativo usado só em metadados ("há 2 h"), conforme o guia de conteúdo. */
export function formatRelative(value?: string | null, now: Date = new Date()): string {
  if (!value) return 'nunca'
  const minutes = Math.round((now.getTime() - new Date(value).getTime()) / 60000)
  if (minutes < 1) return 'agora'
  if (minutes < 60) return `há ${minutes} min`
  const hours = Math.round(minutes / 60)
  if (hours < 24) return `há ${hours} h`
  const days = Math.round(hours / 24)
  return `há ${days} ${days === 1 ? 'dia' : 'dias'}`
}

/** Máscara progressiva do número CNJ: NNNNNNN-DD.AAAA.J.TR.OOOO */
export function maskProcessNumber(value: string): string {
  const d = value.replace(/\D/g, '').slice(0, 20)
  const parts = [d.slice(0, 7), d.slice(7, 9), d.slice(9, 13), d.slice(13, 14), d.slice(14, 16), d.slice(16, 20)]
  let out = parts[0] ?? ''
  if (parts[1]) out += `-${parts[1]}`
  if (parts[2]) out += `.${parts[2]}`
  if (parts[3]) out += `.${parts[3]}`
  if (parts[4]) out += `.${parts[4]}`
  if (parts[5]) out += `.${parts[5]}`
  return out
}

/** Máscara de CPF (11 dígitos) ou CNPJ (14 dígitos). */
export function maskDocument(value: string): string {
  const d = value.replace(/\D/g, '').slice(0, 14)
  if (d.length <= 11) {
    return d
      .replace(/(\d{3})(\d)/, '$1.$2')
      .replace(/(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
      .replace(/(\d{3})\.(\d{3})\.(\d{3})(\d{1,2})$/, '$1.$2.$3-$4')
  }
  return d
    .replace(/^(\d{2})(\d)/, '$1.$2')
    .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1/$2')
    .replace(/(\d{4})(\d{1,2})$/, '$1-$2')
}

export function truncate(text: string, max: number): string {
  return text.length <= max ? text : `${text.slice(0, max - 1).trimEnd()}…`
}

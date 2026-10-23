import { describe, expect, it } from 'vitest'
import { formatDate, formatDateTime, formatRelative, maskDocument, maskProcessNumber, truncate } from './format'

describe('format', () => {
  it('formats civil dates without timezone shifts', () => {
    expect(formatDate('2026-10-22')).toBe('22/10/2026')
    expect(formatDate(null)).toBe('—')
    expect(formatDate('2026-10-23T02:00:00Z')).toBe('22/10/2026')
  })

  it('formats date-time in Brasília time', () => {
    expect(formatDateTime('2026-10-23T09:00:00Z')).toBe('23/10/2026 às 06:00')
    expect(formatDateTime(undefined)).toBe('—')
  })

  it('formats relative times for metadata', () => {
    const now = new Date('2026-10-23T12:00:00Z')
    expect(formatRelative('2026-10-23T11:59:40Z', now)).toBe('agora')
    expect(formatRelative('2026-10-23T11:30:00Z', now)).toBe('há 30 min')
    expect(formatRelative('2026-10-23T10:00:00Z', now)).toBe('há 2 h')
    expect(formatRelative('2026-10-22T12:00:00Z', now)).toBe('há 1 dia')
    expect(formatRelative('2026-10-20T12:00:00Z', now)).toBe('há 3 dias')
    expect(formatRelative(null)).toBe('nunca')
  })

  it('masks CNJ numbers progressively', () => {
    expect(maskProcessNumber('1001234')).toBe('1001234')
    expect(maskProcessNumber('100123483')).toBe('1001234-83')
    expect(maskProcessNumber('10012348320268260100999')).toBe('1001234-83.2026.8.26.0100')
  })

  it('masks CPF and CNPJ', () => {
    expect(maskDocument('52998224725')).toBe('529.982.247-25')
    expect(maskDocument('11222333000181')).toBe('11.222.333/0001-81')
  })

  it('truncates with ellipsis', () => {
    expect(truncate('abc', 5)).toBe('abc')
    expect(truncate('abcdefgh', 5)).toBe('abcd…')
  })
})

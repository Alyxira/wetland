import type { Directive } from 'vue'

declare const ySlice: Directive<HTMLElement, Record<string, unknown> | undefined>

export function applyYSlice(el: HTMLElement, options?: Record<string, unknown>): void

export function cleanupYSlice(el: HTMLElement): void

export function initGlobalYSlice(root?: Document | Element, options?: Record<string, unknown>): void

export function startGlobalYSlice(rootSelector?: string, options?: Record<string, unknown>): (() => void) | null

export default ySlice

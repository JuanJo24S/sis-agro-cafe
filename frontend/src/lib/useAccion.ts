import { useState, type ChangeEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { refrescar } from '../api/agrocafe'
import { ApiError, type ErrorCampo } from '../api/http'
import { useAvisos } from '../components/avisos'

/**
 * Ejecuta una escritura contra el backend: muestra el aviso de exito, refresca las
 * vistas afectadas y conserva el error (con los errores por campo del problem+json).
 */
export function useAccion<A extends unknown[], R>(
  accion: (...args: A) => Promise<R>,
  opciones: {
    exito?: string | ((resultado: R) => string)
    detalle?: string
    /** Muestra los errores como aviso flotante (acciones sin formulario). */
    avisarError?: boolean
    alTerminar?: (resultado: R) => void
  } = {},
) {
  const cliente = useQueryClient()
  const avisos = useAvisos()
  const mutacion = useMutation({
    mutationFn: (args: A) => accion(...args),
    onSuccess: (resultado) => {
      refrescar(cliente)
      if (opciones.exito) {
        const titulo = typeof opciones.exito === 'function' ? opciones.exito(resultado) : opciones.exito
        avisos.exito(titulo, opciones.detalle)
      }
      opciones.alTerminar?.(resultado)
    },
    onError: (error) => {
      if (opciones.avisarError) avisos.error('No se pudo completar la acción', mensajeDe(error))
    },
  })

  const error = mutacion.error
  return {
    ejecutar: (...args: A) => mutacion.mutate(args),
    ocupado: mutacion.isPending,
    error: error ? mensajeDe(error) : null,
    erroresCampo: error instanceof ApiError ? error.errores : ([] as ErrorCampo[]),
    reiniciar: mutacion.reset,
  }
}

export const mensajeDe = (error: unknown) =>
  error instanceof Error ? error.message : 'Ocurrió un error inesperado.'

/** Estado simple de formulario: todos los valores se manejan como texto hasta enviar. */
export function useFormulario<T extends Record<string, string>>(inicial: T) {
  const [valores, setValores] = useState<T>(inicial)
  const enlazar = (nombre: keyof T & string) => ({
    name: nombre,
    value: valores[nombre] ?? '',
    onChange: (e: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
      setValores((v) => ({ ...v, [nombre]: e.target.value })),
  })
  const fijar = (nombre: keyof T & string, valor: string) => setValores((v) => ({ ...v, [nombre]: valor }))
  return { valores, enlazar, fijar, reiniciar: (v: T) => setValores(v) }
}

/** Convierte un campo de texto a numero; vacio => undefined (el contrato lo trata como null). */
export const aNumero = (texto: string): number | undefined => {
  if (texto.trim() === '') return undefined
  const valor = Number(texto.replace(',', '.'))
  return Number.isFinite(valor) ? valor : undefined
}

export const errorDeCampo = (errores: ErrorCampo[], campo: string) =>
  errores.find((e) => e.campo === campo)?.mensaje

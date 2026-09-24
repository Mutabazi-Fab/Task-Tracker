import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createGuidanceNote, deleteGuidanceNote, updateGuidanceNote } from '../api/incidentGuidance.api'
import type { CreateGuidanceNoteRequest, UpdateGuidanceNoteRequest } from '../../../types/incidentGuidance.types'

const QUERY_KEY = ['incidents', 'guidance-notes']

export function useCreateGuidanceNote() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CreateGuidanceNoteRequest) => createGuidanceNote(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUERY_KEY }),
  })
}

export function useUpdateGuidanceNote(id: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: UpdateGuidanceNoteRequest) => updateGuidanceNote(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUERY_KEY }),
  })
}

export function useDeleteGuidanceNote() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteGuidanceNote(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUERY_KEY }),
  })
}

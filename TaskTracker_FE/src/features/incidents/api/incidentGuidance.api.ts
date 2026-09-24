import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type { CreateGuidanceNoteRequest, GuidanceNote, UpdateGuidanceNoteRequest } from '../../../types/incidentGuidance.types'

export async function fetchGuidanceNotes(): Promise<GuidanceNote[]> {
  const { data } = await axiosClient.get<GuidanceNote[]>(endpoints.incidentGuidanceNotes.list())
  return data
}

export async function createGuidanceNote(payload: CreateGuidanceNoteRequest): Promise<GuidanceNote> {
  const { data } = await axiosClient.post<GuidanceNote>(endpoints.incidentGuidanceNotes.create(), payload)
  return data
}

export async function updateGuidanceNote(id: number, payload: UpdateGuidanceNoteRequest): Promise<GuidanceNote> {
  const { data } = await axiosClient.put<GuidanceNote>(endpoints.incidentGuidanceNotes.update(id), payload)
  return data
}

export async function deleteGuidanceNote(id: number): Promise<void> {
  await axiosClient.delete(endpoints.incidentGuidanceNotes.remove(id))
}

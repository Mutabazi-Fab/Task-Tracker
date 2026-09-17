/** One supporting document attached to a task — metadata only, never the bytes (those come
 *  back only from the dedicated download endpoint). */
export interface TaskDocument {
  id: number
  fileName: string
  contentType: string
  fileSize: number
  uploadedByName: string
  uploadedById: number
  uploadedAt: string
}

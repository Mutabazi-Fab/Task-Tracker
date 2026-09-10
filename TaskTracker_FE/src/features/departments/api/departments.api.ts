import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type {
  ChangeDepartmentHeadRequest,
  CreateDepartmentRequest,
  Department,
  RenameDepartmentRequest,
} from '../../../types/department.types'

export async function fetchDepartments(): Promise<Department[]> {
  const { data } = await axiosClient.get<Department[]>(endpoints.departments.list())
  return data
}

export async function fetchDepartment(id: number): Promise<Department> {
  const { data } = await axiosClient.get<Department>(endpoints.departments.detail(id))
  return data
}

export async function createDepartment(payload: CreateDepartmentRequest): Promise<Department> {
  const { data } = await axiosClient.post<Department>(endpoints.departments.create(), payload)
  return data
}

export async function renameDepartment(id: number, payload: RenameDepartmentRequest): Promise<Department> {
  const { data } = await axiosClient.put<Department>(endpoints.departments.rename(id), payload)
  return data
}

export async function changeDepartmentHead(
  id: number,
  payload: ChangeDepartmentHeadRequest,
): Promise<Department> {
  const { data } = await axiosClient.put<Department>(endpoints.departments.changeHead(id), payload)
  return data
}

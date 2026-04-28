import { create } from 'zustand'

interface SubjectState {
    q_subject_area_id: string | null | number | undefined
    setSubject: (id: string | null | number | undefined) => void
}

/**
 * Store for subject area id
 * 学科领域树选择获取问题，保存学科领域id，返回时使用这个Id，间接实现跳转返回不变
 */
export const useSubjectStore = create<SubjectState>((set) => ({
    q_subject_area_id: null,
    setSubject: (id) => set({ q_subject_area_id: id })
}))

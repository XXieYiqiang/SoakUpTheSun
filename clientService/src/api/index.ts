
import { http } from '@/http/http'

/**
 * 上传图片分析
 * @param file 图片文件（H5 FormData）或图片路径（App/小程序）
 * @param description 描述内容
 */
export function uploadPictureAnalysis(file: Blob | File | string, description: string = '') {
  // #ifdef H5
  const form = new FormData()
  if (typeof file === 'string') {
    throw new Error('H5 上传请传入 File/Blob')
  }
  form.append('file', file, `capture_${Date.now()}.jpg`)
  form.append('descriptionContent', description)
  return http.post<any>('/picApi/api/picture/uploadPictureAnalysis', form as any)
  // #endif

  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: '/picApi/api/picture/uploadPictureAnalysis',
      filePath: file as string,
      name: 'file',
      formData: {
        descriptionContent: description,
      },
      success: (res) => {
        const raw = (res as any)?.data
        if (typeof raw === 'string') {
          try {
            resolve(JSON.parse(raw))
          }
          catch {
            resolve(raw)
          }
          return
        }
        resolve(raw)
      },
      fail: err => reject(err),
    })
  })
}

/**
 * 获取图片分析结果
 * @param pictureId 图片ID
 */
export function getPictureAnalysisResponse(pictureId: string | number) {
  return http.get<any>(`/picApi/api/picture/getPictureAnalysisResponse`, { pictureId })
}

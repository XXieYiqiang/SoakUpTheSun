
import { http } from '@/http/http'

/**
 * 上传图片分析
 * @param filePath 图片路径
 * @param description 描述内容
 */
export function uploadPictureAnalysis(filePath: string, description: string = '') {
  return http.post<any>('/picture/uploadPictureAnalysis', {
    filePath,
    name: 'file',
    formData: {
      descriptionContent: description
    }
  })
}

/**
 * 获取图片分析结果
 * @param pictureId 图片ID
 */
export function getPictureAnalysisResponse(pictureId: string | number) {
  return http.get<any>(`/picture/getPictureAnalysisResponse`, { pictureId })
}

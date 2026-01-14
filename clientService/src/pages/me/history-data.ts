export interface HistoryItem {
  id: string
  title: string
  description: string
  time: string
  status: 'success' | 'failed' | 'pending'
  type: 'fingerprint' | 'face' | 'voice' | 'other'
  duration?: string
  fileSize?: string
}

export const historyData: HistoryItem[] = [
  {
    id: '1',
    title: '指纹识别生成',
    description: '用户指纹特征提取完成',
    time: '2024-01-10 14:30:25',
    status: 'success',
    type: 'fingerprint',
    duration: '2.3s',
    fileSize: '128KB',
  },
  {
    id: '2',
    title: '人脸识别生成',
    description: '人脸特征向量计算完成',
    time: '2024-01-10 12:15:30',
    status: 'success',
    type: 'face',
    duration: '3.5s',
    fileSize: '256KB',
  },
  {
    id: '3',
    title: '声纹识别生成',
    description: '语音特征提取失败',
    time: '2024-01-10 10:45:18',
    status: 'failed',
    type: 'voice',
    duration: '1.8s',
  },
  {
    id: '4',
    title: '指纹识别生成',
    description: '用户指纹特征提取完成',
    time: '2024-01-09 16:20:45',
    status: 'success',
    type: 'fingerprint',
    duration: '2.1s',
    fileSize: '125KB',
  },
  {
    id: '5',
    title: '人脸识别生成',
    description: '人脸特征向量计算完成',
    time: '2024-01-09 14:35:22',
    status: 'success',
    type: 'face',
    duration: '3.2s',
    fileSize: '248KB',
  },
  {
    id: '6',
    title: '多模态识别生成',
    description: '正在处理中...',
    time: '2024-01-09 11:10:05',
    status: 'pending',
    type: 'other',
  },
  {
    id: '7',
    title: '指纹识别生成',
    description: '用户指纹特征提取完成',
    time: '2024-01-08 15:50:33',
    status: 'success',
    type: 'fingerprint',
    duration: '2.4s',
    fileSize: '130KB',
  },
  {
    id: '8',
    title: '声纹识别生成',
    description: '语音特征提取完成',
    time: '2024-01-08 13:25:18',
    status: 'success',
    type: 'voice',
    duration: '2.0s',
    fileSize: '64KB',
  },
  {
    id: '9',
    title: '人脸识别生成',
    description: '人脸特征向量计算完成',
    time: '2024-01-08 10:40:55',
    status: 'success',
    type: 'face',
    duration: '3.1s',
    fileSize: '252KB',
  },
  {
    id: '10',
    title: '指纹识别生成',
    description: '用户指纹特征提取完成',
    time: '2024-01-07 17:15:42',
    status: 'success',
    type: 'fingerprint',
    duration: '2.2s',
    fileSize: '127KB',
  },
]

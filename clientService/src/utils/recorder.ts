// AudioWorklet processor code as a string
const workletCode = `
class RecorderProcessor extends AudioWorkletProcessor {
  process(inputs, outputs, parameters) {
    const input = inputs[0];
    if (input.length > 0) {
      const channelData = input[0];
      // Post the data back to the main thread
      this.port.postMessage(channelData);
    }
    return true;
  }
}
registerProcessor('recorder-processor', RecorderProcessor);
`

export class H5Recorder {
  private context: AudioContext | null = null
  private audioInput: MediaStreamAudioSourceNode | null = null
  private workletNode: AudioWorkletNode | null = null
  private audioData: Float32Array[] = []
  private stream: MediaStream | null = null
  private sampleRate: number = 16000

  constructor(sampleRate: number = 16000) {
    this.sampleRate = sampleRate
  }

  async start() {
    this.audioData = []
    
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      throw new Error('浏览器不支持录音')
    }

    this.stream = await navigator.mediaDevices.getUserMedia({
      audio: {
        sampleRate: this.sampleRate,
        channelCount: 1,
        echoCancellation: true,
        noiseSuppression: true,
      },
    })

    const AudioContext = window.AudioContext || (window as any).webkitAudioContext
    this.context = new AudioContext({ sampleRate: this.sampleRate })
    
    // Create AudioWorklet
    const blob = new Blob([workletCode], { type: 'application/javascript' })
    const workletUrl = URL.createObjectURL(blob)
    
    try {
      await this.context.audioWorklet.addModule(workletUrl)
      this.workletNode = new AudioWorkletNode(this.context, 'recorder-processor')
      
      this.workletNode.port.onmessage = (event) => {
        // event.data is Float32Array
        this.audioData.push(event.data)
      }

      this.audioInput = this.context.createMediaStreamSource(this.stream)
      this.audioInput.connect(this.workletNode)
      this.workletNode.connect(this.context.destination)
    } finally {
      URL.revokeObjectURL(workletUrl)
    }
  }

  stop(): Promise<{ buffer: ArrayBuffer, base64: string, len: number }> {
    return new Promise((resolve, reject) => {
      if (this.workletNode && this.audioInput && this.context) {
        this.audioInput.disconnect()
        this.workletNode.disconnect()
        if (this.context.state !== 'closed') {
          this.context.close()
        }
      }
      
      if (this.stream) {
        this.stream.getTracks().forEach(track => track.stop())
        this.stream = null
      }

      if (this.audioData.length === 0) {
        reject(new Error('录音数据为空'))
        return
      }

      // 处理数据
      const length = this.audioData.reduce((acc, cur) => acc + cur.length, 0)
      const mergedData = new Float32Array(length)
      let offset = 0
      for (const chunk of this.audioData) {
        mergedData.set(chunk, offset)
        offset += chunk.length
      }

      const pcmData = this.floatTo16BitPCM(mergedData)
      const base64 = this.arrayBufferToBase64(pcmData.buffer)
      
      resolve({
        buffer: pcmData.buffer,
        base64,
        len: pcmData.byteLength
      })
      
      // 清理
      this.audioData = []
      this.context = null
      this.audioInput = null
      this.workletNode = null
    })
  }

  private floatTo16BitPCM(input: Float32Array) {
    const output = new Int16Array(input.length)
    for (let i = 0; i < input.length; i++) {
      const s = Math.max(-1, Math.min(1, input[i]))
      output[i] = s < 0 ? s * 0x8000 : s * 0x7FFF
    }
    return output
  }

  private arrayBufferToBase64(buffer: ArrayBuffer) {
    let binary = ''
    const bytes = new Uint8Array(buffer)
    const len = bytes.byteLength
    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(bytes[i])
    }
    return window.btoa(binary)
  }
}

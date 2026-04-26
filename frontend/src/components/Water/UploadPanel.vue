<template>
  <div class="upload-panel">
    <h3>上传遥感影像</h3>
    
    <form @submit.prevent="handleUpload" class="upload-form">
      <div class="form-group">
        <label>选择TIFF文件 *</label>
        <input 
          type="file" 
          ref="fileInput"
          accept=".tif,.tiff"
          @change="handleFileChange"
          required
        />
        <div class="file-info" v-if="selectedFile">
          <span>{{ selectedFile.name }}</span>
          <span class="file-size">{{ formatFileSize(selectedFile.size) }}</span>
        </div>
      </div>
      
      <div class="form-group">
        <label>传感器类型 *</label>
        <select v-model="sensor" required>
          <option value="">请选择传感器</option>
          <option value="Sentinel-2 MSI">Sentinel-2 MSI</option>
          <option value="Landsat-8 OLI">Landsat-8 OLI</option>
          <option value="Landsat-9 OLI-2">Landsat-9 OLI-2</option>
          <option value="GF-1 WFV">高分一号 WFV</option>
          <option value="GF-2 PMS">高分二号 PMS</option>
          <option value="GF-6 WFV">高分六号 WFV</option>
          <option value="MODIS">MODIS</option>
          <option value="Other">其他</option>
        </select>
      </div>
      
      <div class="form-group">
        <label>获取日期 *</label>
        <input 
          type="datetime-local" 
          v-model="acquiredAt"
          required
        />
      </div>
      
      <div class="form-group">
        <label>云量 (%)</label>
        <input 
          type="number" 
          v-model.number="cloudCover"
          min="0"
          max="100"
          step="0.1"
          placeholder="0-100"
        />
      </div>
      
      <div class="form-actions">
        <button type="submit" :disabled="uploading" class="upload-btn">
          {{ uploading ? '上传中...' : '上传影像' }}
        </button>
        <button type="button" @click="resetForm" class="reset-btn">重置</button>
      </div>
    </form>
    
    <div class="upload-progress" v-if="uploading">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
      </div>
      <span>{{ uploadProgress }}%</span>
    </div>
    
    <div class="upload-message" :class="messageType" v-if="message">
      {{ message }}
    </div>
    
    <div class="uploaded-info" v-if="uploadedImage">
      <h4>上传成功</h4>
      <div class="info-item">
        <span class="label">影像ID:</span>
        <span class="value">{{ uploadedImage.imageId }}</span>
      </div>
      <div class="info-item">
        <span class="label">文件名:</span>
        <span class="value">{{ uploadedImage.fileName }}</span>
      </div>
      <div class="info-item">
        <span class="label">获取时间:</span>
        <span class="value">{{ formatDateTime(uploadedImage.acquiredAt) }}</span>
      </div>
      <div class="info-item">
        <span class="label">状态:</span>
        <span class="value status">{{ uploadedImage.status }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { imageApi } from '../api'

const emit = defineEmits(['upload-success'])

const fileInput = ref(null)
const selectedFile = ref(null)
const sensor = ref('')
const acquiredAt = ref('')
const cloudCover = ref(null)
const uploading = ref(false)
const uploadProgress = ref(0)
const message = ref('')
const messageType = ref('')
const uploadedImage = ref(null)

function handleFileChange(event) {
  const file = event.target.files[0]
  if (file) {
    selectedFile.value = file
  }
}

function formatFileSize(bytes) {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

function formatDateTime(dateStr) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

async function handleUpload() {
  if (!selectedFile.value) {
    showMessage('请选择文件', 'error')
    return
  }
  
  if (!sensor.value) {
    showMessage('请选择传感器类型', 'error')
    return
  }
  
  if (!acquiredAt.value) {
    showMessage('请选择获取日期', 'error')
    return
  }
  
  uploading.value = true
  uploadProgress.value = 0
  
  const progressInterval = setInterval(() => {
    if (uploadProgress.value < 90) {
      uploadProgress.value += 10
    }
  }, 200)
  
  try {
    const acquiredAtISO = new Date(acquiredAt.value).toISOString()
    
    const response = await imageApi.uploadImage(
      selectedFile.value,
      acquiredAtISO,
      sensor.value,
      cloudCover.value
    )
    
    uploadProgress.value = 100
    
    uploadedImage.value = response.data
    showMessage('影像上传成功！', 'success')
    emit('upload-success', response.data)
    resetForm()
  } catch (error) {
    const errorMsg = error.response?.data?.message || error.message
    showMessage('上传失败: ' + errorMsg, 'error')
  } finally {
    clearInterval(progressInterval)
    uploading.value = false
  }
}

function resetForm() {
  selectedFile.value = null
  sensor.value = ''
  acquiredAt.value = ''
  cloudCover.value = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

function showMessage(text, type) {
  message.value = text
  messageType.value = type
  setTimeout(() => {
    message.value = ''
  }, 5000)
}
</script>

<style scoped>
.upload-panel {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.upload-panel h3 {
  margin: 0 0 20px 0;
  color: #333;
}

.upload-form {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.form-group label {
  font-weight: 500;
  color: #555;
  font-size: 14px;
}

.form-group input[type="text"],
.form-group input[type="number"],
.form-group input[type="datetime-local"],
.form-group select,
.form-group textarea {
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #3388ff;
}

.form-group input[type="file"] {
  padding: 10px;
  border: 2px dashed #ddd;
  border-radius: 4px;
  cursor: pointer;
}

.form-group input[type="file"]:hover {
  border-color: #3388ff;
}

.file-info {
  display: flex;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 4px;
  font-size: 13px;
}

.file-size {
  color: #999;
}

.form-actions {
  display: flex;
  gap: 10px;
}

.upload-btn,
.reset-btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}

.upload-btn {
  background: #28a745;
  color: white;
}

.upload-btn:hover:not(:disabled) {
  background: #218838;
}

.upload-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.reset-btn {
  background: #f8f9fa;
  color: #666;
  border: 1px solid #ddd;
}

.reset-btn:hover {
  background: #e9ecef;
}

.upload-progress {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 15px;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: #e9ecef;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #28a745;
  transition: width 0.3s;
}

.upload-message {
  margin-top: 15px;
  padding: 10px 15px;
  border-radius: 4px;
  font-size: 14px;
}

.upload-message.success {
  background: #d4edda;
  color: #155724;
}

.upload-message.error {
  background: #f8d7da;
  color: #721c24;
}

.uploaded-info {
  margin-top: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e9ecef;
}

.uploaded-info h4 {
  margin: 0 0 15px 0;
  color: #28a745;
  font-size: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .label {
  color: #666;
  font-size: 13px;
}

.info-item .value {
  font-weight: 500;
  color: #333;
  font-size: 13px;
}

.info-item .value.status {
  color: #28a745;
  background: #d4edda;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}
</style>

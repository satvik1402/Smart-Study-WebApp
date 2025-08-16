// Upload Page JavaScript
// Use a single global for API base to avoid re-declaration across multiple scripts
if (!window.API_BASE) {
    window.API_BASE = '/api';
}

// DOM Elements
const uploadZone = document.getElementById('uploadZone');
const fileInput = document.getElementById('fileInput');
const uploadProgress = document.getElementById('uploadProgress');
const progressList = document.getElementById('progressList');
const uploadsList = document.getElementById('uploadsList');
const menuToggle = document.getElementById('menuToggle');
const sidebar = document.querySelector('.sidebar');

// Upload state
let uploadQueue = [];
let isUploading = false;

// Initialize Upload Page
document.addEventListener('DOMContentLoaded', function() {
    setupEventListeners();
    loadRecentUploads();
});

// Setup Event Listeners
function setupEventListeners() {
    // Mobile menu toggle
    if (menuToggle) {
        menuToggle.addEventListener('click', toggleSidebar);
    }

    // File input change
    if (fileInput) {
        fileInput.addEventListener('change', handleFileSelect);
    }

    // Drag and drop events
    if (uploadZone) {
        uploadZone.addEventListener('click', () => fileInput.click());
        uploadZone.addEventListener('dragover', handleDragOver);
        uploadZone.addEventListener('dragleave', handleDragLeave);
        uploadZone.addEventListener('drop', handleDrop);
    }

    // Close sidebar when clicking outside on mobile
    document.addEventListener('click', function(e) {
        if (window.innerWidth <= 1024) {
            if (!sidebar.contains(e.target) && !menuToggle.contains(e.target)) {
                sidebar.classList.remove('active');
            }
        }
    });
}

// Toggle Sidebar
function toggleSidebar() {
    sidebar.classList.toggle('active');
}

// Handle File Selection
function handleFileSelect(event) {
    const files = Array.from(event.target.files);
    if (files.length > 0) {
        processFiles(files);
    }
}

// Handle Drag Over
function handleDragOver(event) {
    event.preventDefault();
    uploadZone.classList.add('dragover');
}

// Handle Drag Leave
function handleDragLeave(event) {
    event.preventDefault();
    uploadZone.classList.remove('dragover');
}

// Handle Drop
function handleDrop(event) {
    event.preventDefault();
    uploadZone.classList.remove('dragover');
    
    const files = Array.from(event.dataTransfer.files);
    if (files.length > 0) {
        processFiles(files);
    }
}

// Process Files
function processFiles(files) {
    console.log('📁 Processing files:', files.map(f => ({ name: f.name, size: f.size, type: f.type })));
    
    const validFiles = files.filter(validateFile);
    console.log('✅ Valid files:', validFiles.map(f => f.name));
    
    if (validFiles.length === 0) {
        showNotification('No valid files selected. Please select PDF, DOC, DOCX, PPT, PPTX, or ZIP files.', 'error');
        return;
    }

    // Add files to upload queue
    uploadQueue.push(...validFiles);
    console.log('📋 Upload queue length:', uploadQueue.length);
    
    // Show upload progress
    showUploadProgress();
    
    // Start uploading if not already uploading
    if (!isUploading) {
        startUpload();
    }
}

// Validate File
function validateFile(file) {
    console.log('🔍 Validating file:', file.name, 'Type:', file.type, 'Size:', file.size);
    
    const allowedTypes = [
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.ms-powerpoint',
        'application/vnd.openxmlformats-officedocument.presentationml.presentation',
        'application/zip'
    ];
    
    const maxSize = 100 * 1024 * 1024; // 100MB
    
    if (!allowedTypes.includes(file.type) && !file.name.toLowerCase().endsWith('.zip')) {
        console.log('❌ File type not supported:', file.name, 'Type:', file.type);
        showNotification(`File type not supported: ${file.name}`, 'error');
        return false;
    }
    
    if (file.size > maxSize) {
        console.log('❌ File too large:', file.name, 'Size:', file.size);
        showNotification(`File too large: ${file.name} (${formatFileSize(file.size)})`, 'error');
        return false;
    }
    
    console.log('✅ File validation passed:', file.name);
    return true;
}

// Show Upload Progress
function showUploadProgress() {
    uploadProgress.style.display = 'block';
    uploadZone.style.display = 'none';
}

// Start Upload
async function startUpload() {
    if (uploadQueue.length === 0) {
        hideUploadProgress();
        return;
    }
    
    isUploading = true;
    
    while (uploadQueue.length > 0) {
        const file = uploadQueue.shift();
        await uploadFile(file);
    }
    
    isUploading = false;
    hideUploadProgress();
    loadRecentUploads();
}

// Upload Single File
async function uploadFile(file) {
    console.log('🚀 Starting upload for file:', file.name, 'Size:', file.size, 'Type:', file.type);
    
    const progressId = 'progress-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
    
    // Create progress item
    const progressItem = createProgressItem(file, progressId);
    progressList.appendChild(progressItem);
    
    try {
        // Create FormData
        const formData = new FormData();
        formData.append('file', file);
        
        console.log('📤 Sending request to:', `${window.API_BASE}/documents/upload`);
        console.log('📄 FormData entries:', Array.from(formData.entries()));
        
        // Update status to uploading
        updateProgressStatus(progressId, 'uploading', 'Uploading...');
        
        // Upload file
        const response = await fetch(`${window.API_BASE}/documents/upload`, {
            method: 'POST',
            body: formData
        });
        
        console.log('📥 Response status:', response.status, response.statusText);
        
        if (!response.ok) {
            throw new Error(`Upload failed: ${response.statusText}`);
        }
        
        const result = await response.json();
        console.log('📋 Response data:', result);
        
        if (result.success) {
            // Update status to processing
            updateProgressStatus(progressId, 'processing', 'Processing...');
            updateProgressBar(progressId, 50);
            
            // Wait for processing to complete
            await waitForProcessing(result.document.id, progressId);
            
            // Update status to completed
            updateProgressStatus(progressId, 'completed', 'Completed');
            updateProgressBar(progressId, 100);
            
            showNotification(`Successfully uploaded: ${file.name}`, 'success');
            
            // Remove progress item after delay
            setTimeout(() => {
                if (progressItem.parentNode) {
                    progressItem.remove();
                }
            }, 3000);
            
        } else {
            throw new Error(result.message || 'Upload failed');
        }
        
    } catch (error) {
        console.error('❌ Upload error:', error);
        console.error('❌ Error details:', {
            name: error.name,
            message: error.message,
            stack: error.stack
        });
        updateProgressStatus(progressId, 'error', 'Upload failed');
        showNotification(`Failed to upload ${file.name}: ${error.message}`, 'error');
        
        // Remove progress item after delay
        setTimeout(() => {
            if (progressItem.parentNode) {
                progressItem.remove();
            }
        }, 5000);
    }
}

// Create Progress Item
function createProgressItem(file, progressId) {
    const fileType = getFileType(file);
    const fileSize = formatFileSize(file.size);
    
    const progressItem = document.createElement('div');
    progressItem.className = 'progress-item';
    progressItem.id = progressId;
    
    progressItem.innerHTML = `
        <div class="progress-item-header">
            <div class="progress-item-info">
                <div class="progress-item-icon ${fileType}">
                    <i class="bx ${getFileIcon(fileType)}"></i>
                </div>
                <div class="progress-item-details">
                    <h4>${file.name}</h4>
                    <p>${fileSize}</p>
                </div>
            </div>
            <div class="progress-item-status uploading">
                <span class="upload-loading"></span>
                <span>Uploading...</span>
            </div>
        </div>
        <div class="progress-bar-container">
            <div class="progress-bar-fill" style="width: 0%"></div>
        </div>
    `;
    
    return progressItem;
}

// Update Progress Status
function updateProgressStatus(progressId, status, text) {
    const progressItem = document.getElementById(progressId);
    if (!progressItem) return;
    
    const statusElement = progressItem.querySelector('.progress-item-status');
    const loadingElement = statusElement.querySelector('.upload-loading');
    
    statusElement.className = `progress-item-status ${status}`;
    statusElement.innerHTML = `
        ${status === 'uploading' || status === 'processing' ? '<span class="upload-loading"></span>' : ''}
        <span>${text}</span>
    `;
}

// Update Progress Bar
function updateProgressBar(progressId, percentage) {
    const progressItem = document.getElementById(progressId);
    if (!progressItem) return;
    
    const progressBar = progressItem.querySelector('.progress-bar-fill');
    if (progressBar) {
        progressBar.style.width = `${percentage}%`;
    }
}

// Wait for Processing
async function waitForProcessing(documentId, progressId) {
    let attempts = 0;
    const maxAttempts = 60; // 5 minutes max
    
    while (attempts < maxAttempts) {
        try {
            const response = await fetch(`${window.API_BASE}/documents/${documentId}`);
            if (response.ok) {
                const document = await response.json();
                
                if (document.status === 'COMPLETED') {
                    return;
                } else if (document.status === 'FAILED') {
                    throw new Error('Document processing failed');
                }
            }
        } catch (error) {
            console.error('Error checking document status:', error);
        }
        
        // Wait 5 seconds before next check
        await new Promise(resolve => setTimeout(resolve, 5000));
        attempts++;
        
        // Update progress bar
        const progress = Math.min(50 + (attempts / maxAttempts) * 50, 99);
        updateProgressBar(progressId, progress);
    }
    
    throw new Error('Processing timeout');
}

// Hide Upload Progress
function hideUploadProgress() {
    uploadProgress.style.display = 'none';
    uploadZone.style.display = 'block';
}

// Cancel Upload
function cancelUpload() {
    uploadQueue = [];
    isUploading = false;
    hideUploadProgress();
    progressList.innerHTML = '';
    showNotification('Upload cancelled', 'info');
}

// Load Recent Uploads
async function loadRecentUploads() {
    try {
        const response = await fetch(`${window.API_BASE}/documents`);
        if (!response.ok) throw new Error('Failed to fetch documents');
        
        const documents = await response.json();
        
        if (!uploadsList) return;
        
        const uploadItems = documents.slice(0, 6).map(doc => {
            const fileType = getFileTypeFromDocument(doc);
            const fileSize = formatFileSize(doc.fileSize);
            const uploadDate = formatDate(doc.uploadDate);
            const isCompleted = (doc.status === 'COMPLETED');
            
            return `
                <div class="upload-item" onclick="${isCompleted ? `viewDocument(${doc.id})` : `event.stopPropagation();`}">
                    <div class="upload-item-header">
                        <div class="upload-item-icon ${fileType}">
                            <i class="bx ${getFileIcon(fileType)}"></i>
                        </div>
                        <div class="upload-item-details">
                            <h4>${doc.originalFilename}</h4>
                            <p>${fileSize} • ${uploadDate}</p>
                        </div>
                    </div>
                    <div class="upload-item-actions">
                        <span class="status-badge ${doc.status.toLowerCase()}">${doc.status}</span>
                        <button class="btn-view" ${isCompleted ? '' : 'disabled title="Processing..."'} onclick="event.stopPropagation(); ${isCompleted ? `viewDocument(${doc.id})` : ''}">
                            <i class="bx bx-show"></i> View
                        </button>
                        <button class="btn-delete" onclick="event.stopPropagation(); deleteDocument(${doc.id})">
                            <i class="bx bx-trash"></i> Delete
                        </button>
                    </div>
                </div>
            `;
        });
        
        uploadsList.innerHTML = uploadItems.join('');
        
    } catch (error) {
        console.error('Error loading recent uploads:', error);
        if (uploadsList) {
            uploadsList.innerHTML = '<p style="color: #666; text-align: center; padding: 1rem;">No documents uploaded yet</p>';
        }
    }
}

// Refresh Uploads
function refreshUploads() {
    loadRecentUploads();
    showNotification('Uploads refreshed', 'success');
}

// View Document
function viewDocument(documentId) {
    // Open a dedicated viewer page to improve inline PDF reliability
    const url = `viewer.html?id=${encodeURIComponent(documentId)}`;
    openInNewTab(url);
}

function openInNewTab(url) {
    const a = document.createElement('a');
    a.href = url;
    a.target = '_blank';
    a.rel = 'noopener';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
}

// Delete Document
async function deleteDocument(documentId) {
    if (!confirm('Are you sure you want to delete this document? This action cannot be undone.')) {
        return;
    }
    
    try {
        const response = await fetch(`${window.API_BASE}/documents/${documentId}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            showNotification('Document deleted successfully', 'success');
            loadRecentUploads();
        } else {
            const msg = await response.text();
            throw new Error(msg || 'Failed to delete document');
        }
    } catch (error) {
        console.error('Error deleting document:', error);
        showNotification(`Failed to delete document: ${error.message}`, 'error');
    }
}

// Utility Functions
function getFileType(file) {
    const extension = file.name.toLowerCase().split('.').pop();
    switch (extension) {
        case 'pdf': return 'pdf';
        case 'doc':
        case 'docx': return 'doc';
        case 'ppt':
        case 'pptx': return 'ppt';
        case 'zip': return 'zip';
        default: return 'doc';
    }
}

function getFileTypeFromDocument(doc) {
    const filename = doc.originalFilename.toLowerCase();
    if (filename.includes('.pdf')) return 'pdf';
    if (filename.includes('.doc')) return 'doc';
    if (filename.includes('.ppt')) return 'ppt';
    if (filename.includes('.zip')) return 'zip';
    return 'doc';
}

function getFileIcon(fileType) {
    switch (fileType) {
        case 'pdf': return 'bx-file-pdf';
        case 'doc': return 'bx-file-doc';
        case 'ppt': return 'bx-file-ppt';
        case 'zip': return 'bx-archive';
        default: return 'bx-file';
    }
}

function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatDate(date) {
    return new Date(date).toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Show Notification
function showNotification(message, type = 'info') {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.innerHTML = `
        <div class="notification-content">
            <i class="bx ${getNotificationIcon(type)}"></i>
            <span>${message}</span>
        </div>
        <button class="notification-close">
            <i class="bx bx-x"></i>
        </button>
    `;
    
    // Add styles
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: white;
        padding: 1rem 1.5rem;
        border-radius: 12px;
        box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
        display: flex;
        align-items: center;
        gap: 0.75rem;
        z-index: 10000;
        transform: translateX(100%);
        transition: transform 0.3s ease;
        max-width: 400px;
    `;
    
    // Add to page
    document.body.appendChild(notification);
    
    // Animate in
    setTimeout(() => {
        notification.style.transform = 'translateX(0)';
    }, 100);
    
    // Auto remove after 5 seconds
    setTimeout(() => {
        notification.style.transform = 'translateX(100%)';
        setTimeout(() => {
            document.body.removeChild(notification);
        }, 300);
    }, 5000);
    
    // Close button
    const closeBtn = notification.querySelector('.notification-close');
    closeBtn.addEventListener('click', () => {
        notification.style.transform = 'translateX(100%)';
        setTimeout(() => {
            document.body.removeChild(notification);
        }, 300);
    });
}

// Get Notification Icon
function getNotificationIcon(type) {
    switch (type) {
        case 'success': return 'bx-check-circle';
        case 'error': return 'bx-error-circle';
        case 'warning': return 'bx-warning';
        default: return 'bx-info-circle';
    }
}



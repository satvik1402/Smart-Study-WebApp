// Dashboard JavaScript
// Use a single global for API base to avoid re-declaration across multiple scripts
if (!window.API_BASE) {
    window.API_BASE = '/api';
}

// DOM Elements
const menuToggle = document.getElementById('menuToggle');
const sidebar = document.querySelector('.sidebar');
const mainContent = document.querySelector('.main-content');

// Initialize Dashboard
document.addEventListener('DOMContentLoaded', function() {
    initializeDashboard();
    setupEventListeners();
    loadDashboardData();
});

// Setup Event Listeners
function setupEventListeners() {
    // Mobile menu toggle
    if (menuToggle) {
        menuToggle.addEventListener('click', toggleSidebar);
    }

    // Close sidebar when clicking outside on mobile
    document.addEventListener('click', function(e) {
        if (window.innerWidth <= 1024) {
            if (!sidebar.contains(e.target) && !menuToggle.contains(e.target)) {
                sidebar.classList.remove('active');
            }
        }
    });

    // Quick search functionality
    const quickSearch = document.querySelector('.search-box input');
    if (quickSearch) {
        quickSearch.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                const query = this.value.trim();
                if (query) {
                    window.location.href = `search.html?q=${encodeURIComponent(query)}`;
                }
            }
        });
    }
}

// Toggle Sidebar
function toggleSidebar() {
    sidebar.classList.toggle('active');
}

// Initialize Dashboard
function initializeDashboard() {
    // Add loading states
    addLoadingStates();
    
    // Initialize animations
    initializeAnimations();
}

// Add Loading States
function addLoadingStates() {
    const statCards = document.querySelectorAll('.stat-card');
    statCards.forEach(card => {
        card.classList.add('loading');
    });
}

// Initialize Animations
function initializeAnimations() {
    // Intersection Observer for scroll animations
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver(function(entries) {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = '1';
                entry.target.style.transform = 'translateY(0)';
            }
        });
    }, observerOptions);

    // Observe elements for animation
    const animatedElements = document.querySelectorAll('.stat-card, .action-card, .recent-activity, .study-progress');
    animatedElements.forEach(el => {
        el.style.opacity = '0';
        el.style.transform = 'translateY(30px)';
        el.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
        observer.observe(el);
    });
}

// Load Dashboard Data
async function loadDashboardData() {
    try {
        await Promise.all([
            loadDocumentStats(),
            loadRecentActivity(),
            updateStudyProgress()
        ]);
    } catch (error) {
        console.error('Error loading dashboard data:', error);
        showNotification('Error loading dashboard data', 'error');
    }
}

// Load Document Statistics
async function loadDocumentStats() {
    try {
        const response = await fetch(`${window.API_BASE}/documents`);
        if (!response.ok) throw new Error('Failed to fetch documents');
        
        const documents = await response.json();
        
        // Update stats
        updateStatCard('totalDocuments', documents.length);
        
        // Calculate total pages
        let totalPages = 0;
        for (const doc of documents) {
            if (doc.status === 'COMPLETED') {
                const contentResponse = await fetch(`${window.API_BASE}/documents/${doc.id}/content`);
                if (contentResponse.ok) {
                    const content = await contentResponse.json();
                    totalPages += content.length;
                }
            }
        }
        
        updateStatCard('totalPages', totalPages);
        
        // Remove loading states
        removeLoadingStates();
        
    } catch (error) {
        console.error('Error loading document stats:', error);
        updateStatCard('totalDocuments', 0);
        updateStatCard('totalPages', 0);
        removeLoadingStates();
    }
}

// Update Stat Card
function updateStatCard(elementId, value) {
    const element = document.getElementById(elementId);
    if (element) {
        // Animate number counting
        animateNumber(element, 0, value, 1000);
    }
}

// Animate Number Counting
function animateNumber(element, start, end, duration) {
    const startTime = performance.now();
    const difference = end - start;
    
    function updateNumber(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);
        
        // Easing function for smooth animation
        const easeOutQuart = 1 - Math.pow(1 - progress, 4);
        const current = Math.floor(start + (difference * easeOutQuart));
        
        element.textContent = current.toLocaleString();
        
        if (progress < 1) {
            requestAnimationFrame(updateNumber);
        }
    }
    
    requestAnimationFrame(updateNumber);
}

// Remove Loading States
function removeLoadingStates() {
    const statCards = document.querySelectorAll('.stat-card');
    statCards.forEach(card => {
        card.classList.remove('loading');
    });
}

// Load Recent Activity
async function loadRecentActivity() {
    try {
        const response = await fetch(`${API_BASE}/documents`);
        if (!response.ok) throw new Error('Failed to fetch documents');
        
        const documents = await response.json();
        const activityList = document.getElementById('recentActivity');
        
        if (!activityList) return;
        
        // Create activity items from recent documents
        const activities = documents.slice(0, 5).map(doc => {
            const activityType = getActivityType(doc);
            const timeAgo = getTimeAgo(new Date(doc.uploadDate));
            
            return `
                <div class="activity-item">
                    <div class="activity-icon ${activityType.icon}">
                        <i class="bx ${activityType.iconClass}"></i>
                    </div>
                    <div class="activity-content">
                        <h4>${activityType.title}</h4>
                        <p>${doc.originalFilename}</p>
                    </div>
                    <div class="activity-time">${timeAgo}</div>
                </div>
            `;
        });
        
        activityList.innerHTML = activities.join('');
        
    } catch (error) {
        console.error('Error loading recent activity:', error);
        const activityList = document.getElementById('recentActivity');
        if (activityList) {
            activityList.innerHTML = '<p style="color: #666; text-align: center; padding: 1rem;">No recent activity</p>';
        }
    }
}

// Get Activity Type
function getActivityType(document) {
    const status = document.status;
    const fileType = document.fileType?.toLowerCase();
    
    if (status === 'PROCESSING') {
        return {
            title: 'Document Processing',
            icon: 'upload',
            iconClass: 'bx-time'
        };
    } else if (status === 'COMPLETED') {
        if (fileType?.includes('pdf')) {
            return {
                title: 'PDF Uploaded',
                icon: 'upload',
                iconClass: 'bx-file-pdf'
            };
        } else if (fileType?.includes('doc')) {
            return {
                title: 'Document Uploaded',
                icon: 'upload',
                iconClass: 'bx-file-doc'
            };
        } else if (fileType?.includes('ppt')) {
            return {
                title: 'Presentation Uploaded',
                icon: 'upload',
                iconClass: 'bx-file-ppt'
            };
        } else {
            return {
                title: 'File Uploaded',
                icon: 'upload',
                iconClass: 'bx-file'
            };
        }
    } else {
        return {
            title: 'Upload Failed',
            icon: 'upload',
            iconClass: 'bx-error'
        };
    }
}

// Get Time Ago
function getTimeAgo(date) {
    const now = new Date();
    const diffInSeconds = Math.floor((now - date) / 1000);
    
    if (diffInSeconds < 60) {
        return 'Just now';
    } else if (diffInSeconds < 3600) {
        const minutes = Math.floor(diffInSeconds / 60);
        return `${minutes}m ago`;
    } else if (diffInSeconds < 86400) {
        const hours = Math.floor(diffInSeconds / 3600);
        return `${hours}h ago`;
    } else {
        const days = Math.floor(diffInSeconds / 86400);
        return `${days}d ago`;
    }
}

// Update Study Progress
function updateStudyProgress() {
    // This would typically fetch from a progress tracking API
    // For now, we'll use mock data
    const progressCards = document.querySelectorAll('.progress-card');
    
    progressCards.forEach(card => {
        const progressFill = card.querySelector('.progress-fill');
        if (progressFill) {
            const currentWidth = progressFill.style.width;
            const targetWidth = progressFill.style.width;
            
            // Animate progress bar
            progressFill.style.width = '0%';
            setTimeout(() => {
                progressFill.style.width = targetWidth;
            }, 500);
        }
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

// Utility Functions
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatDate(date) {
    return new Date(date).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Export functions for use in other scripts
window.dashboardUtils = {
    showNotification,
    formatFileSize,
    formatDate,
    API_BASE: window.API_BASE
};



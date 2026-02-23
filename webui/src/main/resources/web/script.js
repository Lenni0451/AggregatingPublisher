let artifactCount = 0;

document.addEventListener('DOMContentLoaded', () => {
    const path = window.location.pathname;

    if (path === '/login') {
        initLogin();
    } else {
        initDashboard();
    }
});

function initLogin() {
    const loginForm = document.getElementById('login-form');
    const errorMessage = document.getElementById('error-message');

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const formData = new FormData(loginForm);
        const data = Object.fromEntries(formData.entries());

        try {
            const response = await fetch('/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            });

            if (response.ok) {
                window.location.href = '/';
            } else {
                errorMessage.style.display = 'block';
                errorMessage.textContent = 'Invalid credentials';
            }
        } catch (error) {
            errorMessage.style.display = 'block';
            errorMessage.textContent = 'An error occurred';
            console.error(error);
        }
    });
}

async function initDashboard() {
    loadArtifacts();
    loadPublishers();

    // Poll artifacts every 5 seconds
    setInterval(loadArtifacts, 5000);
}

function showNotification(message, type = 'info') {
    const container = document.getElementById('notification-area');
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;

    const content = document.createElement('span');
    content.textContent = message;
    notification.appendChild(content);

    const closeBtn = document.createElement('span');
    closeBtn.className = 'notification-close';
    closeBtn.innerHTML = '&times;';
    closeBtn.onclick = () => notification.remove();
    notification.appendChild(closeBtn);

    container.appendChild(notification);
}

async function loadArtifacts() {
    const list = document.getElementById('artifacts-list');
    try {
        const artifacts = await fetchJson('/api/artifacts');
        artifactCount = artifacts.length;

        if (artifacts.length === 0) {
            list.innerHTML = '<li>No artifacts aggregated yet.</li>';
        } else {
            list.innerHTML = '';
            artifacts.forEach(artifact => {
                const li = document.createElement('li');
                li.textContent = artifact;
                list.appendChild(li);
            });
        }
        updatePublishButtons();
    } catch (error) {
        console.error('Failed to load artifacts', error);
    }
}

function updatePublishButtons() {
    const clearBtn = document.getElementById('clear-artifacts-btn');
    if (clearBtn) clearBtn.disabled = artifactCount === 0;

    const buttons = document.querySelectorAll('#publishers-list .publisher-card button');
    buttons.forEach(button => {
        if (!button.textContent.includes('%') && !button.textContent.includes('Starting')) {
            button.disabled = artifactCount === 0;
        }
    });
}

async function clearArtifacts() {
    if (!confirm('Are you sure you want to clear all aggregated artifacts?')) {
        return;
    }

    try {
        await fetchJson('/api/clear', { method: 'POST' });
        showNotification('Artifacts cleared successfully', 'success');
        loadArtifacts();
    } catch (error) {
        console.error('Failed to clear artifacts', error);
        showNotification('Failed to clear artifacts: ' + error.message, 'error');
    }
}

async function loadPublishers() {
    const container = document.getElementById('publishers-list');
    try {
        const publishers = await fetchJson('/api/publishers');

        if (publishers.length === 0) {
            container.innerHTML = '<p>No publishers configured.</p>';
            return;
        }

        container.innerHTML = '';
        publishers.forEach(name => {
            const card = document.createElement('div');
            card.className = 'publisher-card';

            const title = document.createElement('h3');
            title.textContent = name;
            card.appendChild(title);

            const button = document.createElement('button');
            button.textContent = 'Publish';
            button.onclick = () => startPublish(name, button);
            button.disabled = artifactCount === 0;
            card.appendChild(button);

            container.appendChild(card);
        });
    } catch (error) {
        console.error('Failed to load publishers', error);
        container.innerHTML = '<p>Error loading publishers.</p>';
    }
}

async function startPublish(publisherName, button) {
    if (button.disabled) return;

    const originalText = button.textContent;
    button.disabled = true;
    button.textContent = 'Starting...';

    try {
        const response = await fetchJson(`/api/publish/${encodeURIComponent(publisherName)}`, {
            method: 'POST'
        });

        const taskId = response.taskId;
        pollProgress(taskId, button, originalText, publisherName);

    } catch (error) {
        console.error('Publish failed', error);
        showNotification(`Failed to start publishing to ${publisherName}: ${error.message}`, 'error');
        button.textContent = originalText;
        updatePublishButtons();
    }
}

async function pollProgress(taskId, button, originalText, publisherName) {
    const interval = setInterval(async () => {
        try {
            const task = await fetchJson(`/api/progress/${taskId}`);

            if (task.completed) {
                clearInterval(interval);
                button.textContent = originalText;
                updatePublishButtons();

                if (task.success) {
                    showNotification(`Successfully published to ${publisherName}`, 'success');
                } else {
                    showNotification(`Failed to publish to ${publisherName}: ${task.error || 'Unknown error'}`, 'error');
                }
            } else {
                if (task.totalSteps <= 1) {
                    button.textContent = `${(task.progress * 100).toFixed(1)}%`;
                } else {
                     button.textContent = `${(task.progress * 100).toFixed(1)}% (${task.step}/${task.totalSteps})`;
                }
            }

        } catch (error) {
            console.error('Progress poll failed', error);
            clearInterval(interval);
            showNotification(`Error polling progress for ${publisherName}: ${error.message}`, 'error');
            button.textContent = originalText;
            updatePublishButtons();
        }
    }, 500);
}

function logout() {
    fetch('/api/logout').then(() => {
        window.location.href = '/login';
    });
}

async function fetchJson(url, options = {}) {
    const response = await fetch(url, options);
    if (response.status === 401) {
        window.location.href = '/login';
        throw new Error('Unauthorized');
    }
    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }
    return response.json();
}

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

async function loadArtifacts() {
    const list = document.getElementById('artifacts-list');
    try {
        const artifacts = await fetchJson('/api/artifacts');

        if (artifacts.length === 0) {
            list.innerHTML = '<li>No artifacts aggregated yet.</li>';
            return;
        }

        list.innerHTML = '';
        artifacts.forEach(artifact => {
            const li = document.createElement('li');
            li.textContent = artifact;
            list.appendChild(li);
        });
    } catch (error) {
        console.error('Failed to load artifacts', error);
        // list.innerHTML = '<li>Error loading artifacts.</li>';
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
        pollProgress(taskId, button, originalText);

    } catch (error) {
        console.error('Publish failed', error);
        button.textContent = 'Error';
        setTimeout(() => {
            button.disabled = false;
            button.textContent = originalText;
        }, 2000);
    }
}

async function pollProgress(taskId, button, originalText) {
    const interval = setInterval(async () => {
        try {
            const task = await fetchJson(`/api/progress/${taskId}`);

            if (task.completed) {
                clearInterval(interval);
                if (task.success) {
                    button.textContent = 'Done!';
                } else {
                    button.textContent = 'Failed: ' + (task.error || 'Unknown error');
                }
                setTimeout(() => {
                    button.disabled = false;
                    button.textContent = originalText;
                }, 3000);
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
            button.textContent = 'Error';
            setTimeout(() => {
                button.disabled = false;
                button.textContent = originalText;
            }, 2000);
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

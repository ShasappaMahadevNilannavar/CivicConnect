/**
 * CivicConnect Main Application JavaScript
 */

document.addEventListener('DOMContentLoaded', function () {
    initNotifications();
    initAlerts();
    initTextareaCounters();
    initImagePreviews();
});

/* ====================================================
   CSRF Header Helper for Fetch
   ==================================================== */
function getCsrfHeaders() {
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    const headers = {
        'Content-Type': 'application/json'
    };
    if (token && header) {
        headers[header] = token;
    }
    return headers;
}

/* ====================================================
   Notification Badge & Management
   ==================================================== */
function initNotifications() {
    updateNotificationBadge();
    // Poll every 30 seconds
    setInterval(updateNotificationBadge, 30000);
}

function updateNotificationBadge() {
    const badge = document.getElementById('notificationBadge');
    if (!badge) return;

    fetch('/api/notifications/count')
        .then(res => res.json())
        .then(data => {
            if (data.count > 0) {
                badge.textContent = data.count > 99 ? '99+' : data.count;
                badge.classList.remove('d-none');
            } else {
                badge.classList.add('d-none');
            }
        })
        .catch(err => console.debug('Notification poll error:', err));
}

function markNotificationAsRead(id, btnElement) {
    fetch('/api/notifications/' + id + '/read', {
        method: 'POST',
        headers: getCsrfHeaders()
    })
    .then(res => {
        if (res.ok) {
            const card = document.getElementById('notif-card-' + id);
            if (card) {
                card.classList.remove('unread');
                card.style.opacity = '0.7';
            }
            if (btnElement) btnElement.remove();
            updateNotificationBadge();
        }
    })
    .catch(err => console.error(err));
}

function markAllNotificationsRead() {
    fetch('/api/notifications/read-all', {
        method: 'POST',
        headers: getCsrfHeaders()
    })
    .then(res => {
        if (res.ok) {
            document.querySelectorAll('.notification-card').forEach(card => {
                card.classList.remove('unread');
                card.style.opacity = '0.7';
            });
            document.querySelectorAll('.mark-read-btn').forEach(b => b.remove());
            updateNotificationBadge();
        }
    });
}

/* ====================================================
   Alert Auto-Dismiss
   ==================================================== */
function initAlerts() {
    setTimeout(function () {
        document.querySelectorAll('.alert-dismissible').forEach(function (alert) {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            bsAlert.close();
        });
    }, 6000);
}

/* ====================================================
   Textarea Character Counter
   ==================================================== */
function initTextareaCounters() {
    document.querySelectorAll('textarea[maxlength]').forEach(textarea => {
        const max = textarea.getAttribute('maxlength');
        const counter = document.getElementById(textarea.id + 'Counter');
        if (counter) {
            const update = () => counter.textContent = `${textarea.value.length} / ${max}`;
            textarea.addEventListener('input', update);
            update();
        }
    });
}

/* ====================================================
   Image Preview & Validation
   ==================================================== */
function initImagePreviews() {
    const fileInputs = document.querySelectorAll('input[type="file"][data-preview-target]');
    fileInputs.forEach(input => {
        input.addEventListener('change', function (e) {
            const targetId = this.getAttribute('data-preview-target');
            const previewContainer = document.getElementById(targetId);
            if (!previewContainer) return;

            previewContainer.innerHTML = '';
            const files = Array.from(this.files);

            for (const file of files) {
                // Client-side file size validation (5MB max)
                if (file.size > 5 * 1024 * 1024) {
                    alert(`File ${file.name} exceeds maximum 5MB size.`);
                    this.value = '';
                    previewContainer.innerHTML = '';
                    return;
                }

                if (!file.type.startsWith('image/')) {
                    alert(`File ${file.name} is not a valid image.`);
                    this.value = '';
                    previewContainer.innerHTML = '';
                    return;
                }

                const reader = new FileReader();
                reader.onload = function (event) {
                    const col = document.createElement('div');
                    col.className = 'col-6 col-md-4 col-lg-3';
                    col.innerHTML = `
                        <div class="card h-100 shadow-sm border-0 position-relative">
                            <img src="${event.target.result}" class="card-img-top rounded preview-thumb" alt="Preview" style="height: 120px; object-fit: cover;">
                            <div class="card-body p-2 text-center">
                                <small class="text-truncate d-block text-muted" style="max-width: 100%;">${file.name}</small>
                                <span class="badge bg-secondary">${(file.size / 1024).toFixed(0)} KB</span>
                            </div>
                        </div>
                    `;
                    previewContainer.appendChild(col);
                };
                reader.readAsDataURL(file);
            }
        });
    });
}

/* ====================================================
   Copy Reference ID to Clipboard
   ==================================================== */
function copyRefId(refId) {
    navigator.clipboard.writeText(refId).then(() => {
        showToast('Reference ID copied: ' + refId, 'success');
    }).catch(() => {
        prompt('Copy reference ID:', refId);
    });
}

function showToast(message, type) {
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container position-fixed bottom-0 end-0 p-3';
        document.body.appendChild(container);
    }

    const toastId = 'toast-' + Date.now();
    const bgClass = type === 'success' ? 'bg-success text-white' : 'bg-primary text-white';
    const html = `
        <div id="${toastId}" class="toast align-items-center ${bgClass} border-0" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    <i class="fas fa-check-circle me-2"></i> ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
    `;
    container.insertAdjacentHTML('beforeend', html);
    const toastEl = document.getElementById(toastId);
    const bsToast = new bootstrap.Toast(toastEl, { delay: 3500 });
    bsToast.show();
    toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
}

/* ====================================================
   Leaflet Map Helper: Public Map
   ==================================================== */
function initCivicMap(containerId, filterCategory, filterStatus) {
    const mapElement = document.getElementById(containerId);
    if (!mapElement) return;

    // Default center: Central City Coordinates
    const defaultLat = 12.9716;
    const defaultLng = 77.5946;

    const map = L.map(containerId).setView([defaultLat, defaultLng], 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        maxZoom: 19
    }).addTo(map);

    let markersLayer = L.layerGroup().addTo(map);

    function loadMarkers(cat, stat) {
        markersLayer.clearLayers();
        let url = '/api/complaints/map';
        const params = new URLSearchParams();
        if (cat) params.append('category', cat);
        if (stat) params.append('status', stat);
        if (params.toString()) url += '?' + params.toString();

        fetch(url)
            .then(res => res.json())
            .then(complaints => {
                if (complaints.length === 0) return;

                const latLngs = [];

                complaints.forEach(c => {
                    if (c.latitude && c.longitude) {
                        const statusColors = {
                            'SUBMITTED': '#757575',
                            'UNDER_REVIEW': '#F57F17',
                            'APPROVED': '#1565C0',
                            'IN_PROGRESS': '#FF9800',
                            'RESOLVED_PENDING_VERIFICATION': '#7B1FA2',
                            'VERIFIED_RESOLVED': '#2E7D32',
                            'REJECTED': '#D32F2F'
                        };
                        const color = statusColors[c.status] || '#2E7D32';

                        const markerHtml = `
                            <div style="background-color: ${color}; width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; border: 2px solid white; box-shadow: 0 2px 6px rgba(0,0,0,0.3);">
                                <i class="${c.categoryIcon || 'fas fa-map-marker-alt'}" style="font-size: 14px;"></i>
                            </div>
                        `;

                        const customIcon = L.divIcon({
                            html: markerHtml,
                            className: 'custom-leaflet-marker',
                            iconSize: [32, 32],
                            iconAnchor: [16, 16]
                        });

                        const marker = L.marker([c.latitude, c.longitude], { icon: customIcon });

                        const popupHtml = `
                            <div class="p-2" style="min-width: 200px;">
                                <div class="d-flex justify-content-between align-items-center mb-1">
                                    <span class="badge bg-secondary">${c.referenceId}</span>
                                    <span class="badge" style="background-color: ${color}; color: white;">${c.status.replace(/_/g, ' ')}</span>
                                </div>
                                <h6 class="fw-bold mb-1">${c.title}</h6>
                                <p class="small text-muted mb-1"><i class="fas fa-tag me-1"></i>${c.category}</p>
                                <p class="small text-muted mb-2"><i class="fas fa-map-pin me-1"></i>${c.locationDescription || 'Location tagged'}</p>
                                <a href="/citizen/complaints/${c.referenceId}" class="btn btn-sm btn-outline-success w-100">View Report</a>
                            </div>
                        `;

                        marker.bindPopup(popupHtml);
                        markersLayer.addLayer(marker);
                        latLngs.push([c.latitude, c.longitude]);
                    }
                });

                if (latLngs.length > 0) {
                    map.fitBounds(L.latLngBounds(latLngs), { padding: [50, 50], maxZoom: 15 });
                }
            })
            .catch(err => console.error('Map loading error:', err));
    }

    loadMarkers(filterCategory, filterStatus);

    // Filter controls binding
    const filterCatSelect = document.getElementById('mapFilterCategory');
    const filterStatSelect = document.getElementById('mapFilterStatus');

    if (filterCatSelect && filterStatSelect) {
        const reload = () => loadMarkers(filterCatSelect.value, filterStatSelect.value);
        filterCatSelect.addEventListener('change', reload);
        filterStatSelect.addEventListener('change', reload);
    }
}

/* ====================================================
   Leaflet Map Helper: Location Picker
   ==================================================== */
function initLocationPicker(mapContainerId, latInputId, lngInputId, descInputId) {
    const mapElement = document.getElementById(mapContainerId);
    if (!mapElement) return;

    const defaultLat = 12.9716;
    const defaultLng = 77.5946;

    const map = L.map(mapContainerId).setView([defaultLat, defaultLng], 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);

    let marker;

    const latInput = document.getElementById(latInputId);
    const lngInput = document.getElementById(lngInputId);
    const descInput = document.getElementById(descInputId);

    // If initial coords exist
    if (latInput && latInput.value && lngInput && lngInput.value) {
        const initLat = parseFloat(latInput.value);
        const initLng = parseFloat(lngInput.value);
        marker = L.marker([initLat, initLng], { draggable: true }).addTo(map);
        map.setView([initLat, initLng], 15);
    }

    map.on('click', function (e) {
        const { lat, lng } = e.latlng;
        if (marker) {
            marker.setLatLng(e.latlng);
        } else {
            marker = L.marker(e.latlng, { draggable: true }).addTo(map);
            marker.on('dragend', function (ev) {
                const pos = ev.target.getLatLng();
                updateCoords(pos.lat, pos.lng);
            });
        }
        updateCoords(lat, lng);
    });

    function updateCoords(lat, lng) {
        if (latInput) latInput.value = lat.toFixed(6);
        if (lngInput) lngInput.value = lng.toFixed(6);

        // Reverse geocoding hint via Nominatim (approximate community locality)
        fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}`)
            .then(res => res.json())
            .then(data => {
                if (data && data.display_name && descInput && !descInput.value) {
                    descInput.value = data.display_name.split(',').slice(0, 3).join(',');
                }
            })
            .catch(() => {});
    }

    // Geolocation button if available
    const locateBtn = document.getElementById('useCurrentLocationBtn');
    if (locateBtn && navigator.geolocation) {
        locateBtn.addEventListener('click', function () {
            locateBtn.disabled = true;
            locateBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i> Locating...';
            navigator.geolocation.getCurrentPosition(
                function (pos) {
                    const userLat = pos.coords.latitude;
                    const userLng = pos.coords.longitude;
                    map.setView([userLat, userLng], 15);
                    if (marker) {
                        marker.setLatLng([userLat, userLng]);
                    } else {
                        marker = L.marker([userLat, userLng], { draggable: true }).addTo(map);
                    }
                    updateCoords(userLat, userLng);
                    locateBtn.disabled = false;
                    locateBtn.innerHTML = '<i class="fas fa-crosshairs me-1"></i> Use My Location';
                },
                function (err) {
                    alert('Unable to retrieve location: ' + err.message);
                    locateBtn.disabled = false;
                    locateBtn.innerHTML = '<i class="fas fa-crosshairs me-1"></i> Use My Location';
                }
            );
        });
    }
}

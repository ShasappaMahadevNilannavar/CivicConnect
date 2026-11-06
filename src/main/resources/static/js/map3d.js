/**
 * CivicConnect - Google Maps 3D Civic Explorer Engine
 * Powered by WebGL 3D, Photorealistic Google Satellite Hybrid & 3D Building Extrusions
 */

(function () {
    'use strict';

    // Global Map Instance & State
    let map;
    let currentPitch = 55;
    let is3DActive = true;
    let activeCategory = 'ALL';
    let allComplaints = [];
    let complaintMarkers = [];
    let currentLayer = 'hybrid';

    // Google Maps Tile Sources
    const GOOGLE_TILE_SOURCES = {
        hybrid: {
            type: 'raster',
            tiles: [
                'https://mt0.google.com/vt/lyrs=y&x={x}&y={y}&z={z}',
                'https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}',
                'https://mt2.google.com/vt/lyrs=y&x={x}&y={y}&z={z}',
                'https://mt3.google.com/vt/lyrs=y&x={x}&y={y}&z={z}'
            ],
            tileSize: 256,
            attribution: '&copy; Google Maps'
        },
        satellite: {
            type: 'raster',
            tiles: [
                'https://mt0.google.com/vt/lyrs=s&x={x}&y={y}&z={z}',
                'https://mt1.google.com/vt/lyrs=s&x={x}&y={y}&z={z}',
                'https://mt2.google.com/vt/lyrs=s&x={x}&y={y}&z={z}'
            ],
            tileSize: 256,
            attribution: '&copy; Google Satellite'
        },
        streets: {
            type: 'raster',
            tiles: [
                'https://mt0.google.com/vt/lyrs=m&x={x}&y={y}&z={z}',
                'https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}',
                'https://mt2.google.com/vt/lyrs=m&x={x}&y={y}&z={z}'
            ],
            tileSize: 256,
            attribution: '&copy; Google Maps'
        },
        terrain: {
            type: 'raster',
            tiles: [
                'https://mt0.google.com/vt/lyrs=p&x={x}&y={y}&z={z}',
                'https://mt1.google.com/vt/lyrs=p&x={x}&y={y}&z={z}'
            ],
            tileSize: 256,
            attribution: '&copy; Google Terrain'
        }
    };

    // Major Indian Cities Coordinates & 3D Camera Settings
    const INDIAN_CITIES = {
        delhi: { name: 'New Delhi', coords: [77.2090, 28.6139], zoom: 14.8, pitch: 62, bearing: -25 },
        mumbai: { name: 'Mumbai', coords: [72.8777, 19.0760], zoom: 14.5, pitch: 60, bearing: 30 },
        bengaluru: { name: 'Bengaluru', coords: [77.5946, 12.9716], zoom: 15.0, pitch: 60, bearing: -15 },
        hyderabad: { name: 'Hyderabad', coords: [78.4867, 17.3850], zoom: 14.8, pitch: 58, bearing: 20 },
        chennai: { name: 'Chennai', coords: [80.2707, 13.0827], zoom: 14.5, pitch: 55, bearing: -10 },
        kolkata: { name: 'Kolkata', coords: [88.3639, 22.5726], zoom: 14.6, pitch: 58, bearing: 15 },
        pune: { name: 'Pune', coords: [73.8567, 18.5204], zoom: 14.8, pitch: 60, bearing: -20 },
        ahmedabad: { name: 'Ahmedabad', coords: [72.5714, 23.0225], zoom: 14.7, pitch: 55, bearing: 25 },
        jaipur: { name: 'Jaipur', coords: [75.7873, 26.9124], zoom: 15.0, pitch: 60, bearing: -30 },
        varanasi: { name: 'Varanasi', coords: [82.9739, 25.3176], zoom: 15.2, pitch: 62, bearing: 45 }
    };

    // State & Union Territory Centroids for Search
    const INDIA_REGIONS = [
        { name: "Andhra Pradesh", type: "State", coords: [79.7400, 15.9129] },
        { name: "Arunachal Pradesh", type: "State", coords: [94.7278, 28.2180] },
        { name: "Assam", type: "State", coords: [92.9376, 26.2006] },
        { name: "Bihar", type: "State", coords: [85.3131, 25.0961] },
        { name: "Chhattisgarh", type: "State", coords: [81.8661, 21.2787] },
        { name: "Goa", type: "State", coords: [74.1240, 15.2993] },
        { name: "Gujarat", type: "State", coords: [71.1924, 22.2587] },
        { name: "Haryana", type: "State", coords: [76.0856, 29.0588] },
        { name: "Himachal Pradesh", type: "State", coords: [77.1734, 31.1048] },
        { name: "Jharkhand", type: "State", coords: [85.2799, 23.6102] },
        { name: "Karnataka", type: "State", coords: [75.7139, 15.3173] },
        { name: "Kerala", type: "State", coords: [76.2711, 10.8505] },
        { name: "Madhya Pradesh", type: "State", coords: [77.4126, 22.9734] },
        { name: "Maharashtra", type: "State", coords: [75.7139, 19.7515] },
        { name: "Manipur", type: "State", coords: [93.9063, 24.6637] },
        { name: "Meghalaya", type: "State", coords: [91.3662, 25.4670] },
        { name: "Mizoram", type: "State", coords: [92.9376, 23.1645] },
        { name: "Nagaland", type: "State", coords: [94.5624, 26.1584] },
        { name: "Odisha", type: "State", coords: [85.0985, 20.9517] },
        { name: "Punjab", type: "State", coords: [75.3412, 31.1471] },
        { name: "Rajasthan", type: "State", coords: [74.2179, 27.0238] },
        { name: "Sikkim", type: "State", coords: [88.5122, 27.5330] },
        { name: "Tamil Nadu", type: "State", coords: [78.6569, 11.1271] },
        { name: "Telangana", type: "State", coords: [79.0193, 18.1124] },
        { name: "Tripura", type: "State", coords: [91.9882, 23.9408] },
        { name: "Uttar Pradesh", type: "State", coords: [80.9462, 26.8467] },
        { name: "Uttarakhand", type: "State", coords: [79.0193, 30.0668] },
        { name: "West Bengal", type: "State", coords: [87.8550, 22.9868] },
        { name: "Jammu and Kashmir", type: "UT", coords: [74.7973, 33.7782] },
        { name: "Ladakh", type: "UT", coords: [77.5771, 34.1526] },
        { name: "Delhi", type: "UT", coords: [77.1025, 28.7041] },
        { name: "Chandigarh", type: "UT", coords: [76.7794, 30.7333] },
        { name: "Puducherry", type: "UT", coords: [79.8083, 11.9416] },
        { name: "Andaman and Nicobar Islands", type: "UT", coords: [92.6586, 11.7401] }
    ];

    document.addEventListener('DOMContentLoaded', function () {
        initGoogleMap3D();
        bindUIControls();
        fetchLiveComplaints();
    });

    function initGoogleMap3D() {
        const container = document.getElementById('gmap3dCanvas');
        if (!container) return;

        // Base MapLibre GL Style using Google Hybrid 3D Tiles
        const mapStyle = {
            version: 8,
            sources: {
                'google-tiles': GOOGLE_TILE_SOURCES.hybrid
            },
            layers: [
                {
                    id: 'google-tiles-layer',
                    type: 'raster',
                    source: 'google-tiles',
                    minzoom: 0,
                    maxzoom: 22
                }
            ]
        };

        map = new maplibregl.Map({
            container: 'gmap3dCanvas',
            style: mapStyle,
            center: [78.9629, 20.5937], // Center of India
            zoom: 5.2,
            pitch: currentPitch, // 55° 3D perspective
            bearing: -15, // Slight rotation for realistic 3D angle
            maxPitch: 85,
            attributionControl: false
        });

        // Add 3D Extruded Buildings Layer once style is loaded
        map.on('load', function () {
            addExtrudedBuildingsLayer();
        });

        // Sync Compass and 3D UI states when camera moves
        map.on('rotate', updateCompass);
        map.on('pitch', updatePitchUI);
    }

    /**
     * Realistic 3D Building Extrusions Layer
     */
    function addExtrudedBuildingsLayer() {
        // Add OpenMapTiles 3D Buildings Source if available
        try {
            map.addSource('openmaptiles-buildings', {
                type: 'vector',
                url: 'https://tiles.openfreemap.org/planet'
            });

            map.addLayer({
                'id': '3d-buildings-extrusion',
                'source': 'openmaptiles-buildings',
                'source-layer': 'building',
                'type': 'fill-extrusion',
                'minzoom': 14,
                'paint': {
                    'fill-extrusion-color': [
                        'interpolate',
                        ['linear'],
                        ['get', 'render_height'],
                        0, '#d1d5db',
                        30, '#9ca3af',
                        70, '#60a5fa',
                        150, '#3b82f6'
                    ],
                    'fill-extrusion-height': [
                        'interpolate',
                        ['linear'],
                        ['zoom'],
                        14, 0,
                        14.5, ['get', 'render_height']
                    ],
                    'fill-extrusion-base': ['get', 'render_min_height'],
                    'fill-extrusion-opacity': 0.75
                }
            });
        } catch (e) {
            console.debug('3D vector building source fallback active:', e);
        }
    }

    /**
     * Switch Google Map Layer (Hybrid, Satellite, Roadmap, Terrain)
     */
    function switchGoogleLayer(layerKey) {
        if (!map || !GOOGLE_TILE_SOURCES[layerKey]) return;
        currentLayer = layerKey;

        // Remove old tile layer
        if (map.getLayer('google-tiles-layer')) {
            map.removeLayer('google-tiles-layer');
        }
        if (map.getSource('google-tiles')) {
            map.removeSource('google-tiles');
        }

        // Add selected Google source and layer
        map.addSource('google-tiles', GOOGLE_TILE_SOURCES[layerKey]);
        map.addLayer({
            id: 'google-tiles-layer',
            type: 'raster',
            source: 'google-tiles',
            minzoom: 0,
            maxzoom: 22
        }, map.getLayer('3d-buildings-extrusion') ? '3d-buildings-extrusion' : undefined);

        // Update UI thumbnail & label
        const labels = {
            hybrid: 'Google Hybrid 3D',
            satellite: 'Google Satellite',
            streets: 'Google Roadmap',
            terrain: 'Google Terrain'
        };
        document.getElementById('currentLayerLabel').textContent = labels[layerKey] || 'Satellite 3D';

        document.querySelectorAll('.gmap-layer-opt').forEach(opt => {
            opt.classList.toggle('active', opt.dataset.layer === layerKey);
        });

        document.getElementById('gmapLayerMenu').classList.remove('show');
    }

    /**
     * Toggle 3D / 2D Camera Mode (Iconic Google Maps "3D" Button)
     */
    function toggle3DMode() {
        if (!map) return;
        const btn = document.getElementById('gmap3dToggleBtn');
        const label = document.getElementById('label3d');

        if (is3DActive) {
            // Switch to Flat 2D View
            map.easeTo({
                pitch: 0,
                bearing: 0,
                duration: 900
            });
            is3DActive = false;
            btn.classList.remove('active-3d');
            label.textContent = '2D';
            btn.title = 'Switch to 3D Perspective Tilt';
        } else {
            // Switch to 60° 3D Bird's-Eye Perspective
            map.easeTo({
                pitch: 62,
                bearing: -20,
                duration: 900
            });
            is3DActive = true;
            btn.classList.add('active-3d');
            label.textContent = '3D';
            btn.title = 'Switch to Flat 2D View';
        }
    }

    function updateCompass() {
        const bearing = map.getBearing();
        const icon = document.getElementById('compassIcon');
        if (icon) {
            icon.style.transform = `rotate(${-bearing}deg)`;
        }
    }

    function updatePitchUI() {
        const pitch = map.getPitch();
        const btn = document.getElementById('gmap3dToggleBtn');
        const label = document.getElementById('label3d');
        if (pitch > 25) {
            is3DActive = true;
            btn.classList.add('active-3d');
            label.textContent = '3D';
        } else {
            is3DActive = false;
            btn.classList.remove('active-3d');
            label.textContent = '2D';
        }
    }

    /**
     * Fetch Live Civic Complaints and Display 3D Teardrop Pins
     */
    function fetchLiveComplaints() {
        fetch('/api/complaints/map')
            .then(res => res.json())
            .then(data => {
                allComplaints = data || [];
                renderMarkers(activeCategory);
                const countAllEl = document.getElementById('countAll');
                if (countAllEl) countAllEl.textContent = allComplaints.length;
            })
            .catch(err => {
                console.error('Error fetching complaints for 3D map:', err);
                // Fallback demo markers for major cities if network is empty
                seedFallbackDemoComplaints();
            });
    }

    function seedFallbackDemoComplaints() {
        allComplaints = [
            {
                referenceId: 'CIV-2026-DEL',
                title: 'Deep Road Pothole near Connaught Place',
                category: 'Road Damage',
                categoryIcon: 'fas fa-road',
                status: 'IN_PROGRESS',
                priority: 'HIGH',
                latitude: 28.6315,
                longitude: 77.2167,
                locationDescription: 'Connaught Place Outer Circle, New Delhi',
                createdAt: '2026-11-04'
            },
            {
                referenceId: 'CIV-2026-BLR',
                title: 'Broken LED Streetlights on Outer Ring Road',
                category: 'Streetlight',
                categoryIcon: 'fas fa-lightbulb',
                status: 'SUBMITTED',
                priority: 'MEDIUM',
                latitude: 12.9352,
                longitude: 77.6245,
                locationDescription: 'Koramangala 4th Block, Bengaluru',
                createdAt: '2026-11-05'
            },
            {
                referenceId: 'CIV-2026-MUM',
                title: 'Overflowing Municipal Garbage Bin',
                category: 'Garbage Overflow',
                categoryIcon: 'fas fa-trash-alt',
                status: 'VERIFIED_RESOLVED',
                priority: 'LOW',
                latitude: 19.0596,
                longitude: 72.8295,
                locationDescription: 'Bandra West, Mumbai',
                createdAt: '2026-11-02'
            },
            {
                referenceId: 'CIV-2026-HYD',
                title: 'Major Water Pipeline Leakage',
                category: 'Water Leakage',
                categoryIcon: 'fas fa-tint',
                status: 'APPROVED',
                priority: 'HIGH',
                latitude: 17.4399,
                longitude: 78.3808,
                locationDescription: 'HITEC City Cyber Towers, Hyderabad',
                createdAt: '2026-11-03'
            }
        ];
        renderMarkers(activeCategory);
    }

    function renderMarkers(categoryFilter) {
        // Clear existing markers
        complaintMarkers.forEach(m => m.remove());
        complaintMarkers = [];

        const filtered = categoryFilter === 'ALL'
            ? allComplaints
            : allComplaints.filter(c => c.category && c.category.toLowerCase().includes(categoryFilter.toLowerCase()));

        filtered.forEach(c => {
            if (!c.latitude || !c.longitude) return;

            const pinEl = createPinElement(c);

            // Google Maps Popup
            const popupHtml = `
                <div style="font-family: inherit; min-width: 210px;">
                    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;">
                        <span style="font-size:0.75rem; font-weight:700; color:#5f6368;">${c.referenceId || 'CIV-ID'}</span>
                        <span style="font-size:0.7rem; font-weight:700; padding:2px 8px; border-radius:10px; background:${getStatusBg(c.status)}; color:#fff;">
                            ${(c.status || 'SUBMITTED').replace(/_/g, ' ')}
                        </span>
                    </div>
                    <div style="font-weight:700; font-size:0.92rem; color:#202124; margin-bottom:4px; line-height:1.3;">
                        ${c.title}
                    </div>
                    <div style="font-size:0.78rem; color:#5f6368; margin-bottom:8px;">
                        <i class="fas fa-map-marker-alt" style="color:#d93025; margin-right:4px;"></i>
                        ${c.locationDescription || 'Location tagged'}
                    </div>
                    <button class="btn btn-sm btn-outline-success w-100 py-1" style="font-size:0.8rem; font-weight:600; border-radius:14px;" onclick="window.viewComplaintDetails('${c.referenceId}')">
                        Inspect in 3D Card &rarr;
                    </button>
                </div>
            `;

            const popup = new maplibregl.Popup({ offset: [0, -38], closeButton: true })
                .setHTML(popupHtml);

            const marker = new maplibregl.Marker({ element: pinEl, anchor: 'bottom' })
                .setLngLat([c.longitude, c.latitude])
                .setPopup(popup)
                .addTo(map);

            pinEl.addEventListener('click', () => {
                showComplaintSideCard(c);
            });

            complaintMarkers.push(marker);
        });
    }

    function createPinElement(complaint) {
        const pinContainer = document.createElement('div');
        pinContainer.className = 'civic-3d-pin';

        const color = getStatusColor(complaint.status);
        const iconClass = complaint.categoryIcon || getCategoryIcon(complaint.category);

        pinContainer.innerHTML = `
            <div class="pin-radar" style="background:${color}40;"></div>
            <svg viewBox="0 0 36 48" width="36" height="48" style="filter: drop-shadow(0 4px 8px rgba(0,0,0,0.35));">
                <path d="M18 0 C8.06 0 0 8.06 0 18 C0 31.5 18 48 18 48 C18 48 36 31.5 36 18 C36 8.06 27.94 0 18 0 Z" fill="${color}"/>
                <circle cx="18" cy="18" r="13" fill="#ffffff"/>
            </svg>
            <div style="position:absolute; top:9px; left:50%; transform:translateX(-50%); color:${color}; font-size:14px;">
                <i class="${iconClass}"></i>
            </div>
        `;

        return pinContainer;
    }

    function getStatusColor(status) {
        switch (status) {
            case 'SUBMITTED': return '#757575';
            case 'UNDER_REVIEW': return '#f57f17';
            case 'APPROVED': return '#1a73e8';
            case 'IN_PROGRESS': return '#e65100';
            case 'RESOLVED_PENDING_VERIFICATION': return '#7b1fa2';
            case 'VERIFIED_RESOLVED': return '#2e7d32';
            default: return '#d93025';
        }
    }

    function getStatusBg(status) {
        return getStatusColor(status);
    }

    function getCategoryIcon(category) {
        if (!category) return 'fas fa-exclamation-triangle';
        const cat = category.toLowerCase();
        if (cat.includes('road') || cat.includes('pothole')) return 'fas fa-road';
        if (cat.includes('light')) return 'fas fa-lightbulb';
        if (cat.includes('garbage') || cat.includes('waste')) return 'fas fa-trash-alt';
        if (cat.includes('water')) return 'fas fa-tint';
        if (cat.includes('drain')) return 'fas fa-water';
        return 'fas fa-map-marker-alt';
    }

    /**
     * Show Problem Details in Google Maps-Style Slide Card
     */
    function showComplaintSideCard(c) {
        const card = document.getElementById('gmapDetailsCard');
        if (!card) return;

        document.getElementById('detailRefId').textContent = c.referenceId || 'CIV-ID';
        document.getElementById('detailStatusBadge').textContent = (c.status || 'SUBMITTED').replace(/_/g, ' ');
        document.getElementById('detailStatusBadge').style.backgroundColor = getStatusColor(c.status);
        document.getElementById('detailCategory').textContent = c.category || 'General Civic';
        document.getElementById('detailTitle').textContent = c.title;
        document.getElementById('detailDescription').textContent = c.description || 'Community reported municipal concern under review by local administration.';
        document.getElementById('detailLocation').textContent = c.locationDescription || 'Location tagged in municipality';
        document.getElementById('detailCoords').textContent = `${c.latitude?.toFixed(4)}° N, ${c.longitude?.toFixed(4)}° E`;
        document.getElementById('detailDate').textContent = c.createdAt ? new Date(c.createdAt).toLocaleDateString('en-IN', { month: 'short', day: 'numeric', year: 'numeric' }) : 'Recent';
        document.getElementById('detailPriority').textContent = c.priority || 'MEDIUM';

        const btn = document.getElementById('detailViewReportBtn');
        btn.href = `/citizen/complaints/${c.referenceId}`;

        card.classList.add('show');

        // Center map slightly left so card doesn't occlude marker
        map.flyTo({
            center: [c.longitude, c.latitude],
            zoom: 16.5,
            pitch: 62,
            bearing: -15,
            duration: 1500
        });
    }

    window.viewComplaintDetails = function (refId) {
        const comp = allComplaints.find(c => c.referenceId === refId);
        if (comp) showComplaintSideCard(comp);
    };

    /**
     * Bind UI Buttons, Dropdowns & Google Search
     */
    function bindUIControls() {
        // 3D / 2D Toggle
        document.getElementById('gmap3dToggleBtn')?.addEventListener('click', toggle3DMode);

        // Compass Reset
        document.getElementById('gmapCompassBtn')?.addEventListener('click', () => {
            map.easeTo({ bearing: 0, duration: 600 });
        });

        // Zoom Controls
        document.getElementById('gmapZoomInBtn')?.addEventListener('click', () => map.zoomIn({ duration: 400 }));
        document.getElementById('gmapZoomOutBtn')?.addEventListener('click', () => map.zoomOut({ duration: 400 }));

        // Tilt Controls
        document.getElementById('gmapTiltUpBtn')?.addEventListener('click', () => {
            const newPitch = Math.min(map.getPitch() + 15, 80);
            map.easeTo({ pitch: newPitch, duration: 400 });
        });
        document.getElementById('gmapTiltDownBtn')?.addEventListener('click', () => {
            const newPitch = Math.max(map.getPitch() - 15, 0);
            map.easeTo({ pitch: newPitch, duration: 400 });
        });

        // Locate Me
        document.getElementById('gmapLocateBtn')?.addEventListener('click', () => {
            if (navigator.geolocation) {
                navigator.geolocation.getCurrentPosition(pos => {
                    map.flyTo({
                        center: [pos.coords.longitude, pos.coords.latitude],
                        zoom: 16,
                        pitch: 60,
                        duration: 2000
                    });
                }, () => {
                    // Default to New Delhi
                    flyToCity('delhi');
                });
            } else {
                flyToCity('delhi');
            }
        });

        // Fullscreen
        document.getElementById('gmapFullscreenBtn')?.addEventListener('click', () => {
            const wrapper = document.querySelector('.gmap3d-wrapper');
            if (!document.fullscreenElement) {
                wrapper.requestFullscreen().catch(err => console.debug(err));
            } else {
                document.exitFullscreen();
            }
        });

        // Close Side Details Card
        document.getElementById('closeDetailsCardBtn')?.addEventListener('click', () => {
            document.getElementById('gmapDetailsCard').classList.remove('show');
        });

        // Layer Selector Toggle
        const layerBtn = document.getElementById('gmapLayerBtn');
        const layerMenu = document.getElementById('gmapLayerMenu');
        layerBtn?.addEventListener('click', (e) => {
            e.stopPropagation();
            layerMenu.classList.toggle('show');
        });

        document.querySelectorAll('.gmap-layer-opt').forEach(opt => {
            opt.addEventListener('click', (e) => {
                e.stopPropagation();
                const lk = opt.dataset.layer;
                switchGoogleLayer(lk);
            });
        });

        document.addEventListener('click', (e) => {
            if (!layerBtn.contains(e.target)) {
                layerMenu?.classList.remove('show');
            }
        });

        // Category Filter Chips
        document.querySelectorAll('.gmap-chip').forEach(chip => {
            chip.addEventListener('click', function () {
                document.querySelectorAll('.gmap-chip').forEach(c => c.classList.remove('active'));
                this.classList.add('active');
                activeCategory = this.dataset.category;
                renderMarkers(activeCategory);
            });
        });

        // Quick City Fly-to
        document.querySelectorAll('.city-pill').forEach(pill => {
            pill.addEventListener('click', function () {
                const cityKey = this.dataset.city;
                flyToCity(cityKey);
            });
        });

        // Google Maps Search Input & Dropdown
        setupGoogleSearch();
    }

    function flyToCity(cityKey) {
        const city = INDIAN_CITIES[cityKey];
        if (!city || !map) return;

        map.flyTo({
            center: city.coords,
            zoom: city.zoom,
            pitch: city.pitch,
            bearing: city.bearing,
            duration: 2500,
            essential: true
        });
    }

    function setupGoogleSearch() {
        const input = document.getElementById('gmapSearchInput');
        const clearBtn = document.getElementById('gmapSearchClearBtn');
        const dropdown = document.getElementById('gmapSearchDropdown');
        if (!input || !dropdown) return;

        input.addEventListener('input', function () {
            const query = this.value.trim().toLowerCase();
            if (clearBtn) clearBtn.classList.toggle('d-none', query.length === 0);

            if (query.length === 0) {
                dropdown.style.display = 'none';
                dropdown.innerHTML = '';
                return;
            }

            // Search in Cities, States, and Complaints
            const matches = [];

            // 1. Indian Cities
            Object.values(INDIAN_CITIES).forEach(c => {
                if (c.name.toLowerCase().includes(query)) {
                    matches.push({ title: c.name, type: 'Major City, India', icon: 'fas fa-city text-primary', coords: c.coords, zoom: c.zoom, pitch: c.pitch, bearing: c.bearing });
                }
            });

            // 2. States & UTs
            INDIA_REGIONS.forEach(r => {
                if (r.name.toLowerCase().includes(query)) {
                    matches.push({ title: r.name, type: `${r.type} of India`, icon: 'fas fa-landmark text-success', coords: r.coords, zoom: 8, pitch: 45, bearing: 0 });
                }
            });

            // 3. Complaints
            allComplaints.forEach(c => {
                if (c.title && c.title.toLowerCase().includes(query) || (c.referenceId && c.referenceId.toLowerCase().includes(query))) {
                    matches.push({ title: c.title, type: `Civic Report (${c.referenceId})`, icon: 'fas fa-map-marker-alt text-danger', coords: [c.longitude, c.latitude], zoom: 17, pitch: 62, bearing: -15, complaint: c });
                }
            });

            if (matches.length === 0) {
                dropdown.innerHTML = `<div class="p-3 text-muted text-center small">No matching city, state, or civic issue found</div>`;
                dropdown.style.display = 'block';
                return;
            }

            dropdown.innerHTML = matches.slice(0, 8).map(m => `
                <div class="gmap-search-item" data-coords="${m.coords.join(',')}" data-zoom="${m.zoom}" data-pitch="${m.pitch}" data-bearing="${m.bearing}">
                    <i class="${m.icon} fa-lg"></i>
                    <div>
                        <div class="fw-bold small text-dark">${m.title}</div>
                        <small class="text-muted" style="font-size:0.75rem;">${m.type}</small>
                    </div>
                </div>
            `).join('');

            dropdown.style.display = 'block';

            // Click handling
            dropdown.querySelectorAll('.gmap-search-item').forEach(item => {
                item.addEventListener('click', function () {
                    const coords = this.dataset.coords.split(',').map(Number);
                    const zoom = Number(this.dataset.zoom);
                    const pitch = Number(this.dataset.pitch);
                    const bearing = Number(this.dataset.bearing);

                    map.flyTo({
                        center: coords,
                        zoom: zoom || 14,
                        pitch: pitch || 60,
                        bearing: bearing || 0,
                        duration: 2200
                    });

                    dropdown.style.display = 'none';
                });
            });
        });

        clearBtn?.addEventListener('click', () => {
            input.value = '';
            clearBtn.classList.add('d-none');
            dropdown.style.display = 'none';
        });

        document.addEventListener('click', (e) => {
            if (!input.contains(e.target) && !dropdown.contains(e.target)) {
                dropdown.style.display = 'none';
            }
        });
    }

})();

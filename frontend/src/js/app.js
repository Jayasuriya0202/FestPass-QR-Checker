/**
 * FestPass Main Application Logic
 * Pure JavaScript handling DOM interactions, form submissions, and state
 */

import { api } from './api.js';

// Application State
const state = {
  currentTab: 'dashboard',
  events: [],
  selectedEventId: null,
};

// ==========================================
// 1. UTILITIES & ALERTS
// ==========================================

function showAlert(type, message, containerId = 'global-alert-container') {
  const container = document.getElementById(containerId);
  if (!container) return;

  const icons = {
    success: '✓',
    danger: '✕',
    info: 'ℹ',
  };

  const alertDiv = document.createElement('div');
  alertDiv.className = `alert alert-${type}`;
  alertDiv.innerHTML = `
    <span class="alert-icon">${icons[type] || '•'}</span>
    <div class="alert-content">${escapeHtml(message)}</div>
    <button class="alert-close" aria-label="Close">&times;</button>
  `;

  alertDiv.querySelector('.alert-close').addEventListener('click', () => {
    alertDiv.remove();
  });

  // Clear previous alerts in this container
  container.innerHTML = '';
  container.appendChild(alertDiv);

  // Auto-dismiss success alerts after 6 seconds
  if (type === 'success') {
    setTimeout(() => {
      if (alertDiv.parentNode) alertDiv.remove();
    }, 6000);
  }
}

function clearAlert(containerId = 'global-alert-container') {
  const container = document.getElementById(containerId);
  if (container) container.innerHTML = '';
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function formatIsoDate(dateString) {
  if (!dateString) return '';
  // Ensure yyyy-MM-dd'T'HH:mm:ss format
  return dateString.length === 16 ? `${dateString}:00` : dateString;
}

// ==========================================
// 2. TAB NAVIGATION
// ==========================================

function switchTab(tabId) {
  state.currentTab = tabId;

  // Update navbar button classes
  document.querySelectorAll('.nav-btn').forEach((btn) => {
    if (btn.dataset.tab === tabId) {
      btn.classList.add('active');
    } else {
      btn.classList.remove('active');
    }
  });

  // Update visible section
  document.querySelectorAll('.page-section').forEach((sec) => {
    sec.classList.remove('active');
  });

  const activeSection = document.getElementById(`section-${tabId}`);
  if (activeSection) {
    activeSection.classList.add('active');
  }

  clearAlert();

  // Load section-specific data
  if (tabId === 'dashboard') {
    loadDashboard();
  } else if (tabId === 'events') {
    loadEvents();
  } else if (tabId === 'issue-ticket') {
    loadIssueTicketView();
  } else if (tabId === 'attendance') {
    loadAttendanceView();
  }
}

// ==========================================
// 3. DASHBOARD LOGIC
// ==========================================

async function loadDashboard() {
  const loadingEl = document.getElementById('dashboard-events-loading');
  const emptyEl = document.getElementById('dashboard-events-empty');
  const tableContainer = document.getElementById('dashboard-events-table-container');
  const tbody = document.getElementById('dashboard-events-tbody');

  loadingEl.style.display = 'flex';
  emptyEl.style.display = 'none';
  tableContainer.style.display = 'none';

  try {
    const events = await api.getAllEvents();
    state.events = events || [];

    // Calculate Dashboard Statistics
    let totalIssued = 0;
    let totalCap = 0;
    state.events.forEach((evt) => {
      totalIssued += Number(evt.issuedTicketsCount || 0);
      totalCap += Number(evt.capacity || 0);
    });

    document.getElementById('dash-total-events').textContent = state.events.length;
    document.getElementById('dash-tickets-issued').textContent = totalIssued;
    document.getElementById('dash-available-capacity').textContent = Math.max(0, totalCap - totalIssued);

    // Sum live checked-in attendees from attendance endpoints
    let checkedInTotal = 0;
    if (state.events.length > 0) {
      const attendanceResults = await Promise.allSettled(
        state.events.map((e) => api.getEventAttendance(e.id))
      );
      attendanceResults.forEach((res) => {
        if (res.status === 'fulfilled' && res.value) {
          checkedInTotal += Number(res.value.checkedInHeadcount || 0);
        }
      });
    }
    document.getElementById('dash-checked-in').textContent = checkedInTotal;

    // Render Events Table
    loadingEl.style.display = 'none';
    if (state.events.length === 0) {
      emptyEl.style.display = 'block';
    } else {
      tbody.innerHTML = '';
      state.events.forEach((evt) => {
        const tr = document.createElement('tr');
        const isSoldOut = evt.issuedTicketsCount >= evt.capacity;
        const formattedDate = evt.eventDate ? new Date(evt.eventDate).toLocaleString() : 'N/A';

        tr.innerHTML = `
          <td style="font-weight: 600;">${escapeHtml(evt.name)}</td>
          <td>${escapeHtml(evt.venue)}</td>
          <td>${formattedDate}</td>
          <td>₹${Number(evt.ticketPrice || 0).toFixed(2)}</td>
          <td>
            <span class="badge ${isSoldOut ? 'badge-danger' : 'badge-success'}">
              ${evt.issuedTicketsCount} / ${evt.capacity}
            </span>
          </td>
          <td>
            <div class="button-group">
              <button class="btn btn-secondary btn-sm btn-quick-issue" data-id="${evt.id}">
                🎟️ Issue
              </button>
              <button class="btn btn-secondary btn-sm btn-quick-attendance" data-id="${evt.id}">
                📊 Attendance
              </button>
            </div>
          </td>
        `;

        tr.querySelector('.btn-quick-issue').addEventListener('click', () => {
          state.selectedEventId = evt.id;
          switchTab('issue-ticket');
        });

        tr.querySelector('.btn-quick-attendance').addEventListener('click', () => {
          state.selectedEventId = evt.id;
          switchTab('attendance');
        });

        tbody.appendChild(tr);
      });
      tableContainer.style.display = 'block';
    }
  } catch (err) {
    loadingEl.style.display = 'none';
    showAlert('danger', err.message);
  }
}

// ==========================================
// 4. EVENT MANAGEMENT LOGIC
// ==========================================

async function loadEvents() {
  const loadingEl = document.getElementById('events-loading');
  const emptyEl = document.getElementById('events-empty');
  const tableContainer = document.getElementById('events-table-container');
  const tbody = document.getElementById('events-tbody');

  loadingEl.style.display = 'flex';
  emptyEl.style.display = 'none';
  tableContainer.style.display = 'none';

  try {
    const events = await api.getAllEvents();
    state.events = events || [];

    loadingEl.style.display = 'none';
    if (state.events.length === 0) {
      emptyEl.style.display = 'block';
    } else {
      tbody.innerHTML = '';
      state.events.forEach((evt) => {
        const tr = document.createElement('tr');
        const formattedDate = evt.eventDate ? new Date(evt.eventDate).toLocaleString() : 'N/A';

        tr.innerHTML = `
          <td>#${evt.id}</td>
          <td>
            <div style="font-weight: 600;">${escapeHtml(evt.name)}</div>
            ${evt.description ? `<div style="font-size: 0.8rem; color: var(--text-muted);">${escapeHtml(evt.description)}</div>` : ''}
          </td>
          <td>${escapeHtml(evt.venue)}</td>
          <td>${formattedDate}</td>
          <td>₹${Number(evt.ticketPrice || 0).toFixed(2)}</td>
          <td>
            <span class="badge badge-info">
              ${evt.issuedTicketsCount} / ${evt.capacity}
            </span>
          </td>
          <td>
            <div class="button-group">
              <button class="btn btn-secondary btn-sm btn-edit-event" data-id="${evt.id}">
                ✏️ Edit
              </button>
              <button class="btn btn-danger btn-sm btn-delete-event" data-id="${evt.id}">
                🗑️ Delete
              </button>
            </div>
          </td>
        `;

        tr.querySelector('.btn-edit-event').addEventListener('click', () => {
          openEditModal(evt);
        });

        tr.querySelector('.btn-delete-event').addEventListener('click', () => {
          handleDeleteEvent(evt.id, evt.name);
        });

        tbody.appendChild(tr);
      });
      tableContainer.style.display = 'block';
    }
  } catch (err) {
    loadingEl.style.display = 'none';
    showAlert('danger', err.message);
  }
}

// Create Event
document.getElementById('create-event-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  clearFieldErrors();
  clearAlert();

  const submitBtn = document.getElementById('btn-submit-event');
  submitBtn.disabled = true;
  submitBtn.textContent = 'Registering Event...';

  const payload = {
    name: document.getElementById('event-name').value.trim(),
    venue: document.getElementById('event-venue').value.trim(),
    eventDate: formatIsoDate(document.getElementById('event-date').value),
    capacity: parseInt(document.getElementById('event-capacity').value, 10),
    ticketPrice: parseFloat(document.getElementById('event-price').value),
    description: document.getElementById('event-desc').value.trim() || null,
  };

  try {
    const created = await api.createEvent(payload);
    showAlert('success', `Event "${created.name}" created successfully!`);
    document.getElementById('create-event-form').reset();
    loadEvents();
  } catch (err) {
    if (err.validationErrors) {
      applyFieldErrors(err.validationErrors, 'err-event-');
    }
    showAlert('danger', err.message);
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = 'Register Event';
  }
});

// Edit Event Modal
function openEditModal(evt) {
  document.getElementById('edit-event-id').value = evt.id;
  document.getElementById('edit-name').value = evt.name || '';
  document.getElementById('edit-venue').value = evt.venue || '';
  document.getElementById('edit-date').value = evt.eventDate ? evt.eventDate.substring(0, 16) : '';
  document.getElementById('edit-capacity').value = evt.capacity || '';
  document.getElementById('edit-capacity').min = evt.issuedTicketsCount || 1;
  document.getElementById('edit-capacity-label').textContent = `Capacity (Min: ${evt.issuedTicketsCount} already issued)`;
  document.getElementById('edit-price').value = evt.ticketPrice || '';
  document.getElementById('edit-desc').value = evt.description || '';

  clearFieldErrors();
  document.getElementById('edit-event-modal').style.display = 'flex';
}

function closeEditModal() {
  document.getElementById('edit-event-modal').style.display = 'none';
}

document.getElementById('btn-close-edit-modal').addEventListener('click', closeEditModal);
document.getElementById('btn-cancel-edit-modal').addEventListener('click', closeEditModal);
document.getElementById('edit-event-modal').addEventListener('click', closeEditModal);

document.getElementById('edit-event-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  clearFieldErrors();

  const id = document.getElementById('edit-event-id').value;
  const saveBtn = document.getElementById('btn-save-edit-event');
  saveBtn.disabled = true;
  saveBtn.textContent = 'Saving...';

  const payload = {
    name: document.getElementById('edit-name').value.trim(),
    venue: document.getElementById('edit-venue').value.trim(),
    eventDate: formatIsoDate(document.getElementById('edit-date').value),
    capacity: parseInt(document.getElementById('edit-capacity').value, 10),
    ticketPrice: parseFloat(document.getElementById('edit-price').value),
    description: document.getElementById('edit-desc').value.trim() || null,
  };

  try {
    const updated = await api.updateEvent(id, payload);
    closeEditModal();
    showAlert('success', `Event "${updated.name}" updated successfully!`);
    loadEvents();
  } catch (err) {
    if (err.validationErrors) {
      applyFieldErrors(err.validationErrors, 'err-edit-');
    }
    showAlert('danger', err.message);
  } finally {
    saveBtn.disabled = false;
    saveBtn.textContent = 'Save Updates';
  }
});

// Delete Event
async function handleDeleteEvent(id, name) {
  if (!confirm(`Are you sure you want to delete event "${name}"?`)) return;

  try {
    await api.deleteEvent(id);
    showAlert('success', `Event "${name}" deleted successfully!`);
    loadEvents();
  } catch (err) {
    showAlert('danger', err.message);
  }
}

function clearFieldErrors() {
  document.querySelectorAll('.field-error').forEach((el) => (el.textContent = ''));
}

function applyFieldErrors(validationErrors, prefix = '') {
  for (const [field, msg] of Object.entries(validationErrors)) {
    const el = document.getElementById(`${prefix}${field.toLowerCase()}`);
    if (el) el.textContent = msg;
  }
}

// ==========================================
// 5. ISSUE TICKET LOGIC
// ==========================================

async function loadIssueTicketView() {
  const select = document.getElementById('ticket-event-id');
  select.innerHTML = '<option value="">-- Choose an Event --</option>';

  try {
    const events = await api.getAllEvents();
    state.events = events || [];

    events.forEach((evt) => {
      const opt = document.createElement('option');
      opt.value = evt.id;
      const isSoldOut = evt.issuedTicketsCount >= evt.capacity;
      opt.disabled = isSoldOut;
      opt.textContent = `${evt.name} — ₹${Number(evt.ticketPrice || 0).toFixed(2)} (${evt.issuedTicketsCount}/${evt.capacity} booked) ${isSoldOut ? '[SOLD OUT]' : ''}`;
      select.appendChild(opt);
    });

    if (state.selectedEventId) {
      select.value = state.selectedEventId;
    } else if (events.length > 0 && !select.value) {
      select.value = events[0].id;
    }
  } catch (err) {
    showAlert('danger', err.message);
  }
}

document.getElementById('issue-ticket-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  clearFieldErrors();
  clearAlert();

  const submitBtn = document.getElementById('btn-submit-ticket');
  submitBtn.disabled = true;
  submitBtn.textContent = 'Generating Ticket & QR...';

  const payload = {
    eventId: parseInt(document.getElementById('ticket-event-id').value, 10),
    attendeeName: document.getElementById('attendee-name').value.trim(),
    attendeeEmail: document.getElementById('attendee-email').value.trim(),
    attendeePhone: document.getElementById('attendee-phone').value.trim() || null,
  };

  try {
    const ticket = await api.issueTicket(payload);

    // Display Digital Ticket Pass
    document.getElementById('res-event-name').textContent = ticket.eventName;
    document.getElementById('res-ticket-id').textContent = `#${ticket.ticketId}`;
    document.getElementById('res-ticket-price').textContent = ticket.ticketPrice > 0 ? `₹${Number(ticket.ticketPrice).toFixed(2)}` : 'Free';
    document.getElementById('res-attendee-name').textContent = ticket.attendeeName;
    document.getElementById('res-attendee-email').textContent = ticket.attendeeEmail;
    document.getElementById('res-qr-code').textContent = ticket.qrCode;

    if (ticket.attendeePhone) {
      document.getElementById('res-attendee-phone').textContent = ticket.attendeePhone;
      document.getElementById('res-phone-container').style.display = 'block';
    } else {
      document.getElementById('res-phone-container').style.display = 'none';
    }

    const qrImg = document.getElementById('res-qr-image');
    if (ticket.qrCodeImageBase64) {
      qrImg.src = ticket.qrCodeImageBase64;
      qrImg.style.display = 'block';
    } else {
      qrImg.style.display = 'none';
    }

    document.getElementById('ticket-form-card').style.display = 'none';
    document.getElementById('ticket-result-container').style.display = 'block';
    showAlert('success', `Ticket #${ticket.ticketId} issued successfully!`);
  } catch (err) {
    if (err.validationErrors) {
      applyFieldErrors(err.validationErrors, 'err-');
    }
    showAlert('danger', err.message);
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = '🎟️ Issue Ticket';
  }
});

document.getElementById('btn-print-ticket').addEventListener('click', () => {
  window.print();
});

document.getElementById('btn-issue-another').addEventListener('click', () => {
  document.getElementById('ticket-result-container').style.display = 'none';
  document.getElementById('ticket-form-card').style.display = 'block';
  document.getElementById('attendee-name').value = '';
  document.getElementById('attendee-email').value = '';
  document.getElementById('attendee-phone').value = '';
  clearAlert();
});

// ==========================================
// 6. QR CHECK-IN LOGIC
// ==========================================

document.getElementById('checkin-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const input = document.getElementById('checkin-qr-input');
  const qrCode = input.value.trim();
  if (!qrCode) return;

  const btn = document.getElementById('btn-submit-checkin');
  const alertContainer = document.getElementById('checkin-alert-container');
  const resultCard = document.getElementById('checkin-result-card');

  btn.disabled = true;
  btn.textContent = 'Verifying...';
  alertContainer.innerHTML = '';
  resultCard.style.display = 'none';

  try {
    const res = await api.checkInTicket(qrCode);

    // Show Success Alert
    showAlert('success', 'Ticket checked in successfully', 'checkin-alert-container');

    // Populate Details
    document.getElementById('checkin-res-ticket-id').textContent = `Ticket #${res.ticketId}`;
    document.getElementById('checkin-res-attendee').textContent = res.attendeeName;
    document.getElementById('checkin-res-event').textContent = res.eventName;
    document.getElementById('checkin-res-time').textContent = res.checkedInAt ? new Date(res.checkedInAt).toLocaleTimeString() : 'Just now';
    document.getElementById('checkin-res-headcount').textContent = `${res.checkedInHeadcount} / ${res.eventCapacity} checked in`;
    document.getElementById('checkin-res-qr').textContent = res.qrCode;

    resultCard.style.display = 'block';
  } catch (err) {
    showAlert('danger', err.message, 'checkin-alert-container');
  } finally {
    btn.disabled = false;
    btn.textContent = '[ Check In ]';
  }
});

document.getElementById('btn-checkin-next').addEventListener('click', () => {
  document.getElementById('checkin-qr-input').value = '';
  document.getElementById('checkin-result-card').style.display = 'none';
  document.getElementById('checkin-alert-container').innerHTML = '';
  document.getElementById('checkin-qr-input').focus();
});

// ==========================================
// 7. ATTENDANCE LOGIC
// ==========================================

async function loadAttendanceView() {
  const select = document.getElementById('attendance-event-select');
  select.innerHTML = '<option value="">-- Choose an Event --</option>';

  try {
    const events = await api.getAllEvents();
    state.events = events || [];

    events.forEach((evt) => {
      const opt = document.createElement('option');
      opt.value = evt.id;
      opt.textContent = `${evt.name} (Capacity: ${evt.capacity})`;
      select.appendChild(opt);
    });

    if (state.selectedEventId) {
      select.value = state.selectedEventId;
    } else if (events.length > 0) {
      select.value = events[0].id;
    }

    if (select.value) {
      fetchEventAttendance(select.value);
    }
  } catch (err) {
    showAlert('danger', err.message);
  }
}

async function fetchEventAttendance(eventId) {
  if (!eventId) return;

  const loadingEl = document.getElementById('attendance-loading');
  const contentEl = document.getElementById('attendance-content');

  loadingEl.style.display = 'flex';
  contentEl.style.display = 'none';

  try {
    const att = await api.getEventAttendance(eventId);

    document.getElementById('att-event-name').textContent = att.eventName;
    document.getElementById('att-event-badge').textContent = `Event #${att.eventId}`;
    document.getElementById('att-capacity').textContent = att.capacity;
    document.getElementById('att-issued').textContent = att.issuedTickets;
    document.getElementById('att-headcount').textContent = att.checkedInHeadcount;
    document.getElementById('att-remaining').textContent = att.remainingCapacity;

    const pct = att.capacity > 0 ? Math.min(100, Math.round((att.checkedInHeadcount / att.capacity) * 100)) : 0;
    document.getElementById('att-progress-label').innerHTML = `Checked In: <strong>${att.checkedInHeadcount} / ${att.capacity}</strong>`;
    document.getElementById('att-progress-pct').textContent = `${pct}% Occupied`;
    document.getElementById('att-progress-fill').style.width = `${pct}%`;

    document.getElementById('att-summary-text').innerHTML = `
      ℹ️ <strong>Attendance Summary:</strong> <strong>${att.checkedInHeadcount}</strong> attendees have entered out of <strong>${att.issuedTickets}</strong> issued tickets. <strong>${att.remainingCapacity}</strong> tickets can still be issued before reaching venue capacity (${att.capacity}).
    `;

    loadingEl.style.display = 'none';
    contentEl.style.display = 'block';
  } catch (err) {
    loadingEl.style.display = 'none';
    showAlert('danger', err.message);
  }
}

document.getElementById('attendance-event-select').addEventListener('change', (e) => {
  fetchEventAttendance(e.target.value);
});

document.getElementById('btn-refresh-attendance').addEventListener('click', () => {
  const eventId = document.getElementById('attendance-event-select').value;
  if (eventId) fetchEventAttendance(eventId);
});

// ==========================================
// 8. GLOBAL INITIALIZATION
// ==========================================

document.addEventListener('DOMContentLoaded', () => {
  // Navigation tabs click listeners
  document.querySelectorAll('.nav-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      switchTab(btn.dataset.tab);
    });
  });

  // Dashboard quick button
  document.getElementById('btn-refresh-dashboard').addEventListener('click', loadDashboard);
  document.getElementById('btn-goto-create-event').addEventListener('click', () => switchTab('events'));
  document.getElementById('btn-refresh-events').addEventListener('click', loadEvents);

  // Initialize on Dashboard
  switchTab('dashboard');
});

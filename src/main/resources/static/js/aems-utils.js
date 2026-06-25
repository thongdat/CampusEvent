/* Campus Event Management - reusable browser utilities */
(function (global) {
  'use strict';

  const AEMS = {};
  const DEFAULT_TIMEOUT = 15000;

  function isObject(value) {
    return value !== null && typeof value === 'object' && !Array.isArray(value);
  }

  function merge(target, source) {
    const output = Object.assign({}, target);
    if (!isObject(source)) return output;
    Object.keys(source).forEach((key) => {
      if (isObject(source[key])) output[key] = merge(output[key] || {}, source[key]);
      else output[key] = source[key];
    });
    return output;
  }

  AEMS.storage = {
    get(key, fallback = null) {
      try {
        const raw = localStorage.getItem(key);
        return raw === null ? fallback : JSON.parse(raw);
      } catch (error) {
        console.warn('Unable to read localStorage key:', key, error);
        return fallback;
      }
    },
    set(key, value) {
      try {
        localStorage.setItem(key, JSON.stringify(value));
        return true;
      } catch (error) {
        console.warn('Unable to write localStorage key:', key, error);
        return false;
      }
    },
    remove(key) {
      try {
        localStorage.removeItem(key);
        return true;
      } catch (error) {
        return false;
      }
    }
  };

  AEMS.format = {
    date(value, locale = 'vi-VN') {
      if (!value) return '';
      const date = value instanceof Date ? value : new Date(value);
      if (Number.isNaN(date.getTime())) return '';
      return new Intl.DateTimeFormat(locale, { day: '2-digit', month: '2-digit', year: 'numeric' }).format(date);
    },
    dateTime(value, locale = 'vi-VN') {
      if (!value) return '';
      const date = value instanceof Date ? value : new Date(value);
      if (Number.isNaN(date.getTime())) return '';
      return new Intl.DateTimeFormat(locale, {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
      }).format(date);
    },
    number(value, locale = 'vi-VN') {
      const number = Number(value);
      return Number.isFinite(number) ? new Intl.NumberFormat(locale).format(number) : '0';
    },
    currency(value, currency = 'VND', locale = 'vi-VN') {
      const number = Number(value);
      if (!Number.isFinite(number)) return '';
      return new Intl.NumberFormat(locale, { style: 'currency', currency }).format(number);
    },
    percent(value, digits = 0, locale = 'vi-VN') {
      const number = Number(value);
      if (!Number.isFinite(number)) return '0%';
      return new Intl.NumberFormat(locale, { style: 'percent', maximumFractionDigits: digits }).format(number);
    },
    relativeTime(value, locale = 'vi') {
      const date = value instanceof Date ? value : new Date(value);
      if (Number.isNaN(date.getTime())) return '';
      const diff = date.getTime() - Date.now();
      const units = [
        ['year', 31536000000], ['month', 2592000000], ['week', 604800000],
        ['day', 86400000], ['hour', 3600000], ['minute', 60000], ['second', 1000]
      ];
      const formatter = new Intl.RelativeTimeFormat(locale, { numeric: 'auto' });
      for (const [unit, size] of units) {
        if (Math.abs(diff) >= size || unit === 'second') return formatter.format(Math.round(diff / size), unit);
      }
      return '';
    }
  };

  AEMS.text = {
    escape(value) {
      const node = document.createElement('div');
      node.textContent = value == null ? '' : String(value);
      return node.innerHTML;
    },
    truncate(value, maxLength = 120) {
      const text = value == null ? '' : String(value).trim();
      return text.length <= maxLength ? text : `${text.slice(0, maxLength - 1)}…`;
    },
    slug(value) {
      return String(value || '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase()
        .replace(/đ/g, 'd')
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '');
    },
    initials(value, max = 2) {
      return String(value || '')
        .trim()
        .split(/\s+/)
        .filter(Boolean)
        .slice(-max)
        .map((part) => part.charAt(0).toUpperCase())
        .join('');
    }
  };

  AEMS.http = async function http(url, options = {}) {
    const config = merge({
      method: 'GET',
      credentials: 'same-origin',
      headers: { Accept: 'application/json' },
      timeout: DEFAULT_TIMEOUT,
      parseJson: true
    }, options);

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), config.timeout);
    const headers = Object.assign({}, config.headers);
    let body = config.body;

    if (body && isObject(body) && !(body instanceof FormData)) {
      headers['Content-Type'] = headers['Content-Type'] || 'application/json';
      body = JSON.stringify(body);
    }

    try {
      const response = await fetch(url, {
        method: config.method,
        credentials: config.credentials,
        headers,
        body,
        signal: controller.signal
      });
      const contentType = response.headers.get('content-type') || '';
      let payload = null;
      if (config.parseJson && contentType.includes('application/json')) payload = await response.json();
      else if (response.status !== 204) payload = await response.text();
      if (!response.ok) {
        const error = new Error(payload && payload.message ? payload.message : `HTTP ${response.status}`);
        error.status = response.status;
        error.payload = payload;
        throw error;
      }
      return payload;
    } catch (error) {
      if (error.name === 'AbortError') throw new Error('Yêu cầu đã hết thời gian chờ');
      throw error;
    } finally {
      clearTimeout(timeoutId);
    }
  };

  AEMS.debounce = function debounce(fn, wait = 300) {
    let timeoutId;
    return function debounced(...args) {
      clearTimeout(timeoutId);
      timeoutId = setTimeout(() => fn.apply(this, args), wait);
    };
  };

  AEMS.throttle = function throttle(fn, wait = 300) {
    let waiting = false;
    let lastArgs = null;
    return function throttled(...args) {
      if (waiting) {
        lastArgs = args;
        return;
      }
      fn.apply(this, args);
      waiting = true;
      setTimeout(() => {
        waiting = false;
        if (lastArgs) {
          const pending = lastArgs;
          lastArgs = null;
          throttled.apply(this, pending);
        }
      }, wait);
    };
  };

  AEMS.toast = function toast(message, options = {}) {
    const config = Object.assign({ type: 'info', title: '', duration: 3500 }, options);
    let container = document.querySelector('.aems-toast-container');
    if (!container) {
      container = document.createElement('div');
      container.className = 'aems-toast-container';
      container.setAttribute('aria-live', 'polite');
      document.body.appendChild(container);
    }
    const item = document.createElement('div');
    item.className = `aems-toast aems-toast-${config.type}`;
    item.setAttribute('role', config.type === 'danger' ? 'alert' : 'status');
    item.innerHTML = `
      <div class="aems-toast-content">
        ${config.title ? `<p class="aems-toast-title">${AEMS.text.escape(config.title)}</p>` : ''}
        <p class="aems-toast-message">${AEMS.text.escape(message)}</p>
      </div>
      <button type="button" class="aems-btn aems-btn-ghost aems-btn-sm" aria-label="Đóng">×</button>`;
    container.appendChild(item);
    const close = () => {
      if (!item.isConnected) return;
      item.classList.add('is-leaving');
      setTimeout(() => item.remove(), 190);
    };
    item.querySelector('button').addEventListener('click', close);
    if (config.duration > 0) setTimeout(close, config.duration);
    return { close, element: item };
  };

  AEMS.loading = {
    show(message = 'Đang xử lý...') {
      let overlay = document.querySelector('.aems-loading-overlay');
      if (overlay) return overlay;
      overlay = document.createElement('div');
      overlay.className = 'aems-loading-overlay';
      overlay.setAttribute('role', 'status');
      overlay.innerHTML = `<div class="aems-text-center"><div class="aems-spinner"></div><p>${AEMS.text.escape(message)}</p></div>`;
      document.body.appendChild(overlay);
      return overlay;
    },
    hide() {
      document.querySelector('.aems-loading-overlay')?.remove();
    }
  };

  AEMS.confirm = function confirmDialog(options = {}) {
    const config = Object.assign({
      title: 'Xác nhận thao tác',
      message: 'Bạn có chắc chắn muốn tiếp tục?',
      confirmText: 'Xác nhận',
      cancelText: 'Hủy',
      danger: false
    }, options);
    return new Promise((resolve) => {
      const backdrop = document.createElement('div');
      backdrop.className = 'aems-modal-backdrop';
      backdrop.innerHTML = `
        <div class="aems-modal" role="dialog" aria-modal="true">
          <div class="aems-modal-header"><h3 class="aems-card-title">${AEMS.text.escape(config.title)}</h3></div>
          <div class="aems-modal-body"><p>${AEMS.text.escape(config.message)}</p></div>
          <div class="aems-modal-footer">
            <button type="button" data-action="cancel" class="aems-btn aems-btn-secondary">${AEMS.text.escape(config.cancelText)}</button>
            <button type="button" data-action="confirm" class="aems-btn ${config.danger ? 'aems-btn-danger' : 'aems-btn-primary'}">${AEMS.text.escape(config.confirmText)}</button>
          </div>
        </div>`;
      document.body.appendChild(backdrop);
      const finish = (value) => { backdrop.remove(); resolve(value); };
      backdrop.querySelector('[data-action="cancel"]').addEventListener('click', () => finish(false));
      backdrop.querySelector('[data-action="confirm"]').addEventListener('click', () => finish(true));
      backdrop.addEventListener('click', (event) => { if (event.target === backdrop) finish(false); });
      backdrop.addEventListener('keydown', (event) => { if (event.key === 'Escape') finish(false); });
      backdrop.querySelector('[data-action="confirm"]').focus();
    });
  };

  AEMS.form = {
    toObject(form) {
      const data = new FormData(form);
      const result = {};
      data.forEach((value, key) => {
        if (Object.prototype.hasOwnProperty.call(result, key)) {
          result[key] = Array.isArray(result[key]) ? result[key].concat(value) : [result[key], value];
        } else result[key] = value;
      });
      return result;
    },
    clearErrors(form) {
      form.querySelectorAll('.is-invalid').forEach((element) => element.classList.remove('is-invalid'));
      form.querySelectorAll('.aems-error[data-generated="true"]').forEach((element) => element.remove());
    },
    setError(field, message) {
      field.classList.add('is-invalid');
      const error = document.createElement('div');
      error.className = 'aems-error';
      error.dataset.generated = 'true';
      error.textContent = message;
      field.insertAdjacentElement('afterend', error);
    },
    validate(form) {
      this.clearErrors(form);
      let valid = true;
      form.querySelectorAll('[required]').forEach((field) => {
        if (!String(field.value || '').trim()) {
          this.setError(field, field.dataset.requiredMessage || 'Trường này là bắt buộc');
          valid = false;
        }
      });
      form.querySelectorAll('[type="email"]').forEach((field) => {
        if (field.value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(field.value)) {
          this.setError(field, 'Email không hợp lệ');
          valid = false;
        }
      });
      form.querySelectorAll('[data-match]').forEach((field) => {
        const target = form.querySelector(field.dataset.match);
        if (target && field.value !== target.value) {
          this.setError(field, field.dataset.matchMessage || 'Giá trị xác nhận không khớp');
          valid = false;
        }
      });
      if (!valid) form.querySelector('.is-invalid')?.focus();
      return valid;
    }
  };

  AEMS.table = {
    filter(table, query, columns = null) {
      const normalized = String(query || '').trim().toLowerCase();
      let visibleCount = 0;
      table.querySelectorAll('tbody tr').forEach((row) => {
        const cells = Array.from(row.cells);
        const values = columns ? columns.map((index) => cells[index]?.textContent || '') : cells.map((cell) => cell.textContent || '');
        const visible = !normalized || values.join(' ').toLowerCase().includes(normalized);
        row.hidden = !visible;
        if (visible) visibleCount += 1;
      });
      return visibleCount;
    },
    sort(table, columnIndex, direction = 'asc') {
      const tbody = table.tBodies[0];
      if (!tbody) return;
      const rows = Array.from(tbody.rows);
      const factor = direction === 'desc' ? -1 : 1;
      rows.sort((a, b) => {
        const left = a.cells[columnIndex]?.dataset.sortValue || a.cells[columnIndex]?.textContent.trim() || '';
        const right = b.cells[columnIndex]?.dataset.sortValue || b.cells[columnIndex]?.textContent.trim() || '';
        const leftNumber = Number(left.replace(/[.,\s]/g, ''));
        const rightNumber = Number(right.replace(/[.,\s]/g, ''));
        if (Number.isFinite(leftNumber) && Number.isFinite(rightNumber)) return (leftNumber - rightNumber) * factor;
        return left.localeCompare(right, 'vi', { numeric: true, sensitivity: 'base' }) * factor;
      });
      rows.forEach((row) => tbody.appendChild(row));
    }
  };

  AEMS.tabs = function initTabs(root = document) {
    root.querySelectorAll('[data-aems-tabs]').forEach((container) => {
      const tabs = Array.from(container.querySelectorAll('[data-tab-target]'));
      const panels = Array.from(container.querySelectorAll('[data-tab-panel]'));
      const activate = (name) => {
        tabs.forEach((tab) => tab.classList.toggle('is-active', tab.dataset.tabTarget === name));
        panels.forEach((panel) => panel.classList.toggle('is-active', panel.dataset.tabPanel === name));
      };
      tabs.forEach((tab) => tab.addEventListener('click', () => activate(tab.dataset.tabTarget)));
      if (tabs.length && !tabs.some((tab) => tab.classList.contains('is-active'))) activate(tabs[0].dataset.tabTarget);
    });
  };

  AEMS.copy = async function copyText(value) {
    try {
      await navigator.clipboard.writeText(String(value));
      AEMS.toast('Đã sao chép vào bộ nhớ tạm', { type: 'success' });
      return true;
    } catch (error) {
      const area = document.createElement('textarea');
      area.value = String(value);
      area.style.position = 'fixed';
      area.style.opacity = '0';
      document.body.appendChild(area);
      area.select();
      const success = document.execCommand('copy');
      area.remove();
      return success;
    }
  };

  AEMS.query = {
    get(name, fallback = null) {
      return new URLSearchParams(location.search).get(name) ?? fallback;
    },
    set(values, replace = false) {
      const url = new URL(location.href);
      Object.entries(values).forEach(([key, value]) => {
        if (value === null || value === undefined || value === '') url.searchParams.delete(key);
        else url.searchParams.set(key, value);
      });
      history[replace ? 'replaceState' : 'pushState']({}, '', url);
    }
  };

  AEMS.init = function init() {
    AEMS.tabs(document);
    document.querySelectorAll('[data-copy]').forEach((button) => {
      button.addEventListener('click', () => AEMS.copy(button.dataset.copy));
    });
    document.querySelectorAll('form[data-aems-validate]').forEach((form) => {
      form.addEventListener('submit', (event) => {
        if (!AEMS.form.validate(form)) event.preventDefault();
      });
    });
    document.querySelectorAll('[data-confirm]').forEach((element) => {
      element.addEventListener('click', async (event) => {
        const accepted = await AEMS.confirm({
          title: element.dataset.confirmTitle || 'Xác nhận',
          message: element.dataset.confirm || 'Bạn có chắc chắn muốn tiếp tục?',
          danger: element.dataset.confirmDanger === 'true'
        });
        if (!accepted) event.preventDefault();
      });
    });
  };

  global.AEMS = AEMS;
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', AEMS.init);
  else AEMS.init();
})(window);

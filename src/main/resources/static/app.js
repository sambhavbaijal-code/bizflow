const API = '/api';
const state = { products: [], parties: [], currentPartyFilter: 'ALL', toastTimer: null, profile: null, authMode: 'login' };
const $ = (selector) => document.querySelector(selector);
const money = (value) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(Number(value || 0));
const number = (value) => new Intl.NumberFormat('en-IN', { maximumFractionDigits: 3 }).format(Number(value || 0));
const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));

async function api(path, options = {}) {
  const response = await fetch(`${API}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(sessionStorage.getItem('bizflowToken') ? { Authorization: `Bearer ${sessionStorage.getItem('bizflowToken')}` } : {}), ...(options.headers || {}) }
  });
  if (response.status === 204) return null;
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.error || `Request failed (${response.status})`);
  return data;
}

function showToast(message) {
  const toast = $('#toast');
  toast.textContent = message;
  toast.classList.add('show');
  clearTimeout(state.toastTimer);
  state.toastTimer = setTimeout(() => toast.classList.remove('show'), 3000);
}
function showError(message) { $('#formError').textContent = message; }
function dateLabel(value) {
  if (!value) return '—';
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? value : d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

function goToPage(page) {
  document.querySelectorAll('.page').forEach(el => el.classList.toggle('active', el.id === `page-${page}`));
  document.querySelectorAll('.nav-link').forEach(el => el.classList.toggle('active', el.dataset.page === page));
  const labels = { dashboard: 'Home', products: 'Products & Stock', sales: 'Sales & Billing', purchases: 'Purchases', parties: 'Customers & Suppliers' };
  $('#breadcrumbTitle').textContent = labels[page] || 'Overview';
  $('#sidebar').classList.remove('open');
  if (page === 'products') loadProducts();
  if (page === 'sales') loadSales();
  if (page === 'purchases') loadPurchases();
  if (page === 'parties') loadParties();
}

document.querySelectorAll('[data-page]').forEach(button => button.addEventListener('click', () => goToPage(button.dataset.page)));
$('#mobileMenu').addEventListener('click', () => $('#sidebar').classList.toggle('open'));
$('#refreshDashboard').addEventListener('click', loadDashboard);

async function loadAssistantStatus() {
  const submit = $('#assistantSubmit');
  const setup = $('#assistantSetup');
  const answer = $('#assistantAnswer');
  answer.hidden = true;
  answer.textContent = '';
  try {
    const status = await api('/assistant/status');
    submit.disabled = !status.configured;
    setup.hidden = status.configured;
    setup.textContent = status.configured
      ? ''
      : 'BharatCode is not connected yet. Add BHARATCODE_API_KEY to the server environment and restart BizFlow to enable AI answers.';
  } catch (error) {
    submit.disabled = true;
    setup.hidden = false;
    setup.textContent = error.message;
  }
}

$('#assistantForm').addEventListener('submit', async event => {
  event.preventDefault();
  if (!$('#assistantConsent').checked) return;
  const submit = $('#assistantSubmit');
  const answer = $('#assistantAnswer');
  submit.disabled = true;
  submit.textContent = 'Thinking…';
  answer.hidden = false;
  answer.textContent = 'Getting a practical answer from BharatCode…';
  try {
    const result = await api('/assistant', {
      method: 'POST',
      body: JSON.stringify({ question: $('#assistantQuestion').value.trim() })
    });
    answer.textContent = result.answer;
  } catch (error) {
    answer.textContent = error.message;
  } finally {
    submit.disabled = false;
    submit.textContent = 'Ask BharatCode';
  }
});

async function loadDashboard() {
  try {
    const data = await api('/dashboard');
    $('#metricProducts').textContent = number(data.productCount);
    $('#metricInventory').textContent = money(data.inventoryValue);
    $('#metricSales').textContent = money(data.todaySales);
    $('#metricGrossProfit').textContent = money(data.todayGrossProfit);
    $('#metricLowStock').textContent = number(data.lowStockCount);
    $('#lowStockBadge').textContent = number(data.lowStockCount);
    const sales = data.recentSales || [];
    $('#recentSalesBody').innerHTML = sales.length ? sales.slice(0, 6).map(s => `
      <tr><td><span class="invoice-id">${escapeHtml(s.invoiceNo)}</span></td><td><strong>${escapeHtml(s.partyName)}</strong></td><td>${dateLabel(s.createdAt)}</td><td class="align-right amount">${money(s.total)}</td></tr>`).join('') : '<tr><td colspan="4" class="empty-cell">No sales recorded yet.</td></tr>';
    const low = data.lowStockProducts || [];
    $('#lowStockList').innerHTML = low.length ? low.map(p => `
      <div class="stock-item"><div class="stock-product"><div class="product-thumb">${escapeHtml((p.name || '?').charAt(0).toUpperCase())}</div><div><strong>${escapeHtml(p.name)}</strong><small>${escapeHtml(p.sku)} · Reorder at ${number(p.reorderLevel)}</small></div></div><span class="stock-qty">${number(p.quantity)} ${escapeHtml(p.unit || '')}</span></div>`).join('') : '<div class="empty-state">All products are above their reorder levels.</div>';
  } catch (error) { showToast(error.message); }
}

async function loadProducts() {
  try { state.products = await api('/products'); renderProducts(); }
  catch (error) { $('#productsBody').innerHTML = `<tr><td colspan="8" class="empty-cell">${escapeHtml(error.message)}</td></tr>`; }
}
function renderProducts() {
  const search = ($('#productSearch')?.value || '').trim().toLowerCase();
  const list = state.products.filter(p => [p.name, p.sku, p.barcode, p.category, p.variant].some(v => String(v || '').toLowerCase().includes(search)));
  $('#productCountLabel').textContent = `${list.length} product${list.length === 1 ? '' : 's'}`;
  $('#productsBody').innerHTML = list.length ? list.map(p => `
    <tr>
      <td><strong>${escapeHtml(p.name)}</strong><br><span class="field-help">${escapeHtml(p.variant || 'No variant')}</span></td>
      <td>${escapeHtml(p.sku)}${p.barcode ? `<br><span class="field-help">${escapeHtml(p.barcode)}</span>` : ''}</td><td>${escapeHtml(p.category || '—')}</td>
      <td>${money(p.costPrice)}</td><td><strong>${money(p.sellingPrice)}</strong></td>
      <td><strong>${number(p.quantity)}</strong> ${escapeHtml(p.unit || '')}</td>
      <td><span class="status-pill ${p.lowStock ? 'status-low' : 'status-ok'}">${p.lowStock ? 'LOW STOCK' : 'IN STOCK'}</span></td>
      <td><button class="text-button" data-edit-product="${p.id}">Edit</button></td>
    </tr>`).join('') : '<tr><td colspan="8" class="empty-cell">No matching products found.</td></tr>';
  document.querySelectorAll('[data-edit-product]').forEach(btn => btn.addEventListener('click', () => openModal('product', Number(btn.dataset.editProduct))));
}
$('#productSearch').addEventListener('input', renderProducts);

async function loadSales() {
  try {
    const list = await api('/sales/recent');
    $('#salesBody').innerHTML = list.length ? list.map(s => `<tr><td><span class="invoice-id">${escapeHtml(s.invoiceNo)}</span></td><td><strong>${escapeHtml(s.partyName)}</strong></td><td>${dateLabel(s.createdAt)}</td><td>${s.items.length}</td><td class="align-right amount">${money(s.total)}</td></tr>`).join('') : '<tr><td colspan="5" class="empty-cell">No sales created yet.</td></tr>';
  } catch (error) { $('#salesBody').innerHTML = `<tr><td colspan="5" class="empty-cell">${escapeHtml(error.message)}</td></tr>`; }
}
async function loadPurchases() {
  try {
    const list = await api('/purchases/recent');
    $('#purchasesBody').innerHTML = list.length ? list.map(p => `<tr><td><span class="invoice-id">${escapeHtml(p.invoiceNo)}</span></td><td><strong>${escapeHtml(p.partyName)}</strong></td><td>${dateLabel(p.createdAt)}</td><td>${p.items.length}</td><td class="align-right amount">${money(p.total)}</td></tr>`).join('') : '<tr><td colspan="5" class="empty-cell">No purchases recorded yet.</td></tr>';
  } catch (error) { $('#purchasesBody').innerHTML = `<tr><td colspan="5" class="empty-cell">${escapeHtml(error.message)}</td></tr>`; }
}
async function loadParties() {
  try { state.parties = await api('/parties'); renderParties(); }
  catch (error) { $('#partiesBody').innerHTML = `<tr><td colspan="5" class="empty-cell">${escapeHtml(error.message)}</td></tr>`; }
}
function renderParties() {
  const list = state.parties.filter(p => state.currentPartyFilter === 'ALL' || p.type === state.currentPartyFilter);
  $('#partyCountLabel').textContent = `${list.length} contact${list.length === 1 ? '' : 's'}`;
  $('#partiesBody').innerHTML = list.length ? list.map(p => `<tr><td><strong>${escapeHtml(p.name)}</strong></td><td><span class="status-pill ${p.type === 'SUPPLIER' ? 'status-low' : 'status-ok'}">${p.type}</span></td><td>${escapeHtml(p.phone || '—')}</td><td>${escapeHtml(p.email || '—')}</td><td>${escapeHtml(p.address || '—')}</td></tr>`).join('') : '<tr><td colspan="5" class="empty-cell">No contacts in this category.</td></tr>';
}
document.querySelectorAll('[data-party-filter]').forEach(btn => btn.addEventListener('click', () => {
  state.currentPartyFilter = btn.dataset.partyFilter;
  document.querySelectorAll('[data-party-filter]').forEach(b => b.classList.toggle('active', b === btn));
  renderParties();
}));

const modalConfigs = {
  product: {
    title: 'Add product', eyebrow: 'PRODUCT CATALOGUE', button: 'Save product',
    fields: [
      ['sku','SKU / Product code','text','e.g. TSH-001',true], ['name','Product name','text','e.g. Classic T-Shirt',true],
      ['barcode','Barcode (optional)','text','Scan or enter the product barcode',false],
      ['category','Category','text','e.g. Clothing',false], ['unit','Unit of measure','text','piece, kg, box...',false],
      ['variant','Variant / attributes','text','Size M / Black',false], ['costPrice','Purchase cost (₹)','number','0.00',true],
      ['sellingPrice','Selling price (₹)','number','0.00',true], ['quantity','Opening stock','number','0',true],
      ['reorderLevel','Reorder level','number','5',true]
    ]
  },
  sale: { title: 'Create sale', eyebrow: 'CUSTOMER BILLING', button: 'Create invoice', fields: [] },
  purchase: { title: 'Record purchase', eyebrow: 'STOCK INWARD', button: 'Save purchase', fields: [] },
  business: { title: 'Add business', eyebrow: 'NEW SHOP / FIRM', button: 'Add business', fields: [
    ['name','Shop / firm name','text','Enter the name you want to see in your workspace list',true]
  ] },
  party: { title: 'Add business contact', eyebrow: 'CUSTOMER / SUPPLIER', button: 'Save contact', fields: [
    ['name','Name','text','Business or contact name',true], ['type','Contact type','select','',true],
    ['phone','Phone','tel','Phone number',false], ['email','Email','email','Email address',false], ['address','Address','text','Street, city',false]
  ]}
};
let activeModal = null;
function fieldMarkup([name,label,type,placeholder,required], value = '') {
  if (type === 'select') return `<div class="field"><label for="f-${name}">${label}${required ? ' *' : ''}</label><select id="f-${name}" name="${name}" ${required ? 'required' : ''}><option value="CUSTOMER">Customer</option><option value="SUPPLIER">Supplier</option></select></div>`;
  return `<div class="field"><label for="f-${name}">${label}${required ? ' *' : ''}</label><input id="f-${name}" name="${name}" type="${type}" placeholder="${placeholder}" ${type === 'number' ? 'min="0" step="0.001"' : ''} value="${escapeHtml(value)}" ${required ? 'required' : ''}/></div>`;
}
function optionsForParties(type) {
  const list = state.parties.filter(p => p.type === type);
  return list.map(p => `<option value="${p.id}">${escapeHtml(p.name)}</option>`).join('');
}
function optionsForProducts() {
  return state.products.map(p => `<option value="${p.id}" data-price="${p.sellingPrice}" data-cost="${p.costPrice}" data-stock="${p.quantity}">${escapeHtml(p.name)} · ${escapeHtml(p.sku)} (stock: ${number(p.quantity)})</option>`).join('');
}
async function openModal(type, editId = null) {
  try {
    if (['sale','purchase','party'].includes(type)) await loadParties();
    if (['sale','purchase'].includes(type)) await loadProducts();
    activeModal = { type, editId };
    const config = modalConfigs[type];
    $('#modalTitle').textContent = editId ? 'Edit product' : config.title;
    $('#modalEyebrow').textContent = config.eyebrow;
    $('#submitModal').textContent = editId ? 'Update product' : config.button;
    $('#previewInvoiceButton').hidden = type !== 'sale';
    $('#formError').textContent = '';
    let markup = '';
    if (type === 'sale' || type === 'purchase') {
      const partyType = type === 'sale' ? 'CUSTOMER' : 'SUPPLIER';
      const partyLabel = type === 'sale' ? 'Customer (optional)' : 'Supplier';
      markup = `<div class="field full"><label>${partyLabel}</label><select name="partyId" ${type === 'purchase' ? 'required' : ''}><option value="">${type === 'sale' ? 'Walk-in customer' : 'Choose supplier'}</option>${optionsForParties(partyType)}</select></div>
      <div class="field full"><label>Barcode lookup</label><div class="scan-actions"><input id="scanCodeInput" type="text" placeholder="Scan with USB reader or type barcode / SKU" autocomplete="off"/><button type="button" class="btn btn-soft" id="useBarcodeButton">Find product</button><button type="button" class="btn btn-outline" id="cameraBarcodeButton">Camera</button><button type="button" class="btn btn-outline" id="cashMemoButton">Read cash memo</button><input id="cashMemoFile" type="file" accept="image/*" capture="environment" hidden></div><small class="field-help">Use a USB scanner, camera, or type any saved product code. Common UPC/EAN leading-zero variants and product SKUs are matched. A code must be saved on the product or match its SKU.</small><div id="scanStatus" class="scan-status" role="status"></div></div>
      <div class="field full"><label>Items in this ${type === 'sale' ? 'sale' : 'purchase'}</label><div id="transactionLines" class="transaction-lines"></div><button type="button" class="btn btn-soft add-line-button" id="addTransactionLine">＋ Add another product</button></div>
      <div class="field full gst-choice"><label><input id="gstEnabled" name="gstEnabled" type="checkbox" value="yes"> Apply GST to this ${type === 'sale' ? 'invoice' : 'purchase'}</label></div>
      <div class="field" id="gstRateWrap" hidden><label for="gstRate">GST percentage</label><input id="gstRate" name="gstRate" type="number" min="0.01" max="100" step="0.01" value="12" placeholder="e.g. 12"></div>
      <div class="field" id="gstTypeWrap" hidden><label for="gstType">Supply type</label><select id="gstType" name="gstType"><option value="INTRA">Within state (CGST + SGST)</option><option value="INTER">Outside state (IGST)</option></select></div>
      <div class="field full"><div id="transactionTotals" class="transaction-totals" aria-live="polite"></div></div>
      <div class="field full"><span class="field-help">Enter item prices before GST. The app calculates tax and adds it to the total. ${type === 'sale' ? 'Stock decreases after saving.' : 'Stock increases after saving.'}</span></div>`;
    } else {
      let values = {};
      if (editId) {
        const p = state.products.find(x => x.id === editId);
        if (p) values = p;
      }
      markup = config.fields.map(f => fieldMarkup(f, values[f[0]] ?? '')).join('');
    }
    $('#modalFields').innerHTML = markup;
    if (type === 'product' && editId) $('#modalFields').insertAdjacentHTML('beforeend', '<div class="field full"><span class="field-help">Changing opening stock replaces the recorded quantity. For routine stock movements, use Purchases or Sales instead.</span></div>');
    if (type === 'business') $('#modalFields').insertAdjacentHTML('beforeend', '<div class="field full"><span class="field-help">This creates a separate workspace under your current login. Its products, customers, purchases and sales stay separate.</span></div>');
    if (type === 'sale' || type === 'purchase') {
      appendTransactionLine(type);
      $('#gstEnabled').addEventListener('change', () => {
        $('#gstRateWrap').hidden = !$('#gstEnabled').checked;
        $('#gstTypeWrap').hidden = !$('#gstEnabled').checked;
        updateTransactionTotals();
      });
      $('#gstRate').addEventListener('input', updateTransactionTotals);
      $('#gstType').addEventListener('change', updateTransactionTotals);
      $('#transactionLines').addEventListener('input', updateTransactionTotals);
      $('#transactionLines').addEventListener('change', updateTransactionTotals);
      updateTransactionTotals();
      $('#addTransactionLine').addEventListener('click', () => appendTransactionLine(type));
      $('#useBarcodeButton').addEventListener('click', () => addProductByCode($('#scanCodeInput').value, type));
      $('#scanCodeInput').addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); addProductByCode(event.currentTarget.value, type); } });
      $('#cameraBarcodeButton').addEventListener('click', openBarcodeScanner);
      $('#cashMemoButton').addEventListener('click', () => $('#cashMemoFile').click());
      $('#cashMemoFile').addEventListener('change', event => { const file = event.target.files?.[0]; if (file) readCashMemo(file, type); event.target.value = ''; });
    }
    $('#modalBackdrop').classList.add('open');
    $('#modalBackdrop').setAttribute('aria-hidden', 'false');
  } catch (error) { showToast(error.message); }
}
function appendTransactionLine(type) {
  const wrapper = document.createElement('div');
  wrapper.className = 'transaction-line';
  wrapper.innerHTML = `<select name="productId" class="transaction-product" aria-label="Product" required><option value="">Choose product</option>${optionsForProducts()}</select>
    <input name="quantity" type="number" min="0.001" step="0.001" value="1" aria-label="Quantity" required />
    <input name="price" type="number" min="0" step="0.01" value="0" aria-label="${type === 'sale' ? 'Selling price' : 'Purchase cost'}" required />
    <button type="button" class="remove-line" aria-label="Remove item" title="Remove item">×</button>`;
  wrapper.querySelector('.transaction-product').addEventListener('change', (event) => {
    const option = event.target.selectedOptions[0];
    const priceInput = wrapper.querySelector('[name="price"]');
    if (option && option.value) priceInput.value = type === 'sale' ? option.dataset.price : option.dataset.cost;
    updateTransactionTotals();
  });
  wrapper.querySelector('.remove-line').addEventListener('click', () => {
    if (document.querySelectorAll('.transaction-line').length > 1) wrapper.remove();
    else showError('At least one product is required.');
    updateTransactionTotals();
  });
  $('#transactionLines').appendChild(wrapper);
  return wrapper;
}

function updateTransactionTotals() {
  const host = $('#transactionTotals');
  if (!host) return;
  const subtotal = [...document.querySelectorAll('.transaction-line')].reduce((sum, row) => {
    const lineTotal = (Number(row.querySelector('[name="quantity"]').value) || 0) * (Number(row.querySelector('[name="price"]').value) || 0);
    return sum + Math.round((lineTotal + Number.EPSILON) * 100) / 100;
  }, 0);
  const enabled = $('#gstEnabled')?.checked;
  const rate = enabled ? Math.max(0, Number($('#gstRate')?.value) || 0) : 0;
  const tax = Math.round((subtotal * rate / 100 + Number.EPSILON) * 100) / 100;
  let taxLabel = `GST (${number(rate)}%)`;
  let split = '';
  if (enabled && rate > 0 && $('#gstType')?.value === 'INTRA') {
    const cgst = Math.round((tax / 2 + Number.EPSILON) * 100) / 100;
    const sgst = Math.round((tax - cgst + Number.EPSILON) * 100) / 100;
    split = `<div><span>CGST (${number(rate / 2)}%)</span><strong>${money(cgst)}</strong></div><div><span>SGST (${number(rate / 2)}%)</span><strong>${money(sgst)}</strong></div>`;
  } else if (enabled && rate > 0) {
    taxLabel = `IGST (${number(rate)}%)`;
  }
  host.innerHTML = `<div><span>Subtotal</span><strong>${money(subtotal)}</strong></div>${enabled && rate > 0 ? `<div><span>${taxLabel}</span><strong>${money(tax)}</strong></div>${split}` : `<div><span>GST</span><strong>${money(0)}</strong></div>`}<div class="transaction-grand-total"><span>Total</span><strong>${money(subtotal + tax)}</strong></div>`;
}

function defaultPrice(product, type) { return Number(type === 'sale' ? product.sellingPrice : product.costPrice); }
function fillTransactionLine(row, product, type, quantity = 1, price = defaultPrice(product, type)) {
  row.querySelector('[name="productId"]').value = String(product.id);
  row.querySelector('[name="quantity"]').value = String(quantity);
  row.querySelector('[name="price"]').value = String(price);
}
function addProductByCode(rawCode, type) {
  const code = String(rawCode || '').trim().toLowerCase();
  if (!code) { $('#scanStatus').textContent = 'Scan or enter a barcode first.'; return; }
  const inputKeys = barcodeAliases(code);
  const product = state.products.find(p => {
    const barcodeMatch = [...barcodeAliases(p.barcode)].some(key => inputKeys.has(key));
    const skuMatch = String(p.sku || '').trim().toLowerCase() === code;
    return barcodeMatch || skuMatch;
  });
  if (!product) { $('#scanStatus').textContent = `No product matches “${rawCode.trim()}”. Add this barcode to a product first.`; return; }
  const rows = [...document.querySelectorAll('.transaction-line')];
  let row = rows.find(item => Number(item.querySelector('[name="productId"]').value) === product.id);
  if (row) row.querySelector('[name="quantity"]').value = String(Number(row.querySelector('[name="quantity"]').value || 0) + 1);
  else {
    row = rows.find(item => !item.querySelector('[name="productId"]').value) || appendTransactionLine(type);
    fillTransactionLine(row, product, type);
  }
  $('#scanStatus').textContent = `Added ${product.name} (${product.sku}). Check its quantity and price.`;
  $('#scanCodeInput').value = '';
  $('#scanCodeInput').focus();
}

function normalizeScanText(value) { return String(value || '').toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim(); }
function normalizeBarcode(value) { return String(value || '').trim().toUpperCase().replace(/[\s-]+/g, ''); }
function barcodeAliases(value) {
  const code = normalizeBarcode(value).replace(/^\][A-Z0-9]{2}/, '').replace(/^\(01\)/, '');
  const aliases = new Set(code ? [code] : []);
  if (/^\d{12}$/.test(code)) aliases.add(`0${code}`);
  if (/^0\d{12}$/.test(code)) aliases.add(code.slice(1));
  if (/^\d{13}$/.test(code)) aliases.add(`0${code}`);
  if (/^0\d{13}$/.test(code)) aliases.add(code.slice(1));
  return aliases;
}
function matchMemoProduct(line) {
  const normalized = normalizeScanText(line);
  return state.products.map(product => ({ product, keys: [product.barcode, product.sku, product.name].filter(Boolean).map(normalizeScanText).filter(key => key.length >= 3).sort((a, b) => b.length - a.length) }))
    .map(entry => ({ ...entry, key: entry.keys.find(key => normalized.includes(key)) }))
    .filter(entry => entry.key)
    .sort((a, b) => b.key.length - a.key.length)[0] || null;
}
function parseMemoItems(text, type) {
  const found = new Map();
  for (const line of text.split(/\r?\n/)) {
    const match = matchMemoProduct(line);
    if (!match) continue;
    const values = (line.match(/\d[\d,]*(?:\.\d{1,3})?/g) || []).map(value => Number(value.replace(/,/g, ''))).filter(Number.isFinite);
    let quantity = 1; let price = defaultPrice(match.product, type);
    if (values.length >= 3) { quantity = values.at(-3); price = values.at(-2); }
    else if (values.length === 2) { quantity = values[0]; price = values[1]; }
    else if (values.length === 1) price = values[0];
    if (!(quantity > 0)) quantity = 1;
    if (!(price >= 0)) price = defaultPrice(match.product, type);
    const current = found.get(match.product.id);
    if (current) { current.quantity += quantity; current.price = price; }
    else found.set(match.product.id, { product: match.product, quantity, price });
  }
  return [...found.values()];
}
async function readCashMemo(file, type) {
  const status = $('#scanStatus');
  if (!window.Tesseract?.createWorker) { status.textContent = 'OCR could not load. Check internet and reload the app, then try again.'; return; }
  status.textContent = 'Reading memo text with on-device OCR…';
  $('#cashMemoButton').disabled = true;
  let worker;
  try {
    worker = await Tesseract.createWorker('eng', 1, { logger: progress => { if (progress.status === 'recognizing text') status.textContent = `Reading memo… ${Math.round((progress.progress || 0) * 100)}%`; } });
    const { data: { text } } = await worker.recognize(file);
    const items = parseMemoItems(text, type);
    if (!items.length) { status.textContent = 'Text was read, but no catalog products were recognized. You can still use the text preview and enter the lines yourself.'; }
    else {
      const blankRows = [...document.querySelectorAll('.transaction-line')].filter(row => !row.querySelector('[name="productId"]').value);
      items.forEach(item => {
        const row = blankRows.shift() || appendTransactionLine(type);
        fillTransactionLine(row, item.product, type, item.quantity, item.price);
      });
      status.textContent = `OCR found ${items.length} catalog product${items.length === 1 ? '' : 's'}. Check the party, quantities and prices before saving.`;
    }
    const preview = document.createElement('details');
    preview.className = 'ocr-preview';
    preview.innerHTML = `<summary>View text read from memo</summary><pre></pre>`;
    preview.querySelector('pre').textContent = text.slice(0, 5000) || 'No text recognized.';
    status.appendChild(preview);
  } catch (error) { status.textContent = `Could not read this image: ${error.message}`; }
  finally { if (worker) await worker.terminate(); $('#cashMemoButton').disabled = false; }
}

let barcodeStream = null;
let barcodeDetector = null;
let barcodeAnimation = null;
async function openBarcodeScanner() {
  try {
    if (!('BarcodeDetector' in window) || !navigator.mediaDevices?.getUserMedia) throw new Error('Camera barcode scanning is not supported here. Use a USB scanner or type the code in the barcode box.');
    const formats = await BarcodeDetector.getSupportedFormats();
    const supported = ['ean_13','ean_8','upc_a','upc_e','code_128','code_39','itf','codabar','gs1_databar','gs1_databar_limited','gs1_databar_expanded','qr_code','data_matrix','aztec','pdf417'].filter(format => formats.includes(format));
    barcodeDetector = new BarcodeDetector({ formats: supported });
    barcodeStream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: 'environment' } }, audio: false });
    const panel = $('#scannerBackdrop');
    panel.classList.add('open');
    const video = $('#barcodeVideo'); video.srcObject = barcodeStream; await video.play();
    $('#scannerStatus').textContent = 'Point the camera at a product barcode.';
    scanBarcodeFrame();
  } catch (error) { $('#scanStatus').textContent = error.message || 'Could not start the camera.'; stopBarcodeScanner(); }
}
async function scanBarcodeFrame() {
  const video = $('#barcodeVideo');
  if (!barcodeDetector || !video || video.readyState < 2) { barcodeAnimation = requestAnimationFrame(scanBarcodeFrame); return; }
  try {
    const codes = await barcodeDetector.detect(video);
    if (codes.length) {
      const code = codes[0].rawValue;
      stopBarcodeScanner();
      $('#scannerBackdrop').classList.remove('open');
      $('#scanCodeInput').value = code;
      addProductByCode(code, activeModal.type);
      return;
    }
  } catch { /* Keep scanning until a readable barcode is in frame. */ }
  barcodeAnimation = requestAnimationFrame(scanBarcodeFrame);
}
function stopBarcodeScanner() {
  if (barcodeAnimation) cancelAnimationFrame(barcodeAnimation);
  barcodeAnimation = null; barcodeDetector = null;
  if (barcodeStream) barcodeStream.getTracks().forEach(track => track.stop());
  barcodeStream = null;
  const video = $('#barcodeVideo'); if (video) video.srcObject = null;
}
$('#closeScanner').addEventListener('click', () => { stopBarcodeScanner(); $('#scannerBackdrop').classList.remove('open'); });
$('#scannerBackdrop').addEventListener('click', event => { if (event.target === $('#scannerBackdrop')) { stopBarcodeScanner(); event.currentTarget.classList.remove('open'); } });

function closeModal() {
  $('#modalBackdrop').classList.remove('open');
  $('#modalBackdrop').setAttribute('aria-hidden', 'true');
  $('#modalForm').reset(); activeModal = null;
}
document.querySelectorAll('[data-open]').forEach(btn => btn.addEventListener('click', () => openModal(btn.dataset.open)));
$('#closeModal').addEventListener('click', closeModal);
$('#cancelModal').addEventListener('click', closeModal);
$('#previewInvoiceButton').addEventListener('click', previewInvoice);
$('#modalBackdrop').addEventListener('click', e => { if (e.target === $('#modalBackdrop')) closeModal(); });

document.querySelectorAll('[data-open]').forEach(btn => btn.addEventListener('keydown', e => { if (e.key === 'Escape') closeModal(); }));
$('#modalForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!activeModal) return;
  const { type, editId } = activeModal;
  const data = Object.fromEntries(new FormData(event.currentTarget).entries());
  const submit = $('#submitModal'); submit.disabled = true; submit.textContent = 'Saving...'; showError('');
  try {
    if (type === 'business') {
      const name = data.name.trim();
      const profile = await api('/businesses', { method: 'POST', body: JSON.stringify({ name }) });
      closeModal();
      await enterWorkspace(profile);
      showToast(`${name} workspace added.`);
    } else if (type === 'product') {
      const body = { sku: data.sku.trim(), name: data.name.trim(), barcode: data.barcode.trim(), category: data.category, unit: data.unit,
        variant: data.variant, costPrice: Number(data.costPrice), sellingPrice: Number(data.sellingPrice),
        quantity: Number(data.quantity), reorderLevel: Number(data.reorderLevel) };
      await api(editId ? `/products/${editId}` : '/products', { method: editId ? 'PUT' : 'POST', body: JSON.stringify(body) });
      showToast(editId ? 'Product updated.' : 'Product added.');
    } else if (type === 'party') {
      await api('/parties', { method: 'POST', body: JSON.stringify({ name: data.name.trim(), type: data.type, phone: data.phone, email: data.email, address: data.address }) });
      showToast('Business contact saved.');
    } else {
      const lines = [...document.querySelectorAll('.transaction-line')].map(row => ({
        productId: Number(row.querySelector('[name="productId"]').value),
        quantity: Number(row.querySelector('[name="quantity"]').value),
        price: Number(row.querySelector('[name="price"]').value)
      }));
      if (!lines.length || lines.some(line => !line.productId)) throw new Error('Choose a product for every line.');
      if (lines.some(line => !Number.isFinite(line.quantity) || line.quantity <= 0 || !Number.isFinite(line.price) || line.price < 0)) throw new Error('Enter a valid quantity and price for every product.');
      const gst = { gstRate: data.gstEnabled === 'yes' ? Number(data.gstRate || 0) : 0, gstType: data.gstType || 'INTRA' };
      const body = type === 'sale' ? { customerId: data.partyId ? Number(data.partyId) : null, items: lines, ...gst }
        : { supplierId: Number(data.partyId), items: lines, ...gst };
      const transaction = await api(type === 'sale' ? '/sales' : '/purchases', { method: 'POST', body: JSON.stringify(body) });
      showToast(`${type === 'sale' ? 'Invoice' : 'Purchase'} ${transaction.invoiceNo} saved.`);
      if (type === 'sale') setTimeout(() => printInvoice(transaction), 350);
    }
    closeModal();
    await Promise.all([loadDashboard(), loadProducts(), loadParties(), loadSales(), loadPurchases()]);
  } catch (error) { showError(error.message); }
  finally { submit.disabled = false; submit.textContent = modalConfigs[type].button; }
});

function previewInvoice() {
  if (!activeModal || activeModal.type !== 'sale') return;
  const lines = [...document.querySelectorAll('.transaction-line')].map(row => ({
    productId: Number(row.querySelector('[name="productId"]').value),
    quantity: Number(row.querySelector('[name="quantity"]').value),
    price: Number(row.querySelector('[name="price"]').value)
  }));
  if (!lines.length || lines.some(line => !line.productId)) { showError('Choose a product for every line before previewing.'); return; }
  if (lines.some(line => !Number.isFinite(line.quantity) || line.quantity <= 0 || !Number.isFinite(line.price) || line.price < 0)) { showError('Enter a valid quantity and price for every product before previewing.'); return; }
  const items = lines.map(line => {
    const product = state.products.find(candidate => candidate.id === line.productId);
    const lineTotal = Math.round((line.quantity * line.price + Number.EPSILON) * 100) / 100;
    return { productId: product.id, productName: product.name, sku: product.sku, quantity: line.quantity, unitPrice: line.price, lineTotal };
  });
  const subtotal = Math.round((items.reduce((sum, item) => sum + item.lineTotal, 0) + Number.EPSILON) * 100) / 100;
  const data = Object.fromEntries(new FormData($('#modalForm')).entries());
  const gstRate = data.gstEnabled === 'yes' ? Number(data.gstRate || 0) : 0;
  if (!Number.isFinite(gstRate) || gstRate < 0 || gstRate > 100 || (data.gstEnabled === 'yes' && gstRate === 0)) { showError('Enter a GST rate above 0 and up to 100 before previewing.'); return; }
  const gstAmount = Math.round((subtotal * gstRate / 100 + Number.EPSILON) * 100) / 100;
  const customer = state.parties.find(p => p.id === Number(data.partyId));
  printInvoice({ invoiceNo: 'DRAFT PREVIEW', partyName: customer?.name || 'Walk-in customer', createdAt: new Date().toISOString(),
    subtotal, gstRate, gstAmount, gstType: data.gstType || 'INTRA', total: subtotal + gstAmount, items }, { preview: true });
}

function printInvoice(tx, { preview = false } = {}) {
  const rows = tx.items.map(i => `<tr><td>${escapeHtml(i.productName)}<br><small>${escapeHtml(i.sku)}</small></td><td>${number(i.quantity)}</td><td>${money(i.unitPrice)}</td><td>${money(i.lineTotal)}</td></tr>`).join('');
  const win = window.open('', '_blank', 'width=760,height=700');
  if (!win) { showToast(preview ? 'Allow pop-ups to preview the invoice.' : 'Invoice saved. Allow pop-ups to print it.'); return; }
  const tax = Number(tx.gstAmount || 0);
  const cgst = Math.round((tax / 2 + Number.EPSILON) * 100) / 100;
  const sgst = Math.round((tax - cgst + Number.EPSILON) * 100) / 100;
  const gstLine = Number(tx.gstRate) > 0 ? (tx.gstType === 'INTER'
    ? `<div class="summary-row"><span>IGST (${number(tx.gstRate)}%)</span><strong>${money(tax)}</strong></div>`
    : `<div class="summary-row"><span>CGST (${number(Number(tx.gstRate) / 2)}%)</span><strong>${money(cgst)}</strong></div><div class="summary-row"><span>SGST (${number(Number(tx.gstRate) / 2)}%)</span><strong>${money(sgst)}</strong></div>`)
    : `<div class="summary-row"><span>GST</span><strong>${money(0)}</strong></div>`;
  win.document.write(`<!doctype html><html><head><title>${escapeHtml(preview ? 'Invoice preview' : tx.invoiceNo)}</title><style>body{font:14px Arial,sans-serif;color:#182235;padding:35px}h1{margin:0 0 5px}.muted{color:#778195;font-size:12px}.preview-note{display:inline-block;padding:7px 10px;background:#fff4d6;color:#765400;font-size:11px;font-weight:bold;border-radius:5px;margin:12px 0}table{width:100%;border-collapse:collapse;margin-top:30px}th,td{text-align:left;padding:12px;border-bottom:1px solid #ddd}th{font-size:11px;color:#667085}.summary{margin:24px 0 0 auto;width:min(300px,100%)}.summary-row{display:flex;justify-content:space-between;padding:5px 0}.total{display:flex;justify-content:space-between;border-top:1px solid #ccc;padding-top:12px;margin-top:8px;font-size:20px;font-weight:bold}</style></head><body>${preview ? '<div class="preview-note">DRAFT PREVIEW — NOT SAVED</div>' : ''}<h1>${preview ? 'INVOICE PREVIEW' : 'SALES INVOICE'}</h1><p class="muted">${escapeHtml(tx.invoiceNo)} · ${dateLabel(tx.createdAt)}</p><p><b>Customer:</b> ${escapeHtml(tx.partyName)}</p><table><thead><tr><th>PRODUCT</th><th>QTY</th><th>UNIT PRICE</th><th>AMOUNT</th></tr></thead><tbody>${rows}</tbody></table><div class="summary"><div class="summary-row"><span>Subtotal</span><strong>${money(tx.subtotal)}</strong></div>${gstLine}<div class="total"><span>Total</span><span>${money(tx.total)}</span></div></div><p class="muted">Thank you for your business.</p>${preview ? '' : '<script>window.onload=()=>window.print()</script>'}</body></html>`);
  win.document.close();
}

function showAuth(mode = 'login') {
  state.authMode = mode; $('#appShell').hidden = true; $('#authScreen').hidden = false;
  $('#authFormWrap').hidden = false; $('#workspacePicker').hidden = true; $('#authError').textContent = '';
  const registering = mode === 'register';
  $('#authTitle').textContent = registering ? 'Create your workspace' : 'Welcome back';
  $('#authSubtitle').textContent = registering ? 'One login for each of your business workspaces.' : 'Sign in to open your business workspace.';
  $('#authNameWrap').hidden = !registering; $('#authBusinessWrap').hidden = !registering;
  $('#authName').required = registering; $('#authBusiness').required = registering;
  $('#authPassword').autocomplete = registering ? 'new-password' : 'current-password';
  $('#authPassword').minLength = registering ? 8 : 1;
  $('#authSubmit').textContent = registering ? 'Create account' : 'Sign in';
  $('#authSwitchLine').innerHTML = registering ? 'Already have an account? <button type="button" id="authSwitch">Sign in</button>' : 'New to BizFlow? <button type="button" id="authSwitch">Create an account</button>';
  $('#authSwitch').addEventListener('click', () => showAuth(registering ? 'login' : 'register'), { once: true });
}
function showWorkspacePicker(profile) {
  state.profile = profile; $('#authFormWrap').hidden = true; $('#workspacePicker').hidden = false;
  $('#authTitle').textContent = 'Choose a business'; $('#authSubtitle').textContent = 'Each workspace keeps its ERP records separate.';
  $('#workspaceOptions').innerHTML = (profile.businesses || []).map(b => `<button class="workspace-choice" data-workspace="${Number(b.id)}"><span class="workspace-choice-mark">${escapeHtml((b.name || 'B').slice(0,1).toUpperCase())}</span><span><strong>${escapeHtml(b.name)}</strong><small>Open this business workspace</small></span></button>`).join('');
  document.querySelectorAll('[data-workspace]').forEach(button => button.addEventListener('click', async () => {
    button.disabled = true;
    try { const next = await api(`/businesses/${button.dataset.workspace}/select`, { method: 'POST' }); await enterWorkspace(next); }
    catch (error) { $('#authError').textContent = error.message; button.disabled = false; }
  }));
  $('#workspaceBack').onclick = async () => { await api('/auth/logout', { method: 'POST' }).catch(() => {}); sessionStorage.removeItem('bizflowToken'); showAuth('login'); };
}
async function enterWorkspace(profile) {
  if (profile.token) sessionStorage.setItem('bizflowToken', profile.token);
  state.profile = profile;
  if (!profile.activeBusiness) { showWorkspacePicker(profile); return; }
  $('#authScreen').hidden = true; $('#appShell').hidden = false;
  $('#signedInName').textContent = profile.user.name; $('#sidebarUserName').textContent = profile.user.name;
  $('#businessSwitcher').innerHTML = (profile.businesses || []).map(b => `<option value="${Number(b.id)}" ${Number(b.id) === Number(profile.activeBusiness.id) ? 'selected' : ''}>${escapeHtml(b.name)}</option>`).join('');
  await Promise.all([loadDashboard(), loadProducts(), loadParties(), loadSales(), loadPurchases(), loadAssistantStatus()]);
}
$('#authForm').addEventListener('submit', async event => {
  event.preventDefault(); $('#authError').textContent = ''; $('#authSubmit').disabled = true;
  const registering = state.authMode === 'register';
  const payload = registering ? { name: $('#authName').value.trim(), email: $('#authEmail').value.trim(), password: $('#authPassword').value, businessName: $('#authBusiness').value.trim() } : { email: $('#authEmail').value.trim(), password: $('#authPassword').value };
  try { const profile = await api(registering ? '/auth/register' : '/auth/login', { method: 'POST', body: JSON.stringify(payload) }); await enterWorkspace(profile); }
  catch (error) { $('#authError').textContent = error.message; }
  finally { $('#authSubmit').disabled = false; }
});
$('#businessSwitcher').addEventListener('change', async event => {
  try { const profile = await api(`/businesses/${event.target.value}/select`, { method: 'POST' }); await enterWorkspace(profile); }
  catch (error) { showToast(error.message); }
});
$('#addBusiness').addEventListener('click', () => openModal('business'));
$('#signOut').addEventListener('click', async () => { await api('/auth/logout', { method: 'POST' }).catch(() => {}); sessionStorage.removeItem('bizflowToken'); state.profile = null; showAuth('login'); });
async function init() {
  try { await api('/health'); }
  catch { showAuth('login'); $('#authError').textContent = 'Cannot reach BizFlow server. Start the Java application first.'; return; }
  const token = sessionStorage.getItem('bizflowToken');
  if (!token) { showAuth('login'); return; }
  try { await enterWorkspace(await api('/me')); }
  catch { sessionStorage.removeItem('bizflowToken'); showAuth('login'); }
}
init();

const eyeColors = ["GREEN", "BLACK", "BLUE", "YELLOW", "ORANGE"];
const hairColors = ["RED", "BLACK", "ORANGE", "WHITE", "BROWN"];
const nationalities = ["RUSSIA", "VATICAN", "ITALY", "THAILAND", "NORTH_KOREA"];
const fields = [
    "id", "name", "coordinates.x", "coordinates.y", "creationDate", "height",
    "eyeColor", "hairColor", "nationality", "location.x", "location.y", "location.z"
];
const state = { page: 0, editingId: null };
let filterCounter = 0;
let saveNoticeTimeout;
const byId = id => document.getElementById(id);

function fillSelect(select, values, firstLabel) {
    select.replaceChildren();
    if (firstLabel !== null) {
        const empty = document.createElement("option");
        empty.value = "";
        empty.textContent = firstLabel;
        select.append(empty);
    }
    for (const value of values) {
        const option = document.createElement("option");
        option.value = value;
        option.textContent = value;
        select.append(option);
    }
}

function setMessage(element, text, error = false) {
    element.textContent = text;
    element.classList.toggle("error", error);
}

function showSaveNotice(text) {
    const notice = byId("save-notice");
    notice.textContent = text;
    notice.classList.remove("hidden");
    clearTimeout(saveNoticeTimeout);
    saveNoticeTimeout = setTimeout(() => notice.classList.add("hidden"), 4000);
}

function clearSaveNotice() {
    clearTimeout(saveNoticeTimeout);
    byId("save-notice").classList.add("hidden");
}

async function readResponse(response) {
    const text = await response.text();
    if (!text) return null;
    try {
        return parseLossless(text);
    } catch {
        return text;
    }
}

function parseLossless(text) {
    return JSON.parse(text.replace(/("x"\s*:\s*)(-?\d+)/g, "$1\"$2\""));
}

function errorText(data, fallback) {
    if (data && typeof data === "object") {
        return data.message || data.detail || data.title || fallback;
    }
    if (typeof data === "string" && data.trim()) {
        return /^\s*<(?:!doctype\s+html|html|head|body)\b/i.test(data) ? fallback : data;
    }
    return fallback;
}

function isHtmlResponse(response, data) {
    return response.headers.get("content-type")?.toLowerCase().includes("text/html")
        || (typeof data === "string" && /^\s*<(?:!doctype\s+html|html|head|body)\b/i.test(data));
}

async function request(url, options = {}, failureLabel = "Не удалось выполнить запрос") {
    let response;
    try {
        response = await fetch(url, options);
    } catch (error) {
        console.error("API request failed", url, error);
        throw new Error(`${failureLabel}: сетевая ошибка`);
    }
    let data;
    try {
        data = response.status === 204 ? null : await readResponse(response);
    } catch (error) {
        console.error("API response read failed", url, error);
        throw new Error(`${failureLabel}: не удалось прочитать ответ`);
    }
    if (!response.ok) {
        console.error("API response error", url, response.status, response.headers.get("content-type"));
        if (isHtmlResponse(response, data)) throw new Error(`${failureLabel}: HTTP ${response.status}`);
        throw new Error(errorText(data, `${failureLabel}: HTTP ${response.status}`));
    }
    return data;
}

function showFieldError(field, message) {
    const messageId = field.getAttribute("aria-describedby");
    const messageNode = messageId ? byId(messageId) : null;
    if (messageNode) messageNode.textContent = message;
    field.classList.toggle("invalid", Boolean(message));
    if (message) field.setAttribute("aria-invalid", "true");
    else field.removeAttribute("aria-invalid");
}

function decimalError(value, max, floatValue = false) {
    const text = value.trim();
    if (!/^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?$/.test(text)) {
        return "Введите число, например 2 или 2.5";
    }
    const number = Number(text);
    if (!Number.isFinite(number)) return "Введите конечное число";
    if (Math.abs(number) > max) return "Число вне диапазона Java";
    if (floatValue && number !== 0 && (!Number.isFinite(Math.fround(number)) || Math.fround(number) === 0)) return "Число вне диапазона Float";
    return "";
}

function integerError(value, min, max, message, rangeMessage = "Целое число вне диапазона Java") {
    if (!/^[+-]?\d+$/.test(value)) return message;
    const number = BigInt(value);
    if (number < min || number > max) return rangeMessage;
    return "";
}

function fieldError(field) {
    const value = field.value;
    switch (field.id) {
        case "person-name": return value.length ? "" : "Введите имя";
        case "person-height": return integerError(value, 1n, 2147483647n, "Введите целое число больше 0", "Рост должен быть в диапазоне Integer");
        case "person-coordinates-x": {
            if (!/^[+-]?\d+$/.test(value)) return "Введите целое число не больше 557";
            const number = BigInt(value);
            if (number < -9223372036854775808n) return "Координата X должна быть в диапазоне Long";
            if (number > 557n) return "Введите целое число не больше 557";
            return "";
        }
        case "person-coordinates-y": return decimalError(value, Number.MAX_VALUE);
        case "person-eye-color": return eyeColors.includes(value) ? "" : "Выберите цвет глаз";
        case "person-hair-color": return hairColors.includes(value) ? "" : "Выберите цвет волос";
        case "person-location-x": return integerError(value, -9223372036854775808n, 9223372036854775807n, "Введите целое число", "Местоположение X должно быть в диапазоне Long");
        case "person-location-y": return decimalError(value, 3.4028234663852886e38, true);
        case "person-location-z": return integerError(value, -2147483648n, 2147483647n, "Введите целое число", "Местоположение Z должно быть в диапазоне Integer");
        case "person-id": return integerError(value, 1n, 2147483647n, "Введите целое число больше 0", "ID должен быть в диапазоне Integer");
        case "greater-color":
        case "eye-percent-color": return eyeColors.includes(value) ? "" : "Выберите цвет глаз";
        case "hair-percent-color": return hairColors.includes(value) ? "" : "Выберите цвет волос";
        default: return "";
    }
}

function checkField(field) {
    const error = fieldError(field);
    showFieldError(field, error);
    return error === "";
}

function validateFields(fieldsToCheck) {
    let firstInvalid = null;
    for (const field of fieldsToCheck) {
        if (!checkField(field) && firstInvalid === null) firstInvalid = field;
    }
    if (firstInvalid) {
        firstInvalid.scrollIntoView({ block: "nearest" });
        firstInvalid.focus();
    }
    return firstInvalid === null;
}

function filterRows() {
    return [...byId("filters").querySelectorAll(".query-row")];
}

function sortRows() {
    return [...byId("sorts").querySelectorAll(".sort-row")];
}

function dateError(value) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return "Введите дату в формате ГГГГ-ММ-ДД";
    const date = new Date(`${value}T00:00:00Z`);
    return Number.isNaN(date.getTime()) || date.toISOString().slice(0, 10) !== value
        ? "Введите существующую дату" : "";
}

function filterValueError(field, value) {
    if (value === "") return "";
    switch (field) {
        case "id":
        case "height": return integerError(value, 0n, 2147483647n, "Введите целое число от 0", "Введите число в диапазоне Integer");
        case "coordinates.x": {
            if (!/^[+-]?\d+$/.test(value)) return "Введите целое число не больше 557";
            const number = BigInt(value);
            if (number < -9223372036854775808n) return "Введите число в диапазоне Long";
            if (number > 557n) return "Введите целое число не больше 557";
            return "";
        }
        case "coordinates.y": return decimalError(value, Number.MAX_VALUE);
        case "creationDate": return dateError(value);
        case "eyeColor": return eyeColors.includes(value) ? "" : "Выберите цвет глаз";
        case "hairColor": return hairColors.includes(value) ? "" : "Выберите цвет волос";
        case "nationality": return value === "null" || nationalities.includes(value) ? "" : "Выберите национальность";
        case "location.x": return integerError(value, -9223372036854775808n, 9223372036854775807n, "Введите целое число", "Введите число в диапазоне Long");
        case "location.y": return decimalError(value, 3.4028234663852886e38, true);
        case "location.z": return integerError(value, -2147483648n, 2147483647n, "Введите целое число", "Введите число в диапазоне Integer");
        default: return "";
    }
}

function checkFilterRow(row) {
    const field = row.querySelector(".filter-field");
    const value = row.querySelector(".filter-value-control");
    if (!field.value || value.value === "") {
        showFieldError(value, "");
        return true;
    }
    const error = filterValueError(field.value, value.value);
    showFieldError(value, error);
    return error === "";
}

function makeRow(kind, selected = "") {
    const row = document.createElement("div");
    row.className = `${kind}-row query-row`;
    const field = document.createElement("select");
    field.className = kind === "filter" ? "filter-field" : "";
    field.setAttribute("aria-label", kind === "sort" ? "Поле сортировки" : "Поле фильтра");
    fillSelect(field, fields, "Выберите поле");
    field.value = selected;
    let value;
    let valueBox;
    if (kind === "filter") {
        const rowNumber = ++filterCounter;
        valueBox = document.createElement("div");
        valueBox.className = "filter-value";
        const error = document.createElement("span");
        error.id = `filter-error-${rowNumber}`;
        error.className = "field-error";
        const makeValue = () => {
            error.textContent = "";
            const choices = field.value === "eyeColor" ? eyeColors
                : field.value === "hairColor" ? hairColors
                    : field.value === "nationality" ? ["null", ...nationalities] : null;
            const control = document.createElement(choices ? "select" : "input");
            control.className = "filter-value-control";
            control.id = `filter-value-${rowNumber}`;
            control.setAttribute("aria-label", "Значение фильтра");
            control.setAttribute("aria-describedby", error.id);
            if (choices) fillSelect(control, choices, "Выберите значение");
            else if (field.value === "creationDate") control.type = "date";
            else control.autocomplete = "off";
            control.addEventListener("blur", () => checkFilterRow(row));
            control.addEventListener("input", () => {
                state.page = 0;
                if (control.hasAttribute("aria-invalid")) checkFilterRow(row);
            });
            control.addEventListener("change", () => {
                state.page = 0;
                if (control.hasAttribute("aria-invalid")) checkFilterRow(row);
            });
            valueBox.replaceChildren(control, error);
            value = control;
        };
        makeValue();
        field.addEventListener("change", () => {
            state.page = 0;
            makeValue();
        });
    } else {
        value = document.createElement("select");
        value.setAttribute("aria-label", "Направление");
        fillSelect(value, ["asc", "desc"], null);
        field.addEventListener("change", () => { state.page = 0; });
        value.addEventListener("change", () => { state.page = 0; });
    }
    const remove = document.createElement("button");
    remove.type = "button";
    remove.className = "icon-button";
    remove.textContent = "×";
    remove.setAttribute("aria-label", "Удалить строку");
    remove.addEventListener("click", () => { row.remove(); state.page = 0; });
    row.append(field, kind === "filter" ? valueBox : value, remove);
    return row;
}

function buildQuery() {
    const params = new URLSearchParams();
    params.set("page", String(state.page));
    params.set("pageSize", byId("page-size").value);
    for (const row of filterRows()) {
        const field = row.querySelector(".filter-field");
        const input = row.querySelector(".filter-value-control");
        if (field.value && input.value !== "") params.append(field.value, input.value);
    }
    for (const row of sortRows()) {
        const [field, direction] = row.querySelectorAll("select");
        if (field.value) {
            params.append("sortBy", field.value);
            params.append("order", direction.value);
        }
    }
    return params;
}

function renderPerson(person) {
    const box = document.createElement("div");
    box.className = "result-card";
    const values = [
        ["ID", person.id], ["Имя", person.name],
        ["Координаты", person.coordinates ? `${person.coordinates.x}, ${person.coordinates.y}` : "—"],
        ["Дата создания", person.creationDate], ["Рост", person.height],
        ["Цвет глаз", person.eyeColor], ["Цвет волос", person.hairColor],
        ["Национальность", person.nationality ?? "не указана"],
        ["Местоположение", person.location ? `${person.location.x}, ${person.location.y}, ${person.location.z}` : "—"]
    ];
    const dl = document.createElement("dl");
    for (const [label, value] of values) {
        const dt = document.createElement("dt");
        const dd = document.createElement("dd");
        dt.textContent = label;
        dd.textContent = value == null ? "—" : String(value);
        dl.append(dt, dd);
    }
    box.append(dl);
    return box;
}

function renderTable(person) {
    const row = document.createElement("tr");
    const values = [
        person.id, person.name,
        person.coordinates ? `${person.coordinates.x}; ${person.coordinates.y}` : "—",
        person.creationDate, person.height, person.eyeColor, person.hairColor,
        person.nationality ?? "—",
        person.location ? `${person.location.x}; ${person.location.y}; ${person.location.z}` : "—"
    ];
    for (const value of values) {
        const cell = document.createElement("td");
        cell.textContent = value == null ? "—" : String(value);
        row.append(cell);
    }
    const actions = document.createElement("td");
    const buttons = document.createElement("div");
    buttons.className = "table-actions";
    const edit = document.createElement("button");
    edit.className = "button secondary";
    edit.type = "button";
    edit.textContent = "Изменить";
    edit.addEventListener("click", () => loadPersonIntoForm(person));
    const remove = document.createElement("button");
    remove.className = "button secondary";
    remove.type = "button";
    remove.textContent = "Удалить";
    remove.addEventListener("click", () => deletePerson(person.id));
    buttons.append(edit, remove);
    actions.append(buttons);
    row.append(actions);
    return row;
}

async function loadPeople() {
    const message = byId("list-message");
    setMessage(message, "Загрузка…");
    try {
        const params = buildQuery();
        const people = await request(`${APP_CONFIG.peopleApi}/people?${params}`, {}, "Не удалось загрузить людей");
        if (!Array.isArray(people)) throw new Error("Сервис вернул ответ не в формате массива");
        const tbody = byId("people-table");
        tbody.replaceChildren(...people.map(renderTable));
        setMessage(message, people.length ? `На странице записей: ${people.length}` : "На этой странице записей нет.");
        byId("page-label").textContent = `Страница ${state.page + 1}`;
        byId("previous-page").disabled = state.page === 0;
        byId("next-page").disabled = people.length < Number(byId("page-size").value);
    } catch (error) {
        byId("people-table").replaceChildren();
        setMessage(message, error.message, true);
    }
}

function setForm(person) {
    const form = byId("person-form");
    clearFormErrors(form);
    form.elements.name.value = person.name ?? "";
    form.elements.height.value = person.height ?? "";
    form.elements.coordinatesX.value = person.coordinates?.x ?? "";
    form.elements.coordinatesY.value = person.coordinates?.y ?? "";
    form.elements.eyeColor.value = person.eyeColor ?? "";
    form.elements.hairColor.value = person.hairColor ?? "";
    form.elements.nationality.value = person.nationality ?? "";
    form.elements.locationX.value = person.location?.x ?? "";
    form.elements.locationY.value = person.location?.y ?? "";
    form.elements.locationZ.value = person.location?.z ?? "";
}

function loadPersonIntoForm(person) {
    state.editingId = String(person.id);
    setForm(person);
    byId("person-form-title").textContent = `Редактирование человека ${person.id}`;
    byId("save-person").textContent = "Сохранить изменения";
    byId("cancel-edit").classList.remove("hidden");
    setMessage(byId("form-message"), "Измените данные и сохраните запись.");
    byId("person-form").scrollIntoView({ behavior: "smooth", block: "start" });
}

function resetForm() {
    state.editingId = null;
    byId("person-form").reset();
    clearFormErrors(byId("person-form"));
    byId("person-form-title").textContent = "Создать человека";
    byId("save-person").textContent = "Создать";
    byId("cancel-edit").classList.add("hidden");
    setMessage(byId("form-message"), "");
}

function personFields() {
    return [
        "person-name", "person-height", "person-coordinates-x", "person-coordinates-y",
        "person-eye-color", "person-hair-color", "person-location-x", "person-location-y", "person-location-z"
    ].map(byId);
}

function bindFieldValidation(field) {
    field.addEventListener("blur", () => checkField(field));
    field.addEventListener("input", () => {
        if (field.hasAttribute("aria-invalid")) checkField(field);
    });
    field.addEventListener("change", () => {
        if (field.hasAttribute("aria-invalid")) checkField(field);
    });
}

function clearFormErrors(form) {
    form.querySelectorAll("[aria-describedby]").forEach(field => showFieldError(field, ""));
}

function validateFilters() {
    let firstInvalid = null;
    for (const row of filterRows()) {
        const control = row.querySelector(".filter-value-control");
        if (!checkFilterRow(row) && firstInvalid === null) firstInvalid = control;
    }
    if (firstInvalid) {
        firstInvalid.scrollIntoView({ block: "nearest" });
        firstInvalid.focus();
    }
    return firstInvalid === null;
}

function personFromForm() {
    const form = byId("person-form");
    const coordinatesX = BigInt(form.elements.coordinatesX.value);
    const locationX = BigInt(form.elements.locationX.value);
    const person = {
        name: form.elements.name.value,
        coordinates: { x: `LONGTOKEN${coordinatesX}TOKEN`, y: Number(form.elements.coordinatesY.value) },
        height: Number(BigInt(form.elements.height.value)),
        eyeColor: form.elements.eyeColor.value,
        hairColor: form.elements.hairColor.value,
        nationality: form.elements.nationality.value || null,
        location: {
            x: `LONGTOKEN${locationX}TOKEN`,
            y: Number(form.elements.locationY.value),
            z: Number(BigInt(form.elements.locationZ.value))
        }
    };
    return JSON.stringify(person).replace(/"LONGTOKEN(-?\d+)TOKEN"/g, "$1");
}

async function submitPerson(event) {
    event.preventDefault();
    const message = byId("form-message");
    clearSaveNotice();
    if (!validateFields(personFields())) return;
    try {
        const body = personFromForm();
        const editing = state.editingId !== null;
        const url = editing ? `${APP_CONFIG.peopleApi}/people/${encodeURIComponent(state.editingId)}` : `${APP_CONFIG.peopleApi}/people`;
        const person = await request(url, {
            method: editing ? "PUT" : "POST",
            headers: { "Content-Type": "application/json" },
            body
        }, editing ? "Не удалось изменить человека" : "Не удалось создать человека");
        setMessage(message, "");
        showSaveNotice(editing ? "Изменения сохранены" : "Человек успешно создан");
        if (!editing) resetForm();
        await loadPeople();
        if (person) byId("person-result").replaceChildren(renderPerson(person));
    } catch (error) {
        setMessage(message, error.message, true);
    }
}

async function deletePerson(id) {
    try {
        await request(`${APP_CONFIG.peopleApi}/people/${encodeURIComponent(id)}`, { method: "DELETE" }, "Не удалось удалить человека");
        setMessage(byId("list-message"), `Человек ${id} удалён.`);
        if (state.editingId === String(id)) resetForm();
        await loadPeople();
    } catch (error) {
        setMessage(byId("list-message"), error.message, true);
    }
}

async function getPerson(event) {
    event.preventDefault();
    const idField = byId("person-id");
    if (!validateFields([idField])) return;
    const id = event.currentTarget.elements.id.value;
    const message = byId("get-person-message");
    byId("person-result").replaceChildren();
    try {
        const person = await request(`${APP_CONFIG.peopleApi}/people/${encodeURIComponent(id)}`, {}, "Не удалось получить человека");
        byId("person-result").append(renderPerson(person));
        setMessage(message, "Человек найден.");
    } catch (error) {
        setMessage(message, error.message, true);
    }
}

async function runPersonOperation(path, target) {
    target.replaceChildren();
    target.classList.remove("error");
    try {
        const person = await request(`${APP_CONFIG.peopleApi}/people/${path}`, {}, "Не удалось выполнить операцию");
        target.append(renderPerson(person));
    } catch (error) {
        target.textContent = error.message;
        target.classList.add("error");
    }
}

async function submitCount(event) {
    event.preventDefault();
    const colorField = byId("greater-color");
    if (!validateFields([colorField])) return;
    const color = event.currentTarget.elements.color.value;
    const target = byId("greater-result");
    target.classList.remove("error");
    try {
        const result = await request(`${APP_CONFIG.peopleApi}/people/eye-color-greater/count?${new URLSearchParams({ color })}`, {}, "Не удалось получить количество");
        target.textContent = `Количество: ${result}`;
    } catch (error) {
        target.textContent = error.message;
        target.classList.add("error");
    }
}

async function submitPercentage(event, kind) {
    event.preventDefault();
    const colorField = byId(kind === "hair-color" ? "hair-percent-color" : "eye-percent-color");
    if (!validateFields([colorField])) return;
    const color = event.currentTarget.elements.color.value;
    const target = byId(kind === "hair-color" ? "hair-percent-result" : "eye-percent-result");
    target.classList.remove("error");
    try {
        const result = await request(`${APP_CONFIG.demographyApi}/demography/${kind}/${encodeURIComponent(color)}/percentage`, {}, "Не удалось получить процент");
        if (typeof result !== "number" || !Number.isFinite(result)) throw new Error("Сервис вернул некорректный процент.");
        target.textContent = `${result.toFixed(2)}%`;
    } catch (error) {
        target.textContent = error.message;
        target.classList.add("error");
    }
}

function initialize() {
    fillSelect(byId("person-form").elements.eyeColor, eyeColors, "Выберите цвет");
    fillSelect(byId("person-form").elements.hairColor, hairColors, "Выберите цвет");
    fillSelect(byId("person-form").elements.nationality, nationalities, "Не указана");
    for (const id of ["greater-form", "eye-percent-form"]) fillSelect(byId(id).elements.color, eyeColors, "Выберите цвет");
    fillSelect(byId("hair-percent-form").elements.color, hairColors, "Выберите цвет");
    personFields().forEach(bindFieldValidation);
    bindFieldValidation(byId("person-id"));
    ["greater-color", "hair-percent-color", "eye-percent-color"].forEach(id => bindFieldValidation(byId(id)));
    byId("add-filter").addEventListener("click", () => byId("filters").append(makeRow("filter")));
    byId("add-sort").addEventListener("click", () => byId("sorts").append(makeRow("sort")));
    byId("apply-query").addEventListener("click", () => {
        state.page = 0;
        if (validateFilters()) loadPeople();
    });
    for (const [buttonId, panelId] of [["toggle-filters", "filters-panel"], ["toggle-sorts", "sorts-panel"]]) {
        byId(buttonId).addEventListener("click", () => {
            const panel = byId(panelId);
            const expanded = panel.classList.toggle("hidden") === false;
            byId(buttonId).setAttribute("aria-expanded", String(expanded));
        });
    }
    byId("clear-query").addEventListener("click", () => {
        byId("filters").replaceChildren();
        byId("sorts").replaceChildren();
        byId("page-size").value = "20";
        state.page = 0;
        loadPeople();
    });
    byId("page-size").addEventListener("change", () => { state.page = 0; loadPeople(); });
    byId("previous-page").addEventListener("click", () => { if (state.page > 0) { state.page--; loadPeople(); } });
    byId("next-page").addEventListener("click", () => { state.page++; loadPeople(); });
    byId("person-form").addEventListener("submit", submitPerson);
    byId("cancel-edit").addEventListener("click", resetForm);
    byId("get-person-form").addEventListener("submit", getPerson);
    byId("greater-form").addEventListener("submit", submitCount);
    byId("hair-percent-form").addEventListener("submit", event => submitPercentage(event, "hair-color"));
    byId("eye-percent-form").addEventListener("submit", event => submitPercentage(event, "eye-color"));
    document.querySelectorAll("[data-operation]").forEach(button => {
        button.addEventListener("click", () => runPersonOperation(button.dataset.operation, byId(`${button.dataset.operation}-result`)));
    });
    loadPeople();
}

initialize();

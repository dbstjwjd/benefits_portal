/* 폼 편집 화면: 대상 선택, 질문 카드 빌더, 선택지 이미지 업로드 */

(() => {
    const $ = (sel, root = document) => root.querySelector(sel);
    const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

    const data = $("#editData");
    const form = $("#formEdit");
    const list = $("#questionList");
    if (!data || !form || !list) return;

    const locked = data.dataset.locked === "true";
    const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    /* 타입별로 어떤 영역을 켜고 어떤 안내를 붙일지 한 곳에 모아둔다 */
    const TYPES = {
        SHORT_TEXT: { hint: "응답자가 한 줄로 입력합니다." },
        LONG_TEXT: { hint: "응답자가 여러 줄로 입력합니다." },
        PHONE: { hint: "010-1234-5678 형식으로 입력받습니다." },
        SINGLE_CHOICE: { parts: ["choices"] },
        MULTI_CHOICE: { parts: ["choices", "max"] },
        IMAGE_CHOICE: { parts: ["images"] },
        DATE: { parts: ["date"] },
        ADDRESS: { hint: "우편번호 검색 + 기본주소 + 상세주소로 입력받습니다." },
    };

    /* ── 대상: 구분 / 부서 ─────────────────────────────── */

    const targetInput = $("#targetInput");
    const deptPicker = $("#deptPicker");
    const deptChips = $("#deptChips");
    const deptAdd = $("#deptAdd");

    const setSegment = (segment, value) => {
        $$("button", segment).forEach((btn) =>
            btn.classList.toggle("is-active", btn.dataset.value === String(value)));
    };

    setSegment($("#typeSeg"), targetInput.value);
    $("#typeSeg").addEventListener("click", (e) => {
        const btn = e.target.closest("button");
        if (!btn) return;
        targetInput.value = btn.dataset.value;
        setSegment($("#typeSeg"), btn.dataset.value);
    });

    const departmentNames = new Map(
        $$("option", deptAdd).filter((o) => o.value).map((o) => [o.value, o.textContent]));

    const selectedDepartments = () => $$("[data-dept]", deptChips).map((chip) => chip.dataset.dept);

    const syncDeptOptions = () => {
        const taken = new Set(selectedDepartments());
        $$("option", deptAdd).forEach((option) => {
            if (option.value) option.hidden = taken.has(option.value);
        });
    };

    const addDepartment = (id) => {
        if (!departmentNames.has(String(id)) || selectedDepartments().includes(String(id))) return;

        const chip = document.createElement("span");
        chip.className = "dept-chip";
        chip.dataset.dept = String(id);

        const label = document.createElement("span");
        label.textContent = departmentNames.get(String(id));

        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "chip-x";
        remove.setAttribute("aria-label", "부서 제외");
        remove.textContent = "×";
        remove.addEventListener("click", () => {
            chip.remove();
            syncDeptOptions();
        });

        const hidden = document.createElement("input");
        hidden.type = "hidden";
        hidden.name = "departmentIds";
        hidden.value = String(id);

        chip.append(label, remove, hidden);
        deptChips.appendChild(chip);
        syncDeptOptions();
    };

    const setDeptMode = (mode) => {
        setSegment($("#deptSeg"), mode);
        deptPicker.hidden = mode !== "pick";
        if (mode === "all") {
            deptChips.replaceChildren();
            syncDeptOptions();
        }
    };

    $("#deptSeg").addEventListener("click", (e) => {
        const btn = e.target.closest("button");
        if (btn) setDeptMode(btn.dataset.value);
    });

    deptAdd.addEventListener("change", () => {
        if (deptAdd.value) addDepartment(deptAdd.value);
        deptAdd.value = "";
    });

    /* ── 질문 카드 ─────────────────────────────────────── */

    const questionTemplate = $("#questionTemplate");
    const choiceTemplate = $("#choiceTemplate");
    const imageTemplate = $("#imageTemplate");

    const renumber = () => {
        $$(".q-card", list).forEach((card, index) => {
            $(".q-no", card).textContent = "질문 " + (index + 1);
        });
    };

    /** 접힌 카드에 보여줄 한 줄 요약. 제목이 비었으면 자리만 비워 둔다. */
    const refreshSummary = (card) => {
        const type = $("[data-type]", card);
        const option = type.options[type.selectedIndex];
        // 편집기에 없는 타입이면 selectedIndex 가 -1 이라 option 이 없다
        const name = option ? option.textContent
            : (card.dataset.unsupportedType || "알 수 없는 타입");
        $("[data-summary]", card).textContent = $("[data-title]", card).value;
        $("[data-meta]", card).textContent =
            name + ($("[data-required]", card).checked ? " · 필수" : "");
    };

    /**
     * 편집기가 모르는 타입이 하나라도 있으면 저장을 막는다.
     * 그대로 저장하면 그 질문이 전송되지 않아 DB 에서 지워진다.
     */
    const blockUnsupported = () => {
        const bad = $$(".q-card.is-unsupported", list).length > 0;
        const banner = $("#unsupportedBanner");
        const save = $("#saveBtn");
        if (banner) banner.hidden = !bad;
        if (save) save.disabled = bad;
        return bad;
    };

    /** 한 번에 한 질문만 펼친다. 시안처럼 나머지는 한 줄로 접어 둔다. */
    const openCard = (card) => {
        $$(".q-card", list).forEach((other) => {
            other.classList.toggle("is-open", other === card);
            if (other !== card) refreshSummary(other);
        });
    };

    const applyType = (card) => {
        const type = $("[data-type]", card).value;
        const spec = TYPES[type] || {};
        const parts = spec.parts || [];

        $$("[data-part]", card).forEach((part) => {
            part.hidden = !parts.includes(part.dataset.part);
        });

        const hint = $("[data-hint]", card);
        hint.hidden = !spec.hint;
        hint.textContent = spec.hint || "";

        card.classList.toggle("is-multi", type === "MULTI_CHOICE");

        // 선택지가 필요한 타입인데 하나도 없으면 빈 줄 두 개로 시작한다
        if (type === "SINGLE_CHOICE" || type === "MULTI_CHOICE") {
            const container = $("[data-choices]", card);
            while (container.children.length < 2) addChoice(card, {});
        }
    };

    const addChoice = (card, choice) => {
        const node = choiceTemplate.content.firstElementChild.cloneNode(true);
        $("[data-choice-content]", node).value = choice.content || "";
        $("[data-choice-delete]", node).addEventListener("click", () => node.remove());
        enableDrag(node, "[data-choice-grip]", $("[data-choices]", card));
        $("[data-choices]", card).appendChild(node);
    };

    const addImage = (card, choice) => {
        const node = imageTemplate.content.firstElementChild.cloneNode(true);
        $("[data-tile-image]", node).src = choice.imagePath || "";
        $("[data-choice-image]", node).value = choice.imagePath || "";
        $("[data-choice-content]", node).value = choice.content || "";
        $("[data-choice-delete]", node).addEventListener("click", () => node.remove());

        const grid = $("[data-images]", card);
        grid.insertBefore(node, $(".tile-add", grid));
    };

    const buildAddTile = (card) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "tile-add";
        button.textContent = "+ 추가";
        button.addEventListener("click", () => $("[data-image-input]", card).click());
        return button;
    };

    const upload = async (card, file) => {
        if (file.size > MAX_IMAGE_BYTES) {
            alert("이미지는 5MB 이하만 올릴 수 있습니다");
            return;
        }
        const body = new FormData();
        body.append("file", file);

        const res = await fetch("/admin/forms/images", { method: "POST", body });
        if (!res.ok) {
            alert("이미지를 올리지 못했습니다");
            return;
        }
        const result = await res.json();
        if (result.error) {
            alert(result.error);
            return;
        }
        addImage(card, { imagePath: result.path, content: "" });
    };

    const createCard = (question) => {
        const card = questionTemplate.content.firstElementChild.cloneNode(true);
        const typeSelect = $("[data-type]", card);

        typeSelect.value = question.type || "SHORT_TEXT";
        // 값이 안 붙으면(-1) 편집기에 없는 타입이다. 조용히 넘기지 않고 표시해 둔다.
        if (typeSelect.selectedIndex < 0) {
            card.dataset.unsupportedType = question.type;
            card.classList.add("is-unsupported");
        }
        $("[data-title]", card).value = question.title || "";
        $("[data-required]", card).checked = !!question.required;
        $("[data-max-select]", card).value = question.maxSelect ?? "";
        $("[data-min-date]", card).value = question.minDate || "";
        $("[data-max-date]", card).value = question.maxDate || "";

        const grid = $("[data-images]", card);
        grid.appendChild(buildAddTile(card));

        const multiple = $("[data-multiple]", card);
        multiple.value = question.multiple ? "true" : "false";
        setSegment($("[data-image-mode]", card), multiple.value);
        $("[data-image-mode]", card).addEventListener("click", (e) => {
            const btn = e.target.closest("button");
            if (!btn) return;
            multiple.value = btn.dataset.value;
            setSegment($("[data-image-mode]", card), btn.dataset.value);
        });

        $("[data-image-input]", card).addEventListener("change", (e) => {
            const file = e.target.files[0];
            if (file) upload(card, file);
            e.target.value = "";
        });

        (question.choices || []).forEach((choice) => {
            if (question.type === "IMAGE_CHOICE") addImage(card, choice);
            else addChoice(card, choice);
        });

        typeSelect.addEventListener("change", () => {
            delete card.dataset.unsupportedType;
            card.classList.remove("is-unsupported");
            applyType(card);
            refreshSummary(card);
            blockUnsupported();
        });
        $("[data-add-choice]", card).addEventListener("click", () => addChoice(card, {}));
        $("[data-delete]", card).addEventListener("click", () => {
            card.remove();
            renumber();
            blockUnsupported();
        });
        $("[data-duplicate]", card).addEventListener("click", () => {
            const copy = createCard(readCard(card));
            card.after(copy);
            renumber();
            openCard(copy);
        });

        enableDrag(card, "[data-grip]", list);

        card.addEventListener("click", (e) => {
            // 손잡이와 조작 요소는 그대로 두고, 접힌 카드를 누를 때만 펼친다
            if (e.target.closest("[data-grip], button, input, select, textarea, a")) return;
            if (!card.classList.contains("is-open")) {
                openCard(card);
            }
        });

        applyType(card);
        refreshSummary(card);
        return card;
    };

    /** 복제할 때 쓰려고 카드의 현재 입력값을 다시 데이터로 읽어낸다. */
    const readCard = (card) => {
        const type = $("[data-type]", card).value;
        const choices = type === "IMAGE_CHOICE"
            ? $$(".tile", card).map((tile) => ({
                content: $("[data-choice-content]", tile).value,
                imagePath: $("[data-choice-image]", tile).value,
            }))
            : $$(".choice", card).map((row) => ({
                content: $("[data-choice-content]", row).value,
            }));

        return {
            type,
            title: $("[data-title]", card).value,
            required: $("[data-required]", card).checked,
            maxSelect: $("[data-max-select]", card).value || null,
            multiple: $("[data-multiple]", card).value === "true",
            minDate: $("[data-min-date]", card).value,
            maxDate: $("[data-max-date]", card).value,
            choices,
        };
    };

    $("#addQuestion").addEventListener("click", () => {
        const card = createCard({ type: "SHORT_TEXT" });
        list.appendChild(card);
        renumber();
        openCard(card);
        $("[data-title]", card).focus();
    });

    /* ── 끌어서 순서 변경 ──────────────────────────────── */

    let dragging = null;

    const enableDrag = (item, gripSelector, container) => {
        const grip = $(gripSelector, item);
        if (!grip) return;

        // 손잡이를 잡았을 때만 끌리게 한다. 입력칸 안에서 텍스트를 고르는 걸 막지 않으려는 것.
        grip.addEventListener("mousedown", () => { item.draggable = true; });
        item.addEventListener("dragstart", (e) => {
            dragging = { item, container };
            item.classList.add("is-dragging");
            e.dataTransfer.effectAllowed = "move";
            e.dataTransfer.setData("text/plain", "");
            e.stopPropagation();
        });
        item.addEventListener("dragend", () => {
            item.classList.remove("is-dragging");
            item.draggable = false;
            dragging = null;
            renumber();
        });
    };

    const dropTarget = (container, y) => {
        const candidates = Array.from(container.children)
            .filter((child) => child !== dragging.item && !child.classList.contains("tile-add"));
        return candidates.find((child) => {
            const box = child.getBoundingClientRect();
            return y < box.top + box.height / 2;
        }) || null;
    };

    document.addEventListener("dragover", (e) => {
        if (!dragging) return;
        const container = dragging.container;
        if (!container.contains(e.target) && container !== e.target) return;
        e.preventDefault();
        const after = dropTarget(container, e.clientY);
        if (after) container.insertBefore(dragging.item, after);
        else container.appendChild(dragging.item);
    });

    /* ── 저장 ──────────────────────────────────────────── */

    /**
     * 이름은 저장 직전에 붙인다. 중간에 지우거나 순서를 바꿔도 인덱스가 항상 0부터 이어지고,
     * 지금 타입에 해당하지 않는 칸은 이름이 없어 아예 전송되지 않는다.
     */
    form.addEventListener("invalid", (e) => {
        const card = e.target.closest(".q-card");
        if (card && !card.classList.contains("is-open")) {
            openCard(card);
        }
    }, true);

    form.addEventListener("submit", (e) => {
        if (blockUnsupported()) {
            e.preventDefault();
            return;
        }
        if (locked) return;

        $$(".q-card", list).forEach((card, index) => {
            $$("[name]", card).forEach((el) => el.removeAttribute("name"));

            const prefix = "questions[" + index + "]";
            const type = $("[data-type]", card).value;

            $("[data-type]", card).name = prefix + ".type";
            $("[data-title]", card).name = prefix + ".title";
            $("[data-required]", card).name = prefix + ".required";

            if (type === "MULTI_CHOICE") {
                $("[data-max-select]", card).name = prefix + ".maxSelect";
            }
            if (type === "DATE") {
                $("[data-min-date]", card).name = prefix + ".minDate";
                $("[data-max-date]", card).name = prefix + ".maxDate";
            }
            if (type === "IMAGE_CHOICE") {
                $("[data-multiple]", card).name = prefix + ".multiple";
                $$(".tile", card).forEach((tile, seat) => {
                    $("[data-choice-content]", tile).name = prefix + ".choices[" + seat + "].content";
                    $("[data-choice-image]", tile).name = prefix + ".choices[" + seat + "].imagePath";
                });
            }
            if (type === "SINGLE_CHOICE" || type === "MULTI_CHOICE") {
                $$(".choice", card).forEach((row, seat) => {
                    $("[data-choice-content]", row).name = prefix + ".choices[" + seat + "].content";
                });
            }
        });
    });

    /* ── 초기 렌더 ─────────────────────────────────────── */

    const questions = JSON.parse(data.dataset.questions || "[]");
    questions.forEach((question) => list.appendChild(createCard(question)));
    renumber();
    const first = $(".q-card", list);
    if (first) openCard(first);
    blockUnsupported();

    const initialDepartments = JSON.parse(data.dataset.departments || "[]");
    initialDepartments.forEach(addDepartment);
    setDeptMode(initialDepartments.length > 0 ? "pick" : "all");

    if (locked) {
        // 서버도 마감일만 받지만, 화면에서도 손댈 수 없게 막아 오해를 줄인다
        $$("input, textarea, select, button", form).forEach((el) => {
            const isDue = el.id === "f-endDate" || el.id === "f-endTime";
            if (!isDue && el.type !== "submit") el.disabled = true;
        });
        $("#addQuestion").hidden = true;
        $$(".q-actions, [data-add-choice], .tile-add, .tile-del, .choice-del, .chip-x, .dept-add, .grip",
            form).forEach((el) => { el.hidden = true; });
    }
})();

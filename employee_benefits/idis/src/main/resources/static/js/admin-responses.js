/* 응답 현황: 폼 선택 드롭다운과 개별 응답 모달 */

(() => {
    const $ = (sel, root = document) => root.querySelector(sel);
    const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

    /* ── 폼 선택 드롭다운 ──────────────────────────────── */

    const picker = $("#formPicker");
    if (picker) {
        const button = $("#pickerBtn", picker);
        const menu = $("#pickerMenu", picker);

        const close = () => {
            menu.hidden = true;
            button.setAttribute("aria-expanded", "false");
        };

        button.addEventListener("click", () => {
            const opening = menu.hidden;
            menu.hidden = !opening;
            button.setAttribute("aria-expanded", String(opening));
        });

        document.addEventListener("click", (e) => {
            if (!picker.contains(e.target)) close();
        });
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape") close();
        });
    }

    /* ── 엑셀 다운로드 옵션 ────────────────────────────── */

    const downloadModal = $("#downloadModal");
    if (downloadModal) {
        $("#downloadBtn").addEventListener("click", () => downloadModal.showModal());
        $$("[data-close]", downloadModal).forEach((btn) =>
            btn.addEventListener("click", () => downloadModal.close()));
        downloadModal.addEventListener("click", (e) => {
            if (e.target === downloadModal) downloadModal.close();
        });

        $("#downloadForm").addEventListener("submit", (e) => {
            const picked = $$("input[name=columns]", downloadModal).some((box) => box.checked);
            if (!picked) {
                e.preventDefault();
                alert("포함할 직원 정보를 하나 이상 골라주세요");
                return;
            }
            // 파일은 새 창 없이 내려오므로 모달만 닫아 준다
            setTimeout(() => downloadModal.close(), 0);
        });
    }

    /* ── 개별 응답 ─────────────────────────────────────── */

    const modal = $("#detailModal");
    const data = $("#respData");
    if (!modal || !data) return;

    $$("[data-close]", modal).forEach((btn) => btn.addEventListener("click", () => modal.close()));
    modal.addEventListener("click", (e) => {
        if (e.target === modal) modal.close();
    });

    const formId = data.dataset.formId;
    const meta = $("#detailMeta");
    const list = $("#detailList");

    const stamp = (iso) => (iso ? iso.replace("T", " ").slice(0, 16) : "-");

    const render = (detail) => {
        // 시안은 제목을 '응답 상세'로 고정하고 누구의 응답인지는 아래 줄에 적는다
        const parts = [detail.name];
        if (detail.departmentName) parts.push(detail.departmentName);
        parts.push(stamp(detail.submittedAt));
        if (detail.edited) parts.push("수정됨");
        meta.textContent = parts.join(" · ");

        // 응답 내용은 사용자가 적은 값이라 textContent 로만 넣는다
        list.replaceChildren();
        detail.items.forEach((item, index) => {
            const term = document.createElement("dt");
            term.textContent = (index + 1) + ". " + item.question;
            const value = document.createElement("dd");
            value.textContent = item.answer;
            list.append(term, value);
        });
    };

    $$("[data-detail]").forEach((btn) => {
        btn.addEventListener("click", async () => {
            const empNo = btn.dataset.detail;
            const res = await fetch(`/admin/responses/${formId}/detail/${encodeURIComponent(empNo)}`);
            if (!res.ok) {
                alert("응답을 불러오지 못했습니다");
                return;
            }
            render(await res.json());
            modal.showModal();
        });
    });
})();

/* 엑셀 모달: 컬럼 순서 이동 · 전체 선택 · 빈 선택 차단 */
(() => {
    const $ = (sel, root = document) => root.querySelector(sel);
    const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

    const form = $("#downloadForm");
    if (!form) return;

    const submit = $("#downloadSubmit");
    const questionList = $("#questionList");
    const toggleAll = $("#toggleAllQuestions");

    /*
     * 체크된 입력이 넘어가는 순서가 곧 엑셀 컬럼 순서다.
     * 그래서 정렬을 따로 담지 않고 DOM 자체를 옮긴다.
     */
    $$(".orderable").forEach((list) => {
        list.addEventListener("click", (e) => {
            const btn = e.target.closest("[data-move]");
            if (!btn) return;
            const item = btn.closest("li");
            const sibling = btn.dataset.move === "up"
                ? item.previousElementSibling
                : item.nextElementSibling;
            if (!sibling) return;
            if (btn.dataset.move === "up") list.insertBefore(item, sibling);
            else list.insertBefore(sibling, item);
            refreshOrderButtons(list);
        });
        refreshOrderButtons(list);
    });

    /* 맨 위·맨 아래에서는 갈 곳이 없으니 잠근다 */
    function refreshOrderButtons(list) {
        const items = $$("li", list);
        items.forEach((item, i) => {
            const up = $("[data-move='up']", item);
            const down = $("[data-move='down']", item);
            if (up) up.disabled = i === 0;
            if (down) down.disabled = i === items.length - 1;
        });
    }

    const checked = () => $$("input[type=checkbox][name]", form)
        .filter((c) => c.name !== "includePending" && c.checked).length;

    const refreshSubmit = () => {
        submit.disabled = checked() === 0;
    };

    form.addEventListener("change", (e) => {
        if (e.target.type === "checkbox") refreshSubmit();
    });

    if (toggleAll && questionList) {
        toggleAll.addEventListener("click", () => {
            const boxes = $$("input[type=checkbox]", questionList);
            const turnOn = boxes.some((b) => !b.checked);
            boxes.forEach((b) => { b.checked = turnOn; });
            toggleAll.textContent = turnOn ? "전체 해제" : "전체 선택";
            refreshSubmit();
        });
    }

    refreshSubmit();
})();

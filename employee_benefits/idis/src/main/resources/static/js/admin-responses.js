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

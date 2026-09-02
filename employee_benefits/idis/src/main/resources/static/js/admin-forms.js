/* 폼 관리 화면: 삭제 확인 모달 */

(() => {
    const $ = (sel, root = document) => root.querySelector(sel);
    const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

    const modal = $("#deleteFormModal");
    const form = $("#deleteFormForm");
    if (!modal) return;

    $$("[data-close]", modal).forEach((btn) => btn.addEventListener("click", () => modal.close()));
    modal.addEventListener("click", (e) => {
        if (e.target === modal) modal.close();
    });

    /*
     * 응답이 있으면 지우지 않고 감춘다. 되돌릴 수 있는지가 달라서
     * 어느 쪽인지 누르기 전에 알려 준다. 판정은 서버가 다시 한다.
     */
    $$("[data-delete]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const id = btn.dataset.delete;
            const count = Number(btn.dataset.responses || 0);

            form.action = `/admin/forms/${encodeURIComponent(id)}/delete`;
            $("#deleteFormWho").textContent = `${btn.dataset.title} 을(를) 삭제할까요?`;
            $("#deleteFormNote").textContent = count > 0
                ? `응답 ${count}건이 함께 보관됩니다. 목록에서만 사라집니다.`
                : "완전히 삭제됩니다. 되돌릴 수 없습니다.";
            modal.showModal();
        });
    });
})();

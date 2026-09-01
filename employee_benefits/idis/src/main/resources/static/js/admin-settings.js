/* 설정 화면: 담당자 목록 편집과 부서 즉시 반영 */

(() => {
    const $ = (sel, root = document) => root.querySelector(sel);
    const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

    /* ── 문의 담당자: 저장 버튼을 눌러야 반영된다 ─────────── */

    const personList = $("#personList");
    if (personList) {
        const template = $("#personTemplate");

        const bindRemove = (row) =>
            $("[data-remove]", row).addEventListener("click", () => row.remove());
        $$(".person-row", personList).forEach(bindRemove);

        $("#addPerson").addEventListener("click", () => {
            const row = template.content.firstElementChild.cloneNode(true);
            bindRemove(row);
            personList.appendChild(row);
            $(".c-name", row).focus();
        });

        // 이름은 저장 직전에 붙인다. 중간에 지워도 인덱스가 0부터 이어진다
        $("#contactForm").addEventListener("submit", () => {
            $$(".person-row", personList).forEach((row, index) => {
                const prefix = "persons[" + index + "]";
                $(".c-name", row).name = prefix + ".name";
                $(".c-role", row).name = prefix + ".role";
                $(".c-loc", row).name = prefix + ".location";
                $(".c-ext", row).name = prefix + ".extension";
            });
        });
    }

    /* ── 부서: 행마다 바로 서버에 반영한다 ────────────────── */

    const deptList = $("#deptList");
    if (!deptList) return;

    const deptTemplate = $("#deptTemplate");

    const post = async (url, params) => {
        const res = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
            body: new URLSearchParams(params),
        });
        if (!res.ok) {
            return { error: "요청을 처리하지 못했습니다" };
        }
        return res.json();
    };

    const bindRow = (row) => {
        const input = $(".d-name", row);
        const remove = $("[data-remove]", row);
        // 서버가 거절했을 때 되돌릴 값
        let saved = input.value;

        input.addEventListener("change", async () => {
            const name = input.value.trim();
            const id = row.dataset.id;

            if (!name) {
                // 새로 추가하다 만 줄은 그냥 없애고, 기존 부서는 이름을 되돌린다
                if (id) input.value = saved;
                else row.remove();
                return;
            }
            if (name === saved) return;

            const result = id
                ? await post("/admin/settings/departments/" + id, { name })
                : await post("/admin/settings/departments", { name });

            if (result.error) {
                alert(result.error);
                if (id) input.value = saved;
                else input.focus();
                return;
            }
            saved = result.name;
            input.value = result.name;
            if (!id) {
                row.dataset.id = result.id;
            }
        });

        remove.addEventListener("click", async () => {
            const id = row.dataset.id;
            if (!id) {
                row.remove();
                return;
            }
            if (!confirm(`'${saved}' 부서를 삭제할까요?`)) return;

            const result = await post("/admin/settings/departments/" + id + "/delete", {});
            if (result.error) {
                alert(result.error);
                return;
            }
            row.remove();
        });
    };

    $$(".dept-row", deptList).forEach(bindRow);

    $("#addDept").addEventListener("click", () => {
        const row = deptTemplate.content.firstElementChild.cloneNode(true);
        // 아직 저장되지 않은 줄이라 id 가 없다. 이름을 넣고 포커스를 옮기면 그때 만들어진다
        bindRow(row);
        deptList.appendChild(row);
        $(".d-name", row).focus();
    });
})();

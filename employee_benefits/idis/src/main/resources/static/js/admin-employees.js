/* 직원 관리 화면: 추가·수정·퇴사 모달과 엑셀 업로드 마법사 */

(() => {
    const $ = (sel, root = document) => root.querySelector(sel);
    const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

    const employeeModal = $("#employeeModal");
    const employeeForm = $("#employeeForm");
    const resignModal = $("#resignModal");
    const resignForm = $("#resignForm");
    const excelModal = $("#excelModal");
    const deleteModal = $("#deleteModal");
    const deleteForm = $("#deleteForm");

    const today = () => new Date().toISOString().slice(0, 10);

    /** 모달을 열 때의 역할. PIN 이 필수인지 가르는 기준이 된다. */
    let wasAdmin = false;

    /**
     * 역할은 시안대로 두 칸짜리 토글이라 값은 hidden 에 담는다.
     * 슈퍼 관리자가 아니면 이 영역 자체가 없으므로 조용히 넘어간다.
     */
    const setRole = (value) => {
        const seg = $("#roleSeg");
        if (!seg) return;
        $("#f-role").value = value;
        $$("button", seg).forEach((btn) =>
            btn.classList.toggle("is-active", btn.dataset.value === value));
        syncPinRow(value);
    };

    /**
     * PIN 칸은 관리자일 때만 쓴다.
     * 새로 관리자가 되는 경우엔 필수, 이미 관리자면 비워 두면 그대로 둔다.
     */
    const syncPinRow = (role) => {
        const row = $("#pinRow");
        if (!row) return;
        const admin = role === "ADMIN";
        row.hidden = !admin;
        const input = $("#f-pin");
        input.required = admin && !wasAdmin;
        if (!admin) input.value = "";
        $("#pinHint").textContent = wasAdmin
            ? "비워 두면 그대로 둡니다. 값을 넣으면 PIN 이 초기화됩니다."
            : "본인이 첫 로그인에서 반드시 바꾸게 됩니다.";
    };

    $("#roleSeg")?.addEventListener("click", (e) => {
        const btn = e.target.closest("button");
        if (btn) setRole(btn.dataset.value);
    });

    /*
     * 전화번호는 저장할 때 서버가 표준형으로 바꾸지만,
     * 입력 중에도 눈에 보이게 하이픈을 넣어 준다. (로그인 화면과 같은 방식)
     */
    const phoneInput = $("#f-phone");
    if (phoneInput) {
        phoneInput.addEventListener("input", () => {
            const digits = phoneInput.value.replace(/[^0-9]/g, "").slice(0, 11);
            let out = digits;
            if (digits.length > 7) out = `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
            else if (digits.length > 3) out = `${digits.slice(0, 3)}-${digits.slice(3)}`;
            phoneInput.value = out;
        });
    }

    /* 닫기 버튼 / 배경 클릭 공통 */
    $$("dialog").forEach((dialog) => {
        $$("[data-close]", dialog).forEach((btn) =>
            btn.addEventListener("click", () => dialog.close()));
        dialog.addEventListener("click", (e) => {
            if (e.target === dialog) dialog.close();
        });
    });

    /* ── 추가 ─────────────────────────────────────────── */

    $("#addEmployeeBtn").addEventListener("click", () => {
        employeeForm.reset();
        employeeForm.action = "/admin/employees";
        $("#employeeModalTitle").textContent = "직원 추가";
        const empNo = $("#f-empNo");
        empNo.readOnly = false;
        $("#empNoHint").hidden = true;
        wasAdmin = false;
        $("#deleteEmployeeBtn").hidden = true;
        $("#deleteReason").hidden = true;
        if ($("#roleRow")) $("#roleRow").hidden = true;
        if ($("#pinRow")) $("#pinRow").hidden = true;
        $("#f-hire").value = today();
        employeeModal.showModal();
        empNo.focus();
    });

    /* ── 수정 ─────────────────────────────────────────── */

    $$("[data-edit]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const row = btn.closest(".row");
            const empNo = row.dataset.empNo;

            employeeForm.reset();
            employeeForm.action = `/admin/employees/${encodeURIComponent(empNo)}`;
            $("#employeeModalTitle").textContent = "직원 수정";

            const empNoInput = $("#f-empNo");
            empNoInput.value = empNo;
            empNoInput.readOnly = true;
            $("#empNoHint").hidden = false;

            $("#f-name").value = row.dataset.name || "";
            $("#f-dept").value = row.dataset.dept || "";
            $("#f-type").value = row.dataset.type || "";
            $("#f-phone").value = row.dataset.phone || "";
            $("#f-hire").value = row.dataset.hire || "";

            wasAdmin = (row.dataset.role || "EMPLOYEE") === "ADMIN";
            if ($("#roleRow")) $("#roleRow").hidden = false;
            setRole(row.dataset.role || "EMPLOYEE");
            setupDelete(empNo, row.dataset.name, row.dataset.deleteBlocked || "");

            employeeModal.showModal();
            $("#f-name").focus();
        });
    });

    /**
     * 삭제 버튼. 지울 수 없는 경우엔 버튼을 잠그고 사유를 옆에 적는다.
     * 사유 판단은 서버(EmployeeAdminService.deleteBlockReason)가 하고 여기선 받아 쓰기만 한다.
     */
    const setupDelete = (empNo, name, blockedReason) => {
        const btn = $("#deleteEmployeeBtn");
        const reason = $("#deleteReason");

        btn.hidden = false;
        btn.disabled = Boolean(blockedReason);
        btn.title = blockedReason || "";
        reason.textContent = blockedReason;
        reason.hidden = !blockedReason;

        btn.onclick = () => {
            if (btn.disabled) return;
            deleteForm.action = `/admin/employees/${encodeURIComponent(empNo)}/delete`;
            $("#deleteWho").textContent = `${name} 님을 완전히 삭제할까요?`;
            employeeModal.close();
            deleteModal.showModal();
        };
    };

    /* ── 퇴사 ─────────────────────────────────────────── */

    $$("[data-resign]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const row = btn.closest(".row");
            const empNo = row.dataset.empNo;
            resignForm.action = `/admin/employees/${encodeURIComponent(empNo)}/resign`;
            $("#resignWho").textContent = `${row.dataset.name} 님을 퇴사 처리할까요?`;
            $("#f-resign").value = today();
            resignModal.showModal();
        });
    });

    /* ── 엑셀 업로드 마법사 ───────────────────────────── */

    if (excelModal) {
        const fileInput = $("#excelFile");
        const applyBtn = $("#excelApplyBtn");
        const dropZone = $("#dropZone");
        const dropText = $("#dropText");
        const PLACEHOLDER = dropText.textContent;

        const showFile = () => {
            const file = fileInput.files[0];
            dropText.textContent = file ? file.name : PLACEHOLDER;
            dropZone.classList.toggle("has-file", !!file);
        };

        dropZone.addEventListener("click", () => fileInput.click());
        fileInput.addEventListener("change", showFile);

        ["dragenter", "dragover"].forEach((type) =>
            dropZone.addEventListener(type, (e) => {
                e.preventDefault();
                dropZone.classList.add("is-over");
            }));
        ["dragleave", "drop"].forEach((type) =>
            dropZone.addEventListener(type, () => dropZone.classList.remove("is-over")));

        dropZone.addEventListener("drop", (e) => {
            e.preventDefault();
            const file = e.dataTransfer.files[0];
            if (!file) return;
            if (!file.name.toLowerCase().endsWith(".xlsx")) {
                alert("xlsx 파일만 올릴 수 있습니다");
                return;
            }
            // input 에 넣어 두면 그 뒤 전송 경로가 클릭으로 고른 것과 같아진다
            const box = new DataTransfer();
            box.items.add(file);
            fileInput.files = box.files;
            showFile();
        });

        const showStep = (step) => {
            $$("[data-pane]", excelModal).forEach((pane) => {
                pane.hidden = Number(pane.dataset.pane) !== step;
            });
            $$(".step", excelModal).forEach((el) => {
                el.classList.toggle("is-active", Number(el.dataset.step) <= step);
            });
            $("#excelTitle").textContent =
                step === 1 ? "엑셀 파일 업로드" : step === 2 ? "검증 결과" : "완료";
            // 지나온 단계는 번호 대신 체크로 바꾼다
            $$(".step", excelModal).forEach((el) => {
                const n = Number(el.dataset.step);
                el.classList.toggle("is-done", n < step);
                $(".num", el).textContent = n < step ? "✓" : String(n);
            });
        };

        const renderResult = (result) => {
            $("#statCreate").textContent = `${result.createCount}명`;
            $("#statUpdate").textContent = `${result.updateCount}명`;
            $("#statDept").textContent = `${result.newDepartments.length}개`;

            const pills = $("#newDeptPills");
            pills.replaceChildren();
            result.newDepartments.forEach((name) => {
                const pill = document.createElement("span");
                pill.className = "dept-badge";
                pill.textContent = name;
                pills.appendChild(pill);
            });
            $("#newDepts").hidden = result.newDepartments.length === 0;

            const table = $("#errorTable");
            table.querySelectorAll(".r:not(.head)").forEach((r) => r.remove());

            const hasError = result.errors.length > 0;
            $("#errorTitle").hidden = !hasError;
            $("#errorTitle").textContent = `오류 ${result.errors.length}건`;
            table.hidden = !hasError;
            $("#errorNote").hidden = !hasError;
            $("#okBanner").hidden = hasError;
            applyBtn.disabled = hasError;

            result.errors.forEach((err) => {
                const row = document.createElement("div");
                row.className = "r";
                // textContent 로만 넣어 파일에서 온 문자열이 마크업으로 해석되지 않게 한다
                const no = document.createElement("span");
                no.className = "c-no";
                no.textContent = err.rowNumber > 0 ? err.rowNumber : "-";
                const emp = document.createElement("span");
                emp.className = "c-emp";
                emp.textContent = err.empNo || "-";
                const why = document.createElement("span");
                why.className = "c-why";
                why.textContent = err.reason;
                row.append(no, emp, why);
                table.appendChild(row);
            });
        };

        const send = async (url) => {
            const file = fileInput.files[0];
            if (!file) {
                alert("파일을 선택해주세요");
                return null;
            }
            const body = new FormData();
            body.append("file", file);
            const res = await fetch(url, { method: "POST", body });
            if (!res.ok) {
                alert("요청을 처리하지 못했습니다");
                return null;
            }
            return res.json();
        };

        $("#excelUploadBtn").addEventListener("click", () => {
            fileInput.value = "";
            showFile();
            applyBtn.disabled = false;
            showStep(1);
            excelModal.showModal();
        });

        $("#excelValidateBtn").addEventListener("click", async () => {
            const result = await send("/admin/employees/excel/validate");
            if (!result) return;
            renderResult(result);
            showStep(2);
        });

        $("#excelBackBtn").addEventListener("click", () => showStep(1));

        $("#excelApplyBtn").addEventListener("click", async () => {
            const result = await send("/admin/employees/excel/apply");
            if (!result) return;
            if (result.errors.length > 0) {
                renderResult(result);
                showStep(2);
                return;
            }
            $("#doneMsg").textContent =
                `${result.createCount}명 추가, ${result.updateCount}명 갱신되었습니다`;
            showStep(3);
        });

        $("#excelDoneBtn").addEventListener("click", () => location.reload());
    }
})();

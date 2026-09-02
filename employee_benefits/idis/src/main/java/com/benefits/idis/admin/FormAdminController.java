package com.benefits.idis.admin;

import com.benefits.idis.employee.DepartmentRepository;
import com.benefits.idis.employee.Department;
import com.benefits.idis.employee.Employee;
import com.benefits.idis.form.Form;
import com.benefits.idis.form.FormTarget;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/forms")
@RequiredArgsConstructor
public class FormAdminController {

    private final FormAdminService formAdminService;
    private final FormEditService formEditService;
    private final FormImageService formImageService;
    private final DepartmentRepository departmentRepository;
    private final ObjectMapper objectMapper;

    @ModelAttribute("admin")
    public Employee admin(@RequestAttribute("adminEmployee") Employee adminEmployee) {
        return adminEmployee;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        FormSearch search = new FormSearch(keyword, status, page);
        FormListView view = formAdminService.list(search);

        model.addAttribute("menu", "forms");
        model.addAttribute("search", search);
        model.addAttribute("summary", view.summary());
        model.addAttribute("forms", view.rows());
        return "admin/forms";
    }

    // ── 편집 ────────────────────────────────────────────────

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("formId", null);
        model.addAttribute("formTitle", "");
        model.addAttribute("description", "");
        model.addAttribute("target", FormTarget.ALL);
        model.addAttribute("selectedDepartmentIds", "[]");
        model.addAttribute("endDate", "");
        model.addAttribute("endTime", "");
        model.addAttribute("locked", false);
        model.addAttribute("responseCount", 0L);
        return editView(model, List.of());
    }

    @GetMapping("/{id:\\d+}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Form form = formEditService.load(id);

        model.addAttribute("formId", form.getId());
        model.addAttribute("formTitle", form.getTitle());
        model.addAttribute("description", form.getDescription() == null ? "" : form.getDescription());
        model.addAttribute("target", form.getTarget());
        model.addAttribute("selectedDepartmentIds",
                objectMapper.writeValueAsString(
                        form.getTargetDepartments().stream().map(Department::getId).toList()));
        model.addAttribute("endDate", form.getEndAt() == null ? "" : form.getEndAt().toLocalDate());
        model.addAttribute("endTime", form.getEndAt() == null ? ""
                : form.getEndAt().toLocalTime().withSecond(0).withNano(0));
        model.addAttribute("locked", formEditService.locked(id));
        model.addAttribute("responseCount", formEditService.responseCount(id));

        return editView(model, form.getQuestions().stream().map(QuestionView::of).toList());
    }

    @PostMapping
    public String create(@ModelAttribute FormEditForm input,
                         @RequestAttribute("adminEmployee") Employee adminEmployee,
                         RedirectAttributes redirect, Model model) {
        try {
            Long id = formEditService.create(input, adminEmployee);
            redirect.addFlashAttribute("toast", "폼을 만들었습니다");
            return "redirect:/admin/forms/" + id + "/edit";
        } catch (IllegalArgumentException e) {
            // 목록으로 튕기면 작성하던 질문이 통째로 날아가므로 입력한 내용 그대로 다시 그린다
            return rerender(null, input, e.getMessage(), model);
        }
    }

    @PostMapping("/{id:\\d+}")
    public String update(@PathVariable Long id, @ModelAttribute FormEditForm input,
                         RedirectAttributes redirect, Model model) {
        try {
            formEditService.update(id, input);
            redirect.addFlashAttribute("toast", "저장했습니다");
            return "redirect:/admin/forms/" + id + "/edit";
        } catch (IllegalArgumentException e) {
            return rerender(id, input, e.getMessage(), model);
        }
    }

    /**
     * 삭제. 응답이 있으면 감추고(soft), 0건이면 완전히 지운다.
     * 어느 쪽이었는지는 서비스가 정하고 여기서는 안내 문구만 고른다.
     */
    @PostMapping("/{id:\\d+}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        FormAdminService.DeleteResult result = formAdminService.delete(id);
        redirect.addFlashAttribute("toast", result.removed()
                ? "폼을 삭제했습니다"
                : "폼을 목록에서 감췄습니다. 응답 " + result.keptResponses() + "건은 그대로 보관됩니다");
        return "redirect:/admin/forms";
    }

    @PostMapping("/{id:\\d+}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes redirect) {
        formAdminService.restore(id);
        redirect.addFlashAttribute("toast", "폼을 복구했습니다");
        return "redirect:/admin/forms?status=deleted";
    }

    /** 응답자가 보는 화면 그대로 확인한다. 제출 버튼 없이 form-detail 을 읽기 전용으로 재사용한다. */
    @GetMapping("/{id:\\d+}/preview")
    public String preview(@PathVariable Long id, Model model) {
        model.addAttribute("form", formEditService.load(id));
        model.addAttribute("values", Map.of());
        model.addAttribute("submitted", false);
        model.addAttribute("readOnly", true);
        model.addAttribute("preview", true);
        return "form-detail";
    }

    /** 선택지 이미지는 폼 저장과 따로 먼저 올리고, 저장할 때는 경로만 넘어온다. */
    @PostMapping("/images")
    @ResponseBody
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            return Map.of("path", formImageService.store(file));
        } catch (IllegalArgumentException e) {
            return Map.of("error", e.getMessage());
        }
    }

    private String rerender(Long id, FormEditForm input, String error, Model model) {
        model.addAttribute("formId", id);
        model.addAttribute("formTitle", nullToEmpty(input.getTitle()));
        model.addAttribute("description", nullToEmpty(input.getDescription()));
        model.addAttribute("target", input.getTarget() == null ? FormTarget.ALL : input.getTarget());
        model.addAttribute("selectedDepartmentIds",
                objectMapper.writeValueAsString(input.getDepartmentIds()));
        model.addAttribute("endDate", input.getEndDate() == null ? "" : input.getEndDate());
        model.addAttribute("endTime", input.getEndTime() == null ? "" : input.getEndTime());
        model.addAttribute("locked", formEditService.locked(id));
        model.addAttribute("responseCount", formEditService.responseCount(id));
        model.addAttribute("error", error);

        return editView(model, input.getQuestions().stream().map(QuestionView::of).toList());
    }

    private String editView(Model model, List<QuestionView> questions) {
        model.addAttribute("menu", "forms");
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("questionsJson", objectMapper.writeValueAsString(questions));
        return "admin/form-edit";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** 없는 폼을 열었을 때처럼 화면을 그릴 수 없는 경우만 목록으로 돌려보낸다. */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleInvalid(IllegalArgumentException e, HttpServletRequest request) {
        RequestContextUtils.getOutputFlashMap(request).put("error", e.getMessage());
        return "redirect:/admin/forms";
    }
}

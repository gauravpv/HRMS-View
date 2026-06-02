package com.app.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.app.dto.UserListItem;
import com.app.model.Users;
import com.app.repository.RoleRepository;
import com.app.repository.UserRepository;
import com.app.service.TableDetailsService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UserController {

    private static final Logger logger = LogManager.getLogger(UserController.class);

    private final TableDetailsService tabService;
    private final UserRepository userRepo;
    private final RoleRepository roleRepository;
    private final SessionRegistry sessionRegistry;

    @GetMapping({ "/data-management", "/dataManagement" })
    public String dataManagementRedirect() {
        return "redirect:/search-data";
    }

    @GetMapping({ "/search-data", "/searchData" })
    public String searchData(Model model) {
        addTableLists(model);
        return "search-data";
    }

    @GetMapping({ "/history-data", "/historyData" })
    public String historyData(Model model) {
        addTableLists(model);
        return "history-data";
    }

    @GetMapping({ "/table-status", "/tableStatus" })
    public String tableStatus() {
        return "table-status";
    }

    @GetMapping({ "/data-upload", "/dataUpload" })
    public String bulkUpload(Model model) {
        model.addAttribute("tabList", tabService.getTempTables());
        return "bulk-upload";
    }

    @GetMapping({ "/data-movement", "/dataMovement" })
    public String dataMovement(Model model) {
        List<String> tables = tabService.getAllTableNames();
        model.addAttribute("tableMasterList", tables);
        model.addAttribute("tableMainList", tables);
        return "data-movement";
    }

    @GetMapping({ "/move-to-master", "/moveToMaster", "/move-to-main", "/moveToMain" })
    public String legacyDataMovement() {
        return "redirect:/data-movement";
    }

    @GetMapping({ "/user-management", "/userManagement" })
    public String userManagement(Model model, @RequestParam(defaultValue = "all") String status, Principal principal) {
        populateUserStats(model);
        model.addAttribute("userRows", buildUserRows(resolveUsers(status)));
        model.addAttribute("status", status);
        return "user-management";
    }

    @GetMapping("/users")
    public String userListRedirect() {
        return "redirect:/user-management?status=all";
    }

    @GetMapping("/active")
    public String activeRedirect() {
        return "redirect:/user-management?status=active";
    }

    @GetMapping({ "/inactive", "/inActive" })
    public String inactiveRedirect() {
        return "redirect:/user-management?status=inactive";
    }

    @RequestMapping({ "/deactivate", "/deActive" })
    public String deactivate(@RequestParam("id") int id, RedirectAttributes redirectAttributes) {
        Users user = userRepo.getUserById(id);
        if (user == null) {
            logger.warn("deactivate: user not found id={}", id);
            redirectAttributes.addFlashAttribute("msg", "User not found.");
            return "redirect:/user-management?status=active";
        }
        user.setIsEnabled(1);
        userRepo.save(user);
        return "redirect:/user-management?status=active";
    }

    @RequestMapping("/activate")
    public String activate(@RequestParam("id") int id, RedirectAttributes redirectAttributes) {
        Users user = userRepo.getUserById(id);
        if (user == null) {
            logger.warn("activate: user not found id={}", id);
            redirectAttributes.addFlashAttribute("msg", "User not found.");
            return "redirect:/user-management?status=inactive";
        }
        user.setIsEnabled(0);
        userRepo.save(user);
        return "redirect:/user-management?status=inactive";
    }

    private void addTableLists(Model model) {
        model.addAttribute("tabList", tabService.getAllTableNames());
        model.addAttribute("tabTempList", tabService.getTempTables());
    }

    private List<Users> resolveUsers(String status) {
        return switch (status) {
            case "active" -> userRepo.findActiveUsers();
            case "inactive" -> userRepo.findInactiveUsers();
            default -> userRepo.findAll();
        };
    }

    private void populateUserStats(Model model) {
        List<Users> all = userRepo.findAll();
        model.addAttribute("totalUsers", all.size());
        model.addAttribute("activeEnabled", all.stream().filter(u -> u.getIsEnabled() != null && u.getIsEnabled() == 0).count());
        model.addAttribute("inactiveEnabled", all.stream().filter(u -> u.getIsEnabled() != null && u.getIsEnabled() == 1).count());
        model.addAttribute("activeNow", sessionRegistry.getAllPrincipals().size());
    }

    private List<UserListItem> buildUserRows(List<Users> users) {
        List<UserListItem> rows = new ArrayList<>();
        for (Users user : users) {
            List<String> roles = roleRepository.findRolebyUserId(user.getUserId());
            rows.add(new UserListItem(user, roles.isEmpty() ? "USER" : roles.get(0)));
        }
        return rows;
    }
}

package com.app.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.model.Users;
import com.app.repository.UserRepository;
import com.app.service.TableDetailsService;

@Controller
public class UserController {
	
	@Autowired
	TableDetailsService tabService;
	
	@Autowired
	UserRepository userRepo;
	
	@GetMapping("/dataUpload")
	public String bulkDataUpload(Model model) {
		List<String> tabList = tabService.getTempTables();
		model.addAttribute("tabList", tabList);
		return "file";
	}
	
	@GetMapping("/searchData")
	public String searchData(Model model) {
		List<String> tabList = tabService.getAllTableNames();
		List<String> tabTempList = tabService.getTempTables();
		model.addAttribute("tabList", tabList);
		model.addAttribute("tabTempList", tabTempList);
		return "searchData";
	}

	@GetMapping("/historyData")
	public String historyData(Model model) {
		List<String> tabList = tabService.getAllTableNames();
		List<String> tabTempList = tabService.getTempTables();
		model.addAttribute("tabList", tabList);
		model.addAttribute("tabTempList", tabTempList);
		return "historyData";
	}
	
    @GetMapping("/moveToMaster")
    public String moveToMaster(Model model) {
        List<String> nameList = tabService.getAllTableNames();
        model.addAttribute("tableMasterList", nameList);
        return "moveToMaster";
    }
    
    @GetMapping("/moveToMain")
    public String moveToMain(Model model) {
        List<String> nameList = tabService.getAllTableNames();
        model.addAttribute("tableMainList", nameList);
        return "moveToMain";
    }
    
	@GetMapping("/users")
	public String userlist(Model model, Principal principal) {
		List<Users> activeUsers = userRepo.activeUsers();
		model.addAttribute("getUser", activeUsers);
		String userName = principal.getName();
		Users users = userRepo.findByUsername(userName);
		model.addAttribute("uname", users);
		return "users";
	}
	
	@GetMapping("/active")
	public String deActive(Model model) {
		List<Users> activeUsers = userRepo.activeUsers();
		model.addAttribute("getUser", activeUsers);
		return "active";
	}

	@GetMapping("/inActive")
	public String inActiveList(Model model) {
		List<Users> list = userRepo.inActiveUsers();
		model.addAttribute("list", list);
		return "inactive";
	}
	
	@RequestMapping("/deActive")
	public String delete(Model model, @RequestParam("id") int id) {
		Users user = userRepo.getUserById(id);
		user.setIsEnabled(1);
		userRepo.save(user);
		return "redirect:/active";
	}

	@RequestMapping("/activate")
	public String activateUser(Model model, @RequestParam("id") int id) {
		Users user = userRepo.getUserById(id);
		user.setIsEnabled(0);
		userRepo.save(user);
		return "redirect:/inActive";
	}
	
}

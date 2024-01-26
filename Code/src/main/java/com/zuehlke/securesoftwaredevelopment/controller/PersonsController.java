package com.zuehlke.securesoftwaredevelopment.controller;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.config.SecurityUtil;
import com.zuehlke.securesoftwaredevelopment.domain.Person;
import com.zuehlke.securesoftwaredevelopment.domain.User;
import com.zuehlke.securesoftwaredevelopment.repository.PersonRepository;
import com.zuehlke.securesoftwaredevelopment.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.List;

@Controller

public class PersonsController {

    private static final Logger LOG = LoggerFactory.getLogger(PersonsController.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(PersonRepository.class);

    private final PersonRepository personRepository;
    private final UserRepository userRepository;

    public PersonsController(PersonRepository personRepository, UserRepository userRepository) {
        this.personRepository = personRepository;
        this.userRepository = userRepository;
    }

    // autorizacija
    @GetMapping("/persons/{id}")
    public String person(@PathVariable int id, Model model, Authentication authentication, HttpSession session) {
        User currentUser = (User) authentication.getPrincipal();
        Person person = personRepository.get("" + id);

        if (currentUser.getId() == id || SecurityUtil.hasPermission("VIEW_PERSON")) {
            model.addAttribute("CSRF_TOKEN", session.getAttribute("CSRF_TOKEN"));
            model.addAttribute("person", person);
            return "person";
        } else {
            throw new AccessDeniedException("You are not allowed to view this profile!");
        }
    }


    // autorizacija
    @GetMapping("/myprofile")
    @PreAuthorize("hasAuthority('VIEW_MY_PROFILE')")
    public String self(Model model, Authentication authentication, HttpSession session) {
        User user = (User) authentication.getPrincipal();
        model.addAttribute("CSRF_TOKEN", session.getAttribute("CSRF_TOKEN"));
        model.addAttribute("person", personRepository.get("" + user.getId()));
        return "person";
    }

    // autorizacija
    @DeleteMapping("/persons/{id}")
    public ResponseEntity<Void> deletePerson(@PathVariable int id, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();

        if (currentUser.getId() == id || SecurityUtil.hasPermission("UPDATE_PERSON")) {
            personRepository.delete(id);
            userRepository.delete(id);
            return ResponseEntity.noContent().build();
        } else {
            throw new AccessDeniedException("You are not allowed to delete this profile");
        }
    }

    // autorizacija
    @PostMapping("/update-person")
    public String updatePerson(Person person, Authentication authentication, @RequestParam("csrfToken") String csrfToken, HttpSession session) throws AccessDeniedException {
        String csrf = session.getAttribute("CSRF_TOKEN").toString();
        User currentUser = (User) authentication.getPrincipal();

        if (!csrf.equals(csrfToken)) {
            throw new AccessDeniedException("CSRF token mismatch!");
        }

        if (currentUser.getId() == Integer.parseInt(person.getId())
                || SecurityUtil.hasPermission("UPDATE_PERSON")) {
            personRepository.update(person);
            return "redirect:/persons/" + person.getId();
        } else {
            throw new AccessDeniedException("You are not allowed to update this profile!");
        }
    }


    // autorizacija
    @GetMapping("/persons")
    @PreAuthorize("hasAuthority('VIEW_PERSONS_LIST')")
    public String persons(Model model) {
        model.addAttribute("persons", personRepository.getAll());
        return "persons";
    }

    // autorizacija
    @GetMapping(value = "/persons/search", produces = "application/json")
    @ResponseBody
    @PreAuthorize("hasAuthority('VIEW_PERSONS_LIST')")
    public List<Person> searchPersons(@RequestParam String searchTerm) throws SQLException {
        return personRepository.search(searchTerm);
    }
}

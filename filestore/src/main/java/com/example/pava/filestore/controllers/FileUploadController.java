package com.example.pava.filestore.controllers;

import com.example.pava.filestore.rds.DbUser;
import com.example.pava.filestore.rds.IDbUserRepository;
import com.example.pava.filestore.storage.IStorageService;
import com.example.pava.filestore.storage.StorageFileNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@Controller
public class FileUploadController {

    private IStorageService storageService;
    private IDbUserRepository dbUserRepository;

    @Autowired
    public FileUploadController(IStorageService storageService, IDbUserRepository dbUserRepository) {
        this.storageService = storageService;
        this.dbUserRepository = dbUserRepository;
    }

    @GetMapping("/")
    public String listUploadedFiles(Model model, Authentication authentication, @AuthenticationPrincipal OidcUser oidcUser) {

        //check if works
        if (authentication != null && authentication.isAuthenticated()) {
            if (oidcUser != null) {
                var username = oidcUser.getName();
                var allFiles = storageService.loadAll(username);
                model.addAttribute("files", allFiles
                        .map(file -> file.toString())
                        .collect(Collectors.toList())
                );
                model.addAttribute("username", oidcUser.getName());
                model.addAttribute("firstName", oidcUser.getClaimAsString("given_name"));
                model.addAttribute("lastName", oidcUser.getClaimAsString("family_name"));
                model.addAttribute("email", oidcUser.getClaimAsString("email"));
            }
            if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                model.addAttribute("secretMessage", "Admin message is s3crEt");
            } else {
                model.addAttribute("secretMessage", "Lorem ipsum dolor sit amet");
            }
        }

        model.addAttribute("message", "Filestore application");

        return "uploadForm";
    }

    @GetMapping("/files")
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(@Param("filename") String filename) {

        var responseStream = storageService.loadAsResource(filename);
        return ResponseEntity
                .ok().
                        header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + filename + "\"")
                .contentLength(responseStream.response().contentLength())
                .contentType(MediaType.parseMediaType(responseStream.response().contentType()))
                .body(new InputStreamResource(responseStream));
    }

    @GetMapping("/delete/{username}/{filename}")
    public String deleteFile(@PathVariable String username,
                             @PathVariable String filename,
                             Authentication authentication,
                             @AuthenticationPrincipal OidcUser oidcUser) {
        if (authentication != null && authentication.isAuthenticated()) {
            if (oidcUser != null) {
                var openIdUserName = oidcUser.getName();
                if (openIdUserName.equals(username)) {
                    var response = storageService.deleteFile(username + "/" + filename);
                }
            }
        }
        return "redirect:/";
    }

    @PostMapping("/")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                             RedirectAttributes redirectAttributes,
                             Authentication authentication,
                             @AuthenticationPrincipal OidcUser oidcUser) {
        if (authentication != null && authentication.isAuthenticated()) {
            if (oidcUser != null) {
                storageService.store(file, oidcUser.getName());
                redirectAttributes.addFlashAttribute("message",
                        "You successfully uploaded " + file.getOriginalFilename() + "!");

                var uploadingUser = new DbUser();
                uploadingUser.setFirstName(oidcUser.getClaimAsString("given_name"));
                uploadingUser.setLastName(oidcUser.getClaimAsString("family_name"));
                uploadingUser.setEmail(oidcUser.getClaimAsString("email"));
                uploadingUser.setUserName(oidcUser.getName());
                uploadingUser.setDescription("Test Description");
                uploadingUser.setUploadedAt(OffsetDateTime.now());
                uploadingUser.setLastModifiedAt(OffsetDateTime.now());
                dbUserRepository.save(uploadingUser);
            }
        }

        return "redirect:/";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }
}

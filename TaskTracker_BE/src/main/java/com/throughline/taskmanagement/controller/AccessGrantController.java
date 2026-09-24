package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.request.GrantAccessRequest;
import com.throughline.taskmanagement.dto.response.AccessGrantResponse;
import com.throughline.taskmanagement.enums.AccessResourceType;
import com.throughline.taskmanagement.security.CurrentPersonResolver;
import com.throughline.taskmanagement.service.AccessGrantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Sharing a task or incident with someone outside its normal scope. grantedById/actorId are always
 *  the caller's own login, never taken from the request. Granting, revoking and listing who has access
 *  are for an Executive/Super Admin, or a Director for their own department's items (enforced in the service);
 *  "mine" is for any signed-in person. */
@RestController
@RequestMapping("/api/v1/access-grants")
@RequiredArgsConstructor
public class AccessGrantController {

    private final AccessGrantService accessGrantService;
    private final CurrentPersonResolver currentPersonResolver;

    @PostMapping
    public ResponseEntity<AccessGrantResponse> grant(@Valid @RequestBody GrantAccessRequest request, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        GrantAccessRequest verified = new GrantAccessRequest(
                request.resourceType(), request.resourceId(), request.granteeId(), request.reason(), actorId);
        return new ResponseEntity<>(accessGrantService.grant(verified), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable Long id, Authentication authentication) {
        accessGrantService.revoke(id, currentPersonResolver.resolveId(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<AccessGrantResponse>> listForResource(
            @RequestParam AccessResourceType resourceType, @RequestParam Long resourceId, Authentication authentication) {
        return ResponseEntity.ok(accessGrantService.listForResource(
                resourceType, resourceId, currentPersonResolver.resolveId(authentication)));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<AccessGrantResponse>> sharedWithMe(Authentication authentication) {
        return ResponseEntity.ok(accessGrantService.listSharedWith(currentPersonResolver.resolveId(authentication)));
    }

    @GetMapping("/mine/status")
    public ResponseEntity<Map<String, Object>> myGrantStatus(
            @RequestParam AccessResourceType resourceType, @RequestParam Long resourceId, Authentication authentication) {
        Long viewerId = currentPersonResolver.resolveId(authentication);
        String sharedBy = accessGrantService.sharedByName(viewerId, resourceType, resourceId);
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("viaGrant", sharedBy != null);
        body.put("sharedByName", sharedBy);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/can-manage")
    public ResponseEntity<Map<String, Boolean>> canManage(
            @RequestParam AccessResourceType resourceType, @RequestParam Long resourceId, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        return ResponseEntity.ok(Map.of("canManage", accessGrantService.canManage(actorId, resourceType, resourceId)));
    }
}

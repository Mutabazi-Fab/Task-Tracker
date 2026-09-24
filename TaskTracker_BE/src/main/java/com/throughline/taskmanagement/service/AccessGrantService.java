package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.request.GrantAccessRequest;
import com.throughline.taskmanagement.dto.response.AccessGrantResponse;
import com.throughline.taskmanagement.enums.AccessResourceType;

import java.util.List;

/** Lets an Executive or Super Admin share one task or incident with one person outside its normal
 *  scope. A grant has no expiry: it stays until an Executive/Super Admin revokes it. */
public interface AccessGrantService {

    /** Executive/Super Admin for any task or incident; a Director only for one belonging to their own
     *  department. Enforced here. Returns the existing grant if that person already has one. */
    AccessGrantResponse grant(GrantAccessRequest request);

    /** Same authority as granting it. The person loses access at once. */
    void revoke(Long grantId, Long actorId);

    /** Everyone currently given access to this item. Same authority as granting it. */
    List<AccessGrantResponse> listForResource(AccessResourceType type, Long resourceId, Long actorId);

    /** What has been shared with this person — backs their "Shared with you" list. */
    List<AccessGrantResponse> listSharedWith(Long personId);

    /** Whether this person may share (and remove access to) this item — drives whether the page shows the
     *  Share access panel. */
    boolean canManage(Long actorId, AccessResourceType type, Long resourceId);

    /** The name of whoever shared this item with this person, or null if it isn't shared with them. */
    String sharedByName(Long personId, AccessResourceType type, Long resourceId);

    /** Whether this person currently has a grant on this item — backs the "not from your department" banner. */
    boolean hasActiveGrant(Long personId, AccessResourceType type, Long resourceId);
}

package com.throughline.taskmanagement.exception;

/** The actor is identified and their input is well-formed, but the business rule they're attempting
 *  isn't allowed for their role (e.g. a non-Director creating a team, a Team Leader touching a team
 *  they don't lead). */
public class ForbiddenActionException extends RuntimeException {
    public ForbiddenActionException(String message) {
        super(message);
    }
}

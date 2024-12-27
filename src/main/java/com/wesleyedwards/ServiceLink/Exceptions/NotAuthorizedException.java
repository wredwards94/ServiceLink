package com.wesleyedwards.ServiceLink.Exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotAuthorizedException extends RuntimeException{

    @Serial
    private static final long serialVersionUID = -4962931086062439104L;

    private String message;
}

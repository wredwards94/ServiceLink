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
public class NotFoundException extends RuntimeException{

    @Serial
    private static final long serialVersionUID = -8499648535468831887L;

    private String message;
}

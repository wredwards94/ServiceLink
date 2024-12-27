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
public class BadRequestException extends RuntimeException{

    @Serial
    private static final long serialVersionUID = 6163441484693371153L;

    private String message;
}

package it.davidlab.timeslot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;


public class APIExceptionHandler extends RuntimeException{


    @ExceptionHandler(APIExceptionHandler.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<String> UserCreationException(RuntimeException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }


    @ExceptionHandler(APIExceptionHandler.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<String> BadRequestException(RuntimeException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

}

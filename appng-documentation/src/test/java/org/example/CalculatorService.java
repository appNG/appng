package org.example;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CalculatorService {

    @RequestMapping(value = "/add", // <1>
            method = RequestMethod.POST, // <2>
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE) // <3>
    public ResponseEntity<Result> add(@RequestBody Operators operators) { // <4>
        return new ResponseEntity<Result>(new Result(operators.a, operators.b), HttpStatus.OK);
    }

    class Operators {
        Integer a;
        Integer b;

        public Integer getA() {
            return a;
        }

        public void setA(Integer a) {
            this.a = a;
        }

        public Integer getB() {
            return b;
        }

        public void setB(Integer b) {
            this.b = b;
        }
    }

    class Result extends Operators {
        Integer result;

        public Result(Integer a, Integer b) {
            this.a = a;
            this.b = b;
            this.result = a + b;
        }

        public Integer getResult() {
            return result;
        }

        public void setResult(Integer result) {
            this.result = result;
        }
    }

}